import axiosClient from './axiosClient.js'

export const prescriptionDispenseApi = {
  partialDispense: (prescriptionId, items) =>
    axiosClient.post(`/prescriptions/${prescriptionId}/partial-dispense`, { items }),
  getHistory: (prescriptionId) =>
    axiosClient.get(`/prescriptions/${prescriptionId}/dispense-history`),
}

export default prescriptionDispenseApi
