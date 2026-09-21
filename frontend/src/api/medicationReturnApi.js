import axiosClient from './axiosClient.js'

const medicationReturnApi = {
  returnMedication: (prescriptionId, reason, items) =>
    axiosClient.post(`/prescriptions/${prescriptionId}/return`, {
      reason,
      items,
    }),
}

export default medicationReturnApi
