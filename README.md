# Bank Statement Analyzer

A powerful, privacy-first web application designed to automatically parse, classify, and organize raw bank statement Excel spreadsheets into structured multi-sheet Excel workbooks grouped by entities, merchants, transaction types, and accounts.

---

## 🌟 Overview

Analyzing raw bank statements can be time-consuming and error-prone. **Bank Statement Analyzer** automates the entire organization workflow:
1. Reads raw transaction logs from an uploaded bank statement Excel file (`.xlsx`).
2. Intelligently parses transaction particulars to extract payee names, merchants, UPI modes, NEFT transfers, ATM withdrawals, and bank fees.
3. Groups transactions by recipient, merchant, or category.
4. Calculates sub-totals for debits and credits per group.
5. Compiles an executive **Summary** ledger sheet along with itemized entity sheets in a downloadable Excel workbook.

---

## 🔥 Key Features

- **Automated Pattern Matching & Parser Engine**:
  - **UPI P2A & P2M**: Detects and extracts recipient and merchant names from UPI transaction strings (`UPI/P2A/...`, `UPI/P2M/...`).
  - **NEFT & IMPS & MOB TPFT**: Identifies individual or corporate names from electronic fund transfers.
  - **ATM Cash Withdrawals**: Consolidates cash withdrawal entries into a dedicated **ATM** sheet.
  - **Bank Charges & Interest**: Isolates fee deductions (`BANK CHARGES`) and interest deposits (`INTEREST`).
  - **Credit Adjustments**: Categorizes bank adjustments and refunds (`CREDIT ADJUSTMENT`).

- **Executive Summary Ledger (`To` & `By`)**:
  - Auto-generates a master summary worksheet at the front of the output workbook.
  - Displays structured breakdown columns for **To** (Incoming Credits) and **By** (Outgoing Debits).

- **Itemized Multi-Sheet Output Workbook**:
  - Generates a dedicated tab for each unique person, merchant, or category.
  - Preserves date, cheque/reference number, description, debit, credit, and running balance.
  - Automatically calculates `Total Debit` and `Total Credit` rows with bold formatting for each group.

- **Privacy-First In-Memory Processing**:
  - Operates without a database or persistent file storage.
  - Files are processed dynamically in-memory and immediately returned to the user.

---

## 📊 Data Processing Flow

```text
       ┌───────────────────────────────┐
       │   Input Bank Statement Excel  │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │     Apache POI Data Reader    │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │ Transaction Pattern Matcher & │
       │     Intelligence Engine       │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │ Entity & Category Grouping    │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │  Subtotal & Summary Ledger    │
       │          Calculation          │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │ Multi-Sheet Excel Workbook    │
       │          Generation           │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │    Instant File Download      │
       └───────────────┴───────────────┘
```

---

## 📋 Required Input Excel Format

The input bank statement Excel file must contain a header row matching these 6 standard columns:

| Column Name | Description | Example Value |
| :--- | :--- | :--- |
| `Tran Date` | Transaction date | `01/04/2024` |
| `Chq No` | Cheque / reference number | `100234` or `-` |
| `Particulars` | Transaction description string | `UPI/4092182/TRANSFER TO ALICE` |
| `Debit` | Withdrawal / outgoing amount | `1,500.00` |
| `Credit` | Deposit / incoming amount | `75,000.00` |
| `Balance` | Account balance following transaction | `45,250.00` |

---

## 📑 Output Workbook Structure

The generated Excel workbook (`Organized_Statement.xlsx`) contains:

1. **`Summary` Sheet**:
   - Master ledger view displaying total credits (`To`) and debits (`By`) per group.
2. **Category / Entity Worksheets**:
   - Dedicated sheets for each entity (e.g., `ALICE`, `ATM`, `BANK CHARGES`, `INTEREST`, etc.).
   - Clean, formatted transaction table with auto-sized column widths and sub-totals for `Total Debit` and `Total Credit`.

---

## 🛠️ Technology Stack

- **Backend**: Java, Spring Boot, Apache POI
- **Frontend**: React, Tailwind CSS, Lucide Icons, Axios, Vite
