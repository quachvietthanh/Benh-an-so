import axiosClient from './axiosClient'

const patientAllergyApi = {
  getAllergies: (patientId) => {
    return axiosClient.get(`/patients/${patientId}/allergies`)
  },

  addAllergy: (patientId, data) => {
    return axiosClient.post(`/patients/${patientId}/allergies`, data)
  },

  updateAllergy: (patientId, allergyId, data) => {
    return axiosClient.put(`/patients/${patientId}/allergies/${allergyId}`, data)
  },

  deleteAllergy: (patientId, allergyId, reason = '') => {
    return axiosClient.delete(`/patients/${patientId}/allergies/${allergyId}`, {
      params: reason ? { reason } : {},
    })
  },

  getChangeLogs: (patientId, allergyId) => {
    return axiosClient.get(`/patients/${patientId}/allergies/${allergyId}/history`)
  },
}

export default patientAllergyApi
