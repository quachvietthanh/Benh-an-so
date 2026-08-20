import axiosClient from './axiosClient'

export default {
  getAll: () => axiosClient.get('/backups'),

  createBackup: (data) => axiosClient.post('/backups', data),

  getById: (id) => axiosClient.get(`/backups/${id}`),

  restoreBackup: (backupId) => axiosClient.post(`/backups/${backupId}/restore`),

  downloadBackup: (backupId) =>
    axiosClient.get(`/backups/${backupId}/download`, {
      responseType: 'blob',
    }),
}
