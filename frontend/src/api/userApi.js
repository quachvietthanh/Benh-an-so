import axiosClient from './axiosClient'

/**
 * RESTful API Client for User & Doctor Management
 * Backend Controller: UserController.java (@RequestMapping("/users"))
 */
const userApi = {
  // GET /api/v1/users (Admin only)
  getAll: (params) => {
    return axiosClient.get('/users', { params })
  },

  // GET /api/v1/users/doctors (Admin, Doctor, Receptionist)
  getDoctors: () => {
    return axiosClient.get('/users/doctors')
  },

  // GET /api/v1/users/{id}
  getById: (id) => {
    return axiosClient.get(`/users/${id}`)
  },

  // POST /api/v1/users (Admin only)
  create: (data) => {
    return axiosClient.post('/users', data)
  },

  // PUT /api/v1/users/{id} (Admin only)
  update: (id, data) => {
    return axiosClient.put(`/users/${id}`, data)
  },

  // PATCH /api/v1/users/{id}/activate (Admin only)
  activate: (id) => {
    return axiosClient.patch(`/users/${id}/activate`)
  },

  // PATCH /api/v1/users/{id}/deactivate (Admin only)
  deactivate: (id) => {
    return axiosClient.patch(`/users/${id}/deactivate`)
  },

  // Aliases for backward compatibility
  list: () => {
    return axiosClient.get('/users')
  },

  remove: (id) => {
    return axiosClient.patch(`/users/${id}/deactivate`)
  },

  updateStatus: (id, locked) => {
    return locked ? axiosClient.patch(`/users/${id}/deactivate`) : axiosClient.patch(`/users/${id}/activate`)
  },
}

export default userApi
