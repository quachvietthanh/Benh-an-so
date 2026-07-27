import axiosClient from './axiosClient'

const pharmacyApi = {
  medicines: () => axiosClient.get('/pharmacy/medicines'),
  prescriptions: () => axiosClient.get('/prescriptions'),
  interactions: (medicineIds) => axiosClient.post('/prescriptions/interactions', medicineIds),
  createPrescription: (data) => axiosClient.post('/prescriptions', data),
  updatePrescription: (id, data) => axiosClient.put(`/prescriptions/${id}`, data),
  batches: () => axiosClient.get('/pharmacy/batches'),
  createMedicine: (data) => axiosClient.post('/pharmacy/medicines', data),
  updateMedicine: (id, data) => axiosClient.put(`/pharmacy/medicines/${id}`, data),
  receiveBatch: (data) => axiosClient.post('/pharmacy/batches', data),
  dispense: (id) => axiosClient.post(`/pharmacy/prescriptions/${id}/dispense`),
}

export default pharmacyApi
