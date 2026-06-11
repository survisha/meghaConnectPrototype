package com.meghaconnect.automation.pageobjects.locators;

import org.openqa.selenium.By;

public final class AdminPageLocators {
    public static final By HOME_LOGIN_BUTTON = By.xpath("//*[@id='home']/div[2]/div/button");
    public static final By STAFF_LOGIN_TAB = By.xpath("//*[@id='login_staffModeTab']");
    public static final By USERNAME_INPUT = By.xpath("//*[@id='login_usernameInput']");
    public static final By PASSWORD_INPUT = By.xpath("//*[@id='login_passwordInput']");
    public static final By SIGN_IN_BUTTON = By.xpath("//*[@id='login_submitBtn']");
    public static final By LOGIN_ERROR = By.xpath("//*[@id='login_errorMsg']");
    public static final By DASHBOARD_HEADER = By.xpath("//*[@id='shell_header']/div[2]/div/div[1]");
    public static final By LOGOUT_BUTTON = By.xpath("//*[@id='shell_logoutBtn']");

    private AdminPageLocators() {
    }
}
