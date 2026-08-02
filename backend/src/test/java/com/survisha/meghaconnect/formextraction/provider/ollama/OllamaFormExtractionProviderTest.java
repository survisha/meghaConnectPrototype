package com.survisha.meghaconnect.formextraction.provider.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.provider.*;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OllamaFormExtractionProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void sendsVisionStructuredChatContractAndMapsResponse() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            captured.set(chain.request());
            return response(chain.request(), 200, successfulBody());
        }).build();
        FormExtractionProperties properties = properties();
        OllamaFormExtractionProvider provider = provider(client, properties);

        VisitorFormExtractionResult result = provider.extract(FormExtractionInput.builder()
                .imageBytes(new byte[]{1,2,3,4}).mimeType("image/png").formType("VISITOR_REGISTRATION")
                .formVersion("V1").languageHint("en").requestId("test-request").build());

        Buffer buffer = new Buffer();
        captured.get().body().writeTo(buffer);
        JsonNode request = mapper.readTree(buffer.readUtf8());
        assertEquals("http://127.0.0.1:11434/api/chat", captured.get().url().toString());
        assertNull(captured.get().header("Authorization"));
        assertEquals("qwen2.5vl:3b", request.path("model").asText());
        assertFalse(request.path("stream").asBoolean(true));
        assertEquals("AQIDBA==", request.path("messages").get(1).path("images").get(0).asText());
        assertEquals("object", request.path("format").path("type").asText());
        assertEquals(0.0, request.path("options").path("temperature").asDouble());
        assertEquals(AIProviderType.OLLAMA, result.getProvider());
        assertEquals("Rahul", result.getExtractedName().getValue());
    }

    @Test void rejectsBlankStructuredContent() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                response(chain.request(), 200, "{\"model\":\"qwen2.5vl:7b\",\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true}"))
                .build();
        assertThrows(RuntimeException.class, () -> provider(client, properties()).extract(input()));
    }

    @Test void mapsModelNotFoundToUnavailable() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                response(chain.request(), 404, "{\"error\":\"model not found\"}")).build();
        var exception = assertThrows(com.survisha.meghaconnect.formextraction.exception.FormExtractionException.class,
                () -> provider(client, properties()).extract(input()));
        assertEquals(503, exception.getHttpStatus());
    }

    private OllamaFormExtractionProvider provider(OkHttpClient client, FormExtractionProperties properties) {
        ProviderResultMapper resultMapper = new ProviderResultMapper(mapper);
        return new OllamaFormExtractionProvider(client, mapper, properties,
                new VisitorFormExtractionPromptBuilder(), new VisitorFormExtractionSchema(), resultMapper);
    }
    private FormExtractionProperties properties() {
        FormExtractionProperties p = new FormExtractionProperties();
        p.setEnabled(true); p.setProvider(AIProviderType.OLLAMA);
        return p;
    }
    private FormExtractionInput input() {
        return FormExtractionInput.builder().imageBytes(new byte[]{1}).mimeType("image/png")
                .formType("VISITOR_REGISTRATION").formVersion("V1").languageHint("en").requestId("r").build();
    }
    private Response response(Request request, int code, String body) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code)
                .message(code == 200 ? "OK" : "Error")
                .body(ResponseBody.create(body, MediaType.get("application/json"))).build();
    }
    private String successfulBody() {
        return "{\"model\":\"qwen2.5vl:7b\",\"message\":{\"role\":\"assistant\",\"content\":" +
                "\"{\\\"epic\\\":\\\"ABC1234567\\\",\\\"name\\\":\\\"Rahul\\\",\\\"mobile\\\":\\\"9876543210\\\",\\\"address\\\":\\\"Shillong\\\"," +
                "\\\"warnings\\\":[],\\\"requiresManualReview\\\":true}\"},\"done\":true}";
    }
}
