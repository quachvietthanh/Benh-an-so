import axiosClient from './axiosClient'

const patientApi = {
  getAll: (params) => {
    return axiosClient.get('/patients', { params })
  },
  getById: (id) => {
    return axiosClient.get(`/patients/${id}`)
  },
  getByCode: (code) => {
    return axiosClient.get(`/patients/code/${code}`)
  },
  getHistory: (id, params) => {
    return axiosClient.get(`/patients/${id}/history`, { params })
  },
  create: (data) => {
    return axiosClient.post('/patients', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/patients/${id}`, data)
  },
  delete: (id) => {
    return axiosClient.delete(`/patients/${id}`)
  },
}

export default patientApi
