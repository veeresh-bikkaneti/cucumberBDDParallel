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
 * Talks to Claude's Messages API directly over {@code java.net.http} - no Anthropic SDK,
 * no extra dependency. For one HTTP call with a tiny JSON body, pulling in a whole SDK
 * felt like more than this needed. If the request payload ever grows past "one system
 * prompt, one user message," it's probably worth switching to the real SDK instead of
 * growing these hand-rolled regexes further.
 *
 * Speaking of regexes: yes, this parses the response JSON with regex instead of a real
 * JSON parser. That's a deliberate trade-off, not an oversight - we only need two things
 * out of a much bigger response (the first text block, and the token counts), and adding
 * a JSON library as a dependency for that felt heavier than it's worth. If we ever need
 * to pull more fields out of the response, switch to a real parser at that point.
 */
final class AnthropicHttpClient implements LlmMessagesClient {

    private static final Logger LOG = LoggerFactory.getLogger(AnthropicHttpClient.class);

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    // Matches the first {"type":"text","text":"..."} block in the response. Claude's replies
    // are a list of content blocks; for our use case (one short prompt, one short answer)
    // there's only ever one, so we don't bother handling multiple blocks.
    private static final Pattern FIRST_TEXT_BLOCK =
            Pattern.compile("\"type\"\\s*:\\s*\"text\"\\s*,\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    // input_tokens and output_tokens are matched independently (not as one combined pattern)
    // because Anthropic's usage block can have other fields (like cache token counts)
    // sitting between them - a single "match both in one shot" regex broke on that.
    private static final Pattern INPUT_TOKENS = Pattern.compile("\"input_tokens\"\\s*:\\s*(\\d+)");
    private static final Pattern OUTPUT_TOKENS = Pattern.compile("\"output_tokens\"\\s*:\\s*(\\d+)");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AiHealingSettings settings;

    AnthropicHttpClient() {
        this(AiConfig.resolvedSettings());
    }

    // Package-private so tests can point the client at a local stub server.
    AnthropicHttpClient(AiHealingSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public LlmResponse send(String system, String userMessage) {
        // Values are escaped BEFORE formatting, so a % in the page HTML can never be
        // mistaken for a format specifier - only the template's own %s placeholders count.
        String requestBody = """
                {"model":"%s","max_tokens":256,"system":"%s","messages":[\
                {"role":"user","content":"%s"}]}"""
                .formatted(JsonEscaping.escape(settings.model()),
                        JsonEscaping.escape(system),
                        JsonEscaping.escape(userMessage));

        String apiUrl = settings.baseUrl() + "/messages";
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(LlmHttp.requestTimeout())
                .header("x-api-key", settings.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        String responseBody;
        try {
            HttpResponse<String> response = LlmHttp.sendWithRetry(CLIENT, request);
            if (response.statusCode() != 200) {
                LOG.warn("Claude API returned HTTP {} from {}", response.statusCode(), apiUrl);
                throw new IllegalStateException(
                        "Claude API returned HTTP " + response.statusCode() + ": " + response.body());
            }
            responseBody = response.body();
        } catch (InterruptedException e) {
            // Restore the interrupt flag: swallowing it would leave the test thread
            // thinking it was never interrupted, and the next blocking call would hang.
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Claude API for locator healing", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Claude API for locator healing", e);
        }

        String text = firstTextBlock(responseBody)
                .orElseThrow(() -> new IllegalStateException("Claude returned no selector suggestion"));
        return new LlmResponse(text, parseUsage(responseBody));
    }

    // Package-private (not private) so tests can call it directly with a handful of sample
    // response bodies instead of needing a live API call to test parsing - same treatment
    // as OpenAiCompatibleHttpClient.messageContent.
    static Optional<String> firstTextBlock(String responseJson) {
        Matcher matcher = FIRST_TEXT_BLOCK.matcher(responseJson);
        return matcher.find() ? Optional.of(JsonEscaping.unescape(matcher.group(1))) : Optional.empty();
    }

    // Package-private (not private) so AnthropicHttpClientTest can call it directly with a
    // handful of sample response bodies instead of needing a live API call to test parsing.
    static TokenUsage parseUsage(String responseJson) {
        return new TokenUsage(firstIntGroup(INPUT_TOKENS, responseJson), firstIntGroup(OUTPUT_TOKENS, responseJson));
    }

    private static int firstIntGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
