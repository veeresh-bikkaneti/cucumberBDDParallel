package com.cucumberbddparallel.example.searchresultpage;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;

/** Step definitions for the search results assertions. */
public class SearchResultPageSteps {

    private SearchResultPage searchResultPage;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would call {@code new SearchResultPage()} before {@code Setup}'s
     * {@code @Before(order = 0)} has opened the browser, and {@code BasePage}'s
     * constructor would find no driver. This hook uses the default order (10000), which
     * runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.searchResultPage = new SearchResultPage();
    }

    @Then("the results heading shows {string}")
    public void theResultsHeadingShows(String query) {
        this.searchResultPage.checkHeadingShowsQuery(query);
    }

    @Then("the first {int} result links contain {string}")
    public void theFirstResultLinksContain(int count, String query) {
        this.searchResultPage.checkFirstResultLinksContainQuery(query, count);
    }
}
