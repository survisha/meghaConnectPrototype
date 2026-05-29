package com.survisha.common.sms;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsServiceTest {

    @Test
    void registrationSuccessUsesRegistrationTemplate() {
        SmsClient smsClient = mockClient();
        SmsService service = new SmsServiceImpl(smsClient);

        service.sendRegistrationSuccessSms("9700423723", "REF123");

        ArgumentCaptor<SmsRequest> request = ArgumentCaptor.forClass(SmsRequest.class);
        verify(smsClient).sendSms(request.capture());
        assertEquals("9700423723", request.getValue().getMobileNumber());
        assertEquals(SmsTemplateConstants.REGISTRATION_SUCCESS, request.getValue().getTemplateId());
        assertEquals("Your registration on https://duraumduba.com/ has been completed successfully. Your reference ID is REF123.",
                request.getValue().getMessage());
    }

    @Test
    void otpSmsUsesMobileVerificationTemplate() {
        SmsClient smsClient = mockClient();
        SmsService service = new SmsServiceImpl(smsClient);

        service.sendOtpSms("9700423723", "123456");

        ArgumentCaptor<SmsRequest> request = ArgumentCaptor.forClass(SmsRequest.class);
        verify(smsClient).sendSms(request.capture());
        assertEquals("9700423723", request.getValue().getMobileNumber());
        assertEquals(SmsTemplateConstants.OTP_VERIFICATION, request.getValue().getTemplateId());
        org.junit.jupiter.api.Assertions.assertTrue(request.getValue().getMessage().contains("123456"));
    }

    @Test
    void appointmentApprovedUsesAppointmentApprovedTemplate() {
        SmsClient smsClient = mockClient();
        SmsService service = new SmsServiceImpl(smsClient);

        service.sendAppointmentApprovedSms("9700423723", "APT001");

        ArgumentCaptor<SmsRequest> request = ArgumentCaptor.forClass(SmsRequest.class);
        verify(smsClient).sendSms(request.capture());
        assertEquals("9700423723", request.getValue().getMobileNumber());
        assertEquals(SmsTemplateConstants.APPOINTMENT_APPROVED, request.getValue().getTemplateId());
        org.junit.jupiter.api.Assertions.assertTrue(request.getValue().getMessage().contains("APT001"));
    }

    private SmsClient mockClient() {
        SmsClient smsClient = mock(SmsClient.class);
        when(smsClient.sendSms(org.mockito.ArgumentMatchers.any(SmsRequest.class))).thenReturn(SmsResponse.builder()
                .success(true)
                .statusCode(200)
                .provider("ping4sms")
                .message("ok")
                .build());
        return smsClient;
    }
}
