package com.cucumberbddparallel.example.homepage;

import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

public class HomePage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(HomePage.class);
    private static final String HOME_PAGE_URL = "https://www.google.";

    @FindBy(css = "#hplogo")
    private WebElement logo;

    @FindBy(css = "input[name=q]")
    private WebElement searchInput;

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
