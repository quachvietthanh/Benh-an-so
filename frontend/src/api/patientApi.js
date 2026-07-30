import axiosClient from './axiosClient'

const patientApi = {
  getAll: (params) => {
    return axiosClient.get('/patients', { params })
  },
  getById: async (id) => {
    try {
      return await axiosClient.get(`/patients/${id}`)
    } catch (err) {
      const res = await axiosClient.get('/patients', { params: { size: 100 } })
      const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
      const found = list.find((p) => String(p.id) === String(id) || String(p.patientCode) === String(id))
      return { data: found }
    }
  },
  getByCode: (code) => {
    return axiosClient.get(`/patients/code/${code}`)
  },
  getHistory: (id, params) => {
    return axiosClient.get(`/patients/${id}/history`, { params })
  },
  create: (data) => {
    return axiosClient.post('/patients', data)
  },
  update: (id, data) => {
    return axiosClient.put(`/patients/${id}`, data)
  },
  delete: (id) => {
    return axiosClient.delete(`/patients/${id}`)
  },
}

export default patientApi
