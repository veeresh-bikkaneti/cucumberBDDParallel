package com.cucumberbddparallel.framework.page;

import com.cucumberbddparallel.framework.ai.AiConfig;
import com.cucumberbddparallel.framework.ai.AiElementLocatorFactory;
import com.cucumberbddparallel.framework.driver.DriverManager;
import com.cucumberbddparallel.framework.wait.Wait;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

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
