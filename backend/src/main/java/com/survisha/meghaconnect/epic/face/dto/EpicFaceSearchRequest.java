package com.survisha.meghaconnect.epic.face.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class EpicFaceSearchRequest {
    @NotBlank private String photo;
    private Double latitude;
    private Double longitude;
    private String source;
}
