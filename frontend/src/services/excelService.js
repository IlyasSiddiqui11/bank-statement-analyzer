import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
  || (import.meta.env.PROD ? 'https://bank-lens-backend.onrender.com' : '');
const EXCEL_API = `${API_BASE_URL}/api/excel`;
const PAYMENT_API = `${API_BASE_URL}/api/payment`;
const WORKBOOK_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const getErrorMessage = async (error, fallback) => {
  const body = error.response?.data;
  if (body instanceof Blob) {
    try {
      const parsed = JSON.parse(await body.text());
      return parsed.error || parsed.message || fallback;
    } catch {
      return fallback;
    }
  }
  return body?.error || body?.message || fallback;
};

export const analyzeStatement = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await axios.post(`${EXCEL_API}/analyze`, formData);
    return response.data;
  } catch (error) {
    throw new Error(await getErrorMessage(error, 'Unable to process this spreadsheet. Please try again.'));
  }
};

export const createPaymentOrder = async (sessionToken, customerPhone) => {
  try {
    const response = await axios.post(`${PAYMENT_API}/create-order`, { sessionToken, customerPhone });
    return response.data;
  } catch (error) {
    throw new Error(await getErrorMessage(error, 'Unable to create a payment order. Please try again.'));
  }
};

export const verifyPayment = async ({ sessionToken, orderId }) => {
  try {
    const response = await axios.post(`${PAYMENT_API}/verify`, {
      sessionToken,
      orderId,
    });
    return response.data;
  } catch (error) {
    throw new Error(await getErrorMessage(error, 'Payment could not be verified. Please retry verification.'));
  }
};

export const downloadWorkbook = async (sessionToken) => {
  try {
    const response = await axios.post(
      `${EXCEL_API}/download`,
      { sessionToken },
      { responseType: 'blob' },
    );
    return new Blob([response.data], { type: WORKBOOK_TYPE });
  } catch (error) {
    throw new Error(await getErrorMessage(error, 'Unable to download the workbook. Please try again.'));
  }
};

export const loadCashfreeCheckout = () => {
  if (window.Cashfree) {
    return Promise.resolve();
  }

  return new Promise((resolve, reject) => {
    const existingScript = document.querySelector('script[data-cashfree-checkout]');
    if (existingScript) {
      existingScript.addEventListener('load', resolve, { once: true });
      existingScript.addEventListener('error', () => reject(new Error('Cashfree Checkout could not be loaded.')), {
        once: true,
      });
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://sdk.cashfree.com/js/v3/checkout.js';
    script.async = true;
    script.dataset.cashfreeCheckout = 'true';
    script.onload = resolve;
    script.onerror = () => {
      script.remove();
      reject(new Error('Cashfree Checkout could not be loaded. Check your internet connection and retry.'));
    };
    document.body.appendChild(script);
  });
};
