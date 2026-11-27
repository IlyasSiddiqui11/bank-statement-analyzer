package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionClassificationPipelineTest {

    private static final String[] REQUIRED_HEADERS = {
            "Date", "Check Number", "Particulars", "Debit", "Credit", "Balance"
    };
    private static final String INVALID_HEADER_MESSAGE =
            "Invalid Excel format. Please arrange your columns in this exact order: " +
                    "Date, Check Number, Particulars, Debit, Credit, Balance.";

    private final TransactionParserService parser = new TransactionParserService();

    @Test
    void classifiesPeopleMerchantsAtmAndUnknownDescriptions() {
        assertEquals("AMIT KUMAR",
                parser.extractName("UPI/P2P/10001/AMIT KUMAR/UPI/ICICI BANK"));
        assertEquals("NEHA PATEL",
                parser.extractName("upi/p2p/10002/  Neha   Patel /UPI/SBI"));
        assertEquals("NEHA PATEL", parser.extractName("UPI/P2P/10002/NEHA PATEL"));
        assertEquals("RAHUL SHARMA",
                parser.extractName("  UPI/P2P/10003/RAHUL SHARMA/UPI/HDFC BANK  "));
        assertEquals("ZEPTO MARKETPLACE PVT LTD",
                parser.extractName("UPI/P2M/10004/ZEPTO MARKETPLACE PVT LTD/UPI/ICICI BANK"));
        assertEquals("ZOMATO PRIVATE LIMITED",
                parser.extractName("UPI/P2M/10005/ZOMATO PRIVATE LIMITED/UPI/AXIS BANK"));
        assertEquals("AMAZON PAY",
                parser.extractName("UPI/P2M/10006/amazon pay/UPI/ICICI BANK"));
        assertEquals("UBER INDIA",
                parser.extractName("UPI/P2M/10007/UBER INDIA/UPI/SBI"));
        assertEquals("ATM", parser.extractName("ATM CASH WITHDRAWAL/ATM/123456"));
        assertEquals("UNKNOWN", parser.extractName("SALARY/ABC COMPANY PVT LTD"));
        assertEquals("UNKNOWN", parser.extractName("UPI/P2P//AMIT KUMAR/UPI/ICICI BANK"));
        assertEquals("UNKNOWN", parser.extractName("UPI/P2M/10008//UPI/ICICI BANK"));
        assertEquals("UNKNOWN", parser.extractName("NEFT/TRANSFER//"));
        assertEquals("UNKNOWN", parser.extractName("  "));
        assertEquals("UNKNOWN", parser.extractName(null));
    }

    @Test
    void acceptsExactHeadersBlankOptionalCellsAndLeadingBlankRows() throws Exception {
        List<Transaction> transactions = new ExcelReaderService().readExcel(
                createUpload(createStatementWorkbook(REQUIRED_HEADERS, true, false, false)));
        assertEquals(2, transactions.size());
        Transaction credit = transactions.get(0);
        assertEquals(LocalDate.of(2025, 4, 1), credit.getDate());
        assertNull(credit.getChequeNumber());
        assertEquals(465.0, credit.getCredit());
        assertNull(credit.getDebit());

        List<Transaction> debitTransactions = new ExcelReaderService().readExcel(
                createUpload(createStatementWorkbook(REQUIRED_HEADERS, false, false, false)));
        assertEquals(2, debitTransactions.size());
        Transaction debit = debitTransactions.get(1);
        assertEquals(189.45, debit.getDebit());
        assertNull(debit.getCredit());
    }

    @Test
    void acceptsTextDatesAndNumericStrings() throws Exception {
        List<Transaction> transactions = new ExcelReaderService().readExcel(
                createUpload(createStatementWorkbook(REQUIRED_HEADERS, false, true, true)));

        assertEquals(2, transactions.size());
        assertEquals(LocalDate.of(2025, 4, 1), transactions.get(0).getDate());
        assertEquals(1465.0, transactions.get(0).getCredit());
        assertEquals(189.45, transactions.get(1).getDebit());
    }

    @Test
    void rejectsWrongMissingAdditionalAndRenamedHeadersWithSpecificMessage() throws Exception {
        assertInvalidHeaders("Date", "Particulars", "Check Number", "Debit", "Credit", "Balance");
        assertInvalidHeaders("Date", "Check Number", "Particulars", "Debit", "Credit");
        assertInvalidHeaders("Date", "Check Number", "Particulars", "Debit", "Credit", "Balance", "Notes");
        assertInvalidHeaders("Tran Date", "Chq No", "Particulars", "Debit", "Credit", "Balance");
        assertInvalidHeaders("index", "Tran Date", "Chq No", "Particulars", "Debit", "Credit", "Balance");

        byte[] sourceWorkbook = createStatementWorkbook(REQUIRED_HEADERS, false, false, false);
        byte[] workbookWithExtraData;
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(sourceWorkbook));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getSheetAt(0).getRow(1).createCell(6).setCellValue("extra data");
            workbook.write(output);
            workbookWithExtraData = output.toByteArray();
        }
        assertInvalidWorkbook(workbookWithExtraData);
    }

    @Test
    void reportsTheRowAndColumnForInvalidNumericValues() throws Exception {
        byte[] validWorkbook = createStatementWorkbook(REQUIRED_HEADERS, false, false, false);
        byte[] invalidWorkbook;
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(validWorkbook));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getSheetAt(0).getRow(1).getCell(4).setCellValue("not a number");
            workbook.write(output);
            invalidWorkbook = output.toByteArray();
        }

        WorkflowException exception = assertThrows(
                WorkflowException.class,
                () -> new ExcelReaderService().readExcel(createUpload(invalidWorkbook)));
        assertEquals("Invalid Credit value at row 2. Enter a number or leave the cell blank.",
                exception.getMessage());
    }

    @Test
    void processesOneHundredRowsWithoutLosingOrDuplicatingTransactions() throws Exception {
        List<String> particulars = createHundredParticulars();
        byte[] sourceWorkbook = createSourceWorkbook(particulars);

        ExcelReaderService reader = new ExcelReaderService();
        List<Transaction> transactions = reader.readExcel(new MockMultipartFile(
                "file",
                "statement.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                sourceWorkbook));
        assertEquals(100, transactions.size());

        TransactionGroupingService grouping = new TransactionGroupingService(parser);
        Map<String, List<Transaction>> grouped = grouping.groupTransactions(transactions);
        assertEquals(15, grouped.get("AMIT KUMAR").size());
        assertEquals(15, grouped.get("NEHA PATEL").size());
        assertEquals(15, grouped.get("RAHUL SHARMA").size());
        assertEquals(10, grouped.get("ZEPTO MARKETPLACE PVT LTD").size());
        assertEquals(10, grouped.get("ZOMATO PRIVATE LIMITED").size());
        assertEquals(10, grouped.get("AMAZON PAY").size());
        assertEquals(10, grouped.get("UBER INDIA").size());
        assertEquals(10, grouped.get("ATM").size());
        assertEquals(5, grouped.get("UNKNOWN").size());

        ExcelWriterService writer = new ExcelWriterService();
        try (Workbook output = writer.createWorkbook(grouped);
             ByteArrayOutputStream outputBytes = new ByteArrayOutputStream()) {
            output.write(outputBytes);

            try (Workbook generated = new XSSFWorkbook(
                    new ByteArrayInputStream(outputBytes.toByteArray()))) {
                assertEquals(10, generated.getNumberOfSheets());
                assertTrue(generated.getSheet("Summary") != null);

                Map<String, Integer> expectedCounts = Map.of(
                        "AMIT KUMAR", 15,
                        "NEHA PATEL", 15,
                        "RAHUL SHARMA", 15,
                        "ZEPTO MARKETPLACE PVT LTD", 10,
                        "ZOMATO PRIVATE LIMITED", 10,
                        "AMAZON PAY", 10,
                        "UBER INDIA", 10,
                        "ATM", 10,
                        "UNKNOWN", 5);
                Map<String, Integer> outputParticularCounts = new HashMap<>();
                for (Map.Entry<String, Integer> expected : expectedCounts.entrySet()) {
                    Sheet sheet = generated.getSheet(expected.getKey());
                    assertTrue(sheet != null, "Missing worksheet " + expected.getKey());
                    assertEquals(expected.getValue() + 3, sheet.getPhysicalNumberOfRows());

                    for (int rowNumber = 1; rowNumber <= expected.getValue(); rowNumber++) {
                        String rowParticulars = sheet.getRow(rowNumber).getCell(2).getStringCellValue();
                        outputParticularCounts.merge(rowParticulars, 1, Integer::sum);
                    }
                }

                Map<String, Integer> sourceParticularCounts = new HashMap<>();
                for (String value : particulars) {
                    sourceParticularCounts.merge(value, 1, Integer::sum);
                }
                assertEquals(sourceParticularCounts, outputParticularCounts);

                String originalWhitespace = "UPI/P2P/10001/amit   kumar/UPI/ICICI BANK";
                Sheet amitSheet = generated.getSheet("AMIT KUMAR");
                assertTrue(containsParticulars(amitSheet, originalWhitespace));
                assertEquals("2026-01-01", amitSheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("CHQ-1", amitSheet.getRow(1).getCell(1).getStringCellValue());
                assertEquals(10.0, amitSheet.getRow(1).getCell(3).getNumericCellValue());
                assertEquals(20.0, amitSheet.getRow(1).getCell(4).getNumericCellValue());
                assertEquals(100.0, amitSheet.getRow(1).getCell(5).getNumericCellValue());
            }
        }
    }

    @Test
    void createsDistinctValidNamesForUnsafeLongOrReservedGroupNames() throws Exception {
        Map<String, List<Transaction>> groups = new HashMap<>();
        groups.put("Summary", List.of(new Transaction()));
        groups.put("A".repeat(40), List.of(new Transaction()));
        groups.put("A".repeat(39) + "B", List.of(new Transaction()));
        groups.put("ACME/FOOD", List.of(new Transaction()));

        try (Workbook workbook = new ExcelWriterService().createWorkbook(groups)) {
            Set<String> names = new HashSet<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                String name = workbook.getSheetName(index);
                assertTrue(name.length() <= 31);
                assertTrue(names.add(name.toUpperCase(java.util.Locale.ROOT)));
                assertDoesNotThrow(() -> WorkbookUtil.validateSheetName(name));
            }
            assertTrue(names.contains("SUMMARY"));
            assertTrue(names.contains("SUMMARY (2)"));
            assertEquals(5, workbook.getNumberOfSheets());
        }
    }

    private static boolean containsParticulars(Sheet sheet, String particulars) {
        for (int rowNumber = 1; rowNumber <= sheet.getLastRowNum(); rowNumber++) {
            Row row = sheet.getRow(rowNumber);
            if (row != null
                    && row.getCell(2) != null
                    && particulars.equals(row.getCell(2).getStringCellValue())) {
                return true;
            }
        }
        return false;
    }

    private static void assertInvalidHeaders(String... headers) throws Exception {
        assertInvalidWorkbook(createHeaderOnlyWorkbook(headers));
    }

    private static void assertInvalidWorkbook(byte[] workbook) throws Exception {
        WorkflowException exception = assertThrows(
                WorkflowException.class,
                () -> new ExcelReaderService().readExcel(createUpload(workbook)));
        assertEquals(INVALID_HEADER_MESSAGE, exception.getMessage());
    }

    private static MockMultipartFile createUpload(byte[] workbook) {
        return new MockMultipartFile(
                "file",
                "statement.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbook);
    }

    private static byte[] createHeaderOnlyWorkbook(String[] headers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Statement").createRow(0);
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] createStatementWorkbook(
            String[] headers,
            boolean leadingBlankRows,
            boolean textDates,
            boolean textAmounts) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Statement");
            int headerRowIndex = leadingBlankRows ? 2 : 0;
            Row header = sheet.createRow(headerRowIndex);
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd-mm-yyyy"));
            LocalDate date = LocalDate.of(2025, 4, 1);
            String[] particulars = {
                    "NEFT/TRANSFER/SHOP/UPI/BANK",
                    "UPI/P2M/123/SHOP/UPI/BANK"
            };
            String[] debits = {null, "189.45"};
            String[] credits = {textAmounts ? "1,465.00" : "465.00", null};
            String[] balances = {"31495.85", "31306.40"};

            for (int index = 0; index < particulars.length; index++) {
                Row transaction = sheet.createRow(headerRowIndex + index + 1);
                Cell dateCell = transaction.createCell(0);
                if (textDates) {
                    dateCell.setCellValue("01-04-2025");
                } else {
                    dateCell.setCellValue(java.sql.Date.valueOf(date.plusDays(index)));
                    dateCell.setCellStyle(dateStyle);
                }
                transaction.createCell(1);
                transaction.createCell(2).setCellValue(particulars[index]);
                setAmountCell(transaction, 3, debits[index], textAmounts);
                setAmountCell(transaction, 4, credits[index], textAmounts);
                setAmountCell(transaction, 5, balances[index], textAmounts);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void setAmountCell(Row row, int column, String amount, boolean textAmount) {
        if (amount == null) {
            return;
        }
        Cell cell = row.createCell(column);
        if (textAmount) {
            cell.setCellValue(amount);
        } else {
            cell.setCellValue(Double.parseDouble(amount));
        }
    }

    private static List<String> createHundredParticulars() {
        List<String> values = new ArrayList<>();
        String[] people = {"AMIT KUMAR", "NEHA PATEL", "RAHUL SHARMA"};
        int transactionId = 10001;
        for (String person : people) {
            for (int index = 0; index < 15; index++) {
                String formattedName = index == 0
                        ? person.toLowerCase(java.util.Locale.ROOT).replace(" ", "   ")
                        : person;
                String bank = index % 2 == 0 ? "ICICI BANK" : "HDFC BANK";
                values.add("UPI/P2P/" + transactionId++ + "/" + formattedName + "/UPI/" + bank);
            }
        }

        String[] merchants = {
                "ZEPTO MARKETPLACE PVT LTD",
                "ZOMATO PRIVATE LIMITED",
                "AMAZON PAY",
                "UBER INDIA"
        };
        for (String merchant : merchants) {
            for (int index = 0; index < 10; index++) {
                values.add("UPI/P2M/" + transactionId++ + "/" + merchant + "/UPI/AXIS BANK");
            }
        }
        for (int index = 0; index < 10; index++) {
            values.add("ATM CASH WITHDRAWAL/ATM/" + transactionId++);
        }
        values.add("SALARY/ABC COMPANY PVT LTD");
        values.add("SALARY/OTHER COMPANY");
        values.add("UPI/P2P//AMIT KUMAR/UPI/ICICI BANK");
        values.add("UPI/P2M/10008//UPI/ICICI BANK");
        values.add("");

        assertEquals(100, values.size());
        return values;
    }

    private static byte[] createSourceWorkbook(List<String> particulars) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Statement");
            Row header = sheet.createRow(0);
            String[] headers = REQUIRED_HEADERS;
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            LocalDate date = LocalDate.of(2026, 1, 1);
            for (int index = 0; index < particulars.size(); index++) {
                Row row = sheet.createRow(index + 1);
                var dateCell = row.createCell(0);
                dateCell.setCellValue(java.sql.Date.valueOf(date.plusDays(index)));
                dateCell.setCellStyle(dateStyle);
                row.createCell(1).setCellValue("CHQ-" + (index + 1));
                row.createCell(2).setCellValue(particulars.get(index));
                row.createCell(3).setCellValue(10.0);
                row.createCell(4).setCellValue(20.0);
                row.createCell(5).setCellValue(100.0);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
