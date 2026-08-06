import axiosClient from './axiosClient.js'
import { pickFields } from './apiContract.js'

const CREATE_FIELDS = ['username', 'password', 'fullName', 'email', 'phone', 'roleName']
const UPDATE_FIELDS = ['fullName', 'email', 'phone', 'roleName']

const userApi = {
  list: () => axiosClient.get('/users'),
  getAll: () => axiosClient.get('/users'),
  getDoctors: () => axiosClient.get('/users/doctors'),
  getById: (id) => axiosClient.get(`/users/${id}`),
  create: (data) => axiosClient.post('/users', pickFields(data, CREATE_FIELDS)),
  update: (id, data) => axiosClient.put(`/users/${id}`, pickFields(data, UPDATE_FIELDS)),
  activate: (id) => axiosClient.patch(`/users/${id}/activate`),
  deactivate: (id) => axiosClient.patch(`/users/${id}/deactivate`),
  updateStatus: (id, locked) => (
    locked
      ? axiosClient.patch(`/users/${id}/deactivate`)
      : axiosClient.patch(`/users/${id}/activate`)
  ),
}

export default userApi
