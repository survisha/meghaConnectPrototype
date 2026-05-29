package com.survisha.common.sms;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsClientTest {

    @Test
    void buildUriEncodesMessageAndAddsProviderParameters() {
        SmsClient client = new SmsClient(new RestTemplate(), enabledProperties());

        URI uri = client.buildUri("9700423723", "Your registration has been completed successfully.", "1707177995193042911");

        String value = uri.toString();
        assertThat(value, containsString("key=test-key"));
        assertThat(value, containsString("route=2"));
        assertThat(value, containsString("sender=DEUPL"));
        assertThat(value, containsString("number=9700423723"));
        assertThat(value, containsString("sms=Your%20registration%20has%20been%20completed%20successfully."));
        assertThat(value, containsString("templateid=1707177995193042911"));
    }

    @Test
    void sendSmsReturnsSuccessForSuccessfulProviderResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SmsClient client = new SmsClient(restTemplate, enabledProperties());

        server.expect(requestTo(containsString("templateid=1707177995193042911")))
                .andRespond(withSuccess("SMS Submitted", MediaType.TEXT_PLAIN));

        SmsResponse response = client.sendSms("9700423723", "Registration completed.", "1707177995193042911");

        assertTrue(response.isSuccess());
        assertThat(response.getRawResponse(), containsString("SMS Submitted"));
        server.verify();
    }

    @Test
    void sendSmsDoesNotCallProviderWhenDisabled() {
        SmsClient client = new SmsClient(new RestTemplate(), disabledProperties());

        SmsResponse response = client.sendSms("9700423723", "Registration completed.", "1707177995193042911");

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), containsString("disabled"));
    }

    @Test
    void sendSmsReturnsFailureForProviderError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SmsClient client = new SmsClient(restTemplate, enabledProperties());

        server.expect(requestTo(containsString("number=9700423723")))
                .andRespond(withServerError());

        SmsResponse response = client.sendSms("9700423723", "Registration completed.", "1707177995193042911");

        assertFalse(response.isSuccess());
        server.verify();
    }

    private SmsProperties enabledProperties() {
        SmsProperties properties = disabledProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        return properties;
    }

    private SmsProperties disabledProperties() {
        SmsProperties properties = new SmsProperties();
        properties.setEnabled(false);
        properties.setProvider("ping4sms");
        properties.setBaseUrl("https://site.ping4sms.com/api/smsapi");
        properties.setApiKey("");
        properties.setRoute("2");
        properties.setSenderId("DEUPL");
        return properties;
    }
}
