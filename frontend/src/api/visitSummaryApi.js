import axiosClient from './axiosClient.js'

const visitSummaryApi = {
  /**
   * Lấy dữ liệu xem trước phiếu tóm tắt lượt khám (NCL-04-CN-011)
   * @param {string} visitId - Mã định danh duy nhất của lượt khám
   * @returns {Promise} VisitSummaryResponse
   */
  getSummary: (visitId) => {
    return axiosClient.get(`/visits/${visitId}/summary`)
  },

  /**
   * Tải tệp PDF phiếu tóm tắt lượt khám (NCL-04-CN-011)
   * @param {string} visitId - Mã định danh duy nhất của lượt khám
   * @returns {Promise<Blob>}
   */
  downloadPdf: async (visitId, customFilename) => {
    const response = await axiosClient.get(`/visits/${visitId}/summary/print`, {
      responseType: 'blob',
    })

    // Tự động trích xuất tên file từ header Content-Disposition nếu có
    let filename = customFilename
    if (!filename) {
      const disposition = response.headers?.['content-disposition']
      if (disposition && disposition.includes('filename=')) {
        const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition)
        if (matches != null && matches[1]) {
          filename = matches[1].replace(/['"]/g, '').trim()
        }
      }
    }
    if (!filename) {
      filename = `phieu-tom-tat-${visitId.slice(0, 8)}.pdf`
    }

    // Kích hoạt tải tệp trên trình duyệt nếu chạy trong môi trường browser
    if (typeof window !== 'undefined' && window.URL && typeof document !== 'undefined') {
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(downloadUrl)
    }

    return response
  },

  /**
   * Tra cứu danh sách hồ sơ / lượt khám có hỗ trợ in phiếu tóm tắt
   * @param {object} params
   */
  getVisits: async (params = {}) => {
    // Tận dụng API tra cứu bệnh nhân và lịch sử khám bệnh để tổng hợp danh sách
    return axiosClient.get('/patients', { params: { size: 50, ...params } })
  },
}

export default visitSummaryApi
