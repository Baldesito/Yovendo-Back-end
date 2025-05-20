package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<?> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "online");
        response.put("message", "Yovendo-AI API Server");
        response.put("timestamp", new Date().toString());
        response.put("endpoints", Map.of(
                "auth", "/api/auth",
                "health", "/api/health"
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }
}