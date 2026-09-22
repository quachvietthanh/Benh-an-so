import axiosClient from './axiosClient.js'

const visitApi = {
  getEncounter: (visitId) => axiosClient.get(`/visits/${visitId}/encounter`),
  getHandoverDoctors: () => axiosClient.get('/visits/handover/doctors'),
  handoverPatient: (visitId, data) => axiosClient.post(`/visits/${visitId}/handover`, data),
  getVisitHandovers: (visitId) => axiosClient.get(`/visits/${visitId}/handovers`),
}

export default visitApi
