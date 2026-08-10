import axiosClient from './axiosClient'

const clinicalServiceApi = {
  getCatalog: (params) => {
    return axiosClient.get('/clinical-services', { params: { page: 0, size: 100, ...params } })
  },
}

export default clinicalServiceApi
