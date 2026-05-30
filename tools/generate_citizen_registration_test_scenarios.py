from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import html


OUT = Path("Citizen_Registration_IDType_Test_Scenarios.xlsx")


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
    <col min="2" max="2" width="14" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="24" customWidth="1"/>
    <col min="5" max="5" width="32" customWidth="1"/>
    <col min="6" max="6" width="42" customWidth="1"/>
    <col min="7" max="7" width="58" customWidth="1"/>
    <col min="8" max="8" width="58" customWidth="1"/>
    <col min="9" max="9" width="12" customWidth="1"/>
  </cols>
  <sheetData>{"".join(body)}</sheetData>
</worksheet>'''


headers = [
    "Scenario ID",
    "ID Type",
    "Case Type",
    "Area / Step",
    "Preconditions",
    "Test Data",
    "Test Steps",
    "Expected Result",
    "Priority",
]


scenarios = [
    ["CR-EPIC-P01", "EPIC", "Positive", "ID selection", "Citizen is on New Visitor Registration page", "ID Type = EPIC", "Select EPIC / Voter ID radio option.", "EPIC number, visitor name, and mobile number fields are displayed. Aadhaar QR and No ID notices are hidden.", "High"],
    ["CR-EPIC-P02", "EPIC", "Positive", "EPIC input sanitization", "EPIC option selected", "epic = abc1234567", "Enter lowercase EPIC number.", "Value is converted to uppercase ABC1234567 and limited to 10 characters.", "Medium"],
    ["CR-EPIC-P03", "EPIC", "Positive", "Mobile input sanitization", "EPIC option selected", "mobile = 98765abc43210", "Enter alphanumeric mobile number.", "Only digits remain and field is limited to 10 digits.", "Medium"],
    ["CR-EPIC-P04", "EPIC", "Positive", "Mobile availability", "EPIC option selected and backend registration check available", "mobile = 9876543210, not registered", "Enter valid mobile and tab out of field.", "Mobile availability check runs and success validation message is shown.", "High"],
    ["CR-EPIC-P05", "EPIC", "Positive", "Generate OTP", "EPIC, name, and mobile are valid; mobile/EPIC not duplicate", "EPIC = ABC1234567, name = MAREIAM MOSSANG, mobile = 9876543210", "Click Generate OTP.", "EPIC verification succeeds, OTP is generated, user moves to OTP Verification step, masked phone is shown.", "High"],
    ["CR-EPIC-P06", "EPIC", "Positive", "OTP sanitization", "OTP field is visible", "OTP input = 12a34b56", "Enter mixed OTP value.", "Only numeric OTP 123456 remains and Verify/Next button enables when length is 6.", "Medium"],
    ["CR-EPIC-P07", "EPIC", "Positive", "OTP verification", "OTP sent for current mobile", "OTP = valid 6 digit code", "Enter OTP and click Verify/Next.", "OTP is verified and user proceeds to Photo Capture step.", "High"],
    ["CR-EPIC-P08", "EPIC", "Positive", "Resend OTP", "OTP sent but not verified", "mobile = same valid mobile", "Click Resend OTP.", "Previous OTP state is cleared, new OTP request is sent, success message is shown.", "Medium"],
    ["CR-EPIC-P09", "EPIC", "Positive", "Photo capture", "OTP verified and camera permission is available", "Face is centered and liveness check valid", "Open camera and capture photo.", "Live photo is captured, preview is shown, Continue to Details is enabled.", "High"],
    ["CR-EPIC-P10", "EPIC", "Positive", "Additional details submit", "Photo captured and KYC data populated from EPIC", "designation selected, district present, optional address/email/agenda filled", "Click Complete Registration.", "Registration payload includes EPIC number, KYC provider EPIC, live photo, location details, and success screen is shown.", "High"],
    ["CR-EPIC-N01", "EPIC", "Negative", "Required EPIC number", "EPIC option selected", "EPIC blank, name and mobile valid", "Try to generate OTP.", "Generate OTP remains disabled or invalid ID/name error is shown.", "High"],
    ["CR-EPIC-N02", "EPIC", "Negative", "Invalid EPIC format", "EPIC option selected", "EPIC = AB12345678 or ABC12345", "Enter invalid EPIC format.", "Generate OTP remains disabled because valid format is 3 letters followed by 7 digits.", "High"],
    ["CR-EPIC-N03", "EPIC", "Negative", "Required voter-card name", "EPIC option selected", "EPIC valid, name blank, mobile valid", "Try to generate OTP.", "Generate OTP remains disabled or invalid ID/name error is shown.", "High"],
    ["CR-EPIC-N04", "EPIC", "Negative", "Invalid mobile length", "EPIC option selected", "mobile = 98765", "Blur mobile field or click Generate OTP.", "Mobile validation error is shown and OTP generation is blocked.", "High"],
    ["CR-EPIC-N05", "EPIC", "Negative", "Duplicate mobile or EPIC", "Registration check service returns duplicate", "mobile/EPIC already registered", "Enter valid details and blur mobile or generate OTP.", "Duplicate registration message is shown and registration is blocked.", "High"],
    ["CR-EPIC-N06", "EPIC", "Negative", "EPIC name mismatch", "Backend EPIC API available", "Valid EPIC but mismatching visitor name", "Click Generate OTP.", "Name verification failure is shown and OTP is not sent.", "High"],
    ["CR-EPIC-N07", "EPIC", "Negative", "OTP incomplete", "OTP sent", "OTP = 12345", "Enter 5 digit OTP.", "Verify/Next button remains disabled.", "High"],
    ["CR-EPIC-N08", "EPIC", "Negative", "OTP invalid", "OTP sent", "OTP = invalid 6 digit code", "Enter OTP and click Verify/Next.", "OTP validation error is shown and user stays on OTP Verification step.", "High"],
    ["CR-EPIC-N09", "EPIC", "Negative", "Photo liveness failure", "OTP verified and camera active", "Face not centered / multiple faces / invalid liveness", "Attempt to capture photo.", "Capture button is disabled until liveness result is valid.", "Medium"],
    ["CR-EPIC-N10", "EPIC", "Negative", "Missing district on submit", "Photo captured and Additional Details open", "outside Meghalaya unchecked, district blank", "Click Complete Registration.", "District required error is shown and submission is blocked.", "High"],
    ["CR-AAD-P01", "AADHAAR", "Positive", "ID selection", "Citizen is on New Visitor Registration page", "ID Type = AADHAAR", "Select Aadhaar Card radio option.", "Generate QR button and Aadhaar QR notice are displayed. EPIC and No ID mobile fields are hidden.", "High"],
    ["CR-AAD-P02", "AADHAAR", "Positive", "Generate QR", "Aadhaar option selected and OVSE service available", "No Aadhaar number required on screen", "Click Generate QR.", "QR code is displayed, transaction ID is stored, polling starts, and verification pending countdown is shown.", "High"],
    ["CR-AAD-P03", "AADHAAR", "Positive", "QR verification success", "QR is displayed and user scans via Aadhaar app", "OVSE callback returns KYC payload", "Complete verification in Aadhaar app.", "Aadhaar KYC data is populated and user moves toward photo/details flow based on resident image availability.", "High"],
    ["CR-AAD-P04", "AADHAAR", "Positive", "Resident image available", "Aadhaar KYC response includes resident image", "resident image base64 present", "Complete Aadhaar QR verification.", "Captured photo URL is set from resident image and photo capture is skipped/treated as available.", "High"],
    ["CR-AAD-P05", "AADHAAR", "Positive", "Resident image not available", "Aadhaar KYC success but no resident image", "No image in response", "Proceed after Aadhaar verification.", "User is required to complete live photo capture before additional details.", "Medium"],
    ["CR-AAD-P06", "AADHAAR", "Positive", "Additional details with mobile", "Aadhaar KYC completed", "phoneNumber = 9876543210, designation selected, district filled", "Fill required remaining details and submit.", "Registration succeeds with KYC provider Aadhaar, Aadhaar reference/client transaction data, and live/resident photo.", "High"],
    ["CR-AAD-P07", "AADHAAR", "Positive", "Outside Meghalaya", "Additional Details step open", "outside Meghalaya checked", "Check Applicant Outside Meghalaya and submit with other required data.", "District, constituency, booth, village, and location are sent as NA and submission can proceed.", "Medium"],
    ["CR-AAD-N01", "AADHAAR", "Negative", "QR generation failure", "Aadhaar option selected", "OVSE returns unsuccessful response", "Click Generate QR.", "QR is not displayed and KYC Pending fallback is offered when applicable.", "High"],
    ["CR-AAD-N02", "AADHAAR", "Negative", "QR service unavailable", "Aadhaar option selected", "OVSE/API unavailable", "Click Generate QR.", "Service unavailable message is shown and Continue with KYC Pending option is displayed when fallback is allowed.", "High"],
    ["CR-AAD-N03", "AADHAAR", "Negative", "QR timeout", "QR displayed", "No scan or callback within polling window", "Wait until polling attempts exceed configured maximum.", "QR is hidden and timeout error is shown.", "High"],
    ["CR-AAD-N04", "AADHAAR", "Negative", "QR rejected or KYC failed", "QR displayed", "OVSE result contains error/errorCode", "User rejects or Aadhaar app returns failure.", "KYC failed message is shown and user remains on ID Entry.", "High"],
    ["CR-AAD-N05", "AADHAAR", "Negative", "Cancel QR", "QR displayed", "N/A", "Click Cancel.", "QR and polling state are cleared and Generate QR can be used again.", "Medium"],
    ["CR-AAD-N06", "AADHAAR", "Negative", "Invalid mobile in details", "Aadhaar Additional Details step open", "phoneNumber = 12345 or contains letters", "Enter invalid mobile and attempt submit.", "Phone input keeps only digits and should not allow invalid 10 digit mobile to pass backend validation.", "Medium"],
    ["CR-AAD-N07", "AADHAAR", "Negative", "Missing KYC before submit", "User reaches Additional Details without verifiedKycData by forced state/navigation", "verifiedKycData = null", "Click Complete Registration.", "Complete KYC before registration error is shown.", "High"],
    ["CR-AAD-N08", "AADHAAR", "Negative", "Missing live/resident photo", "Aadhaar KYC completed without resident image", "form.livePhoto blank", "Click Complete Registration.", "Please capture live photo error is shown and submission is blocked.", "High"],
    ["CR-NONE-P01", "NONE", "Positive", "ID selection", "Citizen is on New Visitor Registration page", "ID Type = No ID", "Select No ID radio option.", "Full Name and Mobile Number fields are displayed with KYC Pending notice.", "High"],
    ["CR-NONE-P02", "NONE", "Positive", "Mobile availability", "No ID selected", "fullName = Test Citizen, mobile = 9876543210, not duplicate", "Enter full name and mobile, then blur mobile.", "Mobile availability check runs and success validation message is shown.", "High"],
    ["CR-NONE-P03", "NONE", "Positive", "Continue with No ID", "No ID selected, valid name and mobile, no duplicate", "fullName = Test Citizen, mobile = 9876543210", "Click Continue.", "User proceeds with KYC_PENDING status and KYC provider NONE.", "High"],
    ["CR-NONE-P04", "NONE", "Positive", "OTP generation", "No ID flow after valid registration check", "valid mobile", "Continue from ID Entry.", "OTP is generated for registration mobile and user is moved to OTP Verification step.", "High"],
    ["CR-NONE-P05", "NONE", "Positive", "OTP verification", "OTP sent", "valid 6 digit OTP", "Enter OTP and click Next.", "OTP is verified and user proceeds to Photo Capture.", "High"],
    ["CR-NONE-P06", "NONE", "Positive", "Photo and details", "OTP verified", "valid live photo, designation, district", "Capture photo, continue to details, fill required fields, submit.", "Registration succeeds with KYC status Pending and success message instructing citizen to visit DEO with EPIC card.", "High"],
    ["CR-NONE-P07", "NONE", "Positive", "Outside Meghalaya", "No ID Additional Details step open", "outside Meghalaya checked", "Check outside Meghalaya and submit with valid photo/designation.", "Location fields are sent as NA and registration can complete.", "Medium"],
    ["CR-NONE-N01", "NONE", "Negative", "Required full name", "No ID selected", "fullName blank, mobile valid", "Click Continue.", "Continue button remains disabled or invalid ID/name error is shown.", "High"],
    ["CR-NONE-N02", "NONE", "Negative", "Required mobile", "No ID selected", "fullName valid, mobile blank", "Click Continue.", "Continue button remains disabled.", "High"],
    ["CR-NONE-N03", "NONE", "Negative", "Invalid mobile", "No ID selected", "mobile = 12345", "Blur mobile field or click Continue.", "Valid 10 digit mobile error is shown.", "High"],
    ["CR-NONE-N04", "NONE", "Negative", "Duplicate registration", "No ID selected and check-registration returns duplicate", "mobile already registered", "Enter mobile and blur/click Continue.", "Duplicate registration message is shown and Continue is blocked.", "High"],
    ["CR-NONE-N05", "NONE", "Negative", "OTP incomplete", "OTP sent", "OTP = 123", "Enter partial OTP.", "Next button remains disabled.", "High"],
    ["CR-NONE-N06", "NONE", "Negative", "OTP invalid", "OTP sent", "invalid 6 digit OTP", "Submit OTP.", "OTP validation error is shown and user stays on OTP Verification.", "High"],
    ["CR-NONE-N07", "NONE", "Negative", "Missing photo", "OTP verified and Additional Details reachable by forced state", "form.livePhoto blank", "Click Complete Registration.", "Please capture live photo error is shown.", "High"],
    ["CR-NONE-N08", "NONE", "Negative", "Missing district", "No ID details open, outside Meghalaya unchecked", "district blank", "Click Complete Registration.", "District required error is shown and submission is blocked.", "High"],
    ["CR-COM-P01", "All", "Positive", "Switch ID type reset", "Data entered for any ID type", "Switch EPIC to AADHAAR / NONE", "Change selected ID type.", "Previous ID, OTP, QR, photo, KYC, and validation state are cleared and user remains on ID Entry.", "Medium"],
    ["CR-COM-P02", "All", "Positive", "Back navigation", "User is on OTP, Photo, or Details step", "N/A", "Click Previous/Back controls.", "User returns to the expected previous step with visible errors cleared.", "Medium"],
    ["CR-COM-P03", "All", "Positive", "Optional fields", "Additional Details step open", "email, agenda type, brief description provided or blank", "Submit with optional fields filled and again with them blank.", "Optional fields are included when present and do not block registration when blank.", "Low"],
    ["CR-COM-N01", "All", "Negative", "Registration API failure", "All required data valid", "Backend register returns success=false or HTTP error", "Click Complete Registration.", "Registration failed message is shown and success screen is not displayed.", "High"],
    ["CR-COM-N02", "All", "Negative", "Camera permission denied", "Photo Capture step open", "Browser denies camera permission", "Click Open Camera and deny permission.", "Camera error is shown and capture cannot proceed.", "Medium"],
]


summary_rows = [
    ["Metric", "Count"],
    ["Total scenarios", len(scenarios)],
    ["EPIC scenarios", sum(1 for row in scenarios if row[1] == "EPIC")],
    ["AADHAAR scenarios", sum(1 for row in scenarios if row[1] == "AADHAAR")],
    ["No ID scenarios", sum(1 for row in scenarios if row[1] == "NONE")],
    ["Common scenarios", sum(1 for row in scenarios if row[1] == "All")],
    ["Positive scenarios", sum(1 for row in scenarios if row[2] == "Positive")],
    ["Negative scenarios", sum(1 for row in scenarios if row[2] == "Negative")],
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
