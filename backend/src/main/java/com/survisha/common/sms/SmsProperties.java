package com.survisha.common.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private boolean enabled;
    private String provider = "ping4sms";
    private String baseUrl = "https://site.ping4sms.com/api/smsapi";
    private String apiKey;
    private String route = "2";
    private String senderId = "DEUPL";
}
