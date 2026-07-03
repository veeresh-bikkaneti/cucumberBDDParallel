package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.examples.webpatterns.support.WebPatternsFixtureServer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfValidationTest {

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
    void pdfContainsExpectedInvoiceText() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(server.url("/sample.pdf"))).GET().build();
        byte[] pdfBytes = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Invoice QA-2026"), "PDF text was: " + text);
        }
    }
}