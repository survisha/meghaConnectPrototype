package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AppointmentDocumentAiNotesDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AppointmentDocumentAiNotes;
import com.survisha.meghaconnect.entity.AppointmentDocumentAiNotes.AiNoteStatus;
import com.survisha.meghaconnect.entity.DocumentUpload;
import com.survisha.meghaconnect.exception.AppointmentNotFoundException;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.AppointmentDocumentAiNotesRepository;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.DocumentUploadRepository;
import com.survisha.meghaconnect.util.RequestContextUtil;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentDocumentAiNotesService {

    private final AppointmentDocumentAiNotesRepository aiNotesRepository;
    private final AppointmentRepository appointmentRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final DocumentTextExtractionService textExtractionService;
    private final OllamaAiNotesService ollamaAiNotesService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AppointmentDocumentAiNotesDto> getNotesForAppointment(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new AppointmentNotFoundException(appointmentId);
        }
        return aiNotesRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Optional<AppointmentDocumentAiNotesDto> queueGeneration(DocumentUpload document) {
        if (document == null || document.getId() == null || document.getAppointment() == null
                || document.getAppointment().getId() == null) {
            log.info("Skipping AI notes queue requestId={} documentId={} reason=no-linked-appointment",
                    RequestContextUtil.getRequestId(), document != null ? document.getId() : null);
            return Optional.empty();
        }

        AppointmentDocumentAiNotes notes = aiNotesRepository.findByDocumentId(document.getId())
                .orElseGet(() -> AppointmentDocumentAiNotes.builder()
                        .appointment(document.getAppointment())
                        .document(document)
                        .build());
        prepareForGeneration(notes, document);
        AppointmentDocumentAiNotes saved = aiNotesRepository.save(notes);
        eventPublisher.publishEvent(new AiNotesGenerationRequested(saved.getId()));
        log.info("Queued AI notes requestId={} appointmentId={} documentId={} status={}",
                RequestContextUtil.getRequestId(),
                resolveAppointmentId(saved),
                resolveDocumentId(saved),
                saved.getStatus());
        return Optional.of(toDto(saved));
    }

    @Transactional
    public AppointmentDocumentAiNotesDto regenerate(Long documentId) {
        DocumentUpload document = documentUploadRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        return queueGeneration(document)
                .orElseThrow(() -> new ResourceNotFoundException("Document is not linked to an appointment: " + documentId));
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAiNotesGenerationRequested(AiNotesGenerationRequested event) {
        processNote(event.noteId());
    }

    public void processNote(Long noteId) {
        Optional<Long> documentId = markProcessing(noteId);
        if (documentId.isEmpty()) {
            return;
        }

        try {
            DocumentUpload document = documentUploadRepository.findById(documentId.get())
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId.get()));
            String documentText = textExtractionService.extractText(document);
            String rawResponse = ollamaAiNotesService.generateNotes(documentText);
            ParsedAiNotes parsed = parseAiNotes(rawResponse);
            complete(noteId, rawResponse, parsed);
        } catch (Exception e) {
            fail(noteId, e);
        }
    }

    @Transactional
    protected Optional<Long> markProcessing(Long noteId) {
        return aiNotesRepository.findById(noteId).map(notes -> {
            notes.setStatus(AiNoteStatus.PROCESSING);
            notes.setErrorMessage(null);
            aiNotesRepository.save(notes);
            Long documentId = resolveDocumentId(notes);
            log.info("Processing AI notes requestId={} appointmentId={} documentId={} status={}",
                    RequestContextUtil.getRequestId(),
                    resolveAppointmentId(notes),
                    documentId,
                    notes.getStatus());
            return documentId;
        });
    }

    @Transactional
    protected void complete(Long noteId, String rawResponse, ParsedAiNotes parsed) {
        aiNotesRepository.findById(noteId).ifPresent(notes -> {
            notes.setAiSummary(parsed.summary());
            notes.setImportantDetails(parsed.importantDetails());
            notes.setMissingInfo(parsed.missingInfo());
            notes.setRiskFlags(parsed.riskFlags());
            notes.setRawAiResponse(rawResponse);
            notes.setStatus(AiNoteStatus.COMPLETED);
            notes.setErrorMessage(null);
            notes.setModelName(ollamaAiNotesService.getModelName());
            aiNotesRepository.save(notes);
            log.info("Completed AI notes requestId={} appointmentId={} documentId={} status={}",
                    RequestContextUtil.getRequestId(),
                    resolveAppointmentId(notes),
                    resolveDocumentId(notes),
                    notes.getStatus());
        });
    }

    @Transactional
    protected void fail(Long noteId, Exception error) {
        aiNotesRepository.findById(noteId).ifPresent(notes -> {
            notes.setStatus(AiNoteStatus.FAILED);
            notes.setErrorMessage(limitErrorMessage(error.getMessage()));
            notes.setModelName(ollamaAiNotesService.getModelName());
            aiNotesRepository.save(notes);
            log.warn("Failed AI notes requestId={} appointmentId={} documentId={} status={} error={}",
                    RequestContextUtil.getRequestId(),
                    resolveAppointmentId(notes),
                    resolveDocumentId(notes),
                    notes.getStatus(),
                    error.getClass().getSimpleName());
        });
    }

    public AppointmentDocumentAiNotesDto toDto(AppointmentDocumentAiNotes notes) {
        return AppointmentDocumentAiNotesDto.builder()
                .id(notes.getId())
                .appointmentId(resolveAppointmentId(notes))
                .documentId(resolveDocumentId(notes))
                .fileName(notes.getFileName())
                .aiSummary(notes.getAiSummary())
                .importantDetails(notes.getImportantDetails())
                .missingInfo(notes.getMissingInfo())
                .riskFlags(notes.getRiskFlags())
                .status(notes.getStatus())
                .errorMessage(notes.getErrorMessage())
                .modelName(notes.getModelName())
                .createdAt(notes.getCreatedAt())
                .updatedAt(notes.getUpdatedAt())
                .build();
    }

    private void prepareForGeneration(AppointmentDocumentAiNotes notes, DocumentUpload document) {
        notes.setAppointment(document.getAppointment());
        notes.setDocument(document);
        notes.setFileName(firstNonBlank(document.getOriginalFilename(), document.getStoredFileName(), document.getDocumentType()));
        notes.setAiSummary(null);
        notes.setImportantDetails(null);
        notes.setMissingInfo(null);
        notes.setRiskFlags(null);
        notes.setRawAiResponse(null);
        notes.setStatus(AiNoteStatus.PENDING);
        notes.setErrorMessage(null);
        notes.setModelName(ollamaAiNotesService.getModelName());
    }

    private ParsedAiNotes parseAiNotes(String rawResponse) {
        String summary = section(rawResponse, "Summary:", "Important Details:", "Missing or Unclear Information:", "Risk Flags:");
        String importantDetails = section(rawResponse, "Important Details:", "Missing or Unclear Information:", "Risk Flags:");
        String missingInfo = section(rawResponse, "Missing or Unclear Information:", "Risk Flags:");
        String riskFlags = section(rawResponse, "Risk Flags:");
        return new ParsedAiNotes(
                defaultIfBlank(summary),
                defaultIfBlank(importantDetails),
                defaultIfBlank(missingInfo),
                defaultIfBlank(riskFlags)
        );
    }

    private String section(String rawResponse, String heading, String... nextHeadings) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }
        String lower = rawResponse.toLowerCase();
        int start = lower.indexOf(heading.toLowerCase());
        if (start < 0) {
            return "";
        }
        start += heading.length();
        int end = rawResponse.length();
        for (String nextHeading : nextHeadings) {
            int next = lower.indexOf(nextHeading.toLowerCase(), start);
            if (next >= 0 && next < end) {
                end = next;
            }
        }
        return rawResponse.substring(start, end).trim();
    }

    private String defaultIfBlank(String value) {
        return value == null || value.isBlank() ? "Not found" : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String limitErrorMessage(String message) {
        String value = message == null || message.isBlank() ? "AI notes generation failed." : message.trim();
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private Long resolveAppointmentId(AppointmentDocumentAiNotes notes) {
        if (notes.getAppointmentId() != null) {
            return notes.getAppointmentId();
        }
        Appointment appointment = notes.getAppointment();
        return appointment != null ? appointment.getId() : null;
    }

    private Long resolveDocumentId(AppointmentDocumentAiNotes notes) {
        if (notes.getDocumentId() != null) {
            return notes.getDocumentId();
        }
        DocumentUpload document = notes.getDocument();
        return document != null ? document.getId() : null;
    }

    public record AiNotesGenerationRequested(Long noteId) {}

    private record ParsedAiNotes(String summary, String importantDetails, String missingInfo, String riskFlags) {}
}
