package com.survisha.meghaconnect.epic.face.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provider contract for POST /FaceService11. Never expose this DTO to clients. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerify11ProviderRequest {
    private String apiKey;
    private String epicNumber;
    /** Raw Base64 JPEG/PNG bytes without a data-URL prefix. */
    private String photo;
}
