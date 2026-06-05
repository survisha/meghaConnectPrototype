@generated @admin-module @regression
Feature: Admin Module
  SRS-derived coverage for Admin Module.

  @admin @positive @smoke @generated
  Scenario: Admin login
    Given QA prepares "Admin Module" scenario "Admin login" from SRS requirement "ADM-001"
    When QA executes the "Positive" validation checklist for "Admin login"
    Then the automation catalog should record expected result "Admin login is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: User management create
    Given QA prepares "Admin Module" scenario "User management create" from SRS requirement "ADM-001"
    When QA executes the "Positive" validation checklist for "Create user"
    Then the automation catalog should record expected result "User management create is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: User management edit
    Given QA prepares "Admin Module" scenario "User management edit" from SRS requirement "ADM-001"
    When QA executes the "Positive" validation checklist for "Edit user"
    Then the automation catalog should record expected result "User management edit is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @negative @generated
  Scenario: User management delete
    Given QA prepares "Admin Module" scenario "User management delete" from SRS requirement "ADM-001"
    When QA executes the "Negative" validation checklist for "Delete user"
    Then the automation catalog should record expected result "User management delete is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @negative @validation @generated
  Scenario: Duplicate username validation
    Given QA prepares "Admin Module" scenario "Duplicate username validation" from SRS requirement "ADM-002"
    When QA executes the "Validation" validation checklist for "Duplicate username"
    Then the automation catalog should record expected result "Duplicate username validation is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: Activate deactivate user
    Given QA prepares "Admin Module" scenario "Activate deactivate user" from SRS requirement "ADM-002"
    When QA executes the "Positive" validation checklist for "User status"
    Then the automation catalog should record expected result "Activate deactivate user is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: Unlock user
    Given QA prepares "Admin Module" scenario "Unlock user" from SRS requirement "ADM-003"
    When QA executes the "Positive" validation checklist for "Unlock user"
    Then the automation catalog should record expected result "Unlock user is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: Scheme management add edit activate inactivate
    Given QA prepares "Admin Module" scenario "Scheme management add edit activate inactivate" from SRS requirement "ADM-004"
    When QA executes the "Positive" validation checklist for "Scheme management"
    Then the automation catalog should record expected result "Scheme management add edit activate inactivate is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: Configure required documents
    Given QA prepares "Admin Module" scenario "Configure required documents" from SRS requirement "ADM-004"
    When QA executes the "Positive" validation checklist for "Documents configuration"
    Then the automation catalog should record expected result "Configure required documents is validated successfully."
    And the scenario should capture screenshots on failure

  @admin @positive @generated
  Scenario: Appointment type configuration
    Given QA prepares "Admin Module" scenario "Appointment type configuration" from SRS requirement "ADM-005"
    When QA executes the "Positive" validation checklist for "Appointment type configuration"
    Then the automation catalog should record expected result "Appointment type configuration is validated successfully."
    And the scenario should capture screenshots on failure
