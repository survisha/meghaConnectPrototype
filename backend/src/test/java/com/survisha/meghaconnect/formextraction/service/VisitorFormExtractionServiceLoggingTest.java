package com.survisha.meghaconnect.formextraction.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisitorFormExtractionServiceLoggingTest {
    @Test void masksDiagnosticValues() {
        assertEquals("******4567",VisitorFormExtractionServiceImpl.maskEpic("ABC1234567"));
        assertEquals("4567",VisitorFormExtractionServiceImpl.mobileLast4("9876544567"));
        assertEquals("R****",VisitorFormExtractionServiceImpl.maskName("Rahul"));
        assertFalse(VisitorFormExtractionServiceImpl.maskEpic("ABC1234567").contains("ABC123"));
        assertFalse(VisitorFormExtractionServiceImpl.maskName("Rahul").contains("Rahul"));
    }
}
