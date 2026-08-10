import axiosClient from './axiosClient'

const medicineApi = {
  /**
   * Lấy danh sách / Tìm kiếm danh mục thuốc
   * @param {Object} params { keyword, active, page, size, sort }
   */
  search: (params = {}) => axiosClient.get('/medicines', { params }),

  /**
   * Lấy thông tin chi tiết một thuốc theo ID
   * @param {string} id UUID của thuốc
   */
  getById: (id) => axiosClient.get(`/medicines/${id}`),

  /**
   * Thêm thuốc mới vào danh mục
   * @param {Object} data CreateMedicineRequest
   */
  create: (data) => axiosClient.post('/medicines', data),

  /**
   * Cập nhật thông tin thuốc
   * @param {string} id UUID của thuốc
   * @param {Object} data UpdateMedicineRequest
   */
  update: (id, data) => axiosClient.put(`/medicines/${id}`, data),

  /**
   * Cập nhật trạng thái sử dụng của thuốc (Đang dùng / Ngừng dùng)
   * @param {string} id UUID của thuốc
   * @param {boolean} active Trạng thái active (true: Đang dùng, false: Ngừng dùng)
   */
  updateStatus: (id, active) =>
    axiosClient.patch(`/medicines/${id}/status`, { active }),
}

export default medicineApi
