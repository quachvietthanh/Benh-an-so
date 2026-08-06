import axiosClient from './axiosClient.js'

const visitApi = {
  getEncounter: (visitId) => axiosClient.get(`/visits/${visitId}/encounter`),
}

export default visitApi
