package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.service.WorkflowException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<Map<String, String>> handleWorkflowException(WorkflowException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput() {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Could not read the workbook. Confirm it is a valid, unencrypted .xlsx file."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "The file must be 10 MB or smaller."));
    }

}
