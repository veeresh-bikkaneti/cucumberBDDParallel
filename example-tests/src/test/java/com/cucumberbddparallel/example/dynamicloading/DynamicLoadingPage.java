package com.cucumberbddparallel.example.dynamicloading;

import com.cucumberbddparallel.example.support.AppServerHooks;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Page object for the dynamic loading demo ({@code /dynamic}). Clicking the load button
 * makes the delayed content show up about two seconds later, so every read goes through
 * the framework {@code Wait} - never a {@code Thread.sleep}.
 */
public class DynamicLoadingPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicLoadingPage.class);

    @FindBy(css = "#load-button")
    private WebElement loadButton;

    @FindBy(css = "#delayed-content")
    private WebElement delayedContent;

    /** Public so step classes can {@code new DynamicLoadingPage()} in their {@code @Before} hooks. */
    public DynamicLoadingPage() {
    }

    void goToDynamicLoadingPage() {
        driver.get(AppServerHooks.baseUrl() + "/dynamic");
        wait.forLoading(5);
    }

    void clickLoadButton() {
        wait.forElementToBeDisplayed(5, this.loadButton, "Load button").click();
    }

    /** Waits (up to 10s) for the delayed content to become visible, then checks its text. */
    void checkDelayedContentShows(String expectedText) {
        WebElement content = wait.forElementToBeDisplayed(10, this.delayedContent, "Delayed content");
        String actual = content.getText();
        LOG.info("Delayed content: expected \"{}\", displayed \"{}\"", expectedText, actual);
        Assert.assertEquals(actual, expectedText, "Delayed content text did not match");
    }
}
