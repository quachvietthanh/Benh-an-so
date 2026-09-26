import axiosClient from './axiosClient.js'

/**
 * NCL-14-CN-009: API client cho Cổng thông tin bệnh nhân - Kết quả cận lâm sàng
 * Backend yêu cầu vai trò ROLE_PATIENT và tự động đối soát quyền sở hữu theo JWT token (QTN-23, TC-03)
 */
const patientPortalClinicalResultApi = {
  /**
   * Lấy danh sách kết quả cận lâm sàng đã xác nhận (FINAL) theo lượt khám (TC-01, TC-02, TC-04)
   * @param {string} visitId - UUID của lượt khám
   */
  getClinicalResultsByVisit: (visitId) => {
    return axiosClient.get(`/patient-portal/visits/${visitId}/clinical-results`)
  },

  /**
   * Alias: Lấy danh sách kết quả cận lâm sàng qua query param
   * @param {string} visitId - UUID của lượt khám
   */
  getClinicalResults: (visitId) => {
    return axiosClient.get('/patient-portal/clinical-results', {
      params: { visitId },
    })
  },

  /**
   * Xem chi tiết một kết quả cận lâm sàng cụ thể (TC-01)
   * @param {string} resultId - UUID của kết quả
   */
  getClinicalResultDetail: (resultId) => {
    return axiosClient.get(`/patient-portal/clinical-results/${resultId}`)
  },

  /**
   * Tải về phiếu kết quả cận lâm sàng bản đọc được (PDF) của một kết quả lẻ (TC-01)
   * @param {string} resultId - UUID của kết quả
   */
  downloadResultPdf: (resultId) => {
    return axiosClient.get(`/patient-portal/clinical-results/${resultId}/download`, {
      responseType: 'blob',
    })
  },

  /**
   * Tải về phiếu kết quả cận lâm sàng tổng hợp bản đọc được (PDF) của cả lượt khám (TC-01)
   * @param {string} visitId - UUID của lượt khám
   */
  downloadVisitResultsPdf: (visitId) => {
    return axiosClient.get(`/patient-portal/visits/${visitId}/clinical-results/download`, {
      responseType: 'blob',
    })
  },
}

export default patientPortalClinicalResultApi
