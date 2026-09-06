import React from 'react';
import { Table, CheckCircle, Info } from 'lucide-react';

export default function ExcelFormatPreview() {
  const sampleData = [
    { tranDate: '01/04/2024', chqNo: '-', particulars: 'UPI/4092182/TRANSFER TO ALICE', debit: '1,500.00', credit: '-', balance: '45,250.00' },
    { tranDate: '03/04/2024', chqNo: '100234', particulars: 'SALARY CREDIT - TECH CORP', debit: '-', credit: '75,000.00', balance: '120,250.00' },
    { tranDate: '05/04/2024', chqNo: '-', particulars: 'ATM WITHDRAWAL ICICI BANK', debit: '2,000.00', credit: '-', balance: '118,250.00' },
    { tranDate: '08/04/2024', chqNo: '-', particulars: 'ELECTRICITY BILL PAYMENT', debit: '850.00', credit: '-', balance: '117,400.00' }
  ];

  return (
    <section className="mt-16 max-w-4xl mx-auto">
      <div className="text-center mb-6">
        <div className="inline-flex items-center space-x-2 px-3 py-1 bg-emerald-50 border border-emerald-200 rounded-full text-xs font-semibold text-[#15803D] mb-2">
          <Info className="w-3.5 h-3.5" />
          <span>Excel Template Guide</span>
        </div>
        <h2 className="text-2xl font-bold text-[#1F2937]">Required Excel Format</h2>
        <p className="text-gray-500 text-sm mt-1">
          Your input bank statement spreadsheet should contain the exact 6 columns shown below:
        </p>
      </div>

      {/* Spreadsheet Mockup Container */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-xs overflow-hidden">
        {/* Mock Spreadsheet Header Toolbar */}
        <div className="bg-gray-50 border-b border-gray-200 px-4 py-2.5 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-red-400"></div>
            <div className="w-3 h-3 rounded-full bg-yellow-400"></div>
            <div className="w-3 h-3 rounded-full bg-green-400"></div>
            <span className="text-xs font-medium text-gray-500 ml-2 flex items-center gap-1">
              <Table className="w-3.5 h-3.5 text-[#16A34A]" /> bank_statement_input.xlsx
            </span>
          </div>
          <span className="text-xs text-[#16A34A] font-semibold flex items-center gap-1 bg-[#DCFCE7] px-2.5 py-0.5 rounded-md">
            <CheckCircle className="w-3 h-3" /> Exact Column Match
          </span>
        </div>

        {/* Scrollable Spreadsheet Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs sm:text-sm font-mono border-collapse">
            <thead>
              <tr className="bg-[#DCFCE7]/60 text-[#15803D] border-b border-emerald-200 font-sans">
                <th className="py-3 px-4 font-bold border-r border-emerald-200/50">
                  <div className="flex items-center space-x-1">
                    <span>Tran Date</span>
                  </div>
                </th>
                <th className="py-3 px-4 font-bold border-r border-emerald-200/50">
                  <div className="flex items-center space-x-1">
                    <span>Chq No</span>
                  </div>
                </th>
                <th className="py-3 px-4 font-bold border-r border-emerald-200/50">
                  <div className="flex items-center space-x-1">
                    <span>Particulars</span>
                  </div>
                </th>
                <th className="py-3 px-4 font-bold border-r border-emerald-200/50 text-right">
                  <div className="flex items-center justify-end space-x-1">
                    <span>Debit</span>
                  </div>
                </th>
                <th className="py-3 px-4 font-bold border-r border-emerald-200/50 text-right">
                  <div className="flex items-center justify-end space-x-1">
                    <span>Credit</span>
                  </div>
                </th>
                <th className="py-3 px-4 font-bold text-right">
                  <div className="flex items-center justify-end space-x-1">
                    <span>Balance</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 font-sans text-gray-700">
              {sampleData.map((row, idx) => (
                <tr key={idx} className={idx % 2 === 0 ? 'bg-white' : 'bg-[#F7FAF8]/50'}>
                  <td className="py-2.5 px-4 font-mono text-gray-600 border-r border-gray-100 whitespace-nowrap">{row.tranDate}</td>
                  <td className="py-2.5 px-4 text-gray-500 border-r border-gray-100 whitespace-nowrap">{row.chqNo}</td>
                  <td className="py-2.5 px-4 font-medium text-[#1F2937] border-r border-gray-100">{row.particulars}</td>
                  <td className="py-2.5 px-4 text-right text-red-600 font-mono border-r border-gray-100 whitespace-nowrap">{row.debit}</td>
                  <td className="py-2.5 px-4 text-right text-emerald-700 font-mono border-r border-gray-100 whitespace-nowrap">{row.credit}</td>
                  <td className="py-2.5 px-4 text-right text-gray-900 font-mono font-medium whitespace-nowrap">{row.balance}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <p className="text-center text-xs text-gray-500 mt-4">
        Please upload an Excel file matching the format shown above.
      </p>
    </section>
  );
}
