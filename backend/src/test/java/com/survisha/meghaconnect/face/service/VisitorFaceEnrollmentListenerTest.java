package com.survisha.meghaconnect.face.service;

import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.event.VisitorRegisteredForFaceEnrollmentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VisitorFaceEnrollmentListenerTest {
    @Mock private FaceRecognitionService faceRecognitionService;
    @Mock private FaceEnrollmentStatusService statusService;

    private VisitorFaceEnrollmentListener listener;

    @BeforeEach
    void setUp() {
        listener = new VisitorFaceEnrollmentListener(faceRecognitionService, statusService);
    }

    @Test
    void enrollsWithEpicAndMarksSuccess() {
        listener.enrollAfterRegistration(event(42L, "abc1234567", "data:image/jpeg;base64,photo"));

        ArgumentCaptor<FaceRequests.Enroll> request = ArgumentCaptor.forClass(FaceRequests.Enroll.class);
        verify(faceRecognitionService).enroll(request.capture());
        assertEquals("ABC1234567", request.getValue().getEnrollmentId());
        assertEquals("Citizen Name", request.getValue().getName());
        verify(statusService).markSuccess(42L);
        verify(statusService, never()).markFailed(any(), any());
    }

    @Test
    void usesVisitorIdWhenEpicIsUnavailable() {
        listener.enrollAfterRegistration(event(42L, null, "data:image/jpeg;base64,photo"));

        ArgumentCaptor<FaceRequests.Enroll> request = ArgumentCaptor.forClass(FaceRequests.Enroll.class);
        verify(faceRecognitionService).enroll(request.capture());
        assertEquals("VISITOR_42", request.getValue().getEnrollmentId());
        verify(statusService).markSuccess(42L);
    }

    @Test
    void providerFailureIsContainedAndStored() {
        doThrow(new RuntimeException("provider unavailable")).when(faceRecognitionService).enroll(any());

        listener.enrollAfterRegistration(event(42L, null, "data:image/jpeg;base64,photo"));

        verify(statusService).markFailed(42L, "provider unavailable");
        verify(statusService, never()).markSuccess(any());
    }

    private VisitorRegisteredForFaceEnrollmentEvent event(Long id, String epic, String photo) {
        return new VisitorRegisteredForFaceEnrollmentEvent(id, epic, "Citizen Name", photo, 25.57, 91.88);
    }
}
