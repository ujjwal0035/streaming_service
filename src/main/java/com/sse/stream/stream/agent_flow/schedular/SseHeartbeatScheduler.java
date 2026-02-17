package com.sse.stream.stream.agent_flow.schedular;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sse.stream.stream.agent_flow.services.RedisCacheService;
import com.sse.stream.stream.agent_flow.services.SseRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {
    private final SseRegistry sseRegistry;
    private final RedisCacheService redisCacheService;

    /**
     * Sends heartbeat to all connected agents every 15 seconds.
     * If send fails, user is considered disconnected.
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeatToAllAgents() {

        Map<String, SseEmitter> emitters = sseRegistry.getAllEmitters();

        if (emitters.isEmpty()) {
            return;
        }

        log.debug("Sending heartbeat to {} connected agents", emitters.size());

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {

            String agentId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping")
                        .id(String.valueOf(Instant.now().toEpochMilli()))
                );

                // Refresh Redis TTL to avoid ghost sessions
                redisCacheService.refreshAgentSessionTTL(agentId);

            } catch (IOException ex) {

                log.warn("Detected disconnected agent [{}]. Cleaning up.", agentId);

                cleanupAgent(agentId, emitter);
            } catch (Exception e) {

                log.error("Unexpected error while sending heartbeat to [{}]: {}",
                        agentId, e.getMessage());

                cleanupAgent(agentId, emitter);
            }
        }
    }

    /**
     * Cleanup emitter + Redis session safely
     */
    private void cleanupAgent(String agentId, SseEmitter emitter) {

        try {
            emitter.complete();
        } catch (Exception ignored) {}

        sseRegistry.removeEmitter(agentId);
        redisCacheService.removeAgentSession(agentId);

        log.info("Agent [{}] fully cleaned up from system", agentId);
    }
}
