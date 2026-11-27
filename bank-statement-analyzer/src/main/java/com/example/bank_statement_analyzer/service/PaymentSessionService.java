package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.*;
import com.example.bank_statement_analyzer.repository.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@Service
public class PaymentSessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExcelReaderService excelReader;
    private final TransactionGroupingService transactionGrouping;
    private final ExcelWriterService excelWriter;
    private final TransactionPricingService pricing;
    private final CashfreeService cashfreeService;
    private final UploadSessionRepository sessions;
    private final PaymentRepository payments;
    private final DownloadRecordRepository downloads;
    private final int ttlMinutes;
    private final long minimumOrderPaise;

    public PaymentSessionService(
            ExcelReaderService excelReader,
            TransactionGroupingService transactionGrouping,
            ExcelWriterService excelWriter,
            TransactionPricingService pricing,
            CashfreeService cashfreeService,
            UploadSessionRepository sessions,
            PaymentRepository payments,
            DownloadRecordRepository downloads,
            @Value("${app.session.ttl-minutes:30}") String ttlMinutesValue,
            @Value("${cashfree.minimum-order-paise:100}") String minimumOrderPaiseValue) {
        this.excelReader = excelReader;
        this.transactionGrouping = transactionGrouping;
        this.excelWriter = excelWriter;
        this.pricing = pricing;
        this.cashfreeService = cashfreeService;
        this.sessions = sessions;
        this.payments = payments;
        this.downloads = downloads;
        int parsedTtl = 30;
        if (ttlMinutesValue != null && !ttlMinutesValue.isBlank()) {
            try {
                parsedTtl = Integer.parseInt(ttlMinutesValue.trim());
            } catch (NumberFormatException ignored) {
                parsedTtl = 30;
            }
        }
        this.ttlMinutes = Math.max(1, parsedTtl);
        long parsedMinimum = 100;
        if (minimumOrderPaiseValue != null && !minimumOrderPaiseValue.isBlank()) {
            try {
                parsedMinimum = Long.parseLong(minimumOrderPaiseValue.trim());
            } catch (NumberFormatException ignored) {
                parsedMinimum = 100;
            }
        }
        this.minimumOrderPaise = Math.max(1, parsedMinimum);
    }

    @Transactional
    public AnalysisResponse analyze(MultipartFile file) throws IOException {
        List<Transaction> transactions = List.copyOf(excelReader.readExcel(file));
        if (transactions.isEmpty()) {
            throw new WorkflowException(BAD_REQUEST, "No valid transactions were found in this statement.");
        }

        int transactionCount = transactions.size();
        long amountInPaise = pricing.calculateAmountInPaise(transactionCount);

        byte[] workbookBytes;
        Map<String, List<Transaction>> grouped = transactionGrouping.groupTransactions(transactions);
        try (Workbook workbook = excelWriter.createWorkbook(grouped);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        String token = createToken();
        Instant now = Instant.now();
        UploadSession session = new UploadSession();
        session.setPublicTokenHash(hashToken(token));
        session.setTransactionCount(transactionCount);
        session.setPricePerTransactionPaise(TransactionPricingService.PRICE_PER_TRANSACTION_PAISE);
        session.setTotalAmountPaise(amountInPaise);
        session.setCurrency("INR");
        session.setStatus("ANALYZED");
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusSeconds(Math.max(60L, ttlMinutes) * 60L));
        session.setWorkbook(workbookBytes);
        sessions.saveAndFlush(session);

        return new AnalysisResponse(
                token,
                transactionCount,
                TransactionPricingService.PRICE_PER_TRANSACTION_PAISE,
                amountInPaise,
                "INR");
    }

    @Transactional
    public OrderResponse createOrder(String token) {
        UploadSession session = requireActiveSession(token);
        if ("PAID".equals(session.getStatus())) {
            throw new WorkflowException(CONFLICT, "This statement has already been paid for.");
        }
        if (session.getTotalAmountPaise() < minimumOrderPaise) {
            throw new WorkflowException(
                    BAD_REQUEST,
                    "This statement total is below the payment provider's minimum supported order amount. "
                            + "Please upload at least " + ((minimumOrderPaise + 24) / 25)
                            + " valid transaction rows.");
        }

        var existing = payments.findByUploadSession(session);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getCashfreeOrderId() != null
                    && payment.getPaymentSessionId() != null
                    && "ORDER_CREATED".equals(payment.getStatus())) {
                return new OrderResponse(
                        payment.getCashfreeOrderId(),
                        payment.getPaymentSessionId(),
                        payment.getAmountPaise(),
                        payment.getCurrency(),
                        cashfreeService.isProduction() ? "production" : "sandbox");
            }
        }

        String internalOrderId = "BL_" + session.getId().toString().replace("-", "");
        CashfreeService.PaymentOrder order = cashfreeService.createOrder(
                internalOrderId,
                session.getTotalAmountPaise(),
                "session_" + session.getId());
        if (order.amountPaise() != session.getTotalAmountPaise()
                || !"INR".equals(order.currency())) {
            throw new WorkflowException(BAD_GATEWAY, "The payment provider returned an unexpected order.");
        }

        Payment payment = new Payment();
        payment.setUploadSession(session);
        payment.setCashfreeOrderId(order.orderId());
        payment.setPaymentSessionId(order.paymentSessionId());
        payment.setAmountPaise(order.amountPaise());
        payment.setCurrency(order.currency());
        payment.setStatus("ORDER_CREATED");
        payment.setSignatureVerified(false);
        payment.setCaptured(false);
        payment.setCreatedAt(Instant.now());
        payments.saveAndFlush(payment);

        return new OrderResponse(order.orderId(), order.paymentSessionId(),
                order.amountPaise(), order.currency(),
                cashfreeService.isProduction() ? "production" : "sandbox");
    }

    @Transactional
    public void verifyPayment(String token, String orderId) {
        UploadSession session = requireActiveSession(token);
        if ("PAID".equals(session.getStatus())) {
            throw new WorkflowException(CONFLICT, "This payment has already been verified.");
        }
        if (isBlank(orderId)) {
            throw new WorkflowException(BAD_REQUEST, "A Cashfree order ID is required.");
        }

        Payment payment = payments.findByCashfreeOrderId(orderId)
                .orElseThrow(() -> new WorkflowException(NOT_FOUND, "The payment order was not found."));
        if (!payment.getUploadSession().getId().equals(session.getId())) {
            throw new WorkflowException(BAD_REQUEST, "The payment belongs to another statement.");
        }

        CashfreeService.VerifiedPayment details =
                cashfreeService.verifyOrderPayment(orderId, session.getTotalAmountPaise());
        if (!details.successful()) {
            payment.setStatus("PENDING");
            payment.setCaptured(false);
            payment.setFailureMessage(details.status());
            payments.save(payment);
            throw new WorkflowException(CONFLICT, "Payment is pending or has not been successful.");
        }

        if (details.paymentId() != null) {
            var duplicate = payments.findByCashfreePaymentId(details.paymentId())
                    .filter(p -> !p.getId().equals(payment.getId()));
            if (duplicate.isPresent()) {
                throw new WorkflowException(CONFLICT, "This payment was already used for another order.");
            }
        }

        payment.setSignatureVerified(true);
        payment.setCashfreePaymentId(details.paymentId());
        payment.setCaptured(true);
        payment.setStatus("PAID");
        payment.setVerifiedAt(Instant.now());
        payments.save(payment);
        session.setStatus("PAID");
        sessions.save(session);

        DownloadRecord record = downloads.findByUploadSession(session).orElseGet(DownloadRecord::new);
        record.setUploadSession(session);
        record.setPayment(payment);
        record.setDownloadStatus("READY");
        record.setRedemptionStatus("UNREDEEMED");
        record.setCreatedAt(record.getCreatedAt() == null ? Instant.now() : record.getCreatedAt());
        downloads.save(record);
    }

    @Transactional
    public byte[] download(String token) throws IOException {
        UploadSession session = requireActiveSession(token);
        if (!"PAID".equals(session.getStatus()) || session.getRedeemedAt() != null) {
            throw new WorkflowException(PAYMENT_REQUIRED, "A verified, unredeemed payment is required.");
        }
        DownloadRecord record = downloads.findByUploadSession(session)
                .orElseThrow(() -> new WorkflowException(PAYMENT_REQUIRED, "No verified payment record exists."));
        if (!"UNREDEEMED".equals(record.getRedemptionStatus())
                || !"PAID".equals(record.getPayment().getStatus())) {
            throw new WorkflowException(CONFLICT, "This workbook has already been redeemed.");
        }
        if (session.getWorkbook() == null || session.getWorkbook().length == 0) {
            throw new WorkflowException(NOT_FOUND, "The workbook is no longer available.");
        }

        byte[] bytes = session.getWorkbook();
        record.setRedemptionStatus("REDEEMED");
        record.setDownloadStatus("DOWNLOADED");
        record.setRedeemedAt(Instant.now());
        downloads.save(record);
        session.setRedeemedAt(Instant.now());
        session.setWorkbook(null);
        sessions.save(session);
        return bytes;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void removeExpiredSessions() {
        sessions.expireAndClear(Instant.now());
    }

    private UploadSession requireActiveSession(String token) {
        if (isBlank(token)) {
            throw new WorkflowException(BAD_REQUEST, "A statement session token is required.");
        }
        UploadSession session = sessions.findByPublicTokenHash(hashToken(token))
                .orElseThrow(() -> new WorkflowException(NOT_FOUND, "The statement session was not found or has expired."));
        if (!Instant.now().isBefore(session.getExpiresAt())) {
            throw new WorkflowException(GONE, "The statement session has expired. Upload the file again.");
        }
        if ("EXPIRED".equals(session.getStatus())) {
            throw new WorkflowException(GONE, "The statement session has expired.");
        }
        return session;
    }

    private static String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AnalysisResponse(String sessionToken, int transactionCount,
                                   int pricePerTransactionPaise, long totalAmountPaise, String currency) {}
    public record OrderResponse(String orderId, String paymentSessionId,
                                long amountPaise, String currency, String environment) {}
}
