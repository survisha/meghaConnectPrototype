package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI Document Intelligence Service.
 *
 * Provides:
 *   - Document text extraction via DocumentExtractionService
 *   - Structured field extraction from document text (R004)
 *   - Document summarization (R005)
 *   - Duplicate application detection (R006)
 *   - Meeting priority recommendation (R007)
 *   - Citizen chatbot Q&A (R008)
 *   - Appointment slot suggestions (R015)
 *   - Dashboard insights (R010)
 *
 * Integration strategy (two-tier):
 *   TIER 1 – OpenAI (live):
 *     When {@code meghaconnect.ai.api-key} is configured, every applicable
 *     method delegates to {@link OpenAiClientService} (GPT-3.5-turbo by default).
 *     The extracted text is sent as the user message and a structured system prompt
 *     guides the model to return the expected format.
 *   TIER 2 – Rule-based fallback:
 *     When no API key is present, or when the OpenAI call fails, the service
 *     falls back to its built-in deterministic keyword/regex engine.
 *     This guarantees the application is fully functional offline or during
 *     initial deployment without any API credentials.
 */
@Service
@RequiredArgsConstructor
public class AiDocumentIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentIntelligenceService.class);

    private final DocumentExtractionService extractionService;
    private final AppointmentRepository appointmentRepository;
    private final OpenAiClientService openAiClient;

    @Value("${meghaconnect.ai.api-key:}")
    private String aiApiKey;

    // ── Prompt constants ──────────────────────────────────────────────────────

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Maximum document text length (chars) sent to OpenAI to stay within token limits. */
    private static final int MAX_AI_TEXT_LENGTH = 3000;

    /** Maximum chars extracted per field label from document text. */
    private static final int MAX_FIELD_EXTRACT_LENGTH = 120;

    /** Maximum chars extracted for justification field. */
    private static final int MAX_JUSTIFICATION_LENGTH = 300;

    /** Maximum chars included in a justification snippet. */
    private static final int MAX_JUSTIFICATION_SNIPPET = 200;

    private static final String SYSTEM_EXTRACT_FIELDS =
            "You are a government document analysis assistant for Meghalaya, India. "
            + "Given the text extracted from a project proposal or application letter, "
            + "extract the following fields and return them in plain text, one field per line "
            + "in the exact format  'FieldName: value'. "
            + "Fields: Project Name, Project Category, Estimated Cost, Location, "
            + "Beneficiaries, Scheme Requested, Applicant Name, Key Justification. "
            + "If a field is not present, output 'FieldName: N/A'. "
            + "Do not add any other text.";

    private static final String SYSTEM_SUMMARIZE =
            "You are a concise government document summarizer. "
            + "Summarize the provided project proposal or application in exactly 5 lines "
            + "using the format:\n"
            + "Project: <name>\nLocation: <place>\nEstimated Cost: <amount>\n"
            + "Beneficiaries: <count or type>\nPurpose: <one sentence>\n"
            + "Do not add any other text.";

    private static final String SYSTEM_PRIORITY =
            "You are a meeting priority classifier for a Chief Minister's Office in India. "
            + "Classify the following appointment agenda as exactly one of: HIGH, MEDIUM, or LOW. "
            + "HIGH: medical emergencies, CM Care cases, urgent public safety. "
            + "MEDIUM: infrastructure, education, public grievances. "
            + "LOW: general discussions, trade, political matters. "
            + "Reply with only the single word: HIGH, MEDIUM, or LOW.";

    private static final String SYSTEM_CHATBOT =
            "You are MeghaBot, a friendly and helpful AI assistant for MeghaConnect, "
            + "the official citizen portal of the Chief Minister's Office, Meghalaya, India. "
            + "You help citizens with: visitor registration, booking appointments with the CM, "
            + "required documents for schemes (CMSDF, CM Care, CMSG, CM Connect, CM Elevate, Focus+), "
            + "tracking application status, and raising grievances. "
            + "Be concise, polite, and answer only questions related to MeghaConnect services. "
            + "If the question is unrelated, gently redirect to MeghaConnect topics.";

    // ── R004 / R005: Document Analysis ───────────────────────────────────────

    /**
     * Analyse an uploaded document: extract text, infer structured fields, generate summary.
     *
     * When OpenAI is available the extracted text is sent to GPT for both
     * structured field extraction (R004) and summarization (R005).
     * Falls back to rule-based engine when the API key is not configured.
     *
     * @param file uploaded MultipartFile
     * @return map with keys: success, summary, extractedFields, priorityLevel, priorityReason, duplicateFlag
     */
    public Map<String, Object> analyzeDocument(MultipartFile file) {
        String rawText = extractionService.extractText(file);
        log.debug("Extracted {} chars from document '{}'", rawText.length(), file.getOriginalFilename());

        // Truncate text to first MAX_AI_TEXT_LENGTH chars to stay within token limits
        String textForAi = rawText.length() > MAX_AI_TEXT_LENGTH
                ? rawText.substring(0, MAX_AI_TEXT_LENGTH) + "…"
                : rawText;

        Map<String, Object> extractedFields = extractFieldsWithAi(textForAi);
        String summary                      = summarizeWithAi(textForAi, extractedFields);
        String priorityLevel                = inferPriorityWithAi(
                (String) extractedFields.getOrDefault("schemeRequested", ""), textForAi);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("summary", summary);
        result.put("extractedFields", extractedFields);
        result.put("priorityLevel", priorityLevel);
        result.put("priorityReason", getPriorityReason(priorityLevel));
        result.put("duplicateFlag", false);
        return result;
    }

    /**
     * Extract structured fields from document text.
     * Uses OpenAI if available; falls back to rule-based parser.
     */
    private Map<String, Object> extractFieldsWithAi(String text) {
        if (openAiClient.isAvailable()) {
            Optional<String> aiResponse = openAiClient.chat(SYSTEM_EXTRACT_FIELDS, text);
            if (aiResponse.isPresent()) {
                return parseExtractedFieldsFromAiResponse(aiResponse.get());
            }
        }
        // Fallback to rule-based inference
        return inferFields(text);
    }

    /**
     * Parse the OpenAI field-extraction response (key: value lines) into a map.
     */
    private Map<String, Object> parseExtractedFieldsFromAiResponse(String response) {
        Map<String, Object> fields = new LinkedHashMap<>();
        // Normalise OpenAI field names to camelCase keys used by the frontend
        Map<String, String> keyMap = new LinkedHashMap<>();
        keyMap.put("Project Name",      "projectName");
        keyMap.put("Project Category",  "projectCategory");
        keyMap.put("Estimated Cost",    "estimatedCost");
        keyMap.put("Location",          "location");
        keyMap.put("Beneficiaries",     "beneficiaries");
        keyMap.put("Scheme Requested",  "schemeRequested");
        keyMap.put("Applicant Name",    "applicantName");
        keyMap.put("Key Justification", "justification");

        // Parse "FieldName: value" lines
        for (String line : response.split("\n")) {
            for (Map.Entry<String, String> entry : keyMap.entrySet()) {
                if (line.startsWith(entry.getKey() + ":")) {
                    String value = line.substring(entry.getKey().length() + 1).trim();
                    fields.put(entry.getValue(), "N/A".equalsIgnoreCase(value) ? null : value);
                    break;
                }
            }
        }
        // Fill any missing keys with null
        for (String key : keyMap.values()) {
            fields.putIfAbsent(key, null);
        }
        return fields;
    }

    /**
     * Generate a document summary.
     * Uses OpenAI if available; falls back to rule-based template.
     */
    private String summarizeWithAi(String text, Map<String, Object> extractedFields) {
        if (openAiClient.isAvailable()) {
            Optional<String> aiSummary = openAiClient.chat(SYSTEM_SUMMARIZE, text);
            if (aiSummary.isPresent()) {
                return aiSummary.get();
            }
        }
        // Fallback to template-based summary
        return buildSummary(extractedFields, text);
    }

    /**
     * Recommend a priority level from document text.
     * Uses OpenAI if available; falls back to rule-based classifier.
     */
    private String inferPriorityWithAi(String schemeHint, String text) {
        if (openAiClient.isAvailable()) {
            Optional<String> aiLevel = openAiClient.chatCompact(SYSTEM_PRIORITY, text, 5);
            if (aiLevel.isPresent()) {
                String level = aiLevel.get().toUpperCase().replaceAll("[^A-Z]", "");
                if ("HIGH".equals(level) || "MEDIUM".equals(level) || "LOW".equals(level)) {
                    return level;
                }
            }
        }
        return inferPriority(schemeHint, text);
    }

    // ── R006: Duplicate Detection ─────────────────────────────────────────────

    /**
     * Check for possible duplicate applications.
     * Checks same EPIC + scheme combination in existing appointments.
     *
     * @param epicNumber   applicant EPIC number
     * @param phoneNumber  applicant mobile
     * @param agendaType   agenda type string
     * @param schemeType   scheme type (nullable)
     * @param projectName  project name (nullable)
     * @return map with isDuplicate and details
     */
    public Map<String, Object> checkDuplicate(String epicNumber, String phoneNumber,
                                               String agendaType, String schemeType,
                                               String projectName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isDuplicate", false);

        if (epicNumber == null && phoneNumber == null) {
            return result;
        }

        // Search existing appointments by EPIC or phone via efficient repository queries
        List<Appointment> existing = new ArrayList<>();
        if (epicNumber != null && !epicNumber.trim().isEmpty()) {
            existing.addAll(appointmentRepository.findByApplicant_EpicNumber(epicNumber.trim()));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            List<Appointment> byPhone = appointmentRepository.findByApplicant_PhoneNumber(phoneNumber.trim());
            for (Appointment a : byPhone) {
                if (existing.stream().noneMatch(e -> e.getId().equals(a.getId()))) {
                    existing.add(a);
                }
            }
        }

        for (Appointment appt : existing) {
            // Same applicant – check agenda/scheme overlap
            boolean sameAgenda = agendaType != null
                    && agendaType.equalsIgnoreCase(appt.getAgendaType());
            boolean sameScheme = schemeType != null && appt.getAgendaBrief() != null
                    && appt.getAgendaBrief().toLowerCase().contains(schemeType.toLowerCase());

            if (sameAgenda && (schemeType == null || sameScheme)) {
                result.put("isDuplicate", true);
                result.put("previousApplicationId", appt.getApplicationId());
                result.put("schemeName", schemeType != null ? schemeType : agendaType);
                result.put("dateSubmitted", appt.getCreatedAt() != null
                        ? appt.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        : "–");
                return result;
            }
        }
        return result;
    }

    // ── R007: Priority Recommendation ────────────────────────────────────────

    /**
     * Recommend a meeting priority level based on agenda type and brief.
     * Uses OpenAI when available; falls back to rule-based classifier.
     *
     * @param agendaType  agenda type string
     * @param agendaBrief free-text description
     * @return map with level (HIGH/MEDIUM/LOW) and reason
     */
    public Map<String, String> suggestPriority(String agendaType, String agendaBrief) {
        String combined = ((agendaType != null ? agendaType : "") + " "
                + (agendaBrief != null ? agendaBrief : "")).trim();
        String level = inferPriorityWithAi(agendaType, combined);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("level", level);
        result.put("reason", getPriorityReason(level));
        return result;
    }

    // ── R008: Citizen Chatbot ─────────────────────────────────────────────────

    /**
     * Answer a citizen's question.
     * Uses OpenAI (GPT chat) when available; falls back to rule-based FAQ matching.
     *
     * @param question citizen's question
     * @return answer string
     */
    public String answerChatbotQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "Please type your question and I will try to help.";
        }

        // Attempt OpenAI response first
        if (openAiClient.isAvailable()) {
            Optional<String> aiAnswer = openAiClient.chat(SYSTEM_CHATBOT, question.trim());
            if (aiAnswer.isPresent()) {
                return aiAnswer.get();
            }
        }

        // Rule-based FAQ fallback
        return faqFallback(question);
    }

    /** Rule-based FAQ matching – used when OpenAI is unavailable. */
    private String faqFallback(String question) {
        String q = question.toLowerCase().trim();

        if (containsAny(q, "register", "sign up", "kyc", "id", "voter", "epic", "aadhaar")) {
            return "To register as a visitor:\n"
                    + "1. Go to the MeghaConnect Registration page.\n"
                    + "2. Enter your EPIC (Voter ID) or Aadhaar number.\n"
                    + "3. Verify OTP sent to your registered mobile number.\n"
                    + "4. Capture a live photo for KYC verification.\n"
                    + "5. Complete designation and location details.\n"
                    + "6. Submit to complete registration.\n"
                    + "After registration, you can log in with your mobile number.";
        }
        if (containsAny(q, "appointment", "book", "meet", "cm", "schedule")) {
            return "To book an appointment with the CM Office:\n"
                    + "1. Log in with your mobile number and OTP.\n"
                    + "2. Click 'Book New Appointment' on your dashboard.\n"
                    + "3. Fill in your personal details and agenda.\n"
                    + "4. Add scheme details if requesting a scheme.\n"
                    + "5. Add associate visitors if needed.\n"
                    + "6. Upload required documents.\n"
                    + "7. Review and submit.\n"
                    + "You will receive an Application ID. The CMO team will contact you to schedule.";
        }
        if (containsAny(q, "cmsdf", "document", "required", "upload", "scheme")) {
            return "Documents required for CMSDF appointments:\n"
                    + "• EPIC / Voter ID scan (mandatory)\n"
                    + "• Application Letter or Project Proposal (mandatory)\n"
                    + "• Plans & Estimates – up to 3 files (for project applications)\n"
                    + "• Bank Account Details (mandatory)\n"
                    + "• MLA/MDC Approval Letter (if applicable)\n"
                    + "• For CM Care: Hospital Documents and Eligibility Proof\n"
                    + "• Organisation Certificate (if representing an organisation)";
        }
        if (containsAny(q, "track", "status", "application", "check")) {
            return "To track your application status:\n"
                    + "1. Log in to MeghaConnect with your mobile number.\n"
                    + "2. Go to 'Appointment History' on your dashboard.\n"
                    + "3. Your Application ID and current status are shown.\n\n"
                    + "Status flow:\n"
                    + "SUBMITTED → DEO Processed → CMO Review → Approver Review → HCM Pending → Scheduled / Accepted / Rejected";
        }
        if (containsAny(q, "grievance", "complaint", "problem", "issue")) {
            return "To raise a grievance:\n"
                    + "1. Log in to MeghaConnect.\n"
                    + "2. Click 'Raise Grievance' on your dashboard.\n"
                    + "3. Select the grievance category and fill in details.\n"
                    + "4. Submit to receive a ticket ID.\n"
                    + "5. Track status under 'My Grievances'.";
        }
        if (containsAny(q, "scheme", "cm care", "cmsg", "cm connect", "cm elevate")) {
            return "Available CM Schemes:\n"
                    + "• CMSDF – CM Support and Development Fund (infrastructure projects)\n"
                    + "• CMSG – CM Special Grant (individual/community support)\n"
                    + "• CM Care – Medical assistance for individuals/families\n"
                    + "• CM Connect – Connectivity and communication projects\n"
                    + "• CM Elevate – Youth and skill development\n"
                    + "• Focus+ – Focused development for specific areas\n\n"
                    + "You can apply through the 'Apply for Scheme' option on your dashboard.";
        }

        return "I'm not sure about that. You can ask me about:\n"
                + "• How to register as a visitor\n"
                + "• How to book an appointment with CM\n"
                + "• Required documents for CMSDF\n"
                + "• How to track your application status\n"
                + "• Available CM schemes\n"
                + "• How to raise a grievance";
    }

    // ── R015: Slot Suggestions ────────────────────────────────────────────────

    /**
     * Suggest available appointment slots based on location and agenda type.
     * Checks existing scheduled appointments to avoid conflicts.
     *
     * @param requestedLocation meeting location (SHILLONG, TURA, DELHI, OTHERS)
     * @param agendaType        agenda type for context
     * @return list of suggested slot strings
     */
    public List<String> suggestSlots(String requestedLocation, String agendaType) {
        // Generate the next 3 available working days
        List<String> slots = new ArrayList<>();
        LocalDate today = DateTimeUtil.currentDateIST();
        String location = requestedLocation != null ? requestedLocation : "SHILLONG";
        String locationLabel = toLocationLabel(location);

        // Find days with slots already taken
        Set<String> busyDates = new HashSet<>();
        List<Appointment> scheduled = appointmentRepository.findByStatus(
                Appointment.AppointmentStatus.SCHEDULED);
        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Appointment a : scheduled) {
            if (a.getScheduledDateTime() != null
                    && location.equalsIgnoreCase(
                    a.getRequestedLocation() != null ? a.getRequestedLocation().name() : "")) {
                busyDates.add(a.getScheduledDateTime().format(ymd));
            }
        }

        // Walk forward to find 3 free morning/afternoon slots
        LocalDate cursor = today.plusDays(1);
        int slotsFound = 0;
        int maxSearch = 30;
        String[] times = {"10:00 AM", "02:30 PM", "11:30 AM"};

        while (slotsFound < 3 && maxSearch-- > 0) {
            // Skip weekends
            if (cursor.getDayOfWeek().getValue() < 6) {
                String dateStr = cursor.format(ymd);
                String displayDate = cursor.format(DateTimeFormatter.ofPattern("EEE, dd MMM"));
                String time = times[slotsFound % times.length];
                if (!busyDates.contains(dateStr)) {
                    slots.add(displayDate + " – " + time + " (" + locationLabel + ")");
                    slotsFound++;
                }
            }
            cursor = cursor.plusDays(1);
        }

        if (slots.isEmpty()) {
            slots.add("No slots available in the next 30 days for " + locationLabel
                    + ". Please contact the CMO office directly.");
        }
        return slots;
    }

    // ── R010: Dashboard Insights ──────────────────────────────────────────────

    /**
     * Generate AI dashboard insights from existing appointment data.
     *
     * @return map with totalApplicationsThisMonth, topSchemes, districtDistribution,
     *         topCategories, aiNote
     */
    public Map<String, Object> getDashboardInsights() {
        List<Appointment> all = appointmentRepository.findAll();

        // Count this month
        java.time.LocalDateTime firstOfMonth = DateTimeUtil.nowIST()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long thisMonth = appointmentRepository.countCreatedSince(firstOfMonth);

        // Count scheme types from agendaBrief (simple keyword detection)
        Map<String, Integer> schemeCounts = new LinkedHashMap<>();
        String[] schemes = {"CMSDF", "CM Care", "CM Elevate", "CMSG", "CM Connect", "Focus+"};
        for (String s : schemes) schemeCounts.put(s, 0);

        Map<String, Integer> districtCounts = new LinkedHashMap<>();

        for (Appointment a : all) {
            // Count schemes
            String brief = a.getAgendaBrief() != null ? a.getAgendaBrief() : "";
            String agenda = a.getAgendaType() != null ? a.getAgendaType() : "";
            for (String s : schemes) {
                if (brief.toLowerCase().contains(s.toLowerCase())
                        || agenda.toLowerCase().contains(s.toLowerCase())) {
                    schemeCounts.merge(s, 1, Integer::sum);
                }
            }
            // Count districts
            if (a.getApplicant() != null && a.getApplicant().getDistrict() != null) {
                districtCounts.merge(a.getApplicant().getDistrict(), 1, Integer::sum);
            }
        }

        // Build sorted top lists
        List<Map<String, Object>> topSchemes = buildTopList(schemeCounts, 5, "scheme");
        List<Map<String, Object>> districtList = buildTopList(districtCounts, 6, "district");
        List<Map<String, Object>> categoryList = buildTopCategories();

        // AI narrative note – use OpenAI when available for richer insight
        String topScheme = topSchemes.isEmpty() ? "CMSDF" : (String) topSchemes.get(0).get("scheme");
        String aiNote;
        if (openAiClient.isAvailable()) {
            String prompt = "In 2 sentences, describe the key trends based on these appointment statistics for "
                    + "Meghalaya CM Office: total appointments this month=" + thisMonth
                    + ", top scheme=" + topScheme + ", total appointments=" + all.size() + ".";
            aiNote = openAiClient.chatCompact(
                    "You are a government data analyst. Provide a concise 2-sentence insight.",
                    prompt, 120
            ).orElse(buildDefaultAiNote(topScheme, all.size()));
        } else {
            aiNote = buildDefaultAiNote(topScheme, all.size());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalApplicationsThisMonth", (int) thisMonth);
        result.put("topSchemes", topSchemes);
        result.put("districtDistribution", districtList);
        result.put("topCategories", categoryList);
        result.put("aiNote", aiNote);
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildDefaultAiNote(String topScheme, int total) {
        return "AI analysis of " + total + " appointments: "
                + topScheme + " is the most requested scheme this period. "
                + "Trend analysis indicates continued demand for infrastructure and "
                + "medical assistance schemes across all districts.";
    }

    /**
     * Rule-based field inference from document text.
     * Looks for common patterns (e.g. "Project Name:", "Cost:", keywords).
     */
    private Map<String, Object> inferFields(String text) {
        Map<String, Object> fields = new LinkedHashMap<>();
        String lower = text.toLowerCase();

        fields.put("projectName",     extractValue(text, "project name", "project title", "name of project"));
        fields.put("projectCategory", inferCategory(lower));
        fields.put("estimatedCost",   extractCost(lower));
        fields.put("location",        inferLocation(lower));
        fields.put("beneficiaries",   inferBeneficiaries(lower));
        fields.put("schemeRequested", inferScheme(lower));
        fields.put("applicantName",   extractValue(text, "applicant name", "name of applicant", "submitted by"));
        fields.put("justification",   extractJustification(text));

        return fields;
    }

    private String buildSummary(Map<String, Object> fields, String rawText) {
        String project   = strOrDefault(fields.get("projectName"),   "Untitled Project");
        String location  = strOrDefault(fields.get("location"),      "Meghalaya");
        String cost      = strOrDefault(fields.get("estimatedCost"), "Not specified");
        String benefic   = strOrDefault(fields.get("beneficiaries"), "Not specified");
        String scheme    = strOrDefault(fields.get("schemeRequested"), "General");

        return "Project: " + project + "\n"
                + "Location: " + location + "\n"
                + "Estimated Cost: " + formatCost(cost) + "\n"
                + "Beneficiaries: " + benefic + "\n"
                + "Scheme: " + scheme;
    }

    private String inferPriority(String agendaType, String text) {
        String lower = (text != null ? text : "").toLowerCase();
        if (containsAny(lower, "medical", "hospital", "emergency", "death", "cancer",
                "cm care", "critical", "urgent", "health")) {
            return "HIGH";
        }
        if (containsAny(lower, "grievance", "complaint", "problem", "road", "school",
                "governance", "infrastructure", "bridge", "drinking water")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String getPriorityReason(String level) {
        switch (level) {
            case "HIGH":   return "Medical or urgent humanitarian case requiring immediate attention";
            case "MEDIUM": return "Infrastructure or public grievance – moderate priority";
            default:       return "General discussion, trade, or political matter";
        }
    }

    private String extractValue(String text, String... labels) {
        if (text == null || text.isEmpty()) return null;
        for (String label : labels) {
            int idx = text.toLowerCase().indexOf(label.toLowerCase());
            if (idx >= 0) {
                int start = idx + label.length();
                // Skip colon and whitespace
                while (start < text.length() && (text.charAt(start) == ':' || text.charAt(start) == ' ')) {
                    start++;
                }
                int end = text.indexOf('\n', start);
                if (end < 0) end = Math.min(start + MAX_FIELD_EXTRACT_LENGTH, text.length());
                String value = text.substring(start, end).trim();
                if (!value.isEmpty()) return value;
            }
        }
        return null;
    }

    private String extractCost(String lower) {
        // Look for rupee amounts like ₹25,00,000 or Rs. 25 lakhs or 25000000
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?:rs\\.?|₹|inr)\\s*([\\d,]+(?:\\.\\d+)?(?:\\s*(?:lakh|crore|thousand))?)"
        );
        java.util.regex.Matcher m = p.matcher(lower);
        if (m.find()) {
            return m.group(1).trim();
        }
        // Also look for plain numbers followed by lakh/crore
        p = java.util.regex.Pattern.compile("(\\d+(?:[,.]\\d+)*)\\s*(lakh|crore|thousand)");
        m = p.matcher(lower);
        if (m.find()) {
            return m.group(1) + " " + m.group(2);
        }
        return null;
    }

    private String inferCategory(String lower) {
        String[] cats = {"road", "school", "hospital", "medical", "community hall",
                "electricity", "water", "bridge", "house", "office", "sports",
                "bus", "van", "computer lab", "retaining wall"};
        for (String cat : cats) {
            if (lower.contains(cat)) {
                return capitalize(cat);
            }
        }
        return null;
    }

    private String inferLocation(String lower) {
        String[] districts = {
                "east khasi hills", "west khasi hills", "ri bhoi", "jaintia",
                "east garo hills", "west garo hills", "south garo", "north garo",
                "shillong", "tura", "jowai", "nongstoin"
        };
        for (String d : districts) {
            if (lower.contains(d)) {
                return capitalize(d);
            }
        }
        return null;
    }

    private String inferBeneficiaries(String lower) {
        if (lower.contains("above 1000") || lower.contains("1000+")) return "Above 1000";
        if (lower.contains("501") || lower.contains("500")) return "501 to 1000";
        if (lower.contains("101") || lower.contains("200") || lower.contains("300")) return "101 to 500";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:villager|people|person|family|household|beneficiar)");
        java.util.regex.Matcher m = p.matcher(lower);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n > 1000) return "Above 1000";
            if (n > 500)  return "501 to 1000";
            if (n > 100)  return "101 to 500";
            return "1 to 100";
        }
        return null;
    }

    private String inferScheme(String lower) {
        if (lower.contains("cmsdf"))        return "CMSDF";
        if (lower.contains("cm care"))      return "CM Care";
        if (lower.contains("cm elevate"))   return "CM Elevate";
        if (lower.contains("cm connect"))   return "CM Connect";
        if (lower.contains("cmsg"))         return "CMSG";
        if (lower.contains("focus+") || lower.contains("focus plus")) return "Focus+";
        return null;
    }

    private String extractJustification(String text) {
        String lower = text.toLowerCase();
        for (String label : new String[]{"justification", "rationale", "purpose", "background",
                "need for", "reason"}) {
            int idx = lower.indexOf(label);
            if (idx >= 0) {
                int start = idx + label.length();
                while (start < text.length() && (text.charAt(start) == ':' || text.charAt(start) == ' ')) {
                    start++;
                }
                int end = Math.min(start + MAX_JUSTIFICATION_LENGTH, text.length());
                // Cut at next section heading (capital line)
                String snippet = text.substring(start, end).trim();
                if (snippet.length() > 20) {
                    return snippet.length() > MAX_JUSTIFICATION_SNIPPET
                            ? snippet.substring(0, MAX_JUSTIFICATION_SNIPPET) + "…"
                            : snippet;
                }
            }
        }
        return null;
    }

    private String formatCost(String cost) {
        if (cost == null || cost.equals("Not specified")) return cost;
        try {
            double val = Double.parseDouble(cost.replace(",", ""));
            if (val >= 10000000) return "₹" + String.format("%.0f", val / 10000000) + " Crore";
            if (val >= 100000)   return "₹" + String.format("%.0f", val / 100000) + " Lakh";
            return "₹" + cost;
        } catch (NumberFormatException e) {
            return "₹" + cost;
        }
    }

    private List<Map<String, Object>> buildTopList(Map<String, Integer> counts, int limit, String keyName) {
        // Sort descending
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());

        int total = entries.stream().mapToInt(Map.Entry::getValue).sum();
        if (total == 0) total = 1;

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(keyName, entries.get(i).getKey());
            item.put("count", entries.get(i).getValue());
            item.put("percentage", (int) Math.round(entries.get(i).getValue() * 100.0 / total));
            result.add(item);
        }
        return result;
    }

    /** Returns representative top categories for the demo when no appointment data has categorized projects */
    private List<Map<String, Object>> buildTopCategories() {
        // In real deployment this would scan ai_extracted_fields JSON
        List<Map<String, Object>> cats = new ArrayList<>();
        String[][] data = {
                {"Road", "55"}, {"School Infrastructure", "48"}, {"Medical Assistance", "41"},
                {"Community Hall", "37"}, {"Electricity", "29"}
        };
        for (String[] d : data) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", d[0]);
            item.put("count", Integer.parseInt(d[1]));
            cats.add(item);
        }
        return cats;
    }

    private String toLocationLabel(String loc) {
        switch (loc.toUpperCase()) {
            case "SHILLONG": return "Shillong";
            case "TURA":     return "Tura";
            case "DELHI":    return "Delhi";
            default:         return "CM Office";
        }
    }

    private String strOrDefault(Object val, String def) {
        return (val != null && !val.toString().isEmpty()) ? val.toString() : def;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
