package com.meghaconnect.automation.stepdefinitions;

import com.meghaconnect.automation.pageobjects.AdminPage;
import io.cucumber.java.en.Given;

public class AdminSteps {
    private final AdminPage page = new AdminPage();

    @Given("QA opens Admin User Management page")
    public void qaOpensAdminUserManagementPage() {
        page.openUsers();
    }

    @Given("QA opens Admin Appointment Types page")
    public void qaOpensAdminAppointmentTypesPage() {
        page.openAppointmentTypes();
    }
}
