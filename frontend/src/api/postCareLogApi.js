import axiosClient from './axiosClient.js'

const postCareLogApi = {
  search: (params = {}) => axiosClient.get('/care-logs', { params }),

  getForPatient: (patientId) => axiosClient.get(`/care-logs/patient/${patientId}`),

  create: (payload) => axiosClient.post('/care-logs', payload),
}

export default postCareLogApi
