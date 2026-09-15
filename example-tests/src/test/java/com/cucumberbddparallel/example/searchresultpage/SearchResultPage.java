package com.cucumberbddparallel.example.searchresultpage;

import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.List;

/**
 * Page object for the example app's search results page ({@code /search?q=...}).
 * Verifies the heading names the query and that the result links actually mention it.
 */
public class SearchResultPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(SearchResultPage.class);

    @FindBy(css = "#results-heading")
    private WebElement heading;

    @FindBy(css = ".result .result-url")
    private List<WebElement> resultUrls;

    /** Public so step classes can {@code new SearchResultPage()} in their {@code @Before} hooks. */
    public SearchResultPage() {
    }

    /** The heading reads {@code Results for "query"} - check it names the query searched for. */
    void checkHeadingShowsQuery(String query) {
        wait.forElementToBeDisplayed(5, this.heading, "Results heading");
        String expected = "Results for \"" + query + "\"";
        String actual = this.heading.getText();
        LOG.info("Results heading: expected \"{}\", displayed \"{}\"", expected, actual);
        Assert.assertEquals(actual, expected, "Search results heading did not name the query");
    }

    /** Every one of the first {@code count} result links should mention the query (case-insensitive - the app lowercases queries in its result URLs). */
    void checkFirstResultLinksContainQuery(String query, int count) {
        wait.forPresenceOfElements(5, By.cssSelector(".result .result-url"), "Result links");
        Assert.assertTrue(this.resultUrls.size() >= count,
                "Expected at least " + count + " results but only found " + this.resultUrls.size());
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < count; i++) {
            String url = this.resultUrls.get(i).getText();
            LOG.info("Result {} link: \"{}\"", i + 1, url);
            Assert.assertTrue(url.toLowerCase().contains(lowerQuery),
                    "Result " + (i + 1) + " link \"" + url + "\" does not contain \"" + query + "\"");
        }
    }
}
