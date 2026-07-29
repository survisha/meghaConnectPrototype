package com.survisha.meghaconnect.formextraction.dto;

import lombok.Data;

@Data
public class ExtractedVisitorField<T> {
    private T value;
    private FieldStatus status;
    private Confidence confidence;
    private String reason;
    private boolean valid;

    public enum FieldStatus { EXTRACTED, NOT_FOUND, UNREADABLE, AMBIGUOUS, CROSSED_OUT }
    public enum Confidence { HIGH, MEDIUM, LOW, NONE }
}
