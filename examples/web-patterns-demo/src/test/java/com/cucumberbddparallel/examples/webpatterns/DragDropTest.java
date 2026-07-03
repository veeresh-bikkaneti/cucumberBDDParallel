package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.examples.webpatterns.support.WebPatternsFixtureServer;
import com.cucumberbddparallel.framework.driver.DriverManager;
import com.cucumberbddparallel.framework.interaction.DragDropHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragDropTest {

    private WebPatternsFixtureServer server;
    private WebDriver driver;

    @BeforeEach
    void setUp() throws Exception {
        server = WebPatternsFixtureServer.start(WebPatternsSupport.fixturesRoot());
        driver = WebPatternsSupport.startHeadlessChrome(null);
        driver.get(server.url("/drag-drop.html"));
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void movesCardIntoDoneLane() {
        WebElement source = driver.findElement(By.id("card-qa"));
        WebElement target = driver.findElement(By.id("done"));
        new DragDropHelper(driver).dragAndDrop(source, target);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.textToBePresentInElementLocated(By.id("drop-status"), "QA sign-off"));
        assertEquals("Dropped: QA sign-off", driver.findElement(By.id("drop-status")).getText());
        assertTrue(driver.findElements(By.cssSelector("#done .card")).size() >= 1);
    }
}