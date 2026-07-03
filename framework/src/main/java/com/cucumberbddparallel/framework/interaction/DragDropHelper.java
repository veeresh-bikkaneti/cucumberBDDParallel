package com.cucumberbddparallel.framework.interaction;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * HTML5 drag-and-drop via Selenium Actions (works for sortable lists and drop zones).
 */
public final class DragDropHelper {

    private final WebDriver driver;

    public DragDropHelper(WebDriver driver) {
        this.driver = driver;
    }

    public void dragAndDrop(WebElement source, WebElement target) {
        new Actions(driver)
                .clickAndHold(source)
                .pause(200)
                .moveToElement(target)
                .pause(200)
                .release()
                .perform();
    }
}