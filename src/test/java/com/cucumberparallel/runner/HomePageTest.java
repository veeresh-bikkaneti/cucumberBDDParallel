package com.cucumberparallel.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/Home_page.feature"},
        strict = false,
        plugin =
                {"pretty",
                        "json:target/cucumber_json_reports/home-page.json",
                        "html:target/home-page-html"
                },
        glue = {
                "com.automatedtest.sample.infrastructure.driver",
                "com.automatedtest.sample.homepage"
        })
public class HomePageTest extends AbstractTestNGCucumberTests {
}
