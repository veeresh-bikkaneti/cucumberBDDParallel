package com.cucumberbddparallel.example.homepage;

import com.cucumberbddparallel.example.support.AppServerHooks;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Page object for the example app's home page ({@code /}) - the first page every example
 * in this module starts from. Note there's no Selenium setup code here at all: extending
 * {@code BasePage} gets you {@code driver}, {@code wait}, and working {@code @FindBy} fields
 * for free. All this class does is describe the page and what a test can do with it.
 *
 * <p>The page under test is served by the self-contained {@link AppServerHooks} app server -
 * deliberately not a live site, so runs are hermetic (no geo redirects, consent banners,
 * or markup drift between runs).
 */
public class HomePage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(HomePage.class);

    @FindBy(css = "#logo")
    private WebElement logo;

    @FindBy(css = "#search-box")
    private WebElement searchBox;

    @FindBy(css = "#search-button")
    private WebElement searchButton;

    /** Public so step classes can {@code new HomePage()} in their {@code @Before} hooks. */
    public HomePage() {
    }

    void goToHomePage() {
        driver.get(AppServerHooks.baseUrl() + "/");
        wait.forLoading(5);
    }

    void checkLogoDisplay() {
        wait.forElementToBeDisplayed(5, this.logo, "Logo");
    }

    void checkTitle(String title) {
        String displayedTitle = driver.getTitle();
        LOG.info("Page title check: expected \"{}\", displayed \"{}\"", title, displayedTitle);
        Assert.assertEquals(displayedTitle, title, "Page title did not match");
    }

    void checkSearchBoxDisplay() {
        wait.forElementToBeDisplayed(10, this.searchBox, "Search box");
    }

    void searchFor(String searchValue) {
        this.searchBox.clear();
        this.searchBox.sendKeys(searchValue);
        this.searchButton.click();
    }
}
