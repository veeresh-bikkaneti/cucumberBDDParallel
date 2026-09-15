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

class OpenAiCompatibleHttpClientTest {

    private LocalLlmServer server;
    private OpenAiCompatibleHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new LocalLlmServer();
        AiHealingSettings settings = new AiHealingSettings(
                AiProvider.OPENAI, "gpt-test", "sk-openai-test", server.baseUrl());
        client = new OpenAiCompatibleHttpClient(settings);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static String validReply() {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"```css\\\\n#healed\\\\n```\"}}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20}}";
    }

    @Test
    void extractsSelectorFromMessageContent() {
        String body = """
                {
                  "choices":[{"message":{"role":"assistant","content":"```css\\\\n#logo\\\\n```"}}],
                  "usage":{"prompt_tokens":1,"completion_tokens":2}
                }
                """;

        assertTrue(OpenAiCompatibleHttpClient.messageContent(body).isPresent());
        assertEquals("```css\\n#logo\\n```", OpenAiCompatibleHttpClient.messageContent(body).orElseThrow());
    }

    @Test
    void extractsContentWhenNestedObjectsSitBetweenMessageAndContent() {
        // tool_calls (with their own braces) must not cut the message-object scan short.
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"tool_calls\":[{\"id\":\"1\",\"function\":{\"name\":\"f\",\"arguments\":\"{}\"}}],"
                + "\"content\":\"#healed\"}}]}";

        assertEquals("#healed", OpenAiCompatibleHttpClient.messageContent(body).orElseThrow());
    }

    @Test
    void parsesMessageContentAndUsage() {
        String body = """
                {
                  "choices":[{"message":{"role":"assistant","content":"```css\\\\ninput[name=q]\\\\n```"}}],
                  "usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}
                }
                """;

        assertEquals(new TokenUsage(10, 20), OpenAiCompatibleHttpClient.parseUsage(body));
    }

    @Test
    void usageDefaultsToZeroWhenMissing() {
        String body = "{\"choices\":[{\"message\":{\"content\":\"#logo\"}}]}";

        assertEquals(new TokenUsage(0, 0), OpenAiCompatibleHttpClient.parseUsage(body));
    }

    @Test
    void buildsWellFormedRequestBody() {
        server.respond(200, validReply());

        client.send("say \"hi\"", "page is 100% loaded");

        String body = server.lastRequestBody();
        assertTrue(body.contains("\"model\":\"gpt-test\""), "model in body: " + body);
        assertTrue(body.contains("\"max_tokens\":256"), "max_tokens in body: " + body);
        assertTrue(body.contains("\"role\":\"system\""), "system message in body: " + body);
        // Escaped BEFORE formatting: quotes become \" and the % survives .formatted() untouched.
        assertTrue(body.contains("say \\\"hi\\\""), "escaped system in body: " + body);
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
        server.respond(401, "{\"error\":\"bad key\"}");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.send("system", "user"));

        assertTrue(thrown.getMessage().contains("401"), "message: " + thrown.getMessage());
        assertEquals(1, server.requestCount(), "4xx must fail fast, not retry");
    }

    @Test
    void retriesOn5xxThenSucceeds() {
        server.respondInSequence(
                new LocalLlmServer.Response(500, "boom"),
                new LocalLlmServer.Response(200, validReply()));

        LlmMessagesClient.LlmResponse response = client.send("system", "user");

        assertEquals("```css\\n#healed\\n```", response.text());
        assertEquals(2, server.requestCount());
    }

    @Test
    void givesUpAfterMaxAttemptsOnPersistent429() {
        server.respondInSequence(
                new LocalLlmServer.Response(429, "slow down"),
                new LocalLlmServer.Response(429, "slow down"),
                new LocalLlmServer.Response(429, "slow down"),
                new LocalLlmServer.Response(200, validReply()));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.send("system", "user"));

        assertTrue(thrown.getMessage().contains("429"), "message: " + thrown.getMessage());
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
