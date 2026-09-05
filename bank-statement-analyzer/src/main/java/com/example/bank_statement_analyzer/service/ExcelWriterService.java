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

        // Create bold style
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(boldFont);

        // Create Summary sheet
        Sheet summarySheet = workbook.createSheet("Summary");

        // Summary title
        Row titleRow = summarySheet.createRow(0);

        titleRow.createCell(0)
                .setCellValue("BANK STATEMENT SUMMARY");

        titleRow.getCell(0)
                .setCellStyle(totalStyle);

        // Summary headers
        Row summaryHeaderRow = summarySheet.createRow(2);

        summaryHeaderRow.createCell(0)
                .setCellValue("PARTICULAR");

        summaryHeaderRow.createCell(1)
                .setCellValue("AMOUNT");

        summaryHeaderRow.createCell(3)
                .setCellValue("PARTICULAR");

        summaryHeaderRow.createCell(4)
                .setCellValue("AMOUNT");

        // Make summary headers bold
        for (int i : new int[]{0, 1, 3, 4}) {
            summaryHeaderRow
                    .getCell(i)
                    .setCellStyle(totalStyle);
        }

        // Create To and By labels
        Row sideRow = summarySheet.createRow(3);

        sideRow.createCell(0)
                .setCellValue("To");

        sideRow.createCell(3)
                .setCellValue("By");

        sideRow.getCell(0)
                .setCellStyle(totalStyle);

        sideRow.getCell(3)
                .setCellStyle(totalStyle);

        // Row numbers for Summary
        int toRowNumber = 4;
        int byRowNumber = 4;

        // Create one sheet for each person, merchant or category
        for (Map.Entry<String, List<Transaction>> entry
                : groupedTransactions.entrySet()) {

            String groupName = entry.getKey();

            List<Transaction> transactions = entry.getValue();

            Sheet sheet = workbook.createSheet(groupName);

            // Header row
            Row headerRow = sheet.createRow(0);

            headerRow.createCell(0)
                    .setCellValue("Tran Date");

            headerRow.createCell(1)
                    .setCellValue("Chq No");

            headerRow.createCell(2)
                    .setCellValue("Particulars");

            headerRow.createCell(3)
                    .setCellValue("Debit");

            headerRow.createCell(4)
                    .setCellValue("Credit");

            headerRow.createCell(5)
                    .setCellValue("Balance");

            // Make headers bold
            for (int i = 0; i < 6; i++) {
                headerRow
                        .getCell(i)
                        .setCellStyle(totalStyle);
            }

            // Add transactions
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

                // Cheque number
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
                            .setCellValue(
                                    transaction.getDebit()
                            );
                }

                // Credit
                if (transaction.getCredit() != null) {
                    row.createCell(4)
                            .setCellValue(
                                    transaction.getCredit()
                            );
                }

                // Balance
                if (transaction.getBalance() != null) {
                    row.createCell(5)
                            .setCellValue(
                                    transaction.getBalance()
                            );
                }
            }

            // Calculate totals
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

            // Add credit to To side of Summary
            if (totalCredit > 0) {

                Row summaryRow =
                        summarySheet.createRow(toRowNumber++);

                summaryRow.createCell(0)
                        .setCellValue(groupName);

                summaryRow.createCell(1)
                        .setCellValue(totalCredit);
            }

            // Add debit to By side of Summary
            if (totalDebit > 0) {

                Row summaryRow =
                        summarySheet.createRow(byRowNumber++);

                summaryRow.createCell(3)
                        .setCellValue(groupName);

                summaryRow.createCell(4)
                        .setCellValue(totalDebit);
            }

            // Total debit on group sheet
            Row debitTotalRow =
                    sheet.createRow(rowNumber + 1);

            debitTotalRow.createCell(2)
                    .setCellValue("Total Debit");

            debitTotalRow.createCell(3)
                    .setCellValue(totalDebit);

            debitTotalRow.getCell(2)
                    .setCellStyle(totalStyle);

            debitTotalRow.getCell(3)
                    .setCellStyle(totalStyle);

            // Total credit on group sheet
            Row creditTotalRow =
                    sheet.createRow(rowNumber + 2);

            creditTotalRow.createCell(2)
                    .setCellValue("Total Credit");

            creditTotalRow.createCell(4)
                    .setCellValue(totalCredit);

            creditTotalRow.getCell(2)
                    .setCellStyle(totalStyle);

            creditTotalRow.getCell(4)
                    .setCellStyle(totalStyle);

            // Auto size group sheet columns
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        // Auto size Summary sheet columns
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        summarySheet.autoSizeColumn(3);
        summarySheet.autoSizeColumn(4);

        return workbook;
    }
}