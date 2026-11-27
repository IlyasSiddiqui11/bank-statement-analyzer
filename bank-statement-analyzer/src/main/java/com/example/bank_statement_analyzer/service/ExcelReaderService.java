package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelReaderService {

    private static final String[] REQUIRED_HEADERS = {
            "Date",
            "Cheque Number",
            "Particulars",
            "Debit",
            "Credit",
            "Balance"
    };

    private static final String INVALID_HEADER_MESSAGE =
            "Invalid Excel format. Please arrange your columns in this exact order: " +
                    "Date, Cheque Number, Particulars, Debit, Credit, Balance.";

    private static final List<DateTimeFormatter> TEXT_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d-M-uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/M/d", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
    );

    public List<Transaction> readExcel(MultipartFile file) throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new WorkflowException(HttpStatus.BAD_REQUEST, INVALID_HEADER_MESSAGE);
            }

            Sheet sheet = workbook.getSheetAt(0);
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex < 0 || !hasRequiredHeaders(sheet.getRow(headerRowIndex))) {
                throw new WorkflowException(HttpStatus.BAD_REQUEST, INVALID_HEADER_MESSAGE);
            }

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                if (hasDataBeyondRequiredColumns(row)) {
                    throw new WorkflowException(HttpStatus.BAD_REQUEST, INVALID_HEADER_MESSAGE);
                }

                Transaction transaction = new Transaction();
                transaction.setDate(getDate(row.getCell(0), rowIndex + 1));
                transaction.setChequeNumber(getStringValue(row.getCell(1)));
                transaction.setParticulars(getOriginalStringValue(row.getCell(2)));
                transaction.setDebit(getDoubleValue(row.getCell(3), "Debit", rowIndex + 1));
                transaction.setCredit(getDoubleValue(row.getCell(4), "Credit", rowIndex + 1));
                transaction.setBalance(getDoubleValue(row.getCell(5), "Balance", rowIndex + 1));

                if (transaction.getDebit() != null || transaction.getCredit() != null) {
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }

    private int findHeaderRow(Sheet sheet) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                if (!isBlank(cell)) {
                    return rowIndex;
                }
            }
        }
        return -1;
    }

    private boolean hasRequiredHeaders(Row row) {
        for (int column = 0; column < REQUIRED_HEADERS.length; column++) {
            String header = getStringValue(row.getCell(column));
            if (!REQUIRED_HEADERS[column].equalsIgnoreCase(header)) {
                return false;
            }
        }

        for (int column = REQUIRED_HEADERS.length; column < row.getLastCellNum(); column++) {
            if (!isBlank(row.getCell(column))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasDataBeyondRequiredColumns(Row row) {
        for (int column = REQUIRED_HEADERS.length; column < row.getLastCellNum(); column++) {
            if (!isBlank(row.getCell(column))) {
                return true;
            }
        }
        return false;
    }

    private String getStringValue(Cell cell) {
        if (cell == null || isBlank(cell)) {
            return null;
        }
        return cell.toString().replaceAll("\\s+", " ").trim();
    }

    private String getOriginalStringValue(Cell cell) {
        return cell == null || isBlank(cell) ? null : cell.toString();
    }

    private Double getDoubleValue(Cell cell, String columnName, int rowNumber) {
        if (cell == null || isBlank(cell)) {
            return null;
        }

        CellType cellType = getEffectiveCellType(cell);
        if (cellType == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (Double.isFinite(value)) {
                return value;
            }
            throw invalidNumericValue(columnName, rowNumber);
        }

        if (cellType != CellType.STRING) {
            throw invalidNumericValue(columnName, rowNumber);
        }

        String value = cell.getStringCellValue().trim();
        if (value.isEmpty() || (value.equals("-") &&
                ("Debit".equals(columnName) || "Credit".equals(columnName)))) {
            return null;
        }

        try {
            double parsedValue = Double.parseDouble(value.replace(",", ""));
            if (Double.isFinite(parsedValue)) {
                return parsedValue;
            }
        } catch (NumberFormatException ignored) {
            // Replace the parser's raw value error with a safe row and column hint.
        }
        throw invalidNumericValue(columnName, rowNumber);
    }

    private LocalDate getDate(Cell cell, int rowNumber) {
        if (cell == null || isBlank(cell)) {
            return null;
        }

        CellType cellType = getEffectiveCellType(cell);
        if (cellType == CellType.NUMERIC) {
            double serialDate = cell.getNumericCellValue();
            if (DateUtil.isValidExcelDate(serialDate)) {
                return DateUtil.getLocalDateTime(serialDate).toLocalDate();
            }
            throw invalidDateValue(rowNumber);
        }

        if (cellType == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            for (DateTimeFormatter format : TEXT_DATE_FORMATS) {
                try {
                    return LocalDate.parse(value, format);
                } catch (DateTimeParseException ignored) {
                    // Try the next supported text date format.
                }
            }
        }
        throw invalidDateValue(rowNumber);
    }

    private boolean isBlank(Cell cell) {
        if (cell == null) {
            return true;
        }
        CellType cellType = getEffectiveCellType(cell);
        return cellType == CellType.BLANK
                || (cellType == CellType.STRING && cell.getStringCellValue().isBlank());
    }

    private CellType getEffectiveCellType(Cell cell) {
        return cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
    }

    private WorkflowException invalidNumericValue(String columnName, int rowNumber) {
        return new WorkflowException(
                HttpStatus.BAD_REQUEST,
                "Invalid " + columnName + " value at row " + rowNumber +
                        ". Enter a number or leave the cell blank.");
    }

    private WorkflowException invalidDateValue(int rowNumber) {
        return new WorkflowException(
                HttpStatus.BAD_REQUEST,
                "Invalid Date value at row " + rowNumber +
                        ". Enter a valid Excel date or a date formatted as DD-MM-YYYY.");
    }
}
