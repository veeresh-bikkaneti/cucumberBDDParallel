package com.cucumberbddparallel.framework.driver;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/** The other half of {@link Setup} - closes the browser after each scenario, screenshotting failures first. */
public class TearDown {

    @After
    public void quitDriver(Scenario scenario) {
        WebDriver driver = DriverManager.get();
        // Grab the screenshot BEFORE quitting the driver - once quit() runs, the browser
        // session is gone and there's nothing left to screenshot.
        if (scenario.isFailed()) {
            saveScreenshotsForScenario(driver, scenario);
        }
        DriverManager.quit();
    }

    // scenario.attach() puts the screenshot straight into the Cucumber HTML report, so a
    // failed scenario shows you exactly what the page looked like at the moment it failed -
    // no need to reproduce the failure locally just to see what went wrong.
    private void saveScreenshotsForScenario(WebDriver driver, Scenario scenario) {
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        scenario.attach(screenshot, "image/png", "screenshot");
    }
}
