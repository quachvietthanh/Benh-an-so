import axiosClient from './axiosClient.js'

const clinicalServiceApi = {
  getCatalog: (params) => {
    return axiosClient.get('/clinical-services', { params: { page: 0, size: 100, ...params } })
  },

  search: (params = {}) => {
    return axiosClient.get('/system/clinical-services', { params })
  },

  getById: (clinicalServiceId) => {
    return axiosClient.get(`/system/clinical-services/${clinicalServiceId}`)
  },

  create: (data) => {
    return axiosClient.post('/system/clinical-services', data)
  },

  update: (clinicalServiceId, data) => {
    return axiosClient.put(`/system/clinical-services/${clinicalServiceId}`, data)
  },

  updateStatus: (clinicalServiceId, active) => {
    return axiosClient.patch(`/system/clinical-services/${clinicalServiceId}/status`, { active })
  },

  getReferenceRanges: (clinicalServiceId) => {
    return axiosClient.get(`/system/clinical-services/${clinicalServiceId}/reference-ranges`)
  },

  createReferenceRange: (clinicalServiceId, data) => {
    return axiosClient.post(`/system/clinical-services/${clinicalServiceId}/reference-ranges`, data)
  },

  updateReferenceRange: (clinicalServiceId, referenceRangeId, data) => {
    return axiosClient.put(`/system/clinical-services/${clinicalServiceId}/reference-ranges/${referenceRangeId}`, data)
  },

  updateReferenceRangeStatus: (clinicalServiceId, referenceRangeId, active) => {
    return axiosClient.patch(`/system/clinical-services/${clinicalServiceId}/reference-ranges/${referenceRangeId}/status`, { active })
  },
}

export default clinicalServiceApi
