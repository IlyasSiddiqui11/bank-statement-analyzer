package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.model.*;
import com.example.bank_statement_analyzer.repository.PaymentRepository;
import com.example.bank_statement_analyzer.repository.UploadSessionRepository;
import com.example.bank_statement_analyzer.repository.WebhookEventRepository;
import com.example.bank_statement_analyzer.repository.DownloadRecordRepository;
import com.example.bank_statement_analyzer.service.CashfreeService;
import com.example.bank_statement_analyzer.service.WorkflowException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final CashfreeService cashfreeService;
    private final ObjectMapper objectMapper;
    private final WebhookEventRepository webhookEvents;
    private final PaymentRepository payments;
    private final UploadSessionRepository sessions;
    private final DownloadRecordRepository downloads;

    public WebhookController(
            CashfreeService cashfreeService,
            ObjectMapper objectMapper,
            WebhookEventRepository webhookEvents,
            PaymentRepository payments,
            UploadSessionRepository sessions,
            DownloadRecordRepository downloads) {
        this.cashfreeService = cashfreeService;
        this.objectMapper = objectMapper;
        this.webhookEvents = webhookEvents;
        this.payments = payments;
        this.sessions = sessions;
        this.downloads = downloads;
    }

    @PostMapping(value = "/cashfree", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void handleCashfreeWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature,
            @RequestHeader(value = "x-webhook-event-id", required = false) String suppliedEventId) {
        if (!cashfreeService.verifyWebhookSignature(timestamp, rawBody, signature)) {
            throw new WorkflowException(HttpStatus.UNAUTHORIZED, "Webhook signature is invalid.");
        }
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String event = text(payload, "type",
                    text(payload, "event_type", "UNKNOWN"));
            String orderId = findText(payload, "order_id", "orderId");
            String paymentId = findText(payload, "cf_payment_id", "payment_id");
            String eventId = suppliedEventId;
            if (eventId == null || eventId.isBlank()) {
                eventId = text(payload, "event_id",
                        text(payload, "id",
                                sha256(timestamp + ":" + event + ":" + orderId + ":" + paymentId)));
            }
            if (webhookEvents.findByCashfreeEventId(eventId).isPresent()) {
                return;
            }

            WebhookEvent eventRecord = new WebhookEvent();
            eventRecord.setCashfreeEventId(eventId);
            eventRecord.setEventType(event);
            eventRecord.setProcessingStatus("RECEIVED");
            eventRecord.setReceivedAt(Instant.now());
            webhookEvents.save(eventRecord);

            if (orderId != null && !orderId.isBlank()) {
                Payment payment = payments.findByCashfreeOrderId(orderId).orElse(null);
                if (payment != null) {
                    UploadSession session = payment.getUploadSession();
                    if ("PAYMENT_SUCCESS_WEBHOOK".equalsIgnoreCase(event)
                            || event.toUpperCase().contains("SUCCESS")) {
                        CashfreeService.VerifiedPayment verified =
                                cashfreeService.verifyOrderPayment(orderId, session.getTotalAmountPaise());
                        if (verified.successful()) {
                            markPaid(payment, session, verified.paymentId());
                        }
                    } else if (!"PAID".equals(payment.getStatus())) {
                        payment.setStatus("FAILED");
                        payment.setCaptured(false);
                        payment.setFailureMessage("Cashfree reported an unsuccessful payment.");
                        payments.save(payment);
                    }
                }
            }

            eventRecord.setProcessingStatus("PROCESSED");
            eventRecord.setProcessedAt(Instant.now());
            webhookEvents.save(eventRecord);
        } catch (WorkflowException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorkflowException(HttpStatus.BAD_REQUEST, "The webhook payload is invalid.");
        }
    }

    private void markPaid(Payment payment, UploadSession session, String paymentId) {
        if ("PAID".equals(payment.getStatus())) {
            return;
        }
        payment.setSignatureVerified(true);
        payment.setCashfreePaymentId(paymentId);
        payment.setCaptured(true);
        payment.setStatus("PAID");
        if (payment.getVerifiedAt() == null) {
            payment.setVerifiedAt(Instant.now());
        }
        payments.save(payment);
        session.setStatus("PAID");
        sessions.save(session);

        DownloadRecord record = downloads.findByUploadSession(session).orElseGet(DownloadRecord::new);
        record.setUploadSession(session);
        record.setPayment(payment);
        record.setDownloadStatus("READY");
        record.setRedemptionStatus("UNREDEEMED");
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(Instant.now());
        }
        downloads.save(record);
    }

    private static String text(JsonNode node, String key, String fallback) {
        JsonNode value = node.get(key);
        return value != null && !value.isNull() ? value.asText(fallback) : fallback;
    }

    private static String findText(JsonNode payload, String... keys) {
        for (JsonNode node : payload.findValues("order")) {
            for (String key : keys) {
                JsonNode value = node.get(key);
                if (value != null && !value.isNull()) return value.asText();
            }
        }
        for (String key : keys) {
            JsonNode value = payload.get(key);
            if (value != null && !value.isNull()) return value.asText();
        }
        return payload.findValues(keys[0]).stream()
                .findFirst().map(JsonNode::asText).orElse(null);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            return UUID.randomUUID().toString();
        }
    }
}
