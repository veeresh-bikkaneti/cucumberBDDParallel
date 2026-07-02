package com.cucumberbddparallel.framework.ai;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.DefaultElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

import java.lang.reflect.Field;
import java.util.List;

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

    private final SearchContext searchContext;

    public AiElementLocatorFactory(SearchContext searchContext) {
        this.searchContext = searchContext;
    }

    @Override
    public ElementLocator createLocator(Field field) {
        return new SelfHealingElementLocator(searchContext, field, new DefaultElementLocator(searchContext, field));
    }

    /**
     * Tries the normal Selenium lookup first, every time - there's no caching or "give up
     * after N failures" logic here. If it fails, and we're actually talking to a real
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
                return AiLocatorHealer.heal(driver, field.getName(), notFound);
            }
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
