import axiosClient from './axiosClient'

/**
 * RESTful API Client for Clinical Results & Attachments Management
 * Kết nối API Backend:
 * - GET  /api/results                          - Danh sách tất cả kết quả cận lâm sàng
 * - GET  /api/results/{id}                     - Chi tiết kết quả theo ID
 * - POST /api/results                          - Tạo mới kết quả cận lâm sàng
 * - PUT  /api/results/{id}                     - Cập nhật kết quả cận lâm sàng
 * - POST /api/clinical-results/{resultId}/attachments - Tải lên tệp đính kèm (Multipart)
 * - GET  /api/clinical-result-attachments/{attachmentId}/download - Lấy URL tải xuống tệp
 */
const clinicalResultApi = {
  getAll: (params) => {
    return axiosClient.get('/results', { params })
  },
  getById: (id) => {
    return axiosClient.get(`/results/${id}`)
  },
  getByVisit: (visitId, params) => {
    return axiosClient.get(`/clinical-results/visits/${visitId}`, { params })
  },
  create: (data) => {
    return axiosClient.post('/results', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/results/${id}`, data)
  },
  uploadAttachment: (resultId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const endpoint = resultId ? `/clinical-results/${resultId}/attachments` : '/results/upload'
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
