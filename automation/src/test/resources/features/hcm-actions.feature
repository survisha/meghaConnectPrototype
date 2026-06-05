@generated @hcm-actions @regression
Feature: HCM Actions
  SRS-derived coverage for HCM Actions.

  @positive @smoke @generated
  Scenario: HCM dashboard shows approved scheduled appointments
    Given QA prepares "HCM Actions" scenario "HCM dashboard shows approved scheduled appointments" from SRS requirement "HCM-001"
    When QA executes the "UI" validation checklist for "HCM dashboard"
    Then the automation catalog should record expected result "HCM dashboard shows approved scheduled appointments is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Date filter
    Given QA prepares "HCM Actions" scenario "Date filter" from SRS requirement "HCM-001"
    When QA executes the "UI" validation checklist for "Date filter"
    Then the automation catalog should record expected result "Date filter is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Swipe right accept modify
    Given QA prepares "HCM Actions" scenario "Swipe right accept modify" from SRS requirement "HCM-002"
    When QA executes the "Positive" validation checklist for "Right swipe"
    Then the automation catalog should record expected result "Swipe right accept modify is validated successfully."
    And the scenario should capture screenshots on failure

  @negative @generated
  Scenario: Swipe left reject delay
    Given QA prepares "HCM Actions" scenario "Swipe left reject delay" from SRS requirement "HCM-002"
    When QA executes the "Negative" validation checklist for "Left swipe"
    Then the automation catalog should record expected result "Swipe left reject delay is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Add decision
    Given QA prepares "HCM Actions" scenario "Add decision" from SRS requirement "HCM-003"
    When QA executes the "Positive" validation checklist for "Decision"
    Then the automation catalog should record expected result "Add decision is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Forward to department
    Given QA prepares "HCM Actions" scenario "Forward to department" from SRS requirement "HCM-003"
    When QA executes the "Positive" validation checklist for "Forwarding"
    Then the automation catalog should record expected result "Forward to department is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Save multiple remarks
    Given QA prepares "HCM Actions" scenario "Save multiple remarks" from SRS requirement "HCM-004"
    When QA executes the "Positive" validation checklist for "Remarks"
    Then the automation catalog should record expected result "Save multiple remarks is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Edit saved remarks
    Given QA prepares "HCM Actions" scenario "Edit saved remarks" from SRS requirement "HCM-004"
    When QA executes the "Positive" validation checklist for "Edit remarks"
    Then the automation catalog should record expected result "Edit saved remarks is validated successfully."
    And the scenario should capture screenshots on failure
