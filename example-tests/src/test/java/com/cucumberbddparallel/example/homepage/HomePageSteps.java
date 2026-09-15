package com.cucumberbddparallel.example.homepage;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for Home_page.feature and the home-page part of Search.feature.
 *
 * This class is deliberately thin: every step just calls a method on {@link HomePage}. The
 * step definitions describe "what happens in the Gherkin," the page object knows "how to
 * actually do it on the page" - keeping that split means the same HomePage methods get
 * reused by SearchTest without duplicating any Selenium code.
 */
public class HomePageSteps {

    private HomePage homePage;

    /**
     * The page object is created here - not injected via the constructor. PicoContainer
     * builds glue objects <em>before</em> any {@code @Before} hook runs, so constructor
     * injection would call {@code new HomePage()} before {@code Setup}'s
     * {@code @Before(order = 0)} has opened the browser, and {@code BasePage}'s
     * constructor would find no driver. This hook uses the default order (10000), which
     * runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.homePage = new HomePage();
    }

    @Given("^A user navigates to HomePage \"([^\"]*)\"$")
    public void aUserNavigatesToHomePage(String country) {
        this.homePage.goToHomePage(country);
    }

    @Then("^Google logo is displayed$")
    public void googleLogoIsDisplayed() {
        this.homePage.checkLogoDisplay();
    }

    @And("^search bar is displayed$")
    public void searchBarIsDisplayed() {
        this.homePage.checkSearchBarDisplay();
    }

    @Then("^page title is \"([^\"]*)\"$")
    public void pageTitleIs(String title) {
        this.homePage.checkTitle(title);
    }

    @When("^a user searches for \"([^\"]*)\"$")
    public void aUserSearchesFor(String searchValue) {
        this.homePage.searchFor(searchValue);
    }
}
