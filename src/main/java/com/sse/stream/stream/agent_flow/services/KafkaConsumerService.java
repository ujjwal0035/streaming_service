package com.sse.stream.stream.agent_flow.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private SseRegistry sseRegistry;

    @Autowired
    private BorrowerService borrowerService;

    /**
     * Listen to callback-events topic
     * This is the main event handler for incoming callbacks
     */
    @KafkaListener(topics = "callback-events", groupId = "stream-consumer-group")
    public void consumeCallbackEvent(String message) {
        try {
            log.info("Consuming callback event: {}", message);
            JsonNode eventNode = objectMapper.readTree(message);

            String agentId = eventNode.get("agentId").asText();
            String borrowerId = eventNode.get("borrowerId").asText();
            String batchNo = eventNode.get("batchNo").asText();
            String lenderId = eventNode.get("lenderId").asText();
            JsonNode data = eventNode.get("data");

            log.info("Callback received - Agent [{}], Borrower [{}], Batch [{}], Lender [{}]", 
                agentId, borrowerId, batchNo, lenderId);

            // Check if agent is online in Redis
            boolean isAgentOnline = redisCacheService.isAgentOnline(agentId);
            String agentPod = redisCacheService.getAgentPod(agentId);

            if (isAgentOnline) {
                log.info("Agent [{}] is online on pod [{}]. Processing callback...", agentId, agentPod);
                
                // Process the callback through the database
                String result = borrowerService.processCallback(borrowerId, batchNo, lenderId, agentId);
                
                if ("SUCCESS".equals(result)) {
                    log.info("Callback processing successful for agent [{}]", agentId);
                } else {
                    log.warn("Callback processing returned: {} for agent [{}]", result, agentId);
                    // Send error event to agent
                    sseRegistry.sendToUser(agentId, 
                        "{\"type\": \"callback_error\", \"message\": \"Borrower data not found\", \"borrowerId\": \"" + borrowerId + "\"}");
                }
            } else {
                log.warn("Agent [{}] is offline. Callback cannot be delivered at this moment.", agentId);
                // In production, you might want to queue this or send to a dead-letter topic
                // For now, we just log it
            }

        } catch (Exception e) {
            log.error("Error processing callback event: {}", message, e);
        }
    }

    /**
     * Listen to borrower-events topic
     */
    @KafkaListener(topics = "borrower-events", groupId = "stream-consumer-group")
    public void consumeBorrowerEvent(String message) {
        try {
            log.info("Consuming borrower event: {}", message);
            JsonNode eventNode = objectMapper.readTree(message);

            String borrowerId = eventNode.get("borrowerId").asText();
            String eventType = eventNode.get("eventType").asText();
            JsonNode data = eventNode.get("data");

            log.info("Borrower [{}] event [{}]: {}", borrowerId, eventType, data);

            // Process event based on type
            switch (eventType) {
                case "CREATED":
                    handleBorrowerCreated(borrowerId, data);
                    break;
                case "UPDATED":
                    handleBorrowerUpdated(borrowerId, data);
                    break;
                case "DELETED":
                    handleBorrowerDeleted(borrowerId, data);
                    break;
                default:
                    log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing borrower event: {}", message, e);
        }
    }

    /**
     * Listen to sse-events topic
     */
    @KafkaListener(topics = "sse-events", groupId = "stream-consumer-group")
    public void consumeSseEvent(String message) {
        try {
            log.info("Consuming SSE event: {}", message);
            JsonNode eventNode = objectMapper.readTree(message);

            String clientId = eventNode.get("clientId").asText();
            String eventType = eventNode.get("eventType").asText();
            JsonNode data = eventNode.get("data");

            log.info("SSE Client [{}] event [{}]: {}", clientId, eventType, data);

            // Process event based on type
            switch (eventType) {
                case "CONNECTED":
                    handleClientConnected(clientId, data);
                    break;
                case "DISCONNECTED":
                    handleClientDisconnected(clientId, data);
                    break;
                case "MESSAGE":
                    handleClientMessage(clientId, data);
                    break;
                default:
                    log.warn("Unknown SSE event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing SSE event: {}", message, e);
        }
    }

    /**
     * Listen to generic test-topic for testing
     */
    @KafkaListener(topics = "test-topic", groupId = "stream-consumer-group")
    public void consumeTestMessage(String message) {
        log.info("Test message received: {}", message);
    }

    // Event handlers - implement your business logic here

    private void handleBorrowerCreated(String borrowerId, JsonNode data) {
        log.info("Handling borrower creation for ID: {}", borrowerId);
        // TODO: Implement your logic
    }

    private void handleBorrowerUpdated(String borrowerId, JsonNode data) {
        log.info("Handling borrower update for ID: {}", borrowerId);
        // TODO: Implement your logic
    }

    private void handleBorrowerDeleted(String borrowerId, JsonNode data) {
        log.info("Handling borrower deletion for ID: {}", borrowerId);
        // TODO: Implement your logic
    }

    private void handleClientConnected(String clientId, JsonNode data) {
        log.info("Handling client connection for ID: {}", clientId);
        // TODO: Implement your logic
    }

    private void handleClientDisconnected(String clientId, JsonNode data) {
        log.info("Handling client disconnection for ID: {}", clientId);
        // TODO: Implement your logic
    }

    private void handleClientMessage(String clientId, JsonNode data) {
        log.info("Handling client message for ID: {}", clientId);
        // TODO: Implement your logic
    }
}