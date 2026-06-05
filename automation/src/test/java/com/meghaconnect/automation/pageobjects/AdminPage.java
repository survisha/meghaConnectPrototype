package com.meghaconnect.automation.pageobjects;

public class AdminPage extends MeghaConnectModulePage {
    public void openUsers() {
        openRelativePath("/admin/users");
    }

    public void openAppointmentTypes() {
        openRelativePath("/admin/appointment-types");
    }
}
