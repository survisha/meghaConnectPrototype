package com.survisha.meghaconnect.epic.face.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class EpicFaceVerifyRequest {
    @NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}[0-9]{7}$") private String epicNumber;
    @NotBlank private String photo;
}
