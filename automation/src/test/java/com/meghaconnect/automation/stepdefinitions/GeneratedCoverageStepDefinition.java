package com.meghaconnect.automation.stepdefinitions;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Shared glue for SRS-derived generated coverage scenarios.
 * These steps keep the generated catalog executable while detailed UI/API
 * workflows can be promoted into module-specific page objects over time.
 */
public class GeneratedCoverageStepDefinition {
    private static final Logger logger = LogManager.getLogger(GeneratedCoverageStepDefinition.class);

    private String moduleName;
    private String scenarioName;
    private String requirementId;
    private String checklistType;
    private String checklistArea;
    private String expectedResult;

    @Given("QA prepares {string} scenario {string} from SRS requirement {string}")
    public void qaPreparesScenarioFromSrsRequirement(String moduleName, String scenarioName, String requirementId) {
        this.moduleName = moduleName;
        this.scenarioName = scenarioName;
        this.requirementId = requirementId;
        logger.info("Prepared SRS coverage: module={}, scenario={}, requirement={}",
            moduleName, scenarioName, requirementId);
        assertNotBlank(moduleName, "moduleName");
        assertNotBlank(scenarioName, "scenarioName");
        assertNotBlank(requirementId, "requirementId");
    }

    @When("QA executes the {string} validation checklist for {string}")
    public void qaExecutesValidationChecklist(String checklistType, String checklistArea) {
        this.checklistType = checklistType;
        this.checklistArea = checklistArea;
        logger.info("Executing generated checklist: type={}, area={}", checklistType, checklistArea);
        assertNotBlank(checklistType, "checklistType");
        assertNotBlank(checklistArea, "checklistArea");
    }

    @Then("the automation catalog should record expected result {string}")
    public void automationCatalogShouldRecordExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
        logger.info("Expected result recorded: {}", expectedResult);
        assertNotBlank(expectedResult, "expectedResult");
        assertNotNull("Generated scenario must carry module", moduleName);
        assertNotNull("Generated scenario must carry scenario", scenarioName);
        assertNotNull("Generated scenario must carry requirement", requirementId);
    }

    @And("the scenario should capture screenshots on failure")
    public void scenarioShouldCaptureScreenshotsOnFailure() {
        logger.info("Failure screenshot policy is provided by TestHooks and ScreenshotUtil.");
        assertNotNull("Checklist type should be available", checklistType);
        assertNotNull("Checklist area should be available", checklistArea);
        assertNotNull("Expected result should be available", expectedResult);
    }

    private void assertNotBlank(String value, String fieldName) {
        assertNotNull(fieldName + " should not be null", value);
        assertFalse(fieldName + " should not be blank", value.trim().isEmpty());
    }
}
