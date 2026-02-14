package com.sse.stream.stream.agent_flow.dto;

public class ResponseFormat {
    private String status;
    private String message;
    private Object data;

    public ResponseFormat(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
