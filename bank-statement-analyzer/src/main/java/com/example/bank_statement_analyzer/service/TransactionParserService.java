package com.example.bank_statement_analyzer.service;

import org.springframework.stereotype.Service;

@Service
public class TransactionParserService {

    public String extractName(String particulars) {

        // Empty particulars
        if (particulars == null || particulars.isBlank()) {
            return "UNKNOWN";
        }

        String[] parts = particulars.split("/");

        // --------------------------------------------------
        // UPI P2A → Person
        // --------------------------------------------------
        if (parts.length >= 4 &&
                parts[0].equalsIgnoreCase("UPI") &&
                parts[1].equalsIgnoreCase("P2A")) {

            return parts[3].trim().toUpperCase();
        }

        // --------------------------------------------------
        // UPI P2M → Merchant
        // --------------------------------------------------
        if (parts.length >= 4 &&
                parts[0].equalsIgnoreCase("UPI") &&
                parts[1].equalsIgnoreCase("P2M")) {

            return parts[3].trim().toUpperCase();
        }

        // --------------------------------------------------
        // UPI Credit Adjustment
        // --------------------------------------------------
        if (parts.length >= 2 &&
                parts[0].equalsIgnoreCase("UPI") &&
                parts[1].equalsIgnoreCase("CRADJ")) {

            return "CREDIT ADJUSTMENT";
        }

        // --------------------------------------------------
        // ATM → ATM
        // --------------------------------------------------
        if (particulars.toUpperCase().contains("ATM")) {
            return "ATM";
        }

        // --------------------------------------------------
        // NEFT → Person / Company
        // --------------------------------------------------
        if (parts.length >= 3 &&
                parts[0].equalsIgnoreCase("NEFT")) {

            return parts[2].trim().toUpperCase();
        }

        if (parts.length >= 3 &&
                parts[0].equalsIgnoreCase("MOB") &&
                parts[1].equalsIgnoreCase("TPFT")) {

            return parts[2].trim().toUpperCase();
        }

        if (particulars.toUpperCase().contains("INT.PD")) {
            return "INTEREST";
        }

        if (particulars.toUpperCase().contains("CARD CHARGES")) {
            return "BANK CHARGES";
        }

        if (parts.length >= 2 &&
                parts[0].equalsIgnoreCase("UPI") &&
                parts[1].equalsIgnoreCase("CRADJ")) {

            return "CREDIT ADJUSTMENT";
        }
        // Anything we don't recognize
        return "UNKNOWN";
    }
}