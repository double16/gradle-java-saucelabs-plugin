package com.github.double16;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.ScreenshotException;

public class FunctionalTestUtilsTest {
    private FunctionalTestUtils utils;
    private By byIdMissing = By.id("idDoesNotExist");
    @Rule
    public TemporaryFolder reportDir = new TemporaryFolder();
    @Rule
    public ErrorCollector collector = new ErrorCollector();
    
    @Before
    public void setup() {
        WebDriver driver = AMUILoginPageMock.createDriver();
        utils = new FunctionalTestUtils(driver);
    }

    @Test
    public void testQuick() {
        assertNotNull(utils.quick(AMUILoginPageMock.username));
    }

    @Test(expected = Exception.class)
    public void testQuickFail() {
        utils.quick(byIdMissing);
    }

    @Test
    public void testSlow() {
        assertNotNull(utils.slow(AMUILoginPageMock.username));
    }

    @Test
    public void at() {
    	collector.checkThat(utils.at(AMUILoginPage.class), notNullValue());
    }

    @Test
    public void at_alertHandled() {
    	final WebElement element = AMUILoginPageMock.mockDisplayed("input");
    	WebDriver driver = Mockito.mock(WebDriver.class);
    	
    	Alert alert = Mockito.mock(Alert.class);
    	TargetLocator alertLocator = Mockito.mock(TargetLocator.class);
    	Mockito.when(alertLocator.alert()).thenReturn(alert);

    	TargetLocator noAlertLocator = Mockito.mock(TargetLocator.class);
    	Mockito.when(alertLocator.alert()).thenThrow(new NoAlertPresentException());
    	
    	Mockito.when(driver.switchTo()).thenReturn(alertLocator).thenReturn(noAlertLocator);
    	Mockito.when(driver.findElement(AMUILoginPage.at))
    		.thenThrow(new UnhandledAlertException("alert"))
    		.thenReturn(element);
    	new FunctionalTestUtils(driver).at(AMUILoginPage.class);
    }
    
    /**
     * It's important that we throw a WebDriverException so tests can depend on that for missing
     * content, etc., instead of wider Exception that might indicate an error in the test.
     */
    @Test(expected = WebDriverException.class)
    public void atFail_WebDriverException() {
    	WebDriver driver = Mockito.mock(WebDriver.class);
        Mockito.when(driver.findElement(AMUILoginPage.at)).thenThrow(new NoSuchElementException("not found"));
        new FunctionalTestUtils(driver).at(AMUILoginPage.class);
    }

    /**
     * When something besides a missing element exception is thrown, we want the RuntimeException.
     */
    @Test(expected = RuntimeException.class)
    public void atFail_RuntimeException() {
    	WebDriver driver = Mockito.mock(WebDriver.class);
		Mockito.when(driver.findElement(AMUILoginPage.at)).thenThrow(new UnsupportedOperationException());
        new FunctionalTestUtils(driver).at(AMUILoginPage.class);
    }
    
    @Test
    public void reportWithNoSupport() throws IOException {
    	WebDriver driver = Mockito.mock(WebDriver.class);
    	new FunctionalTestUtils(driver).report(reportDir.getRoot(), "nosupport");
    }
    
    private String readContent(File file) throws IOException {
    	StringWriter str = new StringWriter();
    	FileReader reader = new FileReader(file);
    	IOUtils.copy(reader, str);
    	IOUtils.closeQuietly(str);
    	IOUtils.closeQuietly(reader);
    	return str.toString();
    }
    
    private byte[] readBytes(File file) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	FileInputStream input = new FileInputStream(file);
    	IOUtils.copy(input, out);
    	IOUtils.closeQuietly(input);
    	IOUtils.closeQuietly(out);
    	return out.toByteArray();
    }
    
    @Test
    public void reportWithHtml() throws IOException {
    	WebDriver driver = Mockito.mock(WebDriver.class);
    	WebElement html = AMUILoginPageMock.mockDisplayed("html");
    	Mockito.when(html.getAttribute("innerHTML")).thenReturn("test");
    	Mockito.when(driver.findElement(By.cssSelector("html"))).thenReturn(html);
    	
    	new FunctionalTestUtils(driver).report(reportDir.getRoot(), "htmlonly");
    	File htmlOutput = new File(reportDir.getRoot(), "htmlonly.html");
    	File imageOutput = new File(reportDir.getRoot(), "htmlonly.png");
    	collector.checkThat("HTML output exists", htmlOutput.exists(), is(true));
    	collector.checkThat("Screenshot output does not exist", imageOutput.exists(), is(false));
    	collector.checkThat("HTML output content", readContent(htmlOutput).replaceAll("\\s+", ""), is("<html>test</html>"));
    }
    
    public interface WebDriverWithScreenshot extends WebDriver, TakesScreenshot {};
    
    @Test
    public void reportWithScreenshotAndHtml() throws IOException {
    	final String printIconUrl = getClass().getClassLoader().getResource("com/github/double16/print.gif").toExternalForm();
    	File printIconSource = new File(printIconUrl.replaceFirst("file:", ""));
    	assertTrue("Source icon can't be found, aborting test", printIconSource.exists());
    	
    	WebDriverWithScreenshot driver = Mockito.mock(WebDriverWithScreenshot.class);
    	WebElement html = AMUILoginPageMock.mockDisplayed("html");
    	Mockito.when(html.getAttribute("innerHTML")).thenReturn("test");
    	Mockito.when(driver.findElement(By.cssSelector("html"))).thenReturn(html);
    	
    	Mockito.when(driver.getScreenshotAs(OutputType.FILE)).thenReturn(printIconSource);
    	
    	new FunctionalTestUtils(driver).report(reportDir.getRoot(), "htmlandimage");
    	File htmlOutput = new File(reportDir.getRoot(), "htmlandimage.html");
    	File imageOutput = new File(reportDir.getRoot(), "htmlandimage.png");
    	collector.checkThat("HTML output exists", htmlOutput.exists(), is(true));
    	collector.checkThat("Screenshot output exists", imageOutput.exists(), is(true));
    	collector.checkThat("HTML output content", readContent(htmlOutput).replaceAll("\\s+", ""), is("<html>test</html>"));
    	collector.checkThat("Screenshot content", Arrays.equals(readBytes(printIconSource), readBytes(imageOutput)), is(true));
    }

    @Test
    public void reportWithScreenshotFailure() throws IOException {
        WebDriverWithScreenshot driver = Mockito.mock(WebDriverWithScreenshot.class);
        WebElement html = AMUILoginPageMock.mockDisplayed("html");
        Mockito.when(html.getAttribute("innerHTML")).thenReturn("test");
        Mockito.when(driver.findElement(By.cssSelector("html"))).thenReturn(html);

        Mockito.when(driver.getScreenshotAs(OutputType.FILE)).thenThrow(new ScreenshotException(""));

        new FunctionalTestUtils(driver).report(reportDir.getRoot(), "htmlandimagefail");
        File htmlOutput = new File(reportDir.getRoot(), "htmlandimagefail.html");
        File imageOutput = new File(reportDir.getRoot(), "htmlandimagefail.png");
        collector.checkThat("HTML output exists", htmlOutput.exists(), is(true));
        collector.checkThat("Screenshot output does not exist", imageOutput.exists(), is(false));
        collector.checkThat("HTML output content", readContent(htmlOutput).replaceAll("\\s+", ""), is("<html>test</html>"));
    }

    @Test
    public void visibilityOfFirstElementLocatedBy_NoElements() {
        WebDriver driver = Mockito.mock(WebDriver.class);
        Mockito.when(driver.findElements(By.cssSelector("h1"))).thenReturn(Collections.<WebElement> emptyList());
        WebElement result = FunctionalTestUtils.visibilityOfFirstElementLocatedBy(By.cssSelector("h1")).apply(driver);
        collector.checkThat(result, nullValue());
    }

    @Test
    public void visibilityOfFirstElementLocatedBy_OneElement_NotVisible() {
        WebDriver driver = Mockito.mock(WebDriver.class);
        WebElement h1 = AMUILoginPageMock.mockHidden("h1");
        Mockito.when(driver.findElements(By.cssSelector("h1"))).thenReturn(Collections.<WebElement> singletonList(h1));
        WebElement result = FunctionalTestUtils.visibilityOfFirstElementLocatedBy(By.cssSelector("h1")).apply(driver);
        collector.checkThat(result, nullValue());
    }

    @Test
    public void visibilityOfFirstElementLocatedBy_OneElement_Visible() {
        WebDriver driver = Mockito.mock(WebDriver.class);
        WebElement h1 = AMUILoginPageMock.mockDisplayed("h1");
        Mockito.when(driver.findElements(By.cssSelector("h1"))).thenReturn(Collections.<WebElement> singletonList(h1));
        WebElement result = FunctionalTestUtils.visibilityOfFirstElementLocatedBy(By.cssSelector("h1")).apply(driver);
        collector.checkThat(result, sameInstance(h1));
    }

    @Test
    public void visibilityOfFirstElementLocatedBy_TwoElements_FirstVisible() {
        WebDriver driver = Mockito.mock(WebDriver.class);
        WebElement h1a = AMUILoginPageMock.mockDisplayed("h1");
        WebElement h1b = AMUILoginPageMock.mockHidden("h1");
        Mockito.when(driver.findElements(By.cssSelector("h1"))).thenReturn(Arrays.asList(new WebElement[] { h1a, h1b }));
        WebElement result = FunctionalTestUtils.visibilityOfFirstElementLocatedBy(By.cssSelector("h1")).apply(driver);
        collector.checkThat(result, sameInstance(h1a));
    }

    @Test
    public void visibilityOfFirstElementLocatedBy_TwoElements_SecondVisible() {
        WebDriver driver = Mockito.mock(WebDriver.class);
        WebElement h1a = AMUILoginPageMock.mockHidden("h1");
        WebElement h1b = AMUILoginPageMock.mockDisplayed("h1");
        Mockito.when(driver.findElements(By.cssSelector("h1"))).thenReturn(Arrays.asList(new WebElement[] { h1a, h1b }));
        WebElement result = FunctionalTestUtils.visibilityOfFirstElementLocatedBy(By.cssSelector("h1")).apply(driver);
        collector.checkThat(result, sameInstance(h1b));
    }
}
