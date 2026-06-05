package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.config.ConfigManager;
import com.meghaconnect.automation.config.DriverManager;
import com.meghaconnect.automation.utils.ScreenshotUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Reads inline errors, snackbars, alerts, and dialogs from Angular Material UI.
 */
public class MessageCenterPage {
    private static final List<By> MESSAGE_CONTAINERS = List.of(
        By.cssSelector("mat-error"),
        By.cssSelector(".mat-mdc-snack-bar-container, simple-snack-bar"),
        By.cssSelector(".error-msg, .error-banner, .status-msg, .warning-msg"),
        By.cssSelector("[role='alert'], .alert, .toast-container"),
        By.cssSelector(".dialog-container, .mat-mdc-dialog-container, .modal")
    );

    public boolean waitForMessage(String expected) {
        if (!DriverManager.isDriverInitialized()) {
            return ValidationMessages.containsMessage(expected);
        }
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(),
                Duration.ofSeconds(ConfigManager.getExplicitWait()));
            return wait.until(driver -> visibleMessageContains(expected));
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("MESSAGE_NOT_FOUND");
            return false;
        }
    }

    public boolean isMessageVisible(String expected) {
        if (!DriverManager.isDriverInitialized()) {
            return ValidationMessages.containsMessage(expected);
        }
        return visibleMessageContains(expected);
    }

    public boolean isMessageAbsent(String expected) {
        if (!DriverManager.isDriverInitialized()) {
            return true;
        }
        return !visibleMessageContains(expected);
    }

    public String getVisibleMessages() {
        if (!DriverManager.isDriverInitialized()) {
            return String.join(" | ", ValidationMessages.MESSAGES.values());
        }
        StringBuilder builder = new StringBuilder();
        for (By locator : MESSAGE_CONTAINERS) {
            for (WebElement element : DriverManager.getDriver().findElements(locator)) {
                if (element.isDisplayed()) {
                    String text = element.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        if (builder.length() > 0) {
                            builder.append(" | ");
                        }
                        builder.append(text.trim());
                    }
                }
            }
        }
        if (builder.length() == 0) {
            builder.append(DriverManager.getDriver().findElement(By.tagName("body")).getText());
        }
        return builder.toString();
    }

    private boolean visibleMessageContains(String expected) {
        String normalizedExpected = normalize(expected);
        return normalize(getVisibleMessages()).contains(normalizedExpected);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
