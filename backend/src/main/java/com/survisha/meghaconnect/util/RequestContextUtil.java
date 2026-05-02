package com.survisha.meghaconnect.util;

import org.slf4j.MDC;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Request-scoped correlation helpers backed by SLF4J MDC.
 */
public final class RequestContextUtil {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern SENSITIVE_MESSAGE_PATTERN = Pattern.compile(
            "(?i).*(password|passwd|pwd|aadhaar|aadhar|otp|token|authorization|bearer|api[-_ ]?key|secret|base64|photo|document\\s+content).*"
    );

    private RequestContextUtil() {
    }

    public static String resolveRequestId(String incomingRequestId) {
        if (incomingRequestId == null) {
            return generateRequestId();
        }

        String trimmed = incomingRequestId.trim();
        if (trimmed.isEmpty() || !SAFE_REQUEST_ID_PATTERN.matcher(trimmed).matches()) {
            return generateRequestId();
        }

        return trimmed;
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public static void setRequestId(String requestId) {
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
    }

    public static String getRequestId() {
        String requestId = MDC.get(MDC_REQUEST_ID_KEY);
        if (requestId == null || requestId.isBlank()) {
            requestId = generateRequestId();
            setRequestId(requestId);
        }
        return requestId;
    }

    public static void clear() {
        MDC.remove(MDC_REQUEST_ID_KEY);
    }

    public static Map<String, String> copyMdcContext() {
        return MDC.getCopyOfContextMap();
    }

    public static void restoreMdcContext(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(contextMap);
        }
    }

    public static Runnable wrap(Runnable delegate) {
        Map<String, String> parentContext = copyMdcContext();
        return () -> {
            Map<String, String> previousContext = copyMdcContext();
            try {
                restoreMdcContext(parentContext);
                delegate.run();
            } finally {
                restoreMdcContext(previousContext);
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> delegate) {
        Map<String, String> parentContext = copyMdcContext();
        return () -> {
            Map<String, String> previousContext = copyMdcContext();
            try {
                restoreMdcContext(parentContext);
                return delegate.call();
            } finally {
                restoreMdcContext(previousContext);
            }
        };
    }

    public static String sanitizeForClient(String message, String fallbackMessage) {
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }

        String normalized = message.replace('\r', ' ').replace('\n', ' ').trim();
        if (SENSITIVE_MESSAGE_PATTERN.matcher(normalized).matches()) {
            return fallbackMessage;
        }

        return normalized.length() > 300 ? normalized.substring(0, 300) : normalized;
    }

    public static String sanitizeForLog(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        String normalized = message.replace('\r', ' ').replace('\n', ' ').trim();
        if (SENSITIVE_MESSAGE_PATTERN.matcher(normalized).matches()) {
            return "[REDACTED]";
        }

        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    public static String safeUri(URI uri) {
        if (uri == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (uri.getScheme() != null) {
            builder.append(uri.getScheme()).append("://");
        }
        if (uri.getHost() != null) {
            builder.append(uri.getHost());
        }
        if (uri.getPort() > -1) {
            builder.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null) {
            builder.append(uri.getRawPath());
        }
        return builder.toString();
    }
}
