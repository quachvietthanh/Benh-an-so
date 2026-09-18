import axiosClient from './axiosClient.js'

export const vitalSignApi = {
  record: (payload) => {
    return axiosClient.post('/vital-signs', payload)
  },

  update: (id, payload) => {
    return axiosClient.put(`/vital-signs/${id}`, payload)
  },

  getById: (id) => {
    return axiosClient.get(`/vital-signs/${id}`)
  },

  getByVisitId: (visitId) => {
    return axiosClient.get(`/vital-signs/visits/${visitId}`)
  },

  getPatientHistory: (patientId) => {
    return axiosClient.get(`/vital-signs/patients/${patientId}/history`)
  },

  search: (params) => {
    return axiosClient.get('/vital-signs', { params })
  },
}

export default vitalSignApi
