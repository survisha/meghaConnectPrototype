package com.meghaconnect.automation.utils;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot Utility - Handles screenshot capture and storage
 */
public class ScreenshotUtil {
    private static final Logger logger = LogManager.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOT_DIR = ConfigManager.getReportPath() + "screenshots/";

    static {
        // Create screenshot directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));
            logger.info("✓ Screenshot directory ready: " + SCREENSHOT_DIR);
        } catch (IOException e) {
            logger.error("✗ Failed to create screenshot directory", e);
        }
    }

    /**
     * Capture screenshot with timestamp
     * @param screenshotName Name for the screenshot
     * @return File path of the screenshot
     */
    public static String captureScreenshot(String screenshotName) {
        try {
            if (!DriverManager.isDriverInitialized()) {
                logger.warn("⚠ WebDriver not initialized, skipping screenshot");
                return null;
            }

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
            String fileName = screenshotName + "_" + timestamp + ".png";
            String filePath = SCREENSHOT_DIR + fileName;

            TakesScreenshot screenshot = (TakesScreenshot) DriverManager.getDriver();
            File source = screenshot.getScreenshotAs(OutputType.FILE);

            // Copy to target location
            File destination = new File(filePath);
            Files.copy(source.toPath(), destination.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            logger.info("📸 Screenshot captured: " + fileName);
            return filePath;

        } catch (Exception e) {
            logger.error("✗ Failed to capture screenshot", e);
            return null;
        }
    }

    /**
     * Capture screenshot on test failure
     * @param testName Test name for screenshot naming
     * @return Screenshot file path
     */
    public static String captureScreenshotOnFailure(String testName) {
        if (ConfigManager.isScreenshotOnFail()) {
            return captureScreenshot(testName + "_FAILED");
        }
        return null;
    }

    /**
     * Capture screenshot on test pass
     * @param testName Test name for screenshot naming
     * @return Screenshot file path
     */
    public static String captureScreenshotOnPass(String testName) {
        if (ConfigManager.isScreenshotOnPass()) {
            return captureScreenshot(testName + "_PASSED");
        }
        return null;
    }

    /**
     * Capture screenshot on step execution
     * @param stepName Step name for screenshot naming
     * @return Screenshot file path
     */
    public static String captureScreenshotOnStep(String stepName) {
        if (ConfigManager.isScreenshotOnStep()) {
            return captureScreenshot("STEP_" + stepName);
        }
        return null;
    }

    /**
     * Get screenshot directory
     * @return Screenshot directory path
     */
    public static String getScreenshotDirectory() {
        return SCREENSHOT_DIR;
    }
}
