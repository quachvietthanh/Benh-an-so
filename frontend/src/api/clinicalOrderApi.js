import axiosClient from './axiosClient.js'
import { pageParams, pickFields } from './apiContract.js'

const orderPayload = (data = {}) => ({
  ...pickFields(data, ['clinicalReason']),
  items: Array.isArray(data.items)
    ? data.items.map((item) => pickFields(item, ['serviceId', 'instruction']))
    : [],
})

/**
 * RESTful API Client for Clinical Orders Management
 * Namespace: /clinical-orders/*
 */
const clinicalOrderApi = {
  create: (visitId, data) => {
    return axiosClient.post(`/clinical-orders/visits/${visitId}`, orderPayload(data))
  },
  getByVisit: (visitId, params) => {
    return axiosClient.get(`/clinical-orders/visits/${visitId}`, { params: pageParams(params) })
  },
}

export default clinicalOrderApi
