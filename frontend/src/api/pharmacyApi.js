import axiosClient from './axiosClient'

const pharmacyApi = {
  medicines: (params) => axiosClient.get('/medicines', { params }),
  prescriptions: () => axiosClient.get('/prescriptions'),
  getByMedicalRecord: (medicalRecordId) => axiosClient.get(`/prescriptions/medical-records/${medicalRecordId}`),
  getById: (id) => axiosClient.get(`/prescriptions/${id}`),
  checkInteractions: (drugIds) => axiosClient.post('/prescriptions/check-interactions', { drugIds }),
  createPrescription: (data) => axiosClient.post('/prescriptions', data),
  updatePrescription: (id, data) => axiosClient.patch(`/prescriptions/${id}`, data),
  cancelPrescription: (id) => axiosClient.post(`/prescriptions/${id}/cancel`),
  createMedicine: (data) => axiosClient.post('/medicines', data),
  updateMedicine: (id, data) => axiosClient.put(`/medicines/${id}`, data),
  updateMedicineStatus: (id, active) => axiosClient.patch(`/medicines/${id}/status`, { active }),
  dispense: (id) => axiosClient.post(`/prescriptions/${id}/dispense`),
}

export default pharmacyApi
