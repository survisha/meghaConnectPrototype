package com.survisha.meghaconnect.service;

import java.util.List;

public interface DocumentVisionSummaryProvider {
    VisionSummary summarize(List<byte[]> images);
    record VisionSummary(String aiSummary, String importantDetails, String missingInfo, String riskFlags, String modelName, String rawResponse, long durationMs) {}
}
