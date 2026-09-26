import axiosClient from './axiosClient.js'

const contraindicationApi = {
  /**
   * Đối chiếu cảnh báo chống chỉ định theo tuổi, thai kỳ, bệnh nền
   * @param {string} medicalRecordId UUID bệnh án
   * @param {string[]} medicineIds Danh sách UUID thuốc
   */
  checkContraindications: (medicalRecordId, medicineIds) =>
    axiosClient.post('/prescriptions/check-contraindications', { medicalRecordId, medicineIds }),

  /**
   * Cập nhật tình trạng thai kỳ cho bệnh nhân
   * @param {string} patientId UUID bệnh nhân
   * @param {'PREGNANT' | 'NOT_PREGNANT'} pregnancyStatus
   */
  updatePregnancyStatus: (patientId, pregnancyStatus) =>
    axiosClient.patch(`/patients/${patientId}/pregnancy-status`, { pregnancyStatus }),

  /**
   * Lấy danh sách bệnh nền / bệnh mạn tính của bệnh nhân
   * @param {string} patientId UUID bệnh nhân
   */
  getChronicDiseases: (patientId) =>
    axiosClient.get(`/patients/${patientId}/chronic-diseases`),

  /**
   * Khai báo bổ sung bệnh nền / bệnh mạn tính
   * @param {string} patientId UUID bệnh nhân
   * @param {{ diagnosisCatalogId: string, diagnosedYear?: number, note?: string }} data
   */
  addChronicDisease: (patientId, data) =>
    axiosClient.post(`/patients/${patientId}/chronic-diseases`, data),

  /**
   * Xóa bệnh mạn tính khỏi hồ sơ
   * @param {string} chronicDiseaseId UUID bệnh mạn tính
   */
  deleteChronicDisease: (chronicDiseaseId) =>
    axiosClient.delete(`/patients/chronic-diseases/${chronicDiseaseId}`),
}

export default contraindicationApi
