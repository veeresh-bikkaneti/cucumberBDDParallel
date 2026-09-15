package com.cucumberbddparallel.example.login;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Step definitions for the login example - thin wrappers around {@link LoginPage}. */
public class LoginSteps {

    private LoginPage page;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would create the page before {@code Setup}'s {@code @Before(order = 0)} has opened
     * the browser. Default order (10000) runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.page = new LoginPage();
    }

    @Given("a user is on the login page")
    public void aUserIsOnTheLoginPage() {
        this.page.goToLoginPage();
    }

    @When("the user logs in as {string} with password {string}")
    public void theUserLogsInAsWithPassword(String username, String password) {
        this.page.loginAs(username, password);
    }

    @Then("the welcome message shows {string}")
    public void theWelcomeMessageShows(String expectedMessage) {
        this.page.checkWelcomeMessage(expectedMessage);
    }
}
