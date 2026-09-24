import axiosClient from './axiosClient.js'

const cashierShiftApi = {
  getCurrentSummary: () => axiosClient.get('/cashier-shifts/current-summary'),

  getCurrentShift: () => axiosClient.get('/cashier-shifts/current-summary'),

  closeShift: (payloadOrAmount) => {
    const payload = typeof payloadOrAmount === 'object' && payloadOrAmount !== null
      ? payloadOrAmount
      : { actualCashAmount: Number(payloadOrAmount) || 0 }
    return axiosClient.post('/cashier-shifts/close', payload)
  },

  confirmShift: (id, payload = {}) => axiosClient.post(`/cashier-shifts/${id}/confirm`, payload),

  search: (params) => axiosClient.get('/cashier-shifts', { params }),

  getById: (id) => axiosClient.get(`/cashier-shifts/${id}`),
}

export default cashierShiftApi
