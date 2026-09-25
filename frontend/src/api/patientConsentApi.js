import axiosClient from './axiosClient.js'

/**
 * NCL-15-CN-001 / NCL-15-CN-005: API client quản lý Phiếu đồng ý xử lý dữ liệu cá nhân
 * và Tiếp nhận yêu cầu xóa dữ liệu theo QTN-19 / QTN-24.
 */
const patientConsentApi = {
  /**
   * Lấy lịch sử tất cả các phiên bản phiếu đồng ý của bệnh nhân (AC-02)
   * @param {string} patientId - UUID của bệnh nhân
   * @returns {Promise} Danh sách PatientConsentHistoryResponse theo thứ tự thời gian
   */
  getConsentHistory: (patientId) => {
    return axiosClient.get(`/patients/${patientId}/consent-history`)
  },

  /**
   * Cập nhật phạm vi đồng ý hoặc rút lại toàn bộ sự đồng ý (AC-01)
   * Lưu thành một phiên bản mới, không sửa đè lên phiên bản cũ.
   * @param {string} patientId - UUID của bệnh nhân
   * @param {Object} data - { consentWithdrawn, consentWithdrawnReason, consentAgreed, consentVersion, scopes }
   */
  updateConsent: (patientId, data) => {
    return axiosClient.put(`/patients/${patientId}/consent`, data)
  },

  /**
   * Tiếp nhận yêu cầu xóa dữ liệu cá nhân theo QTN-19 (AC-03)
   * Hồ sơ bệnh án bắt buộc lưu tối thiểu 10 năm theo Luật Khám bệnh, chữa bệnh,
   * hệ thống ghi nhận yêu cầu và tự động thu hồi quyền đối với các mục đích ngoài khám chữa bệnh.
   * @param {string} patientId - UUID của bệnh nhân
   * @param {Object} data - { reason }
   */
  requestDataErasure: (patientId, data) => {
    return axiosClient.post(`/patients/${patientId}/data-erasure-request`, data)
  },
}

export default patientConsentApi
