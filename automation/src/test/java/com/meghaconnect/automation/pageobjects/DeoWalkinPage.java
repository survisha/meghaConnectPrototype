package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class DeoWalkinPage extends MeghaConnectModulePage {
    public static final By SEARCH_INPUT = By.cssSelector("[data-testid='deo-walkin-search-input'], #walkin_searchInput");
    public static final By SEARCH_BUTTON = By.cssSelector("[data-testid='deo-walkin-search-btn'], #walkin_searchBtn");

    public void open() {
        openRelativePath("/walkin");
    }

    public void searchVisitor(String value) {
        WebElementUtil.typeWithHighlight(SEARCH_INPUT, value);
        WebElementUtil.clickWithHighlight(SEARCH_BUTTON);
    }
}
