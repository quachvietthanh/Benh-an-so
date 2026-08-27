import axiosClient from './axiosClient'

const medicalRecordTemplateApi = {
  // 1. Danh mục chuyên khoa
  getSpecialties: (params = {}) => {
    return axiosClient.get('/system/specialties', { params })
  },

  // 2. Tìm kiếm / lọc danh sách mẫu bệnh án
  searchTemplates: (params = {}) => {
    return axiosClient.get('/system/medical-record-templates', { params })
  },

  // 3. Lấy chi tiết mẫu bệnh án và các sections
  getTemplateById: (templateId) => {
    return axiosClient.get(`/system/medical-record-templates/${templateId}`)
  },

  // 4. Tạo mẫu bệnh án mới
  createTemplate: (data) => {
    return axiosClient.post('/system/medical-record-templates', data)
  },

  // 5. Cập nhật mẫu bệnh án (sinh version mới, kèm changeNote)
  updateTemplate: (templateId, data) => {
    return axiosClient.put(`/system/medical-record-templates/${templateId}`, data)
  },

  // 6. Đặt làm mẫu mặc định cho chuyên khoa
  setDefaultTemplate: (templateId) => {
    return axiosClient.patch(`/system/medical-record-templates/${templateId}/default`)
  },

  // 7. Bật / tắt kích hoạt trạng thái (hỗ trợ chọn mẫu thay thế khi tắt mẫu mặc định)
  updateTemplateStatus: (templateId, data) => {
    return axiosClient.patch(`/system/medical-record-templates/${templateId}/status`, data)
  },
}

export default medicalRecordTemplateApi
