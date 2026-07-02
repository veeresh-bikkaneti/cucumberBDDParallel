package com.cucumberbddparallel.example.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * A plain, single-threaded runner for Home_page.feature - useful if you just want to run
 * this one feature from your IDE without going through the parallel machinery.
 *
 * Heads up: this class's name ends in "Test", which matches Maven Surefire's default
 * pattern for unit tests. That means if you ever run the build WITHOUT {@code -DskipTests},
 * Surefire will try to run this class directly during the regular `test` phase - separately
 * from, and before, the parallel runners that cucable-plugin generates for the real
 * integration-test run. That's exactly why every command in README.md and CI keeps
 * {@code -DskipTests}: it's not there to skip tests overall, it's there so THIS class
 * doesn't get double-run outside the intended parallel/failsafe flow.
 */
@CucumberOptions(
        features = {"src/test/resources/features/Home_page.feature"},
        plugin =
                {"pretty",
                        "json:target/cucumber_json_reports/home-page.json",
                        "html:target/home-page-html"
                },
        glue = {
                "com.cucumberbddparallel.framework.driver",
                "com.cucumberbddparallel.example.homepage"
        })
public class HomePageTest extends AbstractTestNGCucumberTests {
}
