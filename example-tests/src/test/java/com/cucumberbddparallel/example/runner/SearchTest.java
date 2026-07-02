package com.cucumberbddparallel.example.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Plain single-threaded runner for Search.feature - same idea as {@link HomePageTest}, and
 * same caveat: keep {@code -DskipTests} on the build command so Surefire doesn't pick this
 * up directly. See {@link HomePageTest}'s Javadoc for the full explanation.
 */
@CucumberOptions(
        features = {"src/test/resources/features/Search.feature"},
        monochrome = true,
        snippets = CucumberOptions.SnippetType.CAMELCASE,
        plugin = {"pretty",
        "json:target/cucumber_json_reports/search.json",
        "html:target/search-html"},
        glue = {"com.cucumberbddparallel.framework.driver",
                "com.cucumberbddparallel.example.homepage",
                "com.cucumberbddparallel.example.searchresultpage"})
public class SearchTest extends AbstractTestNGCucumberTests {
}
