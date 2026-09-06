# Bank Statement Analyzer

A free web-based tool that automatically organizes bank statement Excel files into separate sheets based on people, merchants, and transaction categories.

Upload a bank statement Excel file, and the application reads, analyzes, groups, and generates a new organized Excel workbook.

## Features

- Upload bank statement Excel files
- Automatically read transaction data
- Parse transaction descriptions
- Identify people, merchants, and transaction categories
- Group related transactions
- Generate a separate Excel sheet for each person or merchant
- Separate ATM transactions
- Calculate Total Debit for each group
- Calculate Total Credit for each group
- Generate an overall Summary sheet
- Organize transactions into `To` and `By` sections
- Download the organized Excel workbook
- No login or signup required
- No database required

## How It Works

```text
Bank Statement Excel
        |
        v
Read Transactions
        |
        v
Parse Transaction Details
        |
        v
Identify Person / Merchant / Category
        |
        v
Group Transactions
        |
        v
Calculate Debit & Credit
        |
        v
Generate Summary
        |
        v
Generate Organized Excel
        |
        v
Download Result
