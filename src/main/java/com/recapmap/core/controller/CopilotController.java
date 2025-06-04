package com.recapmap.core.controller;

import com.recapmap.core.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference; // Jackson import
import com.fasterxml.jackson.databind.ObjectMapper; // Jackson import

import java.util.Base64; // Import for Basic Auth
import java.util.HashMap;
import java.util.Map;

@Controller
public class CopilotController {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RestTemplate restTemplate; // Autowire RestTemplate

    @GetMapping("/copilot")
    public String copilot() {
        return "copilot"; // Assuming you have a copilot.html or similar view
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendChatMessage(@RequestBody ChatMessageRequest request) {
        String n8nWebhookUrl = appConfig.getN8nWebhookUrl();
        String username = appConfig.getN8nWebhookUsername();
        String password = appConfig.getN8nWebhookPassword();

        if (n8nWebhookUrl == null || n8nWebhookUrl.equals("YOUR_N8N_WEBHOOK_URL_HERE") || n8nWebhookUrl.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("reply", "n8n webhook URL is not configured in the backend.");
            return ResponseEntity.status(500).body(errorResponse);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add Basic Authentication header if username and password are provided
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }

        Map<String, String> n8nRequestPayload = new HashMap<>();
        n8nRequestPayload.put("message", request.getMessage());
        // n8nRequestPayload.put("conversationId", request.getConversationId()); // Removed conversationId

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(n8nRequestPayload, headers);

        try {
            // 1. Get the response as a String first
            ResponseEntity<String> n8nRawResponse = restTemplate.exchange(
                n8nWebhookUrl,
                HttpMethod.POST,
                entity,
                String.class // Expect a String response
            );

            String rawBody = n8nRawResponse.getBody();
            System.out.println("Raw response from n8n: " + rawBody); 

            Map<String, Object> finalResponseToFrontend = new HashMap<>();
            ObjectMapper objectMapper = new ObjectMapper(); // ObjectMapper for JSON parsing

            if (rawBody != null && !rawBody.trim().isEmpty()) {
                try {
                    // Attempt to parse the n8n response
                    Map<String, Object> parsedN8nResponse = objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
                    
                    // Check for the "output" field and if it's a String
                    Object outputValue = parsedN8nResponse.get("output");
                    if (outputValue instanceof String) {
                        finalResponseToFrontend.put("reply", (String) outputValue);
                    } else {
                        String errorMsg;
                        if (!parsedN8nResponse.containsKey("output")) { // Check if key exists first
                            errorMsg = "AI assistant response is missing the 'output' field. Raw: " + rawBody;
                        } else if (outputValue == null) {
                            errorMsg = "AI assistant 'output' field is null. Raw: " + rawBody;
                        } else {
                            errorMsg = "AI assistant 'output' field is not a String. Type: " + outputValue.getClass().getName() + ". Raw: " + rawBody;
                        }
                        finalResponseToFrontend.put("reply", errorMsg);
                        System.err.println(errorMsg);
                    }
                } catch (Exception e) { // Catch exceptions during parsing (e.g., malformed JSON)
                    String errorMsg = "Error parsing n8n JSON response: " + e.getMessage() + ". Raw: " + rawBody;
                    finalResponseToFrontend.put("reply", errorMsg);
                    System.err.println(errorMsg);
                }
            } else { // rawBody is null or empty
                String errorMsg = "Received an empty or null response from the AI assistant.";
                if (rawBody != null) { // Append rawBody if it's empty but not null
                     errorMsg += " Raw: " + rawBody;
                }
                finalResponseToFrontend.put("reply", errorMsg);
                System.err.println(errorMsg);
            }

            return ResponseEntity.ok(finalResponseToFrontend);

        } catch (Exception e) {
            System.err.println("Error calling n8n webhook: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("reply", "Error communicating with the AI assistant: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Inner class to represent the request body from app.js
    public static class ChatMessageRequest {
        private String message;
        // private String conversationId; // Removed conversationId

        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        // public String getConversationId() { // Removed conversationId
        //     return conversationId;
        // }
        // public void setConversationId(String conversationId) { // Removed conversationId
        //     this.conversationId = conversationId;
        // }
    }
}
