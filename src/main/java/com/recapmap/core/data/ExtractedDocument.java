package com.recapmap.core.data;

import java.util.List;

public class ExtractedDocument {
    private String documentId;
    private String extractionMode;
    private int pageCount;
    private List<PageData> pages;
    private String extractionTimestamp;

    public static class PageData {
        private int pageIndex;
        private String markdown;
        private List<String> keywords;
        // Add more fields if needed

        public int getPageIndex() { return pageIndex; }
        public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }
        public String getMarkdown() { return markdown; }
        public void setMarkdown(String markdown) { this.markdown = markdown; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getExtractionMode() { return extractionMode; }
    public void setExtractionMode(String extractionMode) { this.extractionMode = extractionMode; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public List<PageData> getPages() { return pages; }
    public void setPages(List<PageData> pages) { this.pages = pages; }
    public String getExtractionTimestamp() { return extractionTimestamp; }
    public void setExtractionTimestamp(String extractionTimestamp) { this.extractionTimestamp = extractionTimestamp; }
}
