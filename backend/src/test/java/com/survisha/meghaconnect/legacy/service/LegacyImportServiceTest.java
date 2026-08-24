package com.survisha.meghaconnect.legacy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.legacy.repository.*;
import com.survisha.meghaconnect.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LegacyImportServiceTest {
    private LegacyImportService service;

    @BeforeEach
    void setUp() {
        service = new LegacyImportService(
                mock(LegacyDatasetDefinitionRepository.class), mock(LegacyImportBatchRepository.class),
                mock(LegacyImportSheetRepository.class), mock(LegacyImportColumnRepository.class),
                mock(LegacyImportErrorRepository.class), mock(LegacyDatasetRecordRepository.class),
                mock(LegacyPersonIndexRepository.class), mock(AuditLogService.class),
                new ObjectMapper());
    }

    @Test void normalizesWhitespaceCaseAndHarmlessPunctuation() {
        assertEquals("BENEFICIARY_NAME", service.normalizeHeader("  Beneficiary   Name  "));
        assertEquals("EPIC_NO", service.normalizeHeader("EPIC No."));
        assertEquals("VOTER_ID", service.normalizeHeader("Voter-ID"));
    }
}
