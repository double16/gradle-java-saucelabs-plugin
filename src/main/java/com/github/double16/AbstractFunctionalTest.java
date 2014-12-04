package com.github.double16;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public abstract class AbstractFunctionalTest {
  protected ThreadLocal<NumberFormat> REPORT_OUTPUT_FORMAT = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      return new DecimalFormat("000");
    }
  };

  protected WebDriver driver;
  protected String baseUrl;
  protected File reportDir;
  protected int reportOutputNum = 1;

  @Rule
  public TestName testName = new TestName();

  private static String createBrowserSpecSystemPropertyName(int num) {
    if (num < 1) {
      return "saucelabs.browser";
    }
    return "saucelabs.browser."+num;
  }

  private static void fixupPlatform(Map caps) {
    String platform = (String) caps.get(CapabilityType.PLATFORM);
    if (platform != null) {
      for(Platform p : Platform.values()) {
        for(String osName : p.getPartOfOsName()) {
          if (osName.equalsIgnoreCase(platform)) {
            caps.put(CapabilityType.PLATFORM, p.name());
            return;
          }
        }
      }
    }
  }

  @Parameterized.Parameters
  public static Collection<WebDriver[]> drivers() throws IOException {
    List<WebDriver[]> drivers = new LinkedList<WebDriver[]>();

    int driverSpecNum = 0;
    String spec;
    while ((spec = System.getProperty(createBrowserSpecSystemPropertyName(driverSpecNum++))) != null) {
      Properties browserCaps = new Properties();
      browserCaps.put("name", System.getProperty("saucelabs.job-name", ""));
      browserCaps.put("build", System.getProperty("saucelabs.build", ""));
      browserCaps.load(new StringReader(spec.replaceAll(",", "\n")));
      fixupPlatform(browserCaps);
      DesiredCapabilities capabilities = new DesiredCapabilities((Map) browserCaps);
      WebDriver driver = new RemoteWebDriver(
          new URL("http://" + System.getenv("SAUCE_LABS_USER") + ":" +
              System.getenv("SAUCE_LABS_ACCESS_PASSWORD") + "@ondemand.saucelabs.com:80/wd/hub"),
          capabilities);
      String sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
      System.out.println("SauceOnDemandSessionID=" + sessionId);
      drivers.add(new WebDriver[] { driver });
    }

    if (drivers.isEmpty()) {
      try {
        drivers.add(new WebDriver[] { new PhantomJSDriver() });
      } catch (Exception e) {
        System.err.println("Unable to locate PhantomJS driver");
      }
    }

    return drivers;
  }

  protected AbstractFunctionalTest(WebDriver driver) {
    this.driver = driver;
  }

  @Before
  public void setUp() throws Exception {
    reportDir = new File(new File(System.getProperty("functionalTests.resultsDir", "build/reports/tests")), testName.getMethodName());
    reportDir.mkdirs();

    driver.get(baseUrl);
  }

  @After
  public void tearDown() {
    report("end");
    driver.close();
    driver.quit();
  }

  /**
   * Go to the given page. The class is expected to have a public static final String field named 'url' containing
   * the relative URL. The value will be appended to {@link #baseUrl}. If the page constructor performs an 'at' check,
   * the exception will be thrown here.
   * @param page the page class.
   * @return page instance.
   */
  public <T> T go(Class<T> page) {
    String relative = null;
    try {
      Field urlField = page.getDeclaredField("url");
      if (urlField != null && Modifier.isStatic(urlField.getModifiers())) {
        relative = (String) urlField.get(null);
      }
    } catch (NoSuchFieldException e) {
      // handled below
    } catch (IllegalAccessException e) {
      // handled below
    } catch (ClassCastException e) {
      // handled below
    }
    if (relative == null) {
      throw new IllegalArgumentException(page + " must define 'public static final String url'");
    }
    driver.get(baseUrl+relative);
    return at(page);
  }

  /**
   * Returns an instance of the page. If the page constructor performs an 'at' check, the exception will be
   * thrown here.
   * @param page the page class.
   * @return page instance.
   */
  public <T> T at(Class<T> page) {
    return PageFactory.initElements(driver, page);
  }

  public void report(String name) {
    try {
      File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      FileUtils.copyFile(screenshot, new File(reportDir, REPORT_OUTPUT_FORMAT.get().format(reportOutputNum++) + "-" + name + ".png"));
    } catch (IOException e) {
      e.printStackTrace(System.err);
    }
  }
}
