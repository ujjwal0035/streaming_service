package com.sse.stream.stream.agent_flow.controllar;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sse.stream.stream.agent_flow.services.SseRegistry;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api/v1/stream")
public class SSEStream {
    private final SseRegistry sseRegistry;
    
    public SSEStream(SseRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String userId) {
        // 0L means no timeout, which is common for high-concurrency SSE
        SseEmitter emitter = new SseEmitter(0L); 
        sseRegistry.addEmitter(userId, emitter);

        sseRegistry.initiateConnection(userId);
        
        return emitter;
    }
    
}
