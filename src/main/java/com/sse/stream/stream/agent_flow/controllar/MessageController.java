package com.sse.stream.stream.agent_flow.controllar;

import com.sse.stream.stream.agent_flow.services.KafkaProducerService;
import com.sse.stream.stream.agent_flow.services.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RedisCacheService redisCacheService;

    /**
     * Send a test message to Kafka
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestParam String topic, @RequestParam String message) {
        try {
            kafkaProducerService.sendMessage(topic, message);
            return ResponseEntity.ok("Message sent successfully to topic: " + topic);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Send a borrower event to Kafka
     */
    @PostMapping("/borrower-event")
    public ResponseEntity<String> sendBorrowerEvent(
            @RequestParam String borrowerId,
            @RequestParam String eventType,
            @RequestParam String eventData) {
        try {
            kafkaProducerService.sendBorrowerEvent(borrowerId, eventType, eventData);
            return ResponseEntity.ok("Borrower event sent successfully");
        } catch (Exception e) {
            log.error("Error sending borrower event", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Send a SSE event to Kafka
     */
    @PostMapping("/sse-event")
    public ResponseEntity<String> sendSseEvent(
            @RequestParam String clientId,
            @RequestParam String eventType,
            @RequestParam String eventData) {
        try {
            kafkaProducerService.sendSseEvent(clientId, eventType, eventData);
            return ResponseEntity.ok("SSE event sent successfully");
        } catch (Exception e) {
            log.error("Error sending SSE event", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Cache data in Redis
     */
    @PostMapping("/cache")
    public ResponseEntity<String> cacheData(
            @RequestParam String key,
            @RequestParam String value) {
        try {
            redisCacheService.set(key, value);
            return ResponseEntity.ok("Data cached successfully");
        } catch (Exception e) {
            log.error("Error caching data", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get data from Redis cache
     */
    @GetMapping("/cache/{key}")
    public ResponseEntity<String> getCachedData(@PathVariable String key) {
        try {
            String value = redisCacheService.get(key);
            if (value != null) {
                return ResponseEntity.ok(value);
            } else {
                return ResponseEntity.status(404).body("Key not found in cache");
            }
        } catch (Exception e) {
            log.error("Error retrieving cached data", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Cache borrower data
     */
    @PostMapping("/cache/borrower")
    public ResponseEntity<String> cacheBorrower(
            @RequestParam String borrowerId,
            @RequestParam String borrowerData) {
        try {
            redisCacheService.cacheBorrower(borrowerId, borrowerData);
            return ResponseEntity.ok("Borrower data cached successfully");
        } catch (Exception e) {
            log.error("Error caching borrower data", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get cached borrower data
     */
    @GetMapping("/cache/borrower/{borrowerId}")
    public ResponseEntity<String> getCachedBorrower(@PathVariable String borrowerId) {
        try {
            String cachedData = redisCacheService.getCachedBorrower(borrowerId);
            if (cachedData != null) {
                return ResponseEntity.ok(cachedData);
            } else {
                return ResponseEntity.status(404).body("Borrower not found in cache");
            }
        } catch (Exception e) {
            log.error("Error retrieving cached borrower", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get active connection count
     */
    @GetMapping("/metrics/connections")
    public ResponseEntity<Long> getActiveConnections() {
        try {
            long count = redisCacheService.getActiveConnectionCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error retrieving connection count", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
