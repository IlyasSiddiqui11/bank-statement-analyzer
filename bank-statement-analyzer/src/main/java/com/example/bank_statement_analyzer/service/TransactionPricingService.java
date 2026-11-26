package com.example.bank_statement_analyzer.service;

import org.springframework.stereotype.Service;

@Service
public class TransactionPricingService {

    public static final int PRICE_PER_TRANSACTION_PAISE = 25;

    public long calculateAmountInPaise(int transactionCount) {
        if (transactionCount <= 0) {
            throw new IllegalArgumentException("The statement contains no valid transactions.");
        }
        return Math.multiplyExact((long) transactionCount, PRICE_PER_TRANSACTION_PAISE);
    }
}
