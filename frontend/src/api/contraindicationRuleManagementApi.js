import axiosClient from './axiosClient.js'

const contraindicationRuleManagementApi = {
  /**
   * Tìm kiếm danh mục quy tắc chống chỉ định có phân trang và lọc
   * @param {{ activeIngredient?: string, contraindicationType?: string, severity?: string, active?: boolean, page?: number, size?: number }} params
   */
  searchRules: (params = {}) =>
    axiosClient.get('/contraindication-rules', { params }),

  /**
   * Tạo mới quy tắc chống chỉ định
   * @param {Object} data
   */
  createRule: (data) =>
    axiosClient.post('/contraindication-rules', data),

  /**
   * Cập nhật quy tắc chống chỉ định
   * @param {string} id
   * @param {Object} data
   */
  updateRule: (id, data) =>
    axiosClient.put(`/contraindication-rules/${id}`, data),

  /**
   * Vô hiệu hóa quy tắc chống chỉ định (soft delete)
   * @param {string} id
   */
  deactivateRule: (id) =>
    axiosClient.delete(`/contraindication-rules/${id}`),

  /**
   * Kích hoạt lại quy tắc chống chỉ định
   * @param {string} id
   */
  activateRule: (id) =>
    axiosClient.post(`/contraindication-rules/${id}/activate`),

  /**
   * Tải lên file CSV / Excel danh mục quy tắc
   * @param {File} file
   */
  importRules: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosClient.post('/contraindication-rules/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
}

export default contraindicationRuleManagementApi
