package com.meghaconnect.automation.pageobjects.locators;

import org.openqa.selenium.By;

/**
 * Login Page Locators - Centralized XPath and CSS selectors
 * Separation of concerns: All UI element locators in one place
 * Easy to maintain: Update locators without touching page object logic
 */
public class LoginPageLocators {

    // ==================== INPUT FIELDS ====================
    public static final By USERNAME_INPUT = By.xpath(
            "//input[@name='username' or @id='username' or @placeholder='Username']"
    );

    public static final By PASSWORD_INPUT = By.xpath(
            "//input[@name='password' or @id='password' or @type='password']"
    );

    public static final By EMAIL_INPUT = By.xpath(
            "//input[@name='email' or @id='email' or @placeholder='Email']"
    );

    // ==================== BUTTONS ====================
    public static final By LOGIN_BUTTON = By.xpath(
            "//button[contains(text(), 'Login') or contains(text(), 'Sign In') or contains(text(), 'login')]"
    );

    public static final By FORGOT_PASSWORD_BUTTON = By.xpath(
            "//button[contains(text(), 'Forgot') or contains(text(), 'Reset')]"
    );

    public static final By LOGOUT_BUTTON = By.xpath(
            "//button[contains(text(), 'Logout') or contains(text(), 'Log Out') or contains(text(), 'Sign Out')]"
    );

    public static final By REMEMBER_ME_BUTTON = By.xpath(
            "//button[contains(text(), 'Remember')]"
    );

    // ==================== LINKS ====================
    public static final By FORGOT_PASSWORD_LINK = By.xpath(
            "//a[contains(text(), 'Forgot') or contains(text(), 'forgot') or contains(text(), 'Reset')]"
    );

    public static final By SIGNUP_LINK = By.xpath(
            "//a[contains(text(), 'Sign up') or contains(text(), 'register') or contains(text(), 'Register')]"
    );

    public static final By BACK_TO_LOGIN_LINK = By.xpath(
            "//a[contains(text(), 'Back') or contains(text(), 'login')]"
    );

    // ==================== CHECKBOXES ====================
    public static final By REMEMBER_ME_CHECKBOX = By.xpath(
            "//input[@type='checkbox']"
    );

    public static final By TERMS_CHECKBOX = By.xpath(
            "//input[@type='checkbox' and contains(@name, 'terms') or contains(@id, 'terms')]"
    );

    // ==================== MESSAGES & ALERTS ====================
    public static final By ERROR_MESSAGE = By.xpath(
            "//div[contains(@class, 'error') or contains(@class, 'alert-danger') or contains(@class, 'alert-error') or @role='alert']" +
            " | //mat-error" +
            " | //p[contains(@class, 'error')]"
    );

    public static final By SUCCESS_MESSAGE = By.xpath(
            "//div[contains(@class, 'success') or contains(@class, 'alert-success')] | //mat-card-header[contains(text(), 'success')]"
    );

    public static final By WARNING_MESSAGE = By.xpath(
            "//div[contains(@class, 'warning') or contains(@class, 'alert-warning')]"
    );

    public static final By INFO_MESSAGE = By.xpath(
            "//div[contains(@class, 'info') or contains(@class, 'alert-info')]"
    );

    public static final By VALIDATION_ERROR = By.xpath(
            "//mat-form-field//mat-error | //span[contains(@class, 'error')]"
    );

    // ==================== LOADING & STATUS ====================
    public static final By LOADING_SPINNER = By.xpath(
            "//div[contains(@class, 'spinner') or contains(@class, 'loader') or contains(@class, 'progress')] | //mat-progress-spinner | //mat-progress-bar"
    );

    public static final By LOADING_OVERLAY = By.xpath(
            "//div[contains(@class, 'overlay') or contains(@class, 'backdrop')]"
    );

    // ==================== HEADERS & TITLES ====================
    public static final By PAGE_TITLE = By.xpath(
            "//h1[contains(text(), 'Login') or contains(text(), 'Sign In')] | //mat-card-title"
    );

    public static final By PAGE_SUBTITLE = By.xpath(
            "//h2 | //p[contains(@class, 'subtitle')]"
    );

    // ==================== FORM CONTAINERS ====================
    public static final By LOGIN_FORM = By.xpath(
            "//form | //div[contains(@class, 'login-form') or contains(@class, 'login-container')]"
    );

    public static final By LOGIN_CARD = By.xpath(
            "//mat-card | //div[contains(@class, 'card') and contains(@class, 'login')]"
    );

    // ==================== LOGOS & IMAGES ====================
    public static final By COMPANY_LOGO = By.xpath(
            "//img[contains(@alt, 'logo') or contains(@class, 'logo')] | //img[contains(@src, 'logo')]"
    );

    // ==================== HELP & SUPPORT ====================
    public static final By HELP_LINK = By.xpath(
            "//a[contains(text(), 'Help') or contains(text(), 'Support')]"
    );

    public static final By CONTACT_LINK = By.xpath(
            "//a[contains(text(), 'Contact')]"
    );

    // ==================== FOOTER ====================
    public static final By FOOTER = By.xpath(
            "//footer | //div[contains(@class, 'footer')]"
    );

    public static final By PRIVACY_LINK = By.xpath(
            "//a[contains(text(), 'Privacy')]"
    );

    public static final By TERMS_LINK = By.xpath(
            "//a[contains(text(), 'Terms')]"
    );

    // ==================== MATERIAL DESIGN COMPONENTS ====================
    public static final By MAT_FORM_FIELD = By.xpath(
            "//mat-form-field"
    );

    public static final By MAT_INPUT = By.xpath(
            "//input[contains(@matInput, '') or @matinput]"
    );

    public static final By MAT_BUTTON = By.xpath(
            "//button[contains(@mat-raised-button, '') or contains(@mat-stroked-button, '') or contains(@mat-button, '')]"
    );

    public static final By MAT_ERROR = By.xpath(
            "//mat-error"
    );

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Get input field by label text (Material Design)
     * @param labelText Label text
     * @return By locator
     */
    public static By getInputByLabel(String labelText) {
        return By.xpath("//mat-form-field/mat-label[contains(text(), '" + labelText + "')]/ancestor::mat-form-field//input");
    }

    /**
     * Get button by button text
     * @param buttonText Button text
     * @return By locator
     */
    public static By getButtonByText(String buttonText) {
        return By.xpath("//button[contains(text(), '" + buttonText + "')]");
    }

    /**
     * Get link by link text
     * @param linkText Link text
     * @return By locator
     */
    public static By getLinkByText(String linkText) {
        return By.xpath("//a[contains(text(), '" + linkText + "')]");
    }

    /**
     * Get element by text content
     * @param elementTag HTML tag (div, span, p, etc.)
     * @param text Text content
     * @return By locator
     */
    public static By getElementByText(String elementTag, String text) {
        return By.xpath("//" + elementTag + "[contains(text(), '" + text + "')]");
    }

    /**
     * Get element by partial class name
     * @param className Class name (partial match)
     * @return By locator
     */
    public static By getElementByClass(String className) {
        return By.xpath("//*[contains(@class, '" + className + "')]");
    }

    /**
     * Get element by ID
     * @param id Element ID
     * @return By locator
     */
    public static By getElementById(String id) {
        return By.id(id);
    }

    /**
     * Get element by name attribute
     * @param name Element name
     * @return By locator
     */
    public static By getElementByName(String name) {
        return By.name(name);
    }

    /**
     * Get element by CSS class
     * @param cssClass CSS class name
     * @return By locator
     */
    public static By getElementByCSS(String cssClass) {
        return By.cssSelector("." + cssClass);
    }
}
