package com.meghaconnect.automation.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.concurrent.TimeUnit;

/**
 * WebDriver Manager - Handles WebDriver initialization and management
 * Supports multiple browsers: Chrome, Firefox, Edge
 * Implements singleton pattern for thread-safe driver management
 */
public class DriverManager {
    private static final Logger logger = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    /**
     * Initialize WebDriver based on browser configuration
     * @return WebDriver instance
     */
    public static WebDriver initializeDriver() {
        String browser = ConfigManager.getBrowser();
        logger.info("═══════════════════════════════════════════════════");
        logger.info("🔧 Initializing WebDriver: " + browser.toUpperCase());
        logger.info("═══════════════════════════════════════════════════");

        WebDriver driverInstance = null;

        try {
            switch (browser.toLowerCase()) {
                case "chrome":
                    driverInstance = initializeChromeDriver();
                    break;
                case "firefox":
                    driverInstance = initializeFirefoxDriver();
                    break;
                case "edge":
                    driverInstance = initializeEdgeDriver();
                    break;
                default:
                    logger.error("✗ Browser not supported: " + browser);
                    throw new IllegalArgumentException("Browser not supported: " + browser);
            }

            driver.set(driverInstance);

            // Set implicit and explicit waits
            driverInstance.manage().timeouts()
                    .implicitlyWait(ConfigManager.getImplicitWait(), TimeUnit.SECONDS);
            driverInstance.manage().timeouts()
                    .pageLoadTimeout(ConfigManager.getPageLoadTimeout(), TimeUnit.SECONDS);

            // Maximize window if configured
            if (ConfigManager.isBrowserMaximize()) {
                driverInstance.manage().window().maximize();
                logger.info("✓ Browser window maximized");
            }

            logger.info("✓ WebDriver initialized successfully");
            logger.info("  Base URL: " + ConfigManager.getBaseUrl());
            logger.info("  Headless: " + ConfigManager.isHeadless());

            return driverInstance;

        } catch (Exception e) {
            logger.error("✗ Failed to initialize WebDriver", e);
            throw new RuntimeException("WebDriver initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize Chrome WebDriver
     * @return ChromeDriver instance
     */
    private static WebDriver initializeChromeDriver() {
        logger.info("  Downloading ChromeDriver...");
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        if (ConfigManager.isHeadless()) {
            options.addArguments("--headless=new");
            logger.info("  ✓ Headless mode enabled");
        }

        if (ConfigManager.isIncognito()) {
            options.addArguments("--incognito");
            logger.info("  ✓ Incognito mode enabled");
        }

        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.setAcceptInsecureCerts(true);

        if (ConfigManager.isHeadless()) {
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
        }

        return new ChromeDriver(options);
    }

    /**
     * Initialize Firefox WebDriver
     * @return FirefoxDriver instance
     */
    private static WebDriver initializeFirefoxDriver() {
        logger.info("  Downloading FirefoxDriver...");
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        if (ConfigManager.isHeadless()) {
            options.addArguments("--headless");
            logger.info("  ✓ Headless mode enabled");
        }

        options.setAcceptInsecureCerts(true);

        return new FirefoxDriver(options);
    }

    /**
     * Initialize Edge WebDriver
     * @return EdgeDriver instance
     */
    private static WebDriver initializeEdgeDriver() {
        logger.info("  Downloading EdgeDriver...");
        WebDriverManager.edgedriver().setup();

        EdgeOptions options = new EdgeOptions();

        if (ConfigManager.isHeadless()) {
            options.addArguments("--headless");
            logger.info("  ✓ Headless mode enabled");
        }

        options.setAcceptInsecureCerts(true);
        options.addArguments("--start-maximized");

        return new EdgeDriver(options);
    }

    /**
     * Get current WebDriver instance
     * @return WebDriver instance
     */
    public static WebDriver getDriver() {
        if (driver.get() == null) {
            logger.warn("⚠ WebDriver is null, initializing...");
            initializeDriver();
        }
        return driver.get();
    }

    /**
     * Check if WebDriver is initialized
     * @return true if driver is initialized, false otherwise
     */
    public static boolean isDriverInitialized() {
        return driver.get() != null;
    }

    /**
     * Quit WebDriver and cleanup
     */
    public static void quitDriver() {
        try {
            WebDriver driverInstance = driver.get();
            if (driverInstance != null) {
                logger.info("🔴 Closing WebDriver...");
                driverInstance.quit();
                driver.remove();
                logger.info("✓ WebDriver closed successfully");
            }
        } catch (Exception e) {
            logger.error("✗ Error closing WebDriver", e);
        }
    }

    /**
     * Navigate to URL
     * @param url URL to navigate to
     */
    public static void navigateTo(String url) {
        try {
            logger.info("📍 Navigating to: " + url);
            getDriver().navigate().to(url);
            logger.info("✓ Navigation successful");
        } catch (Exception e) {
            logger.error("✗ Navigation failed to: " + url, e);
            throw new RuntimeException("Navigation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to application base URL
     */
    public static void navigateToBaseUrl() {
        navigateTo(ConfigManager.getBaseUrl());
    }

    /**
     * Get current page URL
     * @return Current URL
     */
    public static String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    /**
     * Refresh current page
     */
    public static void refreshPage() {
        try {
            getDriver().navigate().refresh();
            logger.info("✓ Page refreshed");
        } catch (Exception e) {
            logger.error("✗ Page refresh failed", e);
        }
    }

    /**
     * Clear cookies and cache
     */
    public static void clearCookies() {
        try {
            getDriver().manage().deleteAllCookies();
            logger.info("✓ All cookies cleared");
        } catch (Exception e) {
            logger.error("✗ Failed to clear cookies", e);
        }
    }
}
