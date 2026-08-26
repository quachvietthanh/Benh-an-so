import axiosClient from './axiosClient.js'

const BASE_URL = '/system/diagnosis-catalog'

const diagnosisCatalogApi = {
  search: async (params = {}) => {
    try {
      return await axiosClient.get(BASE_URL, { params })
    } catch (err) {
      if (err?.response?.status === 403) {
        const fallbackRes = await axiosClient.get('/diagnosis-catalog', {
          params: { search: params.keyword || '' },
        })
        return {
          ...fallbackRes,
          isReadOnly: true,
        }
      }
      throw err
    }
  },

  getById: (id) => {
    return axiosClient.get(`${BASE_URL}/${id}`)
  },

  create: (data) => {
    return axiosClient.post(BASE_URL, data)
  },

  update: (id, data) => {
    return axiosClient.put(`${BASE_URL}/${id}`, data)
  },

  updateStatus: (id, active) => {
    return axiosClient.patch(`${BASE_URL}/${id}/status`, { active })
  },

  delete: (id) => {
    return axiosClient.delete(`${BASE_URL}/${id}`)
  },
}

export default diagnosisCatalogApi
