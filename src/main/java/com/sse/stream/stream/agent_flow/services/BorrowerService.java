package com.sse.stream.stream.agent_flow.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sse.stream.stream.agent_flow.entity.BorrowerDetails;
import com.sse.stream.stream.agent_flow.repository.BorrowerRepository;

@Service
public class BorrowerService {
    private final BorrowerRepository repository;
    private final SseRegistry sseRegistry;

    public BorrowerService(BorrowerRepository repository, SseRegistry sseRegistry) {
        this.repository = repository;
        this.sseRegistry = sseRegistry;
    }

    public String processCallback(String borrowerId, String batchNo, String lenderId, String agentId) {
        try{

            List<BorrowerDetails> records = repository.findByLenderIdAndAllocationIdAndSpoctoId(lenderId, batchNo, borrowerId);

            if (!records.isEmpty()) {
                // Push the result to the UI user
                sseRegistry.sendToUser(agentId, records.get(records.size() - 1));
                return "SUCCESS";
            } 
            return "NOT_FOUND";
        }catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID format: " + e.getMessage());
            throw new RuntimeException("Invalid batchNo format, must be a valid UUID: " + e.getMessage(), e);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing failed: " + e.getMessage(), e);
        }
    }
}
