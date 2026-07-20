package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.AdminPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class AdminSteps {
    private final AdminPage page = new AdminPage();

    @Given("Admin opens the MeghaConnect home page")
    public void adminOpensTheMeghaConnectHomePage() {
        page.openBaseUrl();
    }

    @When("Admin clicks the home Login button")
    public void adminClicksTheHomeLoginButton() {
        page.clickHomeLoginButton();
    }

    @When("Admin selects the Staff Login tab")
    public void adminSelectsTheStaffLoginTab() {
        page.selectStaffLoginTab();
    }

    @When("Admin signs in using admin credentials from test data")
    public void adminSignsInUsingAdminCredentialsFromTestData() {
        page.loginAsAdminFromTestData();
    }

    @Then("Admin dashboard header should display System Admin")
    public void adminDashboardHeaderShouldDisplaySystemAdmin() {
        page.verifyAdminDashboardHeader();
    }

    @Then("Admin logs out")
    public void adminLogsOut() {
        page.logout();
    }

    @Given("QA opens Admin User Management page")
    public void qaOpensAdminUserManagementPage() {
        page.openUsers();
    }

    @Given("QA opens Admin Appointment Types page")
    public void qaOpensAdminAppointmentTypesPage() {
        page.openAppointmentTypes();
    }
}
