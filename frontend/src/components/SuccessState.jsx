import React from 'react';
import { CheckCircle2, ArrowLeft, Download, Loader2, AlertTriangle } from 'lucide-react';

export default function SuccessState({
  onReset,
  onDownload,
  isDownloading,
  hasDownloaded,
  transactionCount,
  amount,
  errorMessage,
}) {
  return (
    <section className="bg-white rounded-2xl border border-emerald-200 p-8 sm:p-12 shadow-sm text-center max-w-2xl mx-auto">
      <div className="w-16 h-16 rounded-full bg-[#DCFCE7] flex items-center justify-center mx-auto mb-6 text-[#16A34A]">
        <CheckCircle2 className="w-10 h-10" />
      </div>
      <h2 className="text-2xl font-bold text-[#1F2937] mb-2">Payment verified</h2>
      <p className="text-gray-600 text-sm sm:text-base mb-6 max-w-md mx-auto">
        Your payment was confirmed by the server. Your BankLens workbook is ready to download.
      </p>

      <div className="bg-[#F7FAF8] border border-emerald-100 rounded-xl p-4 mb-6 text-left max-w-md mx-auto space-y-3">
        <div className="flex justify-between gap-4 text-sm">
          <span className="text-gray-500">Transactions</span>
          <strong>{transactionCount?.toLocaleString('en-IN')}</strong>
        </div>
        <div className="flex justify-between gap-4 text-sm">
          <span className="text-gray-500">Amount paid</span>
          <strong>{amount}</strong>
        </div>
        <div className="flex justify-between gap-4 text-sm">
          <span className="text-gray-500">Workbook</span>
          <strong>Organized_Statement.xlsx</strong>
        </div>
      </div>

      {errorMessage && (
        <div className="mb-5 p-3 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-800 flex items-start gap-2 text-left">
          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{errorMessage}</span>
        </div>
      )}

      {hasDownloaded && !errorMessage && (
        <p className="mb-5 text-sm text-emerald-700">Download started. You can download this workbook again during this session.</p>
      )}

      <button
        type="button"
        onClick={onDownload}
        disabled={isDownloading}
        className="w-full max-w-md mx-auto mb-4 inline-flex items-center justify-center space-x-2 px-6 py-3 bg-[#16A34A] hover:bg-[#15803D] disabled:bg-gray-300 text-white font-semibold text-sm rounded-xl transition-colors"
      >
        {isDownloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
        <span>{isDownloading ? 'Preparing workbook…' : hasDownloaded ? 'Download again' : 'Download organized workbook'}</span>
      </button>

      <div>
        <button
          type="button"
          onClick={onReset}
          className="inline-flex items-center justify-center space-x-2 px-5 py-2.5 text-gray-600 hover:text-gray-900 font-medium text-sm rounded-xl"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Analyze another statement</span>
        </button>
      </div>
    </section>
  );
}
