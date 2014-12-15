package com.github.double16;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;

public abstract class AbstractFunctionalTest {
    protected ThreadLocal<NumberFormat> REPORT_OUTPUT_FORMAT = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            return new DecimalFormat("000");
        }
    };

    @Parameter(0)
    public WebDriverFactory driverFactory;
    protected WebDriver driver;
    protected String baseUrl;
    protected File reportDir;
    protected int reportOutputNum = 1;

    @Rule
    public TestName testName = new TestName();

    private static String createBrowserSpecSystemPropertyName(int num) {
        if (num < 1) {
            if (System.getProperty("geb.saucelabs.browser") != null) {
                return "geb.saucelabs.browser";
            }
            return "saucelabs.browser";
        }
        return "saucelabs.browser." + num;
    }

    private static void fixupPlatform(Map caps) {
        String platform = (String) caps.get(CapabilityType.PLATFORM);
        if (platform != null) {
            for (Platform p : Platform.values()) {
                for (String osName : p.getPartOfOsName()) {
                    if (osName.equalsIgnoreCase(platform)) {
                        caps.put(CapabilityType.PLATFORM, p.name());
                        return;
                    }
                }
            }
        }
    }

    private static File locateDriver(File file, URL path) throws IOException {
        File driverInPath = findDriverInPath(file);
        if (driverInPath != null) {
            System.out.println("Using " + driverInPath);
            return driverInPath;
        }
        downloadDriver(file, path);
        System.out.println("Using " + file);
        return file;
    }

    private static File findDriverInPath(File file) throws IOException {
        File driver = new File(new File(System.getProperty("user.home")), file.getName());
        if (driver.canExecute()) {
            return driver;
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        String[] parts = path.split(File.pathSeparator);
        for (String part : parts) {
            driver = new File(new File(part), file.getName());
            if (driver.canExecute()) {
                return driver;
            }
        }
        return null;
    }

    private static void downloadDriver(File file, URL path) throws IOException {
        if (!file.exists()) {
            System.out.println("Downloading " + path);
            // download driver
            File driver = File.createTempFile("driver", ".zip");
            URLConnection connection = path.openConnection();
            String redirect = connection.getHeaderField("Location");
            if (redirect != null) {
                URL redirectUrl = new URL(path, redirect);
                System.out.println("Redirect to " + redirectUrl);
                downloadDriver(file, redirectUrl);
                return;
            }
            InputStream is = null;
            OutputStream os = null;
            try {
                is = connection.getInputStream();
                os = new FileOutputStream(driver);
                IOUtils.copy(is, os);
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }

            // unzip
            System.out.println("Extracting " + path);
            ZipFile zip = new ZipFile(driver);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().equals(file.getName())) {
                    continue;
                }
                System.out.println("Found " + file);
                file.getParentFile().mkdirs();
                InputStream zis = null;
                OutputStream zos = null;
                try {
                    zis = zip.getInputStream(entry);
                    zos = new FileOutputStream(file);
                    IOUtils.copy(zis, zos);
                } finally {
                    if (zis != null) {
                        zis.close();
                    }
                    if (zos != null) {
                        zos.close();
                    }
                }
                file.setExecutable(true);
            }

            driver.delete();

            if (!file.exists()) {
                throw new IOException("Error downloading " + file + " from " + path);
            }
        }
    }

    @Parameterized.Parameters
    public static Collection<WebDriverFactory[]> drivers() throws IOException {
        boolean ci = System.getenv("JENKINS_URL") != null;

        List<WebDriverFactory[]> drivers = new LinkedList<WebDriverFactory[]>();

        int driverSpecNum = 0;
        String spec;
        while ((spec = System.getProperty(createBrowserSpecSystemPropertyName(driverSpecNum++))) != null) {
            final Properties browserCaps = new Properties();
            browserCaps.put("name", System.getProperty("saucelabs.job-name", ""));
            browserCaps.put("build", System.getProperty("saucelabs.build", ""));
            System.out.println("browserSpec = "+spec);
            browserCaps.load(new StringReader(spec.replaceAll(",", "\n")));
            fixupPlatform(browserCaps);
            final DesiredCapabilities capabilities = new DesiredCapabilities((Map) browserCaps);
            WebDriverFactory factory = new WebDriverFactory() {
                @Override
                public WebDriver createWebDriver() throws IOException {
                  WebDriver driver;
                    if (browserCaps.containsKey("url")) {
                      driver = new RemoteWebDriver(new URL((String) browserCaps.get("url")), capabilities);
                        System.setProperty("base.hostname", (String) browserCaps.get("baseHostname"));
                    } else {
                      driver = new RemoteWebDriver(new URL("http://" + System.getenv("SAUCE_LABS_USER") + ":"
                          + System.getenv("SAUCE_LABS_ACCESS_PASSWORD") + "@ondemand.saucelabs.com:80/wd/hub"), capabilities);
                      String sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
                      System.out.println("SauceOnDemandSessionID=" + sessionId);
                    }
                    return driver;
                }
            };
            drivers.add(new WebDriverFactory[] { factory });
        }

        if (drivers.isEmpty()) {
            try {
                if (!ci) {
                    drivers.add(new WebDriverFactory[] { createChromeDriverFactory() });
                }
            } catch (Exception e) {
                System.err.println("Unable to locate Chrome driver: " + e.getMessage());
            }
            try {
                if (!ci) {
                    drivers.add(new WebDriverFactory[] { createFirefoxDriverFactory() });
                }
            } catch (Exception e) {
                System.err.println("Unable to locate Firefox driver: " + e.getMessage());
            }
            try {
                drivers.add(new WebDriverFactory[] { createPhantomJSDriverFactory() });
            } catch (Exception e) {
                System.err.println("Unable to locate PhantomJS driver: " + e.getMessage());
            }
        }

        return drivers;
    }

    protected static WebDriverFactory createChromeDriverFactory() throws IOException {
        File chromeDriver;
        if (SystemUtils.IS_OS_WINDOWS) {
            chromeDriver = locateDriver(new File("target/webdrivers/chrome/chromedriver.exe"), new URL(
                    "http://chromedriver.storage.googleapis.com/2.10/chromedriver_win32.zip"));
        } else if (SystemUtils.IS_OS_LINUX) {
            if (SystemUtils.OS_ARCH.contains("64")) {
                chromeDriver = locateDriver(new File("target/webdrivers/chrome/chromedriver"), new URL(
                        "http://chromedriver.storage.googleapis.com/2.10/chromedriver_linux64.zip"));
            } else {
                chromeDriver = locateDriver(new File("target/webdrivers/chrome/chromedriver"), new URL(
                        "http://chromedriver.storage.googleapis.com/2.10/chromedriver_linux32.zip"));
            }
        } else if (SystemUtils.IS_OS_MAC) {
            chromeDriver = locateDriver(new File("target/webdrivers/chrome/chromedriver"), new URL(
                    "http://chromedriver.storage.googleapis.com/2.10/chromedriver_mac32.zip"));
        } else {
            throw new IOException("No Chrome driver for this OS");
        }
        System.setProperty("webdriver.chrome.driver", chromeDriver.getAbsolutePath());
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver() {
                return new ChromeDriver();
            }
        };
    }

    protected static WebDriverFactory createFirefoxDriverFactory() throws IOException {
        // 2014-12-05 selenium 2.40.0 isn't compatible with FF33
        throw new IOException("Selenium 2.40.0 is not compatible with FF33");
        // return new FirefoxDriver();
    }

    protected static WebDriverFactory createPhantomJSDriverFactory() throws IOException {
        File phantomjsDriver;
        if (SystemUtils.IS_OS_WINDOWS) {
            phantomjsDriver = locateDriver(new File("target/webdrivers/phantomjs/phantomjs.exe"), new URL(
                    "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-windows.zip"));
        } else if (SystemUtils.IS_OS_LINUX) {
            if (SystemUtils.OS_ARCH.contains("64")) {
                phantomjsDriver = locateDriver(new File("target/webdrivers/phantomjs/phantomjs"), new URL(
                        "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-linux-x86_64.tar.bz2"));
            } else {
                phantomjsDriver = locateDriver(new File("target/webdrivers/phantomjs/phantomjs"), new URL(
                        "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-linux-i686.tar.bz2"));
            }
        } else if (SystemUtils.IS_OS_MAC) {
            phantomjsDriver = locateDriver(new File("target/webdrivers/phantomjs/phantomjs"), new URL(
                    "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-macosx.zip"));
        } else {
            throw new IOException("No phantomjs driver for this OS");
        }
        System.setProperty("phantomjs.binary.path", phantomjsDriver.getAbsolutePath());
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver() {
                PhantomJSDriver driver = new PhantomJSDriver();
                driver.manage().window().setSize(new Dimension(1024, 768));
                return driver;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        this.driver = driverFactory.createWebDriver();
        reportDir = new File(new File(new File(System.getProperty("functionalTests.resultsDir", "build/reports/tests/"
                + driver.getClass().getSimpleName())), getClass().getSimpleName()), testName.getMethodName());
        reportDir.mkdirs();
        System.out.println("Reports in " + reportDir.getAbsolutePath());
    }

    @After
    public void tearDown() {
        report("end");
        if (driver != null) {
          driver.quit();
        }
    }

    protected String getBaseUrl() {
        String baseHostname = System.getProperty("base.hostname");
        if (baseHostname != null && baseHostname.trim().length() > 0) {
            return baseUrl.replaceFirst("localhost", baseHostname);
        }
        return baseUrl;
    }

    /**
     * Go to the given page. The class is expected to have a public static final String field named 'url' containing the relative
     * URL. The value will be appended to {@link #getBaseUrl()}. If the page constructor performs an 'at' check, the exception will be
     * thrown here.
     * 
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
        driver.get(getBaseUrl() + relative);
        return at(page);
    }

    /**
     * Returns an instance of the page. If the page constructor performs an 'at' check, the exception will be thrown here.
     * 
     * @param page the page class.
     * @return page instance.
     */
    public <T> T at(Class<T> page) {
        return PageFactory.initElements(driver, page);
    }

    /**
     * Go to the base URL.
     */
    public void home() {
        driver.get(getBaseUrl());
    }

    public void report(String name) {
        try {
            if (driver instanceof TakesScreenshot) {
                name = name.replaceAll("[^A-Za-z0-9-]+", "_");
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(screenshot, new File(reportDir, REPORT_OUTPUT_FORMAT.get().format(reportOutputNum++) + "-"
                        + name + ".png"));
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
