@generated @citizen-appointment-creation @regression
Feature: Citizen Appointment Creation
  SRS-derived coverage for Citizen Appointment Creation.

  @citizen @positive @smoke @generated
  Scenario: Create appointment with valid details
    Given QA prepares "Citizen Appointment Creation" scenario "Create appointment with valid details" from SRS requirement "APT-001"
    When QA executes the "Positive" validation checklist for "Appointment creation"
    Then the automation catalog should record expected result "Create appointment with valid details is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Scheme agenda enables scheme form
    Given QA prepares "Citizen Appointment Creation" scenario "Scheme agenda enables scheme form" from SRS requirement "APT-002"
    When QA executes the "Positive" validation checklist for "Scheme agenda"
    Then the automation catalog should record expected result "Scheme agenda enables scheme form is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Add associate visitor
    Given QA prepares "Citizen Appointment Creation" scenario "Add associate visitor" from SRS requirement "APT-003"
    When QA executes the "Positive" validation checklist for "Associate visitor"
    Then the automation catalog should record expected result "Add associate visitor is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Associate visitor must be registered
    Given QA prepares "Citizen Appointment Creation" scenario "Associate visitor must be registered" from SRS requirement "APT-003"
    When QA executes the "Validation" validation checklist for "Associate validation"
    Then the automation catalog should record expected result "Associate visitor must be registered is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Upload supporting document
    Given QA prepares "Citizen Appointment Creation" scenario "Upload supporting document" from SRS requirement "APT-004"
    When QA executes the "Positive" validation checklist for "Document upload"
    Then the automation catalog should record expected result "Upload supporting document is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Review and submit
    Given QA prepares "Citizen Appointment Creation" scenario "Review and submit" from SRS requirement "APT-005"
    When QA executes the "Positive" validation checklist for "Review submit"
    Then the automation catalog should record expected result "Review and submit is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Application number generated
    Given QA prepares "Citizen Appointment Creation" scenario "Application number generated" from SRS requirement "APT-006"
    When QA executes the "Positive" validation checklist for "Application number"
    Then the automation catalog should record expected result "Application number generated is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Missing agenda
    Given QA prepares "Citizen Appointment Creation" scenario "Missing agenda" from SRS requirement "APT-002"
    When QA executes the "Validation" validation checklist for "Agenda validation"
    Then the automation catalog should record expected result "Missing agenda is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Invalid associate
    Given QA prepares "Citizen Appointment Creation" scenario "Invalid associate" from SRS requirement "APT-003"
    When QA executes the "Validation" validation checklist for "Associate validation"
    Then the automation catalog should record expected result "Invalid associate is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Unsupported document
    Given QA prepares "Citizen Appointment Creation" scenario "Unsupported document" from SRS requirement "APT-004"
    When QA executes the "Validation" validation checklist for "File upload validation"
    Then the automation catalog should record expected result "Unsupported document is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Submit without required fields
    Given QA prepares "Citizen Appointment Creation" scenario "Submit without required fields" from SRS requirement "APT-005"
    When QA executes the "Validation" validation checklist for "Required fields"
    Then the automation catalog should record expected result "Submit without required fields is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for missing appointment agenda
    Given QA prepares "Citizen Appointment Creation" scenario "Missing appointment agenda message" from SRS requirement "APT-002"
    When QA executes the "Validation" validation checklist for "Appointment agenda"
    Then user should see validation message "Please complete the appointment agenda, location, and purpose before submitting."
    And the automation catalog should record expected result "Missing appointment agenda message is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for unregistered associate visitor
    Given QA prepares "Citizen Appointment Creation" scenario "Unregistered associate message" from SRS requirement "APT-003"
    When QA executes the "Validation" validation checklist for "Associate visitor"
    Then user should see validation message "Citizen must register in the portal before being added as an associate visitor."
    And the automation catalog should record expected result "Unregistered associate message is validated successfully."
    And the scenario should capture screenshots on failure
