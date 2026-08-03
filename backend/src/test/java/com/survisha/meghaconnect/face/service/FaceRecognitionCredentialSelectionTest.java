package com.survisha.meghaconnect.face.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.face.client.FaceRecognitionClient;
import com.survisha.meghaconnect.face.config.FaceRecognitionProperties;
import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.validation.FacePhotoValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionCredentialSelectionTest {
    @Mock private FaceRecognitionClient client;
    @Mock private FacePhotoValidator photoValidator;
    @Mock private VisitorEnrollmentLookupService visitorLookupService;

    private FaceRecognitionServiceImpl service;

    @BeforeEach
    void setUp() {
        FaceRecognitionProperties properties = new FaceRecognitionProperties();
        properties.setApiKey("general-key");
        properties.setClientId("client");
        properties.setAppId("app");
        properties.getEnroll().setApiKey("enrollment-key");
        service = new FaceRecognitionServiceImpl(client, properties, photoValidator,
                new SimpleMeterRegistry(), visitorLookupService);
        when(photoValidator.normalize(anyString(), anyString())).thenReturn("normalized-photo");
        when(client.post(anyString(), any())).thenReturn(new ObjectMapper().createObjectNode());
    }

    @Test
    void enrollmentUsesDedicatedApiKey() {
        FaceRequests.Enroll request = new FaceRequests.Enroll();
        request.setEnrollmentId("VISITOR_1");
        request.setName("Citizen");
        request.setPhoto("photo");

        service.enroll(request);

        ArgumentCaptor<Map<String, Object>> payload = mapCaptor();
        verify(client).post(org.mockito.ArgumentMatchers.eq("enroll"), payload.capture());
        assertEquals("enrollment-key", payload.getValue().get("apiKey"));
    }

    @Test
    void nonEnrollmentOperationUsesGeneralApiKey() {
        FaceRequests.Compare request = new FaceRequests.Compare();
        request.setPhoto1("photo-1");
        request.setPhoto2("photo-2");

        service.compare(request);

        ArgumentCaptor<Map<String, Object>> payload = mapCaptor();
        verify(client).post(org.mockito.ArgumentMatchers.eq("compare"), payload.capture());
        assertEquals("general-key", payload.getValue().get("apiKey"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}
