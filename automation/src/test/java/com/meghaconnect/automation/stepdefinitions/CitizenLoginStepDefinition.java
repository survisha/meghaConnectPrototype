package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.flows.CitizenLoginFlow;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.Assert.assertTrue;

/**
 * Cucumber steps for Citizen mobile OTP login.
 */
public class CitizenLoginStepDefinition {
    private static final Logger logger = LogManager.getLogger(CitizenLoginStepDefinition.class);

    private final CitizenLoginFlow citizenLoginFlow;

    public CitizenLoginStepDefinition() {
        this.citizenLoginFlow = new CitizenLoginFlow();
    }

    @Given("Citizen opens MeghaConnect UAT home page")
    public void citizenOpensMeghaConnectUatHomePage() {
        logger.info("GIVEN: Citizen opens MeghaConnect UAT home page");
        citizenLoginFlow.openHomePage();
    }

    @When("Citizen completes OTP login using scenario {string}")
    public void citizenCompletesOtpLoginUsingScenario(String scenarioName) {
        logger.info("WHEN: Citizen completes OTP login using scenario: " + scenarioName);
        citizenLoginFlow.executeCitizenOtpLogin(scenarioName);
    }

    @Then("Citizen should be redirected to visitor dashboard")
    public void citizenShouldBeRedirectedToVisitorDashboard() {
        logger.info("THEN: Citizen should be redirected to visitor dashboard");
        assertTrue("Visitor Profile header should be visible", citizenLoginFlow.isDashboardVisible());
        citizenLoginFlow.getDashboardHeaderText();
    }

    @And("Citizen logs out successfully")
    public void citizenLogsOutSuccessfully() {
        logger.info("AND: Citizen logs out successfully");
        citizenLoginFlow.logout();
    }
}
