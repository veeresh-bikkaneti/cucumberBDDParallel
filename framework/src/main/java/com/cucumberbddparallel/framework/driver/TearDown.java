package com.cucumberbddparallel.framework.driver;

import com.cucumberbddparallel.framework.ai.AiElementLocatorFactory;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The other half of {@link Setup} - closes the browser after each scenario, screenshotting failures first. */
public class TearDown {

    private static final Logger LOG = LoggerFactory.getLogger(TearDown.class);

    // Runs dead last: Cucumber executes @After hooks in DESCENDING order, so this low
    // order value puts the driver quit after any other @After hooks - reporting, custom
    // cleanup - which still have a live browser to work with when they run.
    @After(order = 1)
    public void quitDriver(Scenario scenario) {
        WebDriver driver;
        try {
            driver = DriverManager.get();
        } catch (IllegalStateException e) {
            // The @Before hook never ran (or already failed) - there's no browser to close
            // and no screenshot to take, so just move on instead of failing teardown.
            LOG.warn("Skipping driver teardown: no WebDriver was started for this scenario");
            return;
        }
        try {
            // Grab the screenshot BEFORE quitting the driver - once quit() runs, the browser
            // session is gone and there's nothing left to screenshot.
            if (scenario.isFailed() && driver instanceof TakesScreenshot screenshotTaker) {
                saveScreenshotForScenario(screenshotTaker, scenario);
            }
        } finally {
            // In the finally so the browser always closes, even if screenshotting blew up.
            // Healed selectors are only valid for the page they were healed against, so
            // the cache is dropped with the browser rather than leaking into the next
            // scenario.
            AiElementLocatorFactory.clearHealedSelectors();
            DriverManager.quit();
        }
    }

    // scenario.attach() puts the screenshot straight into the Cucumber HTML report, so a
    // failed scenario shows you exactly what the page looked like at the moment it failed -
    // no need to reproduce the failure locally just to see what went wrong. A failed
    // screenshot must never fail the teardown itself, so problems are logged and swallowed.
    private void saveScreenshotForScenario(TakesScreenshot driver, Scenario scenario) {
        try {
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "screenshot");
        } catch (RuntimeException e) {
            // RuntimeException covers WebDriverException (a dead session mid-screenshot) and
            // anything else the driver or the scenario object might throw.
            LOG.warn("Could not capture failure screenshot for scenario '{}'", scenario.getName(), e);
        }
    }
}
