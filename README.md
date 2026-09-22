````markdown
# Bank Statement Analyzer

A web-based application designed to automatically parse, classify, and organize bank statement Excel spreadsheets into structured multi-sheet Excel workbooks grouped by entities, merchants, and transaction categories.

---

## 🌟 Overview

Organizing large bank statements manually can be time-consuming and repetitive. **Bank Statement Analyzer** automates this workflow by transforming raw transaction data into a structured and easy-to-review Excel workbook.

The application:

1. Reads transaction data from an uploaded `.xlsx` bank statement.
2. Validates the required Excel structure.
3. Parses transaction particulars to identify people, merchants, and transaction categories.
4. Groups transactions based on the extracted entity or category.
5. Calculates debit and credit totals for each group.
6. Generates a master **Summary** sheet.
7. Creates individual worksheets for each person, merchant, or category.
8. Produces a downloadable `Organized_Statement.xlsx` workbook.

---

## 🔥 Key Features

### Automated Transaction Parsing

The application identifies common banking transaction patterns, including:

- **UPI P2A** — Person-to-person transactions
- **UPI P2M** — Merchant transactions
- **NEFT** — Bank transfers
- **IMPS** — Instant bank transfers
- **MOB / TPFT** — Mobile fund transfers
- **ATM** — Cash withdrawals
- **Credit Adjustments** — Bank credit adjustments
- **Interest** — Interest credits
- **Bank Charges** — Card and banking charges

Transactions that cannot be identified are grouped under:

```text
UNKNOWN
````

This allows them to be reviewed manually instead of assigning an uncertain transaction to the wrong entity.

---

### 📊 Automatic Transaction Grouping

Transactions are grouped according to the extracted person, merchant, or category.

Example:

```text
Bank Statement
      │
      ├── ZEPTO
      ├── ZOMATO
      ├── SYAM SUNDAR
      ├── ABDUL SHEHBAZ A
      ├── FAMILY SUPPORT
      ├── ATM
      ├── INTEREST
      ├── BANK CHARGES
      └── UNKNOWN
```

Each group is generated as a separate worksheet in the output workbook.

---

### 📑 Summary Sheet

The generated workbook contains a `Summary` sheet providing an overview of incoming and outgoing transactions.

The summary separates:

* **To** — Credit transactions
* **By** — Debit transactions

Example:

| To       | Amount |   | By              | Amount |
| -------- | -----: | - | --------------- | -----: |
| ZEPTO    |   ₹... |   | SYAM SUNDAR     |   ₹... |
| ZOMATO   |   ₹... |   | ABDUL SHEHBAZ A |   ₹... |
| INTEREST |   ₹... |   | ATM             |   ₹... |

---

### 📄 Individual Transaction Sheets

A dedicated worksheet is generated for each identified entity or category.

Each sheet contains:

| Tran Date  | Chq No | Particulars | Debit | Credit | Balance |
| ---------- | ------ | ----------- | ----: | -----: | ------: |
| 01-04-2025 |        | UPI/...     |   500 |        |    9500 |
| 02-04-2025 |        | NEFT/...    |       |   1000 |   10500 |

Each individual sheet also contains:

* Total Debit
* Total Credit
* Bold formatting for totals
* Automatically sized columns

---

## 📋 Required Input Excel Format

The uploaded Excel file must contain the following six columns in the first row:

| Column        | Description                       | Example       |
| ------------- | --------------------------------- | ------------- |
| `Tran Date`   | Transaction date                  | `01/04/2025`  |
| `Chq No`      | Cheque / reference number         | `100234`      |
| `Particulars` | Transaction description           | `UPI/P2M/...` |
| `Debit`       | Outgoing transaction amount       | `1,500.00`    |
| `Credit`      | Incoming transaction amount       | `75,000.00`   |
| `Balance`     | Account balance after transaction | `45,250.00`   |

Transactions begin from the second row.

The application currently expects this standardized structure rather than additional header or opening-balance rows.

---

## 📑 Output Workbook Structure

The generated workbook is named:

```text
Organized_Statement.xlsx
```

It contains:

### 1. Summary

A master overview of grouped debit and credit amounts.

### 2. Entity / Category Worksheets

Dedicated worksheets for identified people, merchants, and categories, such as:

```text
ZEPTO
ZOMATO
SYAM SUNDAR
ATM
BANK CHARGES
INTEREST
UNKNOWN
```

Each worksheet contains the original transaction details along with group-level debit and credit totals.

---

## 🔄 Data Processing Flow

```text
       ┌───────────────────────────────┐
       │   Input Bank Statement Excel  │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │       Excel Validation        │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │      Transaction Reader       │
       │         Apache POI            │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │     Transaction Parser        │
       │   Pattern-based Extraction    │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │    Transaction Grouping       │
       │   Entity & Category Based     │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │   Summary & Total Calculation │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │   Multi-Sheet Excel Writer    │
       └───────────────┬───────────────┘
                       │
                       ▼
       ┌───────────────────────────────┐
       │    Organized Excel Download   │
       └───────────────────────────────┘
```

---

## 💰 Usage-Based Pricing

The application is being developed with a usage-based pricing model.

The current development pricing is:

```text
₹0.50 per transaction
```

The backend calculates the transaction count and generates the processing amount.

For example:

```text
931 transactions × ₹0.50
= ₹465.50
```

The calculated amount comes from the backend rather than being trusted from the frontend.

Payment integration is currently under development. The planned workflow is:

```text
Upload
   ↓
Analyze
   ↓
Calculate Transaction Count
   ↓
Generate Price
   ↓
Payment
   ↓
Verify Payment
   ↓
Process Statement
   ↓
Download Organized Excel
```

---

## 🛠️ Technology Stack

### Backend

* Java 17
* Spring Boot
* Spring Web
* Apache POI
* Lombok
* Maven

### Frontend

* React
* Vite
* Axios
* Tailwind CSS
* Lucide React

### Excel Processing

* Apache POI
* `.xlsx` workbook reading
* `.xlsx` workbook generation
* Automated worksheet creation
* Transaction grouping
* Debit and credit calculations
* Summary generation

---

## 🏗️ Backend Architecture

The backend follows a service-oriented structure:

```text
                    Controller
                        │
                        ▼
              Excel Reader Service
                        │
                        ▼
            Transaction Parser Service
                        │
                        ▼
          Transaction Grouping Service
                        │
                        ▼
              Excel Writer Service
                        │
                        ▼
             Organized Excel Workbook
```

### Main Components

**ExcelReaderController**

Handles Excel upload and coordinates the transaction processing workflow.

**ExcelReaderService**

Reads and validates the uploaded workbook and converts Excel rows into transaction objects.

**TransactionParserService**

Analyzes transaction descriptions and extracts relevant names or categories.

**TransactionGroupingService**

Groups transactions according to the extracted entity or category.

**ExcelWriterService**

Generates the final workbook containing the Summary sheet and individual transaction worksheets.

---

## 🔐 Data Handling

The application does not require a user account or database for the core statement-processing workflow.

Bank statement data is processed as part of the request and used to generate the resulting workbook.

The application is designed around temporary processing rather than maintaining a permanent transaction database.

---

## 📈 Current Status

### Completed

* [x] Excel file upload
* [x] Excel structure validation
* [x] Transaction extraction
* [x] Transaction parsing
* [x] UPI P2A parsing
* [x] UPI P2M parsing
* [x] NEFT parsing
* [x] IMPS parsing
* [x] MOB / TPFT parsing
* [x] ATM transaction grouping
* [x] Interest classification
* [x] Bank charge classification
* [x] Credit adjustment classification
* [x] Unknown transaction handling
* [x] Transaction grouping
* [x] Debit and credit calculations
* [x] Summary sheet generation
* [x] Individual transaction sheets
* [x] Excel workbook generation
* [x] Automatic Excel download
* [x] Transaction-based quote calculation

### In Progress

* [ ] Payment gateway integration
* [ ] Payment verification
* [ ] Connecting verified payments with Excel processing
* [ ] Production deployment
* [ ] Real-world workflow validation

---

## 🚀 Future Improvements

Future improvements will be driven by real-world usage and accounting workflow requirements.

Potential improvements include:

* Support for additional bank statement formats
* Improved transaction recognition
* More flexible output formats
* Automated payment processing
* Usage-based billing
* Processing history
* Additional accounting-oriented exports

---

## 🎯 Project Goal

The goal of Bank Statement Analyzer is to turn a repetitive manual bank statement organization process into a simple workflow:

```text
Upload
   ↓
Analyze
   ↓
Pay
   ↓
Download
```

The application focuses on automating the repetitive work of identifying, grouping, and organizing transactions while keeping the final output in a familiar Excel format.

```
```
