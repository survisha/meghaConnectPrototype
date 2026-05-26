@Login @APITest @API
Feature: User Login - API Automation Tests
  Testing user login functionality through REST API endpoints
  Focus: API request/response validation, token management, error handling
  No UI interaction required

  @Smoke @APITest
  Scenario: User login with valid credentials via API
    Given User has valid credentials for login
    When User sends login request with valid username and password
    Then API should return HTTP 200 status
    And Response should contain authentication token
    And Response should contain user details
    And Response token should be JWT format

  @APITest @Smoke
  Scenario: User authentication token is valid JWT
    Given User has valid credentials for login
    When User sends login request with valid username and password
    Then Response should contain bearer token
    And JWT token should contain user_id claim
    And JWT token should contain username claim
    And JWT token should contain email claim
    And JWT token should contain roles claim
    And JWT token should have expiration time
    And JWT token signature should be valid

  @APITest @Negative
  Scenario: Login with invalid username
    Given User has invalid username "invalid_user_123"
    And User has valid password
    When User sends login request with invalid username and valid password
    Then API should return HTTP 401 status (Unauthorized)
    And API should return HTTP 400 status (Bad Request)
    And Response should contain error message
    And Response should not contain authentication token
    And Error message should be "Invalid credentials" or "User not found"

  @APITest @Negative
  Scenario: Login with invalid password
    Given User has valid username
    And User has invalid password "wrong_password_123"
    When User sends login request with valid username and invalid password
    Then API should return HTTP 401 status (Unauthorized)
    And Response should contain error message
    And Response should not contain authentication token
    And Error message should indicate invalid password

  @APITest @Negative
  Scenario: Login with empty username
    Given User has empty username ""
    And User has valid password
    When User sends login request with empty username
    Then API should return HTTP 400 status (Bad Request)
    And Response should contain validation error
    And Response should contain error for username field
    And Response should not contain authentication token

  @APITest @Negative
  Scenario: Login with empty password
    Given User has valid username
    And User has empty password ""
    When User sends login request with empty password
    Then API should return HTTP 400 status (Bad Request)
    And Response should contain validation error
    And Response should contain error for password field
    And Response should not contain authentication token

  @APITest @Negative
  Scenario: Login with missing username field
    Given User has password "validPassword123"
    And User has no username field
    When User sends login request without username
    Then API should return HTTP 400 status (Bad Request)
    And Response should contain error message
    And Error message should indicate "username is required"

  @APITest @Negative
  Scenario: Login with missing password field
    Given User has username "valid_user"
    And User has no password field
    When User sends login request without password
    Then API should return HTTP 400 status (Bad Request)
    And Response should contain error message
    And Error message should indicate "password is required"

  @APITest @Negative
  Scenario: Login with null username
    Given User has null username
    And User has valid password
    When User sends login request with null username
    Then API should return HTTP 400 status (Bad Request)
    And Response should contain validation error

  @APITest @Negative
  Scenario: Login with null password
    Given User has valid username
    And User has null password
    When User sends login request with null password
    Then API should return HTTP 400 status (Bad Request)
    And Response should contain validation error

  @APITest @Negative @Security
  Scenario: SQL injection attempt in username field
    Given User has SQL injection payload "admin' OR '1'='1"
    When User sends login request with SQL injection in username
    Then API should return HTTP 401 status (Unauthorized)
    And API should return HTTP 400 status (Bad Request)
    And Response should not contain authentication token
    And API should handle injection safely

  @APITest @Negative @Security
  Scenario: SQL injection attempt in password field
    Given User has SQL injection payload "' OR 1=1 --"
    When User sends login request with SQL injection in password
    Then API should return HTTP 401 status (Unauthorized)
    And API should return HTTP 400 status (Bad Request)
    And Response should not contain authentication token

  @APITest @Security
  Scenario: XSS payload in username field
    Given User has XSS payload "<script>alert('XSS')</script>"
    When User sends login request with XSS payload in username
    Then API should handle XSS payload safely
    And Response should return error or treat as invalid input
    And API should not execute any JavaScript

  @APITest @Negative
  Scenario: Extremely long username
    Given User has username with 10000 characters
    When User sends login request with extremely long username
    Then API should return HTTP 400 status (Bad Request)
    And API should return HTTP 413 status (Payload Too Large)
    And Response should contain error message

  @APITest @Negative
  Scenario: Extremely long password
    Given User has password with 10000 characters
    When User sends login request with extremely long password
    Then API should return HTTP 400 status (Bad Request)
    And API should return HTTP 413 status (Payload Too Large)

  @APITest @Negative
  Scenario: Invalid request content-type
    Given User has valid credentials
    And User sends request with content-type "text/plain"
    When User sends login request
    Then API should return HTTP 400 status or HTTP 415 status (Unsupported Media Type)

  @APITest
  Scenario Outline: Login with multiple user roles
    Given User "<username>" with role "<role>" exists
    When User sends login request for user "<username>" with password "<password>"
    Then API should return HTTP 200 status
    And Response should contain authentication token
    And Token should have role "<role>"

    Examples:
      | username          | password            | role       |
      | citizen_user      | citizen_pass123     | CITIZEN    |
      | deo_user          | deo_pass123         | DEO        |
      | cmo_user          | cmo_pass123         | CMO        |
      | approver_user     | approver_pass123    | APPROVER   |
      | admin_user        | admin_pass123       | ADMIN      |

  @APITest @PersistSession
  Scenario: Reuse authentication token for subsequent API calls
    Given User has valid credentials
    When User sends login request once
    Then API should return HTTP 200 status
    And Response should contain authentication token
    When User uses the received token to call another API endpoint
    Then Subsequent API call should succeed with HTTP 200 or relevant status
    And Subsequent API call should not require login again

  @APITest @Performance
  Scenario: Login request response time validation
    Given User has valid credentials
    When User sends login request
    Then API should return response within 500 milliseconds
    And API should return response within 1000 milliseconds

  @APITest
  Scenario: Login response structure validation
    Given User has valid credentials
    When User sends login request with valid username and password
    Then Response JSON should have "token" field
    And Response JSON should have "user" object
    And User object should have "id" field
    And User object should have "username" field
    And User object should have "email" field
    And User object should have "roles" array
    And Response JSON should have "expiresIn" field
    And Response JSON should have "tokenType" field

  @APITest
  Scenario: Token refresh capability
    Given User has valid authentication token from login
    When User sends refresh token request
    Then API should return HTTP 200 status
    And Response should contain new authentication token
    And New token should be different from old token
    And New token should be valid for subsequent calls

  @APITest @Security
  Scenario: Concurrent login attempts
    Given Multiple users with valid credentials
    When Multiple users send login requests simultaneously
    Then Each user should receive unique authentication token
    And Tokens should not overlap or conflict
    And Each token should be independently valid

  @APITest @Negative
  Scenario: Replay attack prevention
    Given User has valid authentication token from previous login
    When User attempts to reuse old expired token
    Then API should reject the old token
    And API should return HTTP 401 status (Unauthorized)
    And API should return HTTP 403 status (Forbidden)

  @APITest
  Scenario: Case sensitivity validation
    Given User has username "TestUser" with case sensitivity
    When User sends login request with username "testuser" (lowercase)
    Then API behavior should be documented
    And Response should either accept it or reject with appropriate error

  @APITest @Negative
  Scenario: Leading and trailing whitespace in credentials
    Given User has username " test_user " (with spaces)
    And User has password " password123 " (with spaces)
    When User sends login request with credentials containing whitespace
    Then API should either trim whitespace or return appropriate error
    And Response should be consistent with API design

  @APITest @Negative
  Scenario: Special characters in credentials
    Given User has username with special characters "user@example.com"
    And User has password with special characters "p@$$w0rd!#123"
    When User sends login request with special characters
    Then API should handle special characters appropriately
    And Response should succeed or fail with clear error message

  @APITest @Negative
  Scenario: Unicode characters in credentials
    Given User has username with unicode characters "यूजर"
    When User sends login request with unicode characters
    Then API should handle unicode appropriately
    And Response should be consistent

  @APITest
  Scenario: Login response headers validation
    Given User has valid credentials
    When User sends login request
    Then Response should have "Content-Type: application/json" header
    And Response should have "X-Content-Type-Options" header
    And Response should have appropriate cache control headers
    And Response should not expose sensitive headers

  @APITest @Negative
  Scenario: Account lockout after multiple failed attempts
    Given User has valid username
    When User sends 5 failed login requests with wrong password
    Then Account should be temporarily locked
    And Subsequent login request should return HTTP 429 or 403 status
    And Error message should indicate account is locked

  @APITest
  Scenario: Login audit trail
    Given User has valid credentials
    When User sends login request
    Then API should create audit log entry
    And Audit log should contain username
    And Audit log should contain timestamp
    And Audit log should contain IP address
    And Audit log should contain success/failure status

  @APITest @Different-User
  Scenario: Multiple different users in single session
    Given First user has valid credentials
    When First user sends login request
    Then First user receives authentication token
    And First user is successfully logged in
    When Second user with different credentials sends login request
    Then Second user receives different authentication token
    And Tokens should not interfere with each other
    And Each token should be valid independently
