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

/** Wraps Selenium's default locator so a failed lookup is retried once with an AI-suggested selector. */
public final class AiElementLocatorFactory implements ElementLocatorFactory {

    private final SearchContext searchContext;

    public AiElementLocatorFactory(SearchContext searchContext) {
        this.searchContext = searchContext;
    }

    @Override
    public ElementLocator createLocator(Field field) {
        return new SelfHealingElementLocator(searchContext, field, new DefaultElementLocator(searchContext, field));
    }

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

        @Override
        public List<WebElement> findElements() {
            return delegate.findElements();
        }
    }
}
