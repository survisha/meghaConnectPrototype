@Login @UITest @UI
Feature: User Login - UI Automation Tests
  Testing user login functionality through the UI (Selenium WebDriver)
  Focus: Page object interactions, element visibility, form validation

  Background:
    Given User navigates to the application
    And Login page is displayed

  @Smoke @UITest
  Scenario: User login with valid credentials via UI
    Given User has valid credentials
    When User enters username in the text field
    And User enters password in the text field
    And User clicks the login button
    Then User should be redirected to dashboard
    And Dashboard page should be displayed
    And Username should be visible in the header

  @UITest @Smoke
  Scenario: User manually enters and submits credentials
    Given User has valid credentials
    When User clicks on username field
    And User types username
    And User clicks on password field
    And User types password
    And User verifies login button is visible
    And User clicks the login button
    Then Login button should become disabled during processing
    And Loading spinner should disappear
    Then User should be redirected to dashboard

  @UITest @Negative
  Scenario: Username field validates empty submission
    When User leaves username field empty
    And User enters valid password
    And User clicks the login button
    Then Username field should show validation error
    And Error message should contain "Username is required"
    And User should remain on login page

  @UITest @Negative
  Scenario: Password field validates empty submission
    When User enters valid username
    And User leaves password field empty
    And User clicks the login button
    Then Password field should show validation error
    And Error message should contain "Password is required"
    And User should remain on login page

  @UITest @Negative
  Scenario: Both fields empty validation
    When User leaves both username and password fields empty
    And User clicks the login button
    Then Both fields should show validation errors
    And Form should not be submitted

  @UITest @Negative
  Scenario: Invalid username displays error message
    Given User has invalid username
    And User has valid password
    When User enters invalid username in the text field
    And User enters valid password in the text field
    And User clicks the login button
    Then Error message should be displayed on page
    And Error message should contain "Invalid credentials"
    And User should remain on login page

  @UITest @Negative
  Scenario: Invalid password displays error message
    Given User has valid username
    And User has invalid password
    When User enters valid username in the text field
    And User enters invalid password in the text field
    And User clicks the login button
    Then Error message should be displayed on page
    And Error message should contain "Invalid credentials"
    And User should remain on login page

  @UITest
  Scenario: Remember me checkbox functionality
    Given User has valid credentials
    When User enters username in the text field
    And User enters password in the text field
    And User checks the remember me checkbox
    And User clicks the login button
    Then User should be redirected to dashboard
    # And Browser should store remember me preference

  @UITest
  Scenario: Forgot password link navigation
    When User clicks on forgot password link
    Then Forgot password page should be displayed
    And Password reset form should be visible
    And User should see "Enter your email to reset password" message

  @UITest
  Scenario: Sign up link navigation
    When User clicks on sign up link
    Then Sign up page should be displayed
    And Registration form should be visible
    And User should see "Create a new account" message

  @UITest
  Scenario: Login form layout verification
    Then Login page should display company logo
    And Login page should display page title "Login"
    And Username input field should be visible
    And Password input field should be visible
    And Login button should be visible
    And Forgot password link should be visible
    And Sign up link should be visible

  @UITest
  Scenario: Password field input type validation
    When User enters password in the text field
    Then Password field should mask the input
    And Password characters should not be visible as plain text

  @UITest @Negative
  Scenario: Special characters in username field
    Given User has username with special characters "<script>alert('XSS')</script>"
    When User enters username with special characters
    And User enters valid password
    And User clicks the login button
    Then Form should handle special characters safely
    And No alert dialog should appear

  @UITest
  Scenario: Form feedback during login process
    Given User has valid credentials
    When User enters username in the text field
    And User enters password in the text field
    And User clicks the login button
    Then Loading spinner should be visible
    And Login button should display loading state
    And Form should be disabled during processing

  @UITest
  Scenario: Page elements responsiveness
    Then Login form should be centered on page
    And Login card should have proper styling
    And All form fields should have consistent styling
    And Buttons should have proper spacing
    And Error messages should be clearly visible

  @UITest @Accessibility
  Scenario: Keyboard navigation on login page
    Given Login page is displayed
    When User presses Tab key to navigate
    Then Username field should become focused
    When User presses Tab key again
    Then Password field should become focused
    When User presses Tab key again
    Then Login button should become focused
    When User presses Enter key on login button
    Then Form should be submitted

  @UITest @Accessibility
  Scenario: Screen reader friendly labels
    Then Username input should have associated label
    And Password input should have associated label
    And Error messages should have proper aria-role
    And Buttons should have descriptive text

  @UITest
  Scenario: Login page title and metadata
    Then Page title should be "Login - MeghaConnect"
    And Page should have proper favicon
    And Page should have open graph tags
    And Meta description should be present
