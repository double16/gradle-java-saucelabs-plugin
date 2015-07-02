package com.github.double16;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;

public class AbstractFunctionalTestTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String CONTEXT_ROOT = "/";
	private WebDriver driver;
    private AbstractFunctionalTest test;

    @Rule
    public TestName testName = new TestName();

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Before
    public void setup() throws Exception {
    	System.setProperty("functionalTests.baseUrl", BASE_URL);
        driver = Mockito.mock(WebDriver.class);
        test = new AbstractFunctionalTest() {
        	@Override
        	public String getContextRoot() {
        		return CONTEXT_ROOT;
        	}
        };
        test.driverFactory = new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) throws IOException {
                return driver;
            }

            @Override
            public String getIdentifier() {
                return String.valueOf(driver.hashCode());
            }
        };
        test.testName = testName;
        test.setUp();
    }

    @After
    public void teardown() {
        test.tearDown();
    }
    
    protected AbstractFunctionalTest testBaseUrl(final String baseUrl) {
    	System.setProperty("functionalTests.baseUrl", baseUrl);
        AbstractFunctionalTest test = new AbstractFunctionalTest() {
        	@Override
        	public String getContextRoot() {
        		return CONTEXT_ROOT;
        	}
        };
        test.driverFactory = new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) throws IOException {
                return driver;
            }

            @Override
            public String getIdentifier() {
                return String.valueOf(driver.hashCode());
            }
        };
        test.testName = testName;
        return test;
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateBaseUrlFull() throws Exception {
    	testBaseUrl("localhost").setUp();
    }
    
    @Test
    public void validateBaseUrlTrailingSlash() throws Exception {
    	testBaseUrl("http://localhost:8080/").setUp();
        test.home();
        Mockito.verify(driver).get(BASE_URL+CONTEXT_ROOT);
    }
    
    protected AbstractFunctionalTest testContextRoot(final String contextRoot) {
    	System.setProperty("functionalTests.baseUrl", BASE_URL);
        AbstractFunctionalTest test = new AbstractFunctionalTest() {
        	@Override
        	public String getContextRoot() {
        		return contextRoot;
        	}
        };
        test.driverFactory = new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) throws IOException {
                return driver;
            }

            @Override
            public String getIdentifier() {
                return String.valueOf(driver.hashCode());
            }
        };
        test.testName = testName;
        return test;
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void contextRootNull() throws Exception {
    	testContextRoot(null).setUp();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void contextRootEmpty() throws Exception {
    	testContextRoot("").setUp();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void contextRootBlank() throws Exception {
    	testContextRoot(" ").setUp();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void contextRootAppNoSlash() throws Exception {
    	testContextRoot("myapp").setUp();
    }
    
    @Test
    public void contextRootApp() throws Exception {
    	testContextRoot("myapp/").setUp();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void contextRootAppLeadingSlash() throws Exception {
    	testContextRoot("/myapp").setUp();
    }
    
    @Test
    public void goPage() {
        test.go(TestPage2.class);
        Mockito.verify(driver).get(BASE_URL + CONTEXT_ROOT + TestPage2.url);
    }

    @Test
    public void goRelative() {
        test.go(TestPage2.url);
        Mockito.verify(driver).get(BASE_URL + CONTEXT_ROOT + TestPage2.url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void goNoUrl() {
        test.go(TestPage1.class);
    }

    @Test
    public void home() {
        test.home();
        Mockito.verify(driver).get(BASE_URL+CONTEXT_ROOT);
    }

    @Test
    public void quick() {
        assertNotNull(test.quick());
    }

    @Test
    public void slow() {
        assertNotNull(test.slow());
    }

    @Test
    public void driversLocal() throws IOException {
        Path webdrivers = Paths.get("target/webdrivers");
        if (webdrivers.toFile().canRead()) {
            Files.walkFileTree(webdrivers, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Collection<WebDriverFactory[]> drivers = AbstractFunctionalTest.drivers();
        assertTrue(drivers.size() > 0);
    }

    @Test
    public void driversLocalChrome() throws IOException {
        System.setProperty("functionalTests.browser", "chrome");
        Collection<WebDriverFactory[]> drivers = AbstractFunctionalTest.drivers();
        collector.checkThat(drivers.size(), is(1));
        collector.checkThat(drivers.iterator().next()[0].toString(), is("chrome"));
    }

    @Test
    public void driversLocalFireFox() throws IOException {
        System.setProperty("functionalTests.browser", "firefox");
        Collection<WebDriverFactory[]> drivers = AbstractFunctionalTest.drivers();
        collector.checkThat(drivers.size(), is(1));
        collector.checkThat(drivers.iterator().next()[0].toString(), is("FireFox"));
    }

    @Test
    public void driversLocalIE() throws IOException {
        Assume.assumeTrue("OS is Windows", SystemUtils.IS_OS_WINDOWS);
        System.setProperty("functionalTests.browser", "ie");
        Collection<WebDriverFactory[]> drivers = AbstractFunctionalTest.drivers();
        collector.checkThat(drivers.size(), is(1));
        collector.checkThat(drivers.iterator().next()[0].toString(), is("IE"));
    }

    @Test
    public void driversLocalPhantomJS() throws IOException {
        System.setProperty("functionalTests.browser", "phantomjs");
        Collection<WebDriverFactory[]> drivers = AbstractFunctionalTest.drivers();
        collector.checkThat(drivers.size(), is(1));
        collector.checkThat(drivers.iterator().next()[0].toString(), is("phantomjs"));
    }

    @Test
    public void driversSeleniumGrid() throws IOException {
    	System.setProperty(AbstractFunctionalTest.SELENIUM_GRID, "http://ondemand.saucelabs.com:80/wd/hub");
    	System.setProperty(AbstractFunctionalTest.SELENIUM_GRID_USER, "sluser");
    	System.setProperty(AbstractFunctionalTest.SELENIUM_GRID_ACCESS_PASSWORD, "slpassword");
        System.setProperty("functionalTests.job-name", "functional test base");
        System.setProperty("functionalTests.build", "1");
        System.setProperty("functionalTests.browser", "browserName=internet explorer,platform=Windows 7,version=11");
        System.setProperty("functionalTests.browser.1", "browserName=internet explorer,platform=Windows 7,version=10");
        Collection<WebDriverFactory[]> drivers = AbstractFunctionalTest.drivers();
        assertEquals(2, drivers.size());
    }
    
    @Test
    public void resolveSeleniumPlatform() {
    	assertNull(AbstractFunctionalTest.resolveSeleniumPlatform(null));
    	assertEquals("", AbstractFunctionalTest.resolveSeleniumPlatform(""));
    	assertEquals("VISTA", AbstractFunctionalTest.resolveSeleniumPlatform("VISTA"));
    	assertEquals("WINDOWS", AbstractFunctionalTest.resolveSeleniumPlatform("WINDOWS"));
    	assertEquals("XP", AbstractFunctionalTest.resolveSeleniumPlatform("winnt"));
    	assertEquals("VISTA", AbstractFunctionalTest.resolveSeleniumPlatform("windows 7"));
    	assertEquals("VISTA", AbstractFunctionalTest.resolveSeleniumPlatform("Windows 7"));
    	assertEquals("MAC", AbstractFunctionalTest.resolveSeleniumPlatform("mac"));
    }
    
    @Test
    public void buildBrowserCapsWithInternetExplorerForSauceLabs() throws IOException {
    	Properties caps = AbstractFunctionalTest.buildCapabilities("browserName=internet_explorer,platform=win7,version=10", "http://ondemand.saucelabs.com:80");
    	assertEquals("2.42.0", caps.get("iedriver-version"));
    	assertEquals("VISTA", caps.get("platform"));
    	assertEquals("2.42.2", caps.get("selenium-version"));
    }
    
    @Test
    public void buildBrowserCapsWithFirefoxForSauceLabs() throws IOException {
    	Properties caps = AbstractFunctionalTest.buildCapabilities("browserName=firefox,platform=win7,version=10", "http://ondemand.saucelabs.com:80");
    	assertNull(caps.get("iedriver-version"));
    	assertEquals("VISTA", caps.get("platform"));
    	assertEquals("2.42.2", caps.get("selenium-version"));
    }
    
    @Test
    public void buildBrowserCapsWithInternetExplorerForInternalGrid() throws IOException {
    	Properties caps = AbstractFunctionalTest.buildCapabilities("browserName=internet_explorer,platform=win7,version=10", "http://192.168.1.10:80/wd/hub");
    	assertNull(caps.get("iedriver-version"));
    	assertEquals("VISTA", caps.get("platform"));
    	assertNull(caps.get("selenium-version"));
    }
    
    @Test
    public void buildBrowserCapsWithFirefoxForInternalGrid() throws IOException {
    	Properties caps = AbstractFunctionalTest.buildCapabilities("browserName=firefox,platform=win7,version=10", "http://192.168.1.10:80/wd/hub");
    	assertNull(caps.get("iedriver-version"));
    	assertEquals("VISTA", caps.get("platform"));
    	assertNull(caps.get("selenium-version"));
    }
}
