import axiosClient from './axiosClient'

const DEFAULT_PAGE_PARAMS = { page: 0, size: 100 }

const enterRequest = (data = {}) => ({
  numericValue: data.numericValue ?? null,
  textValue: data.textValue ?? null,
  abnormalFlag: data.abnormalFlag ?? 'UNKNOWN',
  conclusion: data.conclusion ?? null,
})

const updateRequest = (data = {}) => ({
  ...enterRequest(data),
  changeReason: data.changeReason,
})

const clinicalResultApi = {
  getAll: (params = {}) => {
    const { visitId, ...query } = params
    if (!visitId) {
      return Promise.reject(new Error('visitId is required to load clinical results.'))
    }
    return axiosClient.get(`/clinical-results/visits/${visitId}`, {
      params: { ...DEFAULT_PAGE_PARAMS, ...query },
    })
  },

  getById: (resultId) => axiosClient.get(`/clinical-results/${resultId}`),

  getByVisit: (visitId, params = {}) =>
    axiosClient.get(`/clinical-results/visits/${visitId}`, {
      params: { ...DEFAULT_PAGE_PARAMS, ...params },
    }),

  getOrdersByVisit: (visitId, params = {}) =>
    axiosClient.get(`/clinical-orders/visits/${visitId}`, {
      params: { ...DEFAULT_PAGE_PARAMS, ...params },
    }),

  enter: (clinicalOrderItemId, data) =>
    axiosClient.post(
      `/clinical-order-items/${clinicalOrderItemId}/results`,
      enterRequest(data)
    ),

  update: (resultId, data) =>
    axiosClient.put(`/clinical-results/${resultId}`, updateRequest(data)),

  finalize: (resultId) =>
    axiosClient.post(`/clinical-results/${resultId}/finalize`),

  getHistory: (resultId) =>
    axiosClient.get(`/clinical-results/${resultId}/history`),

  uploadAttachment: (resultId, file) => {
    if (!resultId) {
      return Promise.reject(new Error('resultId is required to upload a clinical result attachment.'))
    }
    if (!file) {
      return Promise.reject(new Error('A file is required to upload a clinical result attachment.'))
    }

    const formData = new FormData()
    formData.append('file', file)
    return axiosClient.post(`/clinical-results/${resultId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  getDownloadUrl: (attachmentId) =>
    axiosClient.get(`/clinical-result-attachments/${attachmentId}/download`),
}

export default clinicalResultApi
