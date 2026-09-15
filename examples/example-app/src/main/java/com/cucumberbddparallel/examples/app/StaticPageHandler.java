package com.cucumberbddparallel.examples.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Serves one static HTML page from the classpath ({@code src/main/resources/app-pages}).
 * The page bytes are read once at registration time, so request handling is just a memory
 * copy - no disk I/O per request.
 */
final class StaticPageHandler implements HttpHandler {

    private final byte[] body;

    /**
     * @param resourcePath classpath resource, e.g. {@code /app-pages/index.html}
     * @throws IOException if the resource is missing from the classpath
     */
    StaticPageHandler(String resourcePath) throws IOException {
        try (InputStream in = StaticPageHandler.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Page not found on the classpath: " + resourcePath);
            }
            this.body = in.readAllBytes();
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Only GET is meaningful for these demo pages.
        if (!ExampleAppServer.requireGet(exchange)) {
            return;
        }
        ExampleAppServer.respond(exchange, body, "text/html; charset=utf-8");
    }
}
