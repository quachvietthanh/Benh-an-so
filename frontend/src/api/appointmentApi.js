import axiosClient from './axiosClient'

const DEFAULT_DOCTORS = [
  { id: 'u3', fullName: 'BS. Phạm Hồng Anh', username: 'doctor', role: 'DOCTOR', department: 'Nội tổng quát' },
  { id: 'd1', fullName: 'BS. Nguyễn Văn Minh', username: 'minh.doctor', role: 'DOCTOR', department: 'Tim mạch' },
  { id: 'd2', fullName: 'BS. Trần Thị Hoa', username: 'hoa.doctor', role: 'DOCTOR', department: 'Nhi khoa' },
  { id: 'd3', fullName: 'BS. Lê Hoài Nam', username: 'nam.doctor', role: 'DOCTOR', department: 'Tai Mũi Họng' },
  { id: 'd4', fullName: 'BS. Vũ Đức Cường', username: 'cuong.doctor', role: 'DOCTOR', department: 'Chấn thương chỉnh hình' },
]

const appointmentApi = {
  getAll: () => axiosClient.get('/appointments'),
  getDoctors: async () => {
    try {
      const res = await axiosClient.get('/users')
      if (res && Array.isArray(res.data) && res.data.length > 0) {
        const doctors = res.data.filter((u) => {
          const role = String(u.role || u.roleName || '').toUpperCase()
          return role.includes('DOCTOR') || role.includes('BÁC SĨ') || role === 'DOCTOR'
        })
        if (doctors.length > 0) {
          return { data: doctors }
        }
        return { data: res.data }
      }
    } catch (err) {
      console.warn('Backend /users endpoint unavailable, falling back to default doctors list:', err?.message)
    }
    return { data: DEFAULT_DOCTORS }
  },
  create: (data) => axiosClient.post('/appointments', data),
  cancel: (id, reason) => axiosClient.patch(`/appointments/${id}/cancel`, { reason }),
  noShow: (id) => axiosClient.patch(`/appointments/${id}/no-show`),
  checkIn: (id) => axiosClient.post(`/appointments/${id}/check-in`),
}

export default appointmentApi

