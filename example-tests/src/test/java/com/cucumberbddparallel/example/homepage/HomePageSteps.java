package com.cucumberbddparallel.example.homepage;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the home page scenarios. Deliberately thin: every step just calls a
 * method on {@link HomePage}. The steps describe "what happens in the Gherkin," the page
 * object knows "how to actually do it on the page."
 */
public class HomePageSteps {

    private HomePage homePage;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would call {@code new HomePage()} before {@code Setup}'s {@code @Before(order = 0)}
     * has opened the browser, and {@code BasePage}'s constructor would find no driver.
     * This hook uses the default order (10000), which runs after {@code Setup}'s order-0
     * hook.
     */
    @Before
    public void createPageObject() {
        this.homePage = new HomePage();
    }

    @Given("a user is on the example app home page")
    public void aUserIsOnTheExampleAppHomePage() {
        this.homePage.goToHomePage();
    }

    @Then("the logo is displayed")
    public void theLogoIsDisplayed() {
        this.homePage.checkLogoDisplay();
    }

    @Then("the search box is displayed")
    public void theSearchBoxIsDisplayed() {
        this.homePage.checkSearchBoxDisplay();
    }

    @Then("the page title is {string}")
    public void thePageTitleIs(String title) {
        this.homePage.checkTitle(title);
    }

    @When("the user searches for {string}")
    public void theUserSearchesFor(String query) {
        this.homePage.searchFor(query);
    }
}
