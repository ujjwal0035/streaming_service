package com.sse.stream.stream.agent_flow.controllar;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sse.stream.stream.agent_flow.dto.InboundCallbackPaylaod;
import com.sse.stream.stream.agent_flow.repository.BorrowerRepository;
import com.sse.stream.stream.agent_flow.services.BorrowerService;
import com.sse.stream.stream.agent_flow.services.KafkaProducerService;
import com.sse.stream.stream.agent_flow.services.RedisCacheService;
import com.sse.stream.stream.utils.ApiResponse;
import com.sse.stream.stream.utils.ResponseUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;


@Slf4j
@RestController
@RequestMapping("/api/v1/external")
public class ExternalDependency {

    private final BorrowerService borrowerService;
    private final KafkaProducerService kafkaProducerService;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    public ExternalDependency(
            BorrowerService borrowerService, 
            BorrowerRepository borrowerRepository,
            KafkaProducerService kafkaProducerService,
            RedisCacheService redisCacheService,
            ObjectMapper objectMapper) {
        this.borrowerService = borrowerService;
        this.kafkaProducerService = kafkaProducerService;
        this.redisCacheService = redisCacheService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Receive inbound callback and publish to Kafka for processing
     * This enables multi-pod delivery and agent status checking before processing
     */
    @PostMapping("/inbound-data")
    public ApiResponse<String> inboundCallback(@RequestBody InboundCallbackPaylaod data) {
        try {
            log.info("Inbound callback received - AgentId: {}, BorrowerId: {}, BatchNo: {}, LenderId: {}",
                data.getAgentId(), data.getBorrowerId(), data.getBatchNo(), data.getLenderId());

            // Validate payload
            if (data.getAgentId() == null || data.getAgentId().isEmpty()) {
                log.warn("Invalid callback: agentId is missing");
                return ResponseUtil.error("agentId is required");
            }

            if (data.getBorrowerId() == null || data.getBorrowerId().isEmpty()) {
                log.warn("Invalid callback: borrowerId is missing");
                return ResponseUtil.error("borrowerId is required");
            }

            // Check if agent is online
            boolean isAgentOnline = redisCacheService.isAgentOnline(data.getAgentId());
            String agentPod = redisCacheService.getAgentPod(data.getAgentId());

            if (!isAgentOnline) {
                log.warn("Agent [{}] is offline. Callback cannot be delivered at this moment.", data.getAgentId());
                return ResponseUtil.error("Agent is not online. Callback queued for later delivery.");
            }

            log.info("Agent [{}] is online on pod [{}]. Publishing callback to Kafka...", data.getAgentId(), agentPod);

            // Convert payload to JSON string for Kafka
            String eventData = objectMapper.writeValueAsString(data);

            // Publish callback event to Kafka for processing
            // Using agentId as key ensures all messages for same agent go to same partition
            kafkaProducerService.sendCallbackEvent(
                data.getAgentId(),
                data.getBorrowerId(),
                data.getBatchNo(),
                data.getLenderId(),
                eventData
            );

            log.info("Callback event published to Kafka for agent [{}]", data.getAgentId());
            return ResponseUtil.success("QUEUED", "Callback received and queued for processing");

        } catch (Exception e) {
            log.error("Error processing callback", e);
            return ResponseUtil.error("Processing failed: " + e.getMessage());
        }
    }
}