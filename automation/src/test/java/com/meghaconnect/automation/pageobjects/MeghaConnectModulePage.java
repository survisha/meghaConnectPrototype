package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

/**
 * Generic module page object for SRS-derived tests.
 * Prefer stable Angular IDs/data-testid selectors when module-specific
 * automation is promoted from generated catalog scenarios.
 */
public class MeghaConnectModulePage {

    public void openRelativePath(String path) {
        String baseUrl = com.meghaconnect.automation.config.ConfigManager.getBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        DriverManager.getDriver().get(normalizedBase + normalizedPath);
        WebElementUtil.waitForPageLoad();
    }

    public void clickByTestId(String testId) {
        WebElementUtil.clickWithHighlight(By.cssSelector("[data-testid='" + testId + "'], #" + testId));
    }

    public void typeByTestId(String testId, String value) {
        WebElementUtil.typeWithHighlight(By.cssSelector("[data-testid='" + testId + "'], #" + testId), value);
    }

    public boolean isTextVisible(String text) {
        return WebElementUtil.isElementDisplayed(By.xpath("//*[contains(normalize-space(), '" + text + "')]"));
    }
}
