package com.example.bank_statement_analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class CashfreeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CashfreeService.class);

    private final String appId;
    private final String secretKey;
    private final String apiVersion;
    private final String environment;
    private final String frontendUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CashfreeService(
            @Value("${cashfree.app.id:}") String appId,
            @Value("${cashfree.secret.key:}") String secretKey,
            @Value("${cashfree.api.version:2023-08-01}") String apiVersion,
            @Value("${cashfree.env:SANDBOX}") String environment,
            @Value("${app.frontend.url:http://localhost:3000}") String frontendUrl,
            ObjectMapper objectMapper) {
        this.appId = appId == null ? "" : appId.trim();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.apiVersion = apiVersion;
        this.environment = "PRODUCTION".equalsIgnoreCase(environment) ? "PRODUCTION" : "SANDBOX";
        this.frontendUrl = frontendUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public PaymentOrder createOrder(String orderId, long amountPaise, String customerId, String customerPhone) {
        requireCredentials();
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("order_id", orderId);
        requestBody.put("order_amount", amountPaise / 100.0);
        requestBody.put("order_currency", "INR");

        ObjectNode customer = requestBody.putObject("customer_details");
        customer.put("customer_id", customerId);
        customer.put("customer_phone", customerPhone);

        ObjectNode orderMeta = requestBody.putObject("order_meta");
        orderMeta.put("return_url", frontendUrl.replaceAll("/$", "") + "/payment/complete");

        JsonNode response = post("/pg/orders", requestBody);
        JsonNode order = response;
        String returnedOrderId = text(order, "order_id", text(order, "cf_order_id", ""));
        String paymentSessionId = text(order, "payment_session_id", "");
        if (returnedOrderId.isBlank() || paymentSessionId.isBlank()) {
            LOGGER.error("Cashfree create-order returned an incomplete response.");
            throw new WorkflowException(BAD_GATEWAY, "The payment provider returned an incomplete order.");
        }
        long returnedAmountPaise = decimalToPaise(order.path("order_amount").asText("0"));
        String currency = text(order, "order_currency", "INR");
        if (returnedAmountPaise != amountPaise || !"INR".equals(currency)) {
            throw new WorkflowException(BAD_GATEWAY, "The payment provider returned an unexpected order.");
        }
        return new PaymentOrder(returnedOrderId, paymentSessionId, returnedAmountPaise, currency);
    }

    public VerifiedPayment verifyOrderPayment(String orderId, long expectedAmountPaise) {
        requireCredentials();
        JsonNode order = get("/pg/orders/" + encode(orderId));
        String returnedOrderId = text(order, "order_id", text(order, "cf_order_id", ""));
        if (!orderId.equals(returnedOrderId)) {
            throw new WorkflowException(BAD_GATEWAY, "The payment provider returned a different order.");
        }
        long orderAmountPaise = decimalToPaise(order.path("order_amount").asText("0"));
        String orderCurrency = text(order, "order_currency", "INR");
        if (orderAmountPaise != expectedAmountPaise || !"INR".equals(orderCurrency)) {
            throw new WorkflowException(BAD_GATEWAY, "The payment amount or currency does not match.");
        }

        JsonNode payments = get("/pg/orders/" + encode(orderId) + "/payments");
        if (!payments.isArray()) {
            throw new WorkflowException(BAD_GATEWAY, "The payment provider returned an invalid payment response.");
        }
        JsonNode successful = null;
        for (JsonNode payment : payments) {
            if ("SUCCESS".equalsIgnoreCase(text(payment, "payment_status", ""))) {
                long paymentAmount = decimalToPaise(payment.path("payment_amount").asText("0"));
                String paymentCurrency = text(payment, "payment_currency", "INR");
                if (paymentAmount == expectedAmountPaise && "INR".equals(paymentCurrency)) {
                    successful = payment;
                    break;
                }
            }
        }
        if (successful == null) {
            return new VerifiedPayment(false, null, null, "Payment is pending or not successful.");
        }

        String paymentId = text(successful, "cf_payment_id",
                text(successful, "payment_id", ""));
        if (paymentId.isBlank()) {
            throw new WorkflowException(BAD_GATEWAY, "The successful payment has no provider payment ID.");
        }
        return new VerifiedPayment(true, paymentId,
                text(successful, "payment_currency", "INR"), "SUCCESS");
    }

    public boolean verifyWebhookSignature(String timestamp, String rawBody, String signature) {
        requireCredentials();
        if (timestamp == null || timestamp.isBlank() || rawBody == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(
                    mac.doFinal((timestamp + rawBody).getBytes(StandardCharsets.UTF_8)));
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            LOGGER.error("Webhook signature verification could not be performed.");
            return false;
        }
    }

    public boolean isProduction() {
        return "PRODUCTION".equals(environment);
    }

    private JsonNode post(String path, JsonNode body) {
        try {
            HttpRequest request = authorizedRequest(path)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            return execute(request);
        } catch (WorkflowException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.error("Cashfree order creation request failed.");
            throw new WorkflowException(BAD_GATEWAY, "The payment provider could not create the order.");
        }
    }

    private JsonNode get(String path) {
        try {
            HttpRequest request = authorizedRequest(path).GET().build();
            return execute(request);
        } catch (WorkflowException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.error("Cashfree payment lookup request failed.");
            throw new WorkflowException(BAD_GATEWAY, "The payment provider could not verify the payment.");
        }
    }

    private HttpRequest.Builder authorizedRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-api-version", apiVersion)
                .header("x-client-id", appId)
                .header("x-client-secret", secretKey);
    }

    private JsonNode execute(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String providerCode = null;
            String providerMessage = null;
            try {
                JsonNode error = objectMapper.readTree(response.body());
                providerCode = text(error, "code", text(error, "type", null));
                providerMessage = text(error, "message", null);
            } catch (Exception ignored) {
                providerCode = null;
                providerMessage = null;
            }
            LOGGER.error("Cashfree API returned HTTP {} code={} messagePresent={}",
                    response.statusCode(), providerCode, providerMessage != null);
            throw new WorkflowException(BAD_GATEWAY, "The payment provider request failed.");
        }
        return objectMapper.readTree(response.body());
    }

    private String baseUrl() {
        return "PRODUCTION".equals(environment)
                ? "https://api.cashfree.com"
                : "https://sandbox.cashfree.com";
    }

    private void requireCredentials() {
        if (appId.isBlank() || secretKey.isBlank()) {
            throw new WorkflowException(SERVICE_UNAVAILABLE, "Cashfree payments are not configured on the server.");
        }
    }

    private static String text(JsonNode node, String key, String fallback) {
        JsonNode value = node.get(key);
        return value != null && !value.isNull() ? value.asText(fallback) : fallback;
    }

    private static long decimalToPaise(String amount) {
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(amount);
            return value.movePointRight(2).setScale(0, java.math.RoundingMode.UNNECESSARY).longValueExact();
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record PaymentOrder(String orderId, String paymentSessionId, long amountPaise, String currency) {}
    public record VerifiedPayment(boolean successful, String paymentId, String currency, String status) {}
}
