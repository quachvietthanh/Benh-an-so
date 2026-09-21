import axiosClient from './axiosClient.js'

const specialtyApi = {
  /**
   * Tra cứu danh sách chuyên khoa
   * @param {Object} params { keyword?: string, active?: boolean }
   */
  search: (params = {}) => {
    const formattedParams = {}
    if (params.keyword !== undefined && params.keyword !== null && String(params.keyword).trim() !== '') {
      formattedParams.keyword = String(params.keyword).trim()
    }
    if (params.active !== undefined && params.active !== null && params.active !== 'ALL') {
      formattedParams.active = params.active === true || params.active === 'true'
    }
    return axiosClient.get('/system/specialties', { params: formattedParams })
  },

  /**
   * Lấy chi tiết chuyên khoa kèm danh sách bác sĩ & phòng khám
   * @param {string} id
   */
  getById: (id) => axiosClient.get(`/system/specialties/${id}`),

  /**
   * Tạo mới chuyên khoa
   * @param {Object} data { code: string, name: string, description?: string, doctorIds?: string[], roomIds?: string[] }
   */
  create: (data) => axiosClient.post('/system/specialties', data),

  /**
   * Cập nhật thông tin chuyên khoa
   * @param {string} id
   * @param {Object} data { name: string, description?: string, doctorIds?: string[], roomIds?: string[] }
   */
  update: (id, data) => axiosClient.put(`/system/specialties/${id}`, data),

  /**
   * Ngừng dùng chuyên khoa
   * @param {string} id
   * @param {boolean} confirm
   */
  deactivate: (id, confirm = false) =>
    axiosClient.patch(`/system/specialties/${id}/deactivate`, null, {
      params: { confirm: Boolean(confirm) },
    }),

  /**
   * Kích hoạt lại chuyên khoa
   * @param {string} id
   */
  activate: (id) => axiosClient.patch(`/system/specialties/${id}/activate`),
}

export default specialtyApi
