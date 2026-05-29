package com.survisha.common.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private static final String PORTAL_URL = "https://duraumduba.com/";
    private static final String OTP_VALIDITY_MINUTES = "5";

    private final SmsClient smsClient;

    @Override
    public SmsResponse sendRegistrationSuccessSms(String mobileNumber, String referenceId) {
        return send(mobileNumber,
                "Your registration on " + PORTAL_URL + " has been completed successfully. Your reference ID is " + referenceId + ".",
                SmsTemplateConstants.REGISTRATION_SUCCESS);
    }

    @Override
    public SmsResponse sendOtpSms(String mobileNumber, String otp) {
        return send(mobileNumber,
                "Your OTP for mobile number verification on " + PORTAL_URL + " is " + otp
                        + ". It is valid for " + OTP_VALIDITY_MINUTES + " minutes. Do not share this OTP with anyone.",
                SmsTemplateConstants.OTP_VERIFICATION);
    }

    @Override
    public SmsResponse sendLoginOtpSms(String mobileNumber, String otp) {
        return send(mobileNumber,
                "Your " + PORTAL_URL + " login OTP is " + otp + ". This OTP is valid for "
                        + OTP_VALIDITY_MINUTES + " minutes. Do not share it with anyone.",
                SmsTemplateConstants.LOGIN_OTP);
    }

    @Override
    public SmsResponse sendAppointmentCreatedSms(String mobileNumber, String appointmentNo) {
        return send(mobileNumber,
                "Dear citizen, your appointment " + PORTAL_URL + " request has been submitted successfully. Your request ID is "
                        + appointmentNo + ". You will be notified once it is reviewed.",
                SmsTemplateConstants.APPOINTMENT_CREATED);
    }

    @Override
    public SmsResponse sendAppointmentApprovedSms(String mobileNumber, String appointmentNo) {
        return send(mobileNumber,
                "Dear citizen, your appointment " + PORTAL_URL + " request " + appointmentNo
                        + " has been approved. Please visit on the scheduled date.",
                SmsTemplateConstants.APPOINTMENT_APPROVED);
    }

    @Override
    public SmsResponse sendAppointmentRejectedSms(String mobileNumber, String appointmentNo) {
        return send(mobileNumber,
                "Dear citizen, your appointment " + PORTAL_URL + " request " + appointmentNo
                        + " could not be approved. Reason: Not approved.",
                SmsTemplateConstants.APPOINTMENT_REJECTED);
    }

    @Override
    public SmsResponse sendFollowupScheduledSms(String mobileNumber, String appointmentNo, String date) {
        return send(mobileNumber,
                "Dear citizen, your appointment " + PORTAL_URL + " request " + appointmentNo
                        + " has been scheduled for Public Connect on " + date + ". Please carry your reference ID.",
                SmsTemplateConstants.FOLLOWUP_SCHEDULED);
    }

    @Override
    public SmsResponse sendQrPassSms(String mobileNumber, String passNo) {
        return send(mobileNumber,
                "Dear citizen, your entry pass for " + PORTAL_URL + " has been generated. Pass ID: "
                        + passNo + ". Please show the QR code at the entry gate -DURA ENTERPRISES UMDUBA",
                SmsTemplateConstants.QR_PASS_GENERATED);
    }

    @Override
    public SmsResponse sendVisitorCheckInSms(String mobileNumber, String visitorName) {
        return send(mobileNumber,
                "Dear " + visitorName + ", your entry " + PORTAL_URL
                        + " has been recorded successfully at office -DURA ENTERPRISES UMDUBA",
                SmsTemplateConstants.VISITOR_CHECKIN);
    }

    @Override
    public SmsResponse sendVisitorCheckOutSms(String mobileNumber, String visitorName) {
        return send(mobileNumber,
                "Dear " + visitorName + ", your exit " + PORTAL_URL
                        + " has been recorded successfully at office. Thank you.",
                SmsTemplateConstants.VISITOR_CHECKOUT);
    }

    private SmsResponse send(String mobileNumber, String message, String templateId) {
        return smsClient.sendSms(SmsRequest.builder()
                .mobileNumber(mobileNumber)
                .message(message)
                .templateId(templateId)
                .build());
    }
}
