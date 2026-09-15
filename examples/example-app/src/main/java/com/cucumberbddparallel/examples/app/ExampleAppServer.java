package com.cucumberbddparallel.examples.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A tiny self-contained demo web application used as the application-under-test for the
 * repo's example suites.
 *
 * <p>The server binds {@code 127.0.0.1} on an OS-assigned (ephemeral) port and serves a
 * fixed set of teaching pages - search, tables, drag-and-drop, upload, download, login,
 * dynamic content - all from {@code src/main/resources/app-pages}. It uses
 * {@code com.sun.net.httpserver} straight from the JDK, so this module has zero
 * dependencies.
 *
 * <p>Typical use:
 * <pre>{@code
 * ExampleAppServer app = ExampleAppServer.start();
 * try {
 *     driver.get(app.baseUrl() + "/tables");
 *     ...
 * } finally {
 *     app.stop();
 * }
 * }</pre>
 */
public final class ExampleAppServer {

    private static final String REPORT_TXT = "Example App quarterly report\nTotal: 42\n";

    private final HttpServer server;
    private final int port;

    private ExampleAppServer(HttpServer server) {
        this.server = server;
        this.port = server.getAddress().getPort();
    }

    /**
     * Starts the demo app on {@code 127.0.0.1} with an ephemeral port and registers all
     * routes.
     *
     * @return the running server
     * @throws IOException if the socket cannot be bound or a page resource is missing
     */
    public static ExampleAppServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            Map<String, String> pages = Map.of(
                    "/", "/app-pages/index.html",
                    "/dynamic", "/app-pages/dynamic.html",
                    "/tables", "/app-pages/tables.html",
                    "/drag-drop", "/app-pages/drag-drop.html",
                    "/upload", "/app-pages/upload.html",
                    "/login", "/app-pages/login.html");

            for (Map.Entry<String, String> page : pages.entrySet()) {
                server.createContext(page.getKey(), new StaticPageHandler(page.getValue()));
            }

            server.createContext("/search", ExampleAppServer::handleSearch);
            server.createContext("/download/report.txt",
                    exchange -> {
                        if (requireGet(exchange)) {
                            respond(exchange, REPORT_TXT.getBytes(StandardCharsets.UTF_8),
                                    "text/plain; charset=utf-8");
                        }
                    });
        } catch (IOException | RuntimeException e) {
            // The socket is already bound at this point - don't leak it if a page
            // resource turns out to be missing.
            server.stop(0);
            throw e;
        }

        server.start();
        return new ExampleAppServer(server);
    }

    /**
     * The base URL of the running app, e.g. {@code http://127.0.0.1:52341} - no trailing
     * slash.
     */
    public String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    /** Stops the server immediately. */
    public void stop() {
        server.stop(0);
    }

    private static void handleSearch(HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }
        String query = queryParam(exchange.getRequestURI().getRawQuery(), "q");
        String body = SearchPageRenderer.render(query);
        respond(exchange, body.getBytes(StandardCharsets.UTF_8), "text/html; charset=utf-8");
    }

    private static String queryParam(String rawQuery, String name) {
        if (rawQuery == null) {
            return "";
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    static void respond(HttpExchange exchange, byte[] body, String contentType) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /**
     * Answers non-GET requests with 405. Returns {@code true} when the request is a GET
     * and the caller should continue handling it.
     */
    static boolean requireGet(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            return true;
        }
        byte[] message = "Method not allowed".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(405, message.length);
        exchange.getResponseBody().write(message);
        exchange.close();
        return false;
    }
}
