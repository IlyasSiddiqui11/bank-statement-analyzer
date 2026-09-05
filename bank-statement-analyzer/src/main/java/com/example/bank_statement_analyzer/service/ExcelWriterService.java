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

        // CREATE BOLD STYLE
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(boldFont);


        // SUMMARY SHEET
        Sheet summarySheet = workbook.createSheet("Summary");

        // TITLE
        Row titleRow = summarySheet.createRow(0);

        titleRow.createCell(0)
                .setCellValue("BANK STATEMENT SUMMARY");

        titleRow.getCell(0)
                .setCellStyle(totalStyle);


        // SUMMARY HEADERS
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


        // TO / BY
        Row sideRow = summarySheet.createRow(3);

        sideRow.createCell(0)
                .setCellValue("To");

        sideRow.createCell(3)
                .setCellValue("By");

        sideRow.getCell(0)
                .setCellStyle(totalStyle);

        sideRow.getCell(3)
                .setCellStyle(totalStyle);


        // CREATE SHEET FOR EACH GROUP
        for (Map.Entry<String, List<Transaction>> entry
                : groupedTransactions.entrySet()) {

            String groupName = entry.getKey();

            List<Transaction> transactions =
                    entry.getValue();


            // CREATE SHEET
            Sheet sheet =
                    workbook.createSheet(groupName);

            // HEADER ROW
            Row headerRow =
                    sheet.createRow(0);

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


            // Make header bold
            for (int i = 0; i < 6; i++) {

                headerRow
                        .getCell(i)
                        .setCellStyle(totalStyle);
            }

            // TRANSACTIONS
            int rowNumber = 1;

            for (Transaction transaction : transactions) {

                Row row =
                        sheet.createRow(rowNumber++);


                // DATE
                if (transaction.getDate() != null) {

                    row.createCell(0)
                            .setCellValue(
                                    transaction
                                            .getDate()
                                            .toString()
                            );
                }


                // CHEQUE NUMBER
                row.createCell(1)
                        .setCellValue(
                                transaction.getChequeNumber() != null
                                        ? transaction.getChequeNumber()
                                        : ""
                        );


                // PARTICULARS
                row.createCell(2)
                        .setCellValue(
                                transaction.getParticulars() != null
                                        ? transaction.getParticulars()
                                        : ""
                        );


                // DEBIT
                if (transaction.getDebit() != null) {

                    row.createCell(3)
                            .setCellValue(
                                    transaction.getDebit()
                            );
                }


                // CREDIT
                if (transaction.getCredit() != null) {

                    row.createCell(4)
                            .setCellValue(
                                    transaction.getCredit()
                            );
                }


                // BALANCE
                if (transaction.getBalance() != null) {

                    row.createCell(5)
                            .setCellValue(
                                    transaction.getBalance()
                            );
                }
            }


            // CALCULATE TOTALS
            double totalDebit = 0;

            double totalCredit = 0;


            for (Transaction transaction : transactions) {

                if (transaction.getDebit() != null) {

                    totalDebit +=
                            transaction.getDebit();
                }

                if (transaction.getCredit() != null) {

                    totalCredit +=
                            transaction.getCredit();
                }
            }

            // TOTAL DEBIT
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


            // TOTAL CREDIT
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

            // AUTO SIZE COLUMNS
            for (int i = 0; i < 6; i++) {

                sheet.autoSizeColumn(i);
            }
        }


        // AUTO SIZE SUMMARY COLUMNS
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        summarySheet.autoSizeColumn(3);
        summarySheet.autoSizeColumn(4);

        // RETURN WORKBOOK
        return workbook;
    }
}