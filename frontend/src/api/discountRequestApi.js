import axiosClient from './axiosClient.js'

const discountRequestApi = {
  create: (payload) => axiosClient.post('/invoices/discount-requests', payload),
  list: (params) => axiosClient.get('/invoices/discount-requests', { params }),
  getById: (id) => axiosClient.get(`/invoices/discount-requests/${id}`),
  approve: (id) => axiosClient.post(`/invoices/discount-requests/${id}/approve`),
  reject: (id, rejectReason) =>
    axiosClient.post(`/invoices/discount-requests/${id}/reject`, { rejectReason }),
}

export default discountRequestApi
