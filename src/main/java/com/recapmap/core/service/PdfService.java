package com.recapmap.core.service;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import com.recapmap.core.data.PdfSourceDataSource;

@Service
public class PdfService {
    // Configurable global variables
    public static String ROOT_FOLDER = "D:/workspace_recapmap/core-suite-server-files"; // Windows path
    public static int MAX_UPLOAD_SIZE_MB = 200; // Max upload size in MB
    public static int PDF_TO_IMAGE_DPI = 150; // Possible values: 72, 150, 200, 300, etc.
    public static int MAX_CONVERT_THREADS = 20; // Configurable max threads for conversion
    private static ExecutorService convertThreadPool = Executors.newFixedThreadPool(MAX_CONVERT_THREADS);

    // In-memory session-based multi-tenancy
    private final Map<String, PdfSourceDataSource> sessionDataSources = new HashMap<>();

    public PdfSourceDataSource getDataSourceForSession(String sessionId) {
        return sessionDataSources.computeIfAbsent(sessionId, k -> new PdfSourceDataSource());
    }

    public void clearSession(String sessionId) {
        sessionDataSources.remove(sessionId);
    }

    public void clearAllSessions() {
        sessionDataSources.clear();
    }

    // Utility: get session directory
    public File getSessionDir(String sessionId) {
        return new File(ROOT_FOLDER, sessionId);
    }

    // Progress log per session (sessionId -> List of log lines)
    private final Map<String, List<String>> progressLogs = new ConcurrentHashMap<>();

    // Per-session lock objects for conversion
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    // Utility: Sharpen a BufferedImage using a mild kernel (less pixelation)
    public static BufferedImage sharpenImage(BufferedImage image) {
        // Gentler sharpen kernel to avoid pixelation
        float[] kernel = {
            0f,   -0.25f,  0f,
           -0.25f, 2f,   -0.25f,
            0f,   -0.25f,  0f
        };
        Kernel sharpKernel = new Kernel(3, 3, kernel);
        ConvolveOp op = new ConvolveOp(sharpKernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);
    }

    // Utility: Downscale a BufferedImage to target width/height using high-quality scaling
    public static BufferedImage downscaleImage(BufferedImage src, int targetWidth, int targetHeight) {
        BufferedImage out = new BufferedImage(targetWidth, targetHeight, src.getType());
        java.awt.Graphics2D g2d = out.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return out;
    }

    // Convert a PDF file to PNG images (one per page), log progress
    public void convertPdfToImages(String sessionId, String uuid, String storedFilename) {
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            File sessionDir = getSessionDir(sessionId);
            File pdfFile = new File(sessionDir, storedFilename);
            List<String> log = progressLogs.computeIfAbsent(sessionId, k -> new ArrayList<>());
            log.add("Starting conversion for " + storedFilename);
            try (PDDocument document = PDDocument.load(pdfFile)) {
                int pageCount = document.getNumberOfPages();
                String[] imageFilenames = new String[pageCount];
                List<Future<Void>> futures = new ArrayList<>();
                AtomicInteger completed = new AtomicInteger(0);
                // Track per-page status
                Map<Integer, String> pageStatus = new ConcurrentHashMap<>();
                for (int page = 0; page < pageCount; ++page) {
                    final int pageIndex = page;
                    futures.add(convertThreadPool.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            try {
                                PDFRenderer threadRenderer = new PDFRenderer(document);
                                BufferedImage bim = threadRenderer.renderImageWithDPI(pageIndex, PDF_TO_IMAGE_DPI, ImageType.RGB);
                                bim = sharpenImage(bim);
                                String imageName = uuid + "_page" + (pageIndex + 1) + ".png";
                                File imageFile = new File(sessionDir, imageName);
                                ImageIO.write(bim, "png", imageFile);
                                imageFilenames[pageIndex] = imageName;
                                pageStatus.put(pageIndex, "done");
                                synchronized (log) {
                                    log.add("Converted page " + (pageIndex + 1) + "/" + pageCount + " to " + imageName);
                                }
                            } catch (Exception e) {
                                pageStatus.put(pageIndex, "error: " + e.getMessage());
                                synchronized (log) {
                                    log.add("Error converting page " + (pageIndex + 1) + ": " + e.getMessage());
                                }
                            }
                            completed.incrementAndGet();
                            return null;
                        }
                    }));
                }
                // Polling thread for real-time progress
                Thread progressThread = new Thread(() -> {
                    while (completed.get() < pageCount) {
                        int done = completed.get();
                        synchronized (log) {
                            log.add("Progress: " + done + "/" + pageCount + " pages finished");
                        }
                        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                    }
                });
                progressThread.start();
                // Wait for all pages to finish
                for (Future<Void> f : futures) {
                    try { f.get(); } catch (Exception e) {
                        synchronized (log) {
                            log.add("Error converting page: " + e.getMessage());
                        }
                    }
                }
                progressThread.join();
                getDataSourceForSession(sessionId).addOutputImages(uuid, imageFilenames);
                log.add("Conversion complete for " + storedFilename);
            } catch (Exception e) {
                log.add("Error during conversion: " + e.getMessage());
            }
        }
    }

    // Convert a PDF file to PNG images (one per page), log progress
    public void convertPdfToImages(String sessionId, String uuid, String storedFilename, File outputDir) {
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            File sessionDir = getSessionDir(sessionId);
            File pdfFile = new File(sessionDir, storedFilename);
            List<String> log = progressLogs.computeIfAbsent(sessionId, k -> new ArrayList<>());
            log.add("Starting conversion for " + storedFilename);
            try (PDDocument document = PDDocument.load(pdfFile)) {
                int pageCount = document.getNumberOfPages();
                String[] imageFilenames = new String[pageCount];
                List<Future<Void>> futures = new ArrayList<>();
                AtomicInteger completed = new AtomicInteger(0);
                Map<Integer, String> pageStatus = new ConcurrentHashMap<>();
                for (int page = 0; page < pageCount; ++page) {
                    final int pageIndex = page;
                    futures.add(convertThreadPool.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            try {
                                PDFRenderer threadRenderer = new PDFRenderer(document);
                                BufferedImage bim = threadRenderer.renderImageWithDPI(pageIndex, PDF_TO_IMAGE_DPI, ImageType.RGB);
                                bim = sharpenImage(bim);
                                String imageName = uuid + "_page" + (pageIndex + 1) + ".png";
                                File imageFile = new File(outputDir, imageName);
                                ImageIO.write(bim, "png", imageFile);
                                imageFilenames[pageIndex] = new File(outputDir.getName(), imageName).getPath();
                                pageStatus.put(pageIndex, "done");
                                synchronized (log) {
                                    log.add("Converted page " + (pageIndex + 1) + "/" + pageCount + " to " + imageFile.getPath());
                                }
                            } catch (Exception e) {
                                pageStatus.put(pageIndex, "error: " + e.getMessage());
                                synchronized (log) {
                                    log.add("Error converting page " + (pageIndex + 1) + ": " + e.getMessage());
                                }
                            }
                            completed.incrementAndGet();
                            return null;
                        }
                    }));
                }
                // Polling thread for real-time progress
                Thread progressThread = new Thread(() -> {
                    while (completed.get() < pageCount) {
                        int done = completed.get();
                        synchronized (log) {
                            log.add("Progress: " + done + "/" + pageCount + " pages finished");
                        }
                        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                    }
                });
                progressThread.start();
                // Wait for all pages to finish
                for (Future<Void> f : futures) {
                    try { f.get(); } catch (Exception e) {
                        synchronized (log) {
                            log.add("Error converting page: " + e.getMessage());
                        }
                    }
                }
                progressThread.join();
                getDataSourceForSession(sessionId).addOutputImages(uuid, imageFilenames);
                log.add("Conversion complete for " + storedFilename);
            } catch (Exception e) {
                log.add("Error during conversion: " + e.getMessage());
            }
        }
    }

    // Get progress log for a session
    public List<String> getProgressLog(String sessionId) {
        return progressLogs.getOrDefault(sessionId, new ArrayList<>());
    }

    // Clear progress log for a session
    public void clearProgressLog(String sessionId) {
        progressLogs.remove(sessionId);
    }

    public static synchronized void updateConvertThreadPool(int newThreadCount) {
        if (newThreadCount != MAX_CONVERT_THREADS) {
            convertThreadPool.shutdown();
            try { convertThreadPool.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            MAX_CONVERT_THREADS = newThreadCount;
            convertThreadPool = Executors.newFixedThreadPool(MAX_CONVERT_THREADS);
        }
    }
}
