package com.cucumberbddparallel.example.fileupload;

import com.cucumberbddparallel.example.support.AppServerHooks;
import com.cucumberbddparallel.framework.interaction.FileUploadHelper;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.nio.file.Path;

/**
 * Page object for the file upload demo ({@code /upload}). The file itself goes in through
 * the framework {@link FileUploadHelper}; this class handles the app-specific parts -
 * clicking the upload button and reading the confirmation.
 */
public class FileUploadPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(FileUploadPage.class);

    @FindBy(css = "#file-input")
    private WebElement fileInput;

    @FindBy(css = "#upload-button")
    private WebElement uploadButton;

    @FindBy(css = "#upload-result")
    private WebElement uploadResult;

    /** Public so step classes can {@code new FileUploadPage()} in their {@code @Before} hooks. */
    public FileUploadPage() {
    }

    void goToFileUploadPage() {
        driver.get(AppServerHooks.baseUrl() + "/upload");
        wait.forLoading(5);
    }

    void uploadFile(Path localFile) {
        wait.forElementToBeDisplayed(5, this.fileInput, "File input");
        FileUploadHelper.upload(driver, this.fileInput, localFile);
        wait.forElementToBeDisplayed(5, this.uploadButton, "Upload button").click();
    }

    /** The app echoes the uploaded file name as {@code Uploaded: <name>}. */
    void checkUploadResultShows(String fileName) {
        WebElement result = wait.forElementToBeDisplayed(10, this.uploadResult, "Upload result");
        String expected = "Uploaded: " + fileName;
        String actual = result.getText();
        LOG.info("Upload result: expected \"{}\", displayed \"{}\"", expected, actual);
        Assert.assertEquals(actual, expected, "Upload confirmation did not name the uploaded file");
    }
}
