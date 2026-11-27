export const siteConfig = {
  brandName: 'BankLens',
  registeredBusinessName: import.meta.env.VITE_REGISTERED_BUSINESS_NAME?.trim() || '',
  supportEmail: import.meta.env.VITE_SUPPORT_EMAIL?.trim() || '',
  supportAddress: import.meta.env.VITE_SUPPORT_ADDRESS?.trim() || '',
  supportHours: import.meta.env.VITE_SUPPORT_HOURS?.trim() || '',
};
