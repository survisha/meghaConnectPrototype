from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TAGLINE = "AI Powered Appointment & Scheme Management Platform"

JSON_UPDATES = {
    "CITIZEN_PORTAL_GOVT": TAGLINE,
    "CM_OFFICE_SCHEDULING": TAGLINE,
    "OFFICIAL_GOVERNMENT_PORTAL": "MeghaConnect AI",
    "MEGHACONNECT_WELCOME_TITLE": "MeghaConnect AI",
    "MEGHACONNECT_WELCOME_TEXT": "AI powered appointment, scheme tracking, visitor management, citizen engagement, follow-up tracking, and intelligent decision support.",
    "HOME_BRAND_TAGLINE": TAGLINE,
    "HOME_HERO_EYEBROW": "AI Driven Governance Platform",
    "HOME_HERO_COPY": "A modern AI-powered platform for CM appointment management, CM scheme tracking, citizen engagement, visitor management, follow-up tracking, AI insights, AI notes, and intelligent decision support.",
    "HOME_HIGHLIGHT_APPOINTMENT": "Appointment Management",
    "HOME_HIGHLIGHT_DARBAR": "Scheme Tracking",
    "HOME_HIGHLIGHT_VERIFICATION": "AI Decision Support",
    "HOME_ABOUT_TITLE": "Built for AI-powered governance workflows",
    "HOME_ABOUT_COPY": "MeghaConnect AI helps public offices receive requests, understand supporting documents, schedule meetings, track schemes, manage visitors, and keep every follow-up traceable.",
    "HOME_ABOUT_REGISTRATION_TEXT": "Register visitor profiles with KYC-ready details for appointment and engagement workflows.",
    "HOME_ABOUT_APPOINTMENT_TEXT": "Manage CM appointments with agenda, department, document, scheduling, and review intelligence.",
    "HOME_ABOUT_DARBAR_TITLE": "Scheme tracking",
    "HOME_ABOUT_DARBAR_TEXT": "Track CM scheme applications, documentation, reviews, and follow-up actions.",
    "HOME_ABOUT_DEPARTMENT_TEXT": "Route appointments, scheme requests, grievances, and visitor journeys to relevant teams.",
    "HOME_ABOUT_UPLOAD_TITLE": "AI document understanding",
    "HOME_ABOUT_UPLOAD_TEXT": "Use OCR and AI Notes to summarize uploaded documents for faster official review.",
    "HOME_ABOUT_QR_TITLE": "Visitor verification",
    "HOME_ABOUT_QR_TEXT": "QR-based visitor passes and security verification support controlled entry and movement.",
    "HOME_CONNECT_TITLE": "One AI workspace for citizens and officials",
    "HOME_CONNECT_COPY": "Citizens can connect through appointments and scheme requests. Officials can review, approve, schedule, follow up, and act with AI-assisted summaries and insights.",
    "HOME_CONNECT_REQUESTS_TITLE": "Citizen engagement",
    "HOME_CONNECT_REQUESTS_TEXT": "Citizens share purpose, scheme details, department needs, and supporting documents through structured requests.",
    "HOME_CONNECT_REVIEW_TITLE": "Intelligent review",
    "HOME_CONNECT_REVIEW_TEXT": "AI Notes, OCR, and dashboard insights help officers prioritize, route, and decide faster.",
    "HOME_CONNECT_NOTIFICATIONS_TEXT": "Status and schedule alerts keep citizens and teams aligned across appointment and follow-up journeys.",
    "HOME_LOGIN_TITLE": "Access your MeghaConnect AI workspace",
    "HOME_LOGIN_COPY": "Citizens continue through OTP login, while officials sign in to review, approve, schedule, track, and act with AI-assisted governance tools.",
    "LOGIN_WITH_REGISTERED_MOBILE": "Log in with your registered mobile number to access appointments, schemes, visitor services, and AI-assisted support.",
    "SIGN_IN_TO_CONTINUE": TAGLINE,
}

TEXT_REPLACEMENTS = {
    "Government of Meghalaya Citizen Appointment Portal": "MeghaConnect AI",
    "Citizen Appointment Portal": "MeghaConnect AI",
    "Government Portal": "MeghaConnect AI",
    "Official Government Portal": "MeghaConnect AI",
    "Government of Meghalaya citizen appointment portal": TAGLINE,
    "Citizen Portal - Government of Meghalaya": TAGLINE,
    "CM Office - Scheduling & Scheme Management | Government of Meghalaya": TAGLINE,
    "Chief Minister's Office citizen service platform": TAGLINE,
    "MeghaConnect Citizen Services Portal": "MeghaConnect AI",
}


def update_json(path: Path) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    changed = False
    for key, value in JSON_UPDATES.items():
        if key in data and data[key] != value:
            data[key] = value
            changed = True
    if changed:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def replace_text(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    original = text
    for old, new in TEXT_REPLACEMENTS.items():
        text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding="utf-8")


def main() -> None:
    for base in [ROOT / "frontend" / "public" / "assets" / "i18n", ROOT / "mobile" / "assets" / "i18n"]:
        for path in base.glob("*.json"):
            update_json(path)
            replace_text(path)

    text_files = [
        ROOT / "frontend" / "src" / "index.html",
        ROOT / "mobile" / "pubspec.yaml",
        ROOT / "mobile" / "web" / "index.html",
        ROOT / "mobile" / "web" / "manifest.json",
    ]
    for path in text_files:
        if path.exists():
            replace_text(path)


if __name__ == "__main__":
    main()
