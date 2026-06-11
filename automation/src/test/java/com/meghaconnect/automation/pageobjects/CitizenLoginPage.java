package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.utils.WebElementUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page Object for Citizen mobile OTP login.
 */
public class CitizenLoginPage {
    private static final Logger logger = LogManager.getLogger(CitizenLoginPage.class);

    // TODO: Replace absolute XPath with stable Angular ids/data-testid where possible.
    // Recommended ids: home_loginBtn, login_citizenOtpBtn, publicLogin_mobileInput,
    // publicLogin_generateOtpBtn, publicLogin_otpMessage, publicLogin_otpInput,
    // publicLogin_verifyBtn, visitorDashboard_profileHeader, shell_logoutBtn.
    public static final By LOGIN_BUTTON = By.xpath("//*[@id='home']/div[2]/div/button");
    public static final By CITIZEN_OTP_LOGIN_BUTTON = By.xpath("//*[@id='login_citizenOtpBtn']/span[2]");
    public static final By MOBILE_INPUT = By.xpath("//*[@id='publicLogin_mobileInput']");
    public static final By GENERATE_OTP_BUTTON = By.xpath("//*[@id='publicLogin_generateOtpBtn']/span[2]/span");
    public static final By OTP_MESSAGE = By.xpath("/html/body/app-root/app-public-login/div/div[2]/div[2]/div[2]/span");
    public static final By OTP_INPUT = By.xpath("//*[@id='mat-input-1']");
    public static final By VERIFY_LOGIN_BUTTON = By.xpath("/html/body/app-root/app-public-login/div/div[2]/div[2]/button/span[2]/span");
    public static final By VISITOR_PROFILE_HEADER = By.xpath("//*[@id='shell_mainContent']/app-visitor-dashboard/div/div[3]/div[1]/div[1]/div");
    public static final By LOGOUT_BUTTON = By.xpath("//*[@id='shell_logoutBtn']/mat-icon");
    public static final By UNKNOWN_MOBILE_ERROR_MSG = By.xpath("//*[@id=\"publicLogin_errorMsg\"]/div/span");

    private WebDriverWait webDriverWait() {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(ConfigManager.getExplicitWait()));
    }

    public void openHomePage() {
        logger.info("Opening MeghaConnect UAT home page: " + ConfigManager.getBaseUrl());
        DriverManager.navigateToBaseUrl();
        WebElementUtil.waitForVisibleWithHighlight(LOGIN_BUTTON);
        logger.info("Home page loaded and Login button is visible");
    }

    public void clickLogin() {
        logger.info("Clicking home Login button");
        WebElementUtil.clickWithHighlight(LOGIN_BUTTON);
    }

    public void clickCitizenOtpLogin() {
        logger.info("Clicking Citizen OTP Login");
        String beforeUrl = DriverManager.getCurrentUrl();
        WebElementUtil.clickWithHighlight(CITIZEN_OTP_LOGIN_BUTTON);
        webDriverWait().until(driver -> !driver.getCurrentUrl().equals(beforeUrl)
                || !driver.findElements(MOBILE_INPUT).isEmpty());
        WebElementUtil.waitForVisibleWithHighlight(MOBILE_INPUT);
        logger.info("Citizen public login page is ready");
    }

    public void enterMobileNumber(String mobileNumber) {
        logger.info("Entering mobile number from Excel: " + mobileNumber);
        WebElementUtil.typeWithHighlight(MOBILE_INPUT, mobileNumber);
    }

    public void clickGenerateOtp() {
        logger.info("Clicking Generate OTP");
        WebElementUtil.clickWithHighlight(GENERATE_OTP_BUTTON);
        WebElementUtil.waitForVisibleWithHighlight(OTP_MESSAGE);
        logger.info("OTP message is visible");
    }

    public String readOtpMessage() {
        String otpMessage = WebElementUtil.getTextWithHighlight(OTP_MESSAGE).trim();
        logger.info("OTP message from UI: " + otpMessage);
        return otpMessage;
    }

    public String extractDemoOtp(String otpMessage) {
        Matcher matcher = Pattern.compile("demo OTP:\\s*(\\d+)").matcher(otpMessage);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to extract demo OTP from UI message: " + otpMessage);
        }
        String otp = matcher.group(1);
        logger.info("Extracted demo OTP from UI message");
        return otp;
    }

    public void enterOtp(String otp) {
        logger.info("Entering OTP");
        WebElementUtil.typeWithHighlight(OTP_INPUT, otp);
    }

    public void clickVerifyAndLogin() {
        logger.info("Clicking Verify & Login");
        WebElementUtil.clickWithHighlight(VERIFY_LOGIN_BUTTON);
        WebElementUtil.waitForVisibleWithHighlight(VISITOR_PROFILE_HEADER);
        logger.info("Dashboard profile header is visible after login");
    }

    public boolean verifyVisitorProfileHeader() {
        logger.info("Verifying Visitor Profile header");
        WebElementUtil.waitForVisibleWithHighlight(VISITOR_PROFILE_HEADER);
        return true;
    }

    public boolean isVisitorProfileHeaderVisible() {
        try {
            return verifyVisitorProfileHeader();
        } catch (Exception e) {
            logger.error("Visitor Profile header is not visible", e);
            return false;
        }
    }

    public String getVisitorProfileHeaderText() {
        String headerText = WebElementUtil.getTextWithHighlight(VISITOR_PROFILE_HEADER).trim();
        logger.info("Visitor Profile header text: " + headerText);
        return headerText;
    }

    public void logout() {
        logger.info("Logging out citizen");
        WebElementUtil.clickWithHighlight(LOGOUT_BUTTON);
        webDriverWait().until(driver ->
                driver.findElements(LOGOUT_BUTTON).isEmpty()
                        || !driver.findElement(LOGOUT_BUTTON).isDisplayed()
                        || !driver.findElements(LOGIN_BUTTON).isEmpty()
                        || !driver.findElements(CITIZEN_OTP_LOGIN_BUTTON).isEmpty()
                        || !driver.findElements(MOBILE_INPUT).isEmpty()
                        || driver.getCurrentUrl().toLowerCase().contains("login")
                        || driver.getCurrentUrl().toLowerCase().contains("home"));
        logger.info("Citizen logged out successfully");
    }
}
