import axiosClient from './axiosClient'

const medicineApi = {
  search: (params = {}) => axiosClient.get('/medicines', { params }),

  getById: (id) => axiosClient.get(`/medicines/${id}`),

  create: (data) => axiosClient.post('/medicines', data),

  update: (id, data) => axiosClient.put(`/medicines/${id}`, data),

  updateStatus: (id, active) =>
    axiosClient.patch(`/medicines/${id}/status`, { active }),
}

export default medicineApi
