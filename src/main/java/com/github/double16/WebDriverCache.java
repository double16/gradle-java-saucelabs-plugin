package com.github.double16;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openqa.selenium.WebDriver;

/**
 * Caches web driver instances. Caching is optional so that conditional caching is handled in this class and not in all of the
 * callers. This class is not thread safe and not intended to handle concurrent uses of the same WebDriverFactory. It is important
 * when using caching that the browser state is considered by the caller. For example, if the browser is logged into a particular
 * user, the user should be logged out.
 * 
 * Caching is enabled by default.
 */
public class WebDriverCache {
    private Map<String, WebDriver> cache = new HashMap<String, WebDriver>();
    private boolean cacheEnabled = true;

    /**
     * Get or create a web driver using the given factory. This may returned a cached instance.
     * 
     * @return a new or cached WebDriver
     */
    public WebDriver getWebDriver(WebDriverFactory factory, String testName) throws IOException {
        if (!cacheEnabled) {
            return factory.createWebDriver(testName);
        }
        String cacheKey = factory.getIdentifier();
        WebDriver driver = cache.get(cacheKey);
        if (driver == null) {
            driver = factory.createWebDriver(testName);
            cache.put(cacheKey, driver);
        }
        return driver;
    }

    /**
     * Quit the driver if not-cached. Call this when done with the driver for a particular case.
     */
    public void maybeQuitWebDriver(WebDriver driver) {
        if (!cacheEnabled) {
            driver.quit();
        }
    }

    /**
     * Unconditionally quit all drivers. If caching is disabled, no driver will be quit. The {@link #maybeQuitWebDriver(WebDriver)}
     * is expected to quit drivers in this case.
     */
    public void quitAll() {
        Iterator<WebDriver> iterator = cache.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().quit();
            iterator.remove();
        }
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
}
