import React from 'react';
import { Landmark } from 'lucide-react';

export default function Header() {
  const scrollToHowItWorks = (e) => {
    e.preventDefault();
    const element = document.getElementById('how-it-works');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <header className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-gray-200 shadow-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo & Name */}
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-full bg-[#16A34A] flex items-center justify-center text-white shadow-sm">
            <Landmark className="w-5 h-5" />
          </div>
          <span className="text-xl font-bold text-[#1F2937] tracking-tight">
            Bank Statement <span className="text-[#16A34A]">Analyzer</span>
          </span>
        </div>

        {/* Right Navigation */}
        <nav className="flex items-center space-x-6">
          <a
            href="#how-it-works"
            onClick={scrollToHowItWorks}
            className="text-sm font-medium text-gray-600 hover:text-[#16A34A] transition-colors focus:outline-none focus:ring-2 focus:ring-[#16A34A] focus:ring-offset-2 rounded-md px-2 py-1"
          >
            How it works
          </a>
        </nav>
      </div>
    </header>
  );
}
