package com.cucumberbddparallel.framework.wait;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Explicit waits, one per situation you'd actually hit in a page object: waiting for the
 * page to finish loading, waiting for one element to show up, waiting for a list of
 * elements to exist. Every method here fails with a clear, specific message ("Search Bar
 * wasn't displayed after 10 seconds") instead of Selenium's generic timeout exception, so
 * when a test fails in CI you know what it was actually waiting on without digging through
 * a stack trace.
 *
 * Deliberately doesn't use {@code Thread.sleep} anywhere - fixed sleeps are either too
 * short (flaky) or too long (slow), and every method here polls instead until the real
 * condition is true or the timeout is hit.
 *
 * The wait methods return what they waited for (the element, the list, the JS result) so
 * callers can use it directly instead of looking it up a second time.
 */
public class Wait {

    private final WebDriver driver;

    public Wait(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver");
    }

    // The shared plumbing every wait*() method below uses - this is the one place that
    // knows how to build a WebDriverWait and attach a message to it. Adding a new kind of
    // wait means adding a new public method that calls this, not duplicating the
    // WebDriverWait setup again.
    private <T> T waitUntilCondition(ExpectedCondition<T> condition, String timeoutMessage, int timeout) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(timeoutMessage, "timeoutMessage");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        wait.withMessage(timeoutMessage);
        return wait.until(condition);
    }

    /**
     * Waits for {@code document.readyState === "complete"} - use this right after navigating
     * to a new page. Returns the JS expression's result (usually {@code Boolean.TRUE}).
     */
    public Object forLoading(int timeout) {
        ExpectedCondition<Object> condition = ExpectedConditions.jsReturnsValue("return document.readyState===\"complete\";");
        String timeoutMessage = "Page didn't load after " + timeout + " seconds.";
        return waitUntilCondition(condition, timeoutMessage, timeout);
    }

    /** Waits for a single element to be visible - the most common wait you'll reach for in a page object. */
    public WebElement forElementToBeDisplayed(int timeout, WebElement webElement, String webElementName) {
        Objects.requireNonNull(webElement, "webElement");
        Objects.requireNonNull(webElementName, "webElementName");
        ExpectedCondition<WebElement> condition = ExpectedConditions.visibilityOf(webElement);
        String timeoutMessage = webElementName + " wasn't displayed after " + timeout + " seconds.";
        return waitUntilCondition(condition, timeoutMessage, timeout);
    }

    /** Waits for at least one element matching {@code elementLocator} to exist in the DOM - handy for result lists that load asynchronously. */
    public List<WebElement> forPresenceOfElements(int timeout, By elementLocator, String elementName) {
        Objects.requireNonNull(elementLocator, "elementLocator");
        Objects.requireNonNull(elementName, "elementName");
        ExpectedCondition<List<WebElement>> condition = ExpectedConditions.presenceOfAllElementsLocatedBy(elementLocator);
        String timeoutMessage = elementName + " elements were not displayed after " + timeout + " seconds.";
        return waitUntilCondition(condition, timeoutMessage, timeout);
    }
}
