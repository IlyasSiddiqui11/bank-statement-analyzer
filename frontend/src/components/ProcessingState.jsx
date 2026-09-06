import React from 'react';
import { Loader2, FileSpreadsheet } from 'lucide-react';

export default function ProcessingState({ fileName }) {
  return (
    <div className="bg-white rounded-2xl border border-emerald-100 p-8 sm:p-12 shadow-sm text-center max-w-2xl mx-auto">
      <div className="relative w-20 h-20 mx-auto mb-6 flex items-center justify-center">
        <div className="absolute inset-0 rounded-full border-4 border-[#DCFCE7] animate-pulse"></div>
        <div className="w-16 h-16 rounded-full bg-[#DCFCE7] flex items-center justify-center text-[#16A34A]">
          <Loader2 className="w-8 h-8 animate-spin text-[#16A34A]" />
        </div>
      </div>

      <h3 className="text-xl font-bold text-[#1F2937] mb-2">
        Analyzing your statement...
      </h3>
      <p className="text-gray-500 text-sm sm:text-base mb-6 max-w-md mx-auto">
        Please wait while we organize your transactions into separate structured sheets.
      </p>

      {fileName && (
        <div className="inline-flex items-center space-x-2 px-4 py-2 bg-gray-50 rounded-lg border border-gray-200 text-xs font-medium text-gray-600">
          <FileSpreadsheet className="w-4 h-4 text-[#16A34A]" />
          <span className="truncate max-w-[200px]">{fileName}</span>
        </div>
      )}
    </div>
  );
}
