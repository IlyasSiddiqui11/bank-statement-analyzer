package com.example.bank_statement_analyzer.service;

import com.example.bank_statement_analyzer.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class TransactionGroupingService {

    private final TransactionParserService transactionParser;

    public TransactionGroupingService(
            TransactionParserService transactionParser) {

        this.transactionParser = transactionParser;
    }

    public Map<String, List<Transaction>> groupTransactions(
            List<Transaction> transactions) {

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction ->
                                transactionParser.extractName(
                                        transaction.getParticulars()
                                ),
                        TreeMap::new,
                        Collectors.toList()
                ));
    }
}