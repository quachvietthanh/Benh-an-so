import axiosClient from './axiosClient'

const pharmacyApi = {
  medicines: (params) => axiosClient.get('/medicines', { params: { size: 200, ...params } }),
  prescriptions: (params = { status: 'PENDING_DISPENSE' }) => axiosClient.get('/prescriptions', { params }),
  getByMedicalRecord: (medicalRecordId) => axiosClient.get(`/prescriptions/medical-records/${medicalRecordId}`),
  getById: (id) => axiosClient.get(`/prescriptions/${id}`),
  printPrescription: (id) => axiosClient.get(`/prescriptions/${id}/print`, { responseType: 'blob' }),
  checkInteractions: (drugIds) => axiosClient.post('/prescriptions/check-interactions', { drugIds }),
  createPrescription: (data) => axiosClient.post('/prescriptions', data),
  updatePrescription: (id, data) => axiosClient.patch(`/prescriptions/${id}`, data),
  cancelPrescription: (id) => axiosClient.post(`/prescriptions/${id}/cancel`),
  printPrescription: (id) => axiosClient.get(`/prescriptions/${id}/print`, { responseType: 'blob' }),
  stocks: (params) => axiosClient.get('/inventory/stocks', { params }),
  lowStock: () => axiosClient.get('/inventory/low-stock'),
  batches: (params) => axiosClient.get('/inventory/batches', { params }),
  createMedicine: (data) => axiosClient.post('/medicines', data),
  updateMedicine: (id, data) => axiosClient.put(`/medicines/${id}`, data),
  updateMedicineStatus: (id, active) => axiosClient.patch(`/medicines/${id}/status`, { active }),
  receiveBatch: (data) => axiosClient.post('/inventory/receipts', data),
  dispense: (id) => axiosClient.post(`/prescriptions/${id}/dispense`),
  expiryAlerts: (params) => axiosClient.get('/inventory/expiry-alerts', { params }),
}

export default pharmacyApi
