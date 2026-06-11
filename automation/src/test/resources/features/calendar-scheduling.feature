@generated @calendar-and-schedule @regression
Feature: Calendar and Schedule
  SRS-derived coverage for Calendar and Schedule.

  @positive @smoke @generated
  Scenario: Calendar opens current date
    Given QA prepares "Calendar and Schedule" scenario "Calendar opens current date" from SRS requirement "CAL-001"
    When QA executes the "UI" validation checklist for "Calendar"
    Then the automation catalog should record expected result "Calendar opens current date is validated successfully."
    And the scenario should capture screenshots on failure

  @negative @validation @generated
  Scenario: Past dates disabled or cancelled
    Given QA prepares "Calendar and Schedule" scenario "Past dates disabled or cancelled" from SRS requirement "CAL-001"
    When QA executes the "Validation" validation checklist for "Past date validation"
    Then the automation catalog should record expected result "Past dates disabled or cancelled is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Add Event
    Given QA prepares "Calendar and Schedule" scenario "Add Event" from SRS requirement "CAL-002"
    When QA executes the "Positive" validation checklist for "Add event"
    Then the automation catalog should record expected result "Add Event is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Create Public Darbar
    Given QA prepares "Calendar and Schedule" scenario "Create Public Darbar" from SRS requirement "CAL-002"
    When QA executes the "Positive" validation checklist for "Public Darbar"
    Then the automation catalog should record expected result "Create Public Darbar is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Create Walk-in event
    Given QA prepares "Calendar and Schedule" scenario "Create Walk-in event" from SRS requirement "CAL-002"
    When QA executes the "Positive" validation checklist for "Walk-in event"
    Then the automation catalog should record expected result "Create Walk-in event is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Create Program event
    Given QA prepares "Calendar and Schedule" scenario "Create Program event" from SRS requirement "CAL-002"
    When QA executes the "Positive" validation checklist for "Program event"
    Then the automation catalog should record expected result "Create Program event is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Assign waiting citizens
    Given QA prepares "Calendar and Schedule" scenario "Assign waiting citizens" from SRS requirement "CAL-003"
    When QA executes the "Positive" validation checklist for "Assign waiting citizens"
    Then the automation catalog should record expected result "Assign waiting citizens is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Drag event to new time
    Given QA prepares "Calendar and Schedule" scenario "Drag event to new time" from SRS requirement "CAL-004"
    When QA executes the "Positive" validation checklist for "Drag drop"
    Then the automation catalog should record expected result "Drag event to new time is validated successfully."
    And the scenario should capture screenshots on failure

  @api @positive @generated
  Scenario: Reschedule API sends new time
    Given QA prepares "Calendar and Schedule" scenario "Reschedule API sends new time" from SRS requirement "CAL-004"
    When QA executes the "API" validation checklist for "Reschedule API"
    Then the automation catalog should record expected result "Reschedule API sends new time is validated successfully."
    And the scenario should capture screenshots on failure

  @negative @validation @api @generated
  Scenario: Meeting conflict shows error
    Given QA prepares "Calendar and Schedule" scenario "Meeting conflict shows error" from SRS requirement "CAL-005"
    When QA executes the "Negative" validation checklist for "Meeting conflict"
    Then the automation catalog should record expected result "Meeting conflict shows error is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @api @generated
  Scenario: Google Calendar sync if connected
    Given QA prepares "Calendar and Schedule" scenario "Google Calendar sync if connected" from SRS requirement "CAL-005"
    When QA executes the "Positive" validation checklist for "Google Calendar sync"
    Then the automation catalog should record expected result "Google Calendar sync if connected is validated successfully."
    And the scenario should capture screenshots on failure

  @negative @validation @api @generated
  Scenario: Show validation message for meeting conflict
    Given QA prepares "Calendar and Schedule" scenario "Meeting conflict validation message" from SRS requirement "CAL-005"
    When QA executes the "Validation" validation checklist for "Meeting conflict"
    Then user should see validation message "Meeting Conflict Detected."
    And the automation catalog should record expected result "Meeting conflict validation message is validated successfully."
    And the scenario should capture screenshots on failure
