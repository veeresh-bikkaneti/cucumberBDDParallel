package com.cucumberbddparallel.example.searchresultpage;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;

/** Step definition for the one assertion Search.feature makes: a URL appears in the results. */
public class SearchResultPageSteps {

    private SearchResultPage searchResultPage;

    /**
     * The page object is created here - not injected via the constructor. PicoContainer
     * builds glue objects <em>before</em> any {@code @Before} hook runs, so constructor
     * injection would call {@code new SearchResultPage()} before {@code Setup}'s
     * {@code @Before(order = 0)} has opened the browser, and {@code BasePage}'s
     * constructor would find no driver. This hook uses the default order (10000), which
     * runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.searchResultPage = new SearchResultPage();
    }

    @Then("^\"([^\"]*)\" is displayed in the first \"([^\"]*)\" results$")
    public void isDisplayedInTheFirstResults(String expectedResultUrl, int nbOfResultsToSearch) {
        this.searchResultPage.checkExpectedUrlInResults(expectedResultUrl, nbOfResultsToSearch);
    }
}
