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
} from '@ant-design/icons'

const clone = (value) => JSON.parse(JSON.stringify(value))

export const roleRoutes = {
  admin: ['/', '/patients', '/appointments', '/medical-records', '/clinical-orders', '/prescriptions', '/pharmacy', '/billing', '/reports', '/system-management', '/users', '/services', '/public-lookup'],
  manager: ['/', '/patients', '/medical-records', '/clinical-orders', '/prescriptions', '/pharmacy', '/billing', '/reports', '/services'],
  doctor: ['/', '/patients', '/appointments', '/medical-records', '/clinical-orders', '/prescriptions'],
  receptionist: ['/', '/patients', '/appointments', '/clinical-orders', '/billing'],
  pharmacist: ['/', '/pharmacy', '/prescriptions'],
}

export const getNavigationItems = (roles = []) => {
  const allItems = [
    { key: '/', label: 'Tổng quan', icon: DashboardOutlined, roles: ['admin', 'manager', 'doctor', 'receptionist', 'pharmacist'] },
    { key: '/patients', label: 'Quản lý hồ sơ bệnh nhân', icon: UserOutlined, roles: ['admin', 'manager', 'doctor', 'receptionist'] },
    { key: '/appointments', label: 'Lịch hẹn và hàng đợi khám', icon: CalendarOutlined, roles: ['admin', 'doctor', 'receptionist'] },
    { key: '/medical-records', label: 'Khám bệnh & bệnh án điện tử', icon: FileTextOutlined, roles: ['admin', 'manager', 'doctor'] },
    { key: '/prescriptions', label: 'Kê đơn thuốc & cảnh báo TT', icon: MedicineBoxOutlined, roles: ['admin', 'manager', 'doctor', 'pharmacist'] },
    { key: '/clinical-orders', label: 'Chỉ định cận lâm sàng', icon: ExperimentOutlined, roles: ['admin', 'manager', 'doctor', 'receptionist'] },
    { key: '/pharmacy', label: 'Quản lý kho thuốc & cấp phát', icon: MedicineBoxOutlined, roles: ['admin', 'manager', 'pharmacist'] },
    { key: '/billing', label: 'Thu phí & hóa đơn', icon: DollarCircleOutlined, roles: ['admin', 'manager', 'receptionist'] },
    { key: '/reports', label: 'Báo cáo vận hành & nhật ký', icon: BarChartOutlined, roles: ['admin', 'manager'] },
    { key: '/system-management', label: 'Quản trị hệ thống & dịch vụ', icon: SettingOutlined, roles: ['admin'] },
    { key: '/public-lookup', label: 'Cổng tra cứu công khai', icon: SafetyCertificateOutlined, roles: ['admin', 'manager', 'doctor', 'receptionist', 'pharmacist'] },
  ]

  return allItems.filter((item) => item.roles.some((role) => roles.includes(role)))
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
