import axiosClient from './axiosClient'

/**
 * RESTful API Client for Clinical Results Management
 * Endpoints:
 * GET    /api/results             - Fetch all result entries (with search/filter params)
 * GET    /api/results/{id}        - Fetch single result entry by ID
 * POST   /api/results             - Create new clinical result record
 * PUT    /api/results/{id}        - Update existing clinical result entry
 * POST   /api/results/upload      - Upload result attachments (PDF, JPG, PNG)
 */
const clinicalResultApi = {
  getAll: (params) => {
    return axiosClient.get('/results', { params })
  },
  getById: (id) => {
    return axiosClient.get(`/results/${id}`)
  },
  create: (data) => {
    return axiosClient.post('/results', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/results/${id}`, data)
  },
  uploadAttachment: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosClient.post('/results/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
}

export default clinicalResultApi
