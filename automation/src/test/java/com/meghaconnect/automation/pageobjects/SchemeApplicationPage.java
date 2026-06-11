package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class SchemeApplicationPage extends MeghaConnectModulePage {
    public static final By SCHEME_TYPE_SELECT = By.cssSelector("[data-testid='scheme-application-scheme-type-select'], #scheme_schemeTypeSelect");
    public static final By PROJECT_NAME_INPUT = By.cssSelector("[data-testid='scheme-application-project-name-input'], #scheme_projectNameInput");

    public void open() {
        openRelativePath("/schemes/apply");
    }

    public void enterProjectName(String value) {
        WebElementUtil.typeWithHighlight(PROJECT_NAME_INPUT, value);
    }
}
