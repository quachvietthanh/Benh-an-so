import axiosClient from './axiosClient.js'

const patientImportApi = {
  /**
   * Tải tệp mẫu danh sách bệnh nhân chuẩn định dạng (.xlsx)
   */
  downloadTemplate: () => {
    return axiosClient.get('/patients/import/template', {
      responseType: 'blob',
    })
  },

  /**
   * Tải tệp lên để phân tích và xem trước kết quả
   * @param {FormData} formData Chứa key 'file'
   */
  previewImport: (formData) => {
    return axiosClient.post('/patients/import/preview', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * Xác nhận thực hiện nhập hồ sơ bệnh nhân từ tệp bảng tính
   * @param {FormData} formData Chứa key 'file'
   * @param {boolean} skipDuplicates Có tự động bỏ qua các dòng nghi trùng không (mặc định true)
   */
  importPatients: (formData, skipDuplicates = true) => {
    return axiosClient.post('/patients/import', formData, {
      params: { skipDuplicates },
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * Lấy danh sách lịch sử các lần nhập hồ sơ (Audit Log)
   * @param {Object} params Tham số phân trang (page, size, sort)
   */
  getImportLogs: (params) => {
    return axiosClient.get('/patients/import-logs', { params })
  },

  /**
   * Lấy chi tiết một lần nhập hồ sơ kèm danh sách lỗi chi tiết
   * @param {string} id UUID của bản ghi lịch sử nhập
   */
  getImportLogById: (id) => {
    return axiosClient.get(`/patients/import-logs/${id}`)
  },
}

export default patientImportApi
