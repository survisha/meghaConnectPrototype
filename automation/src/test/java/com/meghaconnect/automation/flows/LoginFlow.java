package com.meghaconnect.automation.flows;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.pageobjects.LoginPage;
import com.meghaconnect.automation.apitests.LoginApiTest;
import com.meghaconnect.automation.utils.ScreenshotUtil;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Login Flow - Reusable login workflow
 * Performs login via UI or API and establishes session for subsequent flows
 * IMPORTANT: Login should happen once and subsequent flows should continue with the same session
 */
public class LoginFlow {
    private static final Logger logger = LogManager.getLogger(LoginFlow.class);
    
    // Session state - singleton pattern
    private static LoginFlow instance;
    private static String currentUsername;
    private static String currentPassword;
    private static String sessionToken;
    private static boolean isLoggedIn = false;
    
    private LoginPage loginPage;
    private LoginApiTest loginApiTest;

    private LoginFlow() {
        this.loginPage = new LoginPage();
        this.loginApiTest = new LoginApiTest();
    }

    /**
     * Get singleton instance of LoginFlow
     * Ensures only one login per test session
     * @return LoginFlow instance
     */
    public static synchronized LoginFlow getInstance() {
        if (instance == null) {
            instance = new LoginFlow();
            logger.info("✓ LoginFlow singleton instantiated");
        }
        return instance;
    }

    /**
     * Check if user is already logged in (session exists)
     * @return true if logged in, false otherwise
     */
    public static synchronized boolean isLoggedIn() {
        return isLoggedIn && sessionToken != null && !sessionToken.isEmpty();
    }

    /**
     * Get current session token
     * @return Session token or null
     */
    public static synchronized String getSessionToken() {
        return sessionToken;
    }

    /**
     * Get current logged-in username
     * @return Username
     */
    public static synchronized String getCurrentUsername() {
        return currentUsername;
    }

    // ==================== UI-BASED LOGIN ====================

    /**
     * Login via UI (browser automation)
     * Prevents multiple logins in same session
     * @param username Username
     * @param password Password
     * @return true if login successful, false otherwise
     */
    public synchronized boolean loginViaUI(String username, String password) {
        logger.info("╔═══════════════════════════════════════╗");
        logger.info("║   LOGIN FLOW - UI-Based              ║");
        logger.info("╚═══════════════════════════════════════╝");

        // Check if already logged in with same user
        if (isLoggedIn && currentUsername != null && currentUsername.equals(username)) {
            logger.info("ℹ️  User '" + username + "' already logged in - reusing session");
            return true;
        }

        try {
            logger.info("🔐 Initiating UI login for user: " + username);
            
            // Initialize WebDriver if not already done
            if (!DriverManager.isDriverInitialized()) {
                logger.info("🌐 Initializing browser...");
                DriverManager.initializeDriver();
            }

            // Navigate to login page
            logger.info("📍 Navigating to login page");
            DriverManager.navigateToBaseUrl();
            
            // Wait for login page to load
            loginPage.waitForLoginPageReady();

            // Perform login
            logger.info("🔑 Performing login with credentials");
            loginPage.login(username, password);

            // Wait for loading to complete
            loginPage.waitForLoadingComplete();

            // Verify login success (check URL changed or success message)
            Thread.sleep(3000); // Wait for redirect
            
            String currentUrl = DriverManager.getCurrentUrl();
            logger.info("✓ Current URL after login: " + currentUrl);

            // Check if login failed
            String errorMessage = loginPage.getErrorMessage();
            if (!errorMessage.isEmpty()) {
                logger.error("✗ Login failed with error: " + errorMessage);
                ScreenshotUtil.captureScreenshot("LOGIN_FAILED");
                return false;
            }

            // Login successful - store session state
            currentUsername = username;
            currentPassword = password;
            isLoggedIn = true;
            
            logger.info("✓ UI Login successful for user: " + username);
            ScreenshotUtil.captureScreenshot("LOGIN_SUCCESS");
            
            return true;

        } catch (Exception e) {
            logger.error("✗ UI Login failed", e);
            ScreenshotUtil.captureScreenshot("LOGIN_ERROR");
            isLoggedIn = false;
            return false;
        }
    }

    // ==================== API-BASED LOGIN ====================

    /**
     * Login via API (REST authentication)
     * Prevents multiple logins in same session
     * @param username Username
     * @param password Password
     * @return true if login successful, false otherwise
     */
    public synchronized boolean loginViaAPI(String username, String password) {
        logger.info("╔═══════════════════════════════════════╗");
        logger.info("║   LOGIN FLOW - API-Based             ║");
        logger.info("╚═══════════════════════════════════════╝");

        // Check if already logged in with same user
        if (isLoggedIn && currentUsername != null && currentUsername.equals(username)) {
            logger.info("ℹ️  User '" + username + "' already logged in - reusing session");
            logger.info("  Reusing token: " + maskToken(sessionToken));
            return true;
        }

        try {
            logger.info("🔐 Initiating API login for user: " + username);
            
            // Call login API
            Response response = loginApiTest.testLoginWithValidCredentials(username, password);

            // Check response status
            if (response.getStatusCode() != 200) {
                logger.error("✗ API Login failed with status: " + response.getStatusCode());
                String errorMsg = response.jsonPath().getString("message");
                logger.error("  Error: " + errorMsg);
                return false;
            }

            // Validate response structure
            if (!loginApiTest.validateResponseStructure(response)) {
                logger.error("✗ Invalid response structure");
                return false;
            }

            // Validate token format
            String token = response.jsonPath().getString("data.token");
            if (!loginApiTest.validateTokenFormat(token)) {
                logger.error("✗ Invalid token format");
                return false;
            }

            // Store session state
            currentUsername = username;
            currentPassword = password;
            sessionToken = token;
            isLoggedIn = true;
            
            logger.info("✓ API Login successful for user: " + username);
            logger.info("  Token: " + maskToken(sessionToken));
            logger.info("  User ID: " + loginApiTest.getUserId());
            logger.info("  Role: " + loginApiTest.getUserRole());
            
            return true;

        } catch (Exception e) {
            logger.error("✗ API Login failed", e);
            isLoggedIn = false;
            return false;
        }
    }

    // ==================== LOGIN VALIDATION ====================

    /**
     * Verify login with invalid credentials (negative test)
     * @param username Username
     * @param password Password
     * @return true if correctly rejected, false otherwise
     */
    public boolean loginWithInvalidCredentials(String username, String password) {
        logger.info("╔═══════════════════════════════════════╗");
        logger.info("║   LOGIN FLOW - Invalid Credentials   ║");
        logger.info("╚═══════════════════════════════════════╝");

        try {
            logger.info("🔐 Testing login with invalid credentials");
            
            Response response = loginApiTest.testLoginWithInvalidCredentials(username, password);
            
            if (response.getStatusCode() == 401 || response.getStatusCode() == 400) {
                logger.info("✓ Correctly rejected invalid credentials");
                return true;
            } else {
                logger.error("✗ Unexpected response status: " + response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("✗ Invalid credentials test failed", e);
            return false;
        }
    }

    /**
     * Verify login with missing fields
     * @return true if correctly rejected, false otherwise
     */
    public boolean loginWithMissingCredentials() {
        logger.info("📋 Testing login with missing credentials");

        try {
            Response response = loginApiTest.testLoginWithEmptyCredentials();
            
            if (response.getStatusCode() == 400) {
                logger.info("✓ Correctly rejected missing credentials");
                return true;
            } else {
                logger.warn("⚠ Unexpected response status: " + response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("✗ Test failed", e);
            return false;
        }
    }

    // ==================== LOGOUT ====================

    /**
     * Logout and clear session
     */
    public synchronized void logout() {
        logger.info("🔓 Logging out...");

        try {
            // Call logout API
            loginApiTest.testLogout();
            
            // Clear session state
            currentUsername = null;
            currentPassword = null;
            sessionToken = null;
            isLoggedIn = false;
            
            logger.info("✓ Logout successful");

        } catch (Exception e) {
            logger.error("✗ Logout failed", e);
            // Force clear session state anyway
            isLoggedIn = false;
            sessionToken = null;
        }
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Reset login session (for testing different users)
     * Closes browser and clears session
     */
    public static synchronized void resetSession() {
        logger.info("🔄 Resetting login session...");
        
        // Close webdriver
        if (DriverManager.isDriverInitialized()) {
            DriverManager.quitDriver();
        }
        
        // Clear session state
        currentUsername = null;
        currentPassword = null;
        sessionToken = null;
        isLoggedIn = false;
        
        // Reset singleton
        instance = null;
        
        logger.info("✓ Session reset complete");
    }

    /**
     * Continue with existing session (for subsequent flows)
     * Use this method instead of loginViaUI/loginViaAPI in subsequent flows
     * @return true if session valid, false if session expired
     */
    public synchronized boolean continueWithSession() {
        if (isLoggedIn && sessionToken != null) {
            logger.info("✓ Continuing with existing session for user: " + currentUsername);
            return true;
        } else {
            logger.error("✗ No valid session available");
            return false;
        }
    }

    /**
     * Get flow status summary
     * @return Status string
     */
    public String getFlowStatus() {
        StringBuilder status = new StringBuilder();
        status.append("\n╔════════════════════════════════════╗\n");
        status.append("║      LOGIN FLOW STATUS             ║\n");
        status.append("╠════════════════════════════════════╣\n");
        status.append("║ Logged In: ").append(isLoggedIn ? "YES" : "NO").append("\n");
        status.append("║ Username: ").append(currentUsername != null ? currentUsername : "N/A").append("\n");
        status.append("║ Token: ").append(sessionToken != null ? maskToken(sessionToken) : "N/A").append("\n");
        status.append("╚════════════════════════════════════╝\n");
        return status.toString();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Mask token for logging
     * @param token Token to mask
     * @return Masked token
     */
    private static String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
