package com.example.bank_statement_analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private LocalDate date;
    private String chequeNumber;
    private String particulars;
    private Double debit;
    private Double credit;
    private Double balance;
}