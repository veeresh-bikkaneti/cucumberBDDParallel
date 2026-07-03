package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.examples.webpatterns.support.WebPatternsFixtureServer;
import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requires Tesseract OCR binary (Docker image installs it). Run with {@code -Pocr-demo}.
 */
class OcrValidationTest {

    private WebPatternsFixtureServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = WebPatternsFixtureServer.start(WebPatternsSupport.fixturesRoot());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @EnabledIf("tesseractAvailable")
    void ocrReadsInvoiceLabelFromPng() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(server.url("/ocr.png"))).GET().build();
        byte[] pngBytes = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();

        Path tempImage = Files.createTempFile("ocr-demo", ".png");
        Files.write(tempImage, pngBytes);
        try {
            Tesseract tesseract = new Tesseract();
            String dataPath = System.getenv("TESSDATA_PREFIX");
            if (dataPath != null && !dataPath.isBlank()) {
                tesseract.setDatapath(dataPath);
            }
            String text = tesseract.doOCR(tempImage.toFile());
            assertTrue(text.toUpperCase().contains("INVOICE"),
                    "OCR text was: " + text);
        } finally {
            Files.deleteIfExists(tempImage);
        }
    }

    static boolean tesseractAvailable() {
        String[] candidates = {"tesseract", "/usr/bin/tesseract", "C:\\Program Files\\Tesseract-OCR\\tesseract.exe"};
        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true)
                        .start();
                if (process.waitFor() == 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // try next candidate
            }
        }
        return false;
    }
}