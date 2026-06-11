package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.DeoWalkinPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class DeoWalkinSteps {
    private final DeoWalkinPage page = new DeoWalkinPage();

    @Given("QA opens DEO Walk-in module page")
    public void qaOpensDeoWalkinModulePage() {
        page.open();
    }

    @When("QA searches DEO walk-in visitor {string}")
    public void qaSearchesDeoWalkinVisitor(String query) {
        page.searchVisitor(query);
    }
}
