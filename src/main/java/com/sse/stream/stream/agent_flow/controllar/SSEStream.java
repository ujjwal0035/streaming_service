package com.sse.stream.stream.agent_flow.controllar;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sse.stream.stream.agent_flow.services.RedisCacheService;
import com.sse.stream.stream.agent_flow.services.SseRegistry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Slf4j
@RestController
@RequestMapping("/api/v1/stream")
public class SSEStream {
    private final SseRegistry sseRegistry;
    private final RedisCacheService redisCacheService;
    
    @Value("${app.pod-name:pod-default}")
    private String podName;
    
    @Autowired
    public SSEStream(SseRegistry sseRegistry, RedisCacheService redisCacheService) {
        this.sseRegistry = sseRegistry;
        this.redisCacheService = redisCacheService;
    }

    /**
     * Agent subscribes to SSE stream
     * Registers agent in Redis and stores connection
     */
    @GetMapping(value = "/subscribe/{agentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String agentId) {
        log.info("Agent [{}] subscribing to SSE stream on pod [{}]", agentId, podName);
        
        // 0L means no timeout, which is common for high-concurrency SSE
        SseEmitter emitter = new SseEmitter(0L); 
        
        // Store emitter in registry
        sseRegistry.addEmitter(agentId, emitter);
        
        // Register agent session in Redis with pod information
        redisCacheService.registerAgentSession(agentId, podName);
        
        // Send initial connection event
        sseRegistry.initiateConnection(agentId);
        
        // Handle cleanup when connection is closed
        setupEmitterCleanup(agentId, emitter);
        
        return emitter;
    }

    /**
     * Agent disconnects from SSE stream
     */
    @DeleteMapping("/disconnect/{agentId}")
    public void disconnect(@PathVariable String agentId) {
        log.info("Agent [{}] disconnecting from SSE stream", agentId);
        
        // Remove from Redis
        redisCacheService.removeAgentSession(agentId);
        
        // Remove from registry
        sseRegistry.removeEmitter(agentId);
    }

    /**
     * Check if agent is online
     */
    @GetMapping("/status/{agentId}")
    public String checkAgentStatus(@PathVariable String agentId) {
        boolean isOnline = redisCacheService.isAgentOnline(agentId);
        String pod = redisCacheService.getAgentPod(agentId);
        
        if (isOnline) {
            log.info("Agent [{}] is online on pod [{}]", agentId, pod);
            return "Agent " + agentId + " is online on pod " + pod;
        } else {
            log.info("Agent [{}] is offline", agentId);
            return "Agent " + agentId + " is offline";
        }
    }

    /**
     * Setup cleanup hooks for SSE emitter
     */
    private void setupEmitterCleanup(String agentId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for agent [{}]", agentId);
            redisCacheService.removeAgentSession(agentId);
            sseRegistry.removeEmitter(agentId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for agent [{}]", agentId);
            redisCacheService.removeAgentSession(agentId);
            sseRegistry.removeEmitter(agentId);
        });
        
        emitter.onError((e) -> {
            log.error("SSE connection error for agent [{}]: {}", agentId, e.getMessage());
            redisCacheService.removeAgentSession(agentId);
            sseRegistry.removeEmitter(agentId);
        });
    }
}