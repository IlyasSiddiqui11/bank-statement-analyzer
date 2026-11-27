import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Home from './pages/Home';
import {
  AboutPage,
  ContactPage,
  PrivacyPage,
  RefundPage,
  TermsPage,
} from './pages/LegalPages';
import PaymentCompletePage from './pages/PaymentCompletePage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/refund-policy" element={<RefundPage />} />
        <Route path="/terms" element={<TermsPage />} />
        <Route path="/payment/complete" element={<PaymentCompletePage />} />
        <Route path="*" element={<Home />} />
      </Routes>
    </BrowserRouter>
  );
}
