import axiosClient from './axiosClient.js'

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
  changePassword: (data) => {
    return axiosClient.post('/auth/change-password', data)
  },
}

export default authApi
