package com.github.double16;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

public class FunctionalTestUtils {
    private final WebDriver driver;

    public FunctionalTestUtils(WebDriver driver) {
        this.driver = driver;
    }

    private Throwable next(Throwable e) {
    	if (e instanceof InvocationTargetException) {
    		Throwable target = ((InvocationTargetException) e).getTargetException();
    		if (target != null && target != e) {
    			return target;
    		}
    	}
    	Throwable cause = e.getCause();
    	if (cause == e) {
    		return null;
    	}
    	return cause;
    }
    
    /**
     * Returns an instance of the page. If the page constructor performs an 'at' check, the exception will be thrown here.
     * 
     * @param page the page class.
     * @return page instance.
     */
    public <T> T at(Class<T> page) {
    	try {
            return PageFactory.initElements(driver, page);
    	} catch (RuntimeException e) {
    		Throwable findWebDriverException = e;
    		while (findWebDriverException != null) {
    			if (findWebDriverException instanceof UnhandledAlertException) {
    				try {
    					driver.switchTo().alert().dismiss();
    				} catch (NoAlertPresentException  e2) {
    					// ignore, the alert might have already been dismissed
    				}
    				quick().until(ExpectedConditions.not(ExpectedConditions.alertIsPresent()));
    				return at(page);
    			}
    			if (findWebDriverException instanceof WebDriverException) {
    	    		throw new WebDriverException("Page element(s) not found: "+findWebDriverException.getMessage(), e);
    			}
    			findWebDriverException = next(findWebDriverException);
    		}
    		throw e;
    	}
    }

    public static ExpectedCondition<WebElement> visibilityOfFirstElementLocatedBy(final By locator) {
        return new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                List<WebElement> elements = driver.findElements(locator);
                for (WebElement element : elements) {
                    if (element.isDisplayed()) {
                        Dimension size = element.getSize();
                        if (size.getWidth() > 0 && size.getHeight() > 0) {
                            return element;
                        }
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "visibility of first element located by " + locator;
            }
        };
    }

    public Wait<WebDriver> quick() {
        return new FluentWait<WebDriver>(driver).withTimeout(3, TimeUnit.SECONDS).pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(WebDriverException.class);
    }

    public WebElement quick(By locator) {
        return quick().until(visibilityOfFirstElementLocatedBy(locator));
    }

    public Wait<WebDriver> slow() {
        return new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(5, TimeUnit.SECONDS)
                .ignoring(WebDriverException.class);
    }

    public WebElement slow(By locator) {
        return slow().until(visibilityOfFirstElementLocatedBy(locator));
    }

    /**
     * Returns a collection of at checkers for the page. If the page has no
     * "public static By at" or "public static By[]" defined, returns an empty collection.
     * @return collection, never null but may be empty.
     */
    @SuppressWarnings("PMD")
    public Collection<By> getAtCheckers(Class<?> pageClass) {
        Object locator = null;
        try {
            Field urlField = pageClass.getDeclaredField("at");
            if (urlField != null && Modifier.isStatic(urlField.getModifiers())) {
                locator = urlField.get(null);
            }
        } catch (NoSuchFieldException e) {
            // handled below
        } catch (IllegalAccessException e) {
            // handled below
        } catch (ClassCastException e) {
            // handled below
        }
        if (locator == null) {
        	return Collections.emptyList();
        }
    	Collection<By> locators;
    	if (locator instanceof By) {
    		locators = Collections.singleton((By) locator);
    	} else if (locator.getClass().isArray()) {
    		locators = new ArrayList<By>(Array.getLength(locator));
    		for(int i = 0; i < Array.getLength(locator); i++) {
    			locators.add((By) Array.get(locator, i));
    		}
    	} else if (locator instanceof Collection) {
    		locators = (Collection<By>) locator;
    	} else {
    		throw new IllegalArgumentException("Expecting static 'at' to be a By or Collection of By, found "+locator.getClass());
    	}
    	return locators;
    }
    
    /**
     * Get the relative URL for the page. 
     * @return the url or null if not defined.
     */
    @SuppressWarnings("PMD")
    public String getUrl(Class<?> pageClass) {
        String relative = null;
        try {
            Field urlField = pageClass.getDeclaredField("url");
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
    	return relative;
    }
    
    /**
     * Creates a report including a screenshot and HTML export of the current page.
     * @param reportDir the directory into which to create the report.
     * @param baseFileName the base file name, without a path and an extension (.png and .html will be added).
     * @return list of files created
     */
    public List<File> report(File reportDir, String baseFileName) throws IOException {
    	List<File> ret = new ArrayList<File>();
        if (driver instanceof TakesScreenshot) {
            try {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                File screenshotFile = new File(reportDir, baseFileName + ".png");
                FileUtils.copyFile(screenshot, screenshotFile);
                ret.add(screenshotFile);
            } catch (WebDriverException e) {
                System.err.println("Screen shot failed: " + e.toString());
            }
        }
        
        try {
            WebElement htmlElement = driver.findElement(By.cssSelector("html"));
            if (htmlElement != null) {
                File htmlFile = new File(reportDir, baseFileName + ".html");
                FileWriter writer = new FileWriter(htmlFile);
                writer.append("<html>\n");
                writer.append(htmlElement.getAttribute("innerHTML"));
                writer.append("</html>\n");
                writer.close();
                ret.add(htmlFile);
            }
        } catch (WebDriverException e) {
            System.err.println("HTML capture failed: " + e.toString());
        }
        
        return ret;
    }

    public <T> T navigateMenu(String path, Class<T> page) throws WebDriverException {
        String[] pathEles = path.split(">");
        Actions builder = new Actions(driver);
        for (int i = 0; i < pathEles.length; i++) {
            String pe = pathEles[i].trim();
            WebElement element = quick(By.xpath("//span[contains(text(),'" + pe + "')]"));
            builder.moveToElement(element).build().perform();
        }
        quick(By.xpath("//span[contains(text(),'" + pathEles[pathEles.length - 1].trim() + "')]")).click();
        return at(page);
    }
}
