package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class AppointmentPage extends MeghaConnectModulePage {
    public static final By AGENDA_SELECT = By.cssSelector("[data-testid='appointment-agenda-type-select'], #appointment_agendaTypeSelect");
    public static final By LOCATION_SELECT = By.cssSelector("[data-testid='appointment-location-select'], #appointment_requestedLocationSelect");
    public static final By AGENDA_BRIEF_INPUT = By.cssSelector("[data-testid='appointment-agenda-brief-input'], #appointment_agendaBriefInput");
    public static final By ERROR_MESSAGE = By.cssSelector("[data-testid='appointment-error-message'], #appointment_errorMsg");

    public void open() {
        openRelativePath("/appointments/new");
    }

    public void enterAgendaBrief(String value) {
        WebElementUtil.typeWithHighlight(AGENDA_BRIEF_INPUT, value);
    }
}
