package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class SchedulingPage extends MeghaConnectModulePage {
    public static final By ADD_EVENT_BUTTON = By.cssSelector("[data-testid='scheduling-add-event-btn'], #scheduling_addEventBtn");
    public static final By ERROR_BANNER = By.cssSelector("[data-testid='scheduling-error-banner'], .error-banner");
    public static final By CONFLICT_DIALOG = By.cssSelector("[data-testid='scheduling-conflict-dialog'], .conflict-dialog");

    public void open() {
        openRelativePath("/scheduling");
    }

    public void clickAddEvent() {
        WebElementUtil.clickWithHighlight(ADD_EVENT_BUTTON);
    }

    public boolean isConflictDialogVisible() {
        return WebElementUtil.isElementDisplayed(CONFLICT_DIALOG);
    }
}
