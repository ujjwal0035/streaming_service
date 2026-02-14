package com.sse.stream.stream.agent_flow.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseRegistry {
    // Direct mapping: UserId -> Single Emitter
    private final ConcurrentHashMap<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    public void addEmitter(String userId, SseEmitter emitter) {
        // If user already has a connection, complete the old one to free resources
        SseEmitter oldEmitter = userEmitters.put(userId, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete(); 
        }

        // Cleanup hooks
        emitter.onCompletion(() -> userEmitters.remove(userId, emitter));
        emitter.onTimeout(() -> userEmitters.remove(userId, emitter));
        emitter.onError((e) -> userEmitters.remove(userId, emitter));
    }

    public void initiateConnection(String userId) {
        try {
            SseEmitter emitter = userEmitters.get(userId);
            if (emitter != null) {
                // Send initial "connected" event
                emitter.send(SseEmitter.event().name("init").data("connected"));
            }
        } catch (IOException e) {
            userEmitters.remove(userId);
        }
    }

    public void sendToUser(String userId, Object data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            send(emitter, data);
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
            // If the connection is broken, clean up
            emitter.complete();
        }
    }
}
