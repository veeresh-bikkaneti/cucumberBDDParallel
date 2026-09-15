package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link AiLocatorHealer} with a fake {@link LlmMessagesClient} - no network,
 * no API keys, just a scripted reply per test.
 */
class AiLocatorHealerTest {

    // A hand-rolled fake beats a Mockito mock here: the client is a tiny interface and the
    // fake makes "the LLM replied X" / "the LLM call blew up" read as plain field assignments.
    static final class FakeLlmClient implements LlmMessagesClient {
        String reply = "```css\n#healed\n```";
        RuntimeException failure;

        @Override
        public LlmResponse send(String system, String userMessage) {
            if (failure != null) {
                throw failure;
            }
            return new LlmResponse(reply, new TokenUsage(10, 20));
        }
    }

    private WebDriver driver;
    private FakeLlmClient fakeClient;
    private AiLocatorHealer healer;

    @BeforeEach
    void setUp() {
        driver = mock(WebDriver.class);
        fakeClient = new FakeLlmClient();
        // No-op cost reporter: these tests assert healing behavior, not cost logging.
        healer = new AiLocatorHealer(() -> fakeClient, () -> "test-model", (element, model, usage) -> {
        });
        when(driver.getPageSource()).thenReturn("<html><body></body></html>");
    }

    private static NoSuchElementException originalFailure() {
        return new NoSuchElementException("original locator did not match");
    }

    @Test
    void returnsElementFoundWithSuggestedSelector() {
        WebElement healed = mock(WebElement.class);
        when(driver.findElement(By.cssSelector("#healed"))).thenReturn(healed);

        AiLocatorHealer.HealedTarget result = healer.heal(driver, "search box", originalFailure());

        assertSame(healed, result.element());
        assertEquals("#healed", result.selector());
    }

    @Test
    void rethrowsCauseWithSuppressedWhenLlmCallFails() {
        IllegalStateException llmBlewUp = new IllegalStateException("API key rejected");
        fakeClient.failure = llmBlewUp;
        NoSuchElementException cause = originalFailure();

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> healer.heal(driver, "search box", cause));

        assertSame(cause, thrown, "the ORIGINAL failure must propagate, not the LLM error");
        assertTrue(containsSuppressed(thrown, llmBlewUp));
    }

    @Test
    void rethrowsCauseWithSuppressedWhenHealingLookupMisses() {
        NoSuchElementException stillMissing = new NoSuchElementException("healed selector matched nothing");
        when(driver.findElement(any(By.class))).thenThrow(stillMissing);
        NoSuchElementException cause = originalFailure();

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> healer.heal(driver, "search box", cause));

        assertSame(cause, thrown);
        assertTrue(containsSuppressed(thrown, stillMissing));
    }

    @Test
    void rethrowsCauseWhenHealingLookupThrowsWebDriverException() {
        WebDriverException sessionDied = new WebDriverException("session deleted");
        when(driver.findElement(any(By.class))).thenThrow(sessionDied);
        NoSuchElementException cause = originalFailure();

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> healer.heal(driver, "search box", cause));

        assertSame(cause, thrown, "even a dead session must surface the original lookup failure");
        assertTrue(containsSuppressed(thrown, sessionDied));
    }

    @Test
    void rejectsBlankSelectorSuggestion() {
        fakeClient.reply = "   ";
        NoSuchElementException cause = originalFailure();

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> healer.heal(driver, "search box", cause));

        assertSame(cause, thrown);
        assertTrue(hasSuppressedType(thrown, IllegalArgumentException.class));
    }

    @Test
    void rejectsMultiLineSelectorSuggestion() {
        fakeClient.reply = "```css\n#one\n#two\n```";
        NoSuchElementException cause = originalFailure();

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> healer.heal(driver, "search box", cause));

        assertSame(cause, thrown, "a multi-line reply is the model chatting, not a selector");
        assertTrue(hasSuppressedType(thrown, IllegalArgumentException.class));
    }

    @Test
    void toleratesNullPageSource() {
        when(driver.getPageSource()).thenReturn(null);
        WebElement healed = mock(WebElement.class);
        when(driver.findElement(By.cssSelector("#healed"))).thenReturn(healed);

        AiLocatorHealer.HealedTarget result = healer.heal(driver, "search box", originalFailure());

        assertSame(healed, result.element());
    }

    private static boolean containsSuppressed(Throwable thrown, Throwable expected) {
        for (Throwable suppressed : thrown.getSuppressed()) {
            if (suppressed == expected) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSuppressedType(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable suppressed : thrown.getSuppressed()) {
            if (type.isInstance(suppressed)) {
                return true;
            }
        }
        return false;
    }
}
