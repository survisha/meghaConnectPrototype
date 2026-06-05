@generated @all-appointments @regression
Feature: All Appointments
  SRS-derived coverage for All Appointments.

  @positive @generated
  Scenario: Filter by name status type date
    Given QA prepares "All Appointments" scenario "Filter by name status type date" from SRS requirement "APT-001"
    When QA executes the "UI" validation checklist for "Filters"
    Then the automation catalog should record expected result "Filter by name status type date is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Sorting grid
    Given QA prepares "All Appointments" scenario "Sorting grid" from SRS requirement "APT-001"
    When QA executes the "UI" validation checklist for "Sorting"
    Then the automation catalog should record expected result "Sorting grid is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: View appointment details
    Given QA prepares "All Appointments" scenario "View appointment details" from SRS requirement "APT-002"
    When QA executes the "UI" validation checklist for "Details"
    Then the automation catalog should record expected result "View appointment details is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: View personal info and photo
    Given QA prepares "All Appointments" scenario "View personal info and photo" from SRS requirement "APT-002"
    When QA executes the "UI" validation checklist for "Personal info"
    Then the automation catalog should record expected result "View personal info and photo is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: View associates
    Given QA prepares "All Appointments" scenario "View associates" from SRS requirement "APT-003"
    When QA executes the "UI" validation checklist for "Associates"
    Then the automation catalog should record expected result "View associates is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: View download upload documents
    Given QA prepares "All Appointments" scenario "View download upload documents" from SRS requirement "APT-004"
    When QA executes the "UI" validation checklist for "Documents"
    Then the automation catalog should record expected result "View download upload documents is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: Capture meeting proof photo
    Given QA prepares "All Appointments" scenario "Capture meeting proof photo" from SRS requirement "APT-004"
    When QA executes the "Positive" validation checklist for "Meeting proof"
    Then the automation catalog should record expected result "Capture meeting proof photo is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: AI Notes View
    Given QA prepares "All Appointments" scenario "AI Notes View" from SRS requirement "AI-001"
    When QA executes the "UI" validation checklist for "AI notes"
    Then the automation catalog should record expected result "AI Notes View is validated successfully."
    And the scenario should capture screenshots on failure

  @api @positive @generated
  Scenario: AI Notes Refresh
    Given QA prepares "All Appointments" scenario "AI Notes Refresh" from SRS requirement "AI-001"
    When QA executes the "API" validation checklist for "AI notes refresh"
    Then the automation catalog should record expected result "AI Notes Refresh is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: CMO request missing info
    Given QA prepares "All Appointments" scenario "CMO request missing info" from SRS requirement "APT-005"
    When QA executes the "Role-Based" validation checklist for "CMO workflow"
    Then the automation catalog should record expected result "CMO request missing info is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: CMO edit category
    Given QA prepares "All Appointments" scenario "CMO edit category" from SRS requirement "APT-005"
    When QA executes the "Role-Based" validation checklist for "CMO category"
    Then the automation catalog should record expected result "CMO edit category is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: CMO add remarks
    Given QA prepares "All Appointments" scenario "CMO add remarks" from SRS requirement "APT-005"
    When QA executes the "Role-Based" validation checklist for "CMO remarks"
    Then the automation catalog should record expected result "CMO add remarks is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: CMO forward to Approver
    Given QA prepares "All Appointments" scenario "CMO forward to Approver" from SRS requirement "APT-005"
    When QA executes the "Role-Based" validation checklist for "Forward to approver"
    Then the automation catalog should record expected result "CMO forward to Approver is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: Approver add edit remarks
    Given QA prepares "All Appointments" scenario "Approver add edit remarks" from SRS requirement "APT-006"
    When QA executes the "Role-Based" validation checklist for "Approver remarks"
    Then the automation catalog should record expected result "Approver add edit remarks is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: Approver approve
    Given QA prepares "All Appointments" scenario "Approver approve" from SRS requirement "APT-006"
    When QA executes the "Role-Based" validation checklist for "Approve"
    Then the automation catalog should record expected result "Approver approve is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: Approver reject
    Given QA prepares "All Appointments" scenario "Approver reject" from SRS requirement "APT-006"
    When QA executes the "Role-Based" validation checklist for "Reject"
    Then the automation catalog should record expected result "Approver reject is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: Approver schedule
    Given QA prepares "All Appointments" scenario "Approver schedule" from SRS requirement "APT-006"
    When QA executes the "Role-Based" validation checklist for "Schedule"
    Then the automation catalog should record expected result "Approver schedule is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @generated
  Scenario: Approver follow-up
    Given QA prepares "All Appointments" scenario "Approver follow-up" from SRS requirement "APT-006"
    When QA executes the "Role-Based" validation checklist for "Follow-up"
    Then the automation catalog should record expected result "Approver follow-up is validated successfully."
    And the scenario should capture screenshots on failure
