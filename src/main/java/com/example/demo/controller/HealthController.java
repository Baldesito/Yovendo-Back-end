package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", new Date().toString());
        response.put("service", "Yovendo-AI API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/")
    public ResponseEntity<?> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Yovendo-AI API Server");
        response.put("status", "running");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }
}