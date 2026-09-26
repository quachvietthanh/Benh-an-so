import axiosClient from './axiosClient.js'

const BASE_URL = '/system/diagnosis-catalog'

const diagnosisCatalogApi = {
  search: async (params = {}) => {
    if (params.search !== undefined || (params.diseaseGroup !== undefined && params.keyword === undefined)) {
      return axiosClient.get('/diagnosis-catalog', { params })
    }

    try {
      return await axiosClient.get(BASE_URL, { params })
    } catch (err) {
      if (err?.response?.status === 403) {
        const fallbackRes = await axiosClient.get('/diagnosis-catalog', {
          params: {
            ...params,
            search: params.search || params.keyword || '',
          },
        })
        return {
          ...fallbackRes,
          isReadOnly: true,
        }
      }
      throw err
    }
  },

  getSuggestions: async () => {
    try {
      return await axiosClient.get('/diagnosis-catalog/suggestions')
    } catch (err) {
      console.warn('Endpoint /diagnosis-catalog/suggestions gặp lỗi hoặc chưa sẵn sàng:', err?.response?.status)
      try {
        const fallbackRes = await axiosClient.get('/diagnosis-catalog', { params: { search: '' } })
        const list = Array.isArray(fallbackRes.data) ? fallbackRes.data : []
        return {
          data: {
            recent: [],
            popular: list.slice(0, 15),
            diseaseGroups: [],
          },
        }
      } catch {
        return {
          data: {
            recent: [],
            popular: [],
            diseaseGroups: [],
          },
        }
      }
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
