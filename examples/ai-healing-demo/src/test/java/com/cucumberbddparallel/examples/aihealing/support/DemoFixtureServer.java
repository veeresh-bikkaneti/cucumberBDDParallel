package com.cucumberbddparallel.examples.aihealing.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves the static HTML fixture on a random local port — no google.com, no network flake.
 */
public final class DemoFixtureServer {

    private final HttpServer server;
    private final int port;

    private DemoFixtureServer(HttpServer server, int port) {
        this.server = server;
        this.port = port;
    }

    public static DemoFixtureServer start(Path htmlFile) throws IOException {
        byte[] body = Files.readAllBytes(htmlFile);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        return new DemoFixtureServer(server, port);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port + "/";
    }

    public void stop() {
        server.stop(0);
    }
}