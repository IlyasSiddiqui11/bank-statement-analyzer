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
            List<Transaction> transactions,
            Map<String, List<Transaction>> groupedTransactions) {

        Workbook workbook = new XSSFWorkbook();

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(boldFont);

        Sheet summarySheet = workbook.createSheet("Summary");

        Row titleRow = summarySheet.createRow(0);

        titleRow.createCell(0)
                .setCellValue("BANK STATEMENT SUMMARY");

        titleRow.getCell(0)
                .setCellStyle(totalStyle);

        Row summaryHeaderRow = summarySheet.createRow(2);

        summaryHeaderRow.createCell(0)
                .setCellValue("PARTICULAR");

        summaryHeaderRow.createCell(1)
                .setCellValue("AMOUNT");

        summaryHeaderRow.createCell(3)
                .setCellValue("PARTICULAR");

        summaryHeaderRow.createCell(4)
                .setCellValue("AMOUNT");

        for (int i : new int[]{0, 1, 3, 4}) {
            summaryHeaderRow
                    .getCell(i)
                    .setCellStyle(totalStyle);
        }

        Row sideRow = summarySheet.createRow(3);

        sideRow.createCell(0)
                .setCellValue("To");

        sideRow.createCell(3)
                .setCellValue("By");

        sideRow.getCell(0)
                .setCellStyle(totalStyle);

        sideRow.getCell(3)
                .setCellStyle(totalStyle);

        int toRowNumber = 4;
        int byRowNumber = 4;

        // Calculate opening balance

        double openingBalance = 0;

        if (!transactions.isEmpty()) {

            Transaction firstTransaction = transactions.get(0);

            double firstBalance =
                    firstTransaction.getBalance() != null
                            ? firstTransaction.getBalance()
                            : 0;

            double firstCredit =
                    firstTransaction.getCredit() != null
                            ? firstTransaction.getCredit()
                            : 0;

            double firstDebit =
                    firstTransaction.getDebit() != null
                            ? firstTransaction.getDebit()
                            : 0;

            openingBalance =
                    firstBalance
                            - firstCredit
                            + firstDebit;
        }

        // Add opening balance to To side

        if (openingBalance != 0) {

            Row openingBalanceRow =
                    summarySheet.createRow(toRowNumber++);

            openingBalanceRow.createCell(0)
                    .setCellValue("Opening Balance");

            openingBalanceRow.createCell(1)
                    .setCellValue(openingBalance);
        }

        // Create one sheet for each person, merchant or category

        for (Map.Entry<String, List<Transaction>> entry
                : groupedTransactions.entrySet()) {

            String groupName = entry.getKey();

            List<Transaction> groupTransactions =
                    entry.getValue();

            Sheet sheet =
                    workbook.createSheet(groupName);

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

            for (int i = 0; i < 6; i++) {
                headerRow
                        .getCell(i)
                        .setCellStyle(totalStyle);
            }

            int rowNumber = 1;

            for (Transaction transaction : groupTransactions) {

                Row row = sheet.createRow(rowNumber++);

                if (transaction.getDate() != null) {
                    row.createCell(0)
                            .setCellValue(
                                    transaction.getDate().toString()
                            );
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

            double totalDebit = 0;
            double totalCredit = 0;

            for (Transaction transaction : groupTransactions) {

                if (transaction.getDebit() != null) {
                    totalDebit += transaction.getDebit();
                }

                if (transaction.getCredit() != null) {
                    totalCredit += transaction.getCredit();
                }
            }

            if (totalCredit > 0) {

                Row summaryRow =
                        summarySheet.createRow(toRowNumber++);

                summaryRow.createCell(0)
                        .setCellValue(groupName);

                summaryRow.createCell(1)
                        .setCellValue(totalCredit);
            }

            if (totalDebit > 0) {

                Row summaryRow =
                        summarySheet.createRow(byRowNumber++);

                summaryRow.createCell(3)
                        .setCellValue(groupName);

                summaryRow.createCell(4)
                        .setCellValue(totalDebit);
            }

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

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        summarySheet.autoSizeColumn(3);
        summarySheet.autoSizeColumn(4);

        return workbook;
    }
}