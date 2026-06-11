package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.SchedulingPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.assertTrue;

public class SchedulingSteps {
    private final SchedulingPage page = new SchedulingPage();

    @Given("QA opens Scheduling module page")
    public void qaOpensSchedulingModulePage() {
        page.open();
    }

    @When("QA clicks Scheduling Add Event")
    public void qaClicksSchedulingAddEvent() {
        page.clickAddEvent();
    }

    @Then("QA should see Scheduling conflict dialog")
    public void qaShouldSeeSchedulingConflictDialog() {
        assertTrue(page.isConflictDialogVisible());
    }
}
