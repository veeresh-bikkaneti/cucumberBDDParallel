package com.cucumberbddparallel.framework.interaction;

import org.openqa.selenium.WebElement;

import java.nio.file.Path;

/**
 * Sends a local file path to a hidden {@code input[type=file]} element.
 */
public final class FileUploadHelper {

    private FileUploadHelper() {
    }

    public static void upload(WebElement fileInput, Path localFile) {
        if (!localFile.toFile().isFile()) {
            throw new IllegalArgumentException("Upload path is not a file: " + localFile);
        }
        fileInput.sendKeys(localFile.toAbsolutePath().toString());
    }
}