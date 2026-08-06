import axiosClient from './axiosClient.js'
import { pageParams } from './apiContract.js'

const clinicalServiceApi = {
  getCatalog: (params) => {
    return axiosClient.get('/clinical-services', { params: pageParams(params, ['keyword']) })
  },
}

export default clinicalServiceApi
