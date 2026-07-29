package com.survisha.meghaconnect.face.dto;

import lombok.Data;
import javax.validation.constraints.*;

public final class FaceRequests {
    private FaceRequests() {}

    @Data public static class Enroll {
        @NotBlank @Size(max=100) private String enrollmentId;
        @NotBlank @Size(max=200) private String name;
        @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
        @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
        @NotBlank private String photo;
    }
    @Data public static class Compare {
        @NotBlank private String photo1;
        @NotBlank private String photo2;
    }
    @Data public static class Delete {
        @NotBlank @Size(max=100) private String enrollmentId;
    }
    @Data public static class Search {
        @NotBlank private String photo;
        @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
        @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
        private Boolean includeMatchedPhoto = false;
    }
    @Data public static class Verify {
        @NotBlank @Size(max=100) private String enrollmentId;
        @NotBlank private String photo;
        @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
        @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
    }
}
