from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import html


OUT = Path("Public_Login_Test_Scenarios.xlsx")


def col_name(index: int) -> str:
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


def cell_xml(row: int, col: int, value: object, style: int = 0) -> str:
    ref = f"{col_name(col)}{row}"
    text = html.escape("" if value is None else str(value), quote=False)
    style_attr = f' s="{style}"' if style else ""
    return f'<c r="{ref}" t="inlineStr"{style_attr}><is><t xml:space="preserve">{text}</t></is></c>'


def sheet_xml(rows: list[list[object]]) -> str:
    body = []
    for r_idx, row in enumerate(rows, 1):
        cells = [cell_xml(r_idx, c_idx, value, 1 if r_idx == 1 else 0) for c_idx, value in enumerate(row, 1)]
        body.append(f'<row r="{r_idx}">{"".join(cells)}</row>')
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
  <cols>
    <col min="1" max="1" width="16" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="26" customWidth="1"/>
    <col min="5" max="5" width="38" customWidth="1"/>
    <col min="6" max="6" width="44" customWidth="1"/>
    <col min="7" max="7" width="62" customWidth="1"/>
    <col min="8" max="8" width="62" customWidth="1"/>
    <col min="9" max="9" width="12" customWidth="1"/>
  </cols>
  <sheetData>{"".join(body)}</sheetData>
</worksheet>'''


headers = [
    "Scenario ID",
    "Module / Step",
    "Case Type",
    "Area",
    "Preconditions",
    "Test Data",
    "Test Steps",
    "Expected Result",
    "Priority",
]


scenarios = [
    ["PL-UI-P01", "Page Load", "Positive", "Branding and layout", "User navigates to /public-login", "N/A", "Open public login page.", "MeghaConnect branding, government logo, citizen portal copy, language selector, login card, and AI chatbot are visible.", "Medium"],
    ["PL-UI-P02", "Page Load", "Positive", "Default step", "User navigates to /public-login", "N/A", "Observe initial screen.", "Step is Enter Mobile. Mobile input, Generate OTP button, Register as New Visitor button, and Staff Login link are displayed.", "High"],
    ["PL-UI-P03", "Page Load", "Positive", "Translated labels", "Translation files are available", "Change language from language selector", "Switch language.", "Static labels use translated text and page remains on the same login step.", "Medium"],
    ["PL-UI-P04", "Page Load", "Positive", "AI chatbot", "Public login page loaded", "N/A", "Open the chatbot widget.", "AI chatbot is available for visitor assistance without blocking login inputs.", "Low"],
    ["PL-MOB-P01", "Enter Mobile", "Positive", "Valid mobile OTP generation", "User is on Enter Mobile step and mobile is registered uniquely", "phoneNumber = 9876543210", "Enter 10 digit mobile and click Generate OTP.", "Generate OTP API is called with phoneNumber, success message appears, and user moves to Enter OTP step.", "High"],
    ["PL-MOB-P02", "Enter Mobile", "Positive", "Enter key on mobile field", "User is on Enter Mobile step", "phoneNumber = 9876543210", "Enter valid mobile and press Enter.", "Same behavior as clicking Generate OTP: mobile is checked and OTP is requested.", "High"],
    ["PL-MOB-P03", "Enter Mobile", "Positive", "Mobile sanitization", "User is on Enter Mobile step", "Input = 98765abc43210", "Type mixed alphanumeric value in mobile field.", "Non-digits are removed and value is limited to 10 digits.", "High"],
    ["PL-MOB-P04", "Enter Mobile", "Positive", "Clear messages on input", "An error/warning/success message is visible", "Change mobile number", "Edit the mobile field.", "Error, warning, success, OTP, EPIC, registration options, and selected visitor state are cleared.", "Medium"],
    ["PL-MOB-P05", "Enter Mobile", "Positive", "SMS OTP delivery", "Generate OTP API returns success after SMS gateway accepts request", "phoneNumber = 9876543210", "Generate OTP for a valid registered mobile.", "Success message says OTP was sent to the entered mobile and user moves to OTP step. OTP value is not displayed on screen.", "High"],
    ["PL-MOB-P06", "Enter Mobile", "Positive", "SMS retry after resend", "User is on OTP step and SMS gateway is available", "same phoneNumber", "Click Resend OTP.", "A new OTP SMS request is sent and success message confirms OTP delivery without exposing the OTP value.", "Medium"],
    ["PL-MOB-N01", "Enter Mobile", "Negative", "Blank mobile", "User is on Enter Mobile step", "phoneNumber blank", "Click Generate OTP.", "Valid 10 digit mobile error is shown and API is not called.", "High"],
    ["PL-MOB-N02", "Enter Mobile", "Negative", "Short mobile", "User is on Enter Mobile step", "phoneNumber = 98765", "Click Generate OTP.", "Valid 10 digit mobile error is shown and user stays on Enter Mobile step.", "High"],
    ["PL-MOB-N03", "Enter Mobile", "Negative", "Long pasted mobile", "User is on Enter Mobile step", "phoneNumber = 987654321099", "Paste long mobile number.", "Field is limited to 10 digits. OTP request uses only the 10 digit sanitized value.", "Medium"],
    ["PL-MOB-N04", "Enter Mobile", "Negative", "Alphabetic mobile", "User is on Enter Mobile step", "phoneNumber = abcdefghij", "Type alphabetic value and click Generate OTP.", "Input becomes blank after sanitization and valid 10 digit mobile error is shown.", "High"],
    ["PL-MOB-N05", "Enter Mobile", "Negative", "Generate OTP API failure", "User enters valid mobile", "HTTP error from /visitor/auth/generate-otp", "Click Generate OTP.", "Loading stops, error message from API utility is shown, and user remains on Enter Mobile step.", "High"],
    ["PL-MOB-N06", "Enter Mobile", "Negative", "Generate OTP unsuccessful", "User enters valid mobile", "success=false, requiresEpic=false", "Click Generate OTP.", "Failure message is shown and user remains on Enter Mobile step.", "High"],
    ["PL-MOB-N07", "Enter Mobile", "Negative", "SMS gateway failure", "User enters valid mobile and backend cannot send SMS", "SMS gateway timeout/rejected response", "Click Generate OTP.", "Gateway failure message is shown, OTP step is not opened, and user can retry after the issue is resolved.", "High"],
    ["PL-MULTI-P01", "Multiple Registrations", "Positive", "Requires EPIC branch", "Mobile is linked to multiple registrations", "generate-otp returns requiresEpic=true or code=MULTIPLE_REGISTRATIONS_FOUND", "Enter mobile and click Generate OTP.", "Warning message is shown, search-registrations API is called, registration choice panel appears.", "High"],
    ["PL-MULTI-P02", "Multiple Registrations", "Positive", "Display registration options", "search-registrations returns multiple options", "2+ visitor records with fullName, maskedEpicNumber, district, constituency", "Trigger multiple registrations branch.", "Radio options show visitor name, masked EPIC, district, and constituency where available.", "High"],
    ["PL-MULTI-P03", "Multiple Registrations", "Positive", "Select registration", "Registration options are visible", "Select visitorId from list", "Choose one registration radio option.", "Selected visitor ID is stored, EPIC is normalized to uppercase internally, messages are cleared, Generate OTP is enabled.", "High"],
    ["PL-MULTI-P04", "Multiple Registrations", "Positive", "Generate OTP for selected registration", "Registration option selected", "selected epicNumber = abc1234567", "Click Generate OTP.", "Generate OTP request includes phoneNumber and selected EPIC in uppercase.", "High"],
    ["PL-MULTI-P05", "Multiple Registrations", "Positive", "Single option auto-select", "search-registrations returns exactly one option", "registrationCount=1", "Trigger registration search.", "The only registration is auto-selected and user can continue without manual radio selection.", "Medium"],
    ["PL-MULTI-N01", "Multiple Registrations", "Negative", "No registration selected", "requiresEpic=true and options are visible", "selectedVisitorId = null", "Click Generate OTP.", "Warning message asks user to select registration and OTP request is not sent.", "High"],
    ["PL-MULTI-N02", "Multiple Registrations", "Negative", "No registration options found", "search-registrations returns empty registrations", "registrations=[]", "Trigger registration search.", "Account not found/register message is shown and no selection is available.", "High"],
    ["PL-MULTI-N03", "Multiple Registrations", "Negative", "Search registrations API failure", "Generate OTP response requires EPIC", "HTTP error from /visitor/auth/search-registrations", "Trigger registration search.", "Unable to load registrations error is shown and loading stops.", "High"],
    ["PL-MULTI-N04", "Multiple Registrations", "Negative", "Mobile changed after options load", "Registration options are visible", "Edit phoneNumber", "Change any digit in mobile field.", "Requires EPIC flag, EPIC value, options, selected visitor, OTP, and messages are reset.", "High"],
    ["PL-OTP-P01", "Enter OTP", "Positive", "OTP step layout", "OTP was generated successfully", "phoneNumber = 9876543210", "Observe OTP step.", "Success message, phone display, OTP input, Verify Login button, Change Number, and Resend OTP are visible.", "High"],
    ["PL-OTP-P02", "Enter OTP", "Positive", "Valid OTP login", "OTP step open and backend validates OTP", "otp = valid 6 digit OTP", "Enter OTP and click Verify Login.", "validateOtp is called, visitor session is stored with phone/fullName/token/visitorId, and user navigates to /visitor.", "High"],
    ["PL-OTP-P03", "Enter OTP", "Positive", "Enter key on OTP field", "OTP step open", "otp = valid 6 digit OTP", "Enter OTP and press Enter.", "Same behavior as clicking Verify Login.", "High"],
    ["PL-OTP-P04", "Enter OTP", "Positive", "OTP validation with selected EPIC", "OTP step reached from multiple registration branch", "selectedEpic exists", "Enter valid OTP and verify.", "OTP validation request includes phoneNumber, OTP, and selected EPIC.", "High"],
    ["PL-OTP-P05", "Enter OTP", "Positive", "Resend OTP", "OTP step open", "same phoneNumber and selected EPIC if any", "Click Resend OTP.", "Previous messages are cleared, Generate OTP API is called again, and OTP success message is refreshed.", "Medium"],
    ["PL-OTP-P06", "Enter OTP", "Positive", "Change number", "OTP step open", "N/A", "Click Change Number.", "User returns to Enter Mobile step, OTP and all visible messages are cleared, mock OTP is cleared.", "High"],
    ["PL-OTP-N01", "Enter OTP", "Negative", "Blank OTP", "OTP step open", "otp blank", "Click Verify Login.", "Valid 6 digit OTP error is shown and validation API is not called.", "High"],
    ["PL-OTP-N02", "Enter OTP", "Negative", "Short OTP", "OTP step open", "otp = 12345", "Click Verify Login.", "Valid 6 digit OTP error is shown and user remains on OTP step.", "High"],
    ["PL-OTP-N03", "Enter OTP", "Negative", "Long OTP pasted", "OTP step open", "otp = 1234567", "Paste OTP into field.", "Input is limited to 6 characters by maxlength; verification should use 6 characters only.", "Medium"],
    ["PL-OTP-N04", "Enter OTP", "Negative", "Non-numeric OTP", "OTP step open", "otp = abcdef", "Enter non-numeric 6 characters and verify.", "Current UI length check allows attempt; backend should reject and OTP verification failure is shown.", "High"],
    ["PL-OTP-N05", "Enter OTP", "Negative", "Invalid OTP", "OTP step open", "otp = wrong 6 digit OTP", "Click Verify Login.", "OTP verification failed message is shown and user remains on OTP step.", "High"],
    ["PL-OTP-N06", "Enter OTP", "Negative", "Expired OTP", "OTP step open", "expired OTP", "Enter expired OTP and verify.", "Backend expiry message is shown and user can resend OTP.", "High"],
    ["PL-OTP-N07", "Enter OTP", "Negative", "OTP API requires EPIC", "OTP validation response requiresEpic=true", "otp valid length", "Click Verify Login.", "requiresEpic flag is set and error message is shown; user should go back and select registration.", "High"],
    ["PL-OTP-N08", "Enter OTP", "Negative", "Validate OTP API failure", "OTP step open", "HTTP error from validateOtp", "Click Verify Login.", "Loading stops and API error message is shown.", "High"],
    ["PL-OTP-N09", "Enter OTP", "Negative", "Double click verify while loading", "OTP step open and request in progress", "otp valid length", "Click Verify Login repeatedly.", "Button is disabled while loading, preventing duplicate validation requests.", "Medium"],
    ["PL-NAV-P01", "Navigation", "Positive", "Register as new visitor", "User is on Enter Mobile step", "N/A", "Click Register as New Visitor.", "User navigates to /register-visitor.", "High"],
    ["PL-NAV-P02", "Navigation", "Positive", "Staff login link", "User is on Enter Mobile step", "N/A", "Click Staff Login link.", "User navigates to /login.", "High"],
    ["PL-NAV-P03", "Navigation", "Positive", "Successful visitor dashboard route", "OTP validation succeeds", "visitorId returned", "Complete OTP login.", "User is routed to /visitor dashboard.", "High"],
    ["PL-SEC-N01", "Security / Session", "Negative", "Session not created on OTP failure", "OTP validation fails", "invalid OTP", "Submit invalid OTP.", "No visitor session is stored and user remains on public login.", "High"],
    ["PL-SEC-N02", "Security / Session", "Negative", "Token missing on success", "OTP validation success but token missing", "success=true, token blank", "Verify OTP.", "Session helper is called with blank token; verify whether route guard/dashboard handles missing token correctly.", "Medium"],
    ["PL-SEC-N03", "Security / Session", "Negative", "Multiple registration tampering", "Registration options loaded", "selectedVisitorId changed to non-existing value", "Attempt to continue through manipulated state.", "selectedRegistration is undefined, warning is shown, and OTP is not generated for an unlisted registration.", "High"],
    ["PL-ACCESS-P01", "Accessibility", "Positive", "Keyboard flow", "Public login page loaded", "N/A", "Tab through controls and use Enter on mobile/OTP inputs.", "Controls are reachable by keyboard and Enter triggers the expected action.", "Medium"],
    ["PL-ACCESS-P02", "Accessibility", "Positive", "Loading feedback", "API call in progress", "N/A", "Generate OTP or Verify OTP.", "Button text changes to Checking/Verifying with spinner icon and relevant action button is disabled.", "Medium"],
    ["PL-RESP-P01", "Responsive", "Positive", "Mobile viewport", "Open page on mobile viewport", "360px width", "Load public login page and use login flow.", "Left/right layout adapts, login card remains usable, and no text/control overlap occurs.", "Medium"],
    ["PL-RESP-P02", "Responsive", "Positive", "Desktop viewport", "Open page on desktop viewport", "1366px width", "Load public login page.", "Brand panel and login panel display correctly and controls remain aligned.", "Low"],
]


summary_rows = [
    ["Metric", "Count"],
    ["Total scenarios", len(scenarios)],
    ["Positive scenarios", sum(1 for row in scenarios if row[2] == "Positive")],
    ["Negative scenarios", sum(1 for row in scenarios if row[2] == "Negative")],
    ["Enter Mobile scenarios", sum(1 for row in scenarios if row[1] == "Enter Mobile")],
    ["Multiple Registration scenarios", sum(1 for row in scenarios if row[1] == "Multiple Registrations")],
    ["Enter OTP scenarios", sum(1 for row in scenarios if row[1] == "Enter OTP")],
    ["Navigation scenarios", sum(1 for row in scenarios if row[1] == "Navigation")],
]


styles = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/><color rgb="FFFFFFFF"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1F4E78"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment wrapText="1" vertical="top"/></xf>
    <xf numFmtId="0" fontId="1" fillId="1" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment wrapText="1" vertical="center"/></xf>
  </cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>'''


with ZipFile(OUT, "w", ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>''')
    z.writestr("_rels/.rels", '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>''')
    z.writestr("xl/workbook.xml", '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Test Scenarios" sheetId="1" r:id="rId1"/>
    <sheet name="Summary" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>''')
    z.writestr("xl/_rels/workbook.xml.rels", '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>''')
    z.writestr("xl/styles.xml", styles)
    z.writestr("xl/worksheets/sheet1.xml", sheet_xml([headers] + scenarios))
    z.writestr("xl/worksheets/sheet2.xml", sheet_xml(summary_rows))

print(f"Generated {OUT.resolve()} with {len(scenarios)} scenarios")
