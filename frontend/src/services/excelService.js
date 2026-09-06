import axios from 'axios';

// Configurable API Base URL from environment variables with fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

/**
 * Uploads bank statement Excel file to backend and receives organized workbook blob.
 * @param {File} file - Excel (.xlsx) file object
 * @returns {Promise<Blob>} - Generated Excel blob
 */
export const analyzeStatement = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const endpoint = `${API_BASE_URL}/api/excel/read`;

  try {
    const response = await axios.post(endpoint, formData, {
      responseType: 'blob',
    });

    return response.data;
  } catch (error) {
    // If backend returns error response in Blob format, try reading text from blob
    if (error.response && error.response.data instanceof Blob) {
      try {
        const errorText = await error.response.data.text();
        try {
          const parsed = JSON.parse(errorText);
          throw new Error(parsed.message || parsed.error || 'Failed to process bank statement.');
        } catch {
          if (errorText && errorText.trim().length > 0) {
            throw new Error(errorText);
          }
        }
      } catch (e) {
        if (e.message && e.message !== 'Failed to process bank statement.') {
          throw e;
        }
      }
    }

    if (error.code === 'ERR_NETWORK') {
      throw new Error('Unable to connect to the server. Please check your backend connection.');
    }

    throw new Error(
      error.response?.status === 400
        ? 'Unable to process this file. Please make sure your Excel file matches the required format.'
        : 'An error occurred while processing your bank statement. Please try again.'
    );
  }
};
