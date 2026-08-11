package com.survisha.meghaconnect.epic.face.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provider contract for POST /FaceService1N. Never expose this DTO to clients. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceSearch1NProviderRequest {
    private String apiKey;
    /** Raw Base64 JPEG/PNG bytes without a data-URL prefix. */
    private String photo;
}
