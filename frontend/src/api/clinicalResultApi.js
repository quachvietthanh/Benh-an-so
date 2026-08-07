import axiosClient from './axiosClient'

/**
 * RESTful API Client for Clinical Results & Attachments Management
 * Chuẩn hóa Namespace: /clinical-results/*
 */
const clinicalResultApi = {
  getAll: (params) => {
    return axiosClient.get('/clinical-results', { params }).catch((err) => {
      console.warn('Backend /clinical-results global list endpoint not available, returning empty array:', err?.message)
      return { data: [] }
    })
  },
  getById: (id) => {
    return axiosClient.get(`/clinical-results/${id}`)
  },
  getByVisit: (visitId, params) => {
    return axiosClient.get(`/clinical-results/visits/${visitId}`, { params })
  },
  create: (data) => {
    return axiosClient.post('/clinical-results', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/clinical-results/${id}`, data)
  },
  uploadAttachment: (resultId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const endpoint = resultId ? `/clinical-results/${resultId}/attachments` : '/clinical-results/upload'
    return axiosClient.post(endpoint, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  getDownloadUrl: (attachmentId) => {
    return axiosClient.get(`/clinical-result-attachments/${attachmentId}/download`)
  },
}

export default clinicalResultApi
