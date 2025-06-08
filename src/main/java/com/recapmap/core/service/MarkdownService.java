package com.recapmap.core.service;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        // Configure CommonMark with extensions
        List<Extension> extensions = Arrays.asList(
            TablesExtension.create(),
            AutolinkExtension.create()
        );
        
        this.parser = Parser.builder()
            .extensions(extensions)
            .build();
            
        this.renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true) // XSS protection
            .build();
    }

    /**
     * Convert Markdown content to HTML with security sanitization
     */
    public String convertToHtml(String markdownContent) {
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            return "";
        }

        try {
            // Parse markdown
            Node document = parser.parse(markdownContent);
            
            // Render to HTML
            String html = renderer.render(document);
            
            // Additional security sanitization
            html = sanitizeHtml(html);
            
            return html;
        } catch (Exception e) {
            // Log error and return safe fallback
            System.err.println("Error processing markdown: " + e.getMessage());
            return "<div class='error'>Error processing markdown content</div>";
        }
    }

    /**
     * Sanitize HTML content for security
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return "";
        }

        // Remove potentially dangerous attributes and scripts
        html = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        html = html.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
        html = html.replaceAll("(?i)<object[^>]*>.*?</object>", "");
        html = html.replaceAll("(?i)<embed[^>]*>.*?</embed>", "");
        html = html.replaceAll("(?i)<form[^>]*>.*?</form>", "");
        
        // Sanitize links
        html = sanitizeLinks(html);
        
        return html;
    }

    /**
     * Sanitize links to prevent XSS attacks
     */
    private String sanitizeLinks(String html) {
        // This is a basic sanitization - in production consider using a library like OWASP Java HTML Sanitizer
        return html.replaceAll("(?i)href\\s*=\\s*[\"']javascript:[^\"']*[\"']", "href='#'")
                  .replaceAll("(?i)href\\s*=\\s*[\"']data:[^\"']*[\"']", "href='#'")
                  .replaceAll("(?i)href\\s*=\\s*[\"']vbscript:[^\"']*[\"']", "href='#'");
    }

    /**
     * Extract plain text from markdown (useful for search/indexing)
     */
    public String convertToPlainText(String markdownContent) {
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            return "";
        }

        try {
            Node document = parser.parse(markdownContent);
            
            // Simple text extraction - visit all text nodes
            StringBuilder textBuilder = new StringBuilder();
            document.accept(new org.commonmark.node.AbstractVisitor() {
                @Override
                public void visit(org.commonmark.node.Text text) {
                    textBuilder.append(text.getLiteral()).append(" ");
                }
            });
            
            return textBuilder.toString().trim();
        } catch (Exception e) {
            System.err.println("Error extracting text from markdown: " + e.getMessage());
            return "";
        }
    }

    /**
     * Validate if a file path is safe for processing
     */
    public boolean isValidFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        // Prevent directory traversal attacks
        if (filePath.contains("..") || filePath.contains("/") || filePath.contains("\\")) {
            return false;
        }

        // Only allow markdown files
        if (!filePath.toLowerCase().endsWith(".md")) {
            return false;
        }

        // Limit filename length
        if (filePath.length() > 100) {
            return false;
        }

        return true;
    }
}
