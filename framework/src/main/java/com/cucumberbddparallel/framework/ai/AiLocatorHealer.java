package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.CostLogger;
import com.cucumberbddparallel.framework.ai.cost.TokenUsage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * The actual "ask an LLM for a new selector" logic, kept deliberately small.
 *
 * This class is just the orchestrator - it doesn't know how to talk HTTP
 * ({@link LlmClientFactory} picks the provider client), how to escape JSON ({@link JsonEscaping}),
 * or how to pull a selector out of a chat reply ({@link SelectorResponseParser}). It
 * used to be one big class that did all of that itself, which made it painful to test
 * (you'd need a live network call just to check that a CSS selector gets trimmed
 * correctly). Splitting it up means every piece can be unit tested on its own, and this
 * class is left doing exactly one thing: send the page, get a selector, try it once.
 *
 * The LLM client, model name, and cost reporting are injected (defaulting to the live
 * {@link AiConfig} wiring) so tests can plug in a fake client instead of hitting a real
 * API - see {@code AiLocatorHealerTest}.
 */
final class AiLocatorHealer {

    private static final Logger LOG = LoggerFactory.getLogger(AiLocatorHealer.class);

    // Google's homepage HTML alone is well past this, so we're always truncating in
    // practice - that's fine, the search bar and logo we care about are near the top.
    private static final int MAX_PAGE_SOURCE_CHARS = 12_000;

    /** Reports what one heal call cost; injectable so tests don't touch the real {@link CostLogger}. */
    @FunctionalInterface
    interface HealCostReporter {
        void report(String elementDescription, String model, TokenUsage usage);
    }

    /** A successful heal: the element, plus the selector that found it (the factory caches it). */
    record HealedTarget(WebElement element, String selector) {
    }

    private final Supplier<LlmMessagesClient> clientSupplier;
    private final Supplier<String> modelSupplier;
    private final HealCostReporter costReporter;

    AiLocatorHealer() {
        this(LlmClientFactory::create, AiConfig::model, CostLogger::logHealCall);
    }

    // Package-private for tests: hand it a fake LlmMessagesClient and no network is involved.
    AiLocatorHealer(Supplier<LlmMessagesClient> clientSupplier,
                    Supplier<String> modelSupplier,
                    HealCostReporter costReporter) {
        this.clientSupplier = clientSupplier;
        this.modelSupplier = modelSupplier;
        this.costReporter = costReporter;
    }

    /**
     * Called from {@link AiElementLocatorFactory} after the normal Selenium lookup has
     * already failed once. We ask the LLM for a selector, try it, and if that ALSO fails
     * we give up - re-throwing the original exception (not the new one) so the test
     * failure still points at the real problem, with the healing attempt attached as a
     * suppressed exception for anyone debugging later.
     */
    HealedTarget heal(WebDriver driver, String elementDescription, NoSuchElementException cause) {
        String selector = suggestHealedSelector(driver, elementDescription, cause);
        try {
            WebElement element = driver.findElement(By.cssSelector(selector));
            LOG.debug("Healed '{}' with AI-suggested selector '{}'", elementDescription, selector);
            return new HealedTarget(element, selector);
        } catch (WebDriverException healingFailed) {
            // WebDriverException, not just NoSuchElementException: a dead session or a
            // driver hiccup during the healing lookup is still "healing didn't work," and
            // the caller deserves the original failure, not this one.
            LOG.warn("AI-suggested selector '{}' for '{}' did not match anything",
                    selector, elementDescription, healingFailed);
            cause.addSuppressed(healingFailed);
            throw cause;
        }
    }

    // The LLM call itself can fail in all sorts of ways (bad key, rate limit exhausted
    // after retries, garbage response) - none of that changes the fact that the element
    // wasn't found, so it gets suppressed onto the original exception and the original
    // is rethrown, exactly like a failed healing lookup above.
    private String suggestHealedSelector(WebDriver driver, String elementDescription, NoSuchElementException cause) {
        String selector;
        try {
            selector = suggestSelector(driver.getPageSource(), elementDescription);
        } catch (RuntimeException suggestFailed) {
            LOG.warn("LLM call failed while healing '{}'", elementDescription, suggestFailed);
            cause.addSuppressed(suggestFailed);
            throw cause;
        }
        if (selector == null || selector.isBlank() || selector.indexOf('\n') >= 0 || selector.indexOf('\r') >= 0) {
            // A blank or multi-line "selector" is never a real CSS selector - it's the
            // model chatting instead of answering. Don't feed it to By.cssSelector and
            // hope for the best; treat it as a failed heal.
            IllegalArgumentException badSelector =
                    new IllegalArgumentException("AI suggested an unusable selector: '" + selector + "'");
            LOG.warn("Rejecting unusable AI selector for '{}'", elementDescription, badSelector);
            cause.addSuppressed(badSelector);
            throw cause;
        }
        return selector;
    }

    private String suggestSelector(String pageSource, String elementDescription) {
        String system = "You repair broken Selenium locators. Given a page's HTML and a description of "
                + "the element that could no longer be found, reply with exactly one CSS selector "
                + "that matches the intended element, wrapped in a ``` code fence and nothing else.";
        String userMessage = "Element: " + elementDescription + "\n\nPage HTML:\n" + truncate(pageSource);

        LlmMessagesClient.LlmResponse response = clientSupplier.get().send(system, userMessage);
        // Log the cost even if the suggested selector turns out to be wrong - you paid for
        // the API call either way, and knowing that helps you notice if healing is
        // firing way more often than expected (usually a sign your locators are stale).
        costReporter.report(elementDescription, modelSupplier.get(), response.usage());
        return SelectorResponseParser.selectorFrom(response.text());
    }

    // Keeps the request small and (more importantly) keeps the cost predictable - without
    // a cap, one enormous page could blow the token budget for a single healing call.
    // Null-safe: some drivers return null from getPageSource() instead of throwing.
    private static String truncate(String html) {
        if (html == null) {
            return "";
        }
        return html.length() > MAX_PAGE_SOURCE_CHARS ? html.substring(0, MAX_PAGE_SOURCE_CHARS) : html;
    }
}
