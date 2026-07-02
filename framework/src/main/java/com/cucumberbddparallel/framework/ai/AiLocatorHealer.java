package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.CostLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The actual "ask Claude for a new selector" logic, kept deliberately small.
 *
 * This class is just the orchestrator - it doesn't know how to talk HTTP
 * ({@link AnthropicHttpClient} does that), how to escape JSON ({@link JsonEscaping}),
 * or how to pull a selector out of a chat reply ({@link SelectorResponseParser}). It
 * used to be one big class that did all of that itself, which made it painful to test
 * (you'd need a live network call just to check that a CSS selector gets trimmed
 * correctly). Splitting it up means every piece can be unit tested on its own, and this
 * class is left doing exactly one thing: send the page, get a selector, try it once.
 */
final class AiLocatorHealer {

    // Google's homepage HTML alone is well past this, so we're always truncating in
    // practice - that's fine, the search bar and logo we care about are near the top.
    private static final int MAX_PAGE_SOURCE_CHARS = 12_000;
    private static final ClaudeMessagesClient CLIENT = new AnthropicHttpClient();

    private AiLocatorHealer() {
    }

    /**
     * Called from {@link AiElementLocatorFactory} after the normal Selenium lookup has
     * already failed once. We ask Claude for a selector, try it, and if that ALSO fails
     * we give up - re-throwing the original exception (not the new one) so the test
     * failure still points at the real problem, with the healing attempt attached as a
     * suppressed exception for anyone debugging later.
     */
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

        ClaudeMessagesClient.ClaudeResponse response = CLIENT.send(system, userMessage);
        // Log the cost even if the suggested selector turns out to be wrong - you paid for
        // the API call either way, and knowing that helps you notice if healing is
        // firing way more often than expected (usually a sign your locators are stale).
        CostLogger.logHealCall(elementDescription, AiConfig.model(), response.usage());
        return SelectorResponseParser.selectorFrom(response.text());
    }

    // Keeps the request small and (more importantly) keeps the cost predictable - without
    // a cap, one enormous page could blow the token budget for a single healing call.
    private static String truncate(String html) {
        return html.length() > MAX_PAGE_SOURCE_CHARS ? html.substring(0, MAX_PAGE_SOURCE_CHARS) : html;
    }
}
