package com.github.double16;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage extends AbstractPage {
    public static final By at = By.id("password");

    @FindBy(how = How.ID_OR_NAME, using = "username")
    WebElement username;
    @FindBy(how = How.ID_OR_NAME, using = "password")
    WebElement password;
    @FindBy(how = How.ID_OR_NAME, using = "login")
    WebElement loginButton;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Login with the username and password and return the desired page.
     * 
     * @param page the page to expect, may be null
     * @return the expected page (or null if the page argument is null), if the constructor does an "at" check, an exception could
     * be thrown from this method.
     */
    public <T> T login(String username, String password, Class<T> page) {
        value(this.username, username);
        value(this.password, password);
        this.loginButton.click();
        
        WebDriverWait waitVar = new WebDriverWait(driver, 10);
        waitVar.until(ExpectedConditions.invisibilityOfElementLocated(at));
        waitVar.ignoring(NoSuchElementException.class).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("body")));
        if (page == null) {
            return null;
        }
        return PageFactory.initElements(driver, page);
    }

    /**
     * Same as {@link #login(String, String, Class)} with no expected page (page argument is null).
     */
    public <T> T login(String username, String password) {
        return (T) login(username, password, null);
    }    
}
