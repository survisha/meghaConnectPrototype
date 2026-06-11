package com.meghaconnect.automation.pageobjects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Validation message catalog extracted from Angular templates and components.
 */
public final class ValidationMessages {
    public static final Map<String, String> MESSAGES;

    static {
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("citizenRegistration.epicRequired", "EPIC number is required.");
        messages.put("citizenRegistration.epicInvalid", "EPIC number must be 3 letters followed by 7 digits.");
        messages.put("citizenRegistration.nameRequired", "Name is required.");
        messages.put("citizenRegistration.nameInvalid", "Name should contain only letters and spaces.");
        messages.put("citizenRegistration.mobileRequired", "Mobile number is required.");
        messages.put("citizenRegistration.mobileInvalid", "Mobile number must be 10 digits.");
        messages.put("citizenRegistration.otpInvalid", "Please enter a valid 6-digit OTP.");
        messages.put("citizenRegistration.districtRequired", "District is required.");
        messages.put("citizenRegistration.districtInvalid", "District should contain only letters and spaces.");
        messages.put("citizenRegistration.photoRequired", "Please capture live photo before continuing.");
        messages.put("citizenRegistration.kycUnavailable", "KYC service is temporarily unavailable. You can continue with registration, but your KYC status will remain Pending.");
        messages.put("citizenLogin.mobileInvalid", "Mobile number must be 10 digits.");
        messages.put("appointment.agendaRequired", "Please complete the appointment agenda, location, and purpose before submitting.");
        messages.put("appointment.associateMustBeRegistered", "Citizen must register in the portal before being added as an associate visitor.");
        messages.put("appointment.primaryAssociateDuplicate", "Primary citizen cannot be added again as an associate visitor.");
        messages.put("appointment.duplicateAssociate", "Duplicate associate visitors are not allowed.");
        messages.put("appointment.sessionMissing", "Visitor session is missing. Please login again before creating an appointment.");
        messages.put("scheduling.pastDate", "Cannot schedule or drag appointments to past dates.");
        messages.put("scheduling.meetingConflict", "Meeting Conflict Detected.");
        messages.put("scheduling.invalidDropTime", "Invalid drop time.");
        messages.put("admin.duplicateUsername", "Duplicate username validation");
        MESSAGES = Collections.unmodifiableMap(messages);
    }

    private ValidationMessages() {
    }

    public static boolean containsMessage(String expected) {
        if (expected == null || expected.trim().isEmpty()) {
            return false;
        }
        String normalizedExpected = normalize(expected);
        return MESSAGES.values().stream()
            .map(ValidationMessages::normalize)
            .anyMatch(value -> value.contains(normalizedExpected) || normalizedExpected.contains(value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
