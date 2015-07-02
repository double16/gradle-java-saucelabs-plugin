package com.github.double16;

import java.util.Collections;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ByIdOrName;
import org.openqa.selenium.support.pagefactory.ByAll;

public class PageMockHelper {
    public static final String usernameStr = "username";
    public static final String passwordStr = "password";
    public static final String loginButtonStr = "login";
    public static final By username = By.id(usernameStr);
    public static final By password = By.id(passwordStr);
    public static final By loginButton = By.id(loginButtonStr);
    public static final By body = By.cssSelector("body");

    public static WebElement mockDisplayed(String tagName) {
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.isDisplayed()).thenReturn(true);
        Mockito.when(el.isEnabled()).thenReturn(true);
        Mockito.doNothing().when(el).clear();
        Mockito.doNothing().when(el).sendKeys(Mockito.<String> any());
        Mockito.doNothing().when(el).click();
        if (tagName != null) {
        	Mockito.when(el.getTagName()).thenReturn(tagName);
        }
        Dimension dim = new Dimension(100, 75);
        Mockito.when(el.getSize()).thenReturn(dim);
        return el;
    }

    public static WebElement mockHidden(String tagName) {
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.isDisplayed()).thenReturn(false);
        Mockito.when(el.isEnabled()).thenReturn(false);
        Mockito.doNothing().when(el).clear();
        Mockito.doNothing().when(el).sendKeys(Mockito.<String> any());
        Mockito.doNothing().when(el).click();
        if (tagName != null) {
        	Mockito.when(el.getTagName()).thenReturn(tagName);
        }
        Dimension dim = new Dimension(0, 0);
        Mockito.when(el.getSize()).thenReturn(dim);
        return el;
    }
    
    private static void mockIdAndNameFinder(WebDriver driver, String idOrName, WebElement element) {
        Mockito.when(driver.findElement(By.id(idOrName))).thenReturn(element);
        Mockito.when(driver.findElement(By.name(idOrName))).thenReturn(element);
        Mockito.when(driver.findElement(new ByIdOrName(idOrName))).thenReturn(element);
        
        Mockito.when(driver.findElements(By.id(idOrName))).thenReturn(Collections.singletonList(element));
        Mockito.when(driver.findElements(By.name(idOrName))).thenReturn(Collections.singletonList(element));
        Mockito.when(driver.findElements(new ByIdOrName(idOrName))).thenReturn(Collections.singletonList(element));
    }
    
    public static WebDriver createDriver() {
        final WebDriver driver = Mockito.mock(WebDriver.class);
        
        WebElement usernameEl = mockDisplayed("input");
        WebElement passwordEl = mockDisplayed("input");
        mockIdAndNameFinder(driver, usernameStr, usernameEl);
        mockIdAndNameFinder(driver, passwordStr, passwordEl);

        WebElement loginButtonEl = mockDisplayed("button");
        Mockito.when(driver.findElement(loginButton)).thenReturn(loginButtonEl);
        Mockito.when(driver.findElements(loginButton)).thenReturn(Collections.singletonList(loginButtonEl));
        
        WebElement bodyEl = mockDisplayed("body");
        Mockito.when(driver.findElement(body)).thenReturn(bodyEl);
        Mockito.when(driver.findElements(body)).thenReturn(Collections.singletonList(bodyEl));
        
        Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Mockito.when(driver.findElement(LoginPage.at)).thenThrow(new NoSuchElementException(""));
				return null;
			}
		}).when(loginButtonEl).click();
        
        return driver;
    }
}
