package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.examples.webpatterns.support.WebPatternsFixtureServer;
import com.cucumberbddparallel.framework.driver.DriverManager;
import com.cucumberbddparallel.framework.interaction.FileUploadHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadDownloadTest {

    @TempDir
    Path tempDir;

    private WebPatternsFixtureServer server;
    private WebDriver driver;
    private Path downloadDir;
    private Path uploadFile;

    @BeforeEach
    void setUp() throws Exception {
        server = WebPatternsFixtureServer.start(WebPatternsSupport.fixturesRoot());
        downloadDir = tempDir.resolve("downloads");
        uploadFile = tempDir.resolve("evidence.txt");
        Files.writeString(uploadFile, "screenshot attached");
        driver = WebPatternsSupport.startHeadlessChrome(downloadDir);
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void uploadsFileThroughHiddenInput() {
        driver.get(server.url("/upload.html"));
        WebElement input = driver.findElement(By.cssSelector("#file-input"));
        FileUploadHelper.upload(input, uploadFile);
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.textToBePresentInElementLocated(By.id("result"), "evidence.txt"));
        assertTrue(driver.findElement(By.id("result")).getText().contains("evidence.txt"));
    }

    @Test
    void downloadsAttachmentToConfiguredDirectory() throws Exception {
        driver.get(server.url("/download/report.txt"));
        Path downloaded = waitForFile(downloadDir.resolve("report.txt"), Duration.ofSeconds(15));
        String content = Files.readString(downloaded);
        assertTrue(content.contains("Parallel BDD evidence export"));
    }

    private static Path waitForFile(Path expected, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (Files.isRegularFile(expected)) {
                return expected;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("File did not appear: " + expected);
    }
}