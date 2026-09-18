import axiosClient from './axiosClient.js'

const adminOperationLogApi = {
  getLogs: (params) => axiosClient.get('/admin-operation-logs', { params }),
}

export default adminOperationLogApi
