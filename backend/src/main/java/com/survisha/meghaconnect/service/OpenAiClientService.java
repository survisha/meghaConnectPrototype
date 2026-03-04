package com.survisha.meghaconnect.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Thin wrapper around the {@link OpenAiService} from
 * {@code com.theokanning.openai-gpt3-java}.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>If {@code meghaconnect.ai.api-key} is set, a live {@link OpenAiService}
 *       client is initialised at startup. All calls are forwarded to OpenAI.</li>
 *   <li>If the key is absent or blank, {@link #isAvailable()} returns
 *       {@code false} and callers fall back to the rule-based engine.</li>
 * </ul>
 *
 * <p>Default model: {@code gpt-3.5-turbo} (configurable via
 * {@code meghaconnect.ai.model}).
 *
 * <p>The timeout for each API call is configurable via
 * {@code meghaconnect.ai.timeout-seconds} (default 60 s).
 */
@Service
public class OpenAiClientService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientService.class);

    // ── Configuration ─────────────────────────────────────────────────────────

    @Value("${meghaconnect.ai.api-key:}")
    private String apiKey;

    @Value("${meghaconnect.ai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${meghaconnect.ai.timeout-seconds:60}")
    private int timeoutSeconds;

    /** Maximum tokens to request in a single completion. */
    @Value("${meghaconnect.ai.max-tokens:512}")
    private int maxTokens;

    // ── Runtime state ─────────────────────────────────────────────────────────

    private OpenAiService client;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            client = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
            log.info("OpenAI client initialised (model={}, timeout={}s)", model, timeoutSeconds);
        } else {
            log.info("meghaconnect.ai.api-key not configured – AI features will use rule-based fallback.");
        }
    }

    /** Returns {@code true} if a live OpenAI API key is configured. */
    public boolean isAvailable() {
        return client != null;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Send a chat completion request with a single user message.
     *
     * @param systemPrompt instructions for the model (role = system)
     * @param userMessage  the actual user content (role = user)
     * @return model response text, or {@link Optional#empty()} on error / unavailable
     */
    public Optional<String> chat(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                            new ChatMessage(ChatMessageRole.USER.value(), userMessage)
                    ))
                    .maxTokens(maxTokens)
                    .temperature(0.3)   // lower temperature for structured extractions
                    .build();

            String reply = client.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            return Optional.ofNullable(reply != null ? reply.trim() : null);
        } catch (Exception e) {
            log.warn("OpenAI chat call failed – falling back to rule-based engine: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Convenience overload with a lower max-token budget for short responses
     * (e.g. single-word priority labels).
     */
    public Optional<String> chatCompact(String systemPrompt, String userMessage, int maxTok) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                            new ChatMessage(ChatMessageRole.USER.value(), userMessage)
                    ))
                    .maxTokens(maxTok)
                    .temperature(0.0)   // deterministic for classification tasks
                    .build();

            String reply = client.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            return Optional.ofNullable(reply != null ? reply.trim() : null);
        } catch (Exception e) {
            log.warn("OpenAI compact chat call failed – falling back to rule-based engine: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
