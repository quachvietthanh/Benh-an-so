import axiosClient from './axiosClient'

const medicalRecordApi = {
  getAll: (params) => {
    return axiosClient.get('/medical-records', { params })
  },
  getById: (id) => {
    return axiosClient.get(`/medical-records/${id}`)
  },
  getByPatient: (patientId, params) => {
    return axiosClient.get('/medical-records', { params: { ...params, patientId } })
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
  attach: (id, file) => {
    const data = new FormData()
    data.append('file', file)
    return axiosClient.post(`/medical-records/${id}/attachments`, data, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  downloadAttachment: (id) => axiosClient.get(`/medical-records/attachments/${id}`, { responseType: 'blob' }),
}

export default medicalRecordApi
