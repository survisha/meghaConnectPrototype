package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class PublicIdentificationPage extends MeghaConnectModulePage {
    public static final By PHONE_INPUT = By.cssSelector("[data-testid='public-identification-phone-search-input'], #publicIdentification_phoneInput");
    public static final By EPIC_INPUT = By.cssSelector("[data-testid='public-identification-epic-search-input'], #publicIdentification_epicInput");
    public static final By SEARCH_BUTTON = By.cssSelector("[data-testid='public-identification-search-btn'], #publicIdentification_searchBtn");

    public void open() {
        openRelativePath("/public-identification");
    }

    public void searchByPhone(String phone) {
        WebElementUtil.typeWithHighlight(PHONE_INPUT, phone);
        WebElementUtil.clickWithHighlight(SEARCH_BUTTON);
    }
}
