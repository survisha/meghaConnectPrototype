package com.meghaconnect.automation.flows;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.pageobjects.CitizenLoginPage;
import com.meghaconnect.automation.utils.ExcelUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Citizen OTP login flow using Excel test data and configurable OTP source.
 */
public class CitizenLoginFlow {
    private static final Logger logger = LogManager.getLogger(CitizenLoginFlow.class);
    private static final String SHEET_NAME = "CitizenLogin";

    private final CitizenLoginPage citizenLoginPage;
    private boolean dashboardVisible;

    public CitizenLoginFlow() {
        this.citizenLoginPage = new CitizenLoginPage();
    }

    public void openHomePage() {
        citizenLoginPage.openHomePage();
    }

    public void executeCitizenOtpLogin(String scenarioName) {
        logger.info("Executing Citizen OTP login for scenario: " + scenarioName);
        Map<String, String> testData = ExcelUtil.getRowData(SHEET_NAME, scenarioName);

        String mobileNumber = testData.getOrDefault("mobileNumber", "");
        String expectedResult = testData.getOrDefault("expectedResult", "");

        assertFalse("mobileNumber must be present in Excel for scenario: " + scenarioName, mobileNumber.isEmpty());
        assertEquals("Expected result should be DASHBOARD for valid Citizen OTP login", "DASHBOARD", expectedResult);

        citizenLoginPage.openHomePage();
        citizenLoginPage.clickLogin();
        citizenLoginPage.clickCitizenOtpLogin();
        citizenLoginPage.enterMobileNumber(mobileNumber);
        citizenLoginPage.clickGenerateOtp();

        String otp = resolveOtp(testData);
        citizenLoginPage.enterOtp(otp);
        citizenLoginPage.clickVerifyAndLogin();

        dashboardVisible = citizenLoginPage.isVisitorProfileHeaderVisible();
        assertTrue("Visitor Profile header should be visible after login", dashboardVisible);
    }

    public boolean isDashboardVisible() {
        dashboardVisible = citizenLoginPage.isVisitorProfileHeaderVisible();
        return dashboardVisible;
    }

    public String getDashboardHeaderText() {
        return citizenLoginPage.getVisitorProfileHeaderText();
    }

    public void logout() {
        citizenLoginPage.logout();
    }

    private String resolveOtp(Map<String, String> testData) {
        String otpMode = ConfigManager.getOtpMode();
        logger.info("Resolving OTP using mode: " + otpMode);

        switch (otpMode) {
            case "UI_DEMO":
                return citizenLoginPage.extractDemoOtp(citizenLoginPage.readOtpMessage());
            case "EXCEL":
                String excelOtp = testData.getOrDefault("otp", "");
                assertFalse("otp column must contain OTP when otp.mode=EXCEL", excelOtp.isEmpty());
                return excelOtp;
            case "MANUAL":
                logger.warn("OTP sent successfully, manual OTP required.");
                throw new UnsupportedOperationException("Manual OTP entry is not interactive in this automation run");
            case "API":
                return fetchOtpFromApiPlaceholder(testData);
            default:
                throw new IllegalArgumentException("Unsupported otp.mode: " + otpMode);
        }
    }

    private String fetchOtpFromApiPlaceholder(Map<String, String> testData) {
        throw new UnsupportedOperationException("otp.mode=API placeholder: implement secure backend OTP fetch API when available");
    }
}
