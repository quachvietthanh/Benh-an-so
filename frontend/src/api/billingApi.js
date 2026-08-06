import {
  adjustInvoiceHelper,
  getPayableItems,
  mergeInvoices,
  payEncounterHelper,
} from '../utils/storageHelpers.js'

// Backend hiện không có controller hóa đơn; giữ nghiệp vụ này ở local để FE không gọi URL 404.
const billingApi = {
  getAll: async () => ({ data: mergeInvoices([]) }),
  getPayable: async () => ({ data: getPayableItems() }),
  pay: async (data) => ({ data: payEncounterHelper(data) }),
  adjust: async (_id, data) => {
    const original = mergeInvoices([]).find((invoice) => String(invoice.id) === String(_id))
    if (!original) throw new Error('Không tìm thấy hóa đơn cần điều chỉnh')
    return { data: adjustInvoiceHelper(original, data) }
  },
}

export default billingApi
