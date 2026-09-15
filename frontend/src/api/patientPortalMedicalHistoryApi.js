import axiosClient from './axiosClient.js'

const patientPortalMedicalHistoryApi = {
  getMedicalHistory: () => {
    return axiosClient.get('/patient-portal/medical-history')
  },
  getMedicalHistoryDetail: (visitId) => {
    return axiosClient.get(`/patient-portal/medical-history/${visitId}`)
  },
}

export default patientPortalMedicalHistoryApi
