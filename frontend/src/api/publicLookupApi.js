import publicApiClient from './publicApiClient.js'

const publicLookupApi = {
  lookupAppointment: (data, config = {}) => (
    publicApiClient.post('/public/appointments/lookup', data, config)
  ),
}

export default publicLookupApi
