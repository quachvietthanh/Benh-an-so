import axiosClient from './axiosClient'

const medicalRecordApi = {
  getById: (id) => {
    return axiosClient.get(`/medical-records/${id}`)
  },
  getByVisit: (visitId, params) => {
    return axiosClient.get(`/medical-records/visits/${visitId}`, { params })
  },
  getByPatient: (patientId, params) => {
    return axiosClient.get(`/medical-records/patient/${patientId}`, { params })
  },
  create: (data) => {
    return axiosClient.post('/medical-records', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/medical-records/${id}`, data)
  },
  // Diagnosis & Clinical Orders endpoints connected to Backend
  recordDiagnosis: (recordId, data) => {
    return axiosClient.put(`/medical-records/${recordId}/diagnoses`, data)
  },
  getDiagnosis: (recordId) => {
    return axiosClient.get(`/medical-records/${recordId}/diagnoses`)
  },
  lock: (recordId) => {
    return axiosClient.post(`/medical-records/${recordId}/lock`)
  },
  createClinicalOrder: (visitId, data) => {
    return axiosClient.post(`/clinical-orders/visits/${visitId}`, data)
  },
  getDiagnosisCatalog: (searchQuery) => {
    return axiosClient.get('/diagnosis-catalog', { params: { search: searchQuery } })
  },
}

export default medicalRecordApi
