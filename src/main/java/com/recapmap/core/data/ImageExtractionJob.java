package com.recapmap.core.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageExtractionJob {
    public enum ModelProvider {
        OPENAI_GPT4O,
        OPENAI_GPT4V,
        ANTHROPIC_CLAUDE,
        GOOGLE_GEMINI,
        OTHER
    }

    private File imageFile;
    private List<String> prompts = new ArrayList<>();
    private List<String> results = new ArrayList<>();
    private ModelProvider modelProvider;
    private String status; // e.g. PENDING, RUNNING, DONE, ERROR
    private String errorMessage;

    public ImageExtractionJob(File imageFile, List<String> prompts, ModelProvider modelProvider) {
        this.imageFile = imageFile;
        if (prompts != null) this.prompts.addAll(prompts);
        this.modelProvider = modelProvider;
        this.status = "PENDING";
    }

    public File getImageFile() { return imageFile; }
    public List<String> getPrompts() { return prompts; }
    public List<String> getResults() { return results; }
    public ModelProvider getModelProvider() { return modelProvider; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public void setStatus(String status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void addResult(String result) { this.results.add(result); }
}
