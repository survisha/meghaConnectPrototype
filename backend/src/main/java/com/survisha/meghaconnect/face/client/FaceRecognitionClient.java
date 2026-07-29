package com.survisha.meghaconnect.face.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface FaceRecognitionClient {
    JsonNode post(String operation, Map<String, Object> request);
}
