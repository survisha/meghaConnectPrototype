package com.meghaconnect.automation.utils;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;
import java.util.List;

/**
 * WebElement Utility - Provides common WebElement operations and waits
 * Includes comfortable wrappers for element interaction
 */
public class WebElementUtil {
    private static final Logger logger = LogManager.getLogger(WebElementUtil.class);
    private static final String HIGHLIGHT_STYLE = "border: 3px solid red; background: yellow; box-shadow: 0 0 10px red;";

    /**
     * Wait for element to be visible
     * @param locator By locator
     * @return WebElement
     */
    public static WebElement waitForElementVisible(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                    Duration.ofSeconds(ConfigManager.getExplicitWait()));
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            logger.debug("✓ Element visible: " + locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("✗ Element not visible within timeout: " + locator);
            throw new RuntimeException("Element not visible: " + locator, e);
        }
    }

    public static WebElement waitForVisibleWithHighlight(By locator) {
        WebElement element = waitForElementVisible(locator);
        prepareElementForHighlight(element);
        highlightElement(element);
        sleepForHighlight();
        captureStepScreenshot("visible_" + locator);
        removeHighlight(element);
        return element;
    }

    public static void highlightElement(WebElement element) {
        if (!ConfigManager.isHighlightEnabled() || element == null) {
            return;
        }
        try {
            JavascriptExecutor executor = (JavascriptExecutor) DriverManager.getDriver();
            executor.executeScript("arguments[0].setAttribute('data-original-style', arguments[0].getAttribute('style') || '');", element);
            executor.executeScript("arguments[0].setAttribute('style', (arguments[0].getAttribute('style') || '') + arguments[1]);", element, HIGHLIGHT_STYLE);
        } catch (Exception e) {
            logger.debug("Unable to highlight element: " + e.getClass().getSimpleName());
        }
    }

    public static void removeHighlight(WebElement element) {
        if (!ConfigManager.isHighlightEnabled() || element == null) {
            return;
        }
        try {
            JavascriptExecutor executor = (JavascriptExecutor) DriverManager.getDriver();
            executor.executeScript("arguments[0].setAttribute('style', arguments[0].getAttribute('data-original-style') || '');", element);
            executor.executeScript("arguments[0].removeAttribute('data-original-style');", element);
        } catch (Exception e) {
            logger.debug("Unable to remove element highlight: " + e.getClass().getSimpleName());
        }
    }

    public static void clickWithHighlight(By locator) {
        WebElement element = waitForElementClickable(locator);
        prepareElementForHighlight(element);
        highlightElement(element);
        sleepForHighlight();
        captureStepScreenshot("before_click_" + locator);
        try {
            element.click();
        } catch (Exception e) {
            logger.warn("Normal click failed, using JavaScript click fallback for: " + locator);
            executeScript("arguments[0].click();", element);
        }
        captureStepScreenshot("after_click_" + locator);
        removeHighlight(element);
        logger.info("Clicked highlighted element: " + locator);
    }

    public static void typeWithHighlight(By locator, String value) {
        WebElement element = waitForElementVisible(locator);
        prepareElementForHighlight(element);
        highlightElement(element);
        sleepForHighlight();
        captureStepScreenshot("before_type_" + locator);
        element.clear();
        element.sendKeys(value == null ? "" : value);
        captureStepScreenshot("after_type_" + locator);
        removeHighlight(element);
        logger.info("Typed into highlighted element: " + locator);
    }

    public static String getTextWithHighlight(By locator) {
        WebElement element = waitForElementVisible(locator);
        prepareElementForHighlight(element);
        highlightElement(element);
        sleepForHighlight();
        captureStepScreenshot("before_get_text_" + locator);
        String text = element.getText();
        captureStepScreenshot("after_get_text_" + locator);
        removeHighlight(element);
        logger.info("Read text from highlighted element: " + locator);
        return text;
    }

    public static void captureStepScreenshot(String stepName) {
        if (ConfigManager.isScreenshotEachStep()) {
            ScreenshotUtil.captureScreenshot(stepName);
        }
    }

    /**
     * Wait for element to be clickable
     * @param locator By locator
     * @return WebElement
     */
    public static WebElement waitForElementClickable(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                    Duration.ofSeconds(ConfigManager.getExplicitWait()));
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            logger.debug("✓ Element clickable: " + locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("✗ Element not clickable within timeout: " + locator);
            throw new RuntimeException("Element not clickable: " + locator, e);
        }
    }

    /**
     * Wait for element to be present in DOM
     * @param locator By locator
     * @return WebElement
     */
    public static WebElement waitForElementPresent(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                    Duration.ofSeconds(ConfigManager.getExplicitWait()));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            logger.debug("✓ Element present: " + locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("✗ Element not present within timeout: " + locator);
            throw new RuntimeException("Element not present: " + locator, e);
        }
    }

    /**
     * Click on element with wait
     * @param locator By locator
     */
    public static void click(By locator) {
        try {
            WebElement element = waitForElementClickable(locator);
            element.click();
            logger.info("✓ Clicked on element: " + locator);
        } catch (Exception e) {
            logger.error("✗ Failed to click on element: " + locator, e);
            throw new RuntimeException("Click failed: " + locator, e);
        }
    }

    /**
     * Type text into element
     * @param locator By locator
     * @param text Text to type
     */
    public static void type(By locator, String text) {
        try {
            WebElement element = waitForElementVisible(locator);
            element.clear();
            element.sendKeys(text);
            logger.info("✓ Typed text into element: " + locator);
        } catch (Exception e) {
            logger.error("✗ Failed to type text into element: " + locator, e);
            throw new RuntimeException("Type failed: " + locator, e);
        }
    }

    /**
     * Get text from element
     * @param locator By locator
     * @return Element text
     */
    public static String getText(By locator) {
        try {
            WebElement element = waitForElementVisible(locator);
            String text = element.getText();
            logger.debug("✓ Got text from element: " + locator + " = " + text);
            return text;
        } catch (Exception e) {
            logger.error("✗ Failed to get text from element: " + locator, e);
            throw new RuntimeException("GetText failed: " + locator, e);
        }
    }

    /**
     * Get attribute value from element
     * @param locator By locator
     * @param attributeName Attribute name
     * @return Attribute value
     */
    public static String getAttribute(By locator, String attributeName) {
        try {
            WebElement element = waitForElementVisible(locator);
            String value = element.getAttribute(attributeName);
            logger.debug("✓ Got attribute '" + attributeName + "' from element: " + locator + " = " + value);
            return value;
        } catch (Exception e) {
            logger.error("✗ Failed to get attribute from element: " + locator, e);
            throw new RuntimeException("GetAttribute failed: " + locator, e);
        }
    }

    /**
     * Check if element is displayed
     * @param locator By locator
     * @return true if displayed, false otherwise
     */
    public static boolean isElementDisplayed(By locator) {
        try {
            WebElement element = waitForElementPresent(locator);
            return element.isDisplayed();
        } catch (Exception e) {
            logger.debug("⚠ Element not displayed: " + locator);
            return false;
        }
    }

    /**
     * Check if element is enabled
     * @param locator By locator
     * @return true if enabled, false otherwise
     */
    public static boolean isElementEnabled(By locator) {
        try {
            WebElement element = waitForElementPresent(locator);
            return element.isEnabled();
        } catch (Exception e) {
            logger.debug("⚠ Element not enabled: " + locator);
            return false;
        }
    }

    /**
     * Select dropdown option by visible text
     * @param locator By locator of dropdown
     * @param optionText Text of option to select
     */
    public static void selectDropdownByVisibleText(By locator, String optionText) {
        try {
            WebElement element = waitForElementVisible(locator);
            Select select = new Select(element);
            select.selectByVisibleText(optionText);
            logger.info("✓ Selected dropdown option: " + optionText);
        } catch (Exception e) {
            logger.error("✗ Failed to select dropdown option: " + optionText, e);
            throw new RuntimeException("Dropdown select failed: " + optionText, e);
        }
    }

    /**
     * Select dropdown option by value
     * @param locator By locator of dropdown
     * @param value Value of option to select
     */
    public static void selectDropdownByValue(By locator, String value) {
        try {
            WebElement element = waitForElementVisible(locator);
            Select select = new Select(element);
            select.selectByValue(value);
            logger.info("✓ Selected dropdown option by value: " + value);
        } catch (Exception e) {
            logger.error("✗ Failed to select dropdown option by value: " + value, e);
            throw new RuntimeException("Dropdown select by value failed: " + value, e);
        }
    }

    /**
     * Get all dropdown options text
     * @param locator By locator of dropdown
     * @return List of option texts
     */
    public static List<String> getDropdownOptions(By locator) {
        try {
            WebElement element = waitForElementVisible(locator);
            Select select = new Select(element);
            List<WebElement> options = select.getOptions();
            List<String> optionTexts = new java.util.ArrayList<>();
            for (WebElement option : options) {
                optionTexts.add(option.getText());
            }
            logger.debug("✓ Got dropdown options: " + optionTexts.size());
            return optionTexts;
        } catch (Exception e) {
            logger.error("✗ Failed to get dropdown options: " + locator, e);
            throw new RuntimeException("Get dropdown options failed: " + locator, e);
        }
    }

    /**
     * Wait for text to appear in element
     * @param locator By locator
     * @param text Text to wait for
     */
    public static void waitForTextInElement(By locator, String text) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                    Duration.ofSeconds(ConfigManager.getExplicitWait()));
            wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
            logger.debug("✓ Text found in element: " + text);
        } catch (TimeoutException e) {
            logger.error("✗ Text not found in element within timeout: " + text);
            throw new RuntimeException("Text not found: " + text, e);
        }
    }

    /**
     * Wait for element to disappear
     * @param locator By locator
     */
    public static void waitForElementInvisible(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                    Duration.ofSeconds(ConfigManager.getExplicitWait()));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
            logger.debug("✓ Element became invisible: " + locator);
        } catch (TimeoutException e) {
            logger.error("✗ Element did not become invisible within timeout: " + locator);
            throw new RuntimeException("Element not invisible: " + locator, e);
        }
    }

    /**
     * Move to element (hover)
     * @param locator By locator
     */
    public static void moveToElement(By locator) {
        try {
            WebElement element = waitForElementVisible(locator);
            org.openqa.selenium.interactions.Actions actions = 
                    new org.openqa.selenium.interactions.Actions(DriverManager.getDriver());
            actions.moveToElement(element).perform();
            logger.info("✓ Moved to element (hover): " + locator);
        } catch (Exception e) {
            logger.error("✗ Failed to move to element: " + locator, e);
            throw new RuntimeException("Move to element failed: " + locator, e);
        }
    }

    /**
     * Execute JavaScript
     * @param script JavaScript code to execute
     * @param args Arguments for the script
     * @return Result of script execution
     */
    public static Object executeScript(String script, Object... args) {
        try {
            JavascriptExecutor executor = (JavascriptExecutor) DriverManager.getDriver();
            Object result = executor.executeScript(script, args);
            logger.debug("✓ JavaScript executed successfully");
            return result;
        } catch (Exception e) {
            logger.error("✗ Failed to execute JavaScript", e);
            throw new RuntimeException("JavaScript execution failed: " + e.getMessage(), e);
        }
    }

    private static void prepareElementForHighlight(WebElement element) {
        executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", element);
    }

    private static void sleepForHighlight() {
        if (!ConfigManager.isHighlightEnabled()) {
            return;
        }
        try {
            Thread.sleep(ConfigManager.getHighlightDurationMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wait for page to be ready (document.readyState = complete)
     */
    public static void waitForPageLoad() {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                    Duration.ofSeconds(ConfigManager.getPageLoadTimeout()));
            wait.until(driver -> executeScript("return document.readyState").equals("complete"));
            logger.debug("✓ Page loaded completely");
        } catch (Exception e) {
            logger.warn("⚠ Page load wait timeout");
        }
    }
}
