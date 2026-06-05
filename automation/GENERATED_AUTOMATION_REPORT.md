# Generated Automation Scan Report

## Phase 1 - Existing Framework

- Framework type: Selenium WebDriver with Cucumber BDD and REST Assured.
- Language: Java 17.
- Cucumber version: 7.14.1.
- Existing runners: `LoginTestRunner`, `UatTestRunner`.
- Existing hooks: `TestHooks` handles API reset, WebDriver setup, failure screenshots, optional pass/step screenshots.
- Existing utilities: `WebElementUtil`, `ScreenshotUtil`, `ExcelUtil`.
- Existing report config: Cucumber HTML/JSON/JUnit plugins and Extent Spark configuration.
- Existing test data readers: JSON and Excel through framework utilities.

## Phase 2 - Feature Scan

- Existing feature files found: `Login.feature`, `LoginAPI.feature`, `LoginUI.feature`, `CitizenLogin.feature`.
- Generated feature files found: 15 SRS module feature files.
- Reused steps: generated SRS catalog steps in `GeneratedCoverageStepDefinition`; login steps in `LoginStepDefinition`; citizen login steps in `CitizenLoginStepDefinition`.
- Duplicate steps avoided: module-specific steps use unique `QA ...` phrases and do not shadow existing login or generated catalog steps.
- Missing generated-feature steps: none for the SRS catalog after shared glue.

## Phase 3 - Angular Locator Scan

Locator sources scanned from `frontend/src/app` include IDs, placeholders, button text, Material labels, error banners, snackbars, dialogs, and form bindings.

Stable locator priority used:

1. `data-testid`
2. `id`
3. `formControlName`
4. `aria-label`
5. `placeholder`
6. button text
7. CSS selector
8. XPath fallback

## Phase 4 - Angular `data-testid` Updates

Added stable `data-testid` attributes to high-value controls in:

- `public-login.component.html`
- `visitor-register.component.html`
- `scheduling.component.html`
- `appointment-form.component.html`
- `ai-chatbot.component.html`

No UI styling or business logic was changed.

## Phase 5 - Page Objects Created

- `MessageCenterPage`
- `CitizenRegistrationPage`
- `CitizenDashboardPage`
- `DeoWalkinPage`
- `PublicIdentificationPage`
- `SchedulingPage`
- `AppointmentPage`
- `SchemeApplicationPage`
- `ReportsPage`
- `AdminPage`
- `AiModulePage`
- `MeghaConnectModulePage`

## Phase 6 - Validation Messages

- Java catalog: `ValidationMessages.java`
- JSON catalog: `validation-messages.json`

Messages were extracted from Angular error banners, computed validation getters, snackbars, dialogs, and explicit error strings.

## Phase 7/8 - Validation Scenarios and Assertions

Added validation scenarios to generated feature files for EPIC, mobile, name, district, appointment agenda, and scheduling conflict behavior.

Reusable assertion steps:

- `Then user should see validation message "<message>"`
- `Then user should not see validation message "<message>"`
- `Then user should see message containing "<message>"`
- `Then user should not see message containing "<message>"`

When WebDriver is initialized, these steps assert against visible UI messages. When the generated catalog runs without UI, they validate the expected text against the extracted message catalog.

## Phase 9 - Toast/Snackbar/Dialog Handling

`MessageCenterPage` reads:

- `mat-error`
- Angular Material snackbar containers
- `.error-msg`, `.error-banner`, `.status-msg`, `.warning-msg`
- `[role='alert']`
- dialog/modal containers

## Phase 10 - Test Data

Generated:

- `test-data.json`
- `validation-messages.json`
- Existing `meghaconnect-srs-testdata.json` retained.

Role passwords are referenced through environment variable names and are not hardcoded.

## Phase 11 - XPath Support

XPath remains fallback-only. New page objects prefer `data-testid` and IDs.

## Phase 12 - Reporting

Failure screenshots remain handled by `TestHooks` and `ScreenshotUtil`. Highlighted click/type actions remain handled by `WebElementUtil`.

## Phase 13 - Execution Commands

Smoke:

```bash
mvn test -Dtest=LoginTestRunner '-Dcucumber.filter.tags=@generated and @smoke'
```

Validation:

```bash
mvn test -Dtest=LoginTestRunner '-Dcucumber.filter.tags=@generated and @validation'
```

Generated catalog:

```bash
mvn test -Dtest=LoginTestRunner '-Dcucumber.filter.tags=@generated'
```
