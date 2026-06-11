package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.AppointmentPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class AppointmentSteps {
    private final AppointmentPage page = new AppointmentPage();

    @Given("QA opens Appointment module page")
    public void qaOpensAppointmentModulePage() {
        page.open();
    }

    @When("QA enters appointment agenda brief {string}")
    public void qaEntersAppointmentAgendaBrief(String value) {
        page.enterAgendaBrief(value);
    }
}
