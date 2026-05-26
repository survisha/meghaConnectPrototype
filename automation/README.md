# MeghaConnect Automation Framework

A production-ready, comprehensive automation framework for testing MeghaConnect Spring Boot + Angular application. Built with Selenium WebDriver, Cucumber BDD, REST Assured, and Extent Reports.

## 📋 Table of Contents

1. [Project Structure](#project-structure)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Configuration](#configuration)
5. [Running Tests](#running-tests)
6. [Architecture](#architecture)
7. [Page Object Model](#page-object-model)
8. [API Testing](#api-testing)
9. [BDD with Cucumber](#bdd-with-cucumber)
10. [Reusable Flows](#reusable-flows)
11. [Best Practices](#best-practices)

## 📁 Project Structure

```
automation/
├── pom.xml                                  # Maven configuration with all dependencies
├── src/test/
│   ├── java/com/meghaconnect/automation/
│   │   ├── config/
│   │   │   ├── ConfigManager.java          # Configuration property management
│   │   │   ├── DriverManager.java          # WebDriver initialization & management
│   │   │   └── ApiClient.java              # REST API client for API testing
│   │   ├── pageobjects/
│   │   │   └── LoginPage.java              # Login page object model (centralized locators)
│   │   ├── stepdefinitions/
│   │   │   └── LoginStepDefinition.java    # Cucumber step implementations
│   │   ├── apitests/
│   │   │   └── LoginApiTest.java           # Login API test operations
│   │   ├── flows/
│   │   │   └── LoginFlow.java              # Reusable login flow (UI + API, singleton pattern)
│   │   ├── hooks/
│   │   │   └── TestHooks.java              # Cucumber before/after hooks
│   │   ├── runners/
│   │   │   └── LoginTestRunner.java        # Cucumber test runner
│   │   ├── utils/
│   │   │   ├── WebElementUtil.java         # Common WebElement operations & waits
│   │   │   └── ScreenshotUtil.java         # Screenshot capture utility
│   │   └── reportutil/
│   │       └── ExtentReportUtil.java       # Extent report generation (optional)
│   └── resources/
│       ├── config/
│       │   └── application.properties      # Environment configuration
│       ├── features/
│       │   └── Login.feature               # Cucumber feature file
│       └── testdata/
│           └── login-testdata.json         # Test data in JSON format
└── README.md                               # This file
```

## 🎯 Prerequisites

- **Java**: JDK 17 or higher
- **Maven**: 3.8.1 or higher
- **Browser Drivers**: Automatically downloaded by WebDriverManager
- **Browser**: Chrome/Firefox/Edge installed
- **Git**: For version control

## 🚀 Installation

### 1. Clone/Setup Project
```bash
cd automation
```

### 2. Install Dependencies
```bash
mvn clean install
```

### 3. Verify Installation
```bash
mvn verify -DskipTests
```

## ⚙️ Configuration

### application.properties
```properties
# Environment
environment=dev
base.url=http://localhost:4200
api.base.url=http://localhost:8080

# Browser Configuration
browser=chrome                  # chrome, firefox, edge
headless=false
browser.maximize=true

# Waits
implicit.wait=10               # seconds
explicit.wait=15               # seconds
page.load.timeout=30           # seconds

# API Authentication
api.auth.username=testuser
api.auth.password=testpassword

# Reporting
report.path=test-output/
screenshot.on.fail=true
```

### Environment Switching
```bash
# Development
mvn test -Denvironment=dev

# Staging
mvn test -Denvironment=staging

# Production
mvn test -Denvironment=prod
```

## 🧪 Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Feature
```bash
mvn test -Dtest=LoginTestRunner
```

### Run with Specific Tags
```bash
# Run only smoke tests
mvn test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@Smoke"

# Run API tests only
mvn test -Dcucumber.filter.tags="@APITest"

# Run UI tests only
mvn test -Dcucumber.filter.tags="@UITest"

# Run excluding ignored tests
mvn test -Dcucumber.filter.tags="not @Ignore"

# Run multiple tags
mvn test -Dcucumber.filter.tags="@Login and not @Ignore"
```

### Run in Headless Mode
```bash
mvn test -Dheadless=true
```

### Run Citizen Login UAT Flow
```bash
mvn clean test -Dtest=UatTestRunner -Dcucumber.filter.tags="@CitizenLogin and @UAT" -Dheadless=false
```

### Run Directly by Runner Class
```bash
mvn clean test -Dtest=UatTestRunner
```

### Run All UAT UI Tests
```bash
mvn clean test -Dtest=UatTestRunner -Dcucumber.filter.tags="@UAT and @UITest"
```

### Run Headless
```bash
mvn clean test -Dtest=UatTestRunner -Dheadless=true
```

### Run With Visual Highlighting
```bash
mvn clean test -Dtest=UatTestRunner -Dcucumber.filter.tags="@CitizenLogin and @UAT" -Dheadless=false -Dhighlight.enabled=true -Dscreenshot.each.step=true
```

Visual execution highlights active elements with a red border, yellow background, and red shadow before Selenium clicks, types, or verifies them. Step screenshots are saved and attached to the Cucumber report when `screenshot.each.step=true`; set it to `false` to keep only failure screenshots.

Expected output:
- Chrome opens `https://www.meghaconnect.cloud`
- Citizen login flow executes from Excel data at `src/test/resources/testdata/citizen-login-testdata.xlsx`
- OTP, EPIC, and register branching is validated
- Reports are generated at `target/cucumber-reports/cucumber.html`, `target/cucumber-reports/cucumber.json`, and `target/cucumber-reports/cucumber.xml`
- Failure screenshots are saved and attached to the Cucumber report

For current testing, using the webpage demo OTP is correct. For production or dynamic SMS OTP, automation should not read SMS directly unless a test device or SMS API is available. Recommended future options are: expose OTP in UI only for automation test environments, provide a secure test-only backend OTP fetch API, use a fixed OTP for whitelisted UAT mobile numbers, or use manual OTP mode for production smoke testing. Never expose demo OTP in production UI.

Current absolute XPath locators will work but are fragile. Add stable IDs or `data-testid` attributes in Angular for all automation elements: `home_loginBtn`, `login_citizenOtpBtn`, `publicLogin_mobileInput`, `publicLogin_generateOtpBtn`, `publicLogin_otpMessage`, `publicLogin_otpInput`, `publicLogin_verifyBtn`, `visitorDashboard_profileHeader`, and `shell_logoutBtn`. Prefer `By.id()` over XPath wherever stable IDs are available.

### Parallel Execution
```bash
mvn test -Pparallel
```

### Generate Report
```bash
mvn test
# Reports generated in: target/cucumber-reports/
```

## 🏗️ Architecture

### Configuration Management
- **ConfigManager**: Centralized property reading from application.properties
- **DriverManager**: Singleton WebDriver management (thread-safe)
- **ApiClient**: REST client with token management

### Page Object Model (POM)
- **LoginPage**: Encapsulates all login page elements and actions
- Centralized locators (XPath, CSS selectors)
- Reusable methods for page interactions
- Built-in waits and error handling

### Flows (Reusable Workflows)
- **LoginFlow**: Singleton pattern ensures login happens once per session
- Supports both UI and API-based login
- Session state management
- Prevents duplicate logins in same test session

### Test Hooks
- **@Before**: Setup WebDriver, API client
- **@After**: Cleanup resources, capture screenshots on failure
- **@Login**: Special hooks for login test scenarios
- **@PersistSession**: Maintains session across multiple scenarios

## 🖥️ Page Object Model

### Example: LoginPage
```java
// Centralized locators
private static final By USERNAME_INPUT = By.xpath("//input[@name='username']");
private static final By LOGIN_BUTTON = By.xpath("//button[contains(text(), 'Login')]");

// Methods for page interactions
public void login(String username, String password) {
    WebElementUtil.type(USERNAME_INPUT, username);
    // ...
}
```

### Using Page Objects in Step Definitions
```java
public class LoginStepDefinition {
    private LoginPage loginPage;
    
    @Given("User is on the login page")
    public void navigateToLoginPage() {
        loginPage.waitForLoginPageReady();
    }
}
```

## 🌐 API Testing

### RestAssured Integration
```java
// Login via API
Response response = ApiClient.post("/api/v1/auth/login", loginPayload);
response.getStatusCode();        // 200
response.jsonPath().getString("data.token");  // Extract token
```

### Token Management
```java
// Set token for authenticated requests
ApiClient.setAuthToken(token);

// Subsequent requests include Authorization header
Response profileResponse = ApiClient.get("/api/v1/auth/profile");
```

### Request/Response Validation
```java
// Validate response structure
assertTrue(loginApiTest.validateResponseStructure(response));

// Validate token format
assertTrue(loginApiTest.validateTokenFormat(token));
```

## 🥒 BDD with Cucumber

### Feature Files
```gherkin
@Login
Feature: User Login

  @Smoke
  Scenario: User login with valid credentials
    Given User has valid credentials
    When User logs in via API with valid credentials
    Then Login should be successful
    And User should be logged in
```

### Step Definitions
```java
@Given("User has valid credentials")
public void userHasValidCredentials() {
    testUsername = ConfigManager.getApiAuthUsername();
    testPassword = ConfigManager.getApiAuthPassword();
}

@When("User logs in via API with valid credentials")
public void userLogsInViaAPI() {
    loginResult = loginFlow.loginViaAPI(testUsername, testPassword);
}
```

### Cucumber Tags
- `@Smoke`: Critical smoke tests
- `@UITest`: UI-based tests
- `@APITest`: API-based tests
- `@Negative`: Negative test cases
- `@PersistSession`: Maintain session across scenarios
- `@Ignore`: Skip this scenario

## 🔄 Reusable Flows

### LoginFlow - Session Management
```java
// Login happens once per session
public static boolean isLoggedIn() {
    return isLoggedIn && sessionToken != null;
}

// Prevent duplicate logins
if (LoginFlow.isLoggedIn()) {
    logger.info("Reusing existing session");
    return true;
}

// Perform login (UI or API)
loginFlow.loginViaUI(username, password);
loginFlow.loginViaAPI(username, password);

// Continue with existing session in subsequent flows
loginFlow.continueWithSession();

// Logout and clear session
loginFlow.logout();
LoginFlow.resetSession();
```

### Example: Multi-Flow Scenario
```
Scenario: Complete citizen registration flow
  Given User has valid credentials
  When User logs in via API with valid credentials  [LoginFlow]
  # Session persists - no re-login needed
  And User registers as citizen with details      [CitizenRegistrationFlow]
  And User books appointment                       [AppointmentBookingFlow]
  Then All operations should be successful
```

## 📊 Reporting

### Extent Reports Integration
```java
// Automatically captured:
// - Test execution status
// - Screenshots on failure
// - Step-by-step logs
// - System information
// - Device information (if applicable)
```

### Report Location
```
target/
├── cucumber-reports/
│   ├── cucumber.html          # Main HTML report
│   ├── cucumber.xml           # JUnit XML format
│   ├── screenshots/           # Test screenshots
│   └── logs/                  # Detailed logs
└── json-reports/
    └── cucumber.json          # JSON format for CI/CD
```

## 🎓 Best Practices

### 1. Page Object Modeling
✅ DO:
- Keep locators in one place
- Use meaningful method names
- Include explicit waits
- Handle exceptions gracefully

❌ DON'T:
- Hardcode locators in step definitions
- Mix test logic with page interactions
- Use implicit waits exclusively

### 2. Reusable Flows
✅ DO:
- Use singleton pattern for shared flows (like login)
- Prevent duplicate operations within session
- Clear session between independent tests
- Document session persistence in tags

❌ DON'T:
- Call login multiple times in same scenario
- Forget to cleanup resources
- Hardcode test data in flow classes

### 3. Test Data Management
✅ DO:
- Keep test data in separate JSON files
- Use ConfigManager for property access
- Parameterize scenarios with outline tables
- Handle test data cleanup in hooks

❌ DON'T:
- Hardcode credentials in code
- Mix sensitive data with test scripts
- Leave test data scattered across files

### 4. Error Handling
✅ DO:
- Use informative error messages
- Capture screenshots on failures
- Log step-by-step execution
- Handle timeouts gracefully

❌ DON'T:
- Swallow exceptions silently
- Use generic try-catch blocks
- Skip error logging in production

### 5. Code Organization
✅ DO:
- Separate concerns: Config, Pages, Steps, Flows
- Use meaningful class/method names
- Include Javadoc comments
- Follow SOLID principles

❌ DON'T:
- Mix multiple responsibilities in one class
- Create god objects
- Copy-paste code (use utilities)

## 📝 Adding New Test Scenarios

### 1. Create Feature File
```gherkin
# src/test/resources/features/NewFlow.feature
@NewFlow
Feature: New Test Scenarios

  Scenario: New test case
    Given prerequisite
    When action happens
    Then verify result
```

### 2. Implement Step Definitions
```java
// src/test/.../stepdefinitions/NewStepDefinition.java
public class NewStepDefinition {
    @Given("prerequisite")
    public void prerequisite() { }
    
    @When("action happens")
    public void actionHappens() { }
    
    @Then("verify result")
    public void verifyResult() { }
}
```

### 3. Create Page Object (if UI test)
```java
// src/test/.../pageobjects/NewPage.java
public class NewPage {
    private static final By ELEMENT = By.xpath("//locator");
    
    public void performAction() {
        WebElementUtil.click(ELEMENT);
    }
}
```

### 4. Create Flow (if multi-step workflow)
```java
// src/test/.../flows/NewFlow.java
public class NewFlow {
    public void executeFlow() {
        // Multi-step workflow combining multiple pages/APIs
    }
}
```

### 5. Run New Tests
```bash
mvn test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@NewFlow"
```

## 🔒 Security Considerations

- **Credentials**: Never commit passwords to repository
- **API Keys**: Use environment variables
- **Logs**: Mask sensitive data (tokens, passwords)
- **Screenshots**: Do not capture sensitive information
- **Test Accounts**: Use dedicated test accounts, never production accounts

## 📞 Support & Troubleshooting

### Common Issues

**WebDriver not initializing:**
```bash
# Solution: Run with explicit browser
mvn test -Dbrowser=chrome
```

**Tests timing out:**
```bash
# Solution: Increase waits in application.properties
explicit.wait=20
```

**Report not generating:**
```bash
# Solution: Check target/cucumber-reports directory
mvn clean test
```

## 📚 Additional Resources

- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [Cucumber Documentation](https://cucumber.io/docs/cucumber/)
- [REST Assured Documentation](https://rest-assured.io/)
- [Extent Reports Documentation](https://extentreports.com/)

---

**Framework Version**: 1.0.0  
**Last Updated**: April 28, 2026  
**Maintained By**: QA Automation Team
