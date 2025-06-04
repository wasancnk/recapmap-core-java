package com.recapmap.core.data;

import java.util.List;

public class ExtractionConfig {
    private int windowSizeTokens;
    private int stepSizeTokens;
    private List<String> extractionTypes; // e.g., ["raw", "semantic", "qa", "keywords"]
    private boolean enableAllTypes;

    public ExtractionConfig() {}

    public ExtractionConfig(int windowSizeTokens, int stepSizeTokens, List<String> extractionTypes, boolean enableAllTypes) {
        this.windowSizeTokens = windowSizeTokens;
        this.stepSizeTokens = stepSizeTokens;
        this.extractionTypes = extractionTypes;
        this.enableAllTypes = enableAllTypes;
    }

    public int getWindowSizeTokens() {
        return windowSizeTokens;
    }

    public void setWindowSizeTokens(int windowSizeTokens) {
        this.windowSizeTokens = windowSizeTokens;
    }

    public int getStepSizeTokens() {
        return stepSizeTokens;
    }

    public void setStepSizeTokens(int stepSizeTokens) {
        this.stepSizeTokens = stepSizeTokens;
    }

    public List<String> getExtractionTypes() {
        return extractionTypes;
    }

    public void setExtractionTypes(List<String> extractionTypes) {
        this.extractionTypes = extractionTypes;
    }

    public boolean isEnableAllTypes() {
        return enableAllTypes;
    }

    public void setEnableAllTypes(boolean enableAllTypes) {
        this.enableAllTypes = enableAllTypes;
    }
}
