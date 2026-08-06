import axiosClient from './axiosClient.js'
import { pageParams, pickFields } from './apiContract.js'

const resultPayload = (data = {}, includeChangeReason = false) => pickFields(
  data,
  [
    'numericValue', 'textValue', 'abnormalFlag', 'conclusion',
    ...(includeChangeReason ? ['changeReason'] : []),
  ],
)

const clinicalResultApi = {
  enter: (clinicalOrderItemId, data) => axiosClient.post(
    `/clinical-order-items/${clinicalOrderItemId}/results`,
    resultPayload(data),
  ),
  create: (data) => clinicalResultApi.enter(data.clinicalOrderItemId, data),
  update: (id, data) => axiosClient.put(`/clinical-results/${id}`, resultPayload(data, true)),
  finalize: (id) => axiosClient.post(`/clinical-results/${id}/finalize`),
  getById: (id) => axiosClient.get(`/clinical-results/${id}`),
  getByVisit: (visitId, params = {}) => axiosClient.get(
    `/clinical-results/visits/${visitId}`,
    { params: pageParams(params) },
  ),
  getHistory: (id) => axiosClient.get(`/clinical-results/${id}/history`),
  uploadAttachment: (resultId, file) => {
    if (!resultId) return Promise.reject(new Error('Clinical result ID is required'))
    const formData = new FormData()
    formData.append('file', file)
    return axiosClient.post(`/clinical-results/${resultId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  getDownloadUrl: (attachmentId) => axiosClient.get(
    `/clinical-result-attachments/${attachmentId}/download`,
  ),
}

export default clinicalResultApi
