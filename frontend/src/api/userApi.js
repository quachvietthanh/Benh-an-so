import axiosClient from './axiosClient'

/**
 * API quản lý người dùng (dành cho Admin)
 */
const userApi = {
  list: () => {
    return axiosClient.get('/users')
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

  /**
   * Lấy danh sách tất cả người dùng (phân trang)
   */
  getAll: (params) => {
    return axiosClient.get('/admin/users', { params })
  },

  /**
   * Lấy thông tin chi tiết người dùng
   */
  getById: (id) => {
    return axiosClient.get(`/admin/users/${id}`)
  },

  /**
   * Cập nhật trạng thái khóa / mở khóa tài khoản người dùng
   *
   * PUT /api/v1/admin/users/{id}/status?locked=true|false
   *
   * @param {string} id - UUID của người dùng
   * @param {boolean} locked - true: khóa, false: mở khóa
   */
  updateStatus: (id, locked) => {
    return axiosClient.put(`/admin/users/${id}/status`, null, {
      params: { locked },
    })
  },
}

export default userApi
