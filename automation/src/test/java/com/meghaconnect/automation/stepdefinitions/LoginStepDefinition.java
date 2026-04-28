package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.flows.LoginFlow;
import com.meghaconnect.automation.pageobjects.LoginPage;
import com.meghaconnect.automation.utils.ScreenshotUtil;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.Assert.*;

/**
 * Login Step Definitions - Cucumber steps for Login feature
 * Implements BDD steps for login scenarios
 */
public class LoginStepDefinition {
    private static final Logger logger = LogManager.getLogger(LoginStepDefinition.class);

    private LoginFlow loginFlow;
    private LoginPage loginPage;
    private String testUsername;
    private String testPassword;
    private boolean loginResult;

    /**
     * Constructor - Initialize page objects and flows
     * Called by Cucumber PicoContainer
     */
    public LoginStepDefinition() {
        logger.debug("📌 Initializing LoginStepDefinition");
        this.loginFlow = LoginFlow.getInstance();
        this.loginPage = new LoginPage();
    }

    // ==================== GIVEN STEPS ====================

    /**
     * Given: User is on the login page
     */
    @Given("User is on the login page")
    public void userIsOnLoginPage() {
        logger.info("📋 GIVEN: User is on the login page");

        try {
            // Initialize browser
            if (!DriverManager.isDriverInitialized()) {
                DriverManager.initializeDriver();
            }

            // Navigate to login page
            DriverManager.navigateToBaseUrl();
            
            // Verify page is loaded
            assertTrue("Login page should be loaded", loginPage.isLoginPageLoaded());
            
            ScreenshotUtil.captureScreenshotOnStep("Given_LoginPage_Loaded");
            logger.info("✓ User is on login page");

        } catch (Exception e) {
            logger.error("✗ Failed to navigate to login page", e);
            throw new RuntimeException("Login page navigation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Given: User has valid credentials
     */
    @Given("User has valid credentials")
    public void userHasValidCredentials() {
        logger.info("📋 GIVEN: User has valid credentials");

        try {
            testUsername = ConfigManager.getApiAuthUsername();
            testPassword = ConfigManager.getApiAuthPassword();
            
            assertNotNull("Username should not be null", testUsername);
            assertNotNull("Password should not be null", testPassword);
            assertFalse("Username should not be empty", testUsername.isEmpty());
            assertFalse("Password should not be empty", testPassword.isEmpty());
            
            logger.info("✓ Valid credentials loaded: " + testUsername);

        } catch (Exception e) {
            logger.error("✗ Failed to load valid credentials", e);
            throw new RuntimeException("Credentials loading failed: " + e.getMessage(), e);
        }
    }

    /**
     * Given: User has invalid username
     */
    @Given("User has invalid username")
    public void userHasInvalidUsername() {
        logger.info("📋 GIVEN: User has invalid username");

        testUsername = "invalidusername123";
        testPassword = ConfigManager.getApiAuthPassword();
        
        logger.info("✓ Invalid username set: " + testUsername);
    }

    /**
     * Given: User has invalid password
     */
    @Given("User has invalid password")
    public void userHasInvalidPassword() {
        logger.info("📋 GIVEN: User has invalid password");

        testUsername = ConfigManager.getApiAuthUsername();
        testPassword = "wrongpassword123";
        
        logger.info("✓ Invalid password set");
    }

    /**
     * Given: User has empty credentials
     */
    @Given("User has empty credentials")
    public void userHasEmptyCredentials() {
        logger.info("📋 GIVEN: User has empty credentials");

        testUsername = "";
        testPassword = "";
        
        logger.info("✓ Empty credentials set");
    }

    /**
     * Given: User is already logged in
     */
    @Given("User is already logged in")
    public void userIsAlreadyLoggedIn() {
        logger.info("📋 GIVEN: User is already logged in");

        try {
            if (LoginFlow.isLoggedIn()) {
                logger.info("✓ User is already logged in (reusing session)");
            } else {
                userHasValidCredentials();
                userLogsInViaAPI();
            }

        } catch (Exception e) {
            logger.error("✗ Failed to establish logged-in state", e);
            throw new RuntimeException("Login establishment failed: " + e.getMessage(), e);
        }
    }

    // ==================== WHEN STEPS ====================

    /**
     * When: User enters username
     */
    @When("User enters username")
    public void userEntersUsername() {
        logger.info("📋 WHEN: User enters username");

        try {
            loginPage.enterUsername(testUsername);
            logger.info("✓ Username entered");

        } catch (Exception e) {
            logger.error("✗ Failed to enter username", e);
            throw new RuntimeException("Failed to enter username: " + e.getMessage(), e);
        }
    }

    /**
     * When: User enters password
     */
    @When("User enters password")
    public void userEntersPassword() {
        logger.info("📋 WHEN: User enters password");

        try {
            loginPage.enterPassword(testPassword);
            logger.info("✓ Password entered");

        } catch (Exception e) {
            logger.error("✗ Failed to enter password", e);
            throw new RuntimeException("Failed to enter password: " + e.getMessage(), e);
        }
    }

    /**
     * When: User clicks login button
     */
    @When("User clicks login button")
    public void userClicksLoginButton() {
        logger.info("📋 WHEN: User clicks login button");

        try {
            loginPage.clickLoginButton();
            logger.info("✓ Login button clicked");

        } catch (Exception e) {
            logger.error("✗ Failed to click login button", e);
            throw new RuntimeException("Failed to click login button: " + e.getMessage(), e);
        }
    }

    /**
     * When: User logs in via UI with valid credentials
     */
    @When("User logs in via UI with valid credentials")
    public void userLogsInViaUIWithValidCredentials() {
        logger.info("📋 WHEN: User logs in via UI with valid credentials");

        try {
            ensureLoginPageLoaded();
            loginResult = loginFlow.loginViaUI(testUsername, testPassword);
            logger.info("✓ UI login attempted");

        } catch (Exception e) {
            logger.error("✗ UI login failed", e);
            loginResult = false;
        }
    }

    /**
     * When: User logs in via API with valid credentials
     */
    @When("User logs in via API with valid credentials")
    public void userLogsInViaAPI() {
        logger.info("📋 WHEN: User logs in via API with valid credentials");

        try {
            loginResult = loginFlow.loginViaAPI(testUsername, testPassword);
            logger.info("✓ API login attempted");

        } catch (Exception e) {
            logger.error("✗ API login failed", e);
            loginResult = false;
        }
    }

    /**
     * When: User logs in with invalid credentials
     */
    @When("User logs in with invalid credentials")
    public void userLogsInWithInvalidCredentials() {
        logger.info("📋 WHEN: User logs in with invalid credentials");

        try {
            loginResult = loginFlow.loginWithInvalidCredentials(testUsername, testPassword);
            logger.info("✓ Invalid login attempt completed");

        } catch (Exception e) {
            logger.error("✗ Invalid login test failed", e);
            loginResult = false;
        }
    }

    /**
     * When: User clicks forgot password link
     */
    @When("User clicks forgot password link")
    public void userClicksForgotPasswordLink() {
        logger.info("📋 WHEN: User clicks forgot password link");

        try {
            loginPage.clickForgotPassword();
            logger.info("✓ Forgot password link clicked");

        } catch (Exception e) {
            logger.error("✗ Failed to click forgot password link", e);
            throw new RuntimeException("Failed to click forgot password: " + e.getMessage(), e);
        }
    }

    /**
     * When: User clicks signup link
     */
    @When("User clicks signup link")
    public void userClicksSignupLink() {
        logger.info("📋 WHEN: User clicks signup link");

        try {
            loginPage.clickSignup();
            logger.info("✓ Signup link clicked");

        } catch (Exception e) {
            logger.error("✗ Failed to click signup link", e);
            throw new RuntimeException("Failed to click signup: " + e.getMessage(), e);
        }
    }

    // ==================== THEN STEPS ====================

    /**
     * Then: Login should be successful
     */
    @Then("Login should be successful")
    public void loginShouldBeSuccessful() {
        logger.info("📋 THEN: Login should be successful");

        try {
            // Wait a bit for any redirects
            Thread.sleep(2000);
            
            assertTrue("Login should be successful", loginResult);
            assertTrue("User should be logged in", LoginFlow.isLoggedIn());
            
            ScreenshotUtil.captureScreenshotOnStep("Then_LoginSuccessful");
            logger.info("✓ Login successful - assertion passed");

        } catch (AssertionError e) {
            logger.error("✗ Login success assertion failed", e);
            ScreenshotUtil.captureScreenshotOnStep("Then_LoginFailed");
            throw e;
        } catch (Exception e) {
            logger.error("✗ Error during login success verification", e);
            throw new RuntimeException("Login verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Then: Login should fail
     */
    @Then("Login should fail")
    public void loginShouldFail() {
        logger.info("📋 THEN: Login should fail");

        try {
            assertFalse("Login should fail with invalid credentials", loginResult);
            
            ScreenshotUtil.captureScreenshotOnStep("Then_LoginFailed");
            logger.info("✓ Login failure - assertion passed");

        } catch (AssertionError e) {
            logger.error("✗ Login failure assertion failed", e);
            ScreenshotUtil.captureScreenshotOnStep("Then_LoginExpectedFailed");
            throw e;
        }
    }

    /**
     * Then: Error message should be displayed
     */
    @Then("Error message should be displayed")
    public void errorMessageShouldBeDisplayed() {
        logger.info("📋 THEN: Error message should be displayed");

        try {
            ensureLoginPageLoaded();
            assertTrue("Error message should be visible", loginPage.isErrorMessageDisplayed());
            
            String errorMsg = loginPage.getErrorMessage();
            logger.info("✓ Error message displayed: " + errorMsg);

        } catch (Exception e) {
            logger.error("✗ Error message not found", e);
            throw new RuntimeException("Error message verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Then: Error message should contain text
     */
    @Then("Error message should contain {string}")
    public void errorMessageShouldContain(String expectedText) {
        logger.info("📋 THEN: Error message should contain: " + expectedText);

        try {
            ensureLoginPageLoaded();
            String errorMsg = loginPage.getErrorMessage();
            assertTrue("Error message should contain: " + expectedText,
                    errorMsg.toLowerCase().contains(expectedText.toLowerCase()));
            
            logger.info("✓ Error message contains expected text");

        } catch (Exception e) {
            logger.error("✗ Text verification failed", e);
            throw new RuntimeException("Error message text verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Then: User should be logged in
     */
    @Then("User should be logged in")
    public void userShouldBeLoggedIn() {
        logger.info("📋 THEN: User should be logged in");

        try {
            assertTrue("User should be logged in", LoginFlow.isLoggedIn());
            assertNotNull("Session token should exist", LoginFlow.getSessionToken());
            
            logger.info("✓ User is logged in");

        } catch (AssertionError e) {
            logger.error("✗ Login assertion failed", e);
            ScreenshotUtil.captureScreenshotOnStep("Then_NotLoggedIn");
            throw e;
        }
    }

    /**
     * Then: User should not be logged in
     */
    @Then("User should not be logged in")
    public void userShouldNotBeLoggedIn() {
        logger.info("📋 THEN: User should not be logged in");

        try {
            assertFalse("User should not be logged in", LoginFlow.isLoggedIn());
            logger.info("✓ User is not logged in");

        } catch (AssertionError e) {
            logger.error("✗ Not logged in assertion failed", e);
            throw e;
        }
    }

    /**
     * Then: Page title should be visible
     */
    @Then("User should see login page")
    public void userShouldSeeLoginPage() {
        logger.info("📋 THEN: User should see login page");

        try {
            ensureLoginPageLoaded();
            logger.info("✓ Login page is visible");

        } catch (Exception e) {
            logger.error("✗ Login page not visible", e);
            throw new RuntimeException("Login page visibility check failed: " + e.getMessage(), e);
        }
    }

    // ==================== AND STEPS ====================

    /**
     * And: User should continue with the session
     */
    @And("User should continue with the session")
    public void userShouldContinueWithSession() {
        logger.info("📋 AND: User should continue with the session");

        try {
            boolean sessionValid = loginFlow.continueWithSession();
            assertTrue("Session should be valid", sessionValid);
            logger.info("✓ Continuing with established session");

        } catch (Exception e) {
            logger.error("✗ Session continuation failed", e);
            throw new RuntimeException("Session continuation failed: " + e.getMessage(), e);
        }
    }

    /**
     * And: User logs out
     */
    @And("User logs out")
    public void userLogsOut() {
        logger.info("📋 AND: User logs out");

        try {
            loginFlow.logout();
            assertFalse("User should not be logged in after logout", LoginFlow.isLoggedIn());
            logger.info("✓ User logged out successfully");

        } catch (Exception e) {
            logger.error("✗ Logout failed", e);
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Ensure login page is loaded before interactions
     */
    private void ensureLoginPageLoaded() {
        if (!DriverManager.isDriverInitialized()) {
            DriverManager.initializeDriver();
            DriverManager.navigateToBaseUrl();
        }
        
        if (!loginPage.isLoginPageLoaded()) {
            throw new RuntimeException("Login page is not loaded");
        }
    }
}
