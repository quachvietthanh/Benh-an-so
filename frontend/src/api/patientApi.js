import axiosClient from './axiosClient.js'
import { pageParams, pickFields } from './apiContract.js'

const PATIENT_FIELDS = [
  'fullName', 'dateOfBirth', 'gender', 'phone', 'email', 'address',
  'identityNumber', 'insuranceNumber', 'bloodType', 'emergencyContact', 'emergencyPhone',
]

const PATIENT_SEARCH_FIELDS = [
  'patientCode', 'fullName', 'phone', 'identityNumber', 'insuranceNumber',
  'dateOfBirth', 'gender', 'active',
]

const patientApi = {
  getAll: (params = {}) => axiosClient.get('/patients', {
    params: pageParams(params, PATIENT_SEARCH_FIELDS),
  }),
  getById: (id) => axiosClient.get(`/patients/${id}`),
  getByCode: (code) => axiosClient.get(`/patients/code/${encodeURIComponent(code)}`),
  getHistory: (patientId, params = {}) => axiosClient.get(
    `/medical-history/patients/${patientId}`,
    { params: pageParams(params, ['from', 'to']) },
  ),
  create: (data) => axiosClient.post('/patients', pickFields(data, PATIENT_FIELDS)),
  update: (id, data) => axiosClient.put(`/patients/${id}`, {
    ...pickFields(data, PATIENT_FIELDS),
    active: data.active ?? true,
  }),
}

export default patientApi
