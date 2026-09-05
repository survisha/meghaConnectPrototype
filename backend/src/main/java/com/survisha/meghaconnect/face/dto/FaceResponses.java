package com.survisha.meghaconnect.face.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

public final class FaceResponses {
    private FaceResponses() {}
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL) public static class Enroll {
        boolean success; String enrollmentId; String message;
    }
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL) public static class Compare {
        boolean success; boolean identical; Double distance; String message;
    }
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL) public static class Delete {
        boolean success; String enrollmentId; String message;
    }
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL) public static class Search {
        boolean success; boolean matched; String enrollmentId; String name; Double distance; Double score;
        String matchedPhoto; String message; MatchedVisitor visitor;
    }
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL) public static class MatchedVisitor {
        Long id; String fullName; String phoneNumber; String epicNumber; String designation;
        String address; String district; String constituency; String kycStatus; String photoUrl; String photoBase64;
    }
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL) public static class Verify {
        boolean success; boolean verified; Double distance; Double score; String enrollmentId; String message;
    }
}
