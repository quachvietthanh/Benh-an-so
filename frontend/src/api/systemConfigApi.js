import axiosClient from './axiosClient.js'

const systemConfigApi = {
  getAnonymizationStatus: () => axiosClient.get('/system/anonymization'),
  updateAnonymizationStatus: (enabled) => axiosClient.patch('/system/anonymization', { enabled }),
}

export default systemConfigApi
