package com.github.double16;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AbstractPageTest {
    private WebDriver driver;
    private FunctionalTestUtils utils;
    private By byIdMissing = By.id("idDoesNotExist");
    private WebElement username;

    @Before
    public void setup() {
        driver = PageMockHelper.createDriver();
        utils = new FunctionalTestUtils(driver);
        username = driver.findElement(PageMockHelper.username);
    }

    @Test
    public void checkWithAt() {
        LoginPage page = utils.at(LoginPage.class);
        page.check();
    }

    @Test
    public void checkWithAtArray() {
        Collection<By> checkers = utils.getAtCheckers(TestPage3.class);
        assertEquals(2, checkers.size());
    }

    @Test
    public void checkWithAtCollection() {
        Collection<By> checkers = utils.getAtCheckers(TestPage4.class);
        assertEquals(2, checkers.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkWithAtString() {
        utils.getAtCheckers(BadLocatorPage.class);
    }

    @Test
    public void checkNoAt() {
        TestPage1 page = utils.at(TestPage1.class);
        page.check();
    }

    @Test
    public void testQuick() {
        LoginPage page = utils.at(LoginPage.class);
        assertNotNull(page.quick());
    }

    @Test
    public void testSlow() {
        LoginPage page = utils.at(LoginPage.class);
        assertNotNull(page.slow());
    }

    @Test
    public void testQuickLocator() {
        LoginPage page = utils.at(LoginPage.class);
        page.quick(PageMockHelper.username);
    }

    @Test(expected = Exception.class)
    public void testQuickFail() {
        LoginPage page = utils.at(LoginPage.class);
        page.quick(byIdMissing);
    }

    @Test
    public void testSlowLocator() {
        LoginPage page = utils.at(LoginPage.class);
        page.slow(PageMockHelper.username);
    }

    @Test
    public void at() {
        LoginPage page = utils.at(LoginPage.class);
        assertNotNull(page.at(TestPage1.class));
    }

    @Test
    public void getValue() {
        LoginPage page = utils.at(LoginPage.class);
        Mockito.when(username.getAttribute("value")).thenReturn("user123");
        assertEquals("user123", page.value(username));
    }

    @Test(expected = NoSuchElementException.class)
    public void getValueNotExist() {
        LoginPage page = utils.at(LoginPage.class);
        page.value(null);
    }

    @Test
    public void setValue() {
        LoginPage page = utils.at(LoginPage.class);
        page.value(username, "user999");
        Mockito.verify(username).clear();
        Mockito.verify(username).sendKeys("user999");
    }

    @Test(expected = NoSuchElementException.class)
    public void setValueNotExist() {
        LoginPage page = utils.at(LoginPage.class);
        page.value(null, "abc");
    }

    @Test
    public void setValueBooleanTrueWhenFalse() {
        LoginPage page = utils.at(LoginPage.class);
        Mockito.when(username.isSelected()).thenReturn(false);
        page.value(username, true);
        Mockito.verify(username).isSelected();
        Mockito.verify(username).click();
    }

    @Test
    public void setValueBooleanTrueWhenTrue() {
        LoginPage page = utils.at(LoginPage.class);
        Mockito.when(username.isSelected()).thenReturn(true);
        page.value(username, true);
        Mockito.verify(username).isSelected();
        Mockito.verify(username, Mockito.never()).click();
    }

    @Test
    public void setValueBooleanFalseWhenTrue() {
        LoginPage page = utils.at(LoginPage.class);
        Mockito.when(username.isSelected()).thenReturn(true);
        page.value(username, false);
        Mockito.verify(username).isSelected();
        Mockito.verify(username).click();
    }

    @Test
    public void setValueBooleanFalseWhenFalse() {
        LoginPage page = utils.at(LoginPage.class);
        Mockito.when(username.isSelected()).thenReturn(false);
        page.value(username, false);
        Mockito.verify(username).isSelected();
        Mockito.verify(username, Mockito.never()).click();
    }

    @Test(expected = NoSuchElementException.class)
    public void setValueBooleanNotExist() {
        LoginPage page = utils.at(LoginPage.class);
        page.value(null, true);
    }
    
    @Test
    public void setValueSelectByValue() {
        LoginPage page = utils.at(LoginPage.class);
    	Mockito.when(username.getTagName()).thenReturn("select");
    	WebElement option1 = PageMockHelper.mockDisplayed("option");
    	Mockito.when(username.findElements(Mockito.<By> any())).thenReturn(Collections.singletonList(option1));
        page.value(username, "C");
    }

    @Test
    public void setValueSelectByText() {
        LoginPage page = utils.at(LoginPage.class);
    	Mockito.when(username.getTagName()).thenReturn("select");
    	Mockito.when(username.findElements(Mockito.<By> any())).thenReturn(Collections.<WebElement> emptyList()).thenReturn(Collections.singletonList(PageMockHelper.mockDisplayed("option")));
        page.value(username, "Third");
    }

    @Test(expected = NoSuchElementException.class)
    public void setValueSelectNotExist() {
        LoginPage page = utils.at(LoginPage.class);
    	Mockito.when(username.getTagName()).thenReturn("select");
    	Mockito.when(username.findElements(Mockito.<By> any())).thenReturn(Collections.<WebElement> emptyList());
        page.value(username, "Third");
    }
}
