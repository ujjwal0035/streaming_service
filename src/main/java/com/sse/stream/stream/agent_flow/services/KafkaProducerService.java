package com.sse.stream.stream.agent_flow.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Send a message to Kafka topic
     */
    public void sendMessage(String topic, String message) {
        try {
            kafkaTemplate.send(topic, message);
            log.info("Message sent to topic [{}]: {}", topic, message);
        } catch (Exception e) {
            log.error("Failed to send message to topic [{}]", topic, e);
        }
    }

    /**
     * Send a message with key to Kafka topic
     */
    public void sendMessageWithKey(String topic, String key, String message) {
        try {
            kafkaTemplate.send(topic, key, message);
            log.info("Message sent to topic [{}] with key [{}]: {}", topic, key, message);
        } catch (Exception e) {
            log.error("Failed to send message to topic [{}] with key [{}]", topic, key, e);
        }
    }

    /**
     * Send a message with headers and key to Kafka topic
     */
    public void sendMessageWithKeyAndHeaders(String topic, String key, String message, String headerKey, String headerValue) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
            record.headers().add(headerKey, headerValue.getBytes());
            kafkaTemplate.send(record);
            log.info("Message sent to topic [{}] with key [{}] and headers", topic, key);
        } catch (Exception e) {
            log.error("Failed to send message to topic [{}]", topic, e);
        }
    }

    /**
     * Send borrower event to Kafka
     */
    public void sendBorrowerEvent(String borrowerId, String eventType, String eventData) {
        String topic = "borrower-events";
        String message = String.format("{\"borrowerId\": \"%s\", \"eventType\": \"%s\", \"data\": %s, \"timestamp\": %d}",
                borrowerId, eventType, eventData, System.currentTimeMillis());
        sendMessageWithKey(topic, borrowerId, message);
    }

    /**
     * Send SSE event to Kafka
     */
    public void sendSseEvent(String clientId, String eventType, String eventData) {
        String topic = "sse-events";
        String message = String.format("{\"clientId\": \"%s\", \"eventType\": \"%s\", \"data\": %s, \"timestamp\": %d}",
                clientId, eventType, eventData, System.currentTimeMillis());
        sendMessageWithKey(topic, clientId, message);
    }

    /**
     * Send callback event to Kafka for processing
     * Key: agentId to ensure all messages for same agent go to same partition
     */
    public void sendCallbackEvent(String agentId, String borrowerId, String batchNo, String lenderId, String eventData) {
        String topic = "callback-events";
        String message = String.format(
            "{\"agentId\": \"%s\", \"borrowerId\": \"%s\", \"batchNo\": \"%s\", \"lenderId\": \"%s\", \"data\": %s, \"timestamp\": %d}",
            agentId, borrowerId, batchNo, lenderId, eventData, System.currentTimeMillis()
        );
        // Use agentId as key to ensure messages for same agent go to same partition
        sendMessageWithKey(topic, agentId, message);
        log.info("Callback event sent for agent [{}], borrower [{}]", agentId, borrowerId);
    }
}