package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.CitizenDashboardPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.junit.Assert.assertTrue;

public class CitizenDashboardSteps {
    private final CitizenDashboardPage page = new CitizenDashboardPage();

    @Given("QA opens Citizen Dashboard module page")
    public void qaOpensCitizenDashboardModulePage() {
        page.open();
    }

    @Then("QA should see Citizen Dashboard shell")
    public void qaShouldSeeCitizenDashboardShell() {
        assertTrue(page.isDashboardDisplayed());
    }
}
