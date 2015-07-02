package com.github.double16;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.internal.BuildInfo;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class WebDriverService {
	private static final String WEBDRIVER_CHROME_DRIVER = "webdriver.chrome.driver";
	private static final String WEBDRIVER_INTERNETEXPLORER_DRIVER = "webdriver.ie.driver";
	private static final String PHANTOMJS_BINARY_PATH = "phantomjs.binary.path";

	@SuppressWarnings("PMD")
    private void locateDriver(String systemProperty, File file, URL path) throws IOException {
        if (StringUtils.isNotBlank(System.getProperty(systemProperty)) && new File(System.getProperty(systemProperty)).canExecute()) {
            System.out.println("Using (from system property " + systemProperty + ") " + System.getProperty(systemProperty));
    		return;
    	}
    	String env = System.getenv(systemProperty);
        if (StringUtils.isNotBlank(env) && new File(env).canExecute()) {
            System.out.println("Using (from env " + env + ") " + env);
    		System.setProperty(systemProperty, env);
    		return;
    	}
    	
        File driverInPath = findDriverInPath(file);
        if (driverInPath != null) {
            System.out.println("Using (from user home or path) " + driverInPath);
            System.setProperty(systemProperty, driverInPath.getAbsolutePath());
            return;
        }
        downloadDriver(file, path);
        System.out.println("Using (downloaded) " + file);
        System.setProperty(systemProperty, file.getAbsolutePath());
    }

    private File findDriverInPath(File file) throws IOException {
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

    @SuppressWarnings("PMD")
    private void downloadDriver(File file, URL path) throws IOException {
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
            try {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.getName().equals(file.getName()) && !entry.getName().endsWith("/" + file.getName())) {
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
            } finally {
                zip.close();
            }

            driver.delete();

            if (!file.exists()) {
                throw new IOException("Error downloading " + file + " from " + path);
            }
        }
    }

    public WebDriverFactory createChromeDriverFactory() throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            locateDriver(WEBDRIVER_CHROME_DRIVER, new File("target/webdrivers/chrome/chromedriver.exe"), new URL(
                    "http://chromedriver.storage.googleapis.com/2.15/chromedriver_win32.zip"));
        } else if (SystemUtils.IS_OS_LINUX) {
            if (SystemUtils.OS_ARCH.contains("64")) {
                locateDriver(WEBDRIVER_CHROME_DRIVER, new File("target/webdrivers/chrome/chromedriver"), new URL(
                        "http://chromedriver.storage.googleapis.com/2.15/chromedriver_linux64.zip"));
            } else {
                locateDriver(WEBDRIVER_CHROME_DRIVER, new File("target/webdrivers/chrome/chromedriver"), new URL(
                        "http://chromedriver.storage.googleapis.com/2.15/chromedriver_linux32.zip"));
            }
        } else if (SystemUtils.IS_OS_MAC) {
            locateDriver(WEBDRIVER_CHROME_DRIVER, new File("target/webdrivers/chrome/chromedriver"), new URL(
                    "http://chromedriver.storage.googleapis.com/2.15/chromedriver_mac32.zip"));
        } else {
            throw new IOException("No Chrome driver for this OS");
        }
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) {
                if (StringUtils.isNotBlank(System.getProperty("functionalTests.proxy"))) {
                    ChromeOptions opts = new ChromeOptions();
                    opts.addArguments("--proxy-server=" + System.getProperty("functionalTests.proxy"));
                    return new ChromeDriver(opts);
                } else {
                    DesiredCapabilities caps = DesiredCapabilities.chrome();
                    caps.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
                    return new ChromeDriver(caps);
                }
            }
            @Override
            public String getIdentifier() {
                return "chrome";
            }

            @Override
            public String toString() {
                return getIdentifier();
            }
        };
    }
    
    public WebDriverFactory createInternetExplorerFactory() throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            locateDriver(WEBDRIVER_INTERNETEXPLORER_DRIVER, new File("target/webdrivers/internetexplorer/IEDriverServer.exe"), new URL(
"http://selenium-release.storage.googleapis.com/2.46/IEDriverServer_Win32_2.46.0.zip"));
        } else {
            throw new IOException("No Internet Explorer driver for this OS");
        }
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) {
                DesiredCapabilities caps = DesiredCapabilities.internetExplorer();
                caps.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
                return new InternetExplorerDriver();
            }
            @Override
            public String getIdentifier() {
                return "IE";
            }

            @Override
            public String toString() {
                return getIdentifier();
            }
        };
    }

    public WebDriverFactory createFirefoxDriverFactory() throws IOException {
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) {
                if (StringUtils.isNotBlank(System.getProperty("functionalTests.proxy"))) {
                    String[] proxy = System.getProperty("functionalTests.proxy").split(":");
                    FirefoxProfile profile = new FirefoxProfile();
                    profile.setAcceptUntrustedCertificates(true);
                    profile.setPreference("network.proxy.type", 1);
                    profile.setPreference("network.proxy.http", proxy[0]);
                    profile.setPreference("network.proxy.http_port", Integer.parseInt(proxy[1]));
                    profile.setPreference("network.proxy.ssl", proxy[0]);
                    profile.setPreference("network.proxy.ssl_port", Integer.parseInt(proxy[1]));
                    return new FirefoxDriver(profile);
                } else {
                    DesiredCapabilities caps = new DesiredCapabilities();
                    caps.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
                    caps.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
                    return new FirefoxDriver(caps);
                }
            }
            @Override
            public String getIdentifier() {
                return "FireFox";
            }

            @Override
            public String toString() {
                return getIdentifier();
            }
        };
    }

    public WebDriverFactory createPhantomJSDriverFactory() throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            locateDriver(PHANTOMJS_BINARY_PATH, new File("target/webdrivers/phantomjs/phantomjs.exe"), new URL(
                    "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-windows.zip"));
        } else if (SystemUtils.IS_OS_LINUX) {
            if (SystemUtils.OS_ARCH.contains("64")) {
                locateDriver(PHANTOMJS_BINARY_PATH, new File("target/webdrivers/phantomjs/phantomjs"), new URL(
                        "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-linux-x86_64.tar.bz2"));
            } else {
                locateDriver(PHANTOMJS_BINARY_PATH, new File("target/webdrivers/phantomjs/phantomjs"), new URL(
                        "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-linux-i686.tar.bz2"));
            }
        } else if (SystemUtils.IS_OS_MAC) {
            locateDriver(PHANTOMJS_BINARY_PATH, new File("target/webdrivers/phantomjs/phantomjs"), new URL(
                    "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-macosx.zip"));
        } else {
            throw new IOException("No phantomjs driver for this OS");
        }
        return new WebDriverFactory() {
            @Override
            public WebDriver createWebDriver(String testName) {
                DesiredCapabilities caps = new DesiredCapabilities();
                caps.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
                PhantomJSDriver driver = new PhantomJSDriver(caps);
                driver.manage().window().setSize(new Dimension(1024, 768));
                return driver;
            }
            @Override
            public String getIdentifier() {
                return "phantomjs";
            }

            @Override
            public String toString() {
                return getIdentifier();
            }
        };
    }

}
