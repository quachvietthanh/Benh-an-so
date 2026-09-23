import axiosClient from './axiosClient.js'

/**
 * API client kết nối Backend cho tính năng Chốt ca thu ngân cuối ngày (NCL-07-CN-009)
 * Controller: CashierShiftController (/cashier-shifts)
 */
const cashierShiftApi = {
  /**
   * Lấy tổng hợp ca làm việc hiện tại của thu ngân đang đăng nhập
   * Quyền yêu cầu: CASHIER_SHIFT_READ
   * @returns {Promise<import('axios').AxiosResponse>}
   */
  getCurrentSummary: () => axiosClient.get('/cashier-shifts/current-summary'),

  /**
   * Tạo phiếu chốt ca thu ngân và đối chiếu tiền thực tế
   * Quyền yêu cầu: CASHIER_SHIFT_CREATE
   * @param {{ actualCashAmount: number, notes?: string }} payload
   * @returns {Promise<import('axios').AxiosResponse>}
   */
  closeShift: (payload) => axiosClient.post('/cashier-shifts/close', payload),

  /**
   * Quản lý phòng khám duyệt và xác nhận phiếu chốt ca thu ngân
   * Quyền yêu cầu: CASHIER_SHIFT_CONFIRM
   * @param {string} id - UUID của phiếu chốt ca
   * @param {{ confirmationNotes?: string }} [payload]
   * @returns {Promise<import('axios').AxiosResponse>}
   */
  confirmShift: (id, payload = {}) => axiosClient.post(`/cashier-shifts/${id}/confirm`, payload),

  /**
   * Tìm kiếm, phân trang danh sách phiếu chốt ca
   * Quyền yêu cầu: CASHIER_SHIFT_READ
   * @param {{
   *   cashierId?: string,
   *   status?: 'PENDING_CONFIRMATION' | 'CONFIRMED' | 'REJECTED',
   *   from?: string,
   *   to?: string,
   *   page?: number,
   *   size?: number
   * }} params
   * @returns {Promise<import('axios').AxiosResponse>}
   */
  search: (params) => axiosClient.get('/cashier-shifts', { params }),

  /**
   * Lấy chi tiết thông tin một phiếu chốt ca theo ID
   * Quyền yêu cầu: CASHIER_SHIFT_READ
   * @param {string} id - UUID của phiếu chốt ca
   * @returns {Promise<import('axios').AxiosResponse>}
   */
  getById: (id) => axiosClient.get(`/cashier-shifts/${id}`),
}

export default cashierShiftApi
