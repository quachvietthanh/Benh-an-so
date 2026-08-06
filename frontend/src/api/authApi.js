import axiosClient from './axiosClient.js'

const authApi = {
  login: (credentials) => axiosClient.post('/auth/login', credentials),
  logout: (accessToken) => axiosClient.post('/auth/logout', null, {
    headers: { Authorization: `Bearer ${accessToken}` },
  }),
  refresh: (refreshToken) => axiosClient.post('/auth/refresh', { refreshToken }),
}

export default authApi
