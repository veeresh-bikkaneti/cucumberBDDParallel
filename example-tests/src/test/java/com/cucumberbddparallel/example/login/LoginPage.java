package com.cucumberbddparallel.example.login;

import com.cucumberbddparallel.example.support.AppServerHooks;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Page object for the login demo ({@code /login}) - a straightforward form: two fields,
 * a button, and a welcome message once you're in.
 */
public class LoginPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(LoginPage.class);

    @FindBy(css = "#username")
    private WebElement usernameField;

    @FindBy(css = "#password")
    private WebElement passwordField;

    @FindBy(css = "#login-button")
    private WebElement loginButton;

    @FindBy(css = "#welcome")
    private WebElement welcomeMessage;

    /** Public so step classes can {@code new LoginPage()} in their {@code @Before} hooks. */
    public LoginPage() {
    }

    void goToLoginPage() {
        driver.get(AppServerHooks.baseUrl() + "/login");
        wait.forLoading(5);
    }

    void loginAs(String username, String password) {
        wait.forElementToBeDisplayed(5, this.usernameField, "Username field").clear();
        this.usernameField.sendKeys(username);
        wait.forElementToBeDisplayed(5, this.passwordField, "Password field").clear();
        this.passwordField.sendKeys(password);
        this.loginButton.click();
    }

    void checkWelcomeMessage(String expectedMessage) {
        WebElement welcome = wait.forElementToBeDisplayed(5, this.welcomeMessage, "Welcome message");
        String actual = welcome.getText();
        LOG.info("Welcome message: expected \"{}\", displayed \"{}\"", expectedMessage, actual);
        Assert.assertEquals(actual, expectedMessage, "Welcome message did not match");
    }
}
