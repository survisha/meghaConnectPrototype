package com.meghaconnect.automation.config;

import java.io.*;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration Manager - Handles all property reading from application.properties
 * Supports environment-based configuration switching
 */
public class ConfigManager {
    private static final Logger logger = LogManager.getLogger(ConfigManager.class);
    private static Properties properties;
    private static final String PROPERTIES_FILE = "src/test/resources/config/application.properties";

    static {
        properties = new Properties();
        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(input);
            logger.info("✓ Properties loaded successfully from: " + PROPERTIES_FILE);
            logger.info("  Environment: " + getProperty("environment"));
            logger.info("  Base URL: " + getProperty("base.url"));
            logger.info("  API URL: " + getProperty("api.base.url"));
        } catch (IOException e) {
            logger.error("✗ Failed to load properties file: " + PROPERTIES_FILE, e);
            throw new RuntimeException("Cannot load configuration properties", e);
        }
    }

    /**
     * Get property value with null check
     * @param key Property key
     * @return Property value or default empty string
     */
    public static String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(key);
        }
        if (value == null) {
            logger.warn("⚠ Property not found: " + key);
            return "";
        }
        return value.trim();
    }

    /**
     * Get property value with default fallback
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value or default
     */
    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(key, defaultValue);
        }
        return value.trim();
    }

    public static String get(String key) {
        return getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }

    /**
     * Get boolean property
     * @param key Property key
     * @return Boolean value
     */
    public static Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getProperty(key, "false"));
    }

    /**
     * Get integer property
     * @param key Property key
     * @return Integer value
     */
    public static Integer getInteger(String key) {
        try {
            return Integer.parseInt(getProperty(key, "0"));
        } catch (NumberFormatException e) {
            logger.warn("⚠ Invalid integer property: " + key);
            return 0;
        }
    }

    // Convenience Methods for Commonly Used Properties

    public static String getEnvironment() {
        return getProperty("environment", "dev");
    }

    public static String getBaseUrl() {
        return getProperty("base.url", "http://localhost:4200");
    }

    public static String getApiBaseUrl() {
        return getProperty("api.base.url", "http://localhost:8080");
    }

    public static String getBrowser() {
        return getProperty("browser", "chrome").toLowerCase();
    }

    public static Boolean isHeadless() {
        return getBoolean("headless");
    }

    public static Integer getImplicitWait() {
        return getInteger("implicit.wait");
    }

    public static Integer getExplicitWait() {
        return getInteger("explicit.wait");
    }

    public static Integer getPageLoadTimeout() {
        return getInteger("page.load.timeout");
    }

    public static Boolean isBrowserMaximize() {
        return getBoolean("browser.maximize");
    }

    public static Boolean isIncognito() {
        return getBoolean("browser.incognito");
    }

    public static String getApiAuthTokenEndpoint() {
        return getProperty("api.auth.token.endpoint", "/api/v1/auth/login");
    }

    public static String getApiAuthUsername() {
        return getProperty("api.auth.username");
    }

    public static String getApiAuthPassword() {
        return getProperty("api.auth.password");
    }

    public static String getReportPath() {
        return getProperty("report.path", "test-output/");
    }

    public static Boolean isScreenshotOnPass() {
        return getBoolean("screenshot.on.pass");
    }

    public static Boolean isScreenshotOnFail() {
        return getBoolean("screenshot.on.fail");
    }

    public static Boolean isScreenshotOnStep() {
        return getBoolean("screenshot.on.step");
    }

    public static Boolean isHighlightEnabled() {
        return getBoolean("highlight.enabled");
    }

    public static Boolean isScreenshotEachStep() {
        return getBoolean("screenshot.each.step");
    }

    public static Integer getHighlightDurationMs() {
        int duration = getInteger("highlight.duration.ms");
        return duration > 0 ? duration : 700;
    }

    public static Integer getRetryCount() {
        return getInteger("retry.count");
    }

    public static Integer getApiRequestTimeout() {
        return getInteger("api.request.timeout");
    }

    public static String getTestDataPath() {
        return getProperty("test.data.path", "src/test/resources/testdata/");
    }

    public static String getTestDataExcelPath() {
        return getProperty("testdata.excel.path", "src/test/resources/testdata/citizen-login-testdata.xlsx");
    }

    public static String getTestOtp() {
        return getProperty("test.otp", "123456");
    }

    public static String getOtpMode() {
        return getProperty("otp.mode", "UI_DEMO").toUpperCase();
    }

    public static String getLogLevel() {
        return getProperty("log.level", "INFO");
    }

    public static void reloadProperties() {
        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            properties = new Properties();
            properties.load(input);
            logger.info("✓ Properties reloaded successfully");
        } catch (IOException e) {
            logger.error("✗ Failed to reload properties", e);
        }
    }
}
