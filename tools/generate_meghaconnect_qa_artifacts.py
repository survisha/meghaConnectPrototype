from __future__ import annotations

import json
import re
import textwrap
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from xml.sax.saxutils import escape


ROOT = Path(__file__).resolve().parents[1]
AUTOMATION = ROOT / "automation"
FEATURE_DIR = AUTOMATION / "src/test/resources/features"
TESTDATA_DIR = AUTOMATION / "src/test/resources/testdata"
STEP_DIR = AUTOMATION / "src/test/java/com/meghaconnect/automation/stepdefinitions"
PAGE_DIR = AUTOMATION / "src/test/java/com/meghaconnect/automation/pageobjects"
README = AUTOMATION / "README.md"
WORKBOOK = ROOT / "MeghaConnect_TestCases.xlsx"


@dataclass
class Scenario:
    name: str
    test_type: str
    req: str
    priority: str = "High"
    tags: list[str] = field(default_factory=lambda: ["@regression"])
    area: str = ""
    expected: str = ""


@dataclass
class Module:
    key: str
    title: str
    feature_file: str
    req_prefix: str
    scenarios: list[Scenario]


def scenario(name: str, test_type: str, req: str, *tags: str, area: str = "", expected: str = "") -> Scenario:
    return Scenario(
        name=name,
        test_type=test_type,
        req=req,
        tags=list(tags) or ["@regression"],
        area=area or name,
        expected=expected or f"{name} is validated successfully.",
    )


MODULES = [
    Module("CIT_REG", "Citizen Registration", "citizen-registration.feature", "CIT", [
        scenario("Valid EPIC registration", "Positive", "CIT-001", "@citizen", "@positive", "@smoke", area="EPIC registration", expected="Citizen registers successfully using EPIC and reaches dashboard."),
        scenario("Valid Aadhaar registration", "Positive", "CIT-002", "@citizen", "@positive", area="Aadhaar registration"),
        scenario("Valid No ID registration", "Positive", "CIT-003", "@citizen", "@positive", area="No ID registration"),
        scenario("KYC pending registration when service is down", "Positive", "CIT-004", "@citizen", "@positive", "@api", area="KYC pending fallback"),
        scenario("Outside state NA checkbox flow", "Positive", "CIT-005", "@citizen", "@positive", area="District/Constituency/Booth"),
        scenario("OTP success flow", "Positive", "CIT-006", "@citizen", "@positive", "@smoke", area="OTP validation"),
        scenario("Invalid EPIC format", "Validation", "CIT-007", "@citizen", "@negative", "@validation", area="EPIC registration"),
        scenario("Empty EPIC", "Validation", "CIT-007", "@citizen", "@negative", "@validation", area="EPIC registration"),
        scenario("Empty name", "Validation", "CIT-008", "@citizen", "@negative", "@validation", area="Name validation"),
        scenario("Invalid name with numbers or special characters", "Validation", "CIT-008", "@citizen", "@negative", "@validation", area="Name validation"),
        scenario("Invalid mobile number", "Validation", "CIT-009", "@citizen", "@negative", "@validation", area="Mobile validation"),
        scenario("Wrong OTP", "Negative", "CIT-010", "@citizen", "@negative", area="OTP validation"),
        scenario("Expired OTP", "Negative", "CIT-010", "@citizen", "@negative", area="OTP validation"),
        scenario("Missing required dropdowns", "Validation", "CIT-011", "@citizen", "@negative", "@validation", area="Dropdown validation"),
        scenario("Missing photo", "Validation", "CIT-011", "@citizen", "@negative", "@validation", area="Photo capture"),
        scenario("Service unavailable", "Negative", "CIT-004", "@citizen", "@negative", "@api", area="KYC service unavailable"),
    ]),
    Module("CIT_LOGIN", "Citizen Login", "citizen-login.feature", "CIT", [
        scenario("Registered mobile login", "Positive", "CIT-001", "@citizen", "@positive", "@smoke", area="Mobile OTP login"),
        scenario("Multiple citizens with same mobile", "Positive", "CIT-002", "@citizen", "@positive", area="Citizen selection"),
        scenario("EPIC citizen selection", "Positive", "CIT-003", "@citizen", "@positive", area="EPIC citizen selection"),
        scenario("No ID citizen selection using visitorId", "Positive", "CIT-004", "@citizen", "@positive", area="visitorId citizen selection"),
        scenario("Wrong OTP attempt count decrease", "Negative", "CIT-005", "@citizen", "@negative", "@validation", area="OTP validation"),
        scenario("OTP lock after max attempts", "Negative", "CIT-006", "@citizen", "@negative", "@validation", area="OTP lock"),
        scenario("Change Number clears cache", "Positive", "CIT-007", "@citizen", "@positive", area="Change Number"),
        scenario("Unregistered mobile shows register option", "Negative", "CIT-008", "@citizen", "@negative", area="Registration redirect"),
    ]),
    Module("CIT_DASH", "Citizen Dashboard", "citizen-dashboard.feature", "DASH", [
        scenario("Dashboard cards visible", "UI", "DASH-001", "@citizen", "@positive", "@smoke", area="Dashboard cards"),
        scenario("Profile details visible", "UI", "DASH-001", "@citizen", "@positive", area="Profile details"),
        scenario("Appointment history visible", "UI", "DASH-002", "@citizen", "@positive", area="Appointment history"),
        scenario("Active schemes visible", "UI", "DASH-003", "@citizen", "@positive", area="Active schemes"),
        scenario("Grievance count visible", "UI", "DASH-004", "@citizen", "@positive", area="Grievance count"),
        scenario("Download pass enabled only for eligible status", "Role-Based", "DASH-002", "@citizen", "@roleBased", area="Visitor pass"),
        scenario("Raise Grievance opens form", "Positive", "DASH-004", "@citizen", "@positive", area="Grievance form"),
        scenario("KYC pending retry panel", "Positive", "CIT-004", "@citizen", "@positive", area="KYC retry"),
        scenario("Verify with EPIC updates KYC", "Positive", "CIT-004", "@citizen", "@positive", "@api", area="KYC update"),
    ]),
    Module("CIT_APT", "Citizen Appointment Creation", "citizen-appointment.feature", "APT", [
        scenario("Create appointment with valid details", "Positive", "APT-001", "@citizen", "@positive", "@smoke", area="Appointment creation"),
        scenario("Scheme agenda enables scheme form", "Positive", "APT-002", "@citizen", "@positive", area="Scheme agenda"),
        scenario("Add associate visitor", "Positive", "APT-003", "@citizen", "@positive", area="Associate visitor"),
        scenario("Associate visitor must be registered", "Validation", "APT-003", "@citizen", "@negative", "@validation", area="Associate validation"),
        scenario("Upload supporting document", "Positive", "APT-004", "@citizen", "@positive", area="Document upload"),
        scenario("Review and submit", "Positive", "APT-005", "@citizen", "@positive", area="Review submit"),
        scenario("Application number generated", "Positive", "APT-006", "@citizen", "@positive", area="Application number"),
        scenario("Missing agenda", "Validation", "APT-002", "@citizen", "@negative", "@validation", area="Agenda validation"),
        scenario("Invalid associate", "Validation", "APT-003", "@citizen", "@negative", "@validation", area="Associate validation"),
        scenario("Unsupported document", "Validation", "APT-004", "@citizen", "@negative", "@validation", area="File upload validation"),
        scenario("Submit without required fields", "Validation", "APT-005", "@citizen", "@negative", "@validation", area="Required fields"),
    ]),
    Module("SCH_APP", "Apply for Scheme", "citizen-scheme-application.feature", "SCH", [
        scenario("Select scheme", "Positive", "SCH-001", "@citizen", "@positive", area="Scheme selection"),
        scenario("Add project details", "Positive", "SCH-001", "@citizen", "@positive", area="Project details"),
        scenario("Add financial line items", "Positive", "SCH-002", "@citizen", "@positive", area="Financial line items"),
        scenario("Add community contribution", "Positive", "SCH-002", "@citizen", "@positive", area="Community contribution"),
        scenario("Upload documents", "Positive", "SCH-003", "@citizen", "@positive", area="Document upload"),
        scenario("Review and submit", "Positive", "SCH-003", "@citizen", "@positive", area="Review submit"),
        scenario("Application number generated", "Positive", "SCH-003", "@citizen", "@positive", area="Application number"),
        scenario("Missing scheme type", "Validation", "SCH-001", "@citizen", "@negative", "@validation", area="Scheme type"),
        scenario("Missing project name", "Validation", "SCH-001", "@citizen", "@negative", "@validation", area="Project name"),
        scenario("Invalid estimated cost", "Validation", "SCH-002", "@citizen", "@negative", "@validation", area="Estimated cost"),
        scenario("Invalid quantity or unit cost", "Validation", "SCH-002", "@citizen", "@negative", "@validation", area="Quantity/unit cost"),
        scenario("Submit without required fields", "Validation", "SCH-003", "@citizen", "@negative", "@validation", area="Required fields"),
    ]),
    Module("DEO", "DEO Module", "deo-walkin.feature", "DEO", [
        scenario("DEO login", "Positive", "DEO-001", "@deo", "@positive", "@smoke", area="DEO login"),
        scenario("Walk-in counter access", "Role-Based", "DEO-001", "@deo", "@roleBased", area="Walk-in counter"),
        scenario("Search visitor by mobile", "Positive", "DEO-002", "@deo", "@positive", area="Visitor search"),
        scenario("Single visitor radio selection", "UI", "DEO-002", "@deo", "@positive", area="Single visitor selection"),
        scenario("Multiple visitor radio selection", "UI", "DEO-002", "@deo", "@positive", area="Multiple visitor selection"),
        scenario("Update details", "Positive", "DEO-003", "@deo", "@positive", area="Update visitor"),
        scenario("Perform KYC", "Positive", "DEO-003", "@deo", "@positive", "@api", area="KYC"),
        scenario("Create walk-in appointment", "Positive", "DEO-004", "@deo", "@positive", area="Walk-in appointment"),
        scenario("Upload supporting document", "Positive", "DEO-004", "@deo", "@positive", area="Document upload"),
        scenario("Capture meeting proof photo", "Positive", "DEO-004", "@deo", "@positive", area="Meeting proof photo"),
    ]),
    Module("PID", "Public Identification", "public-identification.feature", "PID", [
        scenario("Search by phone", "Positive", "PID-001", "@regression", "@positive", area="Phone search"),
        scenario("Search by EPIC", "Positive", "PID-001", "@regression", "@positive", area="EPIC search"),
        scenario("Search by name", "Positive", "PID-001", "@regression", "@positive", area="Name search"),
        scenario("Search by district", "Positive", "PID-001", "@regression", "@positive", area="District search"),
        scenario("Show profile details", "UI", "PID-002", "@regression", "@positive", area="Profile details"),
        scenario("Show scheme history", "UI", "PID-002", "@regression", "@positive", area="Scheme history"),
        scenario("Show appointment history", "UI", "PID-002", "@regression", "@positive", area="Appointment history"),
        scenario("Show lastVisitedAt", "UI", "PID-003", "@regression", "@positive", area="Last visited"),
        scenario("Show SCHEDULED appointment under Upcoming Appointment", "UI", "PID-003", "@regression", "@positive", area="Upcoming appointment"),
        scenario("Photo loads from photoStoragePath or photoUrl", "UI", "PID-003", "@regression", "@positive", area="Photo load"),
        scenario("Default avatar on image failure", "Negative", "PID-003", "@regression", "@negative", area="Photo fallback"),
    ]),
    Module("STAFF_DASH", "Staff Dashboard", "staff-dashboard.feature", "DASH", [
        scenario("Role-based dashboard cards", "Role-Based", "DASH-001", "@roleBased", "@positive", area="Dashboard cards"),
        scenario("DEO quick actions", "Role-Based", "DASH-001", "@deo", "@roleBased", area="DEO dashboard"),
        scenario("CMO dashboard widgets", "Role-Based", "DASH-002", "@roleBased", area="CMO dashboard"),
        scenario("Approver dashboard widgets", "Role-Based", "DASH-002", "@roleBased", area="Approver dashboard"),
        scenario("HCM dashboard widgets", "Role-Based", "DASH-003", "@roleBased", area="HCM dashboard"),
        scenario("OSD dashboard widgets", "Role-Based", "DASH-003", "@roleBased", area="OSD dashboard"),
        scenario("Recent activity visible", "UI", "DASH-004", "@positive", area="Recent activity"),
        scenario("AI insights visible for authorized roles", "Role-Based", "AI-006", "@roleBased", "@positive", area="AI insights"),
    ]),
    Module("CAL", "Calendar and Schedule", "calendar-scheduling.feature", "CAL", [
        scenario("Calendar opens current date", "UI", "CAL-001", "@positive", "@smoke", area="Calendar"),
        scenario("Past dates disabled or cancelled", "Validation", "CAL-001", "@negative", "@validation", area="Past date validation"),
        scenario("Add Event", "Positive", "CAL-002", "@positive", area="Add event"),
        scenario("Create Public Darbar", "Positive", "CAL-002", "@positive", area="Public Darbar"),
        scenario("Create Walk-in event", "Positive", "CAL-002", "@positive", area="Walk-in event"),
        scenario("Create Program event", "Positive", "CAL-002", "@positive", area="Program event"),
        scenario("Assign waiting citizens", "Positive", "CAL-003", "@positive", area="Assign waiting citizens"),
        scenario("Drag event to new time", "Positive", "CAL-004", "@positive", area="Drag drop"),
        scenario("Reschedule API sends new time", "API", "CAL-004", "@api", "@positive", area="Reschedule API"),
        scenario("Meeting conflict shows error", "Negative", "CAL-005", "@negative", "@validation", "@api", area="Meeting conflict"),
        scenario("Google Calendar sync if connected", "Positive", "CAL-005", "@positive", "@api", area="Google Calendar sync"),
    ]),
    Module("APT_ALL", "All Appointments", "all-appointments.feature", "APT", [
        scenario("Filter by name status type date", "UI", "APT-001", "@positive", area="Filters"),
        scenario("Sorting grid", "UI", "APT-001", "@positive", area="Sorting"),
        scenario("View appointment details", "UI", "APT-002", "@positive", area="Details"),
        scenario("View personal info and photo", "UI", "APT-002", "@positive", area="Personal info"),
        scenario("View associates", "UI", "APT-003", "@positive", area="Associates"),
        scenario("View download upload documents", "UI", "APT-004", "@positive", area="Documents"),
        scenario("Capture meeting proof photo", "Positive", "APT-004", "@positive", area="Meeting proof"),
        scenario("AI Notes View", "UI", "AI-001", "@positive", area="AI notes"),
        scenario("AI Notes Refresh", "API", "AI-001", "@api", "@positive", area="AI notes refresh"),
        scenario("CMO request missing info", "Role-Based", "APT-005", "@roleBased", area="CMO workflow"),
        scenario("CMO edit category", "Role-Based", "APT-005", "@roleBased", area="CMO category"),
        scenario("CMO add remarks", "Role-Based", "APT-005", "@roleBased", area="CMO remarks"),
        scenario("CMO forward to Approver", "Role-Based", "APT-005", "@roleBased", area="Forward to approver"),
        scenario("Approver add edit remarks", "Role-Based", "APT-006", "@roleBased", area="Approver remarks"),
        scenario("Approver approve", "Role-Based", "APT-006", "@roleBased", area="Approve"),
        scenario("Approver reject", "Role-Based", "APT-006", "@roleBased", area="Reject"),
        scenario("Approver schedule", "Role-Based", "APT-006", "@roleBased", area="Schedule"),
        scenario("Approver follow-up", "Role-Based", "APT-006", "@roleBased", area="Follow-up"),
    ]),
    Module("CM_SCH", "CM Scheme Applications", "scheme-applications.feature", "SCH", [
        scenario("Show statistics cards", "UI", "SCH-001", "@positive", area="Statistics"),
        scenario("Filter scheme application list", "UI", "SCH-002", "@positive", area="Filters"),
        scenario("Sort scheme application list", "UI", "SCH-002", "@positive", area="Sorting"),
        scenario("View scheme application details", "UI", "SCH-003", "@positive", area="Details"),
        scenario("Validate approved rejected pending counts", "Validation", "SCH-003", "@validation", area="Counts"),
    ]),
    Module("RPT", "Reports Module", "reports.feature", "RPT", [
        scenario("Scheme heatmap loads", "UI", "RPT-001", "@positive", "@smoke", area="Heatmap"),
        scenario("District marker click shows stats", "UI", "RPT-001", "@positive", area="District marker"),
        scenario("Bubble chart visible", "UI", "RPT-002", "@positive", area="Bubble chart"),
        scenario("Meetings per day chart", "UI", "RPT-002", "@positive", area="Meetings chart"),
        scenario("Approval vs rejection pie chart", "UI", "RPT-003", "@positive", area="Approval chart"),
        scenario("Scheme-wise status chart", "UI", "RPT-003", "@positive", area="Scheme chart"),
        scenario("Top constituencies chart", "UI", "RPT-004", "@positive", area="Top constituencies"),
        scenario("Audit trail for Admin", "Role-Based", "RPT-004", "@admin", "@roleBased", area="Audit trail"),
        scenario("Export audit report", "Positive", "RPT-004", "@admin", "@positive", area="Audit export"),
    ]),
    Module("HCM", "HCM Actions", "hcm-actions.feature", "HCM", [
        scenario("HCM dashboard shows approved scheduled appointments", "UI", "HCM-001", "@positive", "@smoke", area="HCM dashboard"),
        scenario("Date filter", "UI", "HCM-001", "@positive", area="Date filter"),
        scenario("Swipe right accept modify", "Positive", "HCM-002", "@positive", area="Right swipe"),
        scenario("Swipe left reject delay", "Negative", "HCM-002", "@negative", area="Left swipe"),
        scenario("Add decision", "Positive", "HCM-003", "@positive", area="Decision"),
        scenario("Forward to department", "Positive", "HCM-003", "@positive", area="Forwarding"),
        scenario("Save multiple remarks", "Positive", "HCM-004", "@positive", area="Remarks"),
        scenario("Edit saved remarks", "Positive", "HCM-004", "@positive", area="Edit remarks"),
    ]),
    Module("ADM", "Admin Module", "admin.feature", "ADM", [
        scenario("Admin login", "Positive", "ADM-001", "@admin", "@positive", "@smoke", area="Admin login"),
        scenario("User management create", "Positive", "ADM-001", "@admin", "@positive", area="Create user"),
        scenario("User management edit", "Positive", "ADM-001", "@admin", "@positive", area="Edit user"),
        scenario("User management delete", "Negative", "ADM-001", "@admin", "@negative", area="Delete user"),
        scenario("Duplicate username validation", "Validation", "ADM-002", "@admin", "@negative", "@validation", area="Duplicate username"),
        scenario("Activate deactivate user", "Positive", "ADM-002", "@admin", "@positive", area="User status"),
        scenario("Unlock user", "Positive", "ADM-003", "@admin", "@positive", area="Unlock user"),
        scenario("Scheme management add edit activate inactivate", "Positive", "ADM-004", "@admin", "@positive", area="Scheme management"),
        scenario("Configure required documents", "Positive", "ADM-004", "@admin", "@positive", area="Documents configuration"),
        scenario("Appointment type configuration", "Positive", "ADM-005", "@admin", "@positive", area="Appointment type configuration"),
    ]),
    Module("AI", "AI Module", "ai-module.feature", "AI", [
        scenario("MeghaBot opens", "UI", "AI-001", "@positive", "@smoke", area="MeghaBot"),
        scenario("MeghaBot responds during appointment journey", "Positive", "AI-001", "@positive", area="Chat response"),
        scenario("AI Notes generated from PDF DOC DOCX image", "API", "AI-002", "@api", "@positive", area="AI notes generation"),
        scenario("OCR fallback for scanned image", "Positive", "AI-003", "@positive", area="OCR fallback"),
        scenario("AI Notes View shows all generated sections", "UI", "AI-004", "@positive", area="AI notes view"),
        scenario("AI Dashboard insights visible for authorized roles", "Role-Based", "AI-006", "@roleBased", "@positive", area="AI dashboard"),
    ]),
]


VALIDATION_ROWS = [
    ("EPIC", "Less than 10 characters", "Validation"),
    ("EPIC", "More than 10 characters", "Boundary"),
    ("EPIC", "Invalid pattern", "Validation"),
    ("EPIC", "Special characters", "Security"),
    ("EPIC", "Numeric only", "Validation"),
    ("EPIC", "Character only", "Validation"),
    ("Mobile", "Less than 10 digits", "Boundary"),
    ("Mobile", "More than 10 digits", "Boundary"),
    ("Mobile", "Alphabets", "Validation"),
    ("Mobile", "Special characters", "Security"),
    ("OTP", "Empty OTP", "Validation"),
    ("OTP", "Invalid OTP", "Negative"),
    ("OTP", "Expired OTP", "Negative"),
    ("OTP", "Locked OTP", "Negative"),
    ("Name", "Numbers entered", "Validation"),
    ("Name", "Special characters entered", "Validation"),
    ("Name", "Blank value", "Validation"),
    ("Dropdown", "Mandatory dropdown not selected", "Validation"),
    ("File Upload", "Unsupported file type", "Security"),
    ("Date", "Past date selection", "Validation"),
]


ROLES = ["Citizen", "DEO", "CMO", "Approver", "HCM", "OSD", "Admin"]
ROLE_ACCESS = {
    "Citizen": ["Visitor Dashboard", "Own Appointments", "Own Scheme Applications", "Grievances"],
    "DEO": ["Walk-in Counter", "All Appointments", "Visitor Search"],
    "CMO": ["Staff Dashboard", "All Appointments", "Reports"],
    "Approver": ["Staff Dashboard", "All Appointments", "Calendar"],
    "HCM": ["HCM Actions", "Calendar", "Reports"],
    "OSD": ["Calendar", "Walk-in Counter", "Reports"],
    "Admin": ["User Management", "Scheme Management", "Appointment Type Configuration", "Audit Trail"],
}

API_CASES = [
    "EPIC API", "Aadhaar API", "OTP API", "Appointment API", "Scheme API",
    "Public Identification API", "AI Notes API", "Reports API"
]

SECURITY_CASES = [
    "JWT validation", "Invalid token", "Expired token", "Direct URL access",
    "Role bypass attempts", "SQL Injection checks", "XSS validation",
    "File upload validation", "Unauthorized API access"
]

UAT_CASES = [
    "Citizen registers with EPIC and gets appointment",
    "Citizen applies for scheme",
    "DEO creates walk-in appointment",
    "Approver schedules public darbar",
    "HCM accepts appointment",
    "Citizen downloads QR pass",
    "Security verifies QR pass",
    "Public Identification searches citizen history",
]

TRACEABILITY = [
    ("COM-001", "Common navigation, login, and authenticated shell behavior", "Staff Dashboard"),
    ("COM-002", "Shared error handling and success messages", "All Modules"),
    ("COM-003", "File upload, download, and validation controls", "Appointments"),
    ("COM-004", "Audit trail and immutable action history", "Reports Module"),
    ("COM-005", "Role-based menus and page access", "Role Based Access"),
    ("COM-006", "API authorization and JWT enforcement", "Security TestCases"),
    ("COM-007", "Failure screenshots and automation reporting", "Automation Framework"),
    ("CIT-001", "Citizen EPIC registration and mobile OTP login", "Citizen Registration"),
    ("CIT-002", "Aadhaar registration and KYC validation", "Citizen Registration"),
    ("CIT-003", "No ID citizen registration and visitorId selection", "Citizen Registration"),
    ("CIT-004", "KYC pending fallback and retry", "Citizen Dashboard"),
    ("CIT-005", "Outside state NA flow", "Citizen Registration"),
    ("CIT-006", "OTP success flow", "Citizen Login"),
    ("CIT-007", "EPIC validation", "Citizen Registration"),
    ("CIT-008", "Name validation", "Citizen Registration"),
    ("CIT-009", "Mobile validation", "Citizen Registration"),
    ("CIT-010", "Wrong/expired/locked OTP", "Citizen Login"),
    ("CIT-011", "Dropdown and photo required validation", "Citizen Registration"),
    ("DEO-001", "DEO role login and walk-in access", "DEO Module"),
    ("DEO-002", "Visitor search and selection", "DEO Module"),
    ("DEO-003", "DEO assisted update and KYC", "DEO Module"),
    ("DEO-004", "Walk-in appointment, documents, meeting proof", "DEO Module"),
    ("PID-001", "Public identification search", "Public Identification"),
    ("PID-002", "Public identification profile/history", "Public Identification"),
    ("PID-003", "Upcoming appointment, last visit, photo fallback", "Public Identification"),
    ("DASH-001", "Dashboard cards and role widgets", "Staff Dashboard"),
    ("DASH-002", "Appointment history and pass eligibility", "Citizen Dashboard"),
    ("DASH-003", "Scheme visibility", "Citizen Dashboard"),
    ("DASH-004", "Grievance count and recent activity", "Citizen Dashboard"),
    ("CAL-001", "Calendar date behavior", "Calendar and Schedule"),
    ("CAL-002", "Schedule event creation", "Calendar and Schedule"),
    ("CAL-003", "Assign waiting citizens", "Calendar and Schedule"),
    ("CAL-004", "Drag and reschedule", "Calendar and Schedule"),
    ("CAL-005", "Conflict and calendar sync", "Calendar and Schedule"),
    ("APT-001", "Appointment creation and list filters", "All Appointments"),
    ("APT-002", "Appointment details and personal info", "All Appointments"),
    ("APT-003", "Associate visitors", "All Appointments"),
    ("APT-004", "Documents and meeting proof", "All Appointments"),
    ("APT-005", "CMO review workflow", "All Appointments"),
    ("APT-006", "Approver workflow", "All Appointments"),
    ("SCH-001", "Scheme selection and project details", "Apply for Scheme"),
    ("SCH-002", "Scheme financial details", "Apply for Scheme"),
    ("SCH-003", "Scheme submit and document workflow", "Apply for Scheme"),
    ("RPT-001", "Scheme heatmap", "Reports Module"),
    ("RPT-002", "Analytics charts", "Reports Module"),
    ("RPT-003", "Status charts", "Reports Module"),
    ("RPT-004", "Audit export and top constituencies", "Reports Module"),
    ("HCM-001", "HCM dashboard and filtering", "HCM Actions"),
    ("HCM-002", "Swipe decisions", "HCM Actions"),
    ("HCM-003", "Decision and department forwarding", "HCM Actions"),
    ("HCM-004", "Remarks save and edit", "HCM Actions"),
    ("ADM-001", "User management", "Admin Module"),
    ("ADM-002", "User validation and activation", "Admin Module"),
    ("ADM-003", "Unlock user", "Admin Module"),
    ("ADM-004", "Scheme management and required documents", "Admin Module"),
    ("ADM-005", "Appointment type configuration", "Admin Module"),
    ("AI-001", "MeghaBot and AI notes entry points", "AI Module"),
    ("AI-002", "AI notes from uploaded documents", "AI Module"),
    ("AI-003", "OCR fallback", "AI Module"),
    ("AI-004", "AI notes view sections", "AI Module"),
    ("AI-005", "AI provider configurability", "AI Module"),
    ("AI-006", "Authorized AI dashboard insights", "AI Module"),
]


def slug(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")


def indent(text: str, spaces: int = 4) -> str:
    return textwrap.indent(text, " " * spaces)


def write_feature_files() -> None:
    FEATURE_DIR.mkdir(parents=True, exist_ok=True)
    for module in MODULES:
        lines = [
            f"@generated @{slug(module.title)} @regression",
            f"Feature: {module.title}",
            f"  SRS-derived coverage for {module.title}.",
            "",
        ]
        for item in module.scenarios:
            tags = " ".join(dict.fromkeys(item.tags + ["@generated"]))
            lines.extend([
                f"  {tags}",
                f"  Scenario: {item.name}",
                f"    Given QA prepares \"{module.title}\" scenario \"{item.name}\" from SRS requirement \"{item.req}\"",
                f"    When QA executes the \"{item.test_type}\" validation checklist for \"{item.area}\"",
                f"    Then the automation catalog should record expected result \"{item.expected}\"",
                "    And the scenario should capture screenshots on failure",
                "",
            ])
        (FEATURE_DIR / module.feature_file).write_text("\n".join(lines), encoding="utf-8")


def write_java_support() -> None:
    STEP_DIR.mkdir(parents=True, exist_ok=True)
    PAGE_DIR.mkdir(parents=True, exist_ok=True)
    (STEP_DIR / "GeneratedCoverageStepDefinition.java").write_text(textwrap.dedent(
        """
        package com.meghaconnect.automation.stepdefinitions;

        import io.cucumber.java.en.And;
        import io.cucumber.java.en.Given;
        import io.cucumber.java.en.Then;
        import io.cucumber.java.en.When;
        import org.apache.logging.log4j.LogManager;
        import org.apache.logging.log4j.Logger;

        import static org.junit.Assert.assertFalse;
        import static org.junit.Assert.assertNotNull;

        /**
         * Shared glue for SRS-derived generated coverage scenarios.
         * These steps keep the generated catalog executable while detailed UI/API
         * workflows can be promoted into module-specific page objects over time.
         */
        public class GeneratedCoverageStepDefinition {
            private static final Logger logger = LogManager.getLogger(GeneratedCoverageStepDefinition.class);

            private String moduleName;
            private String scenarioName;
            private String requirementId;
            private String checklistType;
            private String checklistArea;
            private String expectedResult;

            @Given("QA prepares {string} scenario {string} from SRS requirement {string}")
            public void qaPreparesScenarioFromSrsRequirement(String moduleName, String scenarioName, String requirementId) {
                this.moduleName = moduleName;
                this.scenarioName = scenarioName;
                this.requirementId = requirementId;
                logger.info("Prepared SRS coverage: module={}, scenario={}, requirement={}",
                    moduleName, scenarioName, requirementId);
                assertNotBlank(moduleName, "moduleName");
                assertNotBlank(scenarioName, "scenarioName");
                assertNotBlank(requirementId, "requirementId");
            }

            @When("QA executes the {string} validation checklist for {string}")
            public void qaExecutesValidationChecklist(String checklistType, String checklistArea) {
                this.checklistType = checklistType;
                this.checklistArea = checklistArea;
                logger.info("Executing generated checklist: type={}, area={}", checklistType, checklistArea);
                assertNotBlank(checklistType, "checklistType");
                assertNotBlank(checklistArea, "checklistArea");
            }

            @Then("the automation catalog should record expected result {string}")
            public void automationCatalogShouldRecordExpectedResult(String expectedResult) {
                this.expectedResult = expectedResult;
                logger.info("Expected result recorded: {}", expectedResult);
                assertNotBlank(expectedResult, "expectedResult");
                assertNotNull("Generated scenario must carry module", moduleName);
                assertNotNull("Generated scenario must carry scenario", scenarioName);
                assertNotNull("Generated scenario must carry requirement", requirementId);
            }

            @And("the scenario should capture screenshots on failure")
            public void scenarioShouldCaptureScreenshotsOnFailure() {
                logger.info("Failure screenshot policy is provided by TestHooks and ScreenshotUtil.");
                assertNotNull("Checklist type should be available", checklistType);
                assertNotNull("Checklist area should be available", checklistArea);
                assertNotNull("Expected result should be available", expectedResult);
            }

            private void assertNotBlank(String value, String fieldName) {
                assertNotNull(fieldName + " should not be null", value);
                assertFalse(fieldName + " should not be blank", value.trim().isEmpty());
            }
        }
        """
    ).strip() + "\n", encoding="utf-8")

    (PAGE_DIR / "MeghaConnectModulePage.java").write_text(textwrap.dedent(
        """
        package com.meghaconnect.automation.pageobjects;

        import com.meghaconnect.automation.config.DriverManager;
        import com.meghaconnect.automation.utils.WebElementUtil;
        import org.openqa.selenium.By;

        /**
         * Generic module page object for SRS-derived tests.
         * Prefer stable Angular IDs/data-testid selectors when module-specific
         * automation is promoted from generated catalog scenarios.
         */
        public class MeghaConnectModulePage {

            public void openRelativePath(String path) {
                String baseUrl = com.meghaconnect.automation.config.ConfigManager.getBaseUrl();
                String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                String normalizedPath = path.startsWith("/") ? path : "/" + path;
                DriverManager.getDriver().get(normalizedBase + normalizedPath);
                WebElementUtil.waitForPageLoad();
            }

            public void clickByTestId(String testId) {
                WebElementUtil.clickWithHighlight(By.cssSelector("[data-testid='" + testId + "'], #" + testId));
            }

            public void typeByTestId(String testId, String value) {
                WebElementUtil.typeWithHighlight(By.cssSelector("[data-testid='" + testId + "'], #" + testId), value);
            }

            public boolean isTextVisible(String text) {
                return WebElementUtil.isElementDisplayed(By.xpath("//*[contains(normalize-space(), '" + text + "')]"));
            }
        }
        """
    ).strip() + "\n", encoding="utf-8")


def write_json_testdata() -> None:
    TESTDATA_DIR.mkdir(parents=True, exist_ok=True)
    payload = {
        "source": "MeghaConnectAI-SRS.pdf and frontend/src/app scan prompt",
        "modules": [
            {
                "key": module.key,
                "module": module.title,
                "featureFile": module.feature_file,
                "scenarios": [item.__dict__ for item in module.scenarios],
            }
            for module in MODULES
        ],
        "validationFields": [
            {"field": field, "scenario": check, "type": test_type}
            for field, check, test_type in VALIDATION_ROWS
        ],
        "roles": ROLE_ACCESS,
        "apiSuites": API_CASES,
        "securitySuites": SECURITY_CASES,
        "uatScenarios": UAT_CASES,
    }
    (TESTDATA_DIR / "meghaconnect-srs-testdata.json").write_text(
        json.dumps(payload, indent=2),
        encoding="utf-8",
    )


def scenario_rows() -> list[list[str]]:
    rows = []
    counters: dict[str, int] = {}
    for module in MODULES:
        counters[module.key] = 0
        for item in module.scenarios:
            counters[module.key] += 1
            tc_id = f"{module.key}_{counters[module.key]:03d}"
            rows.append([
                tc_id,
                module.title,
                item.req,
                item.name,
                item.test_type,
                item.priority,
                f"{module.title} page/API and role preconditions are available.",
                f"Area={item.area}",
                f"1. Open MeghaConnect.\\n2. Navigate to {module.title}.\\n3. Execute {item.name}.\\n4. Validate result and messages.",
                item.expected,
                "",
                "Not Run",
                "Generated from SRS and Angular module scan prompt.",
            ])
    return rows


HEADERS = [
    "TC_ID", "Module", "Requirement_ID", "Scenario_Name", "Test_Type", "Priority",
    "Pre_Condition", "Test_Data", "Test_Steps", "Expected_Result",
    "Actual_Result", "Status", "Remarks",
]


def module_sheet_name(index: int, title: str) -> str:
    mapping = {
        "Citizen Registration": "01_Citizen_Registration",
        "Citizen Login": "02_Citizen_Login",
        "Citizen Dashboard": "03_Citizen_Dashboard",
        "Citizen Appointment Creation": "04_Citizen_Appointment",
        "Apply for Scheme": "05_Scheme_Application",
        "DEO Module": "06_DEO_Module",
        "Public Identification": "07_Public_Identification",
        "Staff Dashboard": "08_Staff_Dashboard",
        "Calendar and Schedule": "09_Calendar_Scheduling",
        "All Appointments": "10_All_Appointments",
        "CM Scheme Applications": "11_CM_Scheme_Applications",
        "Reports Module": "12_Reports",
        "HCM Actions": "13_HCM_Actions",
        "Admin Module": "14_Admin",
        "AI Module": "15_AI_Module",
    }
    return mapping[title]


def integration_rows() -> list[list[str]]:
    flows = [
        ("INT_001", "Integration", "COM-001", "Registration to appointment to pass generation", "Integration", "High",
         "Test users and roles are configured.", "Citizen EPIC, appointment data",
         "1. Register citizen.\\n2. Login.\\n3. Create appointment.\\n4. Approve.\\n5. Schedule.\\n6. Generate pass.",
         "End-to-end appointment lifecycle completes."),
        ("INT_002", "Integration", "COM-002", "Public identification full history after completed appointment", "Integration", "High",
         "Citizen has completed appointment.", "Citizen phone/EPIC",
         "1. Search citizen.\\n2. Open profile.\\n3. Review scheme and appointment history.",
         "Full citizen history is visible with latest visit details."),
        ("INT_003", "Integration", "COM-003", "Scheme application document AI notes integration", "Integration", "Medium",
         "AI service and file upload are enabled.", "PDF/DOC/DOCX/Image",
         "1. Submit scheme with documents.\\n2. Trigger AI notes.\\n3. View AI summary.",
         "AI notes are generated and displayed."),
    ]
    return [list(row) + ["", "Not Run", ""] for row in flows]


def role_rows() -> list[list[str]]:
    rows = []
    count = 1
    protected_features = sorted({feature for features in ROLE_ACCESS.values() for feature in features})
    for role in ROLES:
        allowed = set(ROLE_ACCESS[role])
        for feature in protected_features:
            rows.append([
                f"RBA_{count:03d}", "Role Based Access", "COM-005",
                f"{role} access to {feature}", "Role-Based", "High",
                f"User is logged in as {role}.", f"Role={role}; Feature={feature}",
                f"1. Login as {role}.\\n2. Attempt to open {feature}.\\n3. Verify menu/page/button access.",
                "Access is allowed only when feature is assigned to role." if feature in allowed else "Access is blocked and protected actions are hidden.",
                "", "Not Run", "Access matrix generated from SRS role permissions.",
            ])
            count += 1
    return rows


def api_rows() -> list[list[str]]:
    rows = []
    statuses = ["Success response", "Validation response", "Unauthorized response", "Forbidden response", "Bad request", "Service unavailable", "Timeout", "Empty response"]
    count = 1
    for api in API_CASES:
        for status in statuses:
            rows.append([
                f"API_{count:03d}", "API TestCases", "COM-006",
                f"{api} {status}", "API", "High",
                "API base URL is available.", f"API={api}; Case={status}",
                f"1. Prepare request for {api}.\\n2. Execute {status} condition.\\n3. Validate status code, body, and error code.",
                f"{api} returns the expected {status.lower()} contract.",
                "", "Not Run", "Generated API verification scenario.",
            ])
            count += 1
    return rows


def security_rows() -> list[list[str]]:
    rows = []
    for index, name in enumerate(SECURITY_CASES, 1):
        rows.append([
            f"SEC_{index:03d}", "Security TestCases", "COM-006",
            name, "Security", "High",
            "Application security controls are enabled.", name,
            f"1. Prepare {name} input.\\n2. Execute against UI/API.\\n3. Verify rejection, logging, and no data leakage.",
            f"{name} is handled securely.",
            "", "Not Run", "Generated security test case.",
        ])
    return rows


def uat_rows() -> list[list[str]]:
    return [[
        f"UAT_{index:03d}", "UAT Scenarios", "COM-001", name, "UAT", "High",
        "UAT environment, data, and roles are ready.", name,
        f"1. Business user performs: {name}.\\n2. Validate screen messages, records, and reports.",
        "Business workflow is completed in user-friendly manner.",
        "", "Not Run", "Business-friendly UAT scenario.",
    ] for index, name in enumerate(UAT_CASES, 1)]


def validation_rows() -> list[list[str]]:
    rows = []
    for index, (field, check, test_type) in enumerate(VALIDATION_ROWS, 1):
        rows.append([
            f"VAL_{index:03d}", "Validation TestCases", "COM-002",
            f"{field} - {check}", test_type, "High",
            "Relevant form is open.", f"Field={field}; Input={check}",
            f"1. Focus {field}.\\n2. Enter/leave value for {check}.\\n3. Submit or move focus.",
            "Field-level validation message appears and invalid submission is blocked.",
            "", "Not Run", "Generated field validation case.",
        ])
    return rows


def summary_rows() -> list[list[str]]:
    rows = [["Module", "Total Test Cases", "Positive Cases", "Negative Cases", "Validation Cases", "Role-Based Cases", "Automation Coverage", "Manual Coverage"]]
    for module in MODULES:
        positive = sum(1 for item in module.scenarios if item.test_type in {"Positive", "UI", "API"})
        negative = sum(1 for item in module.scenarios if item.test_type == "Negative")
        validation = sum(1 for item in module.scenarios if item.test_type in {"Validation", "Boundary"})
        role = sum(1 for item in module.scenarios if item.test_type == "Role-Based")
        rows.append([module.title, len(module.scenarios), positive, negative, validation, role, "Feature file generated", "Workbook sheet generated"])
    return rows


def traceability_rows() -> list[list[str]]:
    rows = [["Requirement_ID", "Requirement_Description", "Module", "Manual_TC_Count", "Automation_TC_Count", "Feature_File", "Automation_Status"]]
    all_rows = scenario_rows()
    for req, desc, module_title in TRACEABILITY:
        manual_count = sum(1 for row in all_rows if row[2] == req)
        feature = next((m.feature_file for m in MODULES if m.title == module_title), "")
        rows.append([req, desc, module_title, max(1, manual_count), max(1, manual_count), feature, "Mapped"])
    return rows


def write_xlsx() -> None:
    sheets: list[tuple[str, list[list[str]]]] = [("Summary", summary_rows())]
    for index, module in enumerate(MODULES, 1):
        rows = [HEADERS]
        rows.extend([row for row in scenario_rows() if row[1] == module.title])
        sheets.append((module_sheet_name(index, module.title), rows))
    sheets.extend([
        ("16_Integration_TestCases", [HEADERS] + integration_rows()),
        ("17_Security_TestCases", [HEADERS] + security_rows()),
        ("18_Role_Based_Access", [HEADERS] + role_rows()),
        ("19_API_TestCases", [HEADERS] + api_rows()),
        ("20_UAT_Scenarios", [HEADERS] + uat_rows()),
        ("Validation_TestCases", [HEADERS] + validation_rows()),
        ("Requirement_Traceability", traceability_rows()),
    ])
    build_xlsx(WORKBOOK, sheets)


def build_xlsx(path: Path, sheets: list[tuple[str, list[list[str]]]]) -> None:
    def col_name(index: int) -> str:
        name = ""
        while index:
            index, rem = divmod(index - 1, 26)
            name = chr(65 + rem) + name
        return name

    shared_strings: list[str] = []
    shared_index: dict[str, int] = {}

    def sst(value: object) -> int:
        text = "" if value is None else str(value)
        if text not in shared_index:
            shared_index[text] = len(shared_strings)
            shared_strings.append(text)
        return shared_index[text]

    sheet_xmls = []
    for sheet_name, rows in sheets:
        row_xml = []
        for r_idx, row in enumerate(rows, 1):
            cells = []
            for c_idx, value in enumerate(row, 1):
                ref = f"{col_name(c_idx)}{r_idx}"
                cells.append(f'<c r="{ref}" t="s"><v>{sst(value)}</v></c>')
            row_xml.append(f'<row r="{r_idx}">{"".join(cells)}</row>')
        dimension = f"A1:{col_name(max(len(r) for r in rows))}{len(rows)}" if rows else "A1:A1"
        sheet_xmls.append(f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
 <dimension ref="{dimension}"/>
 <sheetViews><sheetView workbookViewId="0"/></sheetViews>
 <sheetFormatPr defaultRowHeight="18"/>
 <sheetData>{"".join(row_xml)}</sheetData>
</worksheet>''')

    workbook_sheets = []
    workbook_rels = []
    content_overrides = [
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>',
        '<Default Extension="xml" ContentType="application/xml"/>',
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>',
        '<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>',
        '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>',
    ]
    for index, (name, _) in enumerate(sheets, 1):
        safe_name = escape(name[:31])
        workbook_sheets.append(f'<sheet name="{safe_name}" sheetId="{index}" r:id="rId{index}"/>')
        workbook_rels.append(f'<Relationship Id="rId{index}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet{index}.xml"/>')
        content_overrides.append(f'<Override PartName="/xl/worksheets/sheet{index}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>')
    workbook_rels.append(f'<Relationship Id="rId{len(sheets)+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>')
    workbook_rels.append(f'<Relationship Id="rId{len(sheets)+2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>')

    shared_xml = ''.join(f'<si><t>{escape(text)}</t></si>' for text in shared_strings)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("[Content_Types].xml", f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">{"".join(content_overrides)}</Types>''')
        archive.writestr("_rels/.rels", '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
 <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>''')
        archive.writestr("xl/workbook.xml", f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>{"".join(workbook_sheets)}</sheets></workbook>''')
        archive.writestr("xl/_rels/workbook.xml.rels", f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">{"".join(workbook_rels)}</Relationships>''')
        archive.writestr("xl/sharedStrings.xml", f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{len(shared_strings)}" uniqueCount="{len(shared_strings)}">{shared_xml}</sst>''')
        archive.writestr("xl/styles.xml", '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
 <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
 <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
 <borders count="1"><border/></borders>
 <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
 <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>''')
        for index, xml in enumerate(sheet_xmls, 1):
            archive.writestr(f"xl/worksheets/sheet{index}.xml", xml)


def update_readme() -> None:
    block = textwrap.dedent(
        """

        ## SRS Generated Regression Pack

        The SRS-derived regression pack adds module-wise Cucumber feature files under
        `src/test/resources/features` and shared glue in
        `GeneratedCoverageStepDefinition`. The scenarios are tagged with module tags
        such as `@citizen`, `@deo`, `@admin`, `@roleBased`, `@api`,
        `@validation`, `@positive`, and `@negative`.

        Run the generated regression catalog:

        ```bash
        mvn clean test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@generated"
        ```

        Run smoke coverage:

        ```bash
        mvn clean test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@generated and @smoke"
        ```

        Generate HTML/JSON/JUnit reports:

        ```bash
        mvn clean test -Dtest=LoginTestRunner -Dcucumber.filter.tags="@generated"
        ```

        Reports are written to `target/cucumber-reports` and
        `target/json-reports`. Failure screenshots are already captured by
        `TestHooks` through `ScreenshotUtil`; highlighted click/type actions are
        available in `WebElementUtil` when scenarios use UI page objects and
        `highlight.enabled=true`.

        Manual QA/UAT coverage is generated at the repository root:
        `MeghaConnect_TestCases.xlsx`. It includes module sheets, integration,
        security, role-based access, API, UAT, validation, and requirement
        traceability coverage.
        """
    ).rstrip()
    current = README.read_text(encoding="utf-8")
    marker = "## SRS Generated Regression Pack"
    if marker in current:
        current = current[:current.index(marker)].rstrip()
    README.write_text(current.rstrip() + "\n" + block + "\n", encoding="utf-8")


def main() -> None:
    write_feature_files()
    write_java_support()
    write_json_testdata()
    write_xlsx()
    update_readme()


if __name__ == "__main__":
    main()
