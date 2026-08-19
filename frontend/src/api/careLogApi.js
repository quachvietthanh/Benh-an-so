import axiosClient from './axiosClient'

const careLogApi = {
  search: (params) => {
    return axiosClient.get('/care-logs', { params })
  },
  getForPatient: (patientId) => {
    return axiosClient.get(`/care-logs/patient/${patientId}`)
  },
  create: (data) => {
    return axiosClient.post('/care-logs', data)
  },
}

export default careLogApi
