package com.example.bank_statement_analyzer.repository;

import com.example.bank_statement_analyzer.model.Payment;
import com.example.bank_statement_analyzer.model.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<Payment> findByUploadSession(UploadSession uploadSession);
}
