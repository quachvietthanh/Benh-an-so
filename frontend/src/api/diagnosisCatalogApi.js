import axiosClient from './axiosClient.js'


const BASE_URL = '/system/diagnosis-catalog'

const diagnosisCatalogApi = {
  /**
   * Search / list diagnosis catalog entries with optional filters.
   * Falls back to /diagnosis-catalog for read access if 403 forbidden.
   * @param {Object} params - { keyword?: string, active?: boolean }
   */
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


  /**
   * Get single diagnosis catalog entry by ID.
   * @param {string} id - UUID
   */
  getById: (id) => {
    return axiosClient.get(`${BASE_URL}/${id}`)
  },

  /**
   * Create new diagnosis catalog entry.
   * @param {Object} data - { code: string, name: string, diseaseGroup: string, description?: string }
   */
  create: (data) => {
    return axiosClient.post(BASE_URL, data)
  },

  /**
   * Update diagnosis catalog entry.
   * @param {string} id - UUID
   * @param {Object} data - { name: string, diseaseGroup: string, description?: string }
   */
  update: (id, data) => {
    return axiosClient.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * Toggle / update status (active or inactive) for diagnosis code.
   * @param {string} id - UUID
   * @param {boolean} active - true for active, false for inactive
   */
  updateStatus: (id, active) => {
    return axiosClient.patch(`${BASE_URL}/${id}/status`, { active })
  },

  /**
   * Delete diagnosis catalog entry by ID.
   * @param {string} id - UUID
   */
  delete: (id) => {
    return axiosClient.delete(`${BASE_URL}/${id}`)
  },
}

export default diagnosisCatalogApi
