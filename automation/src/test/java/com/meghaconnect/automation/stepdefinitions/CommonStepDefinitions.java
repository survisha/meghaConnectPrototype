package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.MessageCenterPage;
import io.cucumber.java.en.Then;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommonStepDefinitions {
    private final MessageCenterPage messageCenterPage = new MessageCenterPage();

    @Then("user should see validation message {string}")
    public void userShouldSeeValidationMessage(String expectedMessage) {
        assertTrue("Expected validation message not found: " + expectedMessage,
            messageCenterPage.waitForMessage(expectedMessage));
    }

    @Then("user should not see validation message {string}")
    public void userShouldNotSeeValidationMessage(String expectedMessage) {
        assertTrue("Unexpected validation message found: " + expectedMessage,
            messageCenterPage.isMessageAbsent(expectedMessage));
    }

    @Then("user should see message containing {string}")
    public void userShouldSeeMessageContaining(String expectedMessage) {
        assertTrue("Expected message not found: " + expectedMessage,
            messageCenterPage.waitForMessage(expectedMessage));
    }

    @Then("user should not see message containing {string}")
    public void userShouldNotSeeMessageContaining(String expectedMessage) {
        assertFalse("Unexpected message found: " + expectedMessage,
            messageCenterPage.isMessageVisible(expectedMessage));
    }
}
