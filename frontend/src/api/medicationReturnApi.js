import axiosClient from './axiosClient.js'

const medicationReturnApi = {
  returnMedication: (prescriptionId, reasonOrPayload, maybeItems) => {
    let payload = {}
    if (reasonOrPayload && typeof reasonOrPayload === 'object' && !Array.isArray(reasonOrPayload)) {
      payload = {
        reason: reasonOrPayload.reason,
        items: reasonOrPayload.items,
      }
    } else {
      payload = {
        reason: reasonOrPayload,
        items: maybeItems,
      }
    }
    return axiosClient.post(`/prescriptions/${prescriptionId}/return`, payload)
  },
}

export default medicationReturnApi
