package com.cucumberbddparallel.framework.driver;

import io.cucumber.java.Before;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class Setup {

    @Before
    public void setWebDriver() {
        String browser = System.getProperty("browser");
        if (browser == null) {
            browser = "chrome";
        }
        WebDriver driver = switch (browser) {
            case "chrome" -> {
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
