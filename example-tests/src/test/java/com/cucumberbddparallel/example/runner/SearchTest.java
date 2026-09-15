package com.cucumberbddparallel.example.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Plain single-threaded runner for Search.feature - same idea as {@link HomePageTest}, and
 * same story: Surefire excludes the hand-written runners in this package, so this only runs when you
 * execute it directly (e.g. from your IDE). The real parallel run is
 * {@code ./mvnw clean verify -Pintegration-test -pl example-tests -am} - no
 * {@code -DskipTests}. See {@link HomePageTest}'s Javadoc for the full explanation.
 */
@CucumberOptions(
        features = {"src/test/resources/features/Search.feature"},
        monochrome = true,
        snippets = CucumberOptions.SnippetType.CAMELCASE,
        plugin = {"pretty",
        "json:target/cucumber_json_reports/search.json",
        "html:target/search-html"},
        glue = {"com.cucumberbddparallel.framework.driver",
                "com.cucumberbddparallel.example.support",
                "com.cucumberbddparallel.example.homepage",
                "com.cucumberbddparallel.example.searchresultpage"})
public class SearchTest extends AbstractTestNGCucumberTests {
}
