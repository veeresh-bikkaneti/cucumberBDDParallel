package com.cucumberbddparallel.framework.interaction;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * HTML5 drag-and-drop (sortable lists, drop zones, kanban boards).
 *
 * The primary path simulates the HTML5 drag-and-drop event sequence
 * (dragstart → dragover → drop → dragend) with an injected {@code DataTransfer} object
 * via JavaScript. That's what most HTML5 drop targets actually listen for - plain
 * {@code Actions} mouse events often don't trigger them at all, because no
 * {@code DataTransfer} is ever created. If the JS injection fails (e.g. the driver
 * doesn't support it), it falls back to the classic Actions-based drag.
 */
public final class DragDropHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DragDropHelper.class);

    // Fires the full HTML5 DnD lifecycle against the source and target. Uses the modern
    // DragEvent constructor with a real DataTransfer so drop handlers reading
    // event.dataTransfer see what they'd see from a genuine user drag.
    private static final String JS_DRAG_AND_DROP = """
            const dataTransfer = new DataTransfer();
            const source = arguments[0];
            const target = arguments[1];
            const eventInit = {dataTransfer: dataTransfer, bubbles: true, cancelable: true};
            source.dispatchEvent(new DragEvent('dragstart', eventInit));
            target.dispatchEvent(new DragEvent('dragover', eventInit));
            target.dispatchEvent(new DragEvent('drop', eventInit));
            source.dispatchEvent(new DragEvent('dragend', eventInit));
            """;

    private final WebDriver driver;

    public DragDropHelper(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver");
    }

    public void dragAndDrop(WebElement source, WebElement target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        try {
            ((JavascriptExecutor) driver).executeScript(JS_DRAG_AND_DROP, source, target);
        } catch (WebDriverException | ClassCastException e) {
            LOG.debug("JS drag-and-drop failed; falling back to Actions-based drag", e);
            dragAndDropViaActions(source, target);
        }
    }

    private void dragAndDropViaActions(WebElement source, WebElement target) {
        new Actions(driver)
                .clickAndHold(source)
                .pause(200)
                .moveToElement(target)
                .pause(200)
                .release()
                .perform();
    }
}
