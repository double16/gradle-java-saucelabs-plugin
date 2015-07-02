package com.github.double16;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;

public abstract class AbstractPage {
    protected final WebDriver driver;
    protected final FunctionalTestUtils utils;

    public AbstractPage(WebDriver driver) {
        this.driver = driver;
        this.utils = new FunctionalTestUtils(driver);
        check();
    }

    /**
     * The maximum time to wait for an 'at' check. This may need to be increased for pages loaded by JavaScript or with async.
     */
    protected int getAtCheckWait() {
        try {
            return Integer.parseInt(System.getProperty("functionalTests.atCheckWait", "3"));
        } catch (Exception e) {
            return 3;
        }
    }

    /**
     * Perform an "at" check to ensure we're on this page. The preferred approach to "at" checking is to define a static final
     * {@link By} field named "at". The field may also be an array or {@link Collection} of By elements.
     */
    public void check() {
    	for(By by : utils.getAtCheckers(getClass())) {
            ExpectedCondition<WebElement> condition = ExpectedConditions.presenceOfElementLocated(by);
            new FluentWait<WebDriver>(driver).withTimeout(getAtCheckWait(), TimeUnit.SECONDS).pollingEvery(1, TimeUnit.SECONDS)
                    .ignoring(NoSuchElementException.class).until(condition);
    	}
    }

    public Wait<WebDriver> quick() {
        return utils.quick();
    }

    public void quick(By locator) {
        utils.quick(locator);
    }

    public Wait<WebDriver> slow() {
        return utils.slow();
    }

    public void slow(By locator) {
        utils.slow(locator);
    }

    /**
     * Returns an instance of the page. If the page constructor performs an 'at' check, the exception will be thrown here.
     * 
     * @param page the page class.
     * @return page instance.
     */
    public <T> T at(Class<T> page) {
        return utils.at(page);
    }

    protected void exists(WebElement element) {
        if (element == null) {
            throw new NoSuchElementException("Missing element");
        }
    }

    public String value(WebElement element) {
        exists(element);
        return element.getAttribute("value");
    }

    public void value(WebElement element, String value) {
        exists(element);
		quick().until(ExpectedConditions.elementToBeClickable(element));
        if ("select".equalsIgnoreCase(element.getTagName())) {
        	Select select = new Select(element);
        	try {
            	select.selectByValue(value);
        	} catch (NoSuchElementException e) {
        		try {
        			select.selectByVisibleText(value);
        		} catch (NoSuchElementException e2) {
        			throw e;
        		}
        	}
        } else {
            element.clear();
            element.sendKeys(value);
        }
    }

    public void value(WebElement element, boolean value) {
        exists(element);
		quick().until(ExpectedConditions.elementToBeClickable(element));
        if (value) {
            if (!element.isSelected()) {
                element.click();
            }
        } else {
            if (element.isSelected()) {
                element.click();
            }
        }
    }
    
    public <T> T NavigateMenu(String path, Class<T> page) {
        return utils.navigateMenu(path, page);
    }
}
