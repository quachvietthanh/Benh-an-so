import axiosClient from './axiosClient.js'

const inventoryStockReportApi = {
  getReport: (params) =>
    axiosClient.get('/inventory/report/stock-in-out', { params }),
  exportReport: (params) =>
    axiosClient.get('/inventory/report/stock-in-out/export', {
      params,
      responseType: 'blob',
    }),
}

export default inventoryStockReportApi
