package com.cucumberbddparallel.example.homepage;

import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Page object for google.com's homepage - this is the example the whole `framework` module
 * is built to support. Note there's no Selenium setup code here at all: extending
 * {@code BasePage} gets you `driver`, `wait`, and working {@code @FindBy} fields for free.
 * All this class does is describe the page and what a test can do with it.
 */
public class HomePage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(HomePage.class);
    // "https://www.google." + a country code (e.g. "com", "fr") - see Home_page.feature for
    // where the country comes from.
    private static final String HOME_PAGE_URL = "https://www.google.";

    @FindBy(css = "#hplogo")
    private WebElement logo;

    @FindBy(css = "input[name=q]")
    private WebElement searchInput;

    // Package-private on purpose - only this package's step definitions (HomePageSteps)
    // should be creating page objects. Step definitions are the only thing that should be
    // driving the browser; nothing outside this package needs a HomePage instance.
    HomePage() {
    }

    void goToHomePage(String country) {
        driver.get(HOME_PAGE_URL + country);
        wait.forLoading(5);
    }

    void checkLogoDisplay() {
        wait.forElementToBeDisplayed(5, this.logo, "Logo");
    }

    void checkTitle(String title) {
        String displayedTitle = driver.getTitle();
        boolean matches = title.equals(displayedTitle);
        LOG.info("Displayed title is \"{}\" instead of \"{}\": {}", displayedTitle, title, matches);
        Assert.assertTrue(matches);
    }

    void checkSearchBarDisplay() {
        wait.forElementToBeDisplayed(10, this.searchInput, "Search Bar");
    }

    void searchFor(String searchValue) {
        this.searchInput.sendKeys(searchValue);
        this.searchInput.sendKeys(Keys.ENTER);
    }
}
