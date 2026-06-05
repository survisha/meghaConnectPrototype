@generated @apply-for-scheme @regression
Feature: Apply for Scheme
  SRS-derived coverage for Apply for Scheme.

  @citizen @positive @generated
  Scenario: Select scheme
    Given QA prepares "Apply for Scheme" scenario "Select scheme" from SRS requirement "SCH-001"
    When QA executes the "Positive" validation checklist for "Scheme selection"
    Then the automation catalog should record expected result "Select scheme is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Add project details
    Given QA prepares "Apply for Scheme" scenario "Add project details" from SRS requirement "SCH-001"
    When QA executes the "Positive" validation checklist for "Project details"
    Then the automation catalog should record expected result "Add project details is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Add financial line items
    Given QA prepares "Apply for Scheme" scenario "Add financial line items" from SRS requirement "SCH-002"
    When QA executes the "Positive" validation checklist for "Financial line items"
    Then the automation catalog should record expected result "Add financial line items is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Add community contribution
    Given QA prepares "Apply for Scheme" scenario "Add community contribution" from SRS requirement "SCH-002"
    When QA executes the "Positive" validation checklist for "Community contribution"
    Then the automation catalog should record expected result "Add community contribution is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Upload documents
    Given QA prepares "Apply for Scheme" scenario "Upload documents" from SRS requirement "SCH-003"
    When QA executes the "Positive" validation checklist for "Document upload"
    Then the automation catalog should record expected result "Upload documents is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Review and submit
    Given QA prepares "Apply for Scheme" scenario "Review and submit" from SRS requirement "SCH-003"
    When QA executes the "Positive" validation checklist for "Review submit"
    Then the automation catalog should record expected result "Review and submit is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Application number generated
    Given QA prepares "Apply for Scheme" scenario "Application number generated" from SRS requirement "SCH-003"
    When QA executes the "Positive" validation checklist for "Application number"
    Then the automation catalog should record expected result "Application number generated is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Missing scheme type
    Given QA prepares "Apply for Scheme" scenario "Missing scheme type" from SRS requirement "SCH-001"
    When QA executes the "Validation" validation checklist for "Scheme type"
    Then the automation catalog should record expected result "Missing scheme type is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Missing project name
    Given QA prepares "Apply for Scheme" scenario "Missing project name" from SRS requirement "SCH-001"
    When QA executes the "Validation" validation checklist for "Project name"
    Then the automation catalog should record expected result "Missing project name is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Invalid estimated cost
    Given QA prepares "Apply for Scheme" scenario "Invalid estimated cost" from SRS requirement "SCH-002"
    When QA executes the "Validation" validation checklist for "Estimated cost"
    Then the automation catalog should record expected result "Invalid estimated cost is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Invalid quantity or unit cost
    Given QA prepares "Apply for Scheme" scenario "Invalid quantity or unit cost" from SRS requirement "SCH-002"
    When QA executes the "Validation" validation checklist for "Quantity/unit cost"
    Then the automation catalog should record expected result "Invalid quantity or unit cost is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Submit without required fields
    Given QA prepares "Apply for Scheme" scenario "Submit without required fields" from SRS requirement "SCH-003"
    When QA executes the "Validation" validation checklist for "Required fields"
    Then the automation catalog should record expected result "Submit without required fields is validated successfully."
    And the scenario should capture screenshots on failure
