package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.response.ApiResponse;
import com.survisha.meghaconnect.response.ErrorResponse;
import com.survisha.meghaconnect.util.RequestContextUtil;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class RequestIdResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        String requestId = RequestContextUtil.getRequestId();
        response.getHeaders().set(RequestContextUtil.REQUEST_ID_HEADER, requestId);

        if (body == null || body instanceof String || body instanceof byte[] || body instanceof Resource) {
            return body;
        }

        if (body instanceof ApiResponse<?> apiResponse) {
            apiResponse.setRequestId(requestId);
            return apiResponse;
        }

        if (body instanceof ErrorResponse errorResponse) {
            errorResponse.setRequestId(requestId);
            return errorResponse;
        }

        if (body instanceof Map<?, ?> mapBody && isJsonResponse(selectedContentType)) {
            Map<Object, Object> enriched = new LinkedHashMap<>(mapBody);
            enriched.putIfAbsent(RequestContextUtil.REQUEST_ID_ATTRIBUTE, requestId);
            return enriched;
        }

        return body;
    }

    private boolean isJsonResponse(MediaType mediaType) {
        return mediaType == null
                || MediaType.APPLICATION_JSON.includes(mediaType)
                || (mediaType.getSubtype() != null && mediaType.getSubtype().endsWith("+json"));
    }
}
