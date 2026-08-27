import axiosClient from './axiosClient'

const medicalRecordTemplateApi = {
  getSpecialties: (params = {}) => {
    return axiosClient.get('/system/specialties', { params })
  },
  searchTemplates: (params = {}) => {
    return axiosClient.get('/system/medical-record-templates', { params })
  },
  getTemplateById: (templateId) => {
    return axiosClient.get(`/system/medical-record-templates/${templateId}`)
  },
  createTemplate: (data) => {
    return axiosClient.post('/system/medical-record-templates', data)
  },
  updateTemplate: (templateId, data) => {
    return axiosClient.put(`/system/medical-record-templates/${templateId}`, data)
  },
  setDefaultTemplate: (templateId) => {
    return axiosClient.patch(`/system/medical-record-templates/${templateId}/default`)
  },
  updateTemplateStatus: (templateId, data) => {
    return axiosClient.patch(`/system/medical-record-templates/${templateId}/status`, data)
  },
}

export default medicalRecordTemplateApi
