package com.cucumberbddparallel.framework.ai;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.DefaultElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A drop-in replacement for Selenium's normal locator factory that gives every
 * {@code @FindBy} field one extra chance before giving up.
 *
 * Selenium's {@link org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory}
 * just throws {@link NoSuchElementException} the moment a selector doesn't match anything.
 * That's the right call in general - most of the time a missing element really does mean
 * a bug in the test or the page. But when it's a case of "the markup changed slightly and
 * the old CSS selector no longer applies," asking Claude to suggest a replacement selector
 * and retrying once can save you from a flaky, high-maintenance suite.
 *
 * This class doesn't do the AI part itself - it just detects the failure and hands off to
 * {@link AiLocatorHealer}. See {@code BasePage} for how a page object chooses between this
 * factory and the plain Selenium one.
 */
public final class AiElementLocatorFactory implements ElementLocatorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AiElementLocatorFactory.class);

    // Selectors the LLM already suggested for us, keyed by page class + field name. Every
    // heal costs an API call, so once a broken locator has been repaired we reuse the
    // repaired selector on later lookups instead of asking again - LLM calls stay at ~1
    // per unique broken locator instead of one per poll. If the cached selector stops
    // matching (the page changed again), we drop it and re-heal.
    //
    // Keyed by declaring class as well as field name: two page objects can easily have
    // fields with the same name ("submitButton"), and a selector healed for one page must
    // never leak into another.
    private static final ConcurrentMap<String, String> HEALED_SELECTORS = new ConcurrentHashMap<>();

    /**
     * Drops all cached healed selectors. Called from {@code TearDown} after each scenario:
     * a healed selector is only trustworthy for the page it was healed against, and the
     * next scenario gets a fresh browser anyway - carrying selectors across scenarios
     * risks reusing a stale one, and the map would grow without bound over a long run.
     */
    public static void clearHealedSelectors() {
        HEALED_SELECTORS.clear();
    }

    private final SearchContext searchContext;

    public AiElementLocatorFactory(SearchContext searchContext) {
        this.searchContext = searchContext;
    }

    @Override
    public ElementLocator createLocator(Field field) {
        return new SelfHealingElementLocator(searchContext, field, new DefaultElementLocator(searchContext, field));
    }

    /**
     * Tries the normal Selenium lookup first, every time - then a previously-healed
     * selector if we have one, and only then a fresh (paid) AI healing call.
     * If it fails, and we're actually talking to a real
     * {@link WebDriver} (not some other kind of {@link SearchContext}, like a parent
     * element when looking up a nested field), we hand the failure to the AI healer.
     * If the healer also can't find the element, the original exception is what the
     * caller sees - we don't swallow the real error.
     */
    private static final class SelfHealingElementLocator implements ElementLocator {

        private final SearchContext searchContext;
        private final Field field;
        private final ElementLocator delegate;

        SelfHealingElementLocator(SearchContext searchContext, Field field, ElementLocator delegate) {
            this.searchContext = searchContext;
            this.field = field;
            this.delegate = delegate;
        }

        @Override
        public WebElement findElement() {
            try {
                return delegate.findElement();
            } catch (NoSuchElementException notFound) {
                if (!(searchContext instanceof WebDriver driver)) {
                    throw notFound;
                }
                String description = field.getName();
                String cached = HEALED_SELECTORS.get(cacheKey());
                if (cached != null) {
                    try {
                        LOG.debug("Retrying '{}' with previously healed selector '{}'", description, cached);
                        return driver.findElement(By.cssSelector(cached));
                    } catch (WebDriverException cachedStale) {
                        LOG.debug("Cached healed selector '{}' for '{}' no longer matches; re-healing",
                                cached, description);
                        HEALED_SELECTORS.remove(cacheKey(), cached);
                    }
                }
                AiLocatorHealer.HealedTarget healed = new AiLocatorHealer().heal(driver, description, notFound);
                HEALED_SELECTORS.put(cacheKey(), healed.selector());
                return healed.element();
            }
        }

        private String cacheKey() {
            return field.getDeclaringClass().getName() + "#" + field.getName();
        }

        // findElements() (plural) is used for @FindBy fields typed as List<WebElement> -
        // an empty list there is a perfectly normal, valid result (e.g. "no search results
        // yet"), so we don't want AI healing kicking in on every empty list. Healing only
        // applies to the single-element case above, where "not found" really is an error.
        @Override
        public List<WebElement> findElements() {
            return delegate.findElements();
        }
    }
}
