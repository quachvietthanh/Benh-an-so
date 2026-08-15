import axiosClient from './axiosClient'

export default {
  // Lấy danh sách các bản sao lưu từ Backend (GET /backups)
  getAll: () => axiosClient.get('/backups'),

  // Tạo bản sao lưu mới từ Backend (POST /backups)
  createBackup: (data) => axiosClient.post('/backups', data),

  // Xem chi tiết bản sao lưu từ Backend (GET /backups/{id})
  getById: (id) => axiosClient.get(`/backups/${id}`),

  // Yêu cầu phục hồi dữ liệu từ bản sao lưu chỉ định (POST /backups/{id}/restore)
  restoreBackup: (backupId) => axiosClient.post(`/backups/${backupId}/restore`),

  // Tải file bản sao lưu từ Backend (GET /backups/{id}/download)
  downloadBackup: (backupId) =>
    axiosClient.get(`/backups/${backupId}/download`, {
      responseType: 'blob',
    }),
}

