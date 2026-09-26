import axiosClient from './axiosClient.js'

export const patientChronicDiseaseApi = {
  list: (patientId) => {
    return axiosClient.get(`/patients/${patientId}/chronic-diseases`)
  },

  add: (patientId, payload) => {
    return axiosClient.post(`/patients/${patientId}/chronic-diseases`, payload)
  },

  remove: (patientId, chronicDiseaseId, reason) => {
    return axiosClient.delete(`/patients/${patientId}/chronic-diseases/${chronicDiseaseId}`, {
      params: reason ? { reason } : {},
    })
  },
}

export default patientChronicDiseaseApi
