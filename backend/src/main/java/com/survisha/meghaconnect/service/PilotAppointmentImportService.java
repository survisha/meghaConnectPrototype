package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.PilotImportResultDto;
import com.survisha.meghaconnect.dto.PilotImportRowResultDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class PilotAppointmentImportService {

    private static final String COL_SR_NO = "srno";
    private static final String COL_NAME = "name";
    private static final String COL_PHONE_NUMBER = "phonenumber";
    private static final String COL_ADDRESS_LOCATION = "addresslocation";
    private static final String COL_AGENDA = "descriptionofagendapurpose";
    private static final Pattern DIGIT_RUN = Pattern.compile("\\d(?:[\\s\\u00A0-]*\\d){4,}");

    private final VisitorService visitorService;
    private final AppointmentService appointmentService;
    private final TransactionTemplate transactionTemplate;

    public PilotImportResultDto importPilotSheet(MultipartFile file, String actor) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty Excel file.");
        }

        DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
        List<PilotImportRowResultDto> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Uploaded Excel file does not contain a sheet.");
            }

            Row headerRow = findHeaderRow(sheet, formatter);
            if (headerRow == null) {
                throw new IllegalArgumentException("Uploaded Excel file does not contain any readable rows.");
            }

            Map<String, Integer> columns = readColumns(headerRow, formatter);
            applyDefaultColumn(columns, COL_SR_NO, 0, headerRow);
            applyDefaultColumn(columns, COL_NAME, 1, headerRow);
            applyDefaultColumn(columns, COL_PHONE_NUMBER, 2, headerRow);
            applyDefaultColumn(columns, COL_ADDRESS_LOCATION, 3, headerRow);
            applyDefaultColumn(columns, COL_AGENDA, 4, headerRow);

            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankDataRow(row, columns, formatter)) {
                    continue;
                }
                rows.add(importRow(row, columns, formatter, actor));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded Excel file.", e);
        }

        int imported = (int) rows.stream().filter(row -> Boolean.TRUE.equals(row.getSuccess())).count();
        int failed = rows.size() - imported;
        return PilotImportResultDto.builder()
                .success(failed == 0)
                .totalRows(rows.size())
                .importedCount(imported)
                .failedCount(failed)
                .message("Imported " + imported + " of " + rows.size() + " pilot appointment rows.")
                .rows(rows)
                .build();
    }

    private PilotImportRowResultDto importRow(Row row, Map<String, Integer> columns,
                                             DataFormatter formatter, String actor) {
        int excelRowNumber = row.getRowNum() + 1;
        String srNo = cleanSingleLine(cellValue(row, columns, COL_SR_NO, formatter));
        String rawName = cleanSingleLine(cellValue(row, columns, COL_NAME, formatter));
        String phoneNumber = firstPhoneNumber(cellValue(row, columns, COL_PHONE_NUMBER, formatter));
        String addressLocation = cleanSingleLine(cellValue(row, columns, COL_ADDRESS_LOCATION, formatter));
        String agendaBrief = cleanMultiline(cellValue(row, columns, COL_AGENDA, formatter));
        ParsedName parsedName = parseName(rawName, excelRowNumber);

        try {
            PilotImportRowResultDto result = transactionTemplate.execute(status -> {
                Visitor visitor = visitorService.registerPilotImportedVisitor(
                        parsedName.fullName(), phoneNumber, addressLocation, parsedName.briefProfile(), actor);
                Appointment appointment = appointmentService.createPilotImportedAppointment(visitor, agendaBrief, actor);

                return PilotImportRowResultDto.builder()
                        .rowNumber(excelRowNumber)
                        .srNo(srNo)
                        .success(true)
                        .name(visitor.getFullName())
                        .phoneNumber(visitor.getPhoneNumber())
                        .visitorId(visitor.getId())
                        .appointmentId(appointment.getId())
                        .applicationId(appointment.getApplicationId())
                        .message("Imported")
                        .build();
            });
            return result != null ? result : failedRow(excelRowNumber, srNo, parsedName.fullName(), phoneNumber,
                    "Row could not be imported.");
        } catch (RuntimeException e) {
            log.warn("Pilot appointment row import failed row={} srNo={} message={}",
                    excelRowNumber, srNo, e.getMessage());
            return failedRow(excelRowNumber, srNo, parsedName.fullName(), phoneNumber, e.getMessage());
        }
    }

    private Row findHeaderRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = readColumns(row, formatter);
            if (columns.containsKey(COL_NAME) || columns.containsKey(COL_PHONE_NUMBER)) {
                return row;
            }
        }
        return sheet.getRow(sheet.getFirstRowNum());
    }

    private Map<String, Integer> readColumns(Row row, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        if (row == null) {
            return columns;
        }
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return columns;
        }
        for (int index = 0; index < lastCellNum; index++) {
            String header = normalizeHeader(cellValue(row.getCell(index), formatter));
            if (!header.isEmpty()) {
                columns.putIfAbsent(header, index);
            }
        }
        return columns;
    }

    private void applyDefaultColumn(Map<String, Integer> columns, String key, int index, Row headerRow) {
        if (!columns.containsKey(key) && headerRow != null && index < headerRow.getLastCellNum()) {
            columns.put(key, index);
        }
    }

    private boolean isBlankDataRow(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        return cleanSingleLine(cellValue(row, columns, COL_NAME, formatter)).isEmpty()
                && cleanSingleLine(cellValue(row, columns, COL_PHONE_NUMBER, formatter)).isEmpty()
                && cleanSingleLine(cellValue(row, columns, COL_ADDRESS_LOCATION, formatter)).isEmpty()
                && cleanMultiline(cellValue(row, columns, COL_AGENDA, formatter)).isEmpty();
    }

    private String cellValue(Row row, Map<String, Integer> columns, String key, DataFormatter formatter) {
        Integer index = columns.get(key);
        if (row == null || index == null) {
            return "";
        }
        return cellValue(row.getCell(index), formatter);
    }

    private String cellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell);
    }

    private ParsedName parseName(String rawName, int excelRowNumber) {
        String cleanedName = cleanSingleLine(rawName);
        if (cleanedName.isEmpty()) {
            return new ParsedName("Pilot Visitor Row " + excelRowNumber, null);
        }

        int commaIndex = cleanedName.indexOf(',');
        if (commaIndex < 0) {
            return new ParsedName(cleanedName, null);
        }

        String fullName = cleanedName.substring(0, commaIndex).trim();
        String briefProfile = cleanedName.substring(commaIndex + 1).trim();
        if (fullName.isEmpty()) {
            fullName = cleanedName;
        }
        return new ParsedName(fullName, briefProfile.isEmpty() ? null : briefProfile);
    }

    private String firstPhoneNumber(String rawPhone) {
        String normalized = cleanSingleLine(rawPhone);
        if (normalized.isEmpty()) {
            return null;
        }

        String[] parts = normalized.split("[/;,]");
        for (String part : parts) {
            String candidate = phoneCandidate(part);
            if (candidate != null) {
                return candidate;
            }
        }
        return phoneCandidate(normalized);
    }

    private String phoneCandidate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = DIGIT_RUN.matcher(value);
        if (matcher.find()) {
            return normalizePhoneDigits(matcher.group());
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : normalizePhoneDigits(digits);
    }

    private String normalizePhoneDigits(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() > 10 && digits.startsWith("91")) {
            return digits.substring(digits.length() - 10);
        }
        if (digits.length() > 10) {
            return digits.substring(0, 10);
        }
        return digits;
    }

    private PilotImportRowResultDto failedRow(int rowNumber, String srNo, String name,
                                             String phoneNumber, String message) {
        return PilotImportRowResultDto.builder()
                .rowNumber(rowNumber)
                .srNo(srNo)
                .success(false)
                .name(name)
                .phoneNumber(phoneNumber)
                .message(message != null && !message.trim().isEmpty() ? message : "Import failed")
                .build();
    }

    private String normalizeHeader(String value) {
        return cleanSingleLine(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String cleanSingleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String cleanMultiline(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ').trim();
    }

    private record ParsedName(String fullName, String briefProfile) {
    }
}
