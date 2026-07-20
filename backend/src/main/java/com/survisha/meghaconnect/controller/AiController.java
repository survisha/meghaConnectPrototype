package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.service.AiDocumentIntelligenceService;
import com.survisha.meghaconnect.service.AppointmentPriorityScoringService;
import com.survisha.meghaconnect.service.LLMProviderService;
import com.survisha.meghaconnect.service.LlmHealth;
import com.survisha.meghaconnect.service.RequestValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Endpoints for the MeghaConnect system.
 *
 * AI endpoints are available under both /api/ai and /api/v1/ai. Citizen chatbot
 * remains public; document, dashboard, prioritization, and duplicate checks
 * require authenticated staff.
 *
 * Endpoints:
 *   POST  /api/ai/analyze-document  – R004/R005: Document extraction + summary
 *   POST  /api/ai/check-duplicate   – R006: Duplicate application detection
 *   POST  /api/ai/suggest-priority  – R007: Meeting priority recommendation
 *   POST  /api/v1/ai/chatbot        – R008: Citizen chatbot Q&A
 *   POST  /api/ai/suggest-slots     – R015: Appointment slot suggestions
 *   GET   /api/v1/ai/dashboard-insights – R010: AI dashboard insights for officers
 */
@RestController
@RequestMapping({"/api/ai", "/api/v1/ai"})
@RequiredArgsConstructor
@Tag(name = "AI Integration", description = "AI-powered intelligent endpoints for document analysis, duplicate detection, and chatbot")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiDocumentIntelligenceService aiService;
    private final AppointmentRepository appointmentRepository;
    private final RequestValidationService validationService;
    private final LLMProviderService llmProviderService;
    private final AppointmentPriorityScoringService priorityScoringService;

    // ── R004 / R005: Document Analysis ───────────────────────────────────────

    /**
     * Analyse an uploaded document: extract text, infer structured fields, generate summary.
     *
     * Request: multipart/form-data with field "file"
     * Optional: appointmentId to persist ai_summary on an existing appointment
     *
     * Response:
     * {
     *   "success": true,
     *   "summary": "Project: ...\nLocation: ...\n...",
     *   "extractedFields": { "projectName": "...", "estimatedCost": "...", ... },
     *   "priorityLevel": "MEDIUM",
     *   "priorityReason": "...",
     *   "duplicateFlag": false
     * }
     */
    @PostMapping(value = "/analyze-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<Map<String, Object>> analyzeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "appointmentId", required = false) Long appointmentId) {

        validationService.requireNonEmptyFile(file);

        log.info("AI document analysis requested for file: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = aiService.analyzeDocument(file);

        // Persist AI summary to appointment if appointmentId is provided
        if (appointmentId != null) {
            final String summary = (String) result.get("summary");
            appointmentRepository.findById(appointmentId).ifPresent(appt -> {
                appt.setAiSummary(summary);
                appt.setAiPriorityLevel((String) result.get("priorityLevel"));
                appointmentRepository.save(appt);
            });
        }

        return ResponseEntity.ok(result);
    }

    // ── R006: Duplicate Detection ─────────────────────────────────────────────

    /**
     * Check for possible duplicate applications.
     *
     * Request body:
     * {
     *   "epicNumber":   "ABC1234567",
     *   "phoneNumber":  "9876543210",
     *   "agendaType":   "Scheme availment (CM)",
     *   "schemeType":   "CMSDF",
     *   "projectName":  "Village Road"
     * }
     *
     * Response:
     * {
     *   "isDuplicate": false
     * }
     * or if duplicate:
     * {
     *   "isDuplicate": true,
     *   "previousApplicationId": "MC-20260301-ABCD1234",
     *   "schemeName": "CMSDF",
     *   "dateSubmitted": "01 Mar 2026"
     * }
     */
    @PostMapping("/check-duplicate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<Map<String, Object>> checkDuplicate(@RequestBody Map<String, Object> body) {
        String epicNumber   = getString(body, "epicNumber");
        String phoneNumber  = getString(body, "phoneNumber");
        String agendaType   = getString(body, "agendaType");
        String schemeType   = getString(body, "schemeType");
        String projectName  = getString(body, "projectName");

        Map<String, Object> result = aiService.checkDuplicate(epicNumber, phoneNumber,
                agendaType, schemeType, projectName);
        return ResponseEntity.ok(result);
    }

    // ── R007: Priority Recommendation ────────────────────────────────────────

    /**
     * Recommend a meeting priority level.
     *
     * Request body:
     * {
     *   "agendaType":   "Public Grievance",
     *   "agendaBrief":  "Road construction near village..."
     * }
     *
     * Response:
     * { "level": "MEDIUM", "reason": "Infrastructure or public grievance – moderate priority" }
     */
    @PostMapping("/suggest-priority")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<Map<String, String>> suggestPriority(@RequestBody Map<String, Object> body) {
        String agendaType  = getString(body, "agendaType");
        String agendaBrief = getString(body, "agendaBrief");
        Map<String, String> result = aiService.suggestPriority(agendaType, agendaBrief);
        return ResponseEntity.ok(result);
    }

    // ── R008: Citizen Chatbot ─────────────────────────────────────────────────

    /**
     * Answer a citizen's question.
     *
     * Request body: { "question": "How do I register?" }
     * Response: { "answer": "To register as a visitor: ..." }
     */
    @PostMapping("/chatbot")
    public ResponseEntity<Map<String, String>> chatbot(@RequestBody Map<String, Object> body) {
        String question = getString(body, "question");
        String answer = aiService.answerChatbotQuestion(question);
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        return ResponseEntity.ok(result);
    }

    // ── R015: Slot Suggestions ────────────────────────────────────────────────

    /**
     * Suggest available appointment slots.
     *
     * Request body:
     * {
     *   "requestedLocation": "SHILLONG",
     *   "agendaType": "Scheme availment (CM)"
     * }
     *
     * Response: [ "Mon, 10 Mar – 10:00 AM (Shillong)", "Tue, 11 Mar – 02:30 PM (Shillong)", ... ]
     */
    @PostMapping("/suggest-slots")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM')")
    public ResponseEntity<List<String>> suggestSlots(@RequestBody Map<String, Object> body) {
        String location   = getString(body, "requestedLocation");
        String agendaType = getString(body, "agendaType");
        List<String> slots = aiService.suggestSlots(location, agendaType);
        return ResponseEntity.ok(slots);
    }

    // ── R010: Dashboard Insights ──────────────────────────────────────────────

    /**
     * Return AI-generated dashboard insights for officer dashboards.
     *
     * Response:
     * {
     *   "totalApplicationsThisMonth": 247,
     *   "topSchemes": [ { "scheme": "CMSDF", "count": 89, "percentage": 36 }, ... ],
     *   "districtDistribution": [ ... ],
     *   "topCategories": [ ... ],
     *   "aiNote": "AI analysis of 247 appointments: ..."
     * }
     */
    @GetMapping("/dashboard-insights")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM')")
    public ResponseEntity<Map<String, Object>> getDashboardInsights() {
        Map<String, Object> insights = aiService.getDashboardInsights();
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM')")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(toHealthResponse(llmProviderService.healthCheck()));
    }

    @GetMapping("/appointments/{appointmentId}/priority-insight")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<AppointmentPriorityScoringService.PriorityScore> priorityInsight(
            @PathVariable Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        return ResponseEntity.ok(priorityScoringService.score(appointment));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    static Map<String, Object> toHealthResponse(LlmHealth health) {
        Map<String, Object> response = new HashMap<>();
        response.put("provider", health.getProvider());
        response.put("model", health.getModel());
        response.put("available", health.isAvailable());
        response.put("message", health.getMessage());
        return response;
    }
}
