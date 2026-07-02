package com.cucumberbddparallel.framework.page;

import com.cucumberbddparallel.framework.ai.AiConfig;
import com.cucumberbddparallel.framework.ai.AiElementLocatorFactory;
import com.cucumberbddparallel.framework.driver.DriverManager;
import com.cucumberbddparallel.framework.wait.Wait;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

/**
 * Extend this for every page object in your suite (see {@code HomePage} / {@code SearchResultPage}
 * in example-tests for what that looks like). It gives you a ready-to-use {@code driver} and
 * {@code wait}, and wires up your {@code @FindBy} fields automatically - you never call
 * {@code PageFactory.initElements} yourself.
 *
 * The one interesting decision here is which locator strategy your {@code @FindBy} fields
 * get: the constructor checks {@link AiConfig#isHealingEnabled()} once, and picks either
 * Selenium's own {@link DefaultElementLocatorFactory} (locators fail immediately, like
 * normal) or {@link AiElementLocatorFactory} (locators get one AI-assisted retry before
 * failing). Your page object code doesn't know or care which one it got - that's the whole
 * point of coding against the {@link ElementLocatorFactory} interface instead of a concrete
 * class.
 */
public abstract class BasePage {

    protected WebDriver driver;
    protected Wait wait;

    public BasePage() {
        this.driver = DriverManager.get();
        this.wait = new Wait(this.driver);
        ElementLocatorFactory locatorFactory = AiConfig.isHealingEnabled()
                ? new AiElementLocatorFactory(this.driver)
                : new DefaultElementLocatorFactory(this.driver);
        PageFactory.initElements(locatorFactory, this);
    }
}
