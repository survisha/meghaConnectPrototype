package com.meghaconnect.automation.hooks;

import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.config.ApiClient;
import com.meghaconnect.automation.flows.LoginFlow;
import com.meghaconnect.automation.utils.ScreenshotUtil;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cucumber Hooks - Setup and teardown for tests
 * Handles WebDriver initialization, cleanup, and reporting
 */
public class TestHooks {
    private static final Logger logger = LogManager.getLogger(TestHooks.class);

    /**
     * Before Hook - Executes before each scenario
     * @param scenario Cucumber scenario
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        ScreenshotUtil.setScenario(scenario);
        logger.info("╔═══════════════════════════════════════╗");
        logger.info("║     SCENARIO START: " + scenario.getName().substring(0, Math.min(20, scenario.getName().length())) + "          ║");
        logger.info("╚═══════════════════════════════════════╝");

        try {
            // Initialize API client
            logger.info("🌐 Initializing API client...");
            ApiClient.reset();
            logger.info("✓ API client reset");

            if (scenario.getSourceTagNames().contains("@UITest") && !DriverManager.isDriverInitialized()) {
                logger.info("🔧 Initializing WebDriver for UI scenario: " + scenario.getName());
                DriverManager.initializeDriver();
                logger.info("✓ WebDriver ready for scenario: " + scenario.getName());
            }

            if (DriverManager.isDriverInitialized() && com.meghaconnect.automation.config.ConfigManager.isScreenshotEachStep()) {
                ScreenshotUtil.captureScreenshot("SCENARIO_START");
            }

            logger.info("✓ Pre-scenario setup completed");

        } catch (Exception e) {
            logger.error("✗ Pre-scenario setup failed", e);
            throw new RuntimeException("Before hook failed: " + e.getMessage(), e);
        }
    }

    /**
     * After Hook - Executes after each scenario
     * @param scenario Cucumber scenario
     */
    @After
    public void afterScenario(Scenario scenario) {
        logger.info("╔═══════════════════════════════════════╗");
        logger.info("║     SCENARIO END: " + scenario.getStatus() + "          ║");
        logger.info("╚═══════════════════════════════════════╝");

        try {
            if (DriverManager.isDriverInitialized() && com.meghaconnect.automation.config.ConfigManager.isScreenshotEachStep()) {
                ScreenshotUtil.captureScreenshot("SCENARIO_END_" + scenario.getStatus());
            }

            // Take screenshot on failure
            if (scenario.isFailed()) {
                logger.error("❌ Scenario failed: " + scenario.getName());
                if (DriverManager.isDriverInitialized() && com.meghaconnect.automation.config.ConfigManager.isScreenshotOnFail()) {
                    String screenshotPath = ScreenshotUtil.captureScreenshot("SCENARIO_FAILED_" + scenario.getName().replaceAll("\\s+", "_"));
                    logger.error("📸 Screenshot captured: " + screenshotPath);
                }
            } else {
                logger.info("✓ Scenario passed: " + scenario.getName());
                if (DriverManager.isDriverInitialized() && com.meghaconnect.automation.config.ConfigManager.isScreenshotOnPass()) {
                    ScreenshotUtil.captureScreenshot("SCENARIO_PASSED_" + scenario.getName().replaceAll("\\s+", "_"));
                }
            }

            // Cleanup WebDriver
            if (DriverManager.isDriverInitialized()) {
                logger.info("🔴 Closing WebDriver...");
                DriverManager.quitDriver();
                logger.info("✓ WebDriver closed");
            }

            // Cleanup API client
            logger.info("🔴 Resetting API client...");
            ApiClient.reset();
            logger.info("✓ API client reset");

            // Don't reset LoginFlow session here - it should persist across scenarios
            // unless explicitly reset by the user or new instance created
            
            logger.info("✓ Post-scenario cleanup completed\n");
            ScreenshotUtil.clearScenario();

        } catch (Exception e) {
            logger.error("✗ Post-scenario cleanup failed", e);
            // Ensure WebDriver is closed even if cleanup fails
            try {
                DriverManager.quitDriver();
            } catch (Exception ex) {
                logger.error("✗ Failed to force close WebDriver", ex);
            }
            ScreenshotUtil.clearScenario();
        }
    }

    /**
     * Before Hook for API tests only
     * Tag: @APITest
     */
    @Before("@APITest")
    public void beforeApiTest() {
        logger.info("🔧 API Test Setup - Initializing API client");
        try {
            ApiClient.reset();
            logger.info("✓ API client ready for testing");
        } catch (Exception e) {
            logger.error("✗ API test setup failed", e);
        }
    }

    /**
     * Before Hook for UI tests only
     * Tag: @UITest
     */
    @Before("@UITest")
    public void beforeUiTest() {
        logger.info("🔧 UI Test Setup - Initializing WebDriver");
        try {
            if (!DriverManager.isDriverInitialized()) {
                DriverManager.initializeDriver();
                logger.info("✓ WebDriver ready for testing");
            }
        } catch (Exception e) {
            logger.error("✗ UI test setup failed", e);
            throw new RuntimeException("WebDriver initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Before Hook for Login tests
     * Tag: @Login
     */
    @Before("@Login")
    public void beforeLoginTest() {
        logger.info("🔧 Login Test Setup");
        try {
            // Don't auto-logout here - allow tests to handle login state
            logger.info("✓ Login test setup completed");
        } catch (Exception e) {
            logger.error("✗ Login test setup failed", e);
        }
    }

    /**
     * After Hook for Login tests
     * Tag: @Login
     */
    @After("@Login")
    public void afterLoginTest() {
        logger.info("🔧 Login Test Cleanup");
        try {
            // Reset login session for next test
            if (LoginFlow.isLoggedIn()) {
                logger.info("🔓 Auto-logging out after login test...");
                LoginFlow.getInstance().logout();
            }
            logger.info("✓ Login test cleanup completed");
        } catch (Exception e) {
            logger.error("✗ Login test cleanup failed", e);
        }
    }

    /**
     * After Hook for scenarios needing session persistence
     * Tag: @PersistSession
     * Skips logout to maintain session for next scenario
     */
    @After("@PersistSession")
    public void afterPersistentSessionTest() {
        logger.info("🔧 Persistent Session Test Cleanup");
        logger.info("ℹ️  Session preserved for next scenario (tag: @PersistSession)");
        // Don't logout - session continues
    }

    /**
     * After Hook to explicitly reset session
     * Tag: @ResetSession
     */
    @After("@ResetSession")
    public void afterResetSessionTest() {
        logger.info("🔧 Resetting Session...");
        try {
            LoginFlow.resetSession();
            logger.info("✓ Session reset completed");
        } catch (Exception e) {
            logger.error("✗ Session reset failed", e);
        }
    }
}
