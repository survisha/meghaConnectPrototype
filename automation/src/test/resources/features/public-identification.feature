@generated @public-identification @regression
Feature: Public Identification
  SRS-derived coverage for Public Identification.

  @regression @positive @generated
  Scenario: Search by phone
    Given QA prepares "Public Identification" scenario "Search by phone" from SRS requirement "PID-001"
    When QA executes the "Positive" validation checklist for "Phone search"
    Then the automation catalog should record expected result "Search by phone is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Search by EPIC
    Given QA prepares "Public Identification" scenario "Search by EPIC" from SRS requirement "PID-001"
    When QA executes the "Positive" validation checklist for "EPIC search"
    Then the automation catalog should record expected result "Search by EPIC is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Search by name
    Given QA prepares "Public Identification" scenario "Search by name" from SRS requirement "PID-001"
    When QA executes the "Positive" validation checklist for "Name search"
    Then the automation catalog should record expected result "Search by name is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Search by district
    Given QA prepares "Public Identification" scenario "Search by district" from SRS requirement "PID-001"
    When QA executes the "Positive" validation checklist for "District search"
    Then the automation catalog should record expected result "Search by district is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Show profile details
    Given QA prepares "Public Identification" scenario "Show profile details" from SRS requirement "PID-002"
    When QA executes the "UI" validation checklist for "Profile details"
    Then the automation catalog should record expected result "Show profile details is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Show scheme history
    Given QA prepares "Public Identification" scenario "Show scheme history" from SRS requirement "PID-002"
    When QA executes the "UI" validation checklist for "Scheme history"
    Then the automation catalog should record expected result "Show scheme history is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Show appointment history
    Given QA prepares "Public Identification" scenario "Show appointment history" from SRS requirement "PID-002"
    When QA executes the "UI" validation checklist for "Appointment history"
    Then the automation catalog should record expected result "Show appointment history is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Show lastVisitedAt
    Given QA prepares "Public Identification" scenario "Show lastVisitedAt" from SRS requirement "PID-003"
    When QA executes the "UI" validation checklist for "Last visited"
    Then the automation catalog should record expected result "Show lastVisitedAt is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Show SCHEDULED appointment under Upcoming Appointment
    Given QA prepares "Public Identification" scenario "Show SCHEDULED appointment under Upcoming Appointment" from SRS requirement "PID-003"
    When QA executes the "UI" validation checklist for "Upcoming appointment"
    Then the automation catalog should record expected result "Show SCHEDULED appointment under Upcoming Appointment is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @positive @generated
  Scenario: Photo loads from photoStoragePath or photoUrl
    Given QA prepares "Public Identification" scenario "Photo loads from photoStoragePath or photoUrl" from SRS requirement "PID-003"
    When QA executes the "UI" validation checklist for "Photo load"
    Then the automation catalog should record expected result "Photo loads from photoStoragePath or photoUrl is validated successfully."
    And the scenario should capture screenshots on failure

  @regression @negative @generated
  Scenario: Default avatar on image failure
    Given QA prepares "Public Identification" scenario "Default avatar on image failure" from SRS requirement "PID-003"
    When QA executes the "Negative" validation checklist for "Photo fallback"
    Then the automation catalog should record expected result "Default avatar on image failure is validated successfully."
    And the scenario should capture screenshots on failure
