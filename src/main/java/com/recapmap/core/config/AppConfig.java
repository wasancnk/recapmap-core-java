package com.recapmap.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    @Autowired
    private Environment env;
    
    private String openAiVisionModel = "gpt-4o";
    private String testImagePath = "D:/workspace_recapmap/core-suite-server-files/test-image.png"; // Updated path
    // Add more config fields as needed
    private int simpleExtractionThreads = 6; // Default to 4 threads for safety, can be adjusted
    private String n8nWebhookUrl = "https://wasanch.app.n8n.cloud/webhook/e80476e1-dedc-4c8f-8da7-abd7d0a4a1e3"; // Placeholder for n8n webhook URL
    private String n8nWebhookUsername = "core_n8n_user_test";
    private String n8nWebhookPassword = "68aa35if-81zf-4arb-yc1c-f974a9acfd95";    
      public String getOpenAiApiKey() {
        String apiKey = env.getProperty("openai.api.key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured. Please check your application-local.properties file.");
        }
        return apiKey;
    }

    public String getOpenAiVisionModel() {
        return openAiVisionModel;
    }
    public void setOpenAiVisionModel(String openAiVisionModel) {
        this.openAiVisionModel = openAiVisionModel;
    }

    public String getTestImagePath() {
        return testImagePath;
    }
    public void setTestImagePath(String testImagePath) {
        this.testImagePath = testImagePath;
    }

    public int getSimpleExtractionThreads() {
        return simpleExtractionThreads;
    }
    public void setSimpleExtractionThreads(int simpleExtractionThreads) {
        if (simpleExtractionThreads > 0 && simpleExtractionThreads <= 32) {
            this.simpleExtractionThreads = simpleExtractionThreads;
        }
    }

    public String getN8nWebhookUrl() {
        return n8nWebhookUrl;
    }

    public void setN8nWebhookUrl(String n8nWebhookUrl) {
        this.n8nWebhookUrl = n8nWebhookUrl;
    }

    public String getN8nWebhookUsername() {
        return n8nWebhookUsername;
    }

    public void setN8nWebhookUsername(String n8nWebhookUsername) {
        this.n8nWebhookUsername = n8nWebhookUsername;
    }

    public String getN8nWebhookPassword() {
        return n8nWebhookPassword;
    }

    public void setN8nWebhookPassword(String n8nWebhookPassword) {
        this.n8nWebhookPassword = n8nWebhookPassword;
    }

    // Add getters/setters for other config values as needed
}
