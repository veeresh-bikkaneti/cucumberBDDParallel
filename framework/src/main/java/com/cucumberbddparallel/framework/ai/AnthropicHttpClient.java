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

final class AnthropicHttpClient implements ClaudeMessagesClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final Pattern FIRST_TEXT_BLOCK =
            Pattern.compile("\"type\"\\s*:\\s*\"text\"\\s*,\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern USAGE_BLOCK = Pattern.compile(
            "\"usage\"\\s*:\\s*\\{\\s*\"input_tokens\"\\s*:\\s*(\\d+)\\s*,\\s*\"output_tokens\"\\s*:\\s*(\\d+)");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public ClaudeResponse send(String system, String userMessage) {
        String requestBody = "{"
                + "\"model\":\"" + JsonEscaping.escape(AiConfig.model()) + "\","
                + "\"max_tokens\":256,"
                + "\"system\":\"" + JsonEscaping.escape(system) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + JsonEscaping.escape(userMessage) + "\"}]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .header("x-api-key", System.getenv("ANTHROPIC_API_KEY"))
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        String responseBody;
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Claude API returned HTTP " + response.statusCode() + ": " + response.body());
            }
            responseBody = response.body();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to call Claude API for locator healing", e);
        }

        String text = firstTextBlock(responseBody)
                .orElseThrow(() -> new IllegalStateException("Claude returned no selector suggestion"));
        return new ClaudeResponse(text, parseUsage(responseBody));
    }

    private static Optional<String> firstTextBlock(String responseJson) {
        Matcher matcher = FIRST_TEXT_BLOCK.matcher(responseJson);
        return matcher.find() ? Optional.of(JsonEscaping.unescape(matcher.group(1))) : Optional.empty();
    }

    private static TokenUsage parseUsage(String responseJson) {
        Matcher matcher = USAGE_BLOCK.matcher(responseJson);
        if (!matcher.find()) {
            return new TokenUsage(0, 0);
        }
        return new TokenUsage(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }
}
