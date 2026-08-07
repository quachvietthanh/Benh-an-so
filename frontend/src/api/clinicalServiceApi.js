import axiosClient from './axiosClient'

const clinicalServiceApi = {
  getCatalog: (params) => {
    return axiosClient.get('/clinical-services', { params: { size: 200, ...params } })
  },
}

export default clinicalServiceApi
