package com.cucumberbddparallel.framework.driver;

import io.cucumber.java.Before;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Drop this into your runner's {@code glue} array (see {@code HomePageTest} in
 * example-tests) and every scenario gets a fresh browser before it starts - no manual
 * setup code needed in your own step definitions.
 */
public class Setup {

    @Before
    public void setWebDriver() {
        // -Dbrowser=firefox to switch browsers; chrome is the default so "just run the tests"
        // works without anyone needing to know this flag exists.
        String browser = System.getProperty("browser");
        if (browser == null) {
            browser = "chrome";
        }
        WebDriver driver = switch (browser) {
            case "chrome" -> {
                // WebDriverManager downloads (and caches) a matching driver binary for
                // whatever Chrome/Firefox version is installed - nobody has to manually
                // download chromedriver and keep it in sync with browser updates.
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver();
            }
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                yield new FirefoxDriver();
            }
            default -> throw new IllegalArgumentException("Browser \"" + browser + "\" isn't supported.");
        };
        driver.manage().window().maximize();
        DriverManager.set(driver);
    }
}
