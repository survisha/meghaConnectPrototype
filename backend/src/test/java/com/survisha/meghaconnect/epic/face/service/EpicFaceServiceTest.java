package com.survisha.meghaconnect.epic.face.service;

import com.survisha.meghaconnect.epic.face.client.EpicFaceClient;
import com.survisha.meghaconnect.epic.face.dto.provider.FaceSearch1NProviderResponse;
import com.survisha.meghaconnect.epic.face.dto.provider.FaceVerify11ProviderResponse;
import com.survisha.meghaconnect.epic.face.exception.EpicFaceException;
import com.survisha.meghaconnect.face.validation.FacePhotoValidator;
import com.survisha.meghaconnect.service.AuditLogService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EpicFaceServiceTest {
    private EpicFaceClient client;
    private FacePhotoValidator validator;
    private AuditLogService audit;
    private EpicFaceService service;

    @BeforeEach void setUp() {
        client = mock(EpicFaceClient.class); validator = mock(FacePhotoValidator.class); audit = mock(AuditLogService.class);
        service = new EpicFaceService(client, validator, new SimpleMeterRegistry(), audit);
        when(validator.normalize(anyString(), eq("photo"))).thenReturn("normalized-photo");
    }

    @Test void searchMatchMapsProviderFields() {
        when(client.search("normalized-photo")).thenReturn(FaceSearch1NProviderResponse.builder()
                .matched(true).epicNumber("TUE0153403").name("Citizen").partNumber("17").photo("reference-photo").build());
        var result = service.search("data:image/jpeg;base64,photo", "deo");
        assertTrue(result.isMatched()); assertEquals("EPIC_FACE_1N", result.getSource());
        assertEquals("reference-photo", result.getEpicPhoto());
        verify(audit).log(eq("EpicFace"), eq(0L), eq("SEARCH_1N"), contains("matched=true"), eq("deo"));
    }

    @Test void searchNoMatchIsBusinessOutcome() {
        when(client.search("normalized-photo")).thenReturn(FaceSearch1NProviderResponse.builder().matched(false).build());
        var result = service.search("photo", "deo");
        assertFalse(result.isMatched()); assertEquals("NOT_FOUND", result.getProviderStatus());
    }

    @Test void verifyNoMatchIsBusinessOutcome() {
        when(client.verify("TUE0153403", "normalized-photo")).thenReturn(FaceVerify11ProviderResponse.builder().matched(false).build());
        var result = service.verify("tue0153403", "photo", "deo");
        assertFalse(result.isMatched()); assertEquals("NOT_MATCHED", result.getProviderStatus());
    }

    @Test void providerErrorIsSanitized() {
        when(client.search("normalized-photo")).thenReturn(FaceSearch1NProviderResponse.builder()
                .error(true).errorCode("INVALID_API_KEY<script>").errorDesc("secret provider detail").build());
        EpicFaceException error = assertThrows(EpicFaceException.class, () -> service.search("photo", "deo"));
        assertEquals(503, error.getHttpStatus()); assertFalse(error.getMessage().contains("secret provider detail"));
    }
}
