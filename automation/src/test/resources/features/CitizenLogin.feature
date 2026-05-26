@CitizenLogin @UAT @UITest
Feature: Citizen login with mobile Number

  Scenario Outline: Citizen login using mobile number and demo OTP
    Given Citizen opens MeghaConnect UAT home page
    When Citizen completes OTP login using scenario "<scenarioName>"
    Then Citizen should be redirected to visitor dashboard
    And Citizen logs out successfully

    Examples:
      | scenarioName         |
      | ValidCitizenOtpLogin |
