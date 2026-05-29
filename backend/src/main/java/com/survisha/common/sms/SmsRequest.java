package com.survisha.common.sms;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SmsRequest {
    String mobileNumber;
    String message;
    String templateId;
}
