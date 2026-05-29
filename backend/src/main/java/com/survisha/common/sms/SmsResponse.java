package com.survisha.common.sms;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SmsResponse {
    boolean success;
    int statusCode;
    String provider;
    String message;
    String rawResponse;
}
