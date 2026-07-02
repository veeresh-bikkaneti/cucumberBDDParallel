package com.cucumberbddparallel.example.searchresultpage;

import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.List;
import java.util.stream.IntStream;

/** Page object for the Google search results page - just the "is this URL in the first N results" check. */
public class SearchResultPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(SearchResultPage.class);
    // Google renders each result's visible URL inside a <cite> tag - that's genuinely all
    // we're matching against here, not the full result markup.
    private static final String RESULTS_URL_SELECTOR = "cite";

    @FindBy(css = RESULTS_URL_SELECTOR)
    private List<WebElement> results;

    SearchResultPage() {
    }

    /** True if {@code expectedUrl} shows up among the first {@code nbOfResultsToSearch} results. */
    void checkExpectedUrlInResults(String expectedUrl, int nbOfResultsToSearch) {
        wait.forPresenceOfElements(5, By.cssSelector(RESULTS_URL_SELECTOR), "Result url");
        // Math.min guards against asking for more results than actually came back - without
        // it, a search returning fewer results than expected would throw an
        // IndexOutOfBoundsException instead of a clear assertion failure.
        int indexOfLink = IntStream.range(0, Math.min(this.results.size(), nbOfResultsToSearch))
                .filter(index -> expectedUrl.equals(this.results.get(index).getText()))
                .findFirst()
                .orElse(-1);
        boolean found = indexOfLink != -1;
        LOG.info("Url \"{}\" wasn't found in the results: {}", expectedUrl, found);
        Assert.assertTrue(found);
    }
}
