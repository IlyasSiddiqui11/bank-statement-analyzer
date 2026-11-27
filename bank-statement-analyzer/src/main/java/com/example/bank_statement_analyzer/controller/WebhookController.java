package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.model.Payment;
import com.example.bank_statement_analyzer.model.UploadSession;
import com.example.bank_statement_analyzer.model.WebhookEvent;
import com.example.bank_statement_analyzer.model.DownloadRecord;
import com.example.bank_statement_analyzer.repository.PaymentRepository;
import com.example.bank_statement_analyzer.repository.UploadSessionRepository;
import com.example.bank_statement_analyzer.repository.WebhookEventRepository;
import com.example.bank_statement_analyzer.repository.DownloadRecordRepository;
import com.example.bank_statement_analyzer.service.RazorpayService;
import com.example.bank_statement_analyzer.service.WorkflowException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final RazorpayService razorpayService;
    private final WebhookEventRepository webhookEvents;
    private final PaymentRepository payments;
    private final UploadSessionRepository sessions;
    private final DownloadRecordRepository downloads;

    public WebhookController(
            RazorpayService razorpayService,
            WebhookEventRepository webhookEvents,
            PaymentRepository payments,
            UploadSessionRepository sessions,
            DownloadRecordRepository downloads) {
        this.razorpayService = razorpayService;
        this.webhookEvents = webhookEvents;
        this.payments = payments;
        this.sessions = sessions;
        this.downloads = downloads;
    }

    @PostMapping(value = "/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Event-Id") String eventId,
            @RequestHeader("X-Razorpay-Signature") String signature) throws com.razorpay.RazorpayException {
        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            throw new WorkflowException(HttpStatus.UNAUTHORIZED, "Webhook signature is invalid.");
        }
        JSONObject payload = new JSONObject(rawBody);
        String event = payload.optString("event");
        if (eventId == null || eventId.isBlank()) {
            eventId = payload.optString("event_id",
                    payload.optString("id",
                            event + ":" + Integer.toUnsignedString(payload.toString().hashCode())));
        }

        if (webhookEvents.findByRazorpayEventId(eventId).isPresent()) {
            return;
        }

        WebhookEvent eventRecord = new WebhookEvent();
        eventRecord.setRazorpayEventId(eventId);
        eventRecord.setEventType(event);
        eventRecord.setProcessingStatus("RECEIVED");
        eventRecord.setReceivedAt(Instant.now());
        webhookEvents.save(eventRecord);

        if ("payment.captured".equals(event) || "payment.authorized".equals(event)) {
            JSONObject payloadNode = payload.optJSONObject("payload");
            JSONObject paymentNode = payloadNode == null ? null : payloadNode.optJSONObject("payment");
            JSONObject entity = paymentNode == null ? null : paymentNode.optJSONObject("entity");
            if (entity == null) {
                eventRecord.setProcessingStatus("PROCESSED");
                eventRecord.setProcessedAt(Instant.now());
                webhookEvents.save(eventRecord);
                return;
            }
            String paymentId = entity.optString("id");
            String orderId = entity.optString("order_id");
            long amount = entity.optLong("amount", -1);
            String currency = entity.optString("currency");
            boolean captured = "captured".equalsIgnoreCase(entity.optString("status"))
                    || "payment.captured".equals(event);

            Payment payment = payments.findByRazorpayOrderId(orderId).orElse(null);
            if (payment != null
                    && payment.getAmountPaise() == amount
                    && "INR".equals(currency)
                    && captured
                    && payment.getUploadSession() != null) {
                payment.setRazorpayPaymentId(paymentId);
                payment.setCaptured(true);
                payment.setStatus("PAID");
                if (payment.getVerifiedAt() == null) {
                    payment.setVerifiedAt(Instant.now());
                }
                payments.save(payment);

                UploadSession session = payment.getUploadSession();
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
        }

        eventRecord.setProcessingStatus("PROCESSED");
        eventRecord.setProcessedAt(Instant.now());
        webhookEvents.save(eventRecord);
    }
}
