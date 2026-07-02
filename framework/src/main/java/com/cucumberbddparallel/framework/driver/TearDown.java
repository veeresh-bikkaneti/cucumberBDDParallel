package com.cucumberbddparallel.framework.driver;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class TearDown {

    @After
    public void quitDriver(Scenario scenario) {
        WebDriver driver = DriverManager.get();
        if (scenario.isFailed()) {
            saveScreenshotsForScenario(driver, scenario);
        }
        DriverManager.quit();
    }

    private void saveScreenshotsForScenario(WebDriver driver, Scenario scenario) {
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        scenario.attach(screenshot, "image/png", "screenshot");
    }
}
