package com.recapmap.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recapmap.core.data.ExtractionConfig;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExtractionPipelineService {
    private final OpenAiVisionService openAiVisionService;

    public ExtractionPipelineService(OpenAiVisionService openAiVisionService) {
        this.openAiVisionService = openAiVisionService;
    }

    /**
     * Run the extraction pipeline for a document.
     * @param documentFolder The folder containing the document's images.
     * @param imageFiles List of image files (ordered pages).
     * @param extractionConfig Extraction config (window/step size).
     * @param promptList List of prompt configs (each with detail, note, extractionTypes, includeImage).
     * @throws IOException if file operations fail
     */
    public void runExtractionPipeline(String documentFolder, List<File> imageFiles, ExtractionConfig extractionConfig, List<Map<String, Object>> promptList) throws IOException {
        // Prepare json_result folder
        File jsonResultDir = new File(documentFolder, "json_result");
        if (!jsonResultDir.exists()) jsonResultDir.mkdirs();

        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Separate prompts by extraction type
        // We'll process 'raw paragraph' prompts separately (no windowing)
        // Others (semantic/diagram, q&a) use windowing
        // 'keywords' will be handled after other extractions
        List<Map<String, Object>> rawParagraphPrompts = promptList.stream()
                .filter(p -> {
                    List<?> types = p.get("extractionTypes") instanceof List ? (List<?>) p.get("extractionTypes") : List.of();
                    return types.contains("raw");
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> windowedPrompts = promptList.stream()
                .filter(p -> {
                    List<?> types = p.get("extractionTypes") instanceof List ? (List<?>) p.get("extractionTypes") : List.of();
                    return types.contains("semantic") || types.contains("qa");
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> keywordPrompts = promptList.stream()
                .filter(p -> {
                    List<?> types = p.get("extractionTypes") instanceof List ? (List<?>) p.get("extractionTypes") : List.of();
                    return types.contains("keywords");
                })
                .collect(Collectors.toList());

        // 2. Process 'raw paragraph' prompts (no windowing)
        int extractionCount = 0;
        int totalExtractions = rawParagraphPrompts.size() + (windowedPrompts.size() * ((int)Math.ceil((imageFiles.size() - 2) / 2.0) + 1));
        for (int pIdx = 0; pIdx < rawParagraphPrompts.size(); pIdx++) {
            Map<String, Object> prompt = rawParagraphPrompts.get(pIdx);
            String promptDetail = (String) prompt.getOrDefault("detail", "");
            List<?> extractionTypesRaw = prompt.get("extractionTypes") instanceof List ? (List<?>) prompt.get("extractionTypes") : List.of();
            List<String> extractionTypes = extractionTypesRaw.stream().map(Object::toString).collect(Collectors.toList());
            boolean includeImage = Boolean.TRUE.equals(prompt.get("includeImage"));
            for (String extractionType : extractionTypes) {
                if (!"raw".equals(extractionType)) continue;
                extractionCount++;
                long startTime = System.currentTimeMillis();
                System.out.println("[Extraction] (No window) Round " + extractionCount + "/" + totalExtractions);
                System.out.println("  Prompt idx: " + pIdx + ", Extraction type: " + extractionType);
                System.out.println("  Start timestamp: " + new java.util.Date(startTime));
                String fullPrompt = promptDetail + "\n[Extraction Type: " + extractionType + "]";
                String resultJson = openAiVisionService.extractFromImages(imageFiles, fullPrompt, includeImage);
                long finishTime = System.currentTimeMillis();
                System.out.println("  Finish timestamp: " + new java.util.Date(finishTime));
                System.out.println("  Spent time (ms): " + (finishTime - startTime));
                try {
                    JsonNode root = objectMapper.readTree(resultJson);
                    if (root.has("usage")) {
                        JsonNode usage = root.get("usage");
                        int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : -1;
                        System.out.println("  Token usage: " + totalTokens);
                    } else {
                        System.out.println("  Token usage: (not found in response)");
                    }
                } catch (Exception e) {
                    System.out.println("  [WARN] Could not parse token usage: " + e.getMessage());
                }
                String fileName = "allpages_" + pIdx + "_raw_paragraph.json";
                File outFile = new File(jsonResultDir, fileName);
                try (FileWriter fw = new FileWriter(outFile)) {
                    fw.write(resultJson);
                }
                System.out.println("  Result file: " + outFile.getAbsolutePath());
                // --- Save Markdown as .md file ---
                // Try to extract markdown from the resultJson (if it's a markdown string or in a field)
                String markdown = null;
                try {
                    JsonNode root = objectMapper.readTree(resultJson);
                    // If the result is a plain string, use as-is
                    if (root.isTextual()) {
                        markdown = root.asText();
                    } else if (root.has("content")) {
                        markdown = root.get("content").asText();
                    } else if (root.has("markdown")) {
                        markdown = root.get("markdown").asText();
                    } else if (root.has("choices")) {
                        // OpenAI format: choices[0].message.content
                        JsonNode choices = root.get("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode msg = choices.get(0).get("message");
                            if (msg != null && msg.has("content")) {
                                markdown = msg.get("content").asText();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  [WARN] Could not extract markdown for .md file: " + e.getMessage());
                }
                if (markdown != null && !markdown.isBlank()) {
                    // Remove code block markers if present
                    if (markdown.startsWith("```") ) {
                        int firstNewline = markdown.indexOf('\n');
                        if (firstNewline > 0) {
                            markdown = markdown.substring(firstNewline + 1);
                        }
                        if (markdown.endsWith("```") ) {
                            markdown = markdown.substring(0, markdown.length() - 3);
                        }
                    }
                    // Clean/filter markdown before saving
                    markdown = cleanMarkdown(markdown);
                    String txtFileName = "allpages_" + pIdx + "_raw_paragraph.txt";
                    File txtFile = new File(jsonResultDir, txtFileName);
                    try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(txtFile), java.nio.charset.StandardCharsets.UTF_8)) {
                        fw.write(markdown.trim());
                    }
                    System.out.println("  Markdown file: " + txtFile.getAbsolutePath());
                }
            }
        }

        // 3. Process windowed prompts (semantic/diagram, q&a)
        int pagesPerWindow = 2;
        int stepPages = 2;
        int numWindows = (int)Math.ceil((imageFiles.size() - pagesPerWindow) / (double)stepPages) + 1;
        for (int w = 0; w < numWindows; w++) {
            int startIdx = w * stepPages;
            int endIdx = Math.min(startIdx + pagesPerWindow, imageFiles.size());
            List<File> windowImages = imageFiles.subList(startIdx, endIdx);
            // Try to load previous window's markdown for context
            String prevMarkdown = null;
            if (w > 0) {
                File prevMdFile = new File(jsonResultDir, "allpages_0_raw_paragraph.md");
                if (prevMdFile.exists()) {
                    prevMarkdown = Files.readString(prevMdFile.toPath());
                }
            }
            for (int pIdx = 0; pIdx < windowedPrompts.size(); pIdx++) {
                Map<String, Object> prompt = windowedPrompts.get(pIdx);
                String promptDetail = (String) prompt.getOrDefault("detail", "");
                List<?> extractionTypesRaw = prompt.get("extractionTypes") instanceof List ? (List<?>) prompt.get("extractionTypes") : List.of();
                List<String> extractionTypes = extractionTypesRaw.stream().map(Object::toString).collect(Collectors.toList());
                boolean includeImage = Boolean.TRUE.equals(prompt.get("includeImage"));
                for (String extractionType : extractionTypes) {
                    if (!"semantic".equals(extractionType) && !"qa".equals(extractionType)) continue;
                    extractionCount++;
                    long startTime = System.currentTimeMillis();
                    System.out.println("[Extraction] Round " + extractionCount + "/" + totalExtractions);
                    System.out.println("  Window: " + w + " (pages " + startIdx + " to " + (endIdx-1) + ")");
                    System.out.println("  Prompt idx: " + pIdx + ", Extraction type: " + extractionType);
                    System.out.println("  Start timestamp: " + new java.util.Date(startTime));
                    // Compose the prompt for this extractionType, with previous markdown context if available
                    StringBuilder fullPrompt = new StringBuilder();
                    if (prevMarkdown != null && !prevMarkdown.isBlank()) {
                        fullPrompt.append("Previous context (Markdown):\n\n").append(prevMarkdown).append("\n\n");
                    }
                    fullPrompt.append(promptDetail)
                        .append("\n[Extraction Type: ").append(extractionType).append("]\n")
                        .append("Output your answer as a well-structured Markdown document, using headings, bullet points, and code blocks as appropriate.");
                    String resultJson = openAiVisionService.extractFromImages(windowImages, fullPrompt.toString(), includeImage);
                    long finishTime = System.currentTimeMillis();
                    System.out.println("  Finish timestamp: " + new java.util.Date(finishTime));
                    System.out.println("  Spent time (ms): " + (finishTime - startTime));
                    try {
                        JsonNode root = objectMapper.readTree(resultJson);
                        if (root.has("usage")) {
                            JsonNode usage = root.get("usage");
                            int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : -1;
                            System.out.println("  Token usage: " + totalTokens);
                        } else {
                            System.out.println("  Token usage: (not found in response)");
                        }
                    } catch (Exception e) {
                        System.out.println("  [WARN] Could not parse token usage: " + e.getMessage());
                    }
                    String fileName = w + "_" + pIdx + "_" + extractionType + ".json";
                    File outFile = new File(jsonResultDir, fileName);
                    try (FileWriter fw = new FileWriter(outFile)) {
                        fw.write(resultJson);
                    }
                    System.out.println("  Result file: " + outFile.getAbsolutePath());
                    // --- Save Markdown as .md file ---
                    String markdown = null;
                    try {
                        JsonNode root = objectMapper.readTree(resultJson);
                        if (root.isTextual()) {
                            markdown = root.asText();
                        } else if (root.has("content")) {
                            markdown = root.get("content").asText();
                        } else if (root.has("markdown")) {
                            markdown = root.get("markdown").asText();
                        } else if (root.has("choices")) {
                            JsonNode choices = root.get("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode msg = choices.get(0).get("message");
                                if (msg != null && msg.has("content")) {
                                    markdown = msg.get("content").asText();
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("  [WARN] Could not extract markdown for .md file: " + e.getMessage());
                    }
                    if (markdown != null && !markdown.isBlank()) {
                        if (markdown.startsWith("```") ) {
                            int firstNewline = markdown.indexOf('\n');
                            if (firstNewline > 0) {
                                markdown = markdown.substring(firstNewline + 1);
                            }
                            if (markdown.endsWith("```") ) {
                                markdown = markdown.substring(0, markdown.length() - 3);
                            }
                        }
                        // Clean/filter markdown before saving
                        markdown = cleanMarkdown(markdown);
                        String txtFileName = w + "_" + pIdx + "_" + extractionType + ".txt";
                        File txtFile = new File(jsonResultDir, txtFileName);
                        try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(txtFile), java.nio.charset.StandardCharsets.UTF_8)) {
                            fw.write(markdown.trim());
                        }
                        System.out.println("  Markdown file: " + txtFile.getAbsolutePath());
                    }
                }
            }
        }

        // 4. Process 'keywords' prompts (extract from previous results, no Vision API call)
        for (int pIdx = 0; pIdx < keywordPrompts.size(); pIdx++) {
            Map<String, Object> prompt = keywordPrompts.get(pIdx);
            String promptDetail = (String) prompt.getOrDefault("detail", "");
            // For each window, aggregate results from previous 3 types
            for (int w = 0; w < numWindows; w++) {
                // Load previous results for this window (semantic/diagram, q&a)
                StringBuilder combinedText = new StringBuilder();
                String[] prevTypes = {"raw_paragraph", "semantic/diagram", "q&a"};
                for (int prevIdx = 0; prevIdx < prevTypes.length; prevIdx++) {
                    String prevType = prevTypes[prevIdx];
                    String fileName;
                    if (prevType.equals("raw_paragraph")) {
                        fileName = "allpages_" + pIdx + "_raw_paragraph.json";
                    } else {
                        fileName = w + "_" + pIdx + "_" + prevType + ".json";
                    }
                    File prevFile = new File(jsonResultDir, fileName);
                    if (prevFile.exists()) {
                        String content = Files.readString(prevFile.toPath());
                        combinedText.append(content).append("\n");
                    }
                }
                // Here, you would call your keyword extraction logic on combinedText
                // For now, just save the combined text as a placeholder
                String keywordResult = "{\"keywords\": [\"TODO: extract keywords from combinedText\"]}";
                String outFileName = w + "_" + pIdx + "_keywords.json";
                File outFile = new File(jsonResultDir, outFileName);
                try (FileWriter fw = new FileWriter(outFile)) {
                    fw.write(keywordResult);
                }
                System.out.println("[Keywords Extraction] Window " + w + ", Prompt idx: " + pIdx + " -> " + outFile.getAbsolutePath());
            }
        }
    }

    /**
     * Simple per-page extraction: For each image (page), extract as Markdown with a hardcoded concise prompt.
     * Saves .md and .json for each page in json_result.
     * Now supports multi-threading (thread count configurable).
     */
    public void runSimplePerPageExtraction(String documentFolder, List<File> imageFiles, int numThreads) throws IOException {
        // Sort imageFiles by numeric value in filename for correct page order (e.g., page_1, page_2, ..., page_10)
        // Improved sort: extract the numeric page index from the filename, ignoring the PDF base name and extension
        imageFiles.sort((f1, f2) -> {
            String name1 = f1.getName();
            String name2 = f2.getName();
            // Remove extension
            name1 = name1.contains(".") ? name1.substring(0, name1.lastIndexOf('.')) : name1;
            name2 = name2.contains(".") ? name2.substring(0, name2.lastIndexOf('.')) : name2;
            // Extract trailing number (page index)
            String[] parts1 = name1.split("_page");
            String[] parts2 = name2.split("_page");
            String pageStr1 = parts1[1];
            String pageStr2 = parts2[1];
            try {
                int page1 = Integer.parseInt(pageStr1);
                int page2 = Integer.parseInt(pageStr2);
                return Integer.compare(page1, page2);
            } catch (Exception e) {
                return name1.compareTo(name2);
            }
        });
        File jsonResultDir = new File(documentFolder, "json_result");
        if (!jsonResultDir.exists()) jsonResultDir.mkdirs();
        ObjectMapper objectMapper = new ObjectMapper();
        String hardcodedPrompt = "Extract the content of this page as a Markdown document.\n" +
                "- For paragraphs, keep the raw text as-is, preserving the original wording and order.\n" +
                "- For diagrams, charts, or non-paragraph elements, perform a detailed extraction, not a summary:\n" +
                "    - Identify and list every distinct element, label, node, relationship, and text present in the diagram.\n" +
                "    - For each, provide the exact wording, and if possible, describe its role or meaning in context.\n" +
                "    - Do not summarize; instead, enumerate all visible items, including minor or peripheral details (such as data, algorithm, certification, domain name, etc.).\n" +
                "    - Present the extracted information in a structured Markdown format, using bullet points or tables as appropriate.\n" +
                "    - If the diagram contains groups or categories, reflect this structure in your output.\n" +
                "- In addition, extract and return a list of 30-50 concrete keywords, including direct terms, synonyms, related concepts, scenarios, use cases, processes, consequences, and any other relevant structured information that appears on the page.\n" +
                "- The keywords list should be comprehensive and cover all important terms, entities, and concepts, both explicit and implied, in the content.\n" +
                "- Present the keywords as a JSON array at the end of the Markdown, under a heading '## Extracted Keywords'.\n" +
                "- Do not invent or add information that is not present on the page.\n" +
                "Return only the final result as a well-structured Markdown document, with the keywords section at the end.";

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < imageFiles.size(); i++) {
            final int pageIndex = i;
            futures.add(executor.submit(() -> {
                int maxRetries = 5;
                int retry = 0;
                int backoffMillis = 2000;
                while (retry < maxRetries) {
                    try {
                        File pageImage = imageFiles.get(pageIndex);
                        String prompt = hardcodedPrompt;
                        long startTime = System.currentTimeMillis();
                        System.out.println("[SimpleExtract] Page " + pageIndex + " of " + imageFiles.size());
                        String resultJson = openAiVisionService.extractFromImages(List.of(pageImage), prompt, true);
                        long finishTime = System.currentTimeMillis();
                        System.out.println("  Finish timestamp: " + new java.util.Date(finishTime));
                        System.out.println("  Spent time (ms): " + (finishTime - startTime));
                        // Save JSON
                        String jsonFileName = "page_" + pageIndex + ".json";
                        File outFile = new File(jsonResultDir, jsonFileName);
                        try (FileWriter fw = new FileWriter(outFile)) {
                            fw.write(resultJson);
                        }
                        // Print token usage if present
                        try {
                            JsonNode root = objectMapper.readTree(resultJson);
                            if (root.has("usage")) {
                                JsonNode usage = root.get("usage");
                                int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : -1;
                                System.out.println("  Token usage: " + totalTokens);
                            } else {
                                System.out.println("  Token usage: (not found in response)");
                            }
                        } catch (Exception e) {
                            System.out.println("  [WARN] Could not parse token usage: " + e.getMessage());
                        }
                        // Extract markdown and save as .md
                        String markdown = null;
                        try {
                            JsonNode root = objectMapper.readTree(resultJson);
                            if (root.isTextual()) {
                                markdown = root.asText();
                            } else if (root.has("content")) {
                                markdown = root.get("content").asText();
                            } else if (root.has("markdown")) {
                                markdown = root.get("markdown").asText();
                            } else if (root.has("choices")) {
                                JsonNode choices = root.get("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode msg = choices.get(0).get("message");
                                    if (msg != null && msg.has("content")) {
                                        markdown = msg.get("content").asText();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("  [WARN] Could not extract markdown for .md file: " + e.getMessage());
                        }
                        if (markdown != null && !markdown.isBlank()) {
                            if (markdown.startsWith("```") ) {
                                int firstNewline = markdown.indexOf('\n');
                                if (firstNewline > 0) {
                                    markdown = markdown.substring(firstNewline + 1);
                                }
                                if (markdown.endsWith("```") ) {
                                    markdown = markdown.substring(0, markdown.length() - 3);
                                }
                            }
                            // Clean the markdown before saving
                            markdown = cleanMarkdown(markdown);
                            String txtFileName = "page_" + pageIndex + ".txt";
                            File txtFile = new File(jsonResultDir, txtFileName);
                            try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(txtFile), java.nio.charset.StandardCharsets.UTF_8)) {
                                fw.write(markdown.trim());
                            }
                            System.out.println("  Markdown file: " + txtFile.getAbsolutePath());
                        }
                        return null;
                    } catch (Exception e) {
                        // Check for OpenAI rate limit error (HTTP 429 or message)
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("429") || msg.toLowerCase().contains("rate limit")) {
                            retry++;
                            int sleep = backoffMillis * retry;
                            System.out.println("[WARN] Rate limit hit for page " + pageIndex + ", retry " + retry + " after " + sleep + "ms");
                            try { Thread.sleep(sleep); } catch (InterruptedException ie) { break; }
                        } else {
                            System.out.println("[ERROR] Extraction failed for page " + pageIndex + ": " + e.getMessage());
                            break;
                        }
                    }
                }
                return null;
            }));
        }
        // Wait for all threads to finish
        for (java.util.concurrent.Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { System.out.println("[WARN] Extraction thread error: " + e.getMessage()); }
        }
        executor.shutdown();
    }

    /**
     * Combine per-page results and save to MongoDB Atlas
     * @param documentFolder The folder containing the document's per-page JSON results.
     * @param documentId The ID of the document in MongoDB Atlas.
     * @param mongoDbService The MongoDB service instance for database operations.
     * @throws IOException if file operations or MongoDB operations fail
     */
    public void combinePerPageResultsAndSaveToMongo(String documentFolder, String documentId, MongoDbService mongoDbService) throws IOException {
        combinePerPageResultsAndSaveToMongo(documentFolder, documentId, mongoDbService, true);
    }

    /**
     * Combine per-page results and save to MongoDB Atlas
     * @param documentFolder The folder containing the document's per-page JSON results.
     * @param documentId The ID of the document in MongoDB Atlas.
     * @param mongoDbService The MongoDB service instance for database operations.
     * @param skipObjectMapper Skip ObjectMapper validation and use plain text reading for markdown
     * @throws IOException if file operations or MongoDB operations fail
     */
    public void combinePerPageResultsAndSaveToMongo(String documentFolder, String documentId, MongoDbService mongoDbService, boolean skipObjectMapper) throws IOException {
        File jsonResultDir = new File(documentFolder, "json_result");
        File[] jsonFiles = jsonResultDir.listFiles((dir, name) -> name.matches("page_\\d+\\.json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            throw new IOException("No per-page JSON files found in " + jsonResultDir.getAbsolutePath());
        }
        // Extract original file name from documentFolder (assume last folder name is the base file name)
        String originalFileName = null;
        File docFolderFile = new File(documentFolder);
        originalFileName = docFolderFile.getName();
        // Sort files by page index
        java.util.Arrays.sort(jsonFiles, (f1, f2) -> {
            String name1 = f1.getName();
            String name2 = f2.getName();
            // Remove extension
            name1 = name1.contains(".") ? name1.substring(0, name1.lastIndexOf('.')) : name1;
            name2 = name2.contains(".") ? name2.substring(0, name2.lastIndexOf('.')) : name2;
            // Extract trailing number (page index)
            String[] parts1 = name1.split("_");
            String[] parts2 = name2.split("_");
            String pageStr1 = parts1[1];
            String pageStr2 = parts2[1];
            try {
                int page1 = Integer.parseInt(pageStr1);
                int page2 = Integer.parseInt(pageStr2);
                return Integer.compare(page1, page2);
            } catch (Exception e) {
                return name1.compareTo(name2);
            }
        });
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (int i = 0; i < jsonFiles.length; i++) {
            File pageFile = jsonFiles[i];
            String markdown = null;
            java.util.List<String> keywords = new java.util.ArrayList<>();
            if (skipObjectMapper) {
                // Just read the corresponding .txt file as plain text
                File txtFile = new File(jsonResultDir, "page_" + i + ".txt");
                if (txtFile.exists()) {
                    try {
                        markdown = java.nio.file.Files.readString(txtFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception utf8e) {
                        try {
                            byte[] bytes = java.nio.file.Files.readAllBytes(txtFile.toPath());
                            markdown = new String(bytes, java.nio.charset.Charset.forName("Windows-1252"));
                            markdown = markdown.replaceAll("[^\u0000-\uFFFF]", " ");
                            System.out.println("[WARN] Read page_" + i + ".txt as Windows-1252 due to UTF-8 error: " + utf8e.getMessage());
                        } catch (Exception fallbackE) {
                            System.out.println("[WARN] Could not read page_" + i + ".txt (all attempts failed): " + fallbackE.getMessage());
                            markdown = null;
                        }
                    }
                    // Extract keywords from the text
                    if (markdown != null) {
                        int kwIdx = markdown.indexOf("## Extracted Keywords");
                        if (kwIdx >= 0) {
                            int jsonStart = markdown.indexOf("[", kwIdx);
                            int jsonEnd = markdown.indexOf("]", jsonStart);
                            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                String jsonArr = markdown.substring(jsonStart, jsonEnd + 1);
                                try {
                                    java.util.List<?> rawList = objectMapper.readValue(jsonArr, java.util.List.class);
                                    keywords = rawList.stream().map(Object::toString).toList();
                                } catch (Exception e) {
                                    System.out.println("[WARN] Could not extract keywords for page " + i + ": " + e.getMessage());
                                }
                            }
                        }
                        // Remove keywords section from markdown
                        int kwIdx2 = markdown.indexOf("## Extracted Keywords");
                        if (kwIdx2 >= 0) {
                            markdown = markdown.substring(0, kwIdx2).trim();
                        }
                    }
                }
            } else {
                // ...existing code for objectMapper-based extraction...
                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(pageFile);
                    if (root.isTextual()) {
                        markdown = root.asText();
                    } else if (root.has("content")) {
                        markdown = root.get("content").asText();
                    } else if (root.has("markdown")) {
                        markdown = root.get("markdown").asText();
                    } else if (root.has("choices")) {
                        com.fasterxml.jackson.databind.JsonNode choices = root.get("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            com.fasterxml.jackson.databind.JsonNode msg = choices.get(0).get("message");
                            if (msg != null && msg.has("content")) {
                                markdown = msg.get("content").asText();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[WARN] Could not extract markdown for page " + i + ": " + e.getMessage());
                }
                // Parse keywords from the corresponding .txt file if available
                File txtFile = new File(jsonResultDir, "page_" + i + ".txt");
                if (txtFile.exists()) {
                    String txtContent = null;
                    try {
                        txtContent = java.nio.file.Files.readString(txtFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception utf8e) {
                        try {
                            byte[] bytes = java.nio.file.Files.readAllBytes(txtFile.toPath());
                            txtContent = new String(bytes, java.nio.charset.Charset.forName("Windows-1252"));
                            txtContent = txtContent.replaceAll("[^\u0000-\uFFFF]", " ");
                            System.out.println("[WARN] Read page_" + i + ".txt as Windows-1252 due to UTF-8 error: " + utf8e.getMessage());
                        } catch (Exception fallbackE) {
                            System.out.println("[WARN] Could not extract keywords for page " + i + " (all attempts failed): " + fallbackE.getMessage());
                            txtContent = null;
                        }
                    }
                    if (txtContent != null) {
                        try {
                            int kwIdx = txtContent.indexOf("## Extracted Keywords");
                            if (kwIdx >= 0) {
                                int jsonStart = txtContent.indexOf("[", kwIdx);
                                int jsonEnd = txtContent.indexOf("]", jsonStart);
                                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                    String jsonArr = txtContent.substring(jsonStart, jsonEnd + 1);
                                    java.util.List<?> rawList = objectMapper.readValue(jsonArr, java.util.List.class);
                                    keywords = rawList.stream().map(Object::toString).toList();
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("[WARN] Could not extract keywords for page " + i + ": " + e.getMessage());
                        }
                    }
                }
                // Remove keywords JSON array from markdown before saving to MongoDB
                if (markdown != null) {
                    int kwIdx = markdown.indexOf("## Extracted Keywords");
                    if (kwIdx >= 0) {
                        markdown = markdown.substring(0, kwIdx).trim();
                    }
                }
            }
            // Build per-page JSON structure
            org.bson.Document pageDoc = new org.bson.Document();
            pageDoc.put("documentId", documentId);
            pageDoc.put("pageIndex", i + 1);
            pageDoc.put("markdown", markdown);
            pageDoc.put("keywords", keywords);
            pageDoc.put("extractionMode", "simple-per-page");
            pageDoc.put("extractionTimestamp", java.time.Instant.now().toString());
            pageDoc.put("originalFileName", originalFileName); // <-- Add this line
            mongoDbService.saveExtractedDocument(pageDoc, "extracted_documents");
            System.out.println("[MongoDB] Page document saved for " + documentId + " page " + i);
        }
    }

    /**
     * Clean Markdown string before saving to file:
     * - Remove all non-printable/control characters except line breaks and tabs
     * - Replace invalid/unmappable chars with whitespace
     * - Normalize line endings to \n
     */
    private static String cleanMarkdown(String input) {
        if (input == null) return null;
        // Remove all control characters except \n, \r, \t
        String cleaned = input.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E\\u00A0-\\uFFFF]", " ");
        // Normalize line endings to \n
        cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n");
        // Optionally trim trailing whitespace on each line
        cleaned = java.util.Arrays.stream(cleaned.split("\n", -1))
                .map(String::stripTrailing)
                .collect(java.util.stream.Collectors.joining("\n"));
        return cleaned;
    }
}
