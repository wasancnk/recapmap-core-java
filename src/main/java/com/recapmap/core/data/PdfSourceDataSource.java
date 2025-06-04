package com.recapmap.core.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks uploaded PDF files and their output images for a session/tenant.
 * Also stores original filename mapping.
 */
public class PdfSourceDataSource {
    // Map: UUID filename -> original filename
    private final Map<String, String> originalFilenameMap = new HashMap<>();
    // Map: UUID filename -> output image filenames (per page)
    private final Map<String, String[]> outputImagesMap = new HashMap<>();

    public void addOriginalFilename(String uuid, String originalName) {
        originalFilenameMap.put(uuid, originalName);
    }

    public String getOriginalFilename(String uuid) {
        return originalFilenameMap.get(uuid);
    }

    public void addOutputImages(String uuid, String[] imageFilenames) {
        outputImagesMap.put(uuid, imageFilenames);
    }

    public String[] getOutputImages(String uuid) {
        return outputImagesMap.get(uuid);
    }

    public Map<String, String> getOriginalFilenameMap() {
        return originalFilenameMap;
    }

    public Map<String, String[]> getOutputImagesMap() {
        return outputImagesMap;
    }

    public void clear() {
        originalFilenameMap.clear();
        outputImagesMap.clear();
    }

    // Remove all original filenames for this session
    public void clearOriginalFilenames() {
        originalFilenameMap.clear();
    }
}
