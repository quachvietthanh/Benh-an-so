import axiosClient from './axiosClient.js'

const appointmentEffectivenessReportApi = {
  getReport: (params) =>
    axiosClient.get('/reports/appointment-effectiveness', { params }),
}

export default appointmentEffectivenessReportApi
