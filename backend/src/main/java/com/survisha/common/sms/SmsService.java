package com.survisha.common.sms;

public interface SmsService {

    SmsResponse sendRegistrationSuccessSms(String mobileNumber, String referenceId);

    SmsResponse sendOtpSms(String mobileNumber, String otp);

    SmsResponse sendLoginOtpSms(String mobileNumber, String otp);

    SmsResponse sendAppointmentCreatedSms(String mobileNumber, String appointmentNo);

    SmsResponse sendAppointmentApprovedSms(String mobileNumber, String appointmentNo);

    SmsResponse sendAppointmentRejectedSms(String mobileNumber, String appointmentNo);

    SmsResponse sendFollowupScheduledSms(String mobileNumber, String appointmentNo, String date);

    SmsResponse sendQrPassSms(String mobileNumber, String passNo);

    SmsResponse sendVisitorCheckInSms(String mobileNumber, String visitorName);

    SmsResponse sendVisitorCheckOutSms(String mobileNumber, String visitorName);
}
