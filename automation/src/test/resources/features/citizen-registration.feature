@generated @citizen-registration @regression
Feature: Citizen Registration
  SRS-derived coverage for Citizen Registration.

  @citizen @positive @smoke @generated
  Scenario: Valid EPIC registration
    Given QA prepares "Citizen Registration" scenario "Valid EPIC registration" from SRS requirement "CIT-001"
    When QA executes the "Positive" validation checklist for "EPIC registration"
    Then the automation catalog should record expected result "Citizen registers successfully using EPIC and reaches dashboard."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Valid Aadhaar registration
    Given QA prepares "Citizen Registration" scenario "Valid Aadhaar registration" from SRS requirement "CIT-002"
    When QA executes the "Positive" validation checklist for "Aadhaar registration"
    Then the automation catalog should record expected result "Valid Aadhaar registration is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Valid No ID registration
    Given QA prepares "Citizen Registration" scenario "Valid No ID registration" from SRS requirement "CIT-003"
    When QA executes the "Positive" validation checklist for "No ID registration"
    Then the automation catalog should record expected result "Valid No ID registration is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @api @generated
  Scenario: KYC pending registration when service is down
    Given QA prepares "Citizen Registration" scenario "KYC pending registration when service is down" from SRS requirement "CIT-004"
    When QA executes the "Positive" validation checklist for "KYC pending fallback"
    Then the automation catalog should record expected result "KYC pending registration when service is down is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @generated
  Scenario: Outside state NA checkbox flow
    Given QA prepares "Citizen Registration" scenario "Outside state NA checkbox flow" from SRS requirement "CIT-005"
    When QA executes the "Positive" validation checklist for "District/Constituency/Booth"
    Then the automation catalog should record expected result "Outside state NA checkbox flow is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @positive @smoke @generated
  Scenario: OTP success flow
    Given QA prepares "Citizen Registration" scenario "OTP success flow" from SRS requirement "CIT-006"
    When QA executes the "Positive" validation checklist for "OTP validation"
    Then the automation catalog should record expected result "OTP success flow is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Invalid EPIC format
    Given QA prepares "Citizen Registration" scenario "Invalid EPIC format" from SRS requirement "CIT-007"
    When QA executes the "Validation" validation checklist for "EPIC registration"
    Then the automation catalog should record expected result "Invalid EPIC format is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Empty EPIC
    Given QA prepares "Citizen Registration" scenario "Empty EPIC" from SRS requirement "CIT-007"
    When QA executes the "Validation" validation checklist for "EPIC registration"
    Then the automation catalog should record expected result "Empty EPIC is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Empty name
    Given QA prepares "Citizen Registration" scenario "Empty name" from SRS requirement "CIT-008"
    When QA executes the "Validation" validation checklist for "Name validation"
    Then the automation catalog should record expected result "Empty name is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Invalid name with numbers or special characters
    Given QA prepares "Citizen Registration" scenario "Invalid name with numbers or special characters" from SRS requirement "CIT-008"
    When QA executes the "Validation" validation checklist for "Name validation"
    Then the automation catalog should record expected result "Invalid name with numbers or special characters is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Invalid mobile number
    Given QA prepares "Citizen Registration" scenario "Invalid mobile number" from SRS requirement "CIT-009"
    When QA executes the "Validation" validation checklist for "Mobile validation"
    Then the automation catalog should record expected result "Invalid mobile number is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @generated
  Scenario: Wrong OTP
    Given QA prepares "Citizen Registration" scenario "Wrong OTP" from SRS requirement "CIT-010"
    When QA executes the "Negative" validation checklist for "OTP validation"
    Then the automation catalog should record expected result "Wrong OTP is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @generated
  Scenario: Expired OTP
    Given QA prepares "Citizen Registration" scenario "Expired OTP" from SRS requirement "CIT-010"
    When QA executes the "Negative" validation checklist for "OTP validation"
    Then the automation catalog should record expected result "Expired OTP is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Missing required dropdowns
    Given QA prepares "Citizen Registration" scenario "Missing required dropdowns" from SRS requirement "CIT-011"
    When QA executes the "Validation" validation checklist for "Dropdown validation"
    Then the automation catalog should record expected result "Missing required dropdowns is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Missing photo
    Given QA prepares "Citizen Registration" scenario "Missing photo" from SRS requirement "CIT-011"
    When QA executes the "Validation" validation checklist for "Photo capture"
    Then the automation catalog should record expected result "Missing photo is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @api @generated
  Scenario: Service unavailable
    Given QA prepares "Citizen Registration" scenario "Service unavailable" from SRS requirement "CIT-004"
    When QA executes the "Negative" validation checklist for "KYC service unavailable"
    Then the automation catalog should record expected result "Service unavailable is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for invalid EPIC number
    Given QA prepares "Citizen Registration" scenario "Invalid EPIC field message" from SRS requirement "CIT-007"
    When QA executes the "Validation" validation checklist for "EPIC number"
    Then user should see validation message "EPIC number must be 3 letters followed by 7 digits."
    And the automation catalog should record expected result "Invalid EPIC field message is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for empty citizen name
    Given QA prepares "Citizen Registration" scenario "Empty name field message" from SRS requirement "CIT-008"
    When QA executes the "Validation" validation checklist for "Citizen name"
    Then user should see validation message "Name is required."
    And the automation catalog should record expected result "Empty name field message is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for invalid citizen mobile
    Given QA prepares "Citizen Registration" scenario "Invalid mobile field message" from SRS requirement "CIT-009"
    When QA executes the "Validation" validation checklist for "Mobile number"
    Then user should see validation message "Mobile number must be 10 digits."
    And the automation catalog should record expected result "Invalid mobile field message is validated successfully."
    And the scenario should capture screenshots on failure

  @citizen @negative @validation @generated
  Scenario: Show validation message for missing district
    Given QA prepares "Citizen Registration" scenario "Missing district field message" from SRS requirement "CIT-011"
    When QA executes the "Validation" validation checklist for "District"
    Then user should see validation message "District is required."
    And the automation catalog should record expected result "Missing district field message is validated successfully."
    And the scenario should capture screenshots on failure
