package com.cucumberbddparallel.example.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * A plain, single-threaded runner for Home_page.feature - useful if you just want to run
 * this one feature from your IDE without going through the parallel machinery.
 *
 * <p>Heads up: this class's name ends in "Test", which matches Maven Surefire's default
 * include patterns. The build configures Surefire to exclude the hand-written runners
 * (the {@code *Test} classes in this {@code runner} package),
 * so a normal {@code mvn verify} never picks this class up - the real suite is the parallel
 * {@code *IT} runners that cucable-plugin generates, executed by Failsafe during the
 * {@code integration-test} phase.
 *
 * <p>The command for the real parallel run is simply:
 * <pre>{@code
 * ./mvnw clean verify -Pintegration-test -pl example-tests -am
 * }</pre>
 * Do NOT add {@code -DskipTests}: Failsafe honors it, so you'd get a green build that ran
 * zero tests. The Surefire exclusion above already keeps this runner out of the build.
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
                "com.cucumberbddparallel.example.support",
                "com.cucumberbddparallel.example.homepage"
        })
public class HomePageTest extends AbstractTestNGCucumberTests {
}
