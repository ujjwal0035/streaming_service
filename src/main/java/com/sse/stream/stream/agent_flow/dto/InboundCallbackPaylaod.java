package com.sse.stream.stream.agent_flow.dto;

public class InboundCallbackPaylaod {
    private String agentId;
    private String borrowerId;
    private String batchNo;
    private String lenderId;

    public String getAgentId() {
        return agentId;
    }
    
    public String getBorrowerId() {
        return borrowerId;
    }
    
    public String getBatchNo() {
        return batchNo;
    }
    
    public String getLenderId() {
        return lenderId;
    }
    
}
