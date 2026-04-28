# Test Automation Execution Guide

## 🚀 Quick Start

### Build and Run All Tests
```bash
# Clean build and run all tests
mvn clean test

# Build without running tests
mvn clean install -DskipTests

# Skip build, only run tests
mvn test -o
```

### Run Specific Test Runner
```bash
# Run Login tests only
mvn test -Dtest=LoginTestRunner

# Run with specific Cucumber tags
mvn test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@Smoke"
```

## 📊 Test Execution Scenarios

### 1. Smoke Testing
```bash
# Quick validation of critical functionality
mvn test -Dcucumber.filter.tags="@Smoke"

# Time: ~2-3 minutes
# Scenarios: 3-5
# Coverage: Login, basic flows
```

### 2. Regression Testing
```bash
# Complete test suite excluding known issues
mvn test -Dcucumber.filter.tags="not @Ignore and not @WIP"

# Time: ~15-20 minutes
# Scenarios: All non-ignored scenarios
# Coverage: All features
```

### 3. API Testing Only
```bash
# Test APIs without UI automation
mvn test -Dcucumber.filter.tags="@APITest"

# Time: ~5 minutes
# Scenarios: API validation scenarios
# Coverage: Backend endpoints
```

### 4. UI Testing Only
```bash
# Test UI without API calls
mvn test -Dcucumber.filter.tags="@UITest"

# Time: ~10-15 minutes
# Scenarios: UI interaction scenarios
# Coverage: Frontend workflows
```

### 5. Negative Testing
```bash
# Test error handling and edge cases
mvn test -Dcucumber.filter.tags="@Negative"

# Time: ~5 minutes
# Scenarios: Invalid input, error scenarios
# Coverage: Error handling
```

### 6. Login Flow Testing
```bash
# Test all login variations
mvn test -Dcucumber.filter.tags="@Login"

# Time: ~3-5 minutes
# Scenarios: Login success, failure, edge cases
# Coverage: Authentication
```

## 🌍 Environment-Specific Testing

### Development Environment
```bash
mvn test -Denvironment=dev

# Uses: http://localhost:4200 (UI), http://localhost:8080 (API)
# Browser: Chrome
# Headless: false
```

### Staging Environment
```bash
mvn test -Denvironment=staging

# Uses: https://staging.meghaconnect.com
# Browser: Chrome
# Headless: true
```

### Production Environment
```bash
# WARNING: Use with caution!
mvn test -Denvironment=prod -Dheadless=true

# Uses: https://meghaconnect.com
# Browser: Chrome
# Headless: true
# Screenshot: On failure only
```

## 🌐 Browser Configuration

### Chrome (Default)
```bash
mvn test -Dbrowser=chrome
mvn test -Dbrowser=chrome -Dheadless=true
```

### Firefox
```bash
mvn test -Dbrowser=firefox
mvn test -Dbrowser=firefox -Dheadless=true
```

### Edge
```bash
mvn test -Dbrowser=edge
mvn test -Dbrowser=edge -Dheadless=true
```

### Run Tests Across Multiple Browsers
```bash
# Using separate commands
for browser in chrome firefox edge; do
  mvn test -Dbrowser=$browser
done
```

## ⏱️ Parallel Execution

### Run Tests in Parallel
```bash
# Run with 4 threads
mvn test -DthreadCount=4

# Run profiles in parallel (requires maven-failsafe-plugin)
mvn test -Pparallel
```

### Parallel by Feature
```bash
# Each feature file runs in separate thread
mvn test -Dcucumber.execution.parallel.enabled=true
```

## 📈 Test Reporting

### Generate Reports After Test Run
```bash
# Reports are automatically generated during test execution
# Location: target/cucumber-reports/

# View HTML report
target/cucumber-reports/cucumber.html              # Open in browser
target/json-reports/cucumber.json                  # JSON format
target/cucumber-reports/cucumber.xml               # JUnit XML format
```

### Generate Report Manually
```bash
# Using Maven Cucumber Reporting Plugin
mvn test
# OR after test completion
mvn net.masterthought:maven-cucumber-reporting:generate
```

## 🔍 Debugging & Troubleshooting

### Enable Debug Logging
```bash
# Decrease log level to DEBUG
# Edit: src/test/resources/config/application.properties
log.level=DEBUG

# OR override: 
mvn test -Dlog.level=DEBUG
```

### Run Tests with Console Output
```bash
# Verbose output in console
mvn test -X

# Show Cucumber debug output
mvn test -Dcucumber.execution.dry-run=false
```

### Dry Run (Validate Features)
```bash
# Check feature file syntax and step definition mapping
mvn test -Dcucumber.execution.dry-run=true
```

### Run Single Scenario
```bash
# Edit Login.feature and add @FocusTest to specific scenario
@FocusTest
Scenario: Test this specific scenario only

# Then run:
mvn test -Dcucumber.filter.tags="@FocusTest"
```

### Increase Timeouts for Slow Environments
```bash
# Edit: src/test/resources/config/application.properties
implicit.wait=20
explicit.wait=25
page.load.timeout=45
api.request.timeout=60000

# OR override:
mvn test -Dimplicit.wait=20 -Dexplicit.wait=25
```

## 🔐 Credential Management

### Using Test Credentials
```bash
# Credentials are read from application.properties
api.auth.username=testuser
api.auth.password=testpassword

# Override at runtime:
mvn clean test -Dapi.auth.username=otheruser -Dapi.auth.password=otherpass
```

### Using Environment Variables
```bash
# Set environment variables before running tests
export API_AUTH_USERNAME=testuser
export API_AUTH_PASSWORD=testpassword

mvn test
```

### Using CI/CD Secrets (GitHub Actions, Jenkins, etc.)
```bash
# In GitHub Actions workflow
- name: Run Tests
  env:
    API_AUTH_USERNAME: ${{ secrets.TEST_USERNAME }}
    API_AUTH_PASSWORD: ${{ secrets.TEST_PASSWORD }}
  run: mvn test
```

## 📋 Test Execution Checklist

### Before Running Tests
- [ ] Java 17+ is installed (`java -version`)
- [ ] Maven 3.8+ is installed (`mvn --version`)
- [ ] Application is running (http://localhost:8080, http://localhost:4200)
- [ ] Browser drivers are configured (auto-downloaded by WebDriverManager)
- [ ] Test data is available in src/test/resources/testdata/
- [ ] Configuration is updated in application.properties

### During Test Execution
- [ ] Monitor console output for test progress
- [ ] Check for any warning/error messages
- [ ] Wait for test completion (don't interrupt)
- [ ] Note any failures for investigation

### After Test Execution
- [ ] Review test reports in target/cucumber-reports/
- [ ] Check screenshots of failed tests
- [ ] Review logs in logs/ directory
- [ ] Analyze failed scenarios
- [ ] Document issues/bugs found

## 📊 Test Metrics & Reporting

### Key Metrics
```
Total Scenarios:    15
Passed:             13
Failed:             1
Skipped:            1
Success Rate:       86.67%
Average Duration:   2.5 min
Failed Scenarios:   [Login_FAILED_SCENARIO_NAME]
```

### Report Integration
```bash
# Publish to Cucumber Cloud (optional)
# Enable in LoginTestRunner.java: publish=true

# Jenkins Integration
# Use: target/json-reports/cucumber.json in Jenkins

# CI/CD Pipeline
# Reports can be parsed and published to CI/CD dashboards
```

## 🔄 Continuous Integration Setup

### GitHub Actions Example
```yaml
name: Automation Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: 17
      - name: Run Tests
        run: mvn clean test -Dheadless=true
      - name: Upload Reports
        uses: actions/upload-artifact@v2
        if: always()
        with:
          name: test-reports
          path: target/cucumber-reports/
```

### Jenkins Pipeline Example
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test -Dheadless=true'
            }
        }
        stage('Report') {
            steps {
                publishHTML([
                    reportDir: 'target/cucumber-reports',
                    reportFiles: 'cucumber.html',
                    reportName: 'Cucumber Report'
                ])
            }
        }
    }
}
```

## 🗂️ Test Output Structure

```
project/
├── target/
│   ├── cucumber-reports/
│   │   ├── cucumber.html          # Main HTML report
│   │   ├── cucumber.xml           # JUnit XML
│   │   ├── screenshots/           # Test screenshots
│   │   │   ├── STEP_Username_Entered_2024-04-28_10-30-45.png
│   │   │   ├── LOGIN_FAILED_2024-04-28_10-31-20.png
│   │   │   └── ...
│   │   └── ...
│   └── json-reports/
│       └── cucumber.json          # JSON format for parsing
├── logs/
│   ├── automation.log             # Complete log
│   ├── error.log                  # Errors only
│   └── automation-rolling.log     # Rolled over daily
└── pom.xml
```

## 🆘 Command Quick Reference

```bash
# Basic execution
mvn test                                    # Run all tests
mvn test -Dtest=LoginTestRunner            # Run specific runner
mvn clean test                             # Clean build + test

# Tag-based execution
mvn test -Dcucumber.filter.tags="@Smoke"                    # Smoke tests
mvn test -Dcucumber.filter.tags="@APITest"                  # API only
mvn test -Dcucumber.filter.tags="not @Ignore"               # Exclude ignored
mvn test -Dcucumber.filter.tags="@Login and not @WIP"       # Multiple conditions

# Browser & Environment
mvn test -Dbrowser=chrome                  # Chrome
mvn test -Dheadless=true                   # Headless mode
mvn test -Denvironment=prod                # Production

# Debugging
mvn test -X                                 # Debug logging
mvn test -Dcucumber.execution.dry-run=true # Dry run
mvn test -DskipTests                       # Skip tests, build only

# Performance
mvn test -DthreadCount=4                   # Parallel execution
mvn test -o                                # Offline mode

# Reporting
mvn test -Dpublish=true                    # Publish to CI/CD
mvn net.masterthought:maven-cucumber-reporting:generate  # Manual report

# Combined examples
mvn clean test -Dbrowser=firefox -Dheadless=true -Dcucumber.filter.tags="@Smoke"
mvn test -Denvironment=staging -DthreadCount=4 -Dheadless=true
mvn test -X -Dtest=LoginTestRunner -Dcucumber.filter.tags="@FocusTest"
```

## 📞 Getting Help

### Common Issues & Solutions

**Issue**: WebDriver initialization fails
```bash
# Solution: Clear WebDriverManager cache
rm -rf ~/.cache/selenium

# Solution: Specify browser version
export WDM_CHROMEDRIVER_VERSION=latest
```

**Issue**: Tests timeout on slow network
```bash
# Solution: Increase timeouts
mvn test -Dimplicit.wait=20 -Dexplicit.wait=30 -Dpage.load.timeout=60
```

**Issue**: Port 8080 already in use
```bash
# Solution: Change application.properties
api.base.url=http://localhost:8081
```

**Issue**: Screenshot directory permission denied
```bash
# Solution: Check permissions
chmod -R 755 target/cucumber-reports/
```

---

**Last Updated**: April 28, 2026  
**Framework Version**: 1.0.0
