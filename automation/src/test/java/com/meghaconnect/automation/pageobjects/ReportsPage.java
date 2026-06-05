package com.meghaconnect.automation.pageobjects;

public class ReportsPage extends MeghaConnectModulePage {
    public void open() {
        openRelativePath("/reports");
    }

    public boolean isHeatmapVisible() {
        return isTextVisible("Heatmap") || isTextVisible("Scheme");
    }
}
