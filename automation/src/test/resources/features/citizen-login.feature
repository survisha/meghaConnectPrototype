@generated @citizen-login @regression
Feature: Citizen Login
  SRS-derived coverage for Citizen Login.

  @citizen @positive @smoke @generated
  Scenario: Registered mobile login
    Given QA prepares "Citizen Login" scenario "Registered mobile login" from SRS requirement "CIT-001"
    When QA executes the "Positive" validation checklist for "Mobile OTP login"
    Then the automation catalog should record expected result "Registered mobile login is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Multiple citizens with same mobile
    Given QA prepares "Citizen Login" scenario "Multiple citizens with same mobile" from SRS requirement "CIT-002"
    When QA executes the "Positive" validation checklist for "Citizen selection"
    Then the automation catalog should record expected result "Multiple citizens with same mobile is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: EPIC citizen selection
    Given QA prepares "Citizen Login" scenario "EPIC citizen selection" from SRS requirement "CIT-003"
    When QA executes the "Positive" validation checklist for "EPIC citizen selection"
    Then the automation catalog should record expected result "EPIC citizen selection is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: No ID citizen selection using visitorId
    Given QA prepares "Citizen Login" scenario "No ID citizen selection using visitorId" from SRS requirement "CIT-004"
    When QA executes the "Positive" validation checklist for "visitorId citizen selection"
    Then the automation catalog should record expected result "No ID citizen selection using visitorId is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Wrong OTP attempt count decrease
    Given QA prepares "Citizen Login" scenario "Wrong OTP attempt count decrease" from SRS requirement "CIT-005"
    When QA executes the "Negative" validation checklist for "OTP validation"
    Then the automation catalog should record expected result "Wrong OTP attempt count decrease is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: OTP lock after max attempts
    Given QA prepares "Citizen Login" scenario "OTP lock after max attempts" from SRS requirement "CIT-006"
    When QA executes the "Negative" validation checklist for "OTP lock"
    Then the automation catalog should record expected result "OTP lock after max attempts is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Change Number clears cache
    Given QA prepares "Citizen Login" scenario "Change Number clears cache" from SRS requirement "CIT-007"
    When QA executes the "Positive" validation checklist for "Change Number"
    Then the automation catalog should record expected result "Change Number clears cache is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @generated
  Scenario: Unregistered mobile shows register option
    Given QA prepares "Citizen Login" scenario "Unregistered mobile shows register option" from SRS requirement "CIT-008"
    When QA executes the "Negative" validation checklist for "Registration redirect"
    Then the automation catalog should record expected result "Unregistered mobile shows register option is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for invalid login mobile
    Given QA prepares "Citizen Login" scenario "Invalid login mobile message" from SRS requirement "CIT-009"
    When QA executes the "Validation" validation checklist for "Login mobile number"
    Then user should see validation message "Mobile number must be 10 digits."
    And the automation catalog should record expected result "Invalid login mobile message is validated successfully."
    And the scenario should capture screenshots on failure
