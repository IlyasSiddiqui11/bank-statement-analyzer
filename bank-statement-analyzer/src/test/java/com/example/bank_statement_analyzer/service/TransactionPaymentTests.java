package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.UploadSession;
import com.example.bank_statement_analyzer.repository.PaymentRepository;
import com.example.bank_statement_analyzer.repository.UploadSessionRepository;
import com.example.bank_statement_analyzer.repository.DownloadRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionPaymentTests {

    private final TransactionPricingService pricing = new TransactionPricingService();

    @Test
    void standardPricingExamplesUsePaise() {
        assertEquals(250, pricing.calculateAmountInPaise(10));
        assertEquals(2500, pricing.calculateAmountInPaise(100));
        assertEquals(23000, pricing.calculateAmountInPaise(920));
        assertEquals(125000, pricing.calculateAmountInPaise(5000));
    }

    @Test
    void rejectsInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> pricing.calculateAmountInPaise(0));
        assertThrows(IllegalArgumentException.class, () -> pricing.calculateAmountInPaise(-1));
    }

    @Test
    void rejectsMissingSession() {
        ExcelReaderService reader = mock(ExcelReaderService.class);
        TransactionGroupingService grouping = mock(TransactionGroupingService.class);
        ExcelWriterService writer = mock(ExcelWriterService.class);
        RazorpayService razorpay = mock(RazorpayService.class);
        UploadSessionRepository sessions = mock(UploadSessionRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        DownloadRecordRepository downloads = mock(DownloadRecordRepository.class);
        when(sessions.findByPublicTokenHash(anyString())).thenReturn(Optional.empty());

        PaymentSessionService service = new PaymentSessionService(
                reader, grouping, writer, pricing, razorpay, sessions, payments, downloads, "30");

        WorkflowException exception = assertThrows(
                WorkflowException.class,
                () -> service.createOrder("missing-session"));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(razorpay);
    }

    @Test
    void rejectsExpiredPersistentSession() {
        UploadSession session = new UploadSession();
        session.setStatus("ANALYZED");
        session.setExpiresAt(java.time.Instant.now().minusSeconds(1));

        UploadSessionRepository sessions = mock(UploadSessionRepository.class);
        when(sessions.findByPublicTokenHash(anyString())).thenReturn(Optional.of(session));

        PaymentSessionService service = new PaymentSessionService(
                mock(ExcelReaderService.class),
                mock(TransactionGroupingService.class),
                mock(ExcelWriterService.class),
                pricing,
                mock(RazorpayService.class),
                sessions,
                mock(PaymentRepository.class),
                mock(DownloadRecordRepository.class),
                "30");

        WorkflowException exception = assertThrows(
                WorkflowException.class,
                () -> service.createOrder("expired-session"));
        assertEquals(org.springframework.http.HttpStatus.GONE, exception.getStatus());
    }
}
