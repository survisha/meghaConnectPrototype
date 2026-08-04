package com.survisha.common.sms;

import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import com.survisha.meghaconnect.monitoring.MonitoredOperation;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsClient {

    private final RestTemplate restTemplate;
    private final SmsProperties properties;

    @MonitoredOperation("sms_provider_call")
    public SmsResponse sendSms(SmsRequest request) {
        if (request == null) {
            return failure("SMS request is required.", "");
        }
        return sendSms(request.getMobileNumber(), request.getMessage(), request.getTemplateId());
    }

    @MonitoredOperation("sms_provider_call")
    public SmsResponse sendSms(String mobileNumber, String message, String templateId) {
        String normalizedMobile = normalizeMobileNumber(mobileNumber);
        try {
            validateRequest(normalizedMobile, message, templateId);
        } catch (IllegalArgumentException e) {
            log.error("SMS request validation failed mobile={} templateId={} requestId={} error={}",
                    RequestContextUtil.maskPhone(normalizedMobile), templateId, RequestContextUtil.getRequestId(), e.getMessage());
            return failure(e.getMessage(), properties.getProvider());
        }

        if (!properties.isEnabled()) {
            log.info("SMS sending disabled by configuration. mobile={} templateId={}",
                    RequestContextUtil.maskPhone(normalizedMobile), templateId);
            return SmsResponse.builder()
                    .success(true)
                    .statusCode(0)
                    .provider(properties.getProvider())
                    .message("SMS sending disabled by configuration.")
                    .build();
        }

        URI uri = buildUri(normalizedMobile, message, templateId);
        log.info("Sending SMS to mobile {} using template {} requestId={}",
                RequestContextUtil.maskPhone(normalizedMobile), templateId, RequestContextUtil.getRequestId());

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            return SmsResponse.builder()
                    .success(success)
                    .statusCode(response.getStatusCodeValue())
                    .provider(properties.getProvider())
                    .message(success ? "SMS API request completed." : "SMS API request failed.")
                    .rawResponse(response.getBody())
                    .build();
        } catch (RestClientException e) {
            log.error("Failed to send SMS for mobile {} templateId={} requestId={} error={}",
                    RequestContextUtil.maskPhone(normalizedMobile), templateId, RequestContextUtil.getRequestId(), e.getMessage());
            return failure(e.getMessage(), properties.getProvider());
        }
    }

    URI buildUri(String mobileNumber, String message, String templateId) {
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .queryParam("key", properties.getApiKey())
                .queryParam("route", properties.getRoute())
                .queryParam("sender", properties.getSenderId())
                .queryParam("number", mobileNumber)
                .queryParam("sms", message)
                .queryParam("templateid", templateId)
                .build()
                .encode()
                .toUri();
    }

    private void validateRequest(String mobileNumber, String message, String templateId) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalArgumentException("SMS base URL is required.");
        }
        if (properties.isEnabled() && !StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalArgumentException("SMS API key is required when SMS is enabled.");
        }
        if (!mobileNumber.matches("\\d{10}")) {
            throw new IllegalArgumentException("SMS mobile number must be 10 digits.");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("SMS message is required.");
        }
        if (!StringUtils.hasText(templateId)) {
            throw new IllegalArgumentException("SMS template ID is required.");
        }
    }

    private String normalizeMobileNumber(String mobileNumber) {
        return mobileNumber == null ? "" : mobileNumber.replaceAll("\\D", "");
    }

    private SmsResponse failure(String message, String provider) {
        return SmsResponse.builder()
                .success(false)
                .statusCode(0)
                .provider(provider)
                .message(message)
                .build();
    }
}
