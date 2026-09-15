package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicHttpClientTest {

    private LocalLlmServer server;
    private AnthropicHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new LocalLlmServer();
        AiHealingSettings settings = new AiHealingSettings(
                AiProvider.ANTHROPIC, "claude-test", "sk-ant-test", server.baseUrl());
        client = new AnthropicHttpClient(settings);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static String validReply() {
        return "{\"content\":[{\"type\":\"text\",\"text\":\"```css\\\\n#healed\\\\n```\"}],"
                + "\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}";
    }

    @Test
    void parsesUsageWhenFieldsAreAdjacent() {
        String body = "{\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}";

        assertEquals(new TokenUsage(10, 20), AnthropicHttpClient.parseUsage(body));
    }

    @Test
    void parsesUsageWhenCacheTokenFieldsSitBetweenInputAndOutput() {
        String body = "{\"usage\":{\"input_tokens\":10,\"cache_creation_input_tokens\":5,"
                + "\"cache_read_input_tokens\":2,\"output_tokens\":20}}";

        assertEquals(new TokenUsage(10, 20), AnthropicHttpClient.parseUsage(body));
    }

    @Test
    void defaultsToZeroWhenUsageIsMissing() {
        String body = "{\"content\":[]}";

        assertEquals(new TokenUsage(0, 0), AnthropicHttpClient.parseUsage(body));
    }

    @Test
    void extractsFirstTextBlock() {
        String body = "{\"content\":[{\"type\":\"text\",\"text\":\"```css\\\\n#logo\\\\n```\"}]}";

        assertTrue(AnthropicHttpClient.firstTextBlock(body).isPresent());
        assertEquals("```css\\n#logo\\n```", AnthropicHttpClient.firstTextBlock(body).orElseThrow());
    }

    @Test
    void buildsWellFormedRequestBody() {
        server.respond(200, validReply());

        client.send("say \"hi\"", "page is 100% loaded");

        String body = server.lastRequestBody();
        assertTrue(body.contains("\"model\":\"claude-test\""), "model in body: " + body);
        assertTrue(body.contains("\"max_tokens\":256"), "max_tokens in body: " + body);
        // Escaped BEFORE formatting: quotes become \" and the % survives .formatted() untouched.
        assertTrue(body.contains("\"system\":\"say \\\"hi\\\"\""), "escaped system in body: " + body);
        assertTrue(body.contains("page is 100% loaded"), "percent in body: " + body);
    }

    @Test
    void returnsSelectorAndUsageFromLiveResponse() {
        server.respond(200, validReply());

        LlmMessagesClient.LlmResponse response = client.send("system", "user");

        assertEquals("```css\\n#healed\\n```", response.text());
        assertEquals(new TokenUsage(10, 20), response.usage());
    }

    @Test
    void throwsOnNon200WithoutRetrying() {
        server.respond(400, "{\"error\":\"bad request\"}");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.send("system", "user"));

        assertTrue(thrown.getMessage().contains("400"), "message: " + thrown.getMessage());
        assertEquals(1, server.requestCount(), "4xx must fail fast, not retry");
    }

    @Test
    void retriesOn429ThenSucceeds() {
        server.respondInSequence(
                new LocalLlmServer.Response(429, "{\"error\":\"rate limited\"}"),
                new LocalLlmServer.Response(200, validReply()));

        LlmMessagesClient.LlmResponse response = client.send("system", "user");

        assertEquals("```css\\n#healed\\n```", response.text());
        assertEquals(2, server.requestCount());
    }

    @Test
    void givesUpAfterMaxAttemptsOnPersistent5xx() {
        server.respondInSequence(
                new LocalLlmServer.Response(503, "unavailable"),
                new LocalLlmServer.Response(503, "unavailable"),
                new LocalLlmServer.Response(503, "unavailable"),
                new LocalLlmServer.Response(200, validReply()));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.send("system", "user"));

        assertTrue(thrown.getMessage().contains("503"), "message: " + thrown.getMessage());
        assertEquals(LlmHttp.MAX_ATTEMPTS, server.requestCount(), "must not retry forever");
    }

    @Test
    void restoresInterruptFlagWhenInterrupted() throws InterruptedException {
        server.delayResponses(10_000);
        server.respond(200, validReply());

        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interruptedWhenCaught = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try {
                client.send("system", "user");
            } catch (RuntimeException e) {
                thrown.set(e);
                interruptedWhenCaught.set(Thread.currentThread().isInterrupted());
            }
        });
        caller.start();
        Thread.sleep(500);
        caller.interrupt();
        caller.join(10_000);

        assertInstanceOf(IllegalStateException.class, thrown.get());
        assertTrue(interruptedWhenCaught.get(), "interrupt flag must be restored, not swallowed");
    }
}
