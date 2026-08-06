import axiosClient from './axiosClient.js'
import { pageParams, pickFields } from './apiContract.js'

const RECORD_FIELDS = [
  'visitId', 'chiefComplaint', 'symptoms', 'medicalHistory', 'physicalExamination',
  'clinicalProgress', 'treatmentPlan', 'doctorInstructions', 'conclusion',
]

const medicalRecordApi = {
  getById: (id) => axiosClient.get(`/medical-records/${id}`),
  getByVisit: (visitId) => axiosClient.get(`/medical-records/visits/${visitId}`),
  getByPatient: (patientId) => axiosClient.get(`/medical-records/patient/${patientId}`),
  create: (data) => axiosClient.post('/medical-records', pickFields(data, RECORD_FIELDS)),
  update: (id, data) => axiosClient.put(
    `/medical-records/${id}`,
    pickFields(data, RECORD_FIELDS.filter((field) => field !== 'visitId')),
  ),
  getDiagnoses: (id) => axiosClient.get(`/medical-records/${id}/diagnoses`),
  replaceDiagnoses: (id, data) => axiosClient.put(`/medical-records/${id}/diagnoses`, data),
  recordDiagnosis: (id, data) => axiosClient.put(`/medical-records/${id}/diagnoses`, data),
  getDiagnosis: (id) => axiosClient.get(`/medical-records/${id}/diagnoses`),
  getDiagnosisCatalog: (search) => axiosClient.get('/diagnosis-catalog', {
    params: search ? { search } : {},
  }),
  lock: (id) => axiosClient.post(`/medical-records/${id}/lock`),
  amend: (id, content, reason) => axiosClient.post(
    `/medical-records/${id}/amendments`,
    { content, reason },
  ),
  getAccessLogs: (id, params = {}) => axiosClient.get(
    `/medical-records/${id}/access-logs`,
    { params: pageParams(params, ['from', 'to']) },
  ),
  getPatientAccessLogs: (patientId, params = {}) => axiosClient.get(
    '/medical-records/access-logs',
    { params: { patientId, ...pageParams(params, ['from', 'to']) } },
  ),
}

export default medicalRecordApi
