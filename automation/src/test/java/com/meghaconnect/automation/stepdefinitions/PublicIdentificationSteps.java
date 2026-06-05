package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.PublicIdentificationPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class PublicIdentificationSteps {
    private final PublicIdentificationPage page = new PublicIdentificationPage();

    @Given("QA opens Public Identification module page")
    public void qaOpensPublicIdentificationModulePage() {
        page.open();
    }

    @When("QA searches public identification phone {string}")
    public void qaSearchesPublicIdentificationPhone(String phone) {
        page.searchByPhone(phone);
    }
}
