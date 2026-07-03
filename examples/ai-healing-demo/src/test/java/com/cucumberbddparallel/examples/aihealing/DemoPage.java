package com.cucumberbddparallel.examples.aihealing;

import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
/**
 * Page object with an intentionally stale locator ({@code #hplogo}) so healing must
 * suggest {@code #logo} from the fixture HTML.
 */
public class DemoPage extends BasePage {

    @FindBy(css = "#hplogo")
    private WebElement logo;

    public void assertLogoVisible() {
        if (!logo.isDisplayed()) {
            throw new AssertionError("Logo should be visible after healing");
        }
    }
}