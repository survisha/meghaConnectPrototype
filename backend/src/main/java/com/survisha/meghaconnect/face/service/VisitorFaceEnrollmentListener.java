package com.survisha.meghaconnect.face.service;

import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.event.VisitorRegisteredForFaceEnrollmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class VisitorFaceEnrollmentListener {
    private final FaceRecognitionService faceRecognitionService;
    private final FaceEnrollmentStatusService statusService;

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enrollAfterRegistration(VisitorRegisteredForFaceEnrollmentEvent event) {
        String enrollmentId = enrollmentId(event);
        try {
            if (event.photo() == null || event.photo().isBlank()) {
                throw new IllegalArgumentException("Captured visitor photo is unavailable for face enrollment.");
            }
            FaceRequests.Enroll request = new FaceRequests.Enroll();
            request.setEnrollmentId(enrollmentId);
            request.setName(event.fullName());
            request.setPhoto(event.photo());
            request.setLatitude(event.latitude());
            request.setLongitude(event.longitude());
            faceRecognitionService.enroll(request);
            statusService.markSuccess(event.visitorId());
            log.info("Automatic face enrollment succeeded visitorId={} enrollmentId={}", event.visitorId(), enrollmentId);
        } catch (RuntimeException ex) {
            log.error("Automatic face enrollment failed visitorId={} enrollmentId={} error={}",
                    event.visitorId(), enrollmentId, ex.getMessage(), ex);
            try {
                statusService.markFailed(event.visitorId(), ex.getMessage());
            } catch (RuntimeException statusEx) {
                log.error("Could not persist face enrollment failure visitorId={}", event.visitorId(), statusEx);
            }
        }
    }

    private String enrollmentId(VisitorRegisteredForFaceEnrollmentEvent event) {
        return event.epicNumber() != null && !event.epicNumber().isBlank()
                ? event.epicNumber().trim().toUpperCase()
                : "VISITOR_" + event.visitorId();
    }
}
