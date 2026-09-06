import React, { useState, useRef } from 'react';
import { FileSpreadsheet, UploadCloud, RefreshCw, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

const MAX_FILE_SIZE_MB = 10;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

export default function FileUpload({ file, setFile, onAnalyze, error, setError }) {
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);

  // Format bytes into readable string (e.g. 2.4 MB, 450 KB)
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // Validate selected file
  const validateFile = (selectedFile) => {
    setError('');

    if (!selectedFile) {
      return false;
    }

    // Check extension
    const fileName = selectedFile.name.toLowerCase();
    if (!fileName.endsWith('.xlsx')) {
      setError('Please upload an Excel (.xlsx) file.');
      return false;
    }

    // Check size zero
    if (selectedFile.size === 0) {
      setError('The selected file is empty. Please select a valid Excel file.');
      return false;
    }

    // Check size limit
    if (selectedFile.size > MAX_FILE_SIZE_BYTES) {
      setError(`The selected file is too large. Maximum size is ${MAX_FILE_SIZE_MB} MB.`);
      return false;
    }

    return true;
  };

  const handleFileSelect = (selectedFile) => {
    if (validateFile(selectedFile)) {
      setFile(selectedFile);
    } else {
      setFile(null);
    }
  };

  const handleInputChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      handleFileSelect(selectedFile);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const droppedFile = e.dataTransfer.files[0];
      handleFileSelect(droppedFile);
      e.dataTransfer.clearData();
    }
  };

  const handleRemoveFile = () => {
    setFile(null);
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-200 shadow-md p-6 sm:p-8 max-w-2xl mx-auto transition-all duration-200">
      {/* Hidden native file input */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleInputChange}
        accept=".xlsx"
        className="hidden"
        id="excel-file-input"
      />

      {!file ? (
        /* Drag and drop upload zone */
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-xl p-8 sm:p-12 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center ${
            isDragOver
              ? 'border-[#16A34A] bg-emerald-50/60 scale-[0.99]'
              : 'border-gray-300 hover:border-[#16A34A] bg-[#F7FAF8]/40 hover:bg-[#DCFCE7]/20'
          }`}
        >
          <div className="w-16 h-16 rounded-full bg-[#DCFCE7] flex items-center justify-center text-[#16A34A] mb-4 shadow-2xs">
            <UploadCloud className="w-8 h-8 text-[#16A34A]" />
          </div>

          <p className="text-base font-semibold text-[#1F2937] mb-1">
            Drag & drop your Excel file here
          </p>
          <p className="text-xs text-gray-500 mb-4">
            Supports Microsoft Excel <span className="font-semibold text-gray-700">.xlsx</span> formats up to 10 MB
          </p>

          <div className="flex items-center space-x-3 w-full justify-center">
            <span className="text-xs text-gray-400 font-medium">or</span>
          </div>

          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              fileInputRef.current?.click();
            }}
            className="mt-4 px-5 py-2.5 bg-white border border-gray-300 hover:border-[#16A34A] text-[#1F2937] hover:text-[#16A34A] text-sm font-medium rounded-xl shadow-2xs hover:shadow-xs transition-all duration-150 cursor-pointer"
          >
            Browse File
          </button>
        </div>
      ) : (
        /* Selected file card summary */
        <div className="border border-emerald-200 rounded-xl p-6 bg-[#F7FAF8]/80">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4 min-w-0">
              <div className="w-12 h-12 rounded-xl bg-[#DCFCE7] flex items-center justify-center text-[#16A34A] shrink-0">
                <FileSpreadsheet className="w-6 h-6" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-[#16A34A] uppercase tracking-wider mb-0.5">
                  Selected File
                </p>
                <p className="text-base font-bold text-[#1F2937] truncate">
                  {file.name}
                </p>
                <p className="text-xs text-gray-500 font-mono mt-0.5">
                  {formatFileSize(file.size)}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={handleRemoveFile}
              className="inline-flex items-center space-x-1 px-3 py-1.5 border border-gray-300 hover:border-gray-400 bg-white text-xs font-medium text-gray-700 rounded-lg transition-colors cursor-pointer shrink-0 ml-4"
            >
              <RefreshCw className="w-3.5 h-3.5 text-gray-500" />
              <span>Change File</span>
            </button>
          </div>
        </div>
      )}

      {/* Inline Error Message */}
      {error && (
        <div className="mt-4 p-3.5 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-700 text-xs sm:text-sm animate-shake">
          <AlertCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
          <div className="flex-1">
            <span className="font-semibold block sm:inline">Validation Notice: </span>
            {error}
          </div>
        </div>
      )}

      {/* Primary Action Button */}
      <div className="mt-6">
        <button
          type="button"
          onClick={onAnalyze}
          disabled={!file}
          className={`w-full py-3.5 px-6 rounded-xl font-semibold text-sm sm:text-base flex items-center justify-center space-x-2 transition-all duration-200 ${
            file
              ? 'bg-[#16A34A] hover:bg-[#15803D] text-white shadow-md hover:shadow-lg cursor-pointer transform active:scale-[0.99]'
              : 'bg-gray-100 text-gray-400 border border-gray-200 cursor-not-allowed'
          }`}
        >
          <span>Analyze Statement</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Privacy / Security Note */}
      <div className="mt-6 pt-4 border-t border-gray-100 flex items-center justify-center space-x-2 text-xs text-gray-500">
        <ShieldCheck className="w-4 h-4 text-[#16A34A]" />
        <span>Your statement is processed only to generate the organized workbook.</span>
      </div>
    </div>
  );
}
