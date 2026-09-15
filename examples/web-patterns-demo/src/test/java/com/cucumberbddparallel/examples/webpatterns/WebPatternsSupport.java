package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.framework.driver.DriverManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class WebPatternsSupport {

    private WebPatternsSupport() {
    }

    static WebDriver startHeadlessChrome(Path downloadDir) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // --no-sandbox and --disable-dev-shm-usage are required when Chrome runs as root
        // inside Docker (the default in CI): the sandbox refuses to start as root, and
        // /dev/shm is typically too small for Chrome's renderer.
        options.addArguments("--headless=new", "--window-size=1280,800", "--disable-gpu",
                "--no-sandbox", "--disable-dev-shm-usage");
        if (downloadDir != null) {
            try {
                Files.createDirectories(downloadDir);
            } catch (Exception ignored) {
                // Chrome will still start; download assertions may fail loudly.
            }
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir.toAbsolutePath().toString());
            prefs.put("download.prompt_for_download", false);
            prefs.put("safebrowsing.enabled", true);
            options.setExperimentalOption("prefs", prefs);
        }
        WebDriver driver = new ChromeDriver(options);
        DriverManager.set(driver);
        return driver;
    }

    static Path fixturesRoot() {
        return Path.of("fixtures").toAbsolutePath();
    }
}