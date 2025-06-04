package com.recapmap.core.service;

import com.recapmap.core.config.AppConfig;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.nio.file.Files;
import java.util.*;


@Service
public class OpenAiVisionService {
    @Autowired
    private AppConfig appConfig;

    private static final String OPENAI_VISION_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();

    public String extractFromImage(File imageFile, String prompt) {
        // This method sends an image and prompt to OpenAI Vision API and returns the response as a string.
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(appConfig.getOpenAiApiKey());

            // Read image as base64 and build data URL
            String mimeType = Files.probeContentType(imageFile.toPath());
            if (mimeType == null) mimeType = "image/png"; // fallback
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64;

            // Build content array with correct structure
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            content.add(textContent);

            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            Map<String, Object> imageUrlObj = new HashMap<>();
            imageUrlObj.put("url", dataUrl);
            imageContent.put("image_url", imageUrlObj);
            content.add(imageContent);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", content);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", appConfig.getOpenAiVisionModel());
            requestBody.put("messages", Arrays.asList(userMessage));
            requestBody.put("max_tokens", 4096);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_VISION_ENDPOINT, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Hello world test: Send the configured test image and a simple prompt to OpenAI Vision API.
     * @return The response from OpenAI API as a String.
     */
    public String helloWorldTest() {
        String prompt = "What is this picture about?";
        File imageFile = new File(appConfig.getTestImagePath());
        return extractFromImage(imageFile, prompt);
    }

    /**
     * Extracts information from multiple images using a single OpenAI Vision API call.
     * @param imageFiles List of image files to include in the prompt window.
     * @param prompt The prompt to send to the model.
     * @param includeImage Whether to include images in the content (for future flexibility).
     * @return The response from OpenAI API as a String.
     */
    public String extractFromImages(List<File> imageFiles, String prompt, boolean includeImage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(appConfig.getOpenAiApiKey());

            // Build content array: prompt text first, then all images
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            content.add(textContent);

            if (includeImage) {
                for (File imageFile : imageFiles) {
                    String mimeType = Files.probeContentType(imageFile.toPath());
                    if (mimeType == null) mimeType = "image/png";
                    byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    String dataUrl = "data:" + mimeType + ";base64," + base64;

                    Map<String, Object> imageContent = new HashMap<>();
                    imageContent.put("type", "image_url");
                    Map<String, Object> imageUrlObj = new HashMap<>();
                    imageUrlObj.put("url", dataUrl);
                    imageContent.put("image_url", imageUrlObj);
                    content.add(imageContent);
                }
            }

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", content);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", appConfig.getOpenAiVisionModel());
            requestBody.put("messages", Arrays.asList(userMessage));
            requestBody.put("max_tokens", 4096);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_VISION_ENDPOINT, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
