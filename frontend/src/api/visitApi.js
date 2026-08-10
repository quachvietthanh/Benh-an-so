import axiosClient from './axiosClient'

const visitApi = {
  getEncounter: (visitId) => axiosClient.get(`/visits/${visitId}/encounter`),
}

export default visitApi
