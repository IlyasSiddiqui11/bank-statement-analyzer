package com.example.bank_statement_analyzer.repository;

import com.example.bank_statement_analyzer.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByRazorpayEventId(String razorpayEventId);
}
