package com.cucumberparallel.basepage;

import com.cucumberparallel.hookup.driver.Wait;
import com.cucumberparallel.hookup.driver.Setup;
import org.openqa.selenium.WebDriver;

public abstract class BasePage {

    protected WebDriver driver;
    protected Wait wait;

    public BasePage() {
        this.driver = Setup.driver;
        this.wait = new Wait(this.driver);
    }
}
