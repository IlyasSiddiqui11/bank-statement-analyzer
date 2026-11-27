import React, { useRef, useState } from 'react';
import Header from '../components/Header';
import FileUpload from '../components/FileUpload';
import ProcessingState from '../components/ProcessingState';
import SuccessState from '../components/SuccessState';
import ExcelFormatPreview from '../components/ExcelFormatPreview';
import HowItWorks from '../components/HowItWorks';
import {
  analyzeStatement,
  createPaymentOrder,
  downloadWorkbook,
  loadRazorpayCheckout,
  verifyPayment,
} from '../services/excelService';
import { saveAs } from 'file-saver';
import { AlertTriangle, ArrowRight, RefreshCw, Sparkles } from 'lucide-react';

const formatRupees = (paise) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(paise / 100);

export default function Home() {
  const [file, setFile] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [status, setStatus] = useState('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const [fileValidationError, setFileValidationError] = useState('');
  const [pendingVerification, setPendingVerification] = useState(null);
  const [hasDownloaded, setHasDownloaded] = useState(false);
  const checkoutResolved = useRef(false);

  const handleAnalyze = async () => {
    if (!file) {
      setFileValidationError('Please select an Excel file before analyzing.');
      return;
    }

    setStatus('analyzing');
    setErrorMessage('');
    setAnalysis(null);
    setPendingVerification(null);

    try {
      const result = await analyzeStatement(file);
      setAnalysis(result);
      setStatus('ready');
    } catch (error) {
      setErrorMessage(error.message);
      setStatus('idle');
    }
  };

  const verifyCheckoutPayment = async (paymentDetails) => {
    setPendingVerification(paymentDetails);
    setStatus('verifying');
    setErrorMessage('');
    try {
      const result = await verifyPayment(paymentDetails);
      if (result.verified !== true) {
        throw new Error('The server has not confirmed this payment yet.');
      }
      setPendingVerification(null);
      setStatus('paid');
    } catch (error) {
      setErrorMessage(error.message);
      setStatus('ready');
    }
  };

  const handleRetryVerification = () => {
    if (pendingVerification) {
      verifyCheckoutPayment(pendingVerification);
    }
  };

  const handlePayment = async () => {
    if (!analysis || pendingVerification) {
      return;
    }

    setStatus('paying');
    setErrorMessage('');
    try {
      const order = await createPaymentOrder(analysis.sessionToken);
      if (order.amountPaise !== analysis.totalAmountPaise) {
        throw new Error('The server returned a payment amount that does not match the quote.');
      }
      await loadRazorpayCheckout();

      checkoutResolved.current = false;
      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amountPaise,
        currency: order.currency,
        name: 'Bank Statement Analyzer',
        description: `${analysis.transactionCount} transactions`,
        order_id: order.orderId,
        handler: (response) => {
          checkoutResolved.current = true;
          verifyCheckoutPayment({
            sessionToken: analysis.sessionToken,
            orderId: response.razorpay_order_id,
            paymentId: response.razorpay_payment_id,
            signature: response.razorpay_signature,
          });
        },
        modal: {
          ondismiss: () => {
            if (!checkoutResolved.current) {
              setStatus('ready');
              setErrorMessage('Payment was cancelled. You can try again when ready.');
            }
          },
        },
        theme: { color: '#16A34A' },
      });

      checkout.on('payment.failed', () => {
        checkoutResolved.current = true;
        setStatus('ready');
        setErrorMessage('Payment failed. No workbook was released. Please try again.');
      });

      setStatus('checkout');
      checkout.open();
    } catch (error) {
      setErrorMessage(error.message || 'Unable to start checkout. Please try again.');
      setStatus('ready');
    }
  };

  const handleDownload = async () => {
    if (!analysis) {
      return;
    }
    setStatus('downloading');
    setErrorMessage('');
    try {
      const workbook = await downloadWorkbook(analysis.sessionToken);
      saveAs(workbook, 'Organized_Statement.xlsx');
      setHasDownloaded(true);
      setStatus('paid');
    } catch (error) {
      setErrorMessage(error.message);
      setStatus('paid');
    }
  };

  const handleReset = () => {
    setFile(null);
    setAnalysis(null);
    setStatus('idle');
    setErrorMessage('');
    setFileValidationError('');
    setPendingVerification(null);
    setHasDownloaded(false);
  };

  const isProcessing = ['analyzing', 'paying', 'checkout', 'verifying', 'downloading'].includes(status);
  const processingMessage = {
    analyzing: 'Checking the workbook and counting valid transactions.',
    paying: 'Preparing your secure payment.',
    checkout: 'Complete payment in the Razorpay window.',
    verifying: 'Confirming payment securely with the server.',
    downloading: 'Preparing your organized workbook.',
  }[status];

  return (
    <div className="min-h-screen flex flex-col bg-[#F7FAF8] font-sans antialiased text-[#1F2937]">
      <Header />

      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14 w-full">
        <div className="text-center max-w-3xl mx-auto mb-10">
          <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 bg-[#DCFCE7] border border-emerald-200 rounded-full text-xs font-bold text-[#15803D] mb-4 shadow-2xs">
            <Sparkles className="w-3.5 h-3.5 text-[#16A34A]" />
            <span>Simple · Secure · Pay per transaction</span>
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-[#1F2937] tracking-tight leading-tight mb-4">
            Organize Your Bank Statement
          </h1>
          <p className="text-gray-600 text-base sm:text-lg leading-relaxed max-w-2xl mx-auto">
            Upload an Excel statement, review the transaction-based price, and pay securely before downloading.
          </p>
        </div>

        <div className="mb-14">
          {isProcessing && <ProcessingState fileName={file?.name} message={processingMessage} />}

          {status === 'paid' && (
            <SuccessState
              onReset={handleReset}
              onDownload={handleDownload}
              isDownloading={false}
              hasDownloaded={hasDownloaded}
              transactionCount={analysis?.transactionCount}
              amount={formatRupees(analysis?.totalAmountPaise || 0)}
              errorMessage={errorMessage}
            />
          )}

          {!isProcessing && status !== 'paid' && (
            <>
              {errorMessage && (
                <div className="max-w-2xl mx-auto mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-3 text-red-800 shadow-2xs">
                  <AlertTriangle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
                  <div className="flex-1 text-sm">
                    <p className="font-bold mb-1">Payment or processing needs attention</p>
                    <p className="text-red-700">{errorMessage}</p>
                    {pendingVerification && (
                      <button
                        type="button"
                        onClick={handleRetryVerification}
                        className="mt-3 inline-flex items-center space-x-2 text-xs font-semibold px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-lg"
                      >
                        <RefreshCw className="w-3 h-3" />
                        <span>Retry payment verification</span>
                      </button>
                    )}
                  </div>
                </div>
              )}

              {analysis ? (
                <section className="bg-white rounded-2xl border border-gray-200 shadow-md p-6 sm:p-8 max-w-2xl mx-auto">
                  <p className="text-sm font-semibold text-emerald-700 uppercase tracking-wide mb-2">Statement analyzed</p>
                  <h2 className="text-xl font-bold break-all">{file?.name}</h2>
                  <div className="mt-6 rounded-xl border border-emerald-100 bg-[#F7FAF8] p-5 space-y-3">
                    <div className="flex justify-between gap-4 text-sm">
                      <span className="text-gray-600">Valid transactions</span>
                      <strong>{analysis.transactionCount.toLocaleString('en-IN')}</strong>
                    </div>
                    <div className="flex justify-between gap-4 text-sm">
                      <span className="text-gray-600">Price per transaction</span>
                      <strong>{formatRupees(analysis.pricePerTransactionPaise)}</strong>
                    </div>
                    <div className="border-t border-emerald-100 pt-3 flex justify-between gap-4">
                      <span className="font-semibold">Total payable</span>
                      <strong className="text-xl text-emerald-700">{formatRupees(analysis.totalAmountPaise)}</strong>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={handlePayment}
                    disabled={Boolean(pendingVerification)}
                    className="mt-6 w-full py-3.5 px-6 rounded-xl font-semibold text-sm sm:text-base flex items-center justify-center space-x-2 bg-[#16A34A] hover:bg-[#15803D] disabled:bg-gray-300 text-white transition-colors"
                  >
                    <span>{pendingVerification ? 'Verify your previous payment first' : 'Pay securely with Razorpay'}</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                  <p className="mt-4 text-xs text-center text-gray-500">
                    Your workbook is released only after the server verifies the captured payment.
                  </p>
                </section>
              ) : (
                <FileUpload
                  file={file}
                  setFile={setFile}
                  onAnalyze={handleAnalyze}
                  error={fileValidationError}
                  setError={setFileValidationError}
                />
              )}
            </>
          )}
        </div>

        <ExcelFormatPreview />
        <HowItWorks />
      </main>

      <footer className="bg-white border-t border-gray-200 py-8 text-center mt-auto">
        <div className="max-w-5xl mx-auto px-4">
          <p className="text-sm font-semibold text-[#1F2937]">Bank Statement Analyzer</p>
          <p className="text-xs text-gray-500 mt-1">Organize your financial statements with ease.</p>
          <p className="text-xs text-gray-400 mt-4">
            &copy; {new Date().getFullYear()} Bank Statement Analyzer. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
