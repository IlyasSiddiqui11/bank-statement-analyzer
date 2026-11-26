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
    private final RazorpayService razorpayService;
    private final UploadSessionRepository sessions;
    private final PaymentRepository payments;
    private final DownloadRecordRepository downloads;
    private final int ttlMinutes;

    public PaymentSessionService(
            ExcelReaderService excelReader,
            TransactionGroupingService transactionGrouping,
            ExcelWriterService excelWriter,
            TransactionPricingService pricing,
            RazorpayService razorpayService,
            UploadSessionRepository sessions,
            PaymentRepository payments,
            DownloadRecordRepository downloads,
            @Value("${app.session.ttl-minutes:30}") String ttlMinutesValue) {
        this.excelReader = excelReader;
        this.transactionGrouping = transactionGrouping;
        this.excelWriter = excelWriter;
        this.pricing = pricing;
        this.razorpayService = razorpayService;
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
    public OrderResponse createOrder(String token) throws com.razorpay.RazorpayException {
        UploadSession session = requireActiveSession(token);
        if ("PAID".equals(session.getStatus())) {
            throw new WorkflowException(CONFLICT, "This statement has already been paid for.");
        }

        var existing = payments.findByUploadSession(session);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getRazorpayOrderId() != null
                    && "ORDER_CREATED".equals(payment.getStatus())) {
                return new OrderResponse(
                        payment.getRazorpayOrderId(),
                        payment.getAmountPaise(),
                        payment.getCurrency(),
                        razorpayService.getKeyId());
            }
        }

        RazorpayService.PaymentOrder order = razorpayService.createOrder(
                session.getTotalAmountPaise(),
                "statement_" + session.getId().toString().substring(0, 12));
        if (order.amountPaise() != session.getTotalAmountPaise()
                || !"INR".equals(order.currency())) {
            throw new WorkflowException(BAD_GATEWAY, "The payment provider returned an unexpected order.");
        }

        Payment payment = new Payment();
        payment.setUploadSession(session);
        payment.setRazorpayOrderId(order.orderId());
        payment.setAmountPaise(order.amountPaise());
        payment.setCurrency(order.currency());
        payment.setStatus("ORDER_CREATED");
        payment.setSignatureVerified(false);
        payment.setCaptured(false);
        payment.setCreatedAt(Instant.now());
        payments.saveAndFlush(payment);

        return new OrderResponse(order.orderId(), order.amountPaise(), order.currency(), razorpayService.getKeyId());
    }

    @Transactional
    public void verifyPayment(String token, String orderId, String paymentId, String signature)
            throws com.razorpay.RazorpayException {
        UploadSession session = requireActiveSession(token);
        if ("PAID".equals(session.getStatus())) {
            throw new WorkflowException(CONFLICT, "This payment has already been verified.");
        }
        if (isBlank(orderId) || isBlank(paymentId) || isBlank(signature)) {
            throw new WorkflowException(BAD_REQUEST, "Payment details are required.");
        }

        Payment payment = payments.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new WorkflowException(NOT_FOUND, "The payment order was not found."));
        if (!payment.getUploadSession().getId().equals(session.getId())) {
            throw new WorkflowException(BAD_REQUEST, "The payment belongs to another statement.");
        }
        if (payment.getRazorpayPaymentId() != null
                && !payment.getRazorpayPaymentId().equals(paymentId)) {
            throw new WorkflowException(CONFLICT, "This order was already used with another payment.");
        }
        if (payments.findByRazorpayPaymentId(paymentId).filter(p -> !p.getId().equals(payment.getId())).isPresent()) {
            throw new WorkflowException(CONFLICT, "This payment ID was already used.");
        }

        if (!razorpayService.verifyPaymentSignature(orderId, paymentId, signature)) {
            payment.setStatus("FAILED");
            payment.setSignatureVerified(false);
            payment.setFailureMessage("Invalid payment signature");
            payments.save(payment);
            throw new WorkflowException(BAD_REQUEST, "The payment signature is invalid.");
        }

        payment.setSignatureVerified(true);
        payment.setRazorpayPaymentId(paymentId);
        RazorpayService.PaymentDetails details = razorpayService.fetchPayment(paymentId);
        if (!orderId.equals(details.orderId())
                || details.amountPaise() != session.getTotalAmountPaise()
                || !"INR".equals(details.currency())
                || !"captured".equalsIgnoreCase(details.status())
                || !details.captured()) {
            payment.setStatus("PENDING");
            payment.setCaptured(false);
            payment.setFailureMessage("Payment is not captured or does not match the session");
            payments.save(payment);
            throw new WorkflowException(CONFLICT, "Payment is not captured or does not match the order.");
        }

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
    public record OrderResponse(String orderId, long amountPaise, String currency, String keyId) {}
}
