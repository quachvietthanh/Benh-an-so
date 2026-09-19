import axiosClient from './axiosClient.js'

const diseasePatternReportApi = {
  getReport: (params) => 
    axiosClient.get('/reports/disease-patterns', { params }),
  exportReport: (params) => 
    axiosClient.get('/reports/export', { 
      params: { reportType: 'DISEASE_PATTERN_REPORT', ...params },
      responseType: 'blob',
    }),
}

export default diseasePatternReportApi
