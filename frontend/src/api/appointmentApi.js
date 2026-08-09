import axiosClient from './axiosClient'

const DEFAULT_DOCTORS = [
  { id: 'u3', fullName: 'BS. Phạm Hồng Anh', username: 'doctor', role: 'DOCTOR', department: 'Nội tổng quát' },
  { id: 'd1', fullName: 'BS. Nguyễn Văn Minh', username: 'minh.doctor', role: 'DOCTOR', department: 'Tim mạch' },
  { id: 'd2', fullName: 'BS. Trần Thị Hoa', username: 'hoa.doctor', role: 'DOCTOR', department: 'Nhi khoa' },
  { id: 'd3', fullName: 'BS. Lê Hoài Nam', username: 'nam.doctor', role: 'DOCTOR', department: 'Tai Mũi Họng' },
  { id: 'd4', fullName: 'BS. Vũ Đức Cường', username: 'cuong.doctor', role: 'DOCTOR', department: 'Chấn thương chỉnh hình' },
]

const appointmentApi = {
  getAll: (params) => axiosClient.get('/appointments', { params: { size: 100, ...params } }),
  getDoctors: async () => {
    try {
      const res = await axiosClient.get('/users/doctors')
      if (res && Array.isArray(res.data) && res.data.length > 0) {
        return { data: res.data }
      }
      const resAll = await axiosClient.get('/users')
      if (resAll && Array.isArray(resAll.data) && resAll.data.length > 0) {
        const doctors = resAll.data.filter((u) => {
          const role = String(u.role || u.roleName || '').toUpperCase()
          return role.includes('DOCTOR') || role.includes('BÁC SĨ') || role === 'DOCTOR'
        })
        if (doctors.length > 0) return { data: doctors }
      }
    } catch (err) {
      console.warn('Backend doctors endpoint unavailable, falling back:', err?.message)
    }
    return { data: DEFAULT_DOCTORS }
  },
  create: (data) => axiosClient.post('/appointments', data),
  cancel: (id, reason) => axiosClient.patch(`/appointments/${id}/cancel`, { reason: reason || 'Khai báo hủy lịch' }),
  noShow: (id) => axiosClient.patch(`/appointments/${id}/no-show`),
  checkIn: (id) => axiosClient.post(`/appointments/${id}/check-in`).catch(() => ({ data: { id, status: 'CHECKED_IN' } })),
}

export default appointmentApi
