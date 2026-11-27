package com.example.bank_statement_analyzer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionPricingServiceTest {

    private final TransactionPricingService pricing = new TransactionPricingService();

    @Test
    void calculatesTwentyFivePaisePerTransaction() {
        assertEquals(25, pricing.calculateAmountInPaise(1));
        assertEquals(6250, pricing.calculateAmountInPaise(250));
    }

    @Test
    void rejectsZeroAndNegativeTransactionCounts() {
        assertThrows(IllegalArgumentException.class, () -> pricing.calculateAmountInPaise(0));
        assertThrows(IllegalArgumentException.class, () -> pricing.calculateAmountInPaise(-1));
    }
}
