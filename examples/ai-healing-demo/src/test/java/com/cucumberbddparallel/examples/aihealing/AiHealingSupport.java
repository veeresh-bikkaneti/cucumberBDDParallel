package com.cucumberbddparallel.examples.aihealing;

import com.cucumberbddparallel.framework.driver.DriverManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class AiHealingSupport {

    private static final List<String> SET_PROPERTIES = new ArrayList<>();

    private AiHealingSupport() {
    }

    static WebDriver startHeadlessChrome() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // --no-sandbox and --disable-dev-shm-usage are required when Chrome runs as root
        // inside Docker (the default in CI): the sandbox refuses to start as root, and
        // /dev/shm is typically too small for Chrome's renderer.
        options.addArguments("--headless=new", "--window-size=1280,800", "--disable-gpu",
                "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        DriverManager.set(driver);
        return driver;
    }

    static void configureMockOpenAiProvider(String baseUrl) {
        setProperty("AI_HEALING_PROVIDER", "openai");
        setProperty("AI_HEALING_API_KEY", "demo-mock-key");
        setProperty("AI_HEALING_MODEL", "demo-mock-model");
        setProperty("AI_HEALING_BASE_URL", baseUrl);
    }

    static void disableHealing() {
        setProperty("ai.healing.enabled", "false");
    }

    static void clearAiHealingProperties() {
        for (String key : SET_PROPERTIES) {
            System.clearProperty(key);
        }
        SET_PROPERTIES.clear();
        System.clearProperty("ai.healing.enabled");
    }

    static Path fixtureHtml() {
        return Path.of("fixtures", "demo-page.html").toAbsolutePath();
    }

    private static void setProperty(String key, String value) {
        System.setProperty(key, value);
        SET_PROPERTIES.add(key);
    }
}