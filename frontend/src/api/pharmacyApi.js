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
  // Backend có nghiệp vụ đơn thuốc nhưng chưa có API danh mục/tồn kho thuốc.
  medicines: async () => ({ data: mergeMedicines([]) }),
  batches: async () => ({ data: mergeBatches([]) }),
  prescriptions: async () => ({ data: mergePrescriptions([]) }),
  getPrescriptionById: (id) => axiosClient.get(`/prescriptions/${id}`),
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
    const created = { id: `local-medicine-${Date.now()}`, ...data }
    saveStoredMedicine(created)
    return { data: created }
  },
  updateMedicine: async (id, data) => {
    const existing = mergeMedicines([]).find((item) => String(item.id) === String(id)) || {}
    const updated = { ...existing, ...data, id }
    saveStoredMedicine(updated)
    return { data: updated }
  },
  receiveBatch: async (data) => {
    const created = { id: `local-batch-${Date.now()}`, ...data }
    saveStoredBatch(created)
    return { data: created }
  },
}

export default pharmacyApi
