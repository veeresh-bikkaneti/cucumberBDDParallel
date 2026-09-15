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
 * {@code DataTransfer} is ever created.
 *
 * <p>The {@code DataTransfer} is attached with {@code Object.defineProperty} rather than
 * through the {@code DragEvent} constructor's init dictionary: several browsers ignore
 * the init member and leave {@code event.dataTransfer} {@code null}, which silently
 * breaks drop handlers that read it. Defining it as an own property on each dispatched
 * event shadows the prototype getter and works everywhere. One {@code DataTransfer} is
 * shared by the whole gesture, exactly like a real user drag.
 *
 * <p>If the JS injection fails (e.g. the driver doesn't support it), it falls back to
 * the classic Actions-based drag and logs a warning - a silent fallback here would turn
 * a clear scripting error into a mysterious timeout in the calling test.
 */
public final class DragDropHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DragDropHelper.class);

    // Fires the full HTML5 DnD lifecycle against the source and target. Each event gets
    // the shared DataTransfer injected via defineProperty (see the class javadoc for why
    // the DragEvent constructor's init dict isn't used for it).
    private static final String JS_DRAG_AND_DROP = """
            const source = arguments[0];
            const target = arguments[1];
            // One DataTransfer shared by the whole gesture, like a real user drag: the
            // page's dragstart handler fills it in, the drop handler reads it back.
            const dataTransfer = new DataTransfer();
            function fireDragEvent(element, type) {
                const event = new DragEvent(type, {bubbles: true, cancelable: true});
                Object.defineProperty(event, 'dataTransfer', {value: dataTransfer});
                element.dispatchEvent(event);
            }
            fireDragEvent(source, 'dragstart');
            fireDragEvent(target, 'dragover');
            fireDragEvent(target, 'drop');
            fireDragEvent(source, 'dragend');
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
            LOG.warn("JS drag-and-drop failed; falling back to Actions-based drag. "
                    + "HTML5 drop targets may not recognize the fallback.", e);
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
