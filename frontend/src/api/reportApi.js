import axiosClient from './axiosClient'

const reportApi = {
  summary: (params) => axiosClient.get('/reports/summary', { params }),
  timeline: (params) => axiosClient.get('/reports/visits-timeline', { params }),
  export: (params) =>
    axiosClient.get('/reports/export', {
      params,
      responseType: 'blob',
    }),
  exportReport: ({ reportType, from, to }) =>
    axiosClient.get('/reports/export', {
      params: { reportType, from, to },
      responseType: 'blob',
    }),
  topMedicines: (params) => axiosClient.get('/reports/top-medicines', { params }),
  doctorVisits: (params) => axiosClient.get('/reports/doctor-visits', { params }),
  audit: (params) => axiosClient.get('/reports/audit-logs', { params }),
  dashboard: () => axiosClient.get('/reports/dashboard'),
}

export default reportApi
