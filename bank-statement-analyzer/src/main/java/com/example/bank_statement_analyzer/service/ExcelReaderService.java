package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelReaderService {

    public List<Transaction> readExcel(MultipartFile file) throws IOException {

        List<Transaction> transactions = new ArrayList<>();

        InputStream inputStream = file.getInputStream();

        Workbook workbook = WorkbookFactory.create(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        // Skip header row
        for (int i = 2; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            Transaction transaction = new Transaction();

            transaction.setDate(getDate(row.getCell(0)));
            transaction.setChequeNumber(getStringValue(row.getCell(1)));
            transaction.setParticulars(getStringValue(row.getCell(2)));
            transaction.setDebit(getDoubleValue(row.getCell(3)));
            transaction.setCredit(getDoubleValue(row.getCell(4)));
            transaction.setBalance(getDoubleValue(row.getCell(5)));

            transactions.add(transaction);
        }

        workbook.close();

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