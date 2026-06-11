@generated @citizen-dashboard @regression
Feature: Citizen Dashboard
  SRS-derived coverage for Citizen Dashboard.

  @citizen @positive @smoke @generated
  Scenario: Dashboard cards visible
    Given QA prepares "Citizen Dashboard" scenario "Dashboard cards visible" from SRS requirement "DASH-001"
    When QA executes the "UI" validation checklist for "Dashboard cards"
    Then the automation catalog should record expected result "Dashboard cards visible is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Profile details visible
    Given QA prepares "Citizen Dashboard" scenario "Profile details visible" from SRS requirement "DASH-001"
    When QA executes the "UI" validation checklist for "Profile details"
    Then the automation catalog should record expected result "Profile details visible is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Appointment history visible
    Given QA prepares "Citizen Dashboard" scenario "Appointment history visible" from SRS requirement "DASH-002"
    When QA executes the "UI" validation checklist for "Appointment history"
    Then the automation catalog should record expected result "Appointment history visible is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Active schemes visible
    Given QA prepares "Citizen Dashboard" scenario "Active schemes visible" from SRS requirement "DASH-003"
    When QA executes the "UI" validation checklist for "Active schemes"
    Then the automation catalog should record expected result "Active schemes visible is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Grievance count visible
    Given QA prepares "Citizen Dashboard" scenario "Grievance count visible" from SRS requirement "DASH-004"
    When QA executes the "UI" validation checklist for "Grievance count"
    Then the automation catalog should record expected result "Grievance count visible is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @roleBased @generated
  Scenario: Download pass enabled only for eligible status
    Given QA prepares "Citizen Dashboard" scenario "Download pass enabled only for eligible status" from SRS requirement "DASH-002"
    When QA executes the "Role-Based" validation checklist for "Visitor pass"
    Then the automation catalog should record expected result "Download pass enabled only for eligible status is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Raise Grievance opens form
    Given QA prepares "Citizen Dashboard" scenario "Raise Grievance opens form" from SRS requirement "DASH-004"
    When QA executes the "Positive" validation checklist for "Grievance form"
    Then the automation catalog should record expected result "Raise Grievance opens form is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: KYC pending retry panel
    Given QA prepares "Citizen Dashboard" scenario "KYC pending retry panel" from SRS requirement "CIT-004"
    When QA executes the "Positive" validation checklist for "KYC retry"
    Then the automation catalog should record expected result "KYC pending retry panel is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @api @generated
  Scenario: Verify with EPIC updates KYC
    Given QA prepares "Citizen Dashboard" scenario "Verify with EPIC updates KYC" from SRS requirement "CIT-004"
    When QA executes the "Positive" validation checklist for "KYC update"
    Then the automation catalog should record expected result "Verify with EPIC updates KYC is validated successfully."
    And the scenario should capture screenshots on failure
