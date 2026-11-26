
package com.example.bank_statement_analyzer.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RazorpayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    @PostConstruct
    void logConfigurationStatus() {
        boolean keyIdPresent = keyId != null && !keyId.isBlank();
        boolean secretPresent = keySecret != null && !keySecret.isBlank();
        if (!keyIdPresent || !secretPresent) {
            LOGGER.error(
                    "Razorpay configuration is incomplete. keyIdPresent={}, secretPresent={}",
                    keyIdPresent,
                    secretPresent);
            return;
        }
        LOGGER.info(
                "Razorpay credentials loaded. testMode={}",
                keyId.startsWith("rzp_test_"));
    }

    public PaymentOrder createOrder(long amountInPaise, String receipt) throws RazorpayException {
        requireCredentials();
        if (amountInPaise <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }

        JSONObject orderRequest = new JSONObject()
                .put("amount", amountInPaise)
                .put("currency", "INR")
                .put("receipt", receipt);
        Order order;
        try {
            order = new RazorpayClient(keyId, keySecret).orders.create(orderRequest);
        } catch (RazorpayException exception) {
            LOGGER.error("Razorpay order creation failed.", exception);
            throw exception;
        }
        JSONObject data = order.toJson();
        return new PaymentOrder(
                data.getString("id"),
                data.getLong("amount"),
                data.getString("currency"));
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature)
            throws RazorpayException {
        requireCredentials();
        JSONObject attributes = new JSONObject()
                .put("razorpay_order_id", orderId)
                .put("razorpay_payment_id", paymentId)
                .put("razorpay_signature", signature);
        return Utils.verifyPaymentSignature(attributes, keySecret);
    }

    public PaymentDetails fetchPayment(String paymentId) throws RazorpayException {
        requireCredentials();
        Payment payment;
        try {
            payment = new RazorpayClient(keyId, keySecret).payments.fetch(paymentId);
        } catch (RazorpayException exception) {
            LOGGER.error("Razorpay payment lookup failed.", exception);
            throw exception;
        }
        JSONObject data = payment.toJson();
        return new PaymentDetails(
                data.optString("order_id"),
                data.optLong("amount", -1),
                data.optString("currency"),
                data.optString("status"),
                data.optBoolean("captured", false));
    }

    public String getKeyId() {
        requireCredentials();
        return keyId;
    }

    private void requireCredentials() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new WorkflowException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payments are not configured on the server.");
        }
    }

    private void requireWebhookSecret() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new WorkflowException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Webhook verification is not configured.");
        }
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) throws RazorpayException {
        requireWebhookSecret();
        return Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
    }

    public record PaymentOrder(String orderId, long amountPaise, String currency) {}

    public record PaymentDetails(
            String orderId,
            long amountPaise,
            String currency,
            String status,
            boolean captured) {}
}
