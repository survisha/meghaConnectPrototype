package com.survisha.meghaconnect.epic.face.dto.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EpicFaceProviderDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsFaceSearch1NContract() throws Exception {
        FaceSearch1NProviderRequest request = FaceSearch1NProviderRequest.builder()
                .apiKey("configured-secret").photo("raw-base64").build();
        String requestJson = objectMapper.writeValueAsString(request);
        assertTrue(requestJson.contains("\"apiKey\":\"configured-secret\""));
        assertTrue(requestJson.contains("\"photo\":\"raw-base64\""));

        FaceSearch1NProviderResponse response = objectMapper.readValue(matchedResponse(), FaceSearch1NProviderResponse.class);
        assertFalse(response.isError());
        assertTrue(response.isMatched());
        assertEquals("TUE0153403", response.getEpicNumber());
        assertEquals("17", response.getPartNumber());
        assertEquals("base64-epic-photo", response.getPhoto());
    }

    @Test
    void mapsFaceVerify11Contract() throws Exception {
        FaceVerify11ProviderRequest request = FaceVerify11ProviderRequest.builder()
                .apiKey("configured-secret").epicNumber("TUE0153403").photo("raw-base64").build();
        String requestJson = objectMapper.writeValueAsString(request);
        assertTrue(requestJson.contains("\"epicNumber\":\"TUE0153403\""));

        FaceVerify11ProviderResponse response = objectMapper.readValue(matchedResponse(), FaceVerify11ProviderResponse.class);
        assertFalse(response.isError());
        assertTrue(response.isMatched());
        assertEquals("Shillong", response.getAcpcName());
        assertEquals("793001", response.getPincode());
    }

    private String matchedResponse() {
        return "{\"error\":false,\"errorCode\":null,\"errorDesc\":null,\"matched\":true,"
                + "\"epicNumber\":\"TUE0153403\",\"name\":\"Test Citizen\",\"address\":\"Test Address\","
                + "\"serialNumber\":\"123\",\"partNumber\":\"17\",\"partName\":\"Test Part\","
                + "\"acpcName\":\"Shillong\",\"district\":\"East Khasi Hills\",\"pincode\":\"793001\","
                + "\"photo\":\"base64-epic-photo\"}";
    }
}
