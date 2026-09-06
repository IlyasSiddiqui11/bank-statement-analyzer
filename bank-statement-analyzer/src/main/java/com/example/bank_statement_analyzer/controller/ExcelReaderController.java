package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.model.Transaction;
import com.example.bank_statement_analyzer.service.ExcelReaderService;
import com.example.bank_statement_analyzer.service.ExcelWriterService;
import com.example.bank_statement_analyzer.service.TransactionGroupingService;
import com.example.bank_statement_analyzer.service.TransactionParserService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/excel")
public class ExcelReaderController {

    private final ExcelWriterService excelWriter;
    private final ExcelReaderService excelReader;
    private final TransactionParserService transactionParser;
    private final TransactionGroupingService transactionGrouping;

    public ExcelReaderController(
            ExcelReaderService excelReader,
            TransactionParserService transactionParser,
            TransactionGroupingService transactionGrouping,
            ExcelWriterService excelWriter) {

        this.excelReader = excelReader;
        this.transactionParser = transactionParser;
        this.transactionGrouping = transactionGrouping;
        this.excelWriter = excelWriter;
    }

    @PostMapping(
            value = "/read",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<byte[]> readExcel(
            @RequestPart("file") MultipartFile file) throws IOException {

        List<Transaction> transactions =
                excelReader.readExcel(file);

        Map<String, List<Transaction>> groupedTransactions =
                transactionGrouping.groupTransactions(transactions);

        Workbook workbook =
                excelWriter.createWorkbook(
                        groupedTransactions
                );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        workbook.write(outputStream);
        workbook.close();

        byte[] excelFile = outputStream.toByteArray();

        for (Map.Entry<String, List<Transaction>> entry :
                groupedTransactions.entrySet()) {

            System.out.println("\n===== " + entry.getKey() + " =====");

            for (Transaction transaction : entry.getValue()) {
                System.out.println(transaction);
            }
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Organized_Statement.xlsx"
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
                .body(excelFile);
    }
}