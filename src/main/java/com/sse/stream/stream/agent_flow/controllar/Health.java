package com.sse.stream.stream.agent_flow.controllar;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sse.stream.stream.agent_flow.services.RedisCacheService;

@RestController
@RequestMapping("/")
public class Health {

    private final RedisCacheService redisCacheService;

    public Health(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
    @GetMapping("/agents/active")
    public ResponseEntity<Map<String, ?>> getAllLiveAgentsDetails() {
    Set<String> agentIds = redisCacheService.getAllActiveAgents();
    return ResponseEntity.ok(Map.of(
        "totalLiveAgents", agentIds.size(),
        "agents", agentIds,
        "timestamp", System.currentTimeMillis()
    ));
}
}
