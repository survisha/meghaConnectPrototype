@generated @cm-scheme-applications @regression
Feature: CM Scheme Applications
  SRS-derived coverage for CM Scheme Applications.

  @positive @generated
  Scenario: Show statistics cards
    Given QA prepares "CM Scheme Applications" scenario "Show statistics cards" from SRS requirement "SCH-001"
    When QA executes the "UI" validation checklist for "Statistics"
    Then the automation catalog should record expected result "Show statistics cards is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Filter scheme application list
    Given QA prepares "CM Scheme Applications" scenario "Filter scheme application list" from SRS requirement "SCH-002"
    When QA executes the "UI" validation checklist for "Filters"
    Then the automation catalog should record expected result "Filter scheme application list is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Sort scheme application list
    Given QA prepares "CM Scheme Applications" scenario "Sort scheme application list" from SRS requirement "SCH-002"
    When QA executes the "UI" validation checklist for "Sorting"
    Then the automation catalog should record expected result "Sort scheme application list is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: View scheme application details
    Given QA prepares "CM Scheme Applications" scenario "View scheme application details" from SRS requirement "SCH-003"
    When QA executes the "UI" validation checklist for "Details"
    Then the automation catalog should record expected result "View scheme application details is validated successfully."
    And the scenario should capture screenshots on failure

  @validation @generated
  Scenario: Validate approved rejected pending counts
    Given QA prepares "CM Scheme Applications" scenario "Validate approved rejected pending counts" from SRS requirement "SCH-003"
    When QA executes the "Validation" validation checklist for "Counts"
    Then the automation catalog should record expected result "Validate approved rejected pending counts is validated successfully."
    And the scenario should capture screenshots on failure
