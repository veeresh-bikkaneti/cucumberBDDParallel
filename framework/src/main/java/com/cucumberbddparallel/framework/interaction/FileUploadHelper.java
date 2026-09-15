package com.cucumberbddparallel.framework.interaction;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Sends a local file path to an {@code input[type=file]} element.
 */
public final class FileUploadHelper {

    private FileUploadHelper() {
    }

    public static void upload(WebElement fileInput, Path localFile) {
        Objects.requireNonNull(fileInput, "fileInput");
        validateFile(localFile);
        fileInput.sendKeys(localFile.toAbsolutePath().toString());
    }

    /**
     * Uploads a file, unhiding the input first when it's not directly interactable.
     * File inputs are routinely hidden with {@code display:none} and triggered through a
     * styled button - {@code sendKeys} on a hidden input just fails, so when a
     * {@link WebDriver} is provided we reveal the input via JavaScript first.
     *
     * @throws IllegalStateException if the input still isn't displayed and enabled after
     *                              the unhide attempt - failing fast here beats a cryptic
     *                              ElementNotInteractableException from deep in the driver.
     */
    public static void upload(WebDriver driver, WebElement fileInput, Path localFile) {
        Objects.requireNonNull(fileInput, "fileInput");
        validateFile(localFile);
        if (driver instanceof JavascriptExecutor js && (!fileInput.isDisplayed() || !fileInput.isEnabled())) {
            js.executeScript(
                    "arguments[0].style.display='block';"
                            + "arguments[0].style.visibility='visible';"
                            + "arguments[0].style.opacity='1';"
                            + "arguments[0].removeAttribute('hidden');",
                    fileInput);
        }
        if (!fileInput.isDisplayed() || !fileInput.isEnabled()) {
            throw new IllegalStateException(
                    "File input is not interactable (still hidden or disabled after unhide attempt) - "
                            + "check that the locator points at the actual input[type=file] element.");
        }
        fileInput.sendKeys(localFile.toAbsolutePath().toString());
    }

    private static void validateFile(Path localFile) {
        Objects.requireNonNull(localFile, "localFile");
        if (!Files.isRegularFile(localFile)) {
            throw new IllegalArgumentException("Upload path is not a regular file: " + localFile);
        }
    }
}
