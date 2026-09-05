package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ExcelWriterService {

    public Workbook createWorkbook(
            Map<String, List<Transaction>> groupedTransactions) {

        Workbook workbook = new XSSFWorkbook();

        for (Map.Entry<String, List<Transaction>> entry
                : groupedTransactions.entrySet()) {

            String groupName = entry.getKey();
            List<Transaction> transactions = entry.getValue();

            Sheet sheet = workbook.createSheet(groupName);

            // Header row
            Row headerRow = sheet.createRow(0);

            headerRow.createCell(0).setCellValue("Tran Date");
            headerRow.createCell(1).setCellValue("Chq No");
            headerRow.createCell(2).setCellValue("Particulars");
            headerRow.createCell(3).setCellValue("Debit");
            headerRow.createCell(4).setCellValue("Credit");
            headerRow.createCell(5).setCellValue("Balance");

            // Transaction rows
            int rowNumber = 1;

            for (Transaction transaction : transactions) {

                Row row = sheet.createRow(rowNumber++);

                if (transaction.getDate() != null) {
                    row.createCell(0)
                            .setCellValue(transaction.getDate().toString());
                }

                row.createCell(1)
                        .setCellValue(
                                transaction.getChequeNumber() != null
                                        ? transaction.getChequeNumber()
                                        : ""
                        );

                row.createCell(2)
                        .setCellValue(
                                transaction.getParticulars() != null
                                        ? transaction.getParticulars()
                                        : ""
                        );

                if (transaction.getDebit() != null) {
                    row.createCell(3)
                            .setCellValue(transaction.getDebit());
                }

                if (transaction.getCredit() != null) {
                    row.createCell(4)
                            .setCellValue(transaction.getCredit());
                }

                if (transaction.getBalance() != null) {
                    row.createCell(5)
                            .setCellValue(transaction.getBalance());
                }
            }

            // Automatically adjust column widths
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        return workbook;
    }
}