import axiosClient from './axiosClient'

const medicalRecordApi = {
  getAll: (params) => {
    return axiosClient.get('/medical-records', { params })
  },
  getById: (id) => {
    return axiosClient.get(`/medical-records/${id}`)
  },
  getByVisit: (visitId, params) => {
    return axiosClient.get(`/medical-records/visits/${visitId}`, { params })
  },
  getByPatient: (patientId, params) => {
    return axiosClient.get(`/medical-records/patient/${patientId}`, { params })
  },
  getByDoctor: (doctorId, params) => {
    return axiosClient.get(`/medical-records/by-doctor/${doctorId}`, { params })
  },
  create: (data) => {
    return axiosClient.post('/medical-records', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/medical-records/${id}`, data)
  },
  delete: (id) => {
    return axiosClient.delete(`/medical-records/${id}`)
  },
  // Diagnosis & Clinical Orders endpoints connected to Backend
  recordDiagnosis: (recordId, data) => {
    return axiosClient.put(`/medical-records/${recordId}/diagnoses`, data)
  },
  getDiagnosis: (recordId) => {
    return axiosClient.get(`/medical-records/${recordId}/diagnoses`)
  },
  createClinicalOrder: (visitId, data) => {
    return axiosClient.post(`/clinical-orders/visits/${visitId}`, data)
  },
  getDiagnosisCatalog: (searchQuery) => {
    return axiosClient.get('/diagnosis-catalog', { params: { search: searchQuery } })
  },
  attach: (id, file) => {
    const data = new FormData()
    data.append('file', file)
    return axiosClient.post(`/medical-records/${id}/attachments`, data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  downloadAttachment: (id) =>
    axiosClient.get(`/medical-records/attachments/${id}`, { responseType: 'blob' }),
}

export default medicalRecordApi
