package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ExcelWriterService {

    public Workbook createWorkbook(
            Map<String, List<Transaction>> groupedTransactions) {

        Workbook workbook = new XSSFWorkbook();

        // Create bold font
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        // Create style for total rows
        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(boldFont);

        // Create one sheet for each person / merchant / category
        for (Map.Entry<String, List<Transaction>> entry
                : groupedTransactions.entrySet()) {

            String groupName = entry.getKey();
            List<Transaction> transactions = entry.getValue();

            Sheet sheet = workbook.createSheet(groupName);

            // HEADER ROW
            Row headerRow = sheet.createRow(0);

            headerRow.createCell(0).setCellValue("Tran Date");
            headerRow.createCell(1).setCellValue("Chq No");
            headerRow.createCell(2).setCellValue("Particulars");
            headerRow.createCell(3).setCellValue("Debit");
            headerRow.createCell(4).setCellValue("Credit");
            headerRow.createCell(5).setCellValue("Balance");

            // Make header bold
            for (int i = 0; i < 6; i++) {
                headerRow.getCell(i).setCellStyle(totalStyle);
            }

            // TRANSACTIONS
            int rowNumber = 1;

            for (Transaction transaction : transactions) {

                Row row = sheet.createRow(rowNumber++);

                // Date
                if (transaction.getDate() != null) {
                    row.createCell(0)
                            .setCellValue(
                                    transaction.getDate().toString()
                            );
                }

                // Cheque Number
                row.createCell(1)
                        .setCellValue(
                                transaction.getChequeNumber() != null
                                        ? transaction.getChequeNumber()
                                        : ""
                        );

                // Particulars
                row.createCell(2)
                        .setCellValue(
                                transaction.getParticulars() != null
                                        ? transaction.getParticulars()
                                        : ""
                        );

                // Debit
                if (transaction.getDebit() != null) {
                    row.createCell(3)
                            .setCellValue(transaction.getDebit());
                }

                // Credit
                if (transaction.getCredit() != null) {
                    row.createCell(4)
                            .setCellValue(transaction.getCredit());
                }

                // Balance
                if (transaction.getBalance() != null) {
                    row.createCell(5)
                            .setCellValue(transaction.getBalance());
                }
            }


            // CALCULATE TOTALS

            double totalDebit = 0;
            double totalCredit = 0;

            for (Transaction transaction : transactions) {

                if (transaction.getDebit() != null) {
                    totalDebit += transaction.getDebit();
                }

                if (transaction.getCredit() != null) {
                    totalCredit += transaction.getCredit();
                }
            }

            // TOTAL DEBIT
            Row debitTotalRow = sheet.createRow(rowNumber + 1);

            debitTotalRow.createCell(2)
                    .setCellValue("Total Debit");

            debitTotalRow.createCell(3)
                    .setCellValue(totalDebit);

            debitTotalRow.getCell(2)
                    .setCellStyle(totalStyle);

            debitTotalRow.getCell(3)
                    .setCellStyle(totalStyle);

            // TOTAL CREDIT
            Row creditTotalRow = sheet.createRow(rowNumber + 2);

            creditTotalRow.createCell(2)
                    .setCellValue("Total Credit");

            creditTotalRow.createCell(4)
                    .setCellValue(totalCredit);

            creditTotalRow.getCell(2)
                    .setCellStyle(totalStyle);

            creditTotalRow.getCell(4)
                    .setCellStyle(totalStyle);

            // AUTO SIZE COLUMNS
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        return workbook;
    }
}