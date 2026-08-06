import { getDashboardStats } from '../services/mockDataService.js'
import {
  getStoredAuditLogs,
  getStoredInvoices,
  getStoredMedicalRecords,
  getStoredPrescriptions,
} from '../utils/storageHelpers.js'

// Backend chưa cung cấp report controller; các báo cáo được tổng hợp từ dữ liệu FE đã lưu.
const reportApi = {
  summary: async () => ({
    data: {
      records: getStoredMedicalRecords(),
      invoices: getStoredInvoices(),
      prescriptions: getStoredPrescriptions(),
    },
  }),
  timeline: async () => ({ data: [] }),
  topMedicines: async () => ({ data: [] }),
  audit: async () => ({ data: getStoredAuditLogs() }),
  dashboard: async () => ({ data: getDashboardStats() }),
  export: async () => ({ data: new Blob([], { type: 'text/csv' }) }),
}

export default reportApi
