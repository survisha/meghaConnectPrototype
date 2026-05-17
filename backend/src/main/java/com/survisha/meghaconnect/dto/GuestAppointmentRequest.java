package com.survisha.meghaconnect.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class GuestAppointmentRequest {
    private String fullName;
    private String mobileNumber;
    private String address;
    private String email;
    private String organizationName;
    private String designation;
    private String visitorCategory;
    private String referredOffice;
    private String referredByName;
    private String reasonForAppointment;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate preferredDate;
    private String remarks;
    private MultipartFile supportingDocument;
}
