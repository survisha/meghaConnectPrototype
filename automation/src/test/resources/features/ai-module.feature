@generated @ai-module @regression
Feature: AI Module
  SRS-derived coverage for AI Module.

  @positive @smoke @generated
  Scenario: MeghaBot opens
    Given QA prepares "AI Module" scenario "MeghaBot opens" from SRS requirement "AI-001"
    When QA executes the "UI" validation checklist for "MeghaBot"
    Then the automation catalog should record expected result "MeghaBot opens is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: MeghaBot responds during appointment journey
    Given QA prepares "AI Module" scenario "MeghaBot responds during appointment journey" from SRS requirement "AI-001"
    When QA executes the "Positive" validation checklist for "Chat response"
    Then the automation catalog should record expected result "MeghaBot responds during appointment journey is validated successfully."
    And the scenario should capture screenshots on failure

  @api @positive @generated
  Scenario: AI Notes generated from PDF DOC DOCX image
    Given QA prepares "AI Module" scenario "AI Notes generated from PDF DOC DOCX image" from SRS requirement "AI-002"
    When QA executes the "API" validation checklist for "AI notes generation"
    Then the automation catalog should record expected result "AI Notes generated from PDF DOC DOCX image is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: OCR fallback for scanned image
    Given QA prepares "AI Module" scenario "OCR fallback for scanned image" from SRS requirement "AI-003"
    When QA executes the "Positive" validation checklist for "OCR fallback"
    Then the automation catalog should record expected result "OCR fallback for scanned image is validated successfully."
    And the scenario should capture screenshots on failure

  @positive @generated
  Scenario: AI Notes View shows all generated sections
    Given QA prepares "AI Module" scenario "AI Notes View shows all generated sections" from SRS requirement "AI-004"
    When QA executes the "UI" validation checklist for "AI notes view"
    Then the automation catalog should record expected result "AI Notes View shows all generated sections is validated successfully."
    And the scenario should capture screenshots on failure

  @roleBased @positive @generated
  Scenario: AI Dashboard insights visible for authorized roles
    Given QA prepares "AI Module" scenario "AI Dashboard insights visible for authorized roles" from SRS requirement "AI-006"
    When QA executes the "Role-Based" validation checklist for "AI dashboard"
    Then the automation catalog should record expected result "AI Dashboard insights visible for authorized roles is validated successfully."
    And the scenario should capture screenshots on failure
