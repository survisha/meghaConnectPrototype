package com.meghaconnect.automation.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Cucumber Test Runner - Runs all Cucumber scenarios
 * Configures feature files, step definitions, plugins, and execution options
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        // Feature files location
        features = "src/test/resources/features",
        
        // Step definitions location
        glue = {
                "com.meghaconnect.automation.stepdefinitions",
                "com.meghaconnect.automation.hooks"
        },
        
        // Plugins for reporting
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/json-reports/cucumber.json",
                "junit:target/cucumber-reports/cucumber.xml"
        },
        
        // Tags to run (can be overridden with -Dcucumber.filter.tags)
        tags = "@LoginUI",
        
        // Execution options
        dryRun = false,                    // Set to true to validate features without execution
        monochrome = false                 // Set to true for monochrome output
)
public class LoginTestRunner {
    /**
     * Main test runner for Login feature
     * Execute with: mvn test -Dtest=LoginTestRunner
     * Execute with specific tags: mvn test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@Login and not @Ignore"
     * Execute dry run: mvn test -Dtest=LoginTestRunner -Dcucumber.filter.dryRun=true
     */
}
