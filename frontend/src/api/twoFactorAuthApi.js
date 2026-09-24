import axiosClient from './axiosClient.js'

const twoFactorAuthApi = {
  verify: (twoFactorToken, code) =>
    axiosClient.post('/auth/2fa/verify', { twoFactorToken, code }),
  resend: (twoFactorToken) =>
    axiosClient.post('/auth/2fa/resend', { twoFactorToken }),
  configureRole: (roleName, enabled) =>
    axiosClient.patch(`/admin/two-factor-auth/roles/${roleName}`, { enabled }),
}

export default twoFactorAuthApi
