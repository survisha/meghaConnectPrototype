package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.ReportsPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.junit.Assert.assertTrue;

public class ReportsSteps {
    private final ReportsPage page = new ReportsPage();

    @Given("QA opens Reports module page")
    public void qaOpensReportsModulePage() {
        page.open();
    }

    @Then("QA should see Reports heatmap")
    public void qaShouldSeeReportsHeatmap() {
        assertTrue(page.isHeatmapVisible());
    }
}
