package com.cucumberbddparallel.example.dragdrop;

import com.cucumberbddparallel.example.support.AppServerHooks;
import com.cucumberbddparallel.framework.interaction.DragDropHelper;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;

/**
 * Page object for the drag-and-drop demo ({@code /drag-drop}). The actual drag goes
 * through the framework {@link DragDropHelper} - the page object only knows
 * <em>what</em> to drag <em>where</em> and how to read the result.
 */
public class DragDropPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(DragDropPage.class);

    private final DragDropHelper dragDrop;

    @FindBy(css = "#drag-source")
    private WebElement dragSource;

    @FindBy(css = "#drop-target")
    private WebElement dropTarget;

    @FindBy(css = "#drop-status")
    private WebElement dropStatus;

    /** Public so step classes can {@code new DragDropPage()} in their {@code @Before} hooks. */
    public DragDropPage() {
        this.dragDrop = new DragDropHelper(driver);
    }

    void goToDragDropPage() {
        driver.get(AppServerHooks.baseUrl() + "/drag-drop");
        wait.forLoading(5);
    }

    void dragSourceOntoTarget() {
        wait.forElementToBeDisplayed(5, this.dragSource, "Drag source");
        wait.forElementToBeDisplayed(5, this.dropTarget, "Drop target");
        this.dragDrop.dragAndDrop(this.dragSource, this.dropTarget);
        // The synthetic drop is synchronous, so log the status right away: if the drop
        // didn't register, this line shows it in CI logs instead of a bare timeout later.
        LOG.info("Drag executed; drop status now reads \"{}\"", this.dropStatus.getText());
    }

    /**
     * After a successful drop the status flips from "Waiting for drop…" to
     * "Dropped: drag-source". The status element is visible from page load, so waiting
     * for visibility alone could read the pre-drop text if the drop event fires late -
     * instead we wait for the exact post-drop text. Asserting the full string (not just
     * the "Dropped:" prefix) keeps the example honest: a drop that carries no id would
     * otherwise pass.
     */
    void checkDropSucceeded() {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                d -> "Dropped: drag-source".equals(this.dropStatus.getText()));
        String status = this.dropStatus.getText();
        LOG.info("Drop status: \"{}\"", status);
        Assert.assertEquals(status, "Dropped: drag-source",
                "Expected drop status \"Dropped: drag-source\" but was \"" + status + "\"");
    }
}
