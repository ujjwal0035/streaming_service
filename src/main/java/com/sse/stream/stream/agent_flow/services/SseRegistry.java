package com.sse.stream.stream.agent_flow.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseRegistry {
    // Direct mapping: UserId -> Single Emitter
    private final ConcurrentHashMap<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    public void addEmitter(String userId, SseEmitter emitter) {
        // If user already has a connection, complete the old one to free resources
        SseEmitter oldEmitter = userEmitters.put(userId, emitter);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.warn("Failed to complete old emitter for user [{}]", userId);
            }
        }

        // Cleanup hooks
        emitter.onCompletion(() -> {
            log.info("Emitter completed for user [{}]", userId);
            userEmitters.remove(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("Emitter timeout for user [{}]", userId);
            userEmitters.remove(userId, emitter);
        });
        emitter.onError((e) -> {
            log.error("Emitter error for user [{}]: {}", userId, e.getMessage());
            userEmitters.remove(userId, emitter);
        });
    }

    /**
     * Remove an emitter for a user
     */
    public void removeEmitter(String userId) {
        SseEmitter emitter = userEmitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Failed to complete emitter for user [{}]", userId);
            }
        }
    }

    public void initiateConnection(String userId) {
        try {
            SseEmitter emitter = userEmitters.get(userId);
            if (emitter != null) {
                // Send initial "connected" event
                emitter.send(SseEmitter.event().name("init").data("connected"));
            }
        } catch (IOException e) {
            log.error("Failed to send init event to user [{}]", userId);
            userEmitters.remove(userId);
        }
    }

    public void sendToUser(String userId, Object data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            send(emitter, data);
        } else {
            log.warn("No active connection for user: {}", userId);
        }
    }

    public void sendToAll(Object data) {
        // Parallel stream on virtual threads is very efficient for 15k connections
        userEmitters.forEach((userId, emitter) -> send(emitter, data));
    }

    private void send(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            log.error("Failed to send message, completing emitter: {}", e.getMessage());
            // If the connection is broken, clean up
            try {
                emitter.complete();
            } catch (Exception ex) {
                log.warn("Failed to complete emitter after send failure: {}", ex.getMessage());
            }
        }
    }

    public Map<String, SseEmitter> getAllEmitters() {
        return new ConcurrentHashMap<>(userEmitters);
    }
}