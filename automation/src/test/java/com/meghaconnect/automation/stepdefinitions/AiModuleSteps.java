package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.AiModulePage;
import io.cucumber.java.en.When;

public class AiModuleSteps {
    private final AiModulePage page = new AiModulePage();

    @When("QA opens MeghaBot chat")
    public void qaOpensMeghaBotChat() {
        page.openChat();
    }

    @When("QA asks MeghaBot {string}")
    public void qaAsksMeghaBot(String question) {
        page.ask(question);
    }
}
