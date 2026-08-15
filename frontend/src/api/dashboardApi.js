import axiosClient from './axiosClient.js'

const dashboardApi = {
  getOperational: (config = {}) => axiosClient.get('/dashboard/operational', config),
}

export default dashboardApi
