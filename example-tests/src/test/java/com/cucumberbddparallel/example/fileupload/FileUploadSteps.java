package com.cucumberbddparallel.example.fileupload;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Step definitions for the file upload example. The uploaded file is created fresh for
 * each scenario with {@link Files#createTempFile} - no fixture files to keep in sync.
 */
public class FileUploadSteps {

    private FileUploadPage page;
    private Path tempFile;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would create the page before {@code Setup}'s {@code @Before(order = 0)} has opened
     * the browser. Default order (10000) runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.page = new FileUploadPage();
    }

    @Given("a user is on the file upload page")
    public void aUserIsOnTheFileUploadPage() {
        this.page.goToFileUploadPage();
    }

    @When("the user uploads a text file named {string}")
    public void theUserUploadsATextFileNamed(String fileName) {
        this.tempFile = createTempTextFile(fileName);
        this.page.uploadFile(this.tempFile);
    }

    @Then("the upload result shows {string}")
    public void theUploadResultShows(String fileName) {
        this.page.checkUploadResultShows(fileName);
    }

    /**
     * Temp-file cleanup lives here - not in the {@code @Then} step - so a failure in the
     * {@code @When} step (or an aborted scenario) still deletes the temp dir instead of
     * leaking it under the system temp folder.
     */
    @After
    public void deleteTempFile() {
        if (this.tempFile != null) {
            try {
                Files.deleteIfExists(this.tempFile);
                Files.deleteIfExists(this.tempFile.getParent());
            } catch (IOException e) {
                // Best effort - a stray temp file never fails the test.
            }
            this.tempFile = null;
        }
    }

    /** Creates a temp file whose <em>name</em> is what the scenario asked for. */
    private static Path createTempTextFile(String fileName) {
        try {
            Path dir = Files.createTempDirectory("upload-demo");
            Path file = dir.resolve(fileName);
            Files.writeString(file, "Example upload content", StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create temp upload file", e);
        }
    }
}
