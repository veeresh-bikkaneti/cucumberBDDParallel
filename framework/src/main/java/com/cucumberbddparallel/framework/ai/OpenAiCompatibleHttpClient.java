package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI Chat Completions API — also works for Ollama and other BYOK endpoints that expose
 * {@code /v1/chat/completions}.
 */
final class OpenAiCompatibleHttpClient implements LlmMessagesClient {

    private static final Pattern MESSAGE_CONTENT = Pattern.compile(
            "\"message\"\\s*:\\s*\\{[^}]*\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern PROMPT_TOKENS = Pattern.compile("\"prompt_tokens\"\\s*:\\s*(\\d+)");
    private static final Pattern COMPLETION_TOKENS = Pattern.compile("\"completion_tokens\"\\s*:\\s*(\\d+)");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public LlmResponse send(String system, String userMessage) {
        String requestBody = "{"
                + "\"model\":\"" + JsonEscaping.escape(AiConfig.model()) + "\","
                + "\"max_tokens\":256,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + JsonEscaping.escape(system) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + JsonEscaping.escape(userMessage) + "\"}"
                + "]"
                + "}";

        String apiUrl = AiConfig.baseUrl() + "/chat/completions";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        String apiKey = AiConfig.apiKey();
        if (apiKey != null && !apiKey.isBlank() && !"ollama".equals(apiKey)) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        String responseBody;
        try {
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "LLM API returned HTTP " + response.statusCode() + " from " + apiUrl + ": " + response.body());
            }
            responseBody = response.body();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to call LLM API for locator healing at " + apiUrl, e);
        }

        String text = firstMessageContent(responseBody)
                .orElseThrow(() -> new IllegalStateException("LLM returned no selector suggestion"));
        return new LlmResponse(text, parseUsage(responseBody));
    }

    private static Optional<String> firstMessageContent(String responseJson) {
        Matcher matcher = MESSAGE_CONTENT.matcher(responseJson);
        return matcher.find() ? Optional.of(JsonEscaping.unescape(matcher.group(1))) : Optional.empty();
    }

    static TokenUsage parseUsage(String responseJson) {
        return new TokenUsage(firstIntGroup(PROMPT_TOKENS, responseJson), firstIntGroup(COMPLETION_TOKENS, responseJson));
    }

    private static int firstIntGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}