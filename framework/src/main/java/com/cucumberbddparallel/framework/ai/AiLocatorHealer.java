package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.CostLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Asks Claude for a replacement CSS selector when a page's declared locator can no
 * longer find its element (e.g. after a markup change), then retries the lookup once.
 */
final class AiLocatorHealer {

    private static final int MAX_PAGE_SOURCE_CHARS = 12_000;
    private static final ClaudeMessagesClient CLIENT = new AnthropicHttpClient();

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

        ClaudeMessagesClient.ClaudeResponse response = CLIENT.send(system, userMessage);
        CostLogger.logHealCall(elementDescription, AiConfig.model(), response.usage());
        return SelectorResponseParser.selectorFrom(response.text());
    }

    private static String truncate(String html) {
        return html.length() > MAX_PAGE_SOURCE_CHARS ? html.substring(0, MAX_PAGE_SOURCE_CHARS) : html;
    }
}
