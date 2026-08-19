import axiosClient from './axiosClient'

const followUpReminderApi = {
  getDue: (params) => {
    return axiosClient.get('/follow-up-reminders/due', { params })
  },
  search: (params) => {
    return axiosClient.get('/follow-up-reminders', { params })
  },
  create: (data) => {
    return axiosClient.post('/follow-up-reminders', data)
  },
  updateStatus: (id, status) => {
    return axiosClient.patch(`/follow-up-reminders/${id}/status`, { status })
  },
}

export default followUpReminderApi
