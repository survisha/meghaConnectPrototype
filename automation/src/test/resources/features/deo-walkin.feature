@generated @deo-module @regression
Feature: DEO Module
  SRS-derived coverage for DEO Module.

  @deo @positive @smoke @generated
  Scenario: DEO login
    Given QA prepares "DEO Module" scenario "DEO login" from SRS requirement "DEO-001"
    When QA executes the "Positive" validation checklist for "DEO login"
    Then the automation catalog should record expected result "DEO login is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @roleBased @generated
  Scenario: Walk-in counter access
    Given QA prepares "DEO Module" scenario "Walk-in counter access" from SRS requirement "DEO-001"
    When QA executes the "Role-Based" validation checklist for "Walk-in counter"
    Then the automation catalog should record expected result "Walk-in counter access is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Search visitor by mobile
    Given QA prepares "DEO Module" scenario "Search visitor by mobile" from SRS requirement "DEO-002"
    When QA executes the "Positive" validation checklist for "Visitor search"
    Then the automation catalog should record expected result "Search visitor by mobile is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Single visitor radio selection
    Given QA prepares "DEO Module" scenario "Single visitor radio selection" from SRS requirement "DEO-002"
    When QA executes the "UI" validation checklist for "Single visitor selection"
    Then the automation catalog should record expected result "Single visitor radio selection is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Multiple visitor radio selection
    Given QA prepares "DEO Module" scenario "Multiple visitor radio selection" from SRS requirement "DEO-002"
    When QA executes the "UI" validation checklist for "Multiple visitor selection"
    Then the automation catalog should record expected result "Multiple visitor radio selection is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Update details
    Given QA prepares "DEO Module" scenario "Update details" from SRS requirement "DEO-003"
    When QA executes the "Positive" validation checklist for "Update visitor"
    Then the automation catalog should record expected result "Update details is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @api @generated
  Scenario: Perform KYC
    Given QA prepares "DEO Module" scenario "Perform KYC" from SRS requirement "DEO-003"
    When QA executes the "Positive" validation checklist for "KYC"
    Then the automation catalog should record expected result "Perform KYC is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Create walk-in appointment
    Given QA prepares "DEO Module" scenario "Create walk-in appointment" from SRS requirement "DEO-004"
    When QA executes the "Positive" validation checklist for "Walk-in appointment"
    Then the automation catalog should record expected result "Create walk-in appointment is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Upload supporting document
    Given QA prepares "DEO Module" scenario "Upload supporting document" from SRS requirement "DEO-004"
    When QA executes the "Positive" validation checklist for "Document upload"
    Then the automation catalog should record expected result "Upload supporting document is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @positive @generated
  Scenario: Capture meeting proof photo
    Given QA prepares "DEO Module" scenario "Capture meeting proof photo" from SRS requirement "DEO-004"
    When QA executes the "Positive" validation checklist for "Meeting proof photo"
    Then the automation catalog should record expected result "Capture meeting proof photo is validated successfully."
    And the scenario should capture screenshots on failure
