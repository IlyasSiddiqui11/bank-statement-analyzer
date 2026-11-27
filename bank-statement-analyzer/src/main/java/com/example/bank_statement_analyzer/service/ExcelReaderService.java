package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExcelReaderService {

    public List<Transaction> readExcel(MultipartFile file) throws IOException {

        List<Transaction> transactions = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Invalid Excel format: The workbook has no sheets.");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("Invalid Excel format: Header row is missing.");
            }

            String[] expectedHeaders = {
                    "Tran Date",
                    "Chq No",
                    "Particulars",
                    "Debit",
                    "Credit",
                    "Balance"
            };

            Map<String, Integer> headerColumns = new HashMap<>();
            for (int column = headerRow.getFirstCellNum(); column < headerRow.getLastCellNum(); column++) {
                String header = getStringValue(headerRow.getCell(column));
                if (header != null && !header.isEmpty()) {
                    headerColumns.putIfAbsent(header.toLowerCase(Locale.ROOT), column);
                }
            }

            for (String expectedHeader : expectedHeaders) {
                if (!headerColumns.containsKey(expectedHeader.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException(
                            "Invalid Excel format: Required column '" + expectedHeader + "' is missing.");
                }
            }

            int[] requiredColumns = new int[expectedHeaders.length];
            int previousColumn = -1;
            for (int i = 0; i < expectedHeaders.length; i++) {
                requiredColumns[i] = headerColumns.get(expectedHeaders[i].toLowerCase(Locale.ROOT));
                if (requiredColumns[i] <= previousColumn) {
                    throw new IllegalArgumentException(
                            "Invalid Excel format: Required columns must appear in this order: " +
                                    String.join(", ", expectedHeaders) + ".");
                }
                previousColumn = requiredColumns[i];
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Transaction transaction = new Transaction();
                transaction.setDate(getDate(row.getCell(requiredColumns[0])));
                transaction.setChequeNumber(getStringValue(row.getCell(requiredColumns[1])));
                transaction.setParticulars(getOriginalStringValue(row.getCell(requiredColumns[2])));
                transaction.setDebit(getDoubleValue(row.getCell(requiredColumns[3])));
                transaction.setCredit(getDoubleValue(row.getCell(requiredColumns[4])));
                transaction.setBalance(getDoubleValue(row.getCell(requiredColumns[5])));

                if (transaction.getDebit() != null || transaction.getCredit() != null) {
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }

    private String getStringValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        return cell.toString()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String getOriginalStringValue(Cell cell) {
        return cell == null ? null : cell.toString();
    }

    private Double getDoubleValue(Cell cell) {

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        String value = cell.toString().trim();

        if (value.isEmpty()) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        return Double.parseDouble(value);
    }

    private LocalDate getDate(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(cell)) {

            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        return null;
    }
}