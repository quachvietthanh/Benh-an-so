import axiosClient from './axiosClient'

const pharmacyApi = {
  medicines: () => axiosClient.get('/pharmacy/medicines'),
  prescriptions: () => axiosClient.get('/prescriptions'),
  getByMedicalRecord: (medicalRecordId) => axiosClient.get(`/prescriptions/medical-records/${medicalRecordId}`),
  getById: (id) => axiosClient.get(`/prescriptions/${id}`),
  checkInteractions: (drugIds) => axiosClient.post('/prescriptions/check-interactions', { drugIds }),
  interactions: (medicineIds) => axiosClient.post('/prescriptions/check-interactions', { drugIds: medicineIds }),
  createPrescription: (data) => axiosClient.post('/prescriptions', data),
  updatePrescription: (id, data) => axiosClient.patch(`/prescriptions/${id}`, data),
  cancelPrescription: (id) => axiosClient.post(`/prescriptions/${id}/cancel`),
  batches: () => axiosClient.get('/pharmacy/batches'),
  createMedicine: (data) => axiosClient.post('/pharmacy/medicines', data),
  updateMedicine: (id, data) => axiosClient.put(`/pharmacy/medicines/${id}`, data),
  receiveBatch: (data) => axiosClient.post('/pharmacy/batches', data),
  dispense: (id) => axiosClient.post(`/pharmacy/prescriptions/${id}/dispense`),
}

export default pharmacyApi
