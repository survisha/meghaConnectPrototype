package com.meghaconnect.automation.pageobjects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.pageobjects.locators.AdminPageLocators;
import com.meghaconnect.automation.utils.WebElementUtil;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class AdminPage extends MeghaConnectModulePage {
    private static final String ADMIN_DASHBOARD_HEADER = "System Admin";
    private static final String LOGIN_TEST_DATA_FILE = "login-testdata.json";

    public void openBaseUrl() {
        DriverManager.navigateToBaseUrl();
        WebElementUtil.waitForVisibleWithHighlight(AdminPageLocators.HOME_LOGIN_BUTTON);
    }

    public void clickHomeLoginButton() {
        WebElementUtil.clickWithHighlight(AdminPageLocators.HOME_LOGIN_BUTTON);
        WebElementUtil.waitForElementClickable(AdminPageLocators.STAFF_LOGIN_TAB);
    }

    public void selectStaffLoginTab() {
        WebElementUtil.clickWithHighlight(AdminPageLocators.STAFF_LOGIN_TAB);
        WebElementUtil.waitForVisibleWithHighlight(AdminPageLocators.USERNAME_INPUT);
        WebElementUtil.waitForVisibleWithHighlight(AdminPageLocators.PASSWORD_INPUT);
    }

    public void loginAsAdminFromTestData() {
        JsonNode adminCredentials = readAdminCredentials();
        WebElementUtil.typeWithHighlight(AdminPageLocators.USERNAME_INPUT, requiredText(adminCredentials, "username"));
        WebElementUtil.typeWithHighlight(AdminPageLocators.PASSWORD_INPUT, requiredText(adminCredentials, "password"));
        WebElementUtil.clickWithHighlight(AdminPageLocators.SIGN_IN_BUTTON);
        waitForLoginOutcome();
    }

    public void verifyAdminDashboardHeader() {
        String actualHeader = WebElementUtil.getTextWithHighlight(AdminPageLocators.DASHBOARD_HEADER).trim();
        Assert.assertEquals("Unexpected admin dashboard header", ADMIN_DASHBOARD_HEADER, actualHeader);
    }

    public void logout() {
        WebElementUtil.clickWithHighlight(AdminPageLocators.LOGOUT_BUTTON);
        WebElementUtil.waitForElementInvisible(AdminPageLocators.LOGOUT_BUTTON);
        WebElementUtil.waitForElementClickable(AdminPageLocators.HOME_LOGIN_BUTTON);
    }

    public void openUsers() {
        openRelativePath("/admin/users");
    }

    public void openAppointmentTypes() {
        openRelativePath("/admin/appointment-types");
    }

    private JsonNode readAdminCredentials() {
        File testDataFile = new File(ConfigManager.getTestDataPath(), LOGIN_TEST_DATA_FILE);
        try {
            JsonNode credentials = new ObjectMapper()
                    .readTree(testDataFile)
                    .path("login")
                    .path("adminCredentials");
            if (credentials.isMissingNode() || credentials.isEmpty()) {
                throw new IllegalStateException("Missing login.adminCredentials in " + testDataFile.getPath());
            }
            return credentials;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read admin credentials from " + testDataFile.getPath(), e);
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText();
        if (value.isBlank()) {
            throw new IllegalStateException("Missing adminCredentials." + fieldName + " in " + LOGIN_TEST_DATA_FILE);
        }
        return value;
    }

    private void waitForLoginOutcome() {
        WebDriverWait wait = new WebDriverWait(
                DriverManager.getDriver(),
                Duration.ofSeconds(ConfigManager.getExplicitWait()));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.textToBePresentInElementLocated(
                        AdminPageLocators.DASHBOARD_HEADER,
                        ADMIN_DASHBOARD_HEADER),
                ExpectedConditions.visibilityOfElementLocated(AdminPageLocators.LOGIN_ERROR)));

        List<WebElement> loginErrors = DriverManager.getDriver().findElements(AdminPageLocators.LOGIN_ERROR);
        if (!loginErrors.isEmpty() && loginErrors.get(0).isDisplayed()) {
            throw new AssertionError("Admin login failed: " + loginErrors.get(0).getText().trim());
        }
    }
}
