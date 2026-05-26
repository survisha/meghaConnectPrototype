package com.meghaconnect.automation.utils;

import com.meghaconnect.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excel utility for scenario-based test data lookup.
 */
public class ExcelUtil {
    private static final Logger logger = LogManager.getLogger(ExcelUtil.class);

    private ExcelUtil() {
    }

    public static Map<String, String> getRowData(String sheetName, String scenarioName) {
        return getRowData(ConfigManager.getTestDataExcelPath(), sheetName, scenarioName);
    }

    public static Map<String, String> getRowData(String excelPath, String sheetName, String scenarioName) {
        File file = new File(excelPath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Excel test data file not found: " + file.getAbsolutePath());
        }

        try (FileInputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet '" + sheetName + "' not found in: " + file.getAbsolutePath());
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel sheet '" + sheetName + "' does not contain a header row");
            }

            DataFormatter formatter = new DataFormatter();
            Map<Integer, String> headers = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim();
                if (!header.isEmpty()) {
                    headers.put(cell.getColumnIndex(), header);
                }
            }

            if (!headers.containsValue("scenarioName")) {
                throw new IllegalArgumentException("Excel sheet '" + sheetName + "' must contain column: scenarioName");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> data = new LinkedHashMap<>();
                for (Map.Entry<Integer, String> header : headers.entrySet()) {
                    Cell cell = row.getCell(header.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    data.put(header.getValue(), cell == null ? "" : formatter.formatCellValue(cell).trim());
                }

                if (scenarioName.equalsIgnoreCase(data.getOrDefault("scenarioName", ""))) {
                    logger.info("Loaded Excel test data for scenario: " + scenarioName);
                    return data;
                }
            }

            throw new IllegalArgumentException("Scenario '" + scenarioName + "' not found in sheet '" + sheetName + "'");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to read Excel test data from '" + excelPath + "': " + e.getMessage(), e);
        }
    }
}
