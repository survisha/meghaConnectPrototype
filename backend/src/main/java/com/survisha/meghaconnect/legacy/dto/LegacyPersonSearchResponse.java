package com.survisha.meghaconnect.legacy.dto;
import lombok.*;
import java.util.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LegacyPersonSearchResponse {
    private int page; private int limit; private long totalMatches;
    @Builder.Default private List<Candidate> matches=new ArrayList<>();
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Candidate {
        private int matchScore; private String matchLevel; private boolean manualVerificationRequired;
        @Builder.Default private List<String> matchedOn=new ArrayList<>();
        private Person legacyPerson;
        @Builder.Default private List<DatasetRecord> datasets=new ArrayList<>();
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Person {private String name;private String epic;private String mobile;private String village;private String address;private String district;private String constituency;}
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DatasetRecord {private String datasetCode;private String datasetName;private String schemeName;private Long sourceRecordId;private String sourceFile;private String sourceSheet;private long sourceRowNumber;private Map<String,Object> details;}
}
