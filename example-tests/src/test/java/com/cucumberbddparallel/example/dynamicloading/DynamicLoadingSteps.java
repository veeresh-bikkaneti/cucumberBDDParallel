package com.cucumberbddparallel.example.dynamicloading;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Step definitions for the dynamic loading example - thin wrappers around {@link DynamicLoadingPage}. */
public class DynamicLoadingSteps {

    private DynamicLoadingPage page;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would create the page before {@code Setup}'s {@code @Before(order = 0)} has opened
     * the browser. Default order (10000) runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.page = new DynamicLoadingPage();
    }

    @Given("a user is on the dynamic loading page")
    public void aUserIsOnTheDynamicLoadingPage() {
        this.page.goToDynamicLoadingPage();
    }

    @When("the user clicks the load button")
    public void theUserClicksTheLoadButton() {
        this.page.clickLoadButton();
    }

    @Then("the delayed content appears showing {string}")
    public void theDelayedContentAppearsShowing(String expectedText) {
        this.page.checkDelayedContentShows(expectedText);
    }
}
