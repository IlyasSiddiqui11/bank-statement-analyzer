package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.service.WorkflowException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    @Test
    void returnsSpecificWorkflowValidationMessageInBadRequestResponse() {
        String message =
                "Invalid Excel format. Please arrange your six columns in this exact order: " +
                        "Date, Cheque Number, Particulars, Debit, Credit, Balance.";

        ResponseEntity<Map<String, String>> response =
                new ApiExceptionHandler().handleWorkflowException(
                        new WorkflowException(HttpStatus.BAD_REQUEST, message));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(message, response.getBody().get("error"));
    }
}
