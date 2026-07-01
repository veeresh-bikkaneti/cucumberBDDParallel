package com.cucumberparallel.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/Search.feature"},
        monochrome = true,
        snippets = CucumberOptions.SnippetType.CAMELCASE,
        plugin = {"pretty",
        "json:target/cucumber_json_reports/search.json",
        "html:target/search-html"},
        glue = {"com.cucumberparallel.hookup.driver",
                "com.cucumberparallel.homepage",
                "com.cucumberparallel.searchresultpage"})
public class SearchTest extends AbstractTestNGCucumberTests {
}
