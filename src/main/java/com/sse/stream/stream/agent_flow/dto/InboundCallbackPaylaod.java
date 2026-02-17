package com.sse.stream.stream.agent_flow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundCallbackPaylaod {
    private String agentId;
    private String borrowerId;
    private String batchNo;
    private String lenderId;
}