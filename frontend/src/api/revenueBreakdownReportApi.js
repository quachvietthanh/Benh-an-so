import axiosClient from './axiosClient.js'

const revenueBreakdownReportApi = {
  getRevenueBreakdown: (params) =>
    axiosClient.get('/reports/revenue-breakdown', { params }),
}

export default revenueBreakdownReportApi
