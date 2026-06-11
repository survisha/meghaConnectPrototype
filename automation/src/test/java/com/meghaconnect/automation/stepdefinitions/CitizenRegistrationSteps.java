package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.CitizenRegistrationPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class CitizenRegistrationSteps {
    private final CitizenRegistrationPage page = new CitizenRegistrationPage();

    @Given("QA opens Citizen Registration module page")
    public void qaOpensCitizenRegistrationModulePage() {
        page.open();
    }

    @When("QA enters citizen registration EPIC {string}")
    public void qaEntersCitizenRegistrationEpic(String epic) {
        page.enterEpic(epic);
    }

    @When("QA enters citizen registration mobile {string}")
    public void qaEntersCitizenRegistrationMobile(String mobile) {
        page.enterMobileNumber(mobile);
    }
}
