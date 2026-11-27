package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.service.PaymentSessionService;
import com.example.bank_statement_analyzer.service.WorkflowException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.HttpStatus.GONE;

@RestController
@RequestMapping("/api/excel")
public class ExcelReaderController {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final String WORKBOOK_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PaymentSessionService paymentSessionService;

    public ExcelReaderController(PaymentSessionService paymentSessionService) {
        this.paymentSessionService = paymentSessionService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaymentSessionService.AnalysisResponse analyze(
            @RequestPart("file") MultipartFile file) throws IOException {
        validateFile(file);
        try {
            return paymentSessionService.analyze(file);
        } catch (IOException exception) {
            throw new WorkflowException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Could not read the workbook. Confirm it is a valid, unencrypted .xlsx file.");
        }
    }

    @PostMapping(value = "/download", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> download(@RequestBody DownloadRequest request) throws IOException {
        byte[] workbook = paymentSessionService.download(request.sessionToken());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Organized_Statement.xlsx\"")
                .header(HttpHeaders.CONTENT_TYPE, WORKBOOK_CONTENT_TYPE)
                .header("X-Content-Type-Options", "nosniff")
                .body(workbook);
    }

    @PostMapping(value = "/read", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void rejectLegacyFreeDownload(@RequestPart("file") MultipartFile ignored) {
        throw new WorkflowException(
                GONE,
                "Direct workbook downloads are disabled. Analyze the statement and complete payment first.");
    }

    @PostMapping(value = "/quote", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void rejectLegacyQuote(@RequestPart("file") MultipartFile ignored) {
        throw new WorkflowException(GONE, "Use /api/excel/analyze to start a payment session.");
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new WorkflowException(org.springframework.http.HttpStatus.BAD_REQUEST, "Select a non-empty .xlsx file.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new WorkflowException(org.springframework.http.HttpStatus.BAD_REQUEST, "Only .xlsx files are supported.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new WorkflowException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "The file must be 10 MB or smaller.");
        }
    }

    public record DownloadRequest(String sessionToken) {}
}
