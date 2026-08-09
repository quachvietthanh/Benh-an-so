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
    const payload = data.primaryDiagnosis
      ? data
      : {
          primaryDiagnosis: {
            diagnosisCatalogId:
              data.primaryDiagnosisCatalogId ||
              data.diagnosisCatalogId ||
              '90000000-0000-0000-0000-000000000001',
            code: data.primaryIcdCode || data.primaryIcd?.code || 'Z00.0',
            name: data.primaryIcdName || data.primaryIcd?.name || 'Khám sức khỏe tổng quát',
            note: data.clinicalNotes || data.note || '',
          },
          secondaryDiagnoses: (data.secondaryDiagnoses || data.secondaryIcds || []).map((sec) => ({
            diagnosisCatalogId:
              sec.diagnosisCatalogId || '90000000-0000-0000-0000-000000000001',
            code: typeof sec === 'string' ? sec : sec.code,
            name: typeof sec === 'string' ? sec : sec.name || sec.code,
            note: sec.note || '',
          })),
        }
    return axiosClient.put(`/medical-records/${recordId}/diagnoses`, payload)
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
    // BE controller is /clinical-results/{resultId}/attachments
    return axiosClient.post(`/clinical-results/${id}/attachments`, data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).catch(() => {
      // Fallback for medical record attachment if resultId is not yet created
      return axiosClient.post(`/medical-records/${id}/attachments`, data, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    })
  },
  downloadAttachment: (id) =>
    axiosClient.get(`/clinical-result-attachments/${id}/download`).catch(() =>
      axiosClient.get(`/medical-records/attachments/${id}`, { responseType: 'blob' })
    ),
}

export default medicalRecordApi
