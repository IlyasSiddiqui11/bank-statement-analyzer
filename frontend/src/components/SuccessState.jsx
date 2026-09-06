import React from 'react';
import { CheckCircle2, ArrowLeft, Download } from 'lucide-react';

export default function SuccessState({ onReset }) {
  return (
    <div className="bg-white rounded-2xl border border-emerald-200 p-8 sm:p-12 shadow-sm text-center max-w-2xl mx-auto animate-fade-in">
      {/* Green Check Icon */}
      <div className="w-16 h-16 rounded-full bg-[#DCFCE7] flex items-center justify-center mx-auto mb-6 text-[#16A34A] shadow-2xs">
        <CheckCircle2 className="w-10 h-10" />
      </div>

      {/* Heading & Subtext */}
      <h3 className="text-2xl font-bold text-[#1F2937] mb-2">
        Statement Organized Successfully
      </h3>
      <p className="text-gray-600 text-sm sm:text-base mb-6 max-w-md mx-auto">
        Your organized Excel workbook has been downloaded.
      </p>

      {/* Success Details Box */}
      <div className="bg-[#F7FAF8] border border-emerald-100 rounded-xl p-4 mb-8 text-left max-w-md mx-auto flex items-center justify-between shadow-2xs">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center text-[#16A34A]">
            <Download className="w-4 h-4" />
          </div>
          <div>
            <p className="text-xs text-gray-500 font-medium">Downloaded File</p>
            <p className="text-sm font-semibold text-[#1F2937]">Organized_Statement.xlsx</p>
          </div>
        </div>
        <span className="text-xs bg-[#DCFCE7] text-[#15803D] font-semibold px-2.5 py-1 rounded-full">
          Ready
        </span>
      </div>

      {/* Action Button */}
      <button
        type="button"
        onClick={onReset}
        className="inline-flex items-center justify-center space-x-2 px-6 py-3 bg-[#16A34A] hover:bg-[#15803D] text-white font-medium text-sm rounded-xl transition-all duration-200 shadow-sm hover:shadow-md focus:outline-none focus:ring-2 focus:ring-[#16A34A] focus:ring-offset-2 cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Analyze Another File</span>
      </button>
    </div>
  );
}
