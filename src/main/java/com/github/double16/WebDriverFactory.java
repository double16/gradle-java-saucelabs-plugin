package com.github.double16;

import java.io.IOException;

import org.openqa.selenium.WebDriver;

public interface WebDriverFactory {
    WebDriver createWebDriver() throws IOException;
}
