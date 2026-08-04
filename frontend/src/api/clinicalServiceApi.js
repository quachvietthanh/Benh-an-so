import axiosClient from './axiosClient'

const clinicalServiceApi = {
  getCatalog: (params) => {
    return axiosClient.get('/clinical-services', { params })
  },
}

export default clinicalServiceApi
