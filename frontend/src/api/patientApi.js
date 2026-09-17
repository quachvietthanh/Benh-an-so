import axiosClient from './axiosClient.js'

const patientApi = {
  getAll: (params) => {
    return axiosClient.get('/patients', { params })
  },
  search: (params) => {
    return axiosClient.get('/patients', { params })
  },

  getById: (id) => {
    return axiosClient.get(`/patients/${id}`)
  },
  getByCode: (code) => {
    return axiosClient.get(`/patients/code/${code}`)
  },
  getHistory: (patientId, params) => {
    return axiosClient.get(`/medical-history/patients/${patientId}`, { params })
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
  merge: (data) => {
    return axiosClient.post('/patients/merge', data)
  },
  getDuplicates: () => {
    return axiosClient.get('/patients/duplicates')
  },
}

export default patientApi
