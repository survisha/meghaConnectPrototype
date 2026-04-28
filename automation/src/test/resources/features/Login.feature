@Login
Feature: User Login - All authentication scenarios

  Background:
    Given User is on the login page

  @UITest @Smoke
  Scenario: User login with valid credentials via UI
    Given User has valid credentials
    When User logs in via UI with valid credentials
    Then Login should be successful
    And User should be logged in
    And User should continue with the session

  @APITest @Smoke
  Scenario: User login with valid credentials via API
    Given User has valid credentials
    When User logs in via API with valid credentials
    Then Login should be successful
    And User should be logged in

  @APITest @Negative
  Scenario: User login with invalid username
    Given User has invalid username
    And User has valid credentials (password)
    When User logs in with invalid credentials
    Then Login should fail
    And User should not be logged in

  @APITest @Negative
  Scenario: User login with invalid password
    Given User has valid credentials
    And User has invalid password
    When User logs in with invalid credentials
    Then Login should fail
    And User should not be logged in

  @APITest @Negative
  Scenario: User login with empty credentials
    Given User has empty credentials
    When User logs in with invalid credentials
    Then Login should fail
    And User should not be logged in

  @UITest @Smoke
  Scenario: User enters username and password manually
    Given User has valid credentials
    When User enters username
    And User enters password
    And User clicks login button
    Then Login should be successful

  @APITest @Negative
  Scenario: User login with missing username
    When User logs in with invalid credentials
    Then Login should fail
    And Error message should be displayed

  @APITest @Negative
  Scenario: User login with missing password
    When User logs in with invalid credentials
    Then Login should fail
    And Error message should be displayed

  @UITest
  Scenario: User clicks forgot password link
    When User clicks forgot password link
    Then User should see login page

  @UITest
  Scenario: User clicks signup link
    When User clicks signup link
    Then User should see login page

  @Login @PersistSession
  Scenario: Login once and continue with subsequent flows
    Given User has valid credentials
    When User logs in via API with valid credentials
    Then Login should be successful
    And User should be logged in
    # This scenario preserves the session for next scenarios

  @Login @Smoke
  Scenario Outline: User login with multiple valid users
    Given User has username "<username>" and password "<password>"
    When User logs in via API with valid credentials
    Then Login should be successful
    And User should be logged in
    And User logs out

    Examples:
      | username    | password |
      | admin       | Admin@123 |
      | testuser    | Test@123  |
      | moderator   | Mod@123   |

  @APITest @Security
  Scenario: Token validation after login
    Given User has valid credentials
    When User logs in via API with valid credentials
    Then Login should be successful
    And User should be logged in
    And Session token should be valid JWT format
    And Token should be present in Authorization header

  @Login @Error @Negative
  Scenario: Error message on login fail
    Given User has invalid username
    And User has invalid password
    When User logs in with invalid credentials
    Then Login should fail
    And Error message should be displayed
    And Error message should contain "Invalid credentials"

  @Login @Smoke @PersistSession
  Scenario: Successful login with session reuse
    Given User has valid credentials
    When User logs in via API with valid credentials
    Then Login should be successful
    # Session is reused for subsequent scenarios tagged with @PersistSession
