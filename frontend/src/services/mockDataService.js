import {
  demoUsers,
  demoPatients,
  demoAppointments,
  demoMedicalRecords,
  demoMedicines,
  demoInvoices,
  demoAuditLogs,
  demoServices,
  demoPrescriptions,
  drugInteractions,
} from '../mock-data/mockData'
import {
  DashboardOutlined,
  UserOutlined,
  CalendarOutlined,
  FileTextOutlined,
  MedicineBoxOutlined,
  DollarCircleOutlined,
  BarChartOutlined,
  SettingOutlined,
  SafetyCertificateOutlined,
  BellOutlined,
  PaperClipOutlined,
  ExperimentOutlined,
  FileDoneOutlined,
} from '@ant-design/icons'

const clone = (value) => JSON.parse(JSON.stringify(value))

export const roleRoutes = {
  admin: ['/', '/patients', '/appointments', '/medical-records', '/prescriptions', '/clinical-orders', '/clinical-results', '/results', '/pharmacy', '/billing', '/reports', '/system-management', '/users', '/services', '/public-lookup'],
  doctor: ['/', '/patients', '/appointments', '/medical-records', '/clinical-orders', '/clinical-results', '/results', '/prescriptions'],
  nurse: ['/', '/medical-records'],
  receptionist: ['/', '/patients', '/appointments', '/billing'],
  pharmacist: ['/', '/pharmacy', '/prescriptions'],
}

export const getNavigationItems = (roles = []) => {
  const normalizedUserRoles = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)

  const allItems = [
    { key: '/', label: 'Tổng quan', icon: DashboardOutlined, roles: ['admin', 'doctor', 'nurse', 'receptionist', 'pharmacist'] },
    { key: '/patients', label: 'Quản lý hồ sơ bệnh nhân', icon: UserOutlined, roles: ['admin', 'manager', 'doctor', 'receptionist'] },
    { key: '/appointments', label: 'Lịch hẹn và hàng đợi khám', icon: CalendarOutlined, roles: ['admin', 'doctor', 'receptionist'] },
    { key: '/medical-records', label: 'Khám bệnh & bệnh án điện tử', icon: FileTextOutlined, roles: ['admin', 'doctor', 'nurse'] },
    { key: '/prescriptions', label: 'Kê đơn thuốc', icon: MedicineBoxOutlined, roles: ['admin', 'doctor', 'pharmacist'] },
    { key: '/clinical-orders', label: 'Chỉ định cận lâm sàng', icon: ExperimentOutlined, roles: ['admin', 'doctor'] },
    { key: '/clinical-results', label: 'Nhập kết quả CĐLS', icon: FileDoneOutlined, roles: ['admin', 'doctor'] },
    { key: '/pharmacy', label: 'Quản lý kho thuốc & cấp phát', icon: MedicineBoxOutlined, roles: ['admin', 'pharmacist'] },
    { key: '/billing', label: 'Thu phí & hóa đơn', icon: DollarCircleOutlined, roles: ['admin', 'receptionist'] },
    { key: '/reports', label: 'Báo cáo vận hành & nhật ký', icon: BarChartOutlined, roles: ['admin'] },
    { key: '/system-management', label: 'Quản trị hệ thống & dịch vụ', icon: SettingOutlined, roles: ['admin'] },
    { key: '/public-lookup', label: 'Cổng tra cứu công khai', icon: SafetyCertificateOutlined, roles: ['admin', 'doctor', 'nurse', 'receptionist', 'pharmacist'] },
  ]

  if (!normalizedUserRoles.length) return allItems

  return allItems.filter((item) => item.roles.some((role) => normalizedUserRoles.includes(role)))
}

export const loginUser = ({ username, password }) => {
  const found = demoUsers.find((user) => user.username === username && user.password === password)
  if (!found) {
    throw new Error('Tên đăng nhập hoặc mật khẩu không đúng')
  }

  return {
    ...clone(found),
    token: 'demo-token',
  }
}

export const getDashboardStats = () => {
  const totalPatients = demoPatients.length
  const todayAppointments = demoAppointments.filter((item) => item.date === '2026-03-30').length
  const pendingExaminations = demoAppointments.filter((item) => item.status === 'CHECKED_IN' || item.status === 'CALLED').length
  const totalRevenue = demoInvoices.reduce((sum, item) => sum + item.totalAmount, 0)

  return {
    totalPatients,
    todayAppointments,
    pendingExaminations,
    totalRevenue,
  }
}

export const getPatients = () => clone(demoPatients)
export const getAppointments = () => clone(demoAppointments)
export const getMedicalRecords = () => clone(demoMedicalRecords)
export const getMedicines = () => clone(demoMedicines)
export const getInvoices = () => clone(demoInvoices)
export const getAuditLogs = () => clone(demoAuditLogs)
export const getServices = () => clone(demoServices)
export const getPrescriptions = () => clone(demoPrescriptions)
export const getDrugInteractions = () => clone(drugInteractions)
