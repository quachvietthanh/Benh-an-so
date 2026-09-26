import axiosClient from './axiosClient.js'

export const prescriptionDispenseApi = {
  partialDispense: (prescriptionId, items) =>
    axiosClient.post(`/prescriptions/${prescriptionId}/partial-dispense`, { items }),
  getDispenseSuggestion: (prescriptionId) =>
    axiosClient.get(`/prescriptions/${prescriptionId}/dispense-suggestion`),
  dispenseAll: (prescriptionId) =>
    axiosClient.post(`/prescriptions/${prescriptionId}/dispense`),
  getHistory: (prescriptionId) =>
    axiosClient.get(`/prescriptions/${prescriptionId}/dispense-history`),
}

export default prescriptionDispenseApi
