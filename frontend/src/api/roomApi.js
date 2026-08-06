import axiosClient from './axiosClient.js'
import { pageParams, pickFields } from './apiContract.js'

const roomApi = {
  search: (params = {}) => axiosClient.get('/rooms', {
    params: pageParams(params, ['keyword', 'active']),
  }),
  getById: (id) => axiosClient.get(`/rooms/${id}`),
  create: (data) => axiosClient.post('/rooms', pickFields(data, ['code', 'name'])),
  update: (id, data) => axiosClient.put(`/rooms/${id}`, pickFields(data, ['name'])),
  activate: (id) => axiosClient.patch(`/rooms/${id}/activate`),
  deactivate: (id) => axiosClient.patch(`/rooms/${id}/deactivate`),
  getAssignments: (params = {}) => axiosClient.get('/doctor-room-assignments', {
    params: pickFields(params, ['doctorId', 'roomId']),
  }),
  assignDoctor: (doctorId, roomId) => axiosClient.put(
    `/doctors/${doctorId}/room-assignment`,
    { roomId },
  ),
  removeDoctorAssignment: (doctorId) => axiosClient.delete(
    `/doctors/${doctorId}/room-assignment`,
  ),
}

export default roomApi
