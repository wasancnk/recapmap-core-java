package com.recapmap.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.recapmap.core.service.MarkdownService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ApiDocumentationController {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    @Autowired
    private MarkdownService markdownService;
    
    @Autowired
    private ResourceLoader resourceLoader;

    @GetMapping("/endpoints")
    public ResponseEntity<?> getApiEndpoints(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        List<Map<String, Object>> endpoints = new ArrayList<>();

        // Get all mapped endpoints
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = 
            requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Only include API endpoints
            if (mappingInfo.getPatternsCondition() != null && 
                mappingInfo.getPatternsCondition().getPatterns().stream()
                    .anyMatch(pattern -> pattern.startsWith("/api/"))) {                // Get HTTP methods
                Set<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
                if (methods.isEmpty()) {
                    methods.add("GET"); // Default
                }
                
                // Get paths
                Set<String> patterns = mappingInfo.getPatternsCondition().getPatterns();
                
                for (String method : methods) {
                    for (String pattern : patterns) {
                        Map<String, Object> endpoint = new HashMap<>();
                        endpoint.put("method", method);
                        endpoint.put("path", pattern);
                        endpoint.put("description", getEndpointDescription(pattern, method));
                        endpoint.put("controller", handlerMethod.getBeanType().getSimpleName());
                        endpoint.put("methodName", handlerMethod.getMethod().getName());
                        endpoint.put("parameters", getParameterInfo(handlerMethod));
                        endpoint.put("responses", getResponseExamples(pattern, method));
                        endpoint.put("example", getRequestExample(pattern, method));
                        
                        endpoints.add(endpoint);
                    }
                }
            }
        }

        // Sort endpoints by path
        endpoints.sort((a, b) -> {
            String pathA = (String) a.get("path");
            String pathB = (String) b.get("path");
            return pathA.compareTo(pathB);
        });

        return ResponseEntity.ok(endpoints);
    }

    @GetMapping("/markdown/{filename}")
    public ResponseEntity<Map<String, Object>> getMarkdownContent(
            @PathVariable String filename,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        try {
            // Secure path validation
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid filename"));
            }

            Resource resource = resourceLoader.getResource("classpath:kb/" + filename + ".md");
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String markdownContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String htmlContent = markdownService.convertToHtml(markdownContent);

            Map<String, Object> response = new HashMap<>();
            response.put("filename", filename);
            response.put("markdown", markdownContent);
            response.put("html", htmlContent);
            response.put("lastModified", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listAvailableDocuments(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }        try {
            // This is a simplified list - in production, you'd scan the directory
            List<Map<String, Object>> documents = Arrays.asList(
                Map.of("filename", "Project-Vision-Overview", "title", "Project Vision Overview"),
                Map.of("filename", "Technical-Architecture-Decisions", "title", "Technical Architecture"),
                Map.of("filename", "Development-Phases-Roadmap", "title", "Development Roadmap"),
                Map.of("filename", "Installation-Guide", "title", "Installation Guide"),
                Map.of("filename", "API-Documentation", "title", "API Documentation")
            );

            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to list documents"));
        }
    }    private String getEndpointDescription(String pattern, String method) {
        // Provide descriptions for known endpoints
        Map<String, String> descriptions = new HashMap<>();
        
        // Authentication endpoints
        descriptions.put("GET:/api/userinfo", "Get current user information and roles");
        descriptions.put("POST:/api/auth/login", "Authenticate user and receive access token");
        descriptions.put("POST:/api/auth/logout", "Logout current user and invalidate token");
        
        // Documentation endpoints
        descriptions.put("GET:/api/docs/endpoints", "Get live API documentation with all endpoints");
        descriptions.put("GET:/api/docs/markdown/{filename}", "Retrieve markdown documentation content");
        descriptions.put("GET:/api/docs/list", "List all available documentation files");
        
        // Test endpoints
        descriptions.put("GET:/api/test/hello", "Simple hello world test endpoint");
        descriptions.put("GET:/api/test/auth-test", "Test authentication status and user details");
        descriptions.put("POST:/api/test/echo", "Echo back the request payload for testing");
        descriptions.put("GET:/api/test-vision-hello", "Test OpenAI Vision service integration");
        
        // File processing endpoints
        descriptions.put("POST:/file/upload", "Upload and process PDF files");
        descriptions.put("GET:/file/sessions", "Get all active file processing sessions");
        descriptions.put("POST:/file/cmd/cleanup-upload", "Admin: Clean up uploaded files");
        descriptions.put("POST:/file/cmd/update-config", "Admin: Update system configuration");
        
        // AI/Chat endpoints
        descriptions.put("POST:/api/chat/send", "Send message to AI chat system");

        String key = method + ":" + pattern;
        return descriptions.getOrDefault(key, "API endpoint - " + method + " " + pattern);
    }

    private List<Map<String, Object>> getParameterInfo(HandlerMethod handlerMethod) {
        List<Map<String, Object>> parameters = new ArrayList<>();
        
        // This is a simplified parameter extraction
        // In production, you'd use reflection to get actual parameter info
        String methodName = handlerMethod.getMethod().getName();
        
        if (methodName.equals("login")) {
            parameters.add(Map.of(
                "name", "username",
                "type", "string",
                "required", true,
                "description", "User login name"
            ));
            parameters.add(Map.of(
                "name", "password", 
                "type", "string",
                "required", true,
                "description", "User password"
            ));
        }
        
        return parameters;
    }

    private Map<String, Object> getResponseExamples(String pattern, String method) {
        Map<String, Object> examples = new HashMap<>();
        
        if (pattern.equals("/api/auth/login") && method.equals("POST")) {
            examples.put("200", Map.of(
                "success", true,
                "message", "Login successful",
                "user", "admin",
                "roles", Arrays.asList("ROLE_ADMIN"),
                "token", "uuid-token-here"
            ));
            examples.put("401", Map.of(
                "success", false,
                "message", "Invalid username or password"
            ));
        } else if (pattern.equals("/api/userinfo") && method.equals("GET")) {
            examples.put("200", Map.of(
                "username", "admin",
                "roles", Arrays.asList("ROLE_ADMIN")
            ));
        }
        
        return examples;
    }

    private Map<String, Object> getRequestExample(String pattern, String method) {
        if (pattern.equals("/api/auth/login") && method.equals("POST")) {
            return Map.of(
                "username", "admin",
                "password", "your-password-here"
            );
        }
        return null;
    }
}
