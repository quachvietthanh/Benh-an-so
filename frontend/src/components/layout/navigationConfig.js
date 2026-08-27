import {
  AppstoreOutlined,
  CalendarOutlined,
  CloudServerOutlined,
  CopyOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  FormOutlined,
  HeartOutlined,
  HistoryOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  SettingOutlined,
  ShopOutlined,
  SolutionOutlined,
  UserOutlined,
} from '@ant-design/icons'

export const roleNames = {
  admin: 'Quản trị viên',
  manager: 'Quản lý',
  clinic_manager: 'Quản lý phòng khám',
  doctor: 'Bác sĩ',
  receptionist: 'Lễ tân',
  pharmacist: 'Dược sĩ',
}

export const navigationSections = [
  { key: 'overview', paths: ['/'] },
  { key: 'reception', label: 'Tiếp nhận & Chăm sóc', paths: ['/patients', '/appointments', '/after-care'] },
  { key: 'examination', label: 'Khám bệnh', paths: ['/medical-records', '/medical-records/version-history', '/medical-records/copy-issuance', '/prescriptions', '/clinical-results', '/results'] },
  { key: 'pharmacy', label: 'Nhà thuốc', paths: ['/pharmacy', '/medicines', '/pharmacy/receipts'] },
  { key: 'finance', label: 'Tài chính', paths: ['/billing'] },
  { key: 'reports', label: 'Báo cáo', paths: ['/reports'] },
  { key: 'system', label: 'Hệ thống & Bảng giá', paths: ['/services', '/system/diagnosis-catalog', '/system/medical-record-templates', '/system-management', '/prescription-interconnections'] },
]

export const getNavigationItems = (roles = [], permissions = []) => {
  const normalizedRoles = (Array.isArray(roles) ? roles : [roles])
    .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  const userPerms = (Array.isArray(permissions) ? permissions : [permissions])
    .map((perm) => String(perm || '').toUpperCase().replace(/^PERMISSION_/, ''))
    .filter(Boolean)

  const hasPerm = (code) => userPerms.includes(code)
  const hasRole = (r) => normalizedRoles.includes(r)
  const isAdmin = hasRole('admin')
  const isDoctor = hasRole('doctor')

  const items = [
    { key: '/', label: 'Tổng quan', icon: DashboardOutlined, check: () => hasPerm('DASHBOARD_OPERATIONAL_READ') || isAdmin || hasRole('manager') || hasRole('clinic_manager') },
    { key: '/patients', label: 'Quản lý hồ sơ bệnh nhân', icon: UserOutlined, check: () => !isAdmin && (hasPerm('PATIENT_READ') || hasPerm('PATIENT_CREATE') || isDoctor || hasRole('receptionist')) },
    { key: '/appointments', label: 'Lịch hẹn và hàng đợi khám', icon: CalendarOutlined, check: () => !isAdmin && (hasPerm('APPOINTMENT_READ') || hasPerm('APPOINTMENT_CREATE') || hasPerm('QUEUE_VIEW') || isDoctor || hasRole('receptionist')) },
    { key: '/after-care', label: 'Chăm sóc sau khám', icon: HeartOutlined, check: () => !isAdmin && !isDoctor && (hasPerm('FOLLOW_UP_REMINDER_READ') || hasPerm('CARE_LOG_READ') || hasRole('receptionist')) },
    { key: '/medical-records', label: 'Khám bệnh & Bệnh án', icon: SolutionOutlined, check: () => !isAdmin && (hasPerm('MEDICAL_RECORD_READ') || hasPerm('MEDICAL_RECORD_CREATE') || isDoctor) },
    { key: '/medical-records/version-history', label: 'Lịch sử phiên bản bệnh án', icon: HistoryOutlined, check: () => isAdmin || hasRole('manager') || hasRole('clinic_manager') || hasPerm('MEDICAL_RECORD_VERSION_HISTORY_READ') || hasPerm('AUDIT_READ') },
    { key: '/medical-records/copy-issuance', label: 'Cấp bản sao hồ sơ', icon: CopyOutlined, check: () => isAdmin || hasRole('manager') || hasRole('clinic_manager') || hasPerm('REPORT_EXPORT') },
    { key: '/prescriptions', label: 'Kê đơn thuốc', icon: FormOutlined, check: () => !isAdmin && (hasPerm('PRESCRIPTION_READ') || hasPerm('PRESCRIPTION_CREATE') || isDoctor) },
    { key: '/clinical-results', label: 'Nhập kết quả CĐLS', icon: FileTextOutlined, check: () => !isAdmin && (hasPerm('CLINICAL_RESULT_READ') || hasPerm('CLINICAL_RESULT_CREATE') || isDoctor) },
    { key: '/pharmacy', label: 'Cấp phát thuốc', icon: MedicineBoxOutlined, check: () => !isAdmin && !isDoctor && (hasPerm('PHARMACY_READ') || hasRole('pharmacist')) },
    { key: '/medicines', label: 'Danh mục & Ngưỡng tồn', icon: ShopOutlined, check: () => !isAdmin && !isDoctor && (hasPerm('PHARMACY_READ') || hasRole('pharmacist')) },
    { key: '/pharmacy/receipts', label: 'Nhập kho theo lô', icon: InboxOutlined, check: () => !isAdmin && !isDoctor && (hasPerm('PHARMACY_CREATE') || hasRole('pharmacist')) },
    { key: '/billing', label: 'Thu phí & hóa đơn', icon: FileTextOutlined, check: () => !isAdmin && !isDoctor && (hasPerm('INVOICE_READ') || hasPerm('INVOICE_CREATE') || hasRole('manager') || hasRole('receptionist')) },
    { key: '/reports', label: 'Báo cáo vận hành', icon: FileTextOutlined, check: () => hasPerm('REPORT_VIEW') || isAdmin || hasRole('manager') },
    { key: '/services', label: 'Danh mục dịch vụ & giá', icon: AppstoreOutlined, check: () => hasPerm('SERVICE_CATALOG_READ') || isAdmin || hasRole('manager') || hasRole('clinic_manager') },
    { key: '/system/diagnosis-catalog', label: 'Danh mục mã bệnh (ICD-10)', icon: ExperimentOutlined, check: () => hasPerm('DIAGNOSIS_CATALOG_MANAGE') || hasPerm('SERVICE_CATALOG_READ') || isAdmin || hasRole('manager') || hasRole('clinic_manager') },
    { key: '/system/medical-record-templates', label: 'Mẫu bệnh án chuyên khoa', icon: FileTextOutlined, check: () => hasPerm('MEDICAL_RECORD_TEMPLATE_MANAGE') || isAdmin || hasRole('manager') || hasRole('clinic_manager') },
    { key: '/prescription-interconnections', label: 'Liên thông đơn thuốc', icon: CloudServerOutlined, check: () => hasPerm('PRESCRIPTION_INTERCONNECTION_READ') || isAdmin },
    { key: '/system-management', label: 'Quản trị hệ thống', icon: SettingOutlined, check: () => hasPerm('ROLE_READ') || hasPerm('CLINIC_CONFIGURATION_READ') || hasPerm('USER_READ') || hasPerm('AUDIT_READ') || hasPerm('BACKUP_READ') || hasPerm('SERVICE_CATALOG_READ') || isAdmin || hasRole('manager') || hasRole('clinic_manager') },
  ]

  return items.filter((item) => item.check())
}

export { getDefaultHomePath } from '../../utils/roleRouting.js'


