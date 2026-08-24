package com.survisha.meghaconnect.legacy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.legacy.entity.LegacyImportSheet;
import com.survisha.meghaconnect.legacy.repository.*;
import com.survisha.meghaconnect.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test void acceptsWorkbookAtConfiguredFiftyMbBoundary() {
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 50L);
        MultipartFile file = workbook(50L * 1024 * 1024);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateUpload", file));
    }

    @Test void rejectsWorkbookAboveConfiguredFiftyMbBoundary() {
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 50L);
        MultipartFile file = workbook(50L * 1024 * 1024 + 1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateUpload", file));
        assertEquals("Legacy Excel file exceeds the maximum allowed size of 50 MB.", error.getMessage());
    }

    @Test void typeInferenceHandlesStringCellWithoutNumericAccess() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mixed data");
            sheet.createRow(0).createCell(0).setCellValue("Amount");
            sheet.createRow(1).createCell(0).setCellValue("Not available");

            String detected = ReflectionTestUtils.invokeMethod(service, "inferType", sheet, 0, 0);

            assertEquals("STRING", detected);
        }
    }

    @Test void importGuardRejectsUnmappedSheets() {
        LegacyImportSheet sheet = LegacyImportSheet.builder()
                .id(101L).sheetName("Sheet1").status("MAPPING_REQUIRED").build();

        MeghaConnectException error = assertThrows(MeghaConnectException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateReadyForImport", List.of(sheet)));

        assertEquals("LEGACY_IMPORT_MAPPING_INCOMPLETE", error.getErrorCode());
        assertEquals(409, error.getHttpStatus());
        assertEquals("Mapping is incomplete for 1 sheet.", error.getMessage());
    }

    @Test void importGuardRejectsBatchContainingOnlySkippedSheets() {
        LegacyImportSheet sheet = LegacyImportSheet.builder()
                .id(102L).sheetName("Empty").status("SKIPPED").build();

        MeghaConnectException error = assertThrows(MeghaConnectException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateReadyForImport", List.of(sheet)));

        assertEquals("LEGACY_IMPORT_NO_IMPORTABLE_SHEETS", error.getErrorCode());
        assertEquals(409, error.getHttpStatus());
    }

    private MultipartFile workbook(long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(size);
        when(file.getOriginalFilename()).thenReturn("legacy.xlsx");
        when(file.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return file;
    }
}
