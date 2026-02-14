package com.sse.stream.stream.agent_flow.controllar;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sse.stream.stream.agent_flow.dto.InboundCallbackPaylaod;
import com.sse.stream.stream.utils.ApiResponse;
import com.sse.stream.stream.utils.ResponseUtil;
import com.sse.stream.stream.utils.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/v1/external")
public class ExternalDependency {
    
    @PostMapping("/inbound-data")
    public ApiResponse<String> inboundCallback(@RequestBody InboundCallbackPaylaod data) {
        // validate the incoming data (basic validation, can be extended as needed)
        // For demonstration, we just print the received data and return a success message.
        System.out.println("Received data from external system:");
        System.out.println("Agent ID: " + data.getAgentId());
        System.out.println("Borrower ID: " + data.getBorrowerId());
        System.out.println("Batch No: " + data.getBatchNo());
        System.out.println("Lender ID: " + data.getLenderId());
        
        // In a real application, you would process the data and possibly trigger events or update the database.
        
        return ResponseUtil.success("", "Inbound data processed successfully");
    }
}
