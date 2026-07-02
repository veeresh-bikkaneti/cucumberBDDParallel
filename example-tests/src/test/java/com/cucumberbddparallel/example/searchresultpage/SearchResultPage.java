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

public class SearchResultPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(SearchResultPage.class);
    private static final String RESULTS_URL_SELECTOR = "cite";

    @FindBy(css = RESULTS_URL_SELECTOR)
    private List<WebElement> results;

    SearchResultPage() {
    }

    void checkExpectedUrlInResults(String expectedUrl, int nbOfResultsToSearch) {
        wait.forPresenceOfElements(5, By.cssSelector(RESULTS_URL_SELECTOR), "Result url");
        int indexOfLink = IntStream.range(0, Math.min(this.results.size(), nbOfResultsToSearch))
                .filter(index -> expectedUrl.equals(this.results.get(index).getText()))
                .findFirst()
                .orElse(-1);
        boolean found = indexOfLink != -1;
        LOG.info("Url \"{}\" wasn't found in the results: {}", expectedUrl, found);
        Assert.assertTrue(found);
    }
}
