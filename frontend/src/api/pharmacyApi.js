import axiosClient from './axiosClient.js'
import { pickFields } from './apiContract.js'
import {
  mergeBatches,
  mergeMedicines,
  mergePrescriptions,
  saveStoredBatch,
  saveStoredMedicine,
} from '../utils/storageHelpers.js'

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

const itemPayload = (item = {}) => pickFields(item, [
  'medicineId', 'dosage', 'frequency', 'route', 'durationDays', 'quantity', 'instructions',
])

const prescriptionPayload = (data = {}, includeChangeReason = false) => ({
  ...pickFields(data, [
    'medicalRecordId', 'note', ...(includeChangeReason ? ['changeReason'] : []),
  ]),
  items: Array.isArray(data.items) ? data.items.map(itemPayload) : [],
  interactionOverrides: Array.isArray(data.interactionOverrides)
    ? data.interactionOverrides
      .filter((item) => UUID_PATTERN.test(String(item.ruleId || '')))
      .map((item) => pickFields(item, ['ruleId', 'overrideReason']))
    : [],
})

const pharmacyApi = {
  medicines: async () => {
    try {
      const res = await axiosClient.get('/pharmacy/medicines')
      return res
    } catch {
      return { data: mergeMedicines([]) }
    }
  },
  batches: async () => {
    try {
      const res = await axiosClient.get('/pharmacy/batches')
      return res
    } catch {
      return { data: mergeBatches([]) }
    }
  },
  prescriptions: async () => {
    try {
      const res = await axiosClient.get('/prescriptions')
      return res
    } catch {
      return { data: mergePrescriptions([]) }
    }
  },
  getPrescriptionById: (id) => axiosClient.get(`/prescriptions/${id}`),
  getById: (id) => axiosClient.get(`/prescriptions/${id}`),
  getByMedicalRecord: (medicalRecordId) => axiosClient.get(`/prescriptions/medical-records/${medicalRecordId}`),
  interactions: (medicineIds) => axiosClient.post('/prescriptions/check-interactions', { drugIds: medicineIds }),
  checkInteractions: (medicineIds) => axiosClient.post('/prescriptions/check-interactions', { drugIds: medicineIds }),
  createPrescription: (data) => axiosClient.post('/prescriptions', prescriptionPayload(data)),
  updatePrescription: (id, data) => axiosClient.patch(
    `/prescriptions/${id}`,
    prescriptionPayload(data, true),
  ),
  cancelPrescription: (id) => axiosClient.post(`/prescriptions/${id}/cancel`),
  dispense: (id) => axiosClient.post(`/prescriptions/${id}/dispense`),
  createMedicine: async (data) => {
    try {
      return await axiosClient.post('/pharmacy/medicines', data)
    } catch {
      const created = { id: `local-medicine-${Date.now()}`, ...data }
      saveStoredMedicine(created)
      return { data: created }
    }
  },
  updateMedicine: async (id, data) => {
    try {
      return await axiosClient.put(`/pharmacy/medicines/${id}`, data)
    } catch {
      const existing = mergeMedicines([]).find((item) => String(item.id) === String(id)) || {}
      const updated = { ...existing, ...data, id }
      saveStoredMedicine(updated)
      return { data: updated }
    }
  },
  receiveBatch: async (data) => {
    try {
      return await axiosClient.post('/pharmacy/batches', data)
    } catch {
      const created = { id: `local-batch-${Date.now()}`, ...data }
      saveStoredBatch(created)
      return { data: created }
    }
  },
}

export default pharmacyApi
