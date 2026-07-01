package com.cucumberparallel.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/resources/features/Home_page.feature"},
        plugin =
                {"pretty",
                        "json:target/cucumber_json_reports/home-page.json",
                        "html:target/home-page-html"
                },
        glue = {
                "com.cucumberparallel.hookup.driver",
                "com.cucumberparallel.homepage"
        })
public class HomePageTest extends AbstractTestNGCucumberTests {
}
