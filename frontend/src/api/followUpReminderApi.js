import axiosClient from './axiosClient.js'

const followUpReminderApi = {
  getDue: (params = {}) => axiosClient.get('/follow-up-reminders/due', { params }),

  search: (params = {}) => axiosClient.get('/follow-up-reminders', { params }),

  create: (payload) => axiosClient.post('/follow-up-reminders', payload),

  updateStatus: (id, status) => (
    axiosClient.patch(`/follow-up-reminders/${id}/status`, { status })
  ),
}

export default followUpReminderApi
