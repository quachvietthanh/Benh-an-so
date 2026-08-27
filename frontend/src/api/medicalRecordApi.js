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
  recordDiagnosis: (recordId, data) => {
    const secondarySource = data.secondaryDiagnoses || data.secondaryIcds || data.secondaryIcdCodes || []
    const secondaryDiagnoses = Array.isArray(secondarySource)
      ? secondarySource
      : String(secondarySource).split(',').map((code) => code.trim()).filter(Boolean)
    const payload = data.primaryDiagnosis
      ? data
      : {
        primaryDiagnosis: {
          diagnosisCatalogId:
            data.primaryDiagnosisCatalogId ||
            data.diagnosisCatalogId ||
            data.primaryIcd?.diagnosisCatalogId ||
            data.primaryIcd?.id,
          code: data.primaryIcdCode || data.primaryIcd?.code || 'Z00.0',
          name: data.primaryIcdName || data.primaryIcd?.name || 'Khám sức khỏe tổng quát',
          note: data.clinicalNotes || data.note || '',
        },
        secondaryDiagnoses: secondaryDiagnoses.map((sec) => ({
          diagnosisCatalogId:
            sec.diagnosisCatalogId || sec.id,
          code: typeof sec === 'string' ? sec : sec.code,
          name: typeof sec === 'string' ? sec : sec.name || sec.code,
          note: typeof sec === 'string' ? '' : sec.note || '',
        })),
      }
    return axiosClient.put(`/medical-records/${recordId}/diagnoses`, payload)
  },
  getDiagnosis: (recordId) => {
    return axiosClient.get(`/medical-records/${recordId}/diagnoses`)
  },
  lock: (recordId) => {
    return axiosClient.post(`/medical-records/${recordId}/lock`)
  },
  sign: (recordId, data = {}) => {
    return axiosClient.post(`/medical-records/${recordId}/sign`, data)
  },
  archive: (recordId) => {
    return axiosClient.post(`/medical-records/${recordId}/archive`)
  },
  delete: (recordId) => {
    return axiosClient.delete(`/medical-records/${recordId}`)
  },
  amend: (recordId, data) => {
    return axiosClient.post(`/medical-records/${recordId}/amendments`, data)
  },
  getVersionHistory: (recordId) => {
    return axiosClient.get(`/medical-records/${recordId}/versions`)
  },
  createClinicalOrder: (visitId, data) => {
    return axiosClient.post(`/clinical-orders/visits/${visitId}`, data)
  },
  getClinicalOrders: (visitId, params = {}) => {
    return axiosClient.get(`/clinical-orders/visits/${visitId}`, { params })
  },
  getDiagnosisCatalog: (searchQuery) => {
    return axiosClient.get('/diagnosis-catalog', { params: { search: searchQuery } })
  },
  getAccessLogsByPatient: (patientId, params = {}) => {
    return axiosClient.get('/medical-records/access-logs', { params: { patientId, ...params } })
  },
  getAccessLogsByRecord: (medicalRecordId, params = {}) => {
    return axiosClient.get(`/medical-records/${medicalRecordId}/access-logs`, { params })
  },
  exportCopy: (medicalRecordId) => {
    return axiosClient.get(`/medical-records/${medicalRecordId}/export-copy`, {
      responseType: 'blob',
    })
  },
  getVersionHistory: (medicalRecordId) => {
    return axiosClient.get(`/medical-records/${medicalRecordId}/versions`)
  },
  amendRecord: (medicalRecordId, data) => {
    return axiosClient.post(`/medical-records/${medicalRecordId}/amendments`, data)
  },
  getTemplateOptions: (medicalRecordId) => {
    return axiosClient.get(`/medical-records/${medicalRecordId}/template-options`)
  },
  getTemplateOptionsByVisit: (visitId) => {
    return axiosClient.get(`/medical-records/visits/${visitId}/template-options`)
  },
  applyTemplate: (medicalRecordId, templateId) => {
    return axiosClient.put(`/medical-records/${medicalRecordId}/template`, { templateId })
  },
}

export default medicalRecordApi
