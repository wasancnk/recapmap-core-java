package com.recapmap.core.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @GetMapping({"", "/", "/dashboard", "/api-docs", "/login"})
    public ResponseEntity<String> serveReactApp() throws IOException {
        // Serve the React app's index.html for all admin routes
        Resource resource = new ClassPathResource("static/index.html");
        if (resource.exists()) {
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
