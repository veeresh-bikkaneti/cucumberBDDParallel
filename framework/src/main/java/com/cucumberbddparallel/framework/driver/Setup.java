package com.cucumberbddparallel.framework.driver;

import io.cucumber.java.Before;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Drop this into your runner's {@code glue} array (see {@code HomePageTest} in
 * example-tests) and every scenario gets a fresh browser before it starts - no manual
 * setup code needed in your own step definitions.
 */
public class Setup {

    private static final Logger LOG = LoggerFactory.getLogger(Setup.class);

    // WebDriverManager resolves and downloads the driver binary on first use. In a
    // parallel run, several @Before hooks can hit that resolution at the same moment and
    // step on each other (two threads downloading/extracting the same binary), so the
    // resolution itself is serialized here. Creating the actual browser session
    // (new ChromeDriver()) stays outside the lock - that's the slow part and it's
    // thread-safe, so there's no reason to serialize it.
    private static final Object DRIVER_BINARY_LOCK = new Object();

    @Before(order = 0)
    public void setWebDriver() {
        // -Dbrowser=firefox to switch browsers; chrome is the default so "just run the tests"
        // works without anyone needing to know this flag exists. Matched case-insensitively
        // so -Dbrowser=Chrome and -Dbrowser=CHROME both do what you'd expect.
        String browser = System.getProperty("browser", "chrome").trim().toLowerCase(Locale.ROOT);
        LOG.debug("Starting '{}' browser for scenario", browser);
        WebDriver driver = switch (browser) {
            case "chrome" -> {
                synchronized (DRIVER_BINARY_LOCK) {
                    // WebDriverManager downloads (and caches) a matching driver binary for
                    // whatever Chrome/Firefox version is installed - nobody has to manually
                    // download chromedriver and keep it in sync with browser updates.
                    WebDriverManager.chromedriver().setup();
                }
                yield new ChromeDriver(chromeOptions());
            }
            case "firefox" -> {
                synchronized (DRIVER_BINARY_LOCK) {
                    WebDriverManager.firefoxdriver().setup();
                }
                yield new FirefoxDriver(firefoxOptions());
            }
            default -> throw new IllegalArgumentException("Browser \"" + browser + "\" isn't supported.");
        };
        try {
            driver.manage().window().maximize();
        } catch (WebDriverException e) {
            // Headless windows (and some CI display setups) can't maximize - that's fine,
            // the --window-size argument above already gives us a sane viewport.
            LOG.warn("Could not maximize browser window; continuing with the default size", e);
        }
        DriverManager.set(driver);
    }

    private static ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        if (isHeadless()) {
            options.addArguments("--headless=new");
        }
        if (isHeadless() || isRunningAsRoot()) {
            // --no-sandbox: Chrome refuses to start sandboxed as root, so this is required
            // in root-run containers. --disable-dev-shm-usage avoids crashes when /dev/shm
            // is tiny, which is the default in most Docker images.
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        }
        return options;
    }

    private static FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=1920", "--height=1080");
        if (isHeadless()) {
            options.addArguments("-headless");
        }
        return options;
    }

    /** -Dheadless=true for CI / containers with no display. Defaults to headed. */
    private static boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("headless", "false"));
    }

    private static boolean isRunningAsRoot() {
        return "root".equals(System.getProperty("user.name"));
    }
}
