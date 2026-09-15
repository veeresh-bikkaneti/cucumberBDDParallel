package com.cucumberbddparallel.example.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Serves the example-tests HTML fixtures over HTTP on an ephemeral localhost port.
 *
 * This is what makes the suite hermetic: instead of driving live google.com (geo redirects,
 * consent banners, markup drift - all different per run and per machine), the tests drive
 * these tiny local pages. It uses {@code com.sun.net.httpserver} straight from the JDK, so
 * it adds zero dependencies.
 *
 * Fixtures live under {@code src/test/resources/fixtures} and are served from the classpath,
 * so this works no matter which directory Maven was launched from.
 */
public final class FixtureServer {

    private final HttpServer server;
    private final int port;

    private FixtureServer(HttpServer server) {
        this.server = server;
        this.port = server.getAddress().getPort();
    }

    /** Starts the server on {@code 127.0.0.1} with an OS-assigned (ephemeral) port. */
    public static FixtureServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Map<String, String> pages = Map.of(
                "/home.html", "/fixtures/home.html",
                "/search.html", "/fixtures/search.html");
        for (Map.Entry<String, String> page : pages.entrySet()) {
            byte[] body = readFixture(page.getValue());
            server.createContext(page.getKey(), exchange -> respond(exchange, body));
        }
        server.start();
        return new FixtureServer(server);
    }

    /** e.g. {@code http://127.0.0.1:52341} - no trailing slash. */
    public String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    public void stop() {
        server.stop(0);
    }

    private static byte[] readFixture(String resourcePath) throws IOException {
        try (InputStream in = FixtureServer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Fixture not found on the test classpath: " + resourcePath);
            }
            return in.readAllBytes();
        }
    }

    private static void respond(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
