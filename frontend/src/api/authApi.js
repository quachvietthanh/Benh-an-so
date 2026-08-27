import axiosClient from './axiosClient'

const authApi = {
  login: (credentials) => {
    return axiosClient.post('/auth/login', credentials)
  },
  patientLogin: (credentials) => {
    return axiosClient.post('/auth/patient/login', credentials)
  },
  patientRegister: (data) => {
    return axiosClient.post('/auth/patient/register', data)
  },
}

export default authApi
