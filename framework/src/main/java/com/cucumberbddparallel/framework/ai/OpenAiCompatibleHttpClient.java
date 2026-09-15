package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI Chat Completions API — also works for Ollama and other BYOK endpoints that expose
 * {@code /v1/chat/completions}.
 */
final class OpenAiCompatibleHttpClient implements LlmMessagesClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleHttpClient.class);

    // Matches "content":"..." inside the message object. We don't regex the whole thing in
    // one shot: responses can carry nested objects (e.g. tool_calls) between "message" and
    // "content", and a single "[^}]*" pattern would silently stop at the first nested
    // closing brace and miss the content entirely. Instead we brace-scan to the message
    // object's end first, then look for the content field inside it.
    private static final Pattern CONTENT_FIELD =
            Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern PROMPT_TOKENS = Pattern.compile("\"prompt_tokens\"\\s*:\\s*(\\d+)");
    private static final Pattern COMPLETION_TOKENS = Pattern.compile("\"completion_tokens\"\\s*:\\s*(\\d+)");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AiHealingSettings settings;

    OpenAiCompatibleHttpClient() {
        this(AiConfig.resolvedSettings());
    }

    // Package-private so tests can point the client at a local stub server.
    OpenAiCompatibleHttpClient(AiHealingSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public LlmResponse send(String system, String userMessage) {
        // Values are escaped BEFORE formatting, so a % in the page HTML can never be
        // mistaken for a format specifier - only the template's own %s placeholders count.
        String requestBody = """
                {"model":"%s","max_tokens":256,"messages":[\
                {"role":"system","content":"%s"},\
                {"role":"user","content":"%s"}]}"""
                .formatted(JsonEscaping.escape(settings.model()),
                        JsonEscaping.escape(system),
                        JsonEscaping.escape(userMessage));

        String apiUrl = settings.baseUrl() + "/chat/completions";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(LlmHttp.requestTimeout())
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        settings.bearerToken()
                .ifPresent(token -> builder.header("Authorization", "Bearer " + token));

        String responseBody;
        try {
            HttpResponse<String> response = LlmHttp.sendWithRetry(CLIENT, builder.build());
            if (response.statusCode() != 200) {
                LOG.warn("LLM API returned HTTP {} from {}", response.statusCode(), apiUrl);
                throw new IllegalStateException(
                        "LLM API returned HTTP " + response.statusCode() + " from " + apiUrl + ": " + response.body());
            }
            responseBody = response.body();
        } catch (InterruptedException e) {
            // Restore the interrupt flag: swallowing it would leave the test thread
            // thinking it was never interrupted, and the next blocking call would hang.
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling LLM API for locator healing at " + apiUrl, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call LLM API for locator healing at " + apiUrl, e);
        }

        String text = messageContent(responseBody)
                .orElseThrow(() -> new IllegalStateException("LLM returned no selector suggestion"));
        return new LlmResponse(text, parseUsage(responseBody));
    }

    static Optional<String> messageContent(String responseJson) {
        return messageObject(responseJson)
                .flatMap(message -> {
                    Matcher content = CONTENT_FIELD.matcher(message);
                    return content.find()
                            ? Optional.of(JsonEscaping.unescape(content.group(1)))
                            : Optional.empty();
                });
    }

    /**
     * Extracts the {@code {...}} object that is the value of the first {@code "message"}
     * key, tracking brace depth (and skipping over string literals) so nested objects
     * don't cut the match short.
     */
    private static Optional<String> messageObject(String responseJson) {
        int key = responseJson.indexOf("\"message\"");
        if (key < 0) {
            return Optional.empty();
        }
        int open = responseJson.indexOf('{', key + "\"message\"".length());
        if (open < 0) {
            return Optional.empty();
        }
        int depth = 0;
        for (int i = open; i < responseJson.length(); i++) {
            char c = responseJson.charAt(i);
            if (c == '"') {
                i = skipStringLiteral(responseJson, i);
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return Optional.of(responseJson.substring(open, i + 1));
                }
            }
        }
        return Optional.empty();
    }

    private static int skipStringLiteral(String json, int quoteIndex) {
        int i = quoteIndex + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        return json.length();
    }

    static TokenUsage parseUsage(String responseJson) {
        return new TokenUsage(firstIntGroup(PROMPT_TOKENS, responseJson), firstIntGroup(COMPLETION_TOKENS, responseJson));
    }

    private static int firstIntGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
