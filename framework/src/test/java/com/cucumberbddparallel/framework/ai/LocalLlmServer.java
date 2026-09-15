package com.cucumberbddparallel.framework.ai;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A tiny in-JVM HTTP stub for the LLM client tests - no mocks, no extra dependencies,
 * just {@code com.sun.net.httpserver} from the JDK. Script the responses it should serve
 * ({@link #respond} for one, {@link #respondInSequence} for several), then inspect what
 * the client actually sent via {@link #lastRequestBody()} and {@link #requestCount()}.
 */
final class LocalLlmServer implements AutoCloseable {

    record Response(int status, String body) {
    }

    private final HttpServer server;
    private final Deque<Response> scripted = new ArrayDeque<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>("");
    private volatile long responseDelayMillis;

    LocalLlmServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (responseDelayMillis > 0) {
                try {
                    Thread.sleep(responseDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Response response;
            synchronized (scripted) {
                response = scripted.isEmpty() ? new Response(200, "{}") : scripted.poll();
            }
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void respond(int status, String body) {
        synchronized (scripted) {
            scripted.clear();
            scripted.add(new Response(status, body));
        }
    }

    void respondInSequence(Response... responses) {
        synchronized (scripted) {
            scripted.clear();
            for (Response response : responses) {
                scripted.add(response);
            }
        }
    }

    void delayResponses(long millis) {
        responseDelayMillis = millis;
    }

    int requestCount() {
        return requestCount.get();
    }

    String lastRequestBody() {
        return lastRequestBody.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
