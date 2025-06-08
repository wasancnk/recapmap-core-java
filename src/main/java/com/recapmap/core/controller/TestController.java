package com.recapmap.core.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> helloWorld() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from RecapMap API!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth-test")
    public ResponseEntity<Map<String, Object>> authTest(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null) {
            response.put("authenticated", true);
            response.put("user", authentication.getName());
            response.put("authorities", authentication.getAuthorities().toString());
        } else {
            response.put("authenticated", false);
            response.put("message", "No authentication found");
        }
        
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        response.put("echo", payload);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Echo successful");
        return ResponseEntity.ok(response);
    }
}
