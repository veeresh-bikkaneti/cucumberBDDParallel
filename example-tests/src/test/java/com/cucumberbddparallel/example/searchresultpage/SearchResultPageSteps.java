package com.cucumberbddparallel.example.searchresultpage;

import io.cucumber.java.en.Then;

/** Step definition for the one assertion Search.feature makes: a URL appears in the results. */
public class SearchResultPageSteps {

    private SearchResultPage searchResultPage;

    public SearchResultPageSteps() {
        this.searchResultPage = new SearchResultPage();
    }

    @Then("^\"([^\"]*)\" is displayed in the first \"([^\"]*)\" results$")
    public void isDisplayedInTheFirstResults(String expectedResultUrl, int nbOfResultsToSearch) {
        this.searchResultPage.checkExpectedUrlInResults(expectedResultUrl, nbOfResultsToSearch);
    }
}
