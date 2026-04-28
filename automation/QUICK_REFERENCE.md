# Quick Reference - MeghaConnect Automation Framework

## 🚀 Get Started in 3 Steps

### 1. Setup
```bash
cd automation
mvn clean install
```

### 2. Run Tests
```bash
mvn test
```

### 3. View Results
```
Open: target/cucumber-reports/cucumber.html
```

---

## 📚 Essential Commands

### Test Execution
```bash
mvn test                                           # All tests
mvn test -Dtest=LoginTestRunner                   # Login tests only
mvn clean test                                    # Clean + test
mvn test -DskipTests                              # Build only
```

### Filter by Tags
```bash
mvn test -Dcucumber.filter.tags="@Smoke"          # Smoke tests
mvn test -Dcucumber.filter.tags="@APITest"        # API only
mvn test -Dcucumber.filter.tags="@UITest"         # UI only
mvn test -Dcucumber.filter.tags="not @Ignore"     # Exclude ignored
mvn test -Dcucumber.filter.tags="@Login and not @WIP"  # Multiple conditions
```

### Browser Configuration
```bash
mvn test -Dbrowser=chrome                         # Chrome
mvn test -Dbrowser=firefox                        # Firefox
mvn test -Dbrowser=edge                           # Edge
mvn test -Dheadless=true                          # Headless mode
```

### Environment Setup
```bash
mvn test -Denvironment=dev                        # Development
mvn test -Denvironment=staging                    # Staging
mvn test -Denvironment=prod                       # Production
```

### Debugging
```bash
mvn test -X                                       # Debug mode
mvn test -Dcucumber.execution.dry-run=true        # Validate features only
mvn test -Dlog.level=DEBUG                        # Debug logging
```

### Parallel Execution
```bash
mvn test -DthreadCount=4                          # 4 parallel threads
mvn test -Dcucumber.execution.parallel.enabled=true
```

---

## 🗂️ Project Structure at a Glance

```
automation/
├── pom.xml                          Maven config (all dependencies)
├── README.md                        Complete documentation (500+ lines)
├── EXECUTION_GUIDE.md               How to run tests (600+ lines)
├── EXTENDING_FRAMEWORK.md           Add new flows guide
├── FRAMEWORK_SUMMARY.md             What's implemented
└── src/test/
    ├── java/com/meghaconnect/automation/
    │   ├── config/
    │   │   ├── ConfigManager.java          Property management
    │   │   ├── DriverManager.java          WebDriver singleton
    │   │   └── ApiClient.java              REST client
    │   ├── pageobjects/
    │   │   └── LoginPage.java              Login POM (centralized locators)
    │   ├── flows/
    │   │   └── LoginFlow.java              Reusable login (singleton + session)
    │   ├── stepdefinitions/
    │   │   └── LoginStepDefinition.java    30+ Cucumber steps
    │   ├── apitests/
    │   │   └── LoginApiTest.java           API operations
    │   ├── hooks/
    │   │   └── TestHooks.java              Before/After setup
    │   ├── runners/
    │   │   └── LoginTestRunner.java        Cucumber runner
    │   └── utils/
    │       ├── WebElementUtil.java         UI operations
    │       └── ScreenshotUtil.java         Screenshots
    └── resources/
        ├── config/
        │   └── application.properties      50+ config options
        ├── features/
        │   └── Login.feature               12 scenarios
        ├── testdata/
        │   └── login-testdata.json         Test data
        └── log4j2.xml                      Logging config
```

---

## 🎯 Key Features

| Feature | Status | Details |
|---------|--------|---------|
| UI Automation | ✅ | Selenium WebDriver + POM |
| API Testing | ✅ | REST Assured + Token Management |
| BDD Testing | ✅ | Cucumber (12 Login scenarios) |
| Reusable Flows | ✅ | LoginFlow (singleton pattern) |
| Session Management | ✅ | Login once, reuse session |
| Screenshot Capture | ✅ | Conditional on pass/fail/step |
| Logging | ✅ | Log4j2 async + rolling files |
| Reports | ✅ | Extent, HTML, JSON, JUnit XML |
| Multi-browser | ✅ | Chrome, Firefox, Edge |
| CI/CD Ready | ✅ | GitHub Actions, Jenkins templates |

---

## 💡 Important Concepts

### LoginFlow - Singleton Pattern
```java
// Login happens ONCE per session
LoginFlow loginFlow = LoginFlow.getInstance();

// Check if already logged in
if (LoginFlow.isLoggedIn()) {
    return; // Skip re-login
}

// Perform login (UI or API)
loginFlow.loginViaUI(username, password);
loginFlow.loginViaAPI(username, password);

// Get token for subsequent API calls
String token = LoginFlow.getSessionToken();

// Continue in other flows without re-login
loginFlow.continueWithSession();

// Logout when done
loginFlow.logout();
```

### Page Object Model
```java
// Centralized locators
private static final By USERNAME = By.xpath("//input[@name='username']");

// Reusable methods
public void login(String user, String pass) {
    enterUsername(user);
    enterPassword(pass);
    clickLoginButton();
}

// Used in step definitions
loginPage.login(username, password);
```

### Cucumber BDD
```gherkin
Feature: User Login
  Scenario: Login with valid credentials
    Given User has valid credentials
    When User logs in via API with valid credentials
    Then Login should be successful
```

---

## ⚙️ Configuration Options

**Key Properties** (in `application.properties`):

```properties
# App Settings
environment=dev                    # dev, staging, prod
base.url=http://localhost:4200     # UI URL
api.base.url=http://localhost:8080 # API URL

# Browser
browser=chrome                     # chrome, firefox, edge
headless=false
browser.maximize=true

# Waits
implicit.wait=10                   # seconds
explicit.wait=15                   # seconds
page.load.timeout=30               # seconds

# Authentication
api.auth.username=testuser
api.auth.password=testpassword

# Reporting
report.path=test-output/
screenshot.on.fail=true
screenshot.on.pass=false

# Logging
log.level=INFO
log.path=logs/
```

---

## 📊 Test Statistics

| Element | Count |
|---------|-------|
| Java Classes | 11 |
| Step Definitions | 30+ |
| Test Scenarios | 12+ |
| Feature Files | 1 |
| Page Objects | 1 |
| Flows | 1 |
| Config Files | 4 |
| Documentation Files | 4 |

---

## 🏆 What's Unique About This Framework

1. **LoginFlow Singleton**: Login once, reuse session across multiple flows
2. **Session Persistence**: @PersistSession tag maintains session across scenarios
3. **Production-Ready**: Comprehensive error handling, logging, reporting
4. **Centralized Config**: All properties in one place with convenient getters
5. **Multi-Mode**: UI automation, API testing, BDD all integrated
6. **Secure**: Token masking, credential isolation, no hardcoded passwords
7. **Extensible**: Clear patterns for adding new flows (CitizenRegistration, Appointment, etc.)

---

## 🔗 File Dependencies

```
LoginFlow
  ├─ Depends on: LoginPage, LoginApiTest, ConfigManager
  ├─ Used by: LoginStepDefinition, TestHooks
  └─ Feeds: Other flows (CitizenRegistration, Appointment, etc.)

LoginPage
  ├─ Depends on: WebElementUtil, ScreenshotUtil, DriverManager
  ├─ Used by: LoginFlow, LoginStepDefinition
  └─ Implements: POM pattern with centralized locators

LoginApiTest
  ├─ Depends on: ApiClient, ConfigManager
  ├─ Used by: LoginFlow, LoginStepDefinition
  └─ Handles: Authentication API operations

TestHooks
  ├─ Depends on: DriverManager, ApiClient, ScreenshotUtil
  └─ Triggers: Before/After every scenario
```

---

## 📋 Typical Test Execution Flow

```
1. Feature File (Login.feature)
   ↓
2. Cucumber TestRunner
   ↓
3. Before Hook (TestHooks)
   ├─ Initialize API client
   └─ Reset state
   ↓
4. Step Definition (LoginStepDefinition)
   ├─ Parse feature file step
   ├─ Call appropriate method
   ├─ Use Page Object or Flow
   ├─ Perform assertion (Then step)
   └─ Capture screenshot
   ↓
5. After Hook (TestHooks)
   ├─ Capture screenshot if failed
   ├─ Close WebDriver
   ├─ Reset API client
   └─ Generate logs
   ↓
6. Report Generation
   ├─ HTML report
   ├─ JSON (for CI/CD)
   ├─ Screenshots
   └─ Logs
```

---

## 🎓 Learning Path

### Beginner
1. Read: README.md
2. Run: `mvn test`
3. View: `target/cucumber-reports/cucumber.html`
4. Understand: LoginFlow session management

### Intermediate
1. Study: LoginPage (POM pattern)
2. Study: LoginApiTest (API testing)
3. Study: LoginStepDefinition (BDD)
4. Modify: Login.feature scenarios

### Advanced
1. Follow: EXTENDING_FRAMEWORK.md
2. Create: New Flow (e.g., CitizenRegistration)
3. Create: Associated Page Objects
4. Create: Feature File with Scenarios
5. Run: New tests and integrate

---

## 🆘 Troubleshooting Quick Fixes

| Problem | Solution |
|---------|----------|
| WebDriver fails | `mvn test -Dbrowser=chrome` |
| Tests timeout | `mvn test -Dimplicit.wait=20` |
| Port in use | Change in `application.properties` |
| Permission denied | `chmod -R 755 target/` |
| No reports | Check `target/cucumber-reports/` |
| API fails | Ensure backend running on port 8080 |

---

## 📞 Documentation Map

| Document | Purpose | Audience |
|----------|---------|----------|
| README.md | Framework overview & setup | Everyone |
| EXECUTION_GUIDE.md | How to run tests | Test Engineers |
| EXTENDING_FRAMEWORK.md | Add new flows | Developers |
| FRAMEWORK_SUMMARY.md | What's implemented | Managers |
| This File | Quick reference | Everyone |

---

## 🚀 Getting Started NOW

```bash
# 1. Navigate to automation folder
cd automation

# 2. Install dependencies
mvn clean install

# 3. Run smoke tests
mvn test -Dcucumber.filter.tags="@Smoke"

# 4. View reports
# Open: target/cucumber-reports/cucumber.html
```

That's it! Your framework is running. 🎉

---

## 💾 Important Directories

```
logs/                          # Test logs
target/cucumber-reports/       # HTML reports & screenshots
target/json-reports/           # JSON reports for CI/CD
build/                         # Build artifacts
src/test/resources/testdata/   # Test data files
```

---

**Version**: 1.0.0  
**Last Updated**: April 28, 2026  
**Status**: ✅ Production Ready  
**Framework**: MeghaConnect Automation v1.0.0
