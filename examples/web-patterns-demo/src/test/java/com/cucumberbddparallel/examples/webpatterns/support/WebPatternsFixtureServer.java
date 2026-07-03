package com.cucumberbddparallel.examples.webpatterns.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

/**
 * Local fixture host for tables, drag-drop, upload/download, PDF, QR, and OCR assets.
 */
public final class WebPatternsFixtureServer {

    public static final String QR_PAYLOAD = "BUILD-OK-42";
    public static final String OCR_EXPECTED_TEXT = "INVOICE QA-2026";

    private final HttpServer server;
    private final int port;
    private final Path fixturesRoot;
    private final AtomicReference<String> lastUploadedFileName = new AtomicReference<>("");

    private WebPatternsFixtureServer(HttpServer server, int port, Path fixturesRoot) {
        this.server = server;
        this.port = port;
        this.fixturesRoot = fixturesRoot;
    }

    public static WebPatternsFixtureServer start(Path fixturesRoot) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        WebPatternsFixtureServer host = new WebPatternsFixtureServer(server, 0, fixturesRoot);
        host.registerRoutes(server);
        server.start();
        int boundPort = server.getAddress().getPort();
        return new WebPatternsFixtureServer(server, boundPort, fixturesRoot);
    }

    private void registerRoutes(HttpServer server) throws Exception {
        Map<String, String> htmlRoutes = Map.of(
                "/tables.html", "tables.html",
                "/drag-drop.html", "drag-drop.html",
                "/upload.html", "upload.html",
                "/qr.html", "qr.html"
        );
        for (Map.Entry<String, String> route : htmlRoutes.entrySet()) {
            byte[] body = Files.readAllBytes(fixturesRoot.resolve(route.getValue()));
            server.createContext(route.getKey(), exchange -> respond(exchange, 200, "text/html; charset=utf-8", body));
        }

        server.createContext("/download/report.txt", exchange -> {
            byte[] body = "Parallel BDD evidence export\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"report.txt\"");
            respond(exchange, 200, "text/plain; charset=utf-8", body);
        });

        byte[] samplePdf = buildSamplePdf();
        server.createContext("/sample.pdf", exchange ->
                respond(exchange, 200, "application/pdf", samplePdf));

        byte[] qrPng = buildQrPng(QR_PAYLOAD);
        server.createContext("/qr.png", exchange ->
                respond(exchange, 200, "image/png", qrPng));

        byte[] ocrPng = buildOcrPng(OCR_EXPECTED_TEXT);
        server.createContext("/ocr.png", exchange ->
                respond(exchange, 200, "image/png", ocrPng));

        server.createContext("/upload", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "text/plain", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            byte[] body = exchange.getRequestBody().readAllBytes();
            String raw = new String(body, StandardCharsets.UTF_8);
            String fileName = extractFileName(raw);
            lastUploadedFileName.set(fileName);
            byte[] response = ("Uploaded: " + fileName).getBytes(StandardCharsets.UTF_8);
            respond(exchange, 200, "text/plain; charset=utf-8", response);
        });

        server.createContext("/last-upload", exchange -> {
            String name = lastUploadedFileName.get();
            respond(exchange, 200, "text/plain; charset=utf-8", name.getBytes(StandardCharsets.UTF_8));
        });
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port + "/";
    }

    public String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    public void stop() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String extractFileName(String multipartBody) {
        int marker = multipartBody.indexOf("filename=\"");
        if (marker < 0) {
            return "unknown";
        }
        int start = marker + "filename=\"".length();
        int end = multipartBody.indexOf('"', start);
        if (end < 0) {
            return "unknown";
        }
        return multipartBody.substring(start, end);
    }

    private static byte[] buildQrPng(String payload) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private static byte[] buildOcrPng(String text) throws IOException {
        BufferedImage image = new BufferedImage(420, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 420, 120);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(text, 24, 72);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] buildSamplePdf() {
        String pdf = """
                %PDF-1.4
                1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
                2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
                3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 400 200] /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj
                4 0 obj<< /Length 55 >>stream
                BT /F1 18 Tf 50 120 Td (Invoice QA-2026) Tj ET
                endstream
                endobj
                5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj
                xref
                0 6
                0000000000 65535 f
                0000000009 00000 n
                0000000058 00000 n
                0000000115 00000 n
                0000000264 00000 n
                0000000371 00000 n
                trailer<< /Root 1 0 R /Size 6 >>
                startxref
                447
                %%EOF
                """;
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }
}