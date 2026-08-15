import axiosClient from './axiosClient'

export default {
  // Lấy danh sách các bản sao lưu từ Backend (GET /admin/backups hoặc /backups)
  getAll: (params) => axiosClient.get('/admin/backups', { params }),

  // Tạo bản sao lưu mới từ Backend (POST /admin/backups hoặc /backups)
  createBackup: (data) => axiosClient.post('/admin/backups', data),

  // Yêu cầu phục hồi dữ liệu từ bản sao lưu chỉ định (POST /admin/backups/{id}/restore)
  restoreBackup: (backupId) => axiosClient.post(`/admin/backups/${backupId}/restore`),
}
