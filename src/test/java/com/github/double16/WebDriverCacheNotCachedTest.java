package com.github.double16;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.openqa.selenium.WebDriver;

public class WebDriverCacheNotCachedTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    private WebDriverCache cache;
    private WebDriverFactory testFactory1;
    private WebDriverFactory testFactory2;

    @Before
    public void setup() {
        cache = new WebDriverCache();
        cache.setCacheEnabled(false);

        testFactory1 = new WebDriverFactory() {
            @Override
            public String getIdentifier() {
                return "test1";
            }

            @Override
            public WebDriver createWebDriver(String testName) throws IOException {
                return mock(WebDriver.class, withSettings().defaultAnswer(RETURNS_SMART_NULLS));
            }
        };

        testFactory2 = new WebDriverFactory() {
            @Override
            public String getIdentifier() {
                return "test2";
            }

            @Override
            public WebDriver createWebDriver(String testName) throws IOException {
                return mock(WebDriver.class, withSettings().defaultAnswer(RETURNS_SMART_NULLS));
            }
        };
    }

    @Test
    public void testGetWebDriver() throws IOException {
        WebDriver driver1a = cache.getWebDriver(testFactory1, "a");
        Assert.assertNotNull(driver1a);
        WebDriver driver1b = cache.getWebDriver(testFactory1, "a");
        collector.checkThat("Same instances for the same factory", driver1a, not(sameInstance(driver1b)));
    }

    @Test
    public void testMaybeQuitWebDriver() throws IOException {
        WebDriver driver1 = cache.getWebDriver(testFactory1, "a");
        cache.maybeQuitWebDriver(driver1);
        verify(driver1, times(1)).quit();
    }

    @Test
    public void testQuitAll() throws IOException {
        WebDriver driver1 = cache.getWebDriver(testFactory1, "a");
        WebDriver driver2 = cache.getWebDriver(testFactory2, "a");
        cache.quitAll();
        verify(driver1, never()).quit();
        verify(driver2, never()).quit();
    }
}
