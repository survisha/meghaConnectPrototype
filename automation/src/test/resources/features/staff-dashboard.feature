@generated @staff-dashboard @regression
Feature: Staff Dashboard
  SRS-derived coverage for Staff Dashboard.

  @roleBased @positive @generated
  Scenario: Role-based dashboard cards
    Given QA prepares "Staff Dashboard" scenario "Role-based dashboard cards" from SRS requirement "DASH-001"
    When QA executes the "Role-Based" validation checklist for "Dashboard cards"
    Then the automation catalog should record expected result "Role-based dashboard cards is validated successfully."
    And the scenario should capture screenshots on failure

  @deo @roleBased @generated
  Scenario: DEO quick actions
    Given QA prepares "Staff Dashboard" scenario "DEO quick actions" from SRS requirement "DASH-001"
    When QA executes the "Role-Based" validation checklist for "DEO dashboard"
    Then the automation catalog should record expected result "DEO quick actions is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: CMO dashboard widgets
    Given QA prepares "Staff Dashboard" scenario "CMO dashboard widgets" from SRS requirement "DASH-002"
    When QA executes the "Role-Based" validation checklist for "CMO dashboard"
    Then the automation catalog should record expected result "CMO dashboard widgets is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: Approver dashboard widgets
    Given QA prepares "Staff Dashboard" scenario "Approver dashboard widgets" from SRS requirement "DASH-002"
    When QA executes the "Role-Based" validation checklist for "Approver dashboard"
    Then the automation catalog should record expected result "Approver dashboard widgets is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: HCM dashboard widgets
    Given QA prepares "Staff Dashboard" scenario "HCM dashboard widgets" from SRS requirement "DASH-003"
    When QA executes the "Role-Based" validation checklist for "HCM dashboard"
    Then the automation catalog should record expected result "HCM dashboard widgets is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: OSD dashboard widgets
    Given QA prepares "Staff Dashboard" scenario "OSD dashboard widgets" from SRS requirement "DASH-003"
    When QA executes the "Role-Based" validation checklist for "OSD dashboard"
    Then the automation catalog should record expected result "OSD dashboard widgets is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Recent activity visible
    Given QA prepares "Staff Dashboard" scenario "Recent activity visible" from SRS requirement "DASH-004"
    When QA executes the "UI" validation checklist for "Recent activity"
    Then the automation catalog should record expected result "Recent activity visible is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @positive @generated
  Scenario: AI insights visible for authorized roles
    Given QA prepares "Staff Dashboard" scenario "AI insights visible for authorized roles" from SRS requirement "AI-006"
    When QA executes the "Role-Based" validation checklist for "AI insights"
    Then the automation catalog should record expected result "AI insights visible for authorized roles is validated successfully."
    And the scenario should capture screenshots on failure
