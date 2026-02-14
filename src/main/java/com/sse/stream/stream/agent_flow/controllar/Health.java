package com.sse.stream.stream.agent_flow.controllar;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class Health {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
