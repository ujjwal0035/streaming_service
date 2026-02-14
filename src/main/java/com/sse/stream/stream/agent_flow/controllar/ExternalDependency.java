package com.sse.stream.stream.agent_flow.controllar;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sse.stream.stream.agent_flow.dto.InboundCallbackPaylaod;
import com.sse.stream.stream.agent_flow.repository.BorrowerRepository;
import com.sse.stream.stream.agent_flow.services.BorrowerService;
import com.sse.stream.stream.utils.ApiResponse;
import com.sse.stream.stream.utils.ResponseUtil;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/v1/external")
public class ExternalDependency {

    private final BorrowerService borrowerService;

    ExternalDependency(BorrowerService borrowerService, BorrowerRepository borrowerRepository) {
        this.borrowerService = borrowerService;
    }
    
    @PostMapping("/inbound-data")
    public ApiResponse<String> inboundCallback(@RequestBody InboundCallbackPaylaod data) {
        try{
            String result = borrowerService.processCallback(data.getBorrowerId(), data.getBatchNo(), data.getLenderId(), data.getAgentId());
            if(result == "NOT_FOUND") {
                return ResponseUtil.error("No matching records found for the provided data");
            }
            return ResponseUtil.success(result, "Data processed successfully");
        }catch (Exception e) {
            e.printStackTrace();
            // Log the error for tracking
            System.err.println("Error in processCallback: " + e.getMessage());
            return ResponseUtil.error("Processing failed: " + e.getMessage());
        }
    }
}
