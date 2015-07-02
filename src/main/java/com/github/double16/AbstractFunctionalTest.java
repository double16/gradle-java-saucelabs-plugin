package com.github.double16;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;

/**
 * Base class for functional tests. The browser choices depend on environment:
 * 
 * Local development:
 * 1. phantomjs, a headless WebKit browser
 * 2. Chrome
 * 
 * Selenium Grid:
 * 1. environment variable "SELENIUM_FULL_URL" targeting the grid to use, i.e. https://ondemand.saucelabs.com:80/wd/hub
 * 2. environment variable "SELENIUM_GRID_USER" with grid user name
 * 3. environment variable "SELENIUM_GRID_ACCESS_PASSWORD" with grid password/access key
 * 4. system property "functionalTests.browser" with grid properties to specify the browser, either newline or comma separate,
 * i.e. "browserName=firefox,platform=win7". "platform" must be an enum value from org.openqa.selenium.Platform or part defined in the enum constructor.
 *
 */
public abstract class AbstractFunctionalTest {
	private static final Logger log = Logger.getLogger(AbstractFunctionalTest.class);
	
	static final String SELENIUM_GRID = "SELENIUM_FULL_URL";
	static final String SELENIUM_GRID_USER = "SELENIUM_GRID_USER";
    static final String SELENIUM_GRID_ACCESS_PASSWORD = "SELENIUM_GRID_ACCESS_PASSWORD";
    private static final WebDriverService WEB_DRIVER_SERVICE = new WebDriverService();
    protected static final WebDriverCache WEB_DRIVER_CACHE = new WebDriverCache();
    
	protected final ThreadLocal<NumberFormat> REPORT_OUTPUT_FORMAT = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            return new DecimalFormat("000");
        }
    };

    static {
        Runtime.getRuntime().addShutdownHook(new Thread("WebDriver cache cleanup") {
            @Override
            public void run() {
                WEB_DRIVER_CACHE.quitAll();
            }
        });
    }

    static {
        WEB_DRIVER_CACHE.setCacheEnabled(Boolean.valueOf(System.getProperty("functionalTests.cacheBrowser", "true")));
    }

    @Parameter(0)
    public WebDriverFactory driverFactory;
    protected FunctionalTestUtils utils;
    protected WebDriver driver;
    protected String baseUrl;
    protected File reportDir;
    protected int reportOutputNum = 1;

    @Rule
    public TestName testName = new TestName();
    
    @Rule
    public final ErrorCollector collector = new ErrorCollector();
    
    private static String createBrowserSpecSystemPropertyName(int num) {
        if (num < 1) {
            return "functionalTests.browser";
        }
        return "functionalTests.browser." + num;
    }

    /**
     * Resolve the specified platform to the Selenium enum.
     */
    static String resolveSeleniumPlatform(String platform) {
    	if (StringUtils.isBlank(platform)) {
    		return platform;
    	}
    	for(Platform p : Platform.values()) {
    		if (platform.equalsIgnoreCase(p.name())) {
    			return p.name();
    		}
    		for(String pn : p.getPartOfOsName()) {
    			if (platform.equalsIgnoreCase(pn)) {
    				return p.name();
    			}
    		}
    	}
		return platform;
    }
    
    private static String getPropertyOrEnv(String name) {
    	return System.getProperty(name, System.getenv(name));
    }
    
    static Properties buildCapabilities(String spec, String seleniumGridStr) throws IOException {
        Properties browserCaps = new Properties();
        browserCaps.load(new StringReader(spec.replaceAll(",", "\n")));
        if (seleniumGridStr.contains("saucelabs")) {
            browserCaps.put("selenium-version", "2.42.2");
            String browserName = browserCaps.getProperty("browserName");
            if (StringUtils.isNotBlank(browserName) 
            		&& browserName.replaceAll("[^A-Za-z]", "").equalsIgnoreCase("internetexplorer")) {
            	browserCaps.put("iedriver-version", "2.42.0");
            }        	
        }
        browserCaps.put("name", System.getProperty("functionalTests.job-name", ""));
        browserCaps.put("build", System.getProperty("functionalTests.build", ""));
        browserCaps.put("platform", resolveSeleniumPlatform((String) browserCaps.get("platform")));
        return browserCaps;
    }
    
    private static WebDriverFactory createRemoteWebDriverFactory(String spec) throws IOException {
        final String seleniumGridStr = getPropertyOrEnv(SELENIUM_GRID);
        Properties browserCaps = buildCapabilities(spec, seleniumGridStr);
        final DesiredCapabilities capabilities = new DesiredCapabilities((Map) browserCaps);
        capabilities.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) throws IOException {
            	if (StringUtils.isNotBlank(testName)) {
                	capabilities.setCapability("name", testName);
            	}
				return new RemoteWebDriver(new URL(seleniumGridStr), capabilities);
            }
            @Override
            public String getIdentifier() {
                return sanitizeForFilesystem(capabilities.getBrowserName() + "_" + capabilities.getVersion() + "_"
                        + capabilities.getPlatform());
            }

            @Override
            public String toString() {
                return getIdentifier();
            }
        };
    }
    
    @Parameterized.Parameters(name = "{0}")
    @SuppressWarnings("PMD")
    public static Collection<WebDriverFactory[]> drivers() throws IOException {
        List<WebDriverFactory[]> drivers = new LinkedList<WebDriverFactory[]>();

        int driverSpecNum = 0;
        String spec;
        while ((spec = System.getProperty(createBrowserSpecSystemPropertyName(driverSpecNum++))) != null) {
            if ("chrome".equalsIgnoreCase(spec)) {
                try {
                    drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createChromeDriverFactory() });
                } catch (Exception e) {
                    System.err.println("Unable to locate Chrome driver: " + e.getMessage());
                }
            } else if ("firefox".equalsIgnoreCase(spec)) {
                try {
                    drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createFirefoxDriverFactory() });
                } catch (Exception e) {
                    System.err.println("Unable to locate Firefox driver: " + e.getMessage());
                }
            } else if ("ie".equalsIgnoreCase(spec) || "internetexplorer".equalsIgnoreCase(spec)) {
                try {
                    drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createInternetExplorerFactory() });
                } catch (Exception e) {
                    System.err.println("Unable to locate Internet Explorer driver: " + e.getMessage());
                }
            } else if ("phantomjs".equalsIgnoreCase(spec)) {
                try {
                    drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createPhantomJSDriverFactory() });
                } catch (Exception e) {
                    System.err.println("Unable to locate PhantomJS driver: " + e.getMessage());
                }
            } else {
                if (StringUtils.isBlank(getPropertyOrEnv(SELENIUM_GRID_USER))
                        || StringUtils.isBlank(getPropertyOrEnv(SELENIUM_GRID_USER))
                        || StringUtils.isBlank(getPropertyOrEnv(SELENIUM_GRID_ACCESS_PASSWORD))) {
                    throw new IOException("Missing required environment variables for selenium grid: " + SELENIUM_GRID + ", "
                            + SELENIUM_GRID_USER + " and " + SELENIUM_GRID_ACCESS_PASSWORD);
                }
                drivers.add(new WebDriverFactory[] { createRemoteWebDriverFactory(spec) });
            }
        }

        if (drivers.isEmpty()) {
            try {
                drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createChromeDriverFactory() });
            } catch (Exception e) {
                System.err.println("Unable to locate Chrome driver: " + e.getMessage());
            }
            try {
                drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createFirefoxDriverFactory() });
            } catch (Exception e) {
                System.err.println("Unable to locate Firefox driver: " + e.getMessage());
            }
            try {
                drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createInternetExplorerFactory() });
            } catch (Exception e) {
                System.err.println("Unable to locate Internet Explorer driver: " + e.getMessage());
            }
            // FYI: phantomjs sometimes requires fixes that other browsers don't
            try {
                drivers.add(new WebDriverFactory[] { WEB_DRIVER_SERVICE.createPhantomJSDriverFactory() });
            } catch (Exception e) {
                System.err.println("Unable to locate PhantomJS driver: " + e.getMessage());
            }
        }

        return drivers;
    }

    /**
     * Return the context root for the application. This does not include the beginning of
     * the URL through the hostname and port. The context root must not start with a slash and
     * must end with a slash. The smallest acceptable value is "/". 
     */
    public abstract String getContextRoot();
    
    private static String sanitizeForFilesystem(String str) {
        if (str == null) {
            return "";
        }
    	return str.replaceAll("[^A-Za-z0-9= ]+", "_");
    }
    
    @Before
    public void setUp() throws Exception {
    	this.baseUrl = System.getProperty("functionalTests.baseUrl", "http://localhost:10039");
    	if (!this.baseUrl.contains("://")) {
    		throw new IllegalArgumentException("functionalTests.baseUrl must be in the form http://localhost:10039");
    	}
    	if (this.baseUrl.endsWith("/")) {
    		this.baseUrl = this.baseUrl.substring(0, this.baseUrl.length()-1);
    	}
    	if (StringUtils.isBlank(getContextRoot()) || !getContextRoot().endsWith("/") || (getContextRoot().length() > 1 && getContextRoot().startsWith("/"))) {
    		throw new IllegalArgumentException("getContextRoot() must not be empty, must not begin with a slash and must end with a slash. It may be '/'.");
    	}
    	if ("/".equals(getContextRoot())) {
        	this.baseUrl += getContextRoot();
    	} else {
    		this.baseUrl += "/" + getContextRoot();
    	}
        this.driver = WEB_DRIVER_CACHE.getWebDriver(driverFactory, getClass().getSimpleName() + "." + testName.getMethodName());
        this.utils = new FunctionalTestUtils(driver);
        String browser = driver.getClass().getSimpleName();
        if (driver instanceof RemoteWebDriver) {
        	browser = ((RemoteWebDriver) driver).getCapabilities().getBrowserName();
        }
        reportDir = new File(new File(new File(System.getProperty("functionalTests.resultsDir", "build/functional-test-results/"
                + browser + "/artifacts")), getClass().getSimpleName()), sanitizeForFilesystem(testName.getMethodName()));
        reportDir.mkdirs();
        // clean up previous test results
        File[] oldFiles = reportDir.listFiles();
        if (oldFiles != null) {
        	for(File f : oldFiles) {
        		f.delete();
        	}
        }
        System.out.println("Reports in " + reportDir.getAbsolutePath());
    }

    @After
    public void tearDown() {
    	if (driver != null) {
        	try {
                report("end");    		
        	} finally {
                WEB_DRIVER_CACHE.maybeQuitWebDriver(driver);
        	}    		
    	}
    }

    @Rule
    public final TestRule sauceUpdater = new TestWatcher() {
    	private void updateSauce(Map<String, Object> updates) {
    		String seleniumGridUrl = getPropertyOrEnv(SELENIUM_GRID);
    		if (seleniumGridUrl == null || !seleniumGridUrl.contains("saucelabs.com")) {
    			return;
    		}
    		
    		try {
        		SauceREST sauceREST = new SauceREST(getPropertyOrEnv(SELENIUM_GRID_USER), getPropertyOrEnv(SELENIUM_GRID_ACCESS_PASSWORD));
        		String sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
        		sauceREST.updateJobInfo(sessionId, updates);    			
    		} catch (Exception e) {
    			log.warn("Error updating Sauce Labs test results: "+e.toString());
    		}
    	}
    	
    	protected void succeeded(org.junit.runner.Description description) {
    		Map<String, Object> updates = new HashMap<String, Object>();
    		updates.put("passed", Boolean.TRUE);
    		updateSauce(updates);
    	}

        protected void failed(Throwable e, org.junit.runner.Description description) {
    		Map<String, Object> updates = new HashMap<String, Object>();
    		updates.put("passed", Boolean.FALSE);
    		updateSauce(updates);
    	}
    };
	
    /**
     * Go to the given page. The class is expected to have a public static final String field named 'url' containing the relative
     * URL. The value will be appended to {@link #baseUrl}. If the page constructor performs an 'at' check, the exception will be
     * thrown here.
     * 
     * @param page the page class.
     * @return page instance.
     */
    public <T> T go(Class<T> page) {
        String relative = utils.getUrl(page);
        if (relative == null) {
            throw new IllegalArgumentException(page + " must define 'public static final String url'");
        }
        driver.get(baseUrl + relative);
        return at(page);
    }

    /**
     * Go to the given relative URL. The value will be appended to {@link #baseUrl}.
     */
    public void go(String relative) {
        driver.get(baseUrl + relative);
    }

    /**
     * Returns an instance of the page. If the page constructor performs an 'at' check, the exception will be thrown here.
     * 
     * @param page the page class.
     * @return page instance.
     */
    public <T> T at(Class<T> page) {
        return utils.at(page);
    }

    /**
     * Go to the base URL.
     */
    public void home() {
        driver.get(baseUrl);
    }

    public void report(String name) {
        try {
        	utils.report(reportDir, REPORT_OUTPUT_FORMAT.get().format(reportOutputNum++) + "-"
                    + name.replaceAll("[^A-Za-z0-9-]+", "_"));
        } catch (WebDriverException e) {
            log.error("Reporting screen shot + HTML", e);
        } catch (IOException e) {
            log.error("Reporting screen shot + HTML", e);
        }
    }

    public Wait<WebDriver> quick() {
        return utils.quick();
    }

    public Wait<WebDriver> slow() {
        return utils.slow();
    }
}
