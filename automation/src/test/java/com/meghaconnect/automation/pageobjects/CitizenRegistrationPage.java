package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class CitizenRegistrationPage extends MeghaConnectModulePage {
    public static final By EPIC_INPUT = By.cssSelector("[data-testid='citizen-registration-epic-input'], #register_epicNumberInput");
    public static final By NAME_INPUT = By.cssSelector("[data-testid='citizen-registration-name-input']");
    public static final By MOBILE_INPUT = By.cssSelector("[data-testid='citizen-registration-mobile-input']");
    public static final By GENERATE_OTP_BUTTON = By.cssSelector("[data-testid='citizen-registration-generate-otp-btn']");
    public static final By OTP_INPUT = By.cssSelector("[data-testid='citizen-registration-otp-input']");
    public static final By SUBMIT_BUTTON = By.cssSelector("[data-testid='citizen-registration-submit-btn']");

    public void open() {
        openRelativePath("/visitor-register");
    }

    public void enterEpic(String epic) {
        WebElementUtil.typeWithHighlight(EPIC_INPUT, epic);
    }

    public void enterName(String name) {
        WebElementUtil.typeWithHighlight(NAME_INPUT, name);
    }

    public void enterMobileNumber(String mobile) {
        WebElementUtil.typeWithHighlight(MOBILE_INPUT, mobile);
    }

    public void clickGenerateOtp() {
        WebElementUtil.clickWithHighlight(GENERATE_OTP_BUTTON);
    }
}
