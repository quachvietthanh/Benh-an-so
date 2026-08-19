import axiosClient from './axiosClient'

const systemApi = {
  services: (params) => axiosClient.get('/system/services', { params }),
  getServiceById: (id) => axiosClient.get(`/system/services/${id}`),
  createService: (data) => axiosClient.post('/system/services', data),
  updateService: (id, data) => axiosClient.put(`/system/services/${id}`, data),
  updateServiceStatus: (id, data) => axiosClient.patch(`/system/services/${id}/status`, data),
  getServicePriceHistory: (id) => axiosClient.get(`/system/services/${id}/prices`),

  clinic: () => axiosClient.get('/system/clinic'),
  updateClinic: (data) => axiosClient.put('/system/clinic', data),

  getRooms: (params = {}) => axiosClient.get('/rooms', { params: { size: 100, ...params } }),
  getRoomById: (roomId) => axiosClient.get(`/rooms/${roomId}`),
  createRoom: (data) => axiosClient.post('/rooms', data),
  updateRoom: (roomId, data) => axiosClient.put(`/rooms/${roomId}`, data),
  activateRoom: (roomId) => axiosClient.patch(`/rooms/${roomId}/activate`),
  deactivateRoom: (roomId) => axiosClient.patch(`/rooms/${roomId}/deactivate`),

  getDoctorRoomAssignments: (params = {}) => axiosClient.get('/doctor-room-assignments', { params }),
  assignDoctorRoom: (doctorId, roomId) => axiosClient.put(`/doctors/${doctorId}/room-assignment`, { roomId }),
  removeDoctorRoomAssignment: (doctorId) => axiosClient.delete(`/doctors/${doctorId}/room-assignment`),
}

export default systemApi
