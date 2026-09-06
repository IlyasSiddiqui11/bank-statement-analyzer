import React from 'react';
import { UploadCloud, Cpu, Download } from 'lucide-react';

export default function HowItWorks() {
  const steps = [
    {
      step: '1',
      title: 'Upload',
      description: 'Select your bank statement Excel file.',
      icon: UploadCloud,
    },
    {
      step: '2',
      title: 'Analyze',
      description: 'Our system organizes your transactions automatically.',
      icon: Cpu,
    },
    {
      step: '3',
      title: 'Download',
      description: 'Download your organized Excel workbook.',
      icon: Download,
    },
  ];

  return (
    <section id="how-it-works" className="mt-20 py-12 border-t border-gray-200">
      <div className="max-w-4xl mx-auto px-4">
        <div className="text-center mb-12">
          <h2 className="text-2xl sm:text-3xl font-bold text-[#1F2937]">How It Works</h2>
          <p className="text-gray-500 text-sm sm:text-base mt-2">
            Organize complex bank statements into structured sheets in 3 simple steps.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
          {steps.map((item, index) => {
            const Icon = item.icon;
            return (
              <div
                key={index}
                className="bg-white rounded-2xl border border-gray-200/80 p-6 text-center relative shadow-xs hover:shadow-md transition-shadow duration-200 flex flex-col items-center"
              >
                {/* Step badge */}
                <div className="w-7 h-7 rounded-full bg-[#16A34A] text-white font-bold text-xs flex items-center justify-center mb-4">
                  {item.step}
                </div>

                {/* Step Icon */}
                <div className="w-14 h-14 rounded-2xl bg-[#DCFCE7] flex items-center justify-center text-[#16A34A] mb-4">
                  <Icon className="w-7 h-7" />
                </div>

                <h3 className="text-lg font-bold text-[#1F2937] mb-2">{item.title}</h3>
                <p className="text-gray-500 text-sm leading-relaxed">{item.description}</p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
