package com.github.double16;

import java.io.IOException;

import org.openqa.selenium.WebDriver;

public interface WebDriverFactory {
    WebDriver createWebDriver(String testName) throws IOException;

    /**
     * Get a unique identifier for this factory. Factories that create the same web driver with the same capabilities ought to
     * return the same identifier.
     */
    String getIdentifier();
}
