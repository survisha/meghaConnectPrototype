package com.meghaconnect.automation.pageobjects;

public class CitizenDashboardPage extends MeghaConnectModulePage {
    public void open() {
        openRelativePath("/visitor-dashboard");
    }

    public boolean isDashboardDisplayed() {
        return isTextVisible("Visitor Profile") || isTextVisible("Dashboard");
    }
}
