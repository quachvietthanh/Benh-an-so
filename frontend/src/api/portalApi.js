import publicApiClient from './publicApiClient.js'

const portalApi = {
  lookup: ({ code, phone }) => {
    const params = { code: code ? String(code).trim() : '' }
    const trimmedPhone = phone ? String(phone).trim() : ''
    if (trimmedPhone) {
      params.phone = trimmedPhone
    }
    return publicApiClient.get('/portal/lookup', { params })
  },
}

export default portalApi
