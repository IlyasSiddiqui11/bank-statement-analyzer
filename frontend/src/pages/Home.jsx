import React, { useState } from 'react';
import Header from '../components/Header';
import FileUpload from '../components/FileUpload';
import ProcessingState from '../components/ProcessingState';
import SuccessState from '../components/SuccessState';
import ExcelFormatPreview from '../components/ExcelFormatPreview';
import HowItWorks from '../components/HowItWorks';
import { analyzeStatement } from '../services/excelService';
import { saveAs } from 'file-saver';
import { AlertTriangle, RefreshCw, Sparkles } from 'lucide-react';

export default function Home() {
  const [file, setFile] = useState(null);
  const [generatedBlob, setGeneratedBlob] = useState(null);
  const [status, setStatus] = useState('idle'); // 'idle' | 'processing' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');
  const [fileValidationError, setFileValidationError] = useState('');

  // Trigger analysis and download
  const handleAnalyze = async () => {
    if (!file) {
      setFileValidationError('Please select an Excel file before analyzing.');
      return;
    }

    setStatus('processing');
    setErrorMessage('');

    try {
      // 1. Call API service to process statement
      const blobData = await analyzeStatement(file);

      // 2. Create explicit XLSX blob
      const excelBlob = new Blob([blobData], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });

      // 3. Prompt user with Windows native "Save As" file picker
      if ('showSaveFilePicker' in window) {
        try {
          const handle = await window.showSaveFilePicker({
            suggestedName: 'Organized_Statement.xlsx',
            types: [
              {
                description: 'Excel Workbook (*.xlsx)',
                accept: {
                  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
                },
              },
            ],
          });

          const writable = await handle.createWritable();
          await writable.write(excelBlob);
          await writable.close();
        } catch (pickerErr) {
          // If user cancels the file dialog, abort gracefully
          if (pickerErr.name === 'AbortError') {
            setStatus('idle');
            return;
          }
          saveAs(excelBlob, 'Organized_Statement.xlsx');
        }
      } else {
        saveAs(excelBlob, 'Organized_Statement.xlsx');
      }

      // 4. Update status to success
      setStatus('success');
    } catch (err) {
      console.error('Error analyzing bank statement:', err);
      setErrorMessage(
        err.message ||
          'Unable to process this file. Please make sure your Excel file matches the required format.'
      );
      setStatus('error');
    }
  };

  // Reset page to analyze another file
  const handleReset = () => {
    setFile(null);
    setGeneratedBlob(null);
    setStatus('idle');
    setErrorMessage('');
    setFileValidationError('');
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#F7FAF8] font-sans antialiased text-[#1F2937]">
      {/* Header / Navbar */}
      <Header />

      {/* Main Container */}
      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14 w-full">
        {/* HERO SECTION */}
        <div className="text-center max-w-3xl mx-auto mb-10">
          <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 bg-[#DCFCE7] border border-emerald-200 rounded-full text-xs font-bold text-[#15803D] mb-4 shadow-2xs">
            <Sparkles className="w-3.5 h-3.5 text-[#16A34A]" />
            <span>Simple • Fast • Organized</span>
          </div>

          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-[#1F2937] tracking-tight leading-tight mb-4">
            Organize Your Bank Statement
          </h1>

          <p className="text-gray-600 text-base sm:text-lg leading-relaxed max-w-2xl mx-auto">
            Upload your Excel statement and automatically organize your transactions into a clean, structured workbook.
          </p>
        </div>

        {/* INTERACTION AREA */}
        <div className="mb-14">
          {status === 'processing' && (
            <ProcessingState fileName={file?.name} />
          )}

          {status === 'success' && (
            <SuccessState
              onReset={handleReset}
              excelBlob={generatedBlob}
              fileName={file?.name}
            />
          )}

          {(status === 'idle' || status === 'error') && (
            <>
              {/* Server Error Alert Banner */}
              {status === 'error' && (
                <div className="max-w-2xl mx-auto mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-3 text-red-800 shadow-2xs animate-fade-in">
                  <AlertTriangle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
                  <div className="flex-1 text-sm">
                    <p className="font-bold mb-1">Processing Failed</p>
                    <p className="text-red-700">{errorMessage}</p>
                    <button
                      onClick={() => setStatus('idle')}
                      className="mt-3 inline-flex items-center space-x-1.5 text-xs font-semibold px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors cursor-pointer"
                    >
                      <RefreshCw className="w-3 h-3" />
                      <span>Try Again</span>
                    </button>
                  </div>
                </div>
              )}

              {/* Upload Card Component */}
              <FileUpload
                file={file}
                setFile={setFile}
                onAnalyze={handleAnalyze}
                error={fileValidationError}
                setError={setFileValidationError}
              />
            </>
          )}
        </div>

        {/* REQUIRED EXCEL FORMAT PREVIEW */}
        <ExcelFormatPreview />

        {/* HOW IT WORKS SECTION */}
        <HowItWorks />
      </main>

      {/* FOOTER */}
      <footer className="bg-white border-t border-gray-200 py-8 text-center mt-auto">
        <div className="max-w-5xl mx-auto px-4">
          <p className="text-sm font-semibold text-[#1F2937]">Bank Statement Analyzer</p>
          <p className="text-xs text-gray-500 mt-1">
            Organize your financial statements with ease.
          </p>
          <p className="text-xs text-gray-400 mt-4">
            &copy; {new Date().getFullYear()} Bank Statement Analyzer. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
