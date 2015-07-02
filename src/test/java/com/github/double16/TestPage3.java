package com.github.double16;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class TestPage3 extends AbstractPage {
	public static final By[] at = new By[] {
		By.cssSelector("h2"), By.name("username")
	};
    public static final String url = "page2";

    public TestPage3(WebDriver driver) {
        super(driver);
    }
}
