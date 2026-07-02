package com.cucumberbddparallel.example.runner;

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
                "com.cucumberbddparallel.framework.driver",
                "com.cucumberbddparallel.example.homepage"
        })
public class HomePageTest extends AbstractTestNGCucumberTests {
}
