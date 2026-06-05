package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.SchemeApplicationPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class SchemeApplicationSteps {
    private final SchemeApplicationPage page = new SchemeApplicationPage();

    @Given("QA opens Scheme Application module page")
    public void qaOpensSchemeApplicationModulePage() {
        page.open();
    }

    @When("QA enters scheme project name {string}")
    public void qaEntersSchemeProjectName(String value) {
        page.enterProjectName(value);
    }
}
