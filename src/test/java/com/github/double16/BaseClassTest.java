package com.github.double16;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link com.github.double16.AbstractFunctionalTest}.
 */
public class BaseClassTest {
  WebDriver driver;
  AbstractFunctionalTest test;

  @Rule
  public TestName testName = new TestName();

  @Before
  public void setUp() throws Exception {
    driver = mock(WebDriver.class);
    test = new AbstractFunctionalTest() { };
    test.driverFactory = new WebDriverFactory() {
      @Override
      public WebDriver createWebDriver() throws IOException {
        return driver;
      }
    };
    test.baseUrl = "http://localhost:8080/";
    test.testName = testName;
    test.setUp();
  }

  @Test
  public void testAt() {
    SamplePage1 page = test.at(SamplePage1.class);
    Assert.assertNotNull(page);
  }

  @Test
  public void testGo() {
    SamplePage1 page = test.go(SamplePage1.class);
    Assert.assertNotNull(page);
    verify(driver).get("http://localhost:8080/samplePage1");
  }

  @Test(expected = Exception.class)
  public void testGoFailure() {
    test.go(SamplePage2.class);
  }
}
