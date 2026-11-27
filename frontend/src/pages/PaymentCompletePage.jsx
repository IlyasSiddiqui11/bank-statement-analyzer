import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Header from '../components/Header';
import { verifyPayment } from '../services/excelService';

export default function PaymentCompletePage() {
  const [params] = useSearchParams();
  const [message, setMessage] = useState('Verifying your payment with the server...');
  const [verified, setVerified] = useState(false);

  useEffect(() => {
    const orderId = params.get('order_id');
    const sessionToken = window.sessionStorage.getItem('banklens.sessionToken');
    if (!orderId || !sessionToken) {
      setMessage('This return link is missing its payment reference or statement session.');
      return;
    }
    verifyPayment({ sessionToken, orderId })
      .then(() => {
        setVerified(true);
        setMessage('Payment verified. Return to the statement page to download the workbook.');
      })
      .catch((error) => setMessage(error.message));
  }, [params]);

  return (
    <div className="min-h-screen bg-[#F7FAF8] text-[#1F2937]">
      <Header />
      <main className="max-w-xl mx-auto px-4 py-16 text-center">
        <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm">
          <h1 className="text-2xl font-bold">{verified ? 'Payment confirmed' : 'Payment status'}</h1>
          <p className="mt-4 text-gray-600">{message}</p>
          <Link to="/" className="inline-flex mt-6 px-5 py-3 rounded-xl bg-emerald-600 text-white font-semibold">
            Return to BankLens
          </Link>
        </div>
      </main>
    </div>
  );
}
