import axiosClient from './axiosClient.js'

const userApi = {
  list: () => {
    return axiosClient.get('/users')
  },

  getDoctors: () => {
    return axiosClient.get('/users/doctors')
  },

  create: (data) => {
    return axiosClient.post('/users', data)
  },

  update: (id, data) => {
    return axiosClient.put(`/users/${id}`, data)
  },

  remove: (id) => {
    return axiosClient.delete(`/users/${id}`)
  },

  activate: (id) => {
    return axiosClient.patch(`/users/${id}/activate`)
  },

  deactivate: (id) => {
    return axiosClient.patch(`/users/${id}/deactivate`)
  },

  getAll: (params) => {
    return axiosClient.get('/users', { params })
  },

  getById: (id) => {
    return axiosClient.get(`/users/${id}`)
  },

  updateStatus: (id, locked) => {
    return locked ? userApi.deactivate(id) : userApi.activate(id)
  },

  resetPassword: (id, data = {}) => {
    return axiosClient.post(`/users/${id}/reset-password`, data)
  },
}

export default userApi
