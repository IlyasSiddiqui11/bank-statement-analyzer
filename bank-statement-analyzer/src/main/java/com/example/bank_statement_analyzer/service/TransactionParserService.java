package com.example.bank_statement_analyzer.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TransactionParserService {

    public String extractName(String particulars) {
        if (particulars == null || particulars.isBlank()) {
            return "UNKNOWN";
        }

        String normalizedParticulars = particulars.trim();
        String[] parts = normalizedParticulars.split("/", -1);

        if (parts.length >= 2 && normalize(parts[0]).equals("UPI")) {
            String transactionType = normalize(parts[1]);
            if ("P2P".equals(transactionType)
                    || "P2A".equals(transactionType)
                    || "P2M".equals(transactionType)) {
                return extractUpiCounterparty(parts);
            }

            if ("CRADJ".equals(transactionType)) {
                return "CREDIT ADJUSTMENT";
            }
        }

        if (normalizedParticulars.toUpperCase(Locale.ROOT).contains("ATM")) {
            return "ATM";
        }

        if (parts.length >= 3 && normalize(parts[0]).equals("NEFT")) {
            return normalizeOrUnknown(parts[2]);
        }

        if (parts.length >= 3 &&
                normalize(parts[0]).equals("MOB") &&
                normalize(parts[1]).equals("TPFT")) {
            return normalizeOrUnknown(parts[2]);
        }

        String upperParticulars = normalizedParticulars.toUpperCase(Locale.ROOT);
        if (upperParticulars.contains("INT.PD")) {
            return "INTEREST";
        }

        if (upperParticulars.contains("CARD CHARGES")) {
            return "BANK CHARGES";
        }

        if (parts.length >= 5 && normalize(parts[0]).equals("IMPS")) {
            return normalizeOrUnknown(parts[4]);
        }

        return "UNKNOWN";
    }

    private static String extractUpiCounterparty(String[] parts) {
        if (parts.length < 4 || normalize(parts[2]).isEmpty()) {
            return "UNKNOWN";
        }

        String counterparty = normalize(parts[3]);
        return counterparty.isEmpty() ? "UNKNOWN" : counterparty;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String normalizeOrUnknown(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? "UNKNOWN" : normalized;
    }
}