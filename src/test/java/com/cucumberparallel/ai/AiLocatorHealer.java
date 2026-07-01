package com.cucumberparallel.ai;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
 * Asks Claude for a replacement CSS selector when a page's declared locator can no
 * longer find its element (e.g. after a markup change), then retries the lookup once.
 * Calls the Anthropic Messages API directly over java.net.http — no vendor SDK.
 */
final class AiLocatorHealer {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_PAGE_SOURCE_CHARS = 12_000;
    private static final Pattern CODE_FENCE = Pattern.compile("```(?:css)?\\s*(.+?)\\s*```", Pattern.DOTALL);
    private static final Pattern FIRST_TEXT_BLOCK =
            Pattern.compile("\"type\"\\s*:\\s*\"text\"\\s*,\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private AiLocatorHealer() {
    }

    static WebElement heal(WebDriver driver, String elementDescription, NoSuchElementException cause) {
        String selector = suggestSelector(driver.getPageSource(), elementDescription);
        try {
            return driver.findElement(By.cssSelector(selector));
        } catch (NoSuchElementException healingFailed) {
            cause.addSuppressed(healingFailed);
            throw cause;
        }
    }

    private static String suggestSelector(String pageSource, String elementDescription) {
        String system = "You repair broken Selenium locators. Given a page's HTML and a description of "
                + "the element that could no longer be found, reply with exactly one CSS selector "
                + "that matches the intended element, wrapped in a ``` code fence and nothing else.";
        String userMessage = "Element: " + elementDescription + "\n\nPage HTML:\n" + truncate(pageSource);

        String requestBody = "{"
                + "\"model\":\"" + escapeJson(AiConfig.model()) + "\","
                + "\"max_tokens\":256,"
                + "\"system\":\"" + escapeJson(system) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}]"
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

        Matcher fenced = CODE_FENCE.matcher(text);
        return (fenced.find() ? fenced.group(1) : text).trim();
    }

    private static Optional<String> firstTextBlock(String responseJson) {
        Matcher matcher = FIRST_TEXT_BLOCK.matcher(responseJson);
        return matcher.find() ? Optional.of(unescapeJson(matcher.group(1))) : Optional.empty();
    }

    private static String truncate(String html) {
        return html.length() > MAX_PAGE_SOURCE_CHARS ? html.substring(0, MAX_PAGE_SOURCE_CHARS) : html;
    }

    private static String escapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String unescapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(switch (next) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> next;
                });
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
