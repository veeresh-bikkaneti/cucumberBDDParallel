package com.cucumberparallel.basepage;

import com.cucumberparallel.ai.AiConfig;
import com.cucumberparallel.ai.AiElementLocatorFactory;
import com.cucumberparallel.hookup.driver.Wait;
import com.cucumberparallel.hookup.driver.Setup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

public abstract class BasePage {

    protected WebDriver driver;
    protected Wait wait;

    public BasePage() {
        this.driver = Setup.driver;
        this.wait = new Wait(this.driver);
        // Two implementations of the same ElementLocatorFactory interface: Selenium's own
        // default, and the AI-healing one. AI is opt-in (ANTHROPIC_API_KEY) — never forced.
        ElementLocatorFactory locatorFactory = AiConfig.isHealingEnabled()
                ? new AiElementLocatorFactory(this.driver)
                : new DefaultElementLocatorFactory(this.driver);
        PageFactory.initElements(locatorFactory, this);
    }
}
