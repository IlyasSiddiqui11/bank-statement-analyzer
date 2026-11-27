package com.example.bank_statement_analyzer.controller;

import com.example.bank_statement_analyzer.service.PaymentSessionService;
import com.example.bank_statement_analyzer.service.WorkflowException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentSessionService paymentSessionService;

    public PaymentController(PaymentSessionService paymentSessionService) {
        this.paymentSessionService = paymentSessionService;
    }

    @PostMapping("/create-order")
    public PaymentSessionService.OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        if (request == null) {
            throw new WorkflowException(HttpStatus.BAD_REQUEST, "A statement session token is required.");
        }
        return paymentSessionService.createOrder(request.sessionToken());
    }

    @PostMapping("/verify")
    public Map<String, Boolean> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        if (request == null) {
            throw new WorkflowException(HttpStatus.BAD_REQUEST, "Payment details are required.");
        }
        paymentSessionService.verifyPayment(request.sessionToken(), request.orderId());
        return Map.of("verified", true);
    }

    public record CreateOrderRequest(String sessionToken) {}

    public record VerifyPaymentRequest(
            String sessionToken,
            String orderId) {}
}
