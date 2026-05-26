package com.meghaconnect.automation.utils;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot Utility - Handles screenshot capture, storage, and Cucumber attachment.
 */
public class ScreenshotUtil {
    private static final Logger logger = LogManager.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOT_DIR = "target/cucumber-reports/screenshots/";
    private static final ThreadLocal<Scenario> CURRENT_SCENARIO = new ThreadLocal<>();

    private ScreenshotUtil() {
    }

    public static void setScenario(Scenario scenario) {
        CURRENT_SCENARIO.set(scenario);
    }

    public static void clearScenario() {
        CURRENT_SCENARIO.remove();
    }

    public static String captureScreenshot(String fileName) {
        try {
            if (!DriverManager.isDriverInitialized()) {
                logger.warn("WebDriver not initialized, skipping screenshot");
                return null;
            }

            Files.createDirectories(Paths.get(SCREENSHOT_DIR));

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
            String scenarioName = getScenarioName();
            String safeFileName = sanitize(scenarioName + "_" + fileName + "_" + timestamp) + ".png";
            String filePath = SCREENSHOT_DIR + safeFileName;

            TakesScreenshot screenshot = (TakesScreenshot) DriverManager.getDriver();
            File source = screenshot.getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), Paths.get(filePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            attachScreenshotBytes(screenshot.getScreenshotAs(OutputType.BYTES), safeFileName);
            logger.info("Screenshot captured: " + filePath);
            return filePath;
        } catch (Exception e) {
            logger.error("Failed to capture screenshot: " + fileName, e);
            return null;
        }
    }

    public static String captureScreenshotOnFailure(String testName) {
        if (ConfigManager.isScreenshotOnFail()) {
            return captureScreenshot(testName + "_FAILED");
        }
        return null;
    }

    public static String captureScreenshotOnPass(String testName) {
        if (ConfigManager.isScreenshotOnPass()) {
            return captureScreenshot(testName + "_PASSED");
        }
        return null;
    }

    public static String captureScreenshotOnStep(String stepName) {
        if (ConfigManager.isScreenshotEachStep()) {
            return captureScreenshot("STEP_" + stepName);
        }
        return null;
    }

    public static String getScreenshotDirectory() {
        return SCREENSHOT_DIR;
    }

    private static void attachScreenshotBytes(byte[] bytes, String name) {
        Scenario scenario = CURRENT_SCENARIO.get();
        if (scenario != null) {
            scenario.attach(bytes, "image/png", name);
        }
    }

    private static String getScenarioName() {
        Scenario scenario = CURRENT_SCENARIO.get();
        return scenario == null ? "NO_SCENARIO" : scenario.getName();
    }

    private static String sanitize(String value) {
        return value == null ? "screenshot" : value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }
}
