import React from 'react';
import { Link } from 'react-router-dom';
import Header from '../components/Header';
import { siteConfig } from '../config/siteConfig';

function LegalLayout({ title, children }) {
  return (
    <div className="min-h-screen bg-[#F7FAF8] text-[#1F2937]">
      <Header />
      <main className="max-w-4xl mx-auto px-4 sm:px-6 py-10 sm:py-14">
        <article className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6 sm:p-10">
          <Link to="/" className="text-sm font-semibold text-emerald-700 hover:text-emerald-800">
            &larr; Back to BankLens
          </Link>
          <h1 className="mt-5 text-3xl sm:text-4xl font-bold tracking-tight">{title}</h1>
          <div className="mt-7 space-y-6 text-sm sm:text-base leading-7 text-gray-700">{children}</div>
        </article>
      </main>
    </div>
  );
}

export function AboutPage() {
  return (
    <LegalLayout title="About Us">
      <p>
        <strong>{siteConfig.brandName}</strong> is an online spreadsheet-processing utility for organizing
        supported bank transaction workbooks. It reads an uploaded Excel statement, identifies valid
        transaction rows, groups entries by person, merchant, or category, and generates a structured
        downloadable Excel workbook.
      </p>
      <p>
        The service is intended to help users review and organize their own transaction data. It is not
        a bank, financial institution, accounting authority, or financial advisory service. Processing
        results may require user review and should not be treated as guaranteed financial reconciliation.
      </p>
      <p>
        The service uses a pay-per-use model of <strong>₹0.25 per valid transaction row</strong>. The
        backend recalculates the applicable total from the processed workbook, and no subscription is required.
        Small totals below the payment provider's supported minimum are not accepted for processing as a
        separate payment order.
      </p>
    </LegalLayout>
  );
}

export function ContactPage() {
  return (
    <LegalLayout title="Contact Us">
      <p>
        {siteConfig.registeredBusinessName
          ? `Registered business / proprietor: ${siteConfig.registeredBusinessName}`
          : 'Registered business information is maintained by the operator of this website.'}
      </p>
      {siteConfig.supportEmail && (
        <p><strong>Customer and payment support:</strong> <a className="text-emerald-700 underline" href={`mailto:${siteConfig.supportEmail}`}>{siteConfig.supportEmail}</a></p>
      )}
      {siteConfig.supportAddress && <p><strong>Business address:</strong> {siteConfig.supportAddress}</p>}
      {siteConfig.supportHours && <p><strong>Support hours:</strong> {siteConfig.supportHours}</p>}
      <p>
        For payment, technical, or refund support, include the relevant statement session reference and
        Cashfree order ID when available. Do not send passwords, card details, bank PINs, or complete
        financial credentials by email.
      </p>
    </LegalLayout>
  );
}

export function TermsPage() {
  return (
    <LegalLayout title="Terms & Conditions">
      <p>These terms govern use of BankLens and the processing of uploaded Excel workbooks.</p>
      <section>
        <h2 className="text-lg font-bold">Service and eligibility</h2>
        <p>Users may upload workbooks they are authorized to process. The service accepts supported .xlsx files and produces an organized Excel workbook. You must comply with applicable law and must not use the service to process unauthorized or unlawful data.</p>
      </section>
      <section>
        <h2 className="text-lg font-bold">Input format and processing</h2>
        <p>The input must contain exactly these six columns in order: Date, Check Number, Particulars, Debit, Credit, and Balance. The software derives a valid transaction count and groups recognized persons, merchants, and categories. Classification and extraction may require review, and the software does not guarantee perfect reconciliation or accuracy.</p>
      </section>
      <section>
        <h2 className="text-lg font-bold">Pricing and payment</h2>
        <p>The price is ₹0.25 per valid transaction row. The backend calculates the total in paise and verifies it before creating a Cashfree order. A payment is considered paid only after the backend verifies a successful provider payment for the matching order and amount. A browser redirect or checkout callback alone does not release a workbook.</p>
      </section>
      <section>
        <h2 className="text-lg font-bold">Downloads and sessions</h2>
        <p>A paid result is bound to its analysis session and is available for a limited time. The processed workbook is released only after successful backend verification and is redeemed once after download. Users are responsible for saving the downloaded file promptly.</p>
      </section>
      <section>
        <h2 className="text-lg font-bold">Acceptable use and prohibited conduct</h2>
        <p>Do not upload malware, encrypted or corrupted workbooks, data you do not have permission to use, or content intended to violate law or another person's rights. Do not attempt to bypass payment verification, manipulate requests, probe the service, or interfere with availability.</p>
      </section>
      <section>
        <h2 className="text-lg font-bold">Availability, intellectual property, and liability</h2>
        <p>The service may be interrupted for maintenance or technical reasons. BankLens software and website materials remain property of their respective owners. To the extent permitted by law, liability is limited to the extent caused by a proven breach of these terms; nothing excludes rights that cannot legally be excluded.</p>
      </section>
      <section>
        <h2 className="text-lg font-bold">Changes and disputes</h2>
        <p>We may update these terms for lawful operation of the service. Continued use after an update is subject to the revised terms. Questions or disputes may be raised through the contact page or the support channel published by the operator.</p>
      </section>
    </LegalLayout>
  );
}

export function PrivacyPage() {
  return (
    <LegalLayout title="Privacy Policy">
      <p>This policy describes the information handled by BankLens and how the current implementation processes it.</p>
      <p><strong>Uploads:</strong> Uploaded Excel files are read and processed to create a temporary output workbook. The original uploaded statement is not stored in PostgreSQL. Temporary workbook bytes and the session record are stored in PostgreSQL for fulfilment and expire or are cleared after redemption or session expiry. The implementation does not claim retention beyond its session lifecycle.</p>
      <p><strong>Payment data:</strong> PostgreSQL stores upload-session and payment metadata, including internal session identifiers, transaction count, amount, currency, Cashfree order/payment identifiers, payment status, timestamps, and download status. Do not send sensitive card or bank credentials to this website.</p>
      <p><strong>Processors:</strong> Cashfree processes payment checkout and payment information. Hosting and database providers may process technical logs and infrastructure data needed to operate the service. Public IP addresses and request metadata may be recorded by hosting infrastructure or application logs.</p>
      <p><strong>Technical information:</strong> The application and hosting providers may process connection information, browser request metadata, operational logs, and error diagnostics. Secrets are not intentionally logged or returned to the browser.</p>
      <p><strong>Security and deletion:</strong> Session workbooks are cleared after redemption or expiry, and payment records remain as operational records. The current implementation does not provide a customer account or customer deletion portal. For privacy requests or concerns, use the published contact page and identify the relevant session or order reference.</p>
    </LegalLayout>
  );
}

export function RefundPage() {
  return (
    <LegalLayout title="Refund & Cancellation Policy">
      <p>A customer may cancel before completing checkout. A failed or pending payment does not release a workbook and does not require a successful-charge refund.</p>
      <p>If a payment is successful but the workbook is not generated or cannot be downloaded because of a technical failure, retain the Cashfree order ID and contact support with the session reference. Duplicate or accidental charges may be reviewed against the payment and download records.</p>
      <p>Refund eligibility is assessed for duplicate charges, failed fulfilment, or other payment-provider-supported exceptions. This policy does not promise an unconditional refund or that Cashfree will automatically issue a refund. Any approved refund must be processed by the operator through the approved payment-provider refund workflow.</p>
      <p>Refund and cancellation requests should include the Cashfree order ID, date, amount, and a concise description of the issue. The operator will confirm the applicable next step and timing during review.</p>
    </LegalLayout>
  );
}
