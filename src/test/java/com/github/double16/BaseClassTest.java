package com.github.double16;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link com.github.double16.AbstractFunctionalTest}.
 */
public class BaseClassTest {
  WebDriver driver;
  AbstractFunctionalTest test;

  @Before
  public void setUp() {
    driver = mock(WebDriver.class);
    test = new AbstractFunctionalTest(driver) { };
    test.baseUrl = "http://localhost:8080/";
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
