package com.cucumberbddparallel.example.homepage;

import com.cucumberbddparallel.example.support.FixtureHooks;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Page object for the example search homepage - this is the example the whole `framework`
 * module is built to support. Note there's no Selenium setup code here at all: extending
 * {@code BasePage} gets you `driver`, `wait`, and working {@code @FindBy} fields for free.
 * All this class does is describe the page and what a test can do with it.
 *
 * The page under test is the local {@code fixtures/home.html} served by
 * {@link FixtureHooks}' fixture server - deliberately not live google.com, so runs are
 * hermetic (no geo redirects, consent banners, or markup drift between runs).
 */
public class HomePage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(HomePage.class);

    @FindBy(css = "#hplogo")
    private WebElement logo;

    @FindBy(css = "input[name=q]")
    private WebElement searchInput;

    /**
     * Public because cucumber-picocontainer instantiates page objects via constructor
     * injection (see {@link HomePageSteps}) - one instance per scenario, created lazily
     * after the {@code @Before} hook has opened the browser.
     */
    public HomePage() {
    }

    void goToHomePage(String country) {
        // `country` is legacy Gherkin wording ("A user navigates to HomePage "fr"") kept so the
        // feature files still read naturally; the hermetic fixture serves the same page for
        // every country code.
        driver.get(FixtureHooks.baseUrl() + "/home.html");
        wait.forLoading(5);
    }

    void checkLogoDisplay() {
        wait.forElementToBeDisplayed(5, this.logo, "Logo");
    }

    void checkTitle(String title) {
        String displayedTitle = driver.getTitle();
        boolean matches = title.equals(displayedTitle);
        LOG.info("Page title check: expected \"{}\", displayed \"{}\", match={}", title, displayedTitle, matches);
        Assert.assertTrue(matches,
                "Expected page title \"" + title + "\" but the page showed \"" + displayedTitle + "\"");
    }

    void checkSearchBarDisplay() {
        wait.forElementToBeDisplayed(10, this.searchInput, "Search Bar");
    }

    void searchFor(String searchValue) {
        this.searchInput.sendKeys(searchValue);
        this.searchInput.sendKeys(Keys.ENTER);
    }
}
