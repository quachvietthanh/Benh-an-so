import axiosClient from './axiosClient'

const securityAlertApi = {
  getAlerts: (params = {}) => {
    return axiosClient.get('/security-alerts', {
      params: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        ...params,
      },
    })
  },

  updateStatus: (id, status) => {
    return axiosClient.patch(`/security-alerts/${id}/status`, null, {
      params: { status },
    })
  },
}

export default securityAlertApi
