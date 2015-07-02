package com.github.double16;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class TestPage4 extends AbstractPage {
	public static final List<By> at = Arrays.asList(new By[] {
		By.cssSelector("h2"), By.name("username")
	});
    public static final String url = "page2";

    public TestPage4(WebDriver driver) {
        super(driver);
    }
}
