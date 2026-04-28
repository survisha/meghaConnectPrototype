package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.utils.WebElementUtil;
import com.meghaconnect.automation.utils.ScreenshotUtil;
import com.meghaconnect.automation.pageobjects.locators.LoginPageLocators;
import org.openqa.selenium.By;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Login Page Object Model
 * Encapsulates all UI elements and operations on the Login page
 * 
 * NOTE: XPath and CSS selectors are centralized in LoginPageLocators class
 * This separation improves maintainability and allows easy updates to selectors
 */
public class LoginPage {
    private static final Logger logger = LogManager.getLogger(LoginPage.class);

    // ==================== LOCATORS ====================
    // All locators are now managed in LoginPageLocators class
    // This provides centralized locator management, better maintainability,
    // and easier updates without modifying this page object

    // Username/Email Input Field
    private static final By USERNAME_INPUT = LoginPageLocators.USERNAME_INPUT;
    
    // Password Input Field
    private static final By PASSWORD_INPUT = LoginPageLocators.PASSWORD_INPUT;
    
    // Login Button
    private static final By LOGIN_BUTTON = LoginPageLocators.LOGIN_BUTTON;
    
    // Remember Me Checkbox
    private static final By REMEMBER_ME_CHECKBOX = LoginPageLocators.REMEMBER_ME_CHECKBOX;
    
    // Forgot Password Link
    private static final By FORGOT_PASSWORD_LINK = LoginPageLocators.FORGOT_PASSWORD_LINK;
    
    // Signup/Register Link
    private static final By SIGNUP_LINK = LoginPageLocators.SIGNUP_LINK;
    
    // Error Message
    private static final By ERROR_MESSAGE = LoginPageLocators.ERROR_MESSAGE;
    
    // Success Message
    private static final By SUCCESS_MESSAGE = LoginPageLocators.SUCCESS_MESSAGE;
    
    // Loading Spinner
    private static final By LOADING_SPINNER = LoginPageLocators.LOADING_SPINNER;
    
    // Page Title
    private static final By PAGE_TITLE = LoginPageLocators.PAGE_TITLE;

    // ==================== CONSTRUCTOR ====================
    public LoginPage() {
        logger.info("╔══════════════════════════════════════╗");
        logger.info("║   Initializing Login Page Object     ║");
        logger.info("╚══════════════════════════════════════╝");
    }

    // ==================== PAGE VERIFICATION ====================

    /**
     * Verify if Login page is loaded
     * @return true if page loaded, false otherwise
     */
    public boolean isLoginPageLoaded() {
        try {
            WebElementUtil.waitForElementVisible(USERNAME_INPUT);
            logger.info("✓ Login page loaded successfully");
            return true;
        } catch (Exception e) {
            logger.error("✗ Login page failed to load", e);
            return false;
        }
    }

    /**
     * Wait for login page to be fully ready
     */
    public void waitForLoginPageReady() {
        try {
            logger.info("⏳ Waiting for login page to be ready...");
            WebElementUtil.waitForPageLoad();
            WebElementUtil.waitForElementVisible(USERNAME_INPUT);
            logger.info("✓ Login page is ready");
        } catch (Exception e) {
            logger.error("✗ Login page readiness check failed", e);
            throw new RuntimeException("Login page not ready: " + e.getMessage(), e);
        }
    }

    // ==================== USER INTERACTIONS ====================

    /**
     * Enter username
     * @param username Username to enter
     */
    public void enterUsername(String username) {
        try {
            logger.info("📝 Entering username: " + username);
            WebElementUtil.type(USERNAME_INPUT, username);
            ScreenshotUtil.captureScreenshotOnStep("Username_Entered");
        } catch (Exception e) {
            logger.error("✗ Failed to enter username", e);
            throw new RuntimeException("Failed to enter username: " + e.getMessage(), e);
        }
    }

    /**
     * Enter password
     * @param password Password to enter (not logged for security)
     */
    public void enterPassword(String password) {
        try {
            logger.info("🔐 Entering password (masked for security)");
            WebElementUtil.type(PASSWORD_INPUT, password);
            ScreenshotUtil.captureScreenshotOnStep("Password_Entered");
        } catch (Exception e) {
            logger.error("✗ Failed to enter password", e);
            throw new RuntimeException("Failed to enter password: " + e.getMessage(), e);
        }
    }

    /**
     * Click login button
     */
    public void clickLoginButton() {
        try {
            logger.info("🔘 Clicking Login button");
            WebElementUtil.click(LOGIN_BUTTON);
            logger.info("✓ Login button clicked");
            ScreenshotUtil.captureScreenshotOnStep("Login_Button_Clicked");
        } catch (Exception e) {
            logger.error("✗ Failed to click login button", e);
            throw new RuntimeException("Failed to click login button: " + e.getMessage(), e);
        }
    }

    /**
     * Perform complete login action
     * @param username Username
     * @param password Password
     */
    public void login(String username, String password) {
        try {
            logger.info("🔐 Starting login process for user: " + username);
            
            waitForLoginPageReady();
            enterUsername(username);
            enterPassword(password);
            clickLoginButton();
            
            // Wait for login to process
            WebElementUtil.waitForPageLoad();
            Thread.sleep(2000); // Wait for redirect
            
            logger.info("✓ Login process completed");
        } catch (Exception e) {
            logger.error("✗ Login process failed", e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check and check remember me checkbox
     */
    public void checkRememberMe() {
        try {
            logger.info("☑ Clicking Remember Me checkbox");
            WebElementUtil.click(REMEMBER_ME_CHECKBOX);
            logger.info("✓ Remember Me checked");
        } catch (Exception e) {
            logger.error("✗ Failed to check Remember Me", e);
        }
    }

    /**
     * Click forgot password link
     */
    public void clickForgotPassword() {
        try {
            logger.info("🔗 Clicking Forgot Password link");
            WebElementUtil.click(FORGOT_PASSWORD_LINK);
            logger.info("✓ Forgot Password link clicked");
            ScreenshotUtil.captureScreenshotOnStep("ForgotPassword_Clicked");
        } catch (Exception e) {
            logger.error("✗ Failed to click Forgot Password link", e);
            throw new RuntimeException("Failed to click Forgot Password: " + e.getMessage(), e);
        }
    }

    /**
     * Click signup/register link
     */
    public void clickSignup() {
        try {
            logger.info("🔗 Clicking Sign Up link");
            WebElementUtil.click(SIGNUP_LINK);
            logger.info("✓ Sign Up link clicked");
            ScreenshotUtil.captureScreenshotOnStep("Signup_Clicked");
        } catch (Exception e) {
            logger.error("✗ Failed to click Sign Up link", e);
            throw new RuntimeException("Failed to click Sign Up: " + e.getMessage(), e);
        }
    }

    // ==================== VALIDATIONS ====================

    /**
     * Get error message text
     * @return Error message or empty string if not present
     */
    public String getErrorMessage() {
        try {
            if (WebElementUtil.isElementDisplayed(ERROR_MESSAGE)) {
                String message = WebElementUtil.getText(ERROR_MESSAGE);
                logger.error("❌ Error message displayed: " + message);
                return message;
            }
            return "";
        } catch (Exception e) {
            logger.debug("⚠ No error message found");
            return "";
        }
    }

    /**
     * Check if error message is displayed
     * @return true if error displayed, false otherwise
     */
    public boolean isErrorMessageDisplayed() {
        return WebElementUtil.isElementDisplayed(ERROR_MESSAGE);
    }

    /**
     * Get success message text
     * @return Success message or empty string if not present
     */
    public String getSuccessMessage() {
        try {
            if (WebElementUtil.isElementDisplayed(SUCCESS_MESSAGE)) {
                String message = WebElementUtil.getText(SUCCESS_MESSAGE);
                logger.info("✓ Success message displayed: " + message);
                return message;
            }
            return "";
        } catch (Exception e) {
            logger.debug("⚠ No success message found");
            return "";
        }
    }

    /**
     * Check if loading spinner is displayed
     * @return true if loading, false otherwise
     */
    public boolean isLoadingSpinnerDisplayed() {
        try {
            return WebElementUtil.isElementDisplayed(LOADING_SPINNER);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for loading to complete
     */
    public void waitForLoadingComplete() {
        try {
            logger.info("⏳ Waiting for loading to complete...");
            WebElementUtil.waitForElementInvisible(LOADING_SPINNER);
            logger.info("✓ Loading complete");
        } catch (Exception e) {
            logger.debug("⚠ Loading spinner not found or already invisible");
        }
    }

    /**
     * Get username field value
     * @return Current username value
     */
    public String getUsernameValue() {
        try {
            return WebElementUtil.getAttribute(USERNAME_INPUT, "value");
        } catch (Exception e) {
            logger.error("✗ Failed to get username value", e);
            return "";
        }
    }

    /**
     * Clear username and password fields
     */
    public void clearCredentials() {
        try {
            logger.info("🗑️ Clearing all credential fields");
            WebElementUtil.waitForElementVisible(USERNAME_INPUT).clear();
            WebElementUtil.waitForElementVisible(PASSWORD_INPUT).clear();
            logger.info("✓ Credentials cleared");
        } catch (Exception e) {
            logger.error("✗ Failed to clear credentials", e);
        }
    }

    /**
     * Check if username field is focused
     * @return true if focused, false otherwise
     */
    public boolean isUsernameFocused() {
        try {
            return DriverManager.getDriver().switchTo().activeElement()
                    .equals(WebElementUtil.waitForElementVisible(USERNAME_INPUT));
        } catch (Exception e) {
            return false;
        }
    }
}
