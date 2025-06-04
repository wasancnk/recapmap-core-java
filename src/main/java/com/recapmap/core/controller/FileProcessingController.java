package com.recapmap.core.controller;

import com.recapmap.core.data.PdfSourceDataSource;
import com.recapmap.core.service.PdfService;
import com.recapmap.core.data.ExtractionConfig;
import com.recapmap.core.service.ExtractionPipelineService;
import com.recapmap.core.config.AppConfig;
import com.recapmap.core.service.MongoDbService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/file")
public class FileProcessingController {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingController.class);
    @Autowired
    private PdfService pdfService;

    @Autowired
    private ExtractionPipelineService extractionPipelineService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private MongoDbService mongoDbService;

    // Upload PDF(s)
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile[] files, HttpSession session, Authentication auth) {
        logger.info("[CONFIG] uploadPdf: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        String sessionId = session.getId();
        PdfSourceDataSource dataSource = pdfService.getDataSourceForSession(sessionId);
        File sessionDir = pdfService.getSessionDir(sessionId);
        if (!sessionDir.exists()) sessionDir.mkdirs();
        List<Map<String, String>> uploaded = new ArrayList<>();
        // Clear the uploaded list for this session before each upload
        dataSource.clearOriginalFilenames();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            if (file.getSize() > PdfService.MAX_UPLOAD_SIZE_MB * 1024L * 1024L) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File too large: " + file.getOriginalFilename());
            }
            // Generate a unique UUID for each file
            String uuid = UUID.randomUUID().toString();
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String storedName = uuid + (ext != null ? "." + ext : "");
            File dest = new File(sessionDir, storedName);
            try {
                Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                dataSource.addOriginalFilename(uuid, file.getOriginalFilename());
                uploaded.add(Map.of("uuid", uuid, "original", file.getOriginalFilename(), "stored", storedName));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save: " + file.getOriginalFilename());
            }
        }
        return ResponseEntity.ok(uploaded);
    }

    // List uploaded files for session
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> listUploadedFiles(HttpSession session) {
        logger.info("[CONFIG] listUploadedFiles: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        String sessionId = session.getId();
        PdfSourceDataSource dataSource = pdfService.getDataSourceForSession(sessionId);
        Map<String, String> map = dataSource.getOriginalFilenameMap();
        return ResponseEntity.ok(map);
    }

    // Trigger PDF-to-image conversion for a file (by UUID)
    @PostMapping("/convert")
    @ResponseBody
    public ResponseEntity<?> convertPdf(@RequestParam("uuid") String uuid, HttpSession session) {
        logger.info("[CONFIG] convertPdf: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        String sessionId = session.getId();
        PdfSourceDataSource dataSource = pdfService.getDataSourceForSession(sessionId);
        String original = dataSource.getOriginalFilename(uuid);
        if (original == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
        // Find stored filename by UUID
        File sessionDir = pdfService.getSessionDir(sessionId);
        File[] files = sessionDir.listFiles((dir, name) -> name.startsWith(uuid + "."));
        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stored file not found");
        }
        String storedFilename = files[0].getName();
        // Create output folder named after the stored UUID (normalized file name, without extension)
        String baseName = storedFilename;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
        File outputDir = new File(sessionDir, baseName);
        if (!outputDir.exists()) outputDir.mkdirs();
        pdfService.convertPdfToImages(sessionId, uuid, storedFilename, outputDir);
        return ResponseEntity.ok("Conversion started for " + original);
    }

    // Poll progress log for current session
    @GetMapping("/progress")
    @ResponseBody
    public ResponseEntity<?> getProgress(HttpSession session) {
        logger.info("[CONFIG] getProgress: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        String sessionId = session.getId();
        List<String> log = pdfService.getProgressLog(sessionId);
        return ResponseEntity.ok(log);
    }

    // Admin-only cleanup endpoint
    @PostMapping("/cmd/cleanup-upload")
    @ResponseBody
    public ResponseEntity<?> cleanupUploads(Authentication auth) {
        logger.info("[CONFIG] cleanupUploads: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Admins only");
        }
        // Delete all files and folders under ROOT_FOLDER
        File root = new File(PdfService.ROOT_FOLDER);
        int deleted = deleteRecursive(root);
        pdfService.clearAllSessions();
        return ResponseEntity.ok("Cleanup complete. Files deleted: " + deleted);
    }

    // Admin: update config (max upload size, DPI, threads)
    @PostMapping("/cmd/update-config")
    @ResponseBody
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> body, Authentication auth) {
        logger.info("[CONFIG] updateConfig: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden: Admins only"));
        }
        try {
            int maxUploadSizeMB = Integer.parseInt(body.get("maxUploadSizeMB").toString());
            int pdfToImageDpi = Integer.parseInt(body.get("pdfToImageDpi").toString());
            int maxConvertThreads = Integer.parseInt(body.get("maxConvertThreads").toString());
            if (maxUploadSizeMB < 1 || maxUploadSizeMB > 500) throw new IllegalArgumentException("Max upload size out of range");
            if (pdfToImageDpi < 72 || pdfToImageDpi > 600) throw new IllegalArgumentException("DPI out of range");
            if (maxConvertThreads < 1 || maxConvertThreads > 64) throw new IllegalArgumentException("Threads out of range");
            com.recapmap.core.service.PdfService.MAX_UPLOAD_SIZE_MB = maxUploadSizeMB;
            com.recapmap.core.service.PdfService.PDF_TO_IMAGE_DPI = pdfToImageDpi;
            com.recapmap.core.service.PdfService.updateConvertThreadPool(maxConvertThreads);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Admin: get current config for UI
    @GetMapping("/cmd/get-config")
    @ResponseBody
    public Map<String, Object> getConfig() {
        logger.info("[CONFIG] getConfig: MAX_UPLOAD_SIZE_MB={}, PDF_TO_IMAGE_DPI={}, MAX_CONVERT_THREADS={}", PdfService.MAX_UPLOAD_SIZE_MB, PdfService.PDF_TO_IMAGE_DPI, PdfService.MAX_CONVERT_THREADS);
        Map<String, Object> map = new HashMap<>();
        map.put("maxUploadSizeMB", com.recapmap.core.service.PdfService.MAX_UPLOAD_SIZE_MB);
        map.put("pdfToImageDpi", com.recapmap.core.service.PdfService.PDF_TO_IMAGE_DPI);
        map.put("maxConvertThreads", com.recapmap.core.service.PdfService.MAX_CONVERT_THREADS);
        return map;
    }

    // Get and set extraction config
    private ExtractionConfig extractionConfig = new ExtractionConfig(12000, 6000, List.of("raw", "semantic", "qa", "keywords"), true);

    @GetMapping("/extraction-config")
    public ExtractionConfig getExtractionConfig() {
        return extractionConfig;
    }

    @PostMapping("/extraction-config")
    public void setExtractionConfig(@RequestBody ExtractionConfig config) {
        this.extractionConfig = config;
    }

    // Trigger extraction pipeline for a converted PDF (by UUID)
    @PostMapping("/extract")
    public ResponseEntity<?> extractData(@RequestBody Map<String, Object> body, HttpSession session) {
        String uuid = (String) body.get("uuid");
        Object promptListObj = body.get("promptList");
        List<Map<String, Object>> promptList = null;
        if (promptListObj instanceof List) {
            ObjectMapper mapper = new ObjectMapper();
            promptList = mapper.convertValue(promptListObj, new TypeReference<List<Map<String, Object>>>(){});
        }
        String sessionId = session.getId();
        PdfSourceDataSource dataSource = pdfService.getDataSourceForSession(sessionId);
        String original = dataSource.getOriginalFilename(uuid);
        if (original == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
        // Find stored filename by UUID
        File sessionDir = pdfService.getSessionDir(sessionId);
        File[] files = sessionDir.listFiles((dir, name) -> name.startsWith(uuid + "."));
        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stored file not found");
        }
        String storedFilename = files[0].getName();
        // Output folder named after the stored UUID (normalized file name, without extension)
        String baseName = storedFilename;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
        File outputDir = new File(sessionDir, baseName);
        if (!outputDir.exists()) outputDir.mkdirs();
        // List all PNG images in outputDir (sorted by page)
        File[] imageFiles = outputDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (imageFiles == null || imageFiles.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No images found for extraction");
        }
        List<File> sortedImages = Arrays.asList(imageFiles);
        sortedImages.sort(Comparator.comparing(File::getName));
        // Use promptList from request if provided, otherwise fallback to default
        if (promptList == null || promptList.isEmpty()) {
            promptList = new ArrayList<>();
            Map<String, Object> defaultPrompt = new HashMap<>();
            defaultPrompt.put("detail", "Extract all paragraphs.");
            defaultPrompt.put("extractionTypes", List.of("raw"));
            defaultPrompt.put("includeImage", true);
            promptList.add(defaultPrompt);
        }
        try {
            extractionPipelineService.runExtractionPipeline(outputDir.getAbsolutePath(), sortedImages, extractionConfig, promptList);
            return ResponseEntity.ok("Extraction started for " + original);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Extraction error: " + e.getMessage());
        }
    }

    @GetMapping("/simple-extraction-threads")
    public int getSimpleExtractionThreads() {
        return appConfig.getSimpleExtractionThreads();
    }

    @PostMapping("/simple-extraction-threads")
    public void setSimpleExtractionThreads(@RequestBody Map<String, Object> body) {
        if (body.containsKey("threads")) {
            int threads = (int) body.get("threads");
            appConfig.setSimpleExtractionThreads(threads);
        }
    }

    // Trigger simple per-page extraction for a converted PDF (by UUID)
    @PostMapping("/extract-simple")
    public ResponseEntity<?> extractSimplePerPage(@RequestBody Map<String, Object> body, HttpSession session) {
        String uuid = (String) body.get("uuid");
        String sessionId = session.getId();
        PdfSourceDataSource dataSource = pdfService.getDataSourceForSession(sessionId);
        String original = dataSource.getOriginalFilename(uuid);
        if (original == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
        // Find stored filename by UUID
        File sessionDir = pdfService.getSessionDir(sessionId);
        File[] files = sessionDir.listFiles((dir, name) -> name.startsWith(uuid + "."));
        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stored file not found");
        }
        String storedFilename = files[0].getName();
        // Output folder named after the stored UUID (normalized file name, without extension)
        String baseName = storedFilename;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
        File outputDir = new File(sessionDir, baseName);
        if (!outputDir.exists()) outputDir.mkdirs();
        // List all PNG images in outputDir (sorted by page)
        File[] imageFiles = outputDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (imageFiles == null || imageFiles.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No images found for extraction");
        }
        List<File> sortedImages = Arrays.asList(imageFiles);
        sortedImages.sort(Comparator.comparing(File::getName));
        try {
            extractionPipelineService.runSimplePerPageExtraction(outputDir.getAbsolutePath(), sortedImages, appConfig.getSimpleExtractionThreads());
            return ResponseEntity.ok("Simple per-page extraction started for " + original);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Extraction error: " + e.getMessage());
        }
    }

    // New endpoint: combine per-page results and save to MongoDB Atlas
    @PostMapping("/save-extracted-data")
    public ResponseEntity<?> saveExtractedData(@RequestBody Map<String, Object> body, HttpSession session) {
        String uuid = (String) body.get("uuid"); // Example: deec1cc5-8f42-46ac-8b76-6ce9dbab561f
        String sessionId = session.getId();
        PdfSourceDataSource dataSource = pdfService.getDataSourceForSession(sessionId);
        String original = dataSource.getOriginalFilename(uuid);
        if (original == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
        // Find stored filename by UUID
        File sessionDir = pdfService.getSessionDir(sessionId);
        File[] files = sessionDir.listFiles((dir, name) -> name.startsWith(uuid + "."));
        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stored file not found");
        }
        String storedFilename = files[0].getName();
        // Output folder named after the stored UUID (normalized file name, without extension)
        String baseName = storedFilename;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
        File outputDir = new File(sessionDir, baseName);
        if (!outputDir.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No extraction output found");
        }
        try {
            extractionPipelineService.combinePerPageResultsAndSaveToMongo(outputDir.getAbsolutePath(), uuid, mongoDbService);
            return ResponseEntity.ok("Extracted data saved to MongoDB Atlas for " + original);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Save error: " + e.getMessage());
        }
    }

    // Helper: recursively delete files/folders, return count
    private int deleteRecursive(File file) {
        int count = 0;
        try {
            File root = new File(PdfService.ROOT_FOLDER).getCanonicalFile();
            File current = file.getCanonicalFile();
            if (current.isDirectory()) {
                for (File child : current.listFiles()) {
                    count += deleteRecursive(child);
                }
                // Do not delete the root folder itself
                if (!current.equals(root)) {
                    if (current.delete()) count++;
                }
            } else {
                if (current.exists()) {
                    if (current.delete()) count++;
                }
            }
        } catch (IOException e) {
            // Optionally log error
        }
        return count;
    }
}
