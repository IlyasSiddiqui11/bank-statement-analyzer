
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

| Date       | Cheque Number | Particulars | Debit | Credit | Balance |
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

The first row containing recognizable transaction headers must contain the following six columns in the order shown. Approved bank header variations are accepted. Do not add, remove, or reorder columns. Empty or unrelated rows before the header are ignored.

| Column        | Description                       | Example       |
| ------------- | --------------------------------- | ------------- |
| `Date`        | Transaction date                  | `01/04/2025`  |
| `Cheque Number`| Cheque / reference number        | `100234`      |
| `Particulars` | Transaction description           | `UPI/P2M/...` |
| `Debit`       | Outgoing transaction amount       | `1,500.00`    |
| `Credit`      | Incoming transaction amount       | `75,000.00`   |
| `Balance`     | Account balance after transaction | `45,250.00`   |

Common header variations such as `Tran Date`, `Chq No`, `Narration`, `Withdrawal`, and `Deposit` are accepted in their respective positions. Transactions begin in the row immediately after the header. The reader then reads values from columns A through F. Blank Cheque Number, Debit, and Credit cells are allowed.

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

## Payment and Pricing

The price is **₹0.25 (25 paise) per valid transaction row**. The backend parses the uploaded workbook, calculates the count and total in paise, creates the Cashfree order, verifies the authoritative Cashfree order/payment status for the expected amount, and then permits a workbook download.

```text
Upload .xlsx → Analyze → Review quote → Cashfree Checkout
             → Server verifies successful provider payment → Download workbook
```

The legacy `/api/excel/read` free-download endpoint is disabled. A payment session is tied to one analyzed statement and expires after 30 minutes. The workbook is redeemed once after successful captured-payment verification.

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

## Data Handling and Session Limitations

The application remains login-free. Upload sessions, payments, download records and webhook events are persisted in Neon PostgreSQL or another PostgreSQL database. Generated workbook bytes are stored temporarily in the upload-session record and deleted after redemption or expiry. Original uploaded statements are not retained.

Flyway migrations create the database schema. Use the `dev` profile locally and the `prod` profile on Render. Production uses Hibernate validation rather than automatic schema creation.

## Run Locally

Use Java 17 and Node.js. In separate terminals:

```powershell
cd bank-statement-analyzer
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm ci
npm run dev
```

The Vite development server runs on `http://localhost:3000` and proxies `/api` requests to `http://localhost:8080`.

Set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `CASHFREE_APP_ID`, `CASHFREE_SECRET_KEY`, `CASHFREE_ENV`, `CASHFREE_API_VERSION`, `FRONTEND_URL`, `FRONTEND_ORIGIN`, and `BACKEND_URL` in the backend `.env` file. `.env` is ignored by Git. Never put the Cashfree secret in a frontend variable.

## Render Deployment Checklist

### Backend Web Service

- Root directory: `bank-statement-analyzer`
- Build command: `./mvnw clean package`
- Start command: `java -jar target/bank-statement-analyzer-0.0.1-SNAPSHOT.jar`
- Environment: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `CASHFREE_APP_ID`, `CASHFREE_SECRET_KEY`, `CASHFREE_ENV`, `CASHFREE_API_VERSION`, `FRONTEND_URL`, `FRONTEND_ORIGIN`, and `BACKEND_URL`.
- Use Cashfree **Sandbox** credentials for testing. Switch to Production only after configuring the live account, return URL, webhook URL, session persistence, and payment reconciliation.

### Frontend Static Site

- Root directory: `frontend`
- Build command: `npm ci && npm run build`
- Publish directory: `dist`
- `VITE_API_BASE_URL` defaults to `https://bank-lens-backend.onrender.com`; set it as a build environment variable only when using a different backend URL.

The backend persists sessions and payments in PostgreSQL and uses the Cashfree payment API for authoritative verification. Configure the Cashfree webhook for the `/api/webhooks/cashfree` endpoint.

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

* [x] Cashfree Checkout and authoritative server-side payment verification
* [x] Paid-session workbook download
* [x] Persistent shared session/payment storage
* [ ] Neon connection and migration verification
* [ ] Cashfree sandbox/live order verification
* [ ] Webhook delivery verification
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
