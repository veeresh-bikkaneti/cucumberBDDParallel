package com.cucumberbddparallel.examples.aihealing.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal OpenAI-compatible {@code /v1/chat/completions} stub for deterministic demos.
 */
public final class MockLlmServer {

    private final HttpServer server;
    private final int port;
    private final AtomicBoolean called;

    private MockLlmServer(HttpServer server, int port, AtomicBoolean called) {
        this.server = server;
        this.port = port;
        this.called = called;
    }

    public static MockLlmServer start(String healedSelector) throws IOException {
        AtomicBoolean called = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body = ("""
                {
                  "choices":[{"message":{"role":"assistant","content":"```css\\n%s\\n```"}}],
                  "usage":{"prompt_tokens":42,"completion_tokens":7,"total_tokens":49}
                }
                """.formatted(healedSelector)).getBytes(StandardCharsets.UTF_8);

        server.createContext("/v1/chat/completions", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                called.set(true);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        server.start();
        return new MockLlmServer(server, server.getAddress().getPort(), called);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port + "/v1";
    }

    public boolean wasCalled() {
        return called.get();
    }

    public void stop() {
        server.stop(0);
    }
}