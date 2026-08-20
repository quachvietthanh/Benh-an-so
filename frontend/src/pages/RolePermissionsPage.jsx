import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Empty,
  Input,
  Modal,
  Progress,
  Row,
  Select,
  Space,
  Spin,
  Switch,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  BarChartOutlined,
  BranchesOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DollarOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  FilterOutlined,
  FormOutlined,
  FundViewOutlined,
  HeartOutlined,
  KeyOutlined,
  LockOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SaveOutlined,
  SearchOutlined,
  SecurityScanOutlined,
  SettingOutlined,
  ShopOutlined,
  TeamOutlined,
  UndoOutlined,
  UnorderedListOutlined,
  UserOutlined,
  UsergroupAddOutlined,
} from '@ant-design/icons'
import roleApi from '../api/roleApi'
import { useAuthContext } from '../context/AuthContext'

const { Title, Text, Paragraph } = Typography

// Friendly display mapping for system roles (fallback to role.description || role.name)
const ROLE_DISPLAY_NAMES = {
  ADMIN: 'Quản trị viên',
  DOCTOR: 'Bác sĩ',
  RECEPTIONIST: 'Lễ tân',
  PHARMACIST: 'Dược sĩ',
  MANAGER: 'Quản lý phòng khám',
}

const ROLE_THEMES = {
  ADMIN: { color: 'purple', bg: '#f5f3ff', border: '#ddd6fe', text: '#6d28d9', icon: '👑' },
  DOCTOR: { color: 'blue', bg: '#eff6ff', border: '#bfdbfe', text: '#1d4ed8', icon: '🩺' },
  RECEPTIONIST: { color: 'orange', bg: '#fff7ed', border: '#fed7aa', text: '#c2410c', icon: '📋' },
  PHARMACIST: { color: 'green', bg: '#f0fdf4', border: '#bbf7d0', text: '#15803d', icon: '💊' },
  MANAGER: { color: 'cyan', bg: '#ecfeff', border: '#a5f3fc', text: '#0e7490', icon: '📊' },
}

const MODULE_DISPLAY_NAMES = {
  USER: 'Quản lý Tài khoản & Người dùng',
  ROLE: 'Vai trò & Phân quyền Hệ thống',
  PERMISSION: 'Danh mục Quyền Hệ thống',
  PATIENT: 'Hồ sơ Bệnh nhân',
  MEDICAL_RECORD: 'Hồ sơ Bệnh án & Khám bệnh',
  APPOINTMENT: 'Lịch hẹn Khám bệnh',
  PRESCRIPTION: 'Kê đơn & Cấp phát Đơn thuốc',
  PHARMACY: 'Kho Dược & Quản lý Thuốc',
  VITAL_SIGN: 'Chỉ số Sinh hiệu',
  DIAGNOSIS: 'Chẩn đoán Bệnh (ICD-10)',
  INVOICE: 'Hóa đơn & Thu Viện phí',
  QUEUE: 'Hàng đợi & Điều phối Khám',
  SERVICE_CATALOG: 'Danh mục & Bảng giá Dịch vụ',
  ROOM: 'Phòng khám & Phân công Trực',
  CLINICAL_ORDER: 'Chỉ định Cận lâm sàng',
  CLINICAL_RESULT: 'Kết quả Cận lâm sàng',
  CLINICAL_SERVICE: 'Dịch vụ Cận lâm sàng',
  FOLLOW_UP: 'Nhắc hẹn Tái khám',
  CARE_LOG: 'Nhật ký Chăm sóc Sau khám',
  CLINIC_CONFIGURATION: 'Cấu hình Thông tin Phòng khám',
  BACKUP: 'Sao lưu & Phục hồi Dữ liệu',
  REPORT: 'Báo cáo Doanh thu & Thống kê',
  DASHBOARD: 'Dashboard Tổng quan Vận hành',
  AUDIT: 'Kiểm toán & Nhật ký An toàn',
}

const MODULE_ICONS = {
  USER: <UserOutlined style={{ color: '#2563eb' }} />,
  ROLE: <SafetyCertificateOutlined style={{ color: '#7c3aed' }} />,
  PERMISSION: <KeyOutlined style={{ color: '#d97706' }} />,
  PATIENT: <TeamOutlined style={{ color: '#059669' }} />,
  MEDICAL_RECORD: <FileTextOutlined style={{ color: '#0284c7' }} />,
  APPOINTMENT: <CalendarOutlined style={{ color: '#ea580c' }} />,
  PRESCRIPTION: <MedicineBoxOutlined style={{ color: '#dc2626' }} />,
  PHARMACY: <ShopOutlined style={{ color: '#16a34a' }} />,
  VITAL_SIGN: <HeartOutlined style={{ color: '#e11d48' }} />,
  DIAGNOSIS: <ExperimentOutlined style={{ color: '#9333ea' }} />,
  INVOICE: <DollarOutlined style={{ color: '#ca8a04' }} />,
  QUEUE: <UnorderedListOutlined style={{ color: '#0d9488' }} />,
  SERVICE_CATALOG: <AppstoreOutlined style={{ color: '#4f46e5' }} />,
  ROOM: <BankOutlined style={{ color: '#475569' }} />,
  CLINICAL_ORDER: <FormOutlined style={{ color: '#2563eb' }} />,
  CLINICAL_RESULT: <FundViewOutlined style={{ color: '#0891b2' }} />,
  CLINICAL_SERVICE: <BranchesOutlined style={{ color: '#059669' }} />,
  FOLLOW_UP: <ClockCircleOutlined style={{ color: '#d97706' }} />,
  CARE_LOG: <AuditOutlined style={{ color: '#0284c7' }} />,
  CLINIC_CONFIGURATION: <SettingOutlined style={{ color: '#64748b' }} />,
  BACKUP: <DatabaseOutlined style={{ color: '#7c3aed' }} />,
  REPORT: <BarChartOutlined style={{ color: '#2563eb' }} />,
  DASHBOARD: <DashboardOutlined style={{ color: '#059669' }} />,
  AUDIT: <SecurityScanOutlined style={{ color: '#dc2626' }} />,
}

// Clear Vietnamese explanations for every single technical permission code
const PERMISSION_DETAILS = {
  // USER
  USER_CREATE: { title: 'Tạo tài khoản người dùng mới', desc: 'Thêm tài khoản nhân viên mới và thiết lập mật khẩu ban đầu' },
  USER_READ: { title: 'Xem danh sách tài khoản', desc: 'Tra cứu danh sách tài khoản, email, số điện thoại nhân sự phòng khám' },
  USER_UPDATE: { title: 'Cập nhật thông tin tài khoản', desc: 'Chỉnh sửa họ tên, SĐT, kích hoạt hoặc tạm khóa tài khoản người dùng' },
  USER_DELETE: { title: 'Xóa tài khoản người dùng', desc: 'Xóa tài khoản nhân viên khỏi hệ thống phòng khám' },
  USER_ASSIGN_ROLE: { title: 'Gán vai trò cho tài khoản', desc: 'Phân vai trò (Quản trị viên, Bác sĩ, Lễ tân, Dược sĩ, Quản lý) cho nhân viên' },

  // ROLE
  ROLE_CREATE: { title: 'Tạo nhóm vai trò mới', desc: 'Định nghĩa nhóm vai trò mới trong hệ thống phòng khám' },
  ROLE_READ: { title: 'Xem danh sách vai trò', desc: 'Xem danh sách các vai trò chuẩn và bảng quyền hạn hiện tại' },
  ROLE_UPDATE: { title: 'Điều chỉnh quyền của vai trò', desc: 'Bật / tắt từng quyền chức năng cho các vai trò chuẩn hệ thống' },
  ROLE_DELETE: { title: 'Xóa vai trò', desc: 'Xóa nhóm vai trò khỏi hệ thống phòng khám' },

  // PERMISSION
  PERMISSION_READ: { title: 'Xem danh mục quyền hệ thống', desc: 'Tra cứu danh mục 55 quyền chức năng và phân hệ nghiệp vụ' },

  // PATIENT
  PATIENT_CREATE: { title: 'Tiếp nhận & Tạo hồ sơ bệnh nhân', desc: 'Đăng ký hồ sơ hành chính bệnh nhân mới (Họ tên, SĐT, Ngày sinh, CCCD)' },
  PATIENT_READ: { title: 'Tra cứu hồ sơ bệnh nhân', desc: 'Xem thông tin chi tiết và lịch sử các lần đến khám của người bệnh' },
  PATIENT_UPDATE: { title: 'Cập nhật hồ sơ bệnh nhân', desc: 'Sửa đổi thông tin liên lạc, địa chỉ và thông tin nhân thân bệnh nhân' },
  PATIENT_DELETE: { title: 'Xóa hồ sơ bệnh nhân', desc: 'Xóa hồ sơ bệnh nhân khỏi cơ sở dữ liệu' },

  // MEDICAL_RECORD
  MEDICAL_RECORD_CREATE: { title: 'Tạo hồ sơ bệnh án khám bệnh', desc: 'Mở bệnh án khám mới khi bắt đầu ca khám bệnh' },
  MEDICAL_RECORD_READ: { title: 'Xem hồ sơ bệnh án', desc: 'Bác sĩ xem lý do khám, tiền sử bệnh, quá trình khám và kết luận' },
  MEDICAL_RECORD_UPDATE: { title: 'Cập nhật diễn tiến bệnh án', desc: 'Bác sĩ chỉnh sửa triệu chứng, khám lâm sàng và hướng điều trị' },
  MEDICAL_RECORD_DELETE: { title: 'Hủy / Xóa hồ sơ bệnh án', desc: 'Hủy bệnh án tạo nhầm khi chưa khóa khám' },
  MEDICAL_RECORD_UPDATE_STATUS: { title: 'Khóa bệnh án & Hoàn tất khám', desc: 'Khóa dữ liệu bệnh án và kết thúc ca khám bệnh của người bệnh' },

  // APPOINTMENT
  APPOINTMENT_CREATE: { title: 'Đặt lịch hẹn khám mới', desc: 'Tạo phiếu hẹn ngày giờ khám bệnh cho bệnh nhân' },
  APPOINTMENT_READ: { title: 'Xem danh sách lịch hẹn khám', desc: 'Tra cứu danh sách các ca hẹn khám theo ngày và theo bác sĩ' },
  APPOINTMENT_UPDATE: { title: 'Đổi / Dời lịch hẹn khám', desc: 'Thay đổi thời gian hẹn hoặc bác sĩ phụ trách ca hẹn' },
  APPOINTMENT_DELETE: { title: 'Hủy lịch hẹn khám', desc: 'Hủy ca hẹn khám khi bệnh nhân có yêu cầu hủy' },

  // PRESCRIPTION
  PRESCRIPTION_CREATE: { title: 'Kê đơn thuốc mới cho bệnh nhân', desc: 'Bác sĩ chọn thuốc, liều dùng, số ngày dùng và lưu đơn thuốc' },
  PRESCRIPTION_READ: { title: 'Xem danh sách & chi tiết đơn thuốc', desc: 'Tra cứu thông tin toa thuốc đã kê cho bệnh nhân' },
  PRESCRIPTION_UPDATE: { title: 'Điều chỉnh đơn thuốc', desc: 'Bác sĩ chỉnh sửa số lượng, liều dùng trước khi quầy thuốc cấp phát' },
  PRESCRIPTION_DELETE: { title: 'Hủy đơn thuốc', desc: 'Hủy đơn thuốc khi không còn chỉ định dùng thuốc' },
  PRESCRIPTION_UPDATE_STATUS: { title: 'Cấp phát thuốc cho người bệnh', desc: 'Dược sĩ xác nhận đã xuất thuốc và trừ tồn kho tại quầy dược' },
  PRESCRIPTION_PRINT: { title: 'In và bàn giao đơn thuốc', desc: 'Xuất bản in toa thuốc chuẩn y tế để người bệnh mua thuốc bên ngoài' },

  // PHARMACY
  PHARMACY_CREATE: { title: 'Thêm thuốc mới vào danh mục', desc: 'Khai báo tên thuốc, hoạt chất, đơn vị tính và hàm lượng vào kho' },
  PHARMACY_READ: { title: 'Xem kho thuốc & Tồn kho', desc: 'Tra cứu số lượng tồn khả dụng, hạn dùng và thông tin thuốc' },
  PHARMACY_UPDATE: { title: 'Nhập kho & Cập nhật thuốc', desc: 'Cập nhật lô hàng, số lượng tồn kho và thông tin sử dụng thuốc' },
  PHARMACY_DELETE: { title: 'Ngừng lưu hành / Xóa thuốc', desc: 'Xóa hoặc ngừng kinh doanh thuốc trong danh mục phòng khám' },

  // VITAL_SIGN
  VITAL_SIGN_CREATE: { title: 'Đo & Ghi nhận chỉ số sinh hiệu', desc: 'Nhập mạch, huyết áp, nhiệt độ, SpO2, nhịp thở cho người bệnh' },
  VITAL_SIGN_READ: { title: 'Xem chỉ số sinh hiệu', desc: 'Theo dõi diễn biến các chỉ số sinh hiệu trong quá trình khám' },
  VITAL_SIGN_UPDATE: { title: 'Chỉnh sửa chỉ số sinh hiệu', desc: 'Cập nhật lại kết quả sinh hiệu khi đo lại' },

  // DIAGNOSIS
  DIAGNOSIS_CREATE: { title: 'Ghi nhận chẩn đoán bệnh (ICD-10)', desc: 'Chọn mã bệnh ICD-10 và nhập chẩn đoán xác định / chẩn đoán kèm theo' },
  DIAGNOSIS_READ: { title: 'Xem chẩn đoán bệnh', desc: 'Xem danh sách các chẩn đoán bệnh án của người bệnh' },
  DIAGNOSIS_UPDATE: { title: 'Sửa đổi chẩn đoán bệnh', desc: 'Cập nhật lại chẩn đoán sau khi có thêm kết quả xét nghiệm, CĐHA' },

  // INVOICE
  INVOICE_CREATE: { title: 'Tạo hóa đơn thu tiền khám & thuốc', desc: 'Tạo phiếu thanh toán tiền khám, cận lâm sàng và tiền thuốc' },
  INVOICE_READ: { title: 'Xem hóa đơn & Lịch sử thanh toán', desc: 'Tra cứu thông tin phiếu thu và trạng thái thanh toán của bệnh nhân' },
  INVOICE_UPDATE: { title: 'Thu tiền & Xuất biên lai viện phí', desc: 'Thu ngân xác nhận nhận tiền thanh toán hoặc hoàn phí dịch vụ' },
  INVOICE_DELETE: { title: 'Hủy hóa đơn thanh toán', desc: 'Hủy phiếu thu viện phí khi có sai sót nghiệp vụ' },

  // QUEUE
  QUEUE_CREATE: { title: 'Tiếp nhận & Cấp số thứ tự khám', desc: 'Lễ tân tiếp đón người bệnh và phát số thứ tự vào phòng khám' },
  QUEUE_CALL_NEXT: { title: 'Gọi số tiếp theo vào khám', desc: 'Bác sĩ hoặc Lễ tân gọi bệnh nhân kế tiếp vào phòng khám' },
  QUEUE_UPDATE_STATUS: { title: 'Cập nhật trạng thái hàng đợi', desc: 'Chuyển trạng thái đang khám, bỏ qua vắng mặt, hoàn tất khám' },
  QUEUE_VIEW: { title: 'Xem danh sách hàng đợi khám', desc: 'Theo dõi màn hình điều phối bệnh nhân đang chờ khám tại các phòng' },
  QUEUE_COUNT: { title: 'Đếm số lượng bệnh nhân chờ', desc: 'Xem tổng số ca chờ khám, đang khám và đã khám trong ngày' },

  // SERVICE_CATALOG
  SERVICE_CATALOG_READ: { title: 'Xem bảng giá & Danh mục dịch vụ', desc: 'Tra cứu danh mục các kỹ thuật khám, xét nghiệm và giá niêm yết' },
  SERVICE_CATALOG_CREATE: { title: 'Thêm dịch vụ khám mới', desc: 'Tạo mới gói khám, xét nghiệm hoặc thủ thuật vào bảng giá' },
  SERVICE_CATALOG_UPDATE: { title: 'Cập nhật thông tin dịch vụ', desc: 'Sửa tên dịch vụ, mô tả và khoa phòng thực hiện' },
  SERVICE_PRICE_MANAGE: { title: 'Điều chỉnh bảng giá dịch vụ', desc: 'Thiết lập đơn giá mới và theo dõi lịch sử tăng/giảm giá dịch vụ' },

  // ROOM
  ROOM_READ: { title: 'Xem danh sách phòng khám', desc: 'Tra cứu sơ đồ phòng khám bệnh, phòng thủ thuật và cận lâm sàng' },
  ROOM_CREATE: { title: 'Tạo phòng khám mới', desc: 'Khai báo thêm phòng khám chuyên khoa mới trong phòng khám' },
  ROOM_UPDATE: { title: 'Cập nhật phòng khám', desc: 'Sửa tên phòng, số phòng, trang thiết bị và trạng thái hoạt động' },
  ROOM_ASSIGNMENT_READ: { title: 'Xem lịch trực phòng khám', desc: 'Xem danh sách bác sĩ được phân công trực tại các phòng' },
  ROOM_ASSIGNMENT_UPDATE: { title: 'Phân công bác sĩ trực phòng', desc: 'Gán bác sĩ phụ trách ca trực vào phòng khám tương ứng' },

  // CLINICAL_ORDER
  CLINICAL_ORDER_CREATE: { title: 'Chỉ định cận lâm sàng', desc: 'Bác sĩ ra y lệnh xét nghiệm máu, nước tiểu, X-quang, Siêu âm, ECG' },
  CLINICAL_ORDER_READ: { title: 'Xem danh sách chỉ định', desc: 'Tra cứu danh sách các dịch vụ cận lâm sàng đã chỉ định cho bệnh nhân' },

  // CLINICAL_RESULT
  CLINICAL_RESULT_CREATE: { title: 'Nhập kết quả cận lâm sàng', desc: 'Kỹ thuật viên nhập các chỉ số kết quả xét nghiệm, chẩn đoán hình ảnh' },
  CLINICAL_RESULT_READ: { title: 'Xem kết quả cận lâm sàng', desc: 'Bác sĩ xem kết quả cận lâm sàng để phục vụ chẩn đoán và điều trị' },
  CLINICAL_RESULT_UPDATE: { title: 'Chỉnh sửa kết quả cận lâm sàng', desc: 'Cập nhật chỉ số kết quả khi cần hiệu chỉnh chuyên môn' },
  CLINICAL_RESULT_FINALIZE: { title: 'Duyệt & Khóa kết quả cận lâm sàng', desc: 'Bác sĩ CĐHA/Xét nghiệm ký duyệt chốt kết quả chính thức' },
  CLINICAL_RESULT_ATTACHMENT_CREATE: { title: 'Tải lên ảnh / File đính kèm kết quả', desc: 'Đính kèm ảnh chụp phim X-quang, siêu âm, tài liệu kết quả' },
  CLINICAL_RESULT_ATTACHMENT_READ: { title: 'Xem file đính kèm kết quả', desc: 'Xem và tải về các file phim ảnh, tài liệu đính kèm kết quả' },

  // CLINICAL_SERVICE
  CLINICAL_SERVICE_READ: { title: 'Xem danh mục kỹ thuật cận lâm sàng', desc: 'Tra cứu các kỹ thuật cận lâm sàng có thể thực hiện tại phòng khám' },

  // FOLLOW_UP
  FOLLOW_UP_REMINDER_CREATE: { title: 'Tạo lịch nhắc hẹn tái khám', desc: 'Thiết lập ngày hẹn tái khám và nội dung cần dặn dò người bệnh' },
  FOLLOW_UP_REMINDER_READ: { title: 'Xem danh sách nhắc tái khám', desc: 'Tra cứu các bệnh nhân đến ngày cần gọi điện/nhắn tin nhắc tái khám' },
  FOLLOW_UP_REMINDER_UPDATE: { title: 'Cập nhật lịch nhắc tái khám', desc: 'Thay đổi ngày hẹn tái khám hoặc ghi chú sau khi đã liên hệ' },

  // CARE_LOG
  CARE_LOG_CREATE: { title: 'Ghi nhật ký chăm sóc sau khám', desc: 'Ghi nhận phản hồi sức khỏe và dặn dò sau khi gọi hỏi thăm người bệnh' },
  CARE_LOG_READ: { title: 'Xem nhật ký chăm sóc sau khám', desc: 'Theo dõi lịch sử chăm sóc và phục hồi của bệnh nhân sau điều trị' },

  // CLINIC_CONFIGURATION
  CLINIC_CONFIGURATION_READ: { title: 'Xem cấu hình phòng khám', desc: 'Xem thông tin tên phòng khám, địa chỉ, hotline, giờ làm việc' },
  CLINIC_CONFIGURATION_UPDATE: { title: 'Cập nhật thông tin phòng khám', desc: 'Sửa tên phòng khám, logo, số hotline, địa chỉ, giờ mở/đóng cửa' },

  // BACKUP
  BACKUP_CREATE: { title: 'Tạo bản sao lưu dữ liệu', desc: 'Chủ động sao lưu toàn bộ cơ sở dữ liệu phòng khám ra file lưu trữ' },
  BACKUP_READ: { title: 'Xem lịch sử sao lưu dữ liệu', desc: 'Tra cứu danh sách các bản sao lưu đã tạo và dung lượng lưu trữ' },
  BACKUP_RESTORE: { title: 'Phục hồi dữ liệu hệ thống', desc: 'Khôi phục lại dữ liệu phòng khám từ một file sao lưu an toàn' },

  // REPORT
  REPORT_VIEW: { title: 'Xem báo cáo thống kê phòng khám', desc: 'Xem báo cáo doanh thu, số lượt bệnh nhân, công suất phòng khám' },
  REPORT_EXPORT: { title: 'Xuất file báo cáo (Excel / PDF)', desc: 'Tải các bảng báo cáo tài chính, hoạt động khám bệnh về máy tính' },

  // DASHBOARD
  DASHBOARD_OPERATIONAL_READ: { title: 'Xem Dashboard tổng quan vận hành', desc: 'Xem bảng đồng hồ đo các chỉ số hoạt động khám chữa bệnh trực quan' },

  // AUDIT
  AUDIT_READ: { title: 'Xem nhật ký kiểm toán hệ thống', desc: 'Tra cứu lịch sử truy cập bệnh án, nhật ký phân quyền và thay đổi dữ liệu' },
}

const getRoleDisplayName = (role) => {
  if (!role) return '—'
  return ROLE_DISPLAY_NAMES[role.name] || role.description || role.name
}

const getRoleTheme = (roleName) => {
  return ROLE_THEMES[roleName] || { color: 'default', bg: '#f8fafc', border: '#e2e8f0', text: '#334155', icon: '👤' }
}

const getModuleDisplayName = (mod) => {
  return MODULE_DISPLAY_NAMES[mod] || mod
}

const getPermissionDetails = (perm) => {
  if (PERMISSION_DETAILS[perm.code]) {
    return PERMISSION_DETAILS[perm.code]
  }
  const cleanTitle = (perm.name || perm.code).replace(/_/g, ' ')
  return {
    title: cleanTitle.charAt(0).toUpperCase() + cleanTitle.slice(1).toLowerCase(),
    desc: perm.description || 'Quyền hạn thao tác chức năng trong hệ thống',
  }
}

function RolePermissionsPage() {
  const { user } = useAuthContext()

  // Strict authorization check based on technical roles & permissions
  const userRoles = user?.roles || []
  const userPermissions = user?.permissions || []

  const isAdmin = userRoles.includes('admin') || userRoles.includes('role_admin')
  const canRead = isAdmin || userPermissions.includes('ROLE_READ') || userPermissions.includes('PERMISSION_READ')
  const canUpdate = isAdmin || userPermissions.includes('ROLE_UPDATE')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const [roles, setRoles] = useState([])
  const [permissions, setPermissions] = useState([])

  // State maps: roleId -> Set of permission codes
  const [originalPermissionsByRole, setOriginalPermissionsByRole] = useState({})
  const [draftPermissionsByRole, setDraftPermissionsByRole] = useState({})

  // Saving state tracking per roleId
  const [savingRoleId, setSavingRoleId] = useState(null)

  // Filters & Search
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedModule, setSelectedModule] = useState('ALL')
  const [onlyShowDirty, setOnlyShowDirty] = useState(false)

  // Mobile selected role view
  const [mobileSelectedRoleId, setMobileSelectedRoleId] = useState(null)

  // Load data from Backend
  const loadData = useCallback(async () => {
    if (!canRead) {
      setError('Bạn không có quyền xem danh sách vai trò hoặc danh mục quyền.')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const [rolesRes, permissionsRes] = await Promise.all([
        roleApi.getRoles(),
        roleApi.getPermissions(),
      ])

      const fetchedRoles = Array.isArray(rolesRes.data) ? rolesRes.data : []
      const fetchedPermissions = Array.isArray(permissionsRes.data) ? permissionsRes.data : []

      setRoles(fetchedRoles)
      setPermissions(fetchedPermissions)

      if (fetchedRoles.length > 0 && !mobileSelectedRoleId) {
        setMobileSelectedRoleId(fetchedRoles[0].id)
      }

      // Build Set maps for original and draft
      const origMap = {}
      const draftMap = {}

      fetchedRoles.forEach((role) => {
        const codes = (role.permissions || []).map((p) => p.code).filter(Boolean)
        origMap[role.id] = new Set(codes)
        draftMap[role.id] = new Set(codes)
      })

      setOriginalPermissionsByRole(origMap)
      setDraftPermissionsByRole(draftMap)
    } catch (err) {
      const status = err.response?.status
      if (status === 403) {
        setError('Bạn không có quyền xem danh sách vai trò hoặc danh mục quyền (403 Forbidden).')
      } else {
        setError(err.response?.data?.message || 'Không thể tải dữ liệu phân quyền từ máy chủ.')
      }
    } finally {
      setLoading(false)
    }
  }, [canRead, mobileSelectedRoleId])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Check if a specific role has unsaved changes
  const isRoleDirty = useCallback(
    (roleId) => {
      const orig = originalPermissionsByRole[roleId] || new Set()
      const draft = draftPermissionsByRole[roleId] || new Set()
      if (orig.size !== draft.size) return true
      for (const code of draft) {
        if (!orig.has(code)) return true
      }
      return false
    },
    [originalPermissionsByRole, draftPermissionsByRole],
  )

  // Get all dirty roles
  const dirtyRoles = useMemo(() => {
    return roles.filter((role) => isRoleDirty(role.id))
  }, [roles, isRoleDirty])

  const hasAnyDirtyRole = dirtyRoles.length > 0

  // Check if a specific permission is dirty in any role
  const isPermissionDirtyInAnyRole = useCallback(
    (permissionCode) => {
      return roles.some((role) => {
        const origHas = (originalPermissionsByRole[role.id] || new Set()).has(permissionCode)
        const draftHas = (draftPermissionsByRole[role.id] || new Set()).has(permissionCode)
        return origHas !== draftHas
      })
    },
    [roles, originalPermissionsByRole, draftPermissionsByRole],
  )

  // Calculate diff for confirmation modal
  const getRoleDiff = useCallback(
    (roleId) => {
      const orig = originalPermissionsByRole[roleId] || new Set()
      const draft = draftPermissionsByRole[roleId] || new Set()

      const addedCodes = []
      const removedCodes = []

      draft.forEach((code) => {
        if (!orig.has(code)) addedCodes.push(code)
      })

      orig.forEach((code) => {
        if (!draft.has(code)) removedCodes.push(code)
      })

      return { addedCodes, removedCodes }
    },
    [originalPermissionsByRole, draftPermissionsByRole],
  )

  // Toggle single permission for a role (In-Memory Draft only, NO PUT)
  const handleTogglePermission = useCallback(
    (roleId, permissionCode, permissionActive) => {
      if (!canUpdate || permissionActive === false) return

      setDraftPermissionsByRole((prev) => {
        const currentSet = new Set(prev[roleId] || [])
        if (currentSet.has(permissionCode)) {
          currentSet.delete(permissionCode)
        } else {
          currentSet.add(permissionCode)
        }
        return {
          ...prev,
          [roleId]: currentSet,
        }
      })
    },
    [canUpdate],
  )

  // Toggle all permissions in a module for a role
  const handleToggleModuleForRole = useCallback(
    (roleId, modulePermissions, shouldEnable) => {
      if (!canUpdate) return

      setDraftPermissionsByRole((prev) => {
        const currentSet = new Set(prev[roleId] || [])
        modulePermissions.forEach((p) => {
          if (p.active !== false) {
            if (shouldEnable) {
              currentSet.add(p.code)
            } else {
              currentSet.delete(p.code)
            }
          }
        })
        return {
          ...prev,
          [roleId]: currentSet,
        }
      })
    },
    [canUpdate],
  )

  // Cancel changes for a single role
  const handleCancelRole = useCallback(
    (roleId) => {
      const orig = originalPermissionsByRole[roleId] || new Set()
      setDraftPermissionsByRole((prev) => ({
        ...prev,
        [roleId]: new Set(orig),
      }))
      message.info('Đã hoàn tác thay đổi chưa lưu của vai trò này.')
    },
    [originalPermissionsByRole],
  )

  // Save changes for a single role (Sends FULL list of permissionCodes)
  const executeSaveRole = async (role) => {
    if (savingRoleId) return
    setSavingRoleId(role.id)

    // ALWAYS SEND FULL LIST OF DRAFT PERMISSION CODES
    const fullDraftCodes = Array.from(draftPermissionsByRole[role.id] || [])

    try {
      await roleApi.updateRolePermissions(role.id, fullDraftCodes)

      message.success(
        `Cập nhật quyền cho vai trò "${getRoleDisplayName(role)}" thành công. Người dùng thuộc vai trò này cần đăng nhập lại để nhận quyền mới.`,
        6,
      )

      // Re-fetch roles from Backend to guarantee source of truth synchronization
      const rolesRes = await roleApi.getRoles()
      const fetchedRoles = Array.isArray(rolesRes.data) ? rolesRes.data : []
      setRoles(fetchedRoles)

      const updatedRole = fetchedRoles.find((r) => r.id === role.id)
      const freshCodes = (updatedRole?.permissions || []).map((p) => p.code).filter(Boolean)

      setOriginalPermissionsByRole((prev) => ({
        ...prev,
        [role.id]: new Set(freshCodes),
      }))
      setDraftPermissionsByRole((prev) => ({
        ...prev,
        [role.id]: new Set(freshCodes),
      }))
    } catch (err) {
      const status = err.response?.status
      if (status === 409) {
        message.error(
          err.response?.data?.message ||
            'Không thể gỡ quyền quản trị cốt lõi khỏi quản trị viên cuối cùng (409 Conflict).',
          7,
        )
      } else if (status === 403) {
        message.error('Bạn không có quyền thay đổi phân quyền vai trò (403 Forbidden).')
      } else if (status === 400) {
        message.error(err.response?.data?.message || 'Dữ liệu phân quyền không hợp lệ.')
      } else {
        message.error(err.response?.data?.message || 'Hệ thống không thể cập nhật phân quyền. Vui lòng thử lại.')
      }
    } finally {
      setSavingRoleId(null)
    }
  }

  // Confirm before saving
  const handleSaveRoleClick = (role) => {
    if (!canUpdate) {
      message.warning('Bạn không có quyền chỉnh sửa phân quyền.')
      return
    }

    const { addedCodes, removedCodes } = getRoleDiff(role.id)
    if (addedCodes.length === 0 && removedCodes.length === 0) {
      message.info('Không có thay đổi nào để lưu.')
      return
    }

    const roleName = getRoleDisplayName(role)

    Modal.confirm({
      title: `Xác nhận cập nhật phân quyền cho vai trò: ${roleName}`,
      icon: <ExclamationCircleOutlined style={{ color: '#fa8c16' }} />,
      width: 620,
      content: (
        <div style={{ marginTop: 12 }}>
          <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 14 }}>
            Hệ thống sẽ ghi nhận toàn bộ danh sách quyền mới cho vai trò <strong>{roleName}</strong>. Người dùng thuộc vai
            trò này sẽ được áp dụng quyền mới ở lần đăng nhập tiếp theo.
          </Paragraph>

          {addedCodes.length > 0 && (
            <div style={{ marginBottom: 12, backgroundColor: '#f0fdf4', padding: 12, borderRadius: 8, border: '1px solid #bbf7d0' }}>
              <Text strong style={{ color: '#16a34a', display: 'block', marginBottom: 6 }}>
                + Cấp mới ({addedCodes.length} quyền):
              </Text>
              <div style={{ maxHeight: 130, overflowY: 'auto', paddingLeft: 4 }}>
                {addedCodes.map((code) => {
                  const perm = permissions.find((p) => p.code === code)
                  const details = perm ? getPermissionDetails(perm) : { title: code, desc: '' }
                  return (
                    <div key={code} style={{ fontSize: 12.5, color: '#15803d', margin: '4px 0' }}>
                      • <strong>{details.title}</strong> <Text code style={{ color: '#16a34a', fontSize: 11.5 }}>{code}</Text>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {removedCodes.length > 0 && (
            <div style={{ marginBottom: 8, backgroundColor: '#fef2f2', padding: 12, borderRadius: 8, border: '1px solid #fecaca' }}>
              <Text strong style={{ color: '#dc2626', display: 'block', marginBottom: 6 }}>
                - Gỡ bỏ ({removedCodes.length} quyền):
              </Text>
              <div style={{ maxHeight: 130, overflowY: 'auto', paddingLeft: 4 }}>
                {removedCodes.map((code) => {
                  const perm = permissions.find((p) => p.code === code)
                  const details = perm ? getPermissionDetails(perm) : { title: code, desc: '' }
                  return (
                    <div key={code} style={{ fontSize: 12.5, color: '#b91c1c', margin: '4px 0' }}>
                      • <strong>{details.title}</strong> <Text code style={{ color: '#dc2626', fontSize: 11.5 }}>{code}</Text>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </div>
      ),
      okText: 'Xác nhận lưu',
      cancelText: 'Hủy bỏ',
      okButtonProps: { type: 'primary', loading: savingRoleId === role.id },
      onOk: () => executeSaveRole(role),
    })
  }

  // Handle Refresh
  const handleRefresh = () => {
    if (hasAnyDirtyRole) {
      Modal.confirm({
        title: 'Có thay đổi chưa lưu',
        content: 'Bạn đang có các quyền đã điều chỉnh nhưng chưa lưu. Việc làm mới sẽ khôi phục về dữ liệu máy chủ. Bạn có chắc chắn muốn làm mới không?',
        okText: 'Làm mới & Hủy thay đổi',
        cancelText: 'Tiếp tục chỉnh sửa',
        okButtonProps: { danger: true },
        onOk: () => loadData(),
      })
    } else {
      loadData()
    }
  }

  // Derive unique modules dynamically from permissions catalog
  const modules = useMemo(() => {
    const unique = [...new Set(permissions.map((p) => p.module).filter(Boolean))]
    return unique.sort()
  }, [permissions])

  // Filter permissions by search term, selected module, and onlyShowDirty toggle
  const filteredPermissions = useMemo(() => {
    return permissions.filter((perm) => {
      if (onlyShowDirty && !isPermissionDirtyInAnyRole(perm.code)) {
        return false
      }

      const matchModule = selectedModule === 'ALL' || perm.module === selectedModule
      if (!matchModule) return false

      if (!searchTerm.trim()) return true
      const query = searchTerm.toLowerCase().trim()
      const details = getPermissionDetails(perm)
      const codeMatch = perm.code?.toLowerCase().includes(query)
      const nameMatch = perm.name?.toLowerCase().includes(query)
      const titleMatch = details.title.toLowerCase().includes(query)
      const descMatch = details.desc.toLowerCase().includes(query)
      const moduleMatch = perm.module?.toLowerCase().includes(query)

      return codeMatch || nameMatch || titleMatch || descMatch || moduleMatch
    })
  }, [permissions, selectedModule, searchTerm, onlyShowDirty, isPermissionDirtyInAnyRole])

  // Group filtered permissions by module for structured table rendering
  const groupedPermissions = useMemo(() => {
    const map = {}
    filteredPermissions.forEach((p) => {
      const mod = p.module || 'OTHER'
      if (!map[mod]) map[mod] = []
      map[mod].push(p)
    })
    return map
  }, [filteredPermissions])

  // Mobile selected role object
  const mobileRole = useMemo(() => {
    return roles.find((r) => r.id === mobileSelectedRoleId) || roles[0]
  }, [roles, mobileSelectedRoleId])

  // If user does not have permission to view
  if (!canRead) {
    return (
      <Card style={{ margin: '16px 0', borderRadius: 12 }}>
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Từ chối truy cập"
          description="Bạn không có quyền xem hoặc quản lý phân quyền vai trò hệ thống (Yêu cầu quyền ROLE_READ hoặc vai trò Quản trị viên)."
        />
      </Card>
    )
  }

  return (
    <div className="role-permissions-management-page" style={{ padding: '4px 0 28px' }}>
      <style>{`
        .visible-mobile {
          display: none;
        }
        .hidden-mobile {
          display: block;
        }
        .role-matrix-table th, .role-matrix-table td {
          transition: background-color 0.15s ease;
        }
        .perm-row-hover:hover {
          background-color: #f1f5f9 !important;
        }
        @media (max-width: 860px) {
          .visible-mobile {
            display: block;
          }
          .hidden-mobile, .role-matrix-desktop-container {
            display: none;
          }
        }
      `}</style>

      {/* HEADER BANNER */}
      <Card
        style={{
          marginBottom: 16,
          borderRadius: 12,
          background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%)',
          boxShadow: '0 4px 14px rgba(37, 99, 235, 0.18)',
          border: 'none',
        }}
        bodyStyle={{ padding: '18px 24px' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                backdropFilter: 'blur(8px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 24,
                color: '#ffffff',
                border: '1px solid rgba(255, 255, 255, 0.3)',
              }}
            >
              <SafetyCertificateOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0, color: '#ffffff', fontWeight: 600 }}>
                Quản lý & Phân quyền Vai trò Hệ thống
              </Title>
              <div style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 13, marginTop: 2 }}>
                Ma trận kiểm soát và điều chỉnh quyền hạn từng phân hệ cho các vai trò chuẩn phòng khám.
              </div>
            </div>
          </div>

          <Space wrap size="middle">
            <div style={{ display: 'flex', gap: 8 }}>
              <Tag color="cyan" style={{ fontSize: 12, padding: '4px 10px', borderRadius: 20 }}>
                <strong>{roles.length}</strong> vai trò
              </Tag>
              <Tag color="blue" style={{ fontSize: 12, padding: '4px 10px', borderRadius: 20 }}>
                <strong>{permissions.length}</strong> quyền
              </Tag>
              <Tag color="geekblue" style={{ fontSize: 12, padding: '4px 10px', borderRadius: 20 }}>
                <strong>{modules.length}</strong> phân hệ
              </Tag>
            </div>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              loading={loading}
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                color: '#ffffff',
                borderColor: 'rgba(255, 255, 255, 0.35)',
                borderRadius: 8,
              }}
            >
              Làm mới
            </Button>
          </Space>
        </div>
      </Card>

      {/* ERROR ALERT */}
      {error && (
        <Alert
          type="error"
          showIcon
          message="Không thể nạp dữ liệu phân quyền"
          description={error}
          style={{ marginBottom: 16, borderRadius: 8 }}
          action={
            <Button size="small" type="primary" onClick={loadData}>
              Thử lại
            </Button>
          }
        />
      )}

      {/* ROLE OVERVIEW CARDS (DESKTOP) */}
      <div className="hidden-mobile" style={{ marginBottom: 18 }}>
        <Row gutter={[12, 12]} style={{ display: 'flex', flexWrap: 'wrap' }}>
          {roles.map((role) => {
            const isDirty = isRoleDirty(role.id)
            const draftCount = (draftPermissionsByRole[role.id] || new Set()).size
            const isSaving = savingRoleId === role.id
            const theme = getRoleTheme(role.name)
            const percent = permissions.length > 0 ? Math.round((draftCount / permissions.length) * 100) : 0

            return (
              <Col
                key={role.id}
                xs={24}
                sm={12}
                md={8}
                lg={Math.max(4, Math.floor(24 / Math.max(1, roles.length)))}
                style={{ display: 'flex' }}
              >
                <Card
                  size="small"
                  style={{
                    width: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    borderRadius: 10,
                    borderWidth: isDirty ? 1.5 : 1,
                    borderColor: isDirty ? '#f59e0b' : theme.border,
                    backgroundColor: isDirty ? '#fffdf7' : '#ffffff',
                    boxShadow: isDirty ? '0 4px 12px rgba(245, 158, 11, 0.15)' : '0 1px 3px rgba(0,0,0,0.04)',
                    transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                  }}
                  bodyStyle={{
                    padding: '12px 14px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    height: '100%',
                    flex: 1,
                  }}
                >
                  <div>
                    {/* Header */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 6, marginBottom: 8, height: 26 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0, overflow: 'hidden' }}>
                        <span style={{ fontSize: 15, flexShrink: 0 }}>{theme.icon}</span>
                        <Text
                          strong
                          ellipsis
                          style={{ fontSize: 13, color: theme.text, whiteSpace: 'nowrap' }}
                          title={getRoleDisplayName(role)}
                        >
                          {getRoleDisplayName(role)}
                        </Text>
                      </div>
                      {isDirty ? (
                        <Badge count="Chưa lưu" style={{ backgroundColor: '#f59e0b', fontSize: 10, flexShrink: 0 }} />
                      ) : (
                        <Tag color={theme.color} style={{ margin: 0, fontSize: 10.5, flexShrink: 0 }}>
                          {role.name}
                        </Tag>
                      )}
                    </div>

                    {/* Progress details */}
                    <div style={{ marginBottom: 6 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: '#64748b', marginBottom: 2 }}>
                        <span>Quyền đã cấp:</span>
                        <span>
                          <strong style={{ color: '#1e293b' }}>{draftCount}</strong>/{permissions.length} ({percent}%)
                        </span>
                      </div>
                      <Progress
                        percent={percent}
                        showInfo={false}
                        size="small"
                        strokeColor={isDirty ? '#f59e0b' : '#2563eb'}
                        trailColor="#e2e8f0"
                      />
                    </div>
                  </div>

                  {/* Actions */}
                  {canUpdate && (
                    <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 6, marginTop: 10, paddingTop: 8, borderTop: '1px dashed #e2e8f0' }}>
                      {isDirty && (
                        <Tooltip title="Hủy các thay đổi chưa lưu của vai trò này">
                          <Button
                            size="small"
                            icon={<UndoOutlined />}
                            onClick={() => handleCancelRole(role.id)}
                            disabled={isSaving}
                            style={{ borderRadius: 6 }}
                          >
                            Hủy
                          </Button>
                        </Tooltip>
                      )}
                      <Button
                        size="small"
                        type={isDirty ? 'primary' : 'default'}
                        icon={<SaveOutlined />}
                        loading={isSaving}
                        disabled={!isDirty || isSaving}
                        onClick={() => handleSaveRoleClick(role)}
                        style={{
                          borderRadius: 6,
                          backgroundColor: isDirty ? '#16a34a' : undefined,
                          borderColor: isDirty ? '#16a34a' : undefined,
                          fontSize: 12,
                        }}
                      >
                        {isDirty ? 'Lưu thay đổi' : 'Đã lưu'}
                      </Button>
                    </div>
                  )}
                </Card>
              </Col>
            )
          })}
        </Row>
      </div>

      {/* FILTER & SEARCH CONTROL BAR */}
      <Card
        size="small"
        style={{
          marginBottom: 16,
          borderRadius: 10,
          boxShadow: '0 1px 4px rgba(0,0,0,0.03)',
        }}
        bodyStyle={{ padding: '12px 18px' }}
      >
        <Row gutter={[14, 12]} align="middle" justify="space-between">
          <Col xs={24} sm={10} md={8}>
            <Input
              placeholder="Tìm theo mã quyền, tên quyền, chức năng..."
              prefix={<SearchOutlined style={{ color: '#9ca3af' }} />}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              allowClear
              style={{ borderRadius: 8 }}
            />
          </Col>

          <Col xs={24} sm={8} md={6}>
            <Select
              style={{ width: '100%' }}
              value={selectedModule}
              onChange={setSelectedModule}
              placeholder="Lọc theo phân hệ"
            >
              <Select.Option value="ALL">
                <Space>
                  <AppstoreOutlined />
                  <span>Tất cả phân hệ ({modules.length})</span>
                </Space>
              </Select.Option>
              {modules.map((mod) => (
                <Select.Option key={mod} value={mod}>
                  <Space>
                    {MODULE_ICONS[mod] || <BranchesOutlined />}
                    <span>{getModuleDisplayName(mod)} ({mod})</span>
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Col>

          <Col xs={24} sm={6} md={5}>
            <Space>
              <Switch
                size="small"
                checked={onlyShowDirty}
                onChange={setOnlyShowDirty}
                disabled={!hasAnyDirtyRole && !onlyShowDirty}
              />
              <Text style={{ fontSize: 12.5, color: onlyShowDirty ? '#f59e0b' : '#64748b' }}>
                Chỉ xem quyền đã sửa
              </Text>
            </Space>
          </Col>

          <Col xs={24} md={5} style={{ textAlign: 'right' }}>
            <Space size="small">
              <Text type="secondary" style={{ fontSize: 12.5 }}>
                Đang hiển thị: <strong>{filteredPermissions.length}</strong>/{permissions.length} quyền
              </Text>
              {!canUpdate && (
                <Tag color="orange" icon={<LockOutlined />} style={{ borderRadius: 6 }}>
                  Chỉ xem
                </Tag>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* MATRIX TABLE SECTION */}
      <Spin spinning={loading}>
        {filteredPermissions.length === 0 ? (
          <Card style={{ borderRadius: 10, padding: '30px 0', textAlign: 'center' }}>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <div>
                  <div style={{ color: '#64748b', fontSize: 14 }}>Không tìm thấy quyền nào phù hợp.</div>
                  {onlyShowDirty && (
                    <Button size="small" style={{ marginTop: 8 }} onClick={() => setOnlyShowDirty(false)}>
                      Tắt bộ lọc quyền đã sửa
                    </Button>
                  )}
                </div>
              }
            />
          </Card>
        ) : (
          <div>
            {/* DESKTOP MATRIX TABLE */}
            <div
              className="role-matrix-desktop-container"
              style={{
                overflowX: 'auto',
                borderRadius: 10,
                boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                border: '1px solid #e2e8f0',
              }}
            >
              <table
                className="role-matrix-table"
                style={{
                  width: '100%',
                  borderCollapse: 'collapse',
                  backgroundColor: '#ffffff',
                  fontSize: 13,
                }}
              >
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc', borderBottom: '2px solid #cbd5e1' }}>
                    <th style={{ padding: '14px 18px', textAlign: 'left', minWidth: 360, color: '#334155' }}>
                      Phân hệ & Chi tiết Chức năng Quyền hạn
                    </th>
                    {roles.map((role) => {
                      const theme = getRoleTheme(role.name)
                      const isDirty = isRoleDirty(role.id)
                      const draftCount = (draftPermissionsByRole[role.id] || new Set()).size

                      return (
                        <th
                          key={role.id}
                          style={{
                            padding: '12px 10px',
                            textAlign: 'center',
                            minWidth: 135,
                            borderLeft: '1px solid #e2e8f0',
                            backgroundColor: isDirty ? '#fffbeb' : '#f8fafc',
                          }}
                        >
                          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                              <span>{theme.icon}</span>
                              <strong style={{ color: theme.text, fontSize: 13 }}>{getRoleDisplayName(role)}</strong>
                            </div>
                            <div style={{ fontSize: 11.5, color: '#64748b' }}>
                              ({draftCount} quyền)
                            </div>
                          </div>
                        </th>
                      )
                    })}
                  </tr>
                </thead>

                <tbody>
                  {Object.entries(groupedPermissions).map(([modName, permsInMod]) => (
                    <React.Fragment key={modName}>
                      {/* MODULE HEADER ROW */}
                      <tr
                        style={{
                          backgroundColor: '#f1f5f9',
                          borderTop: '2px solid #cbd5e1',
                          borderBottom: '1px solid #cbd5e1',
                        }}
                      >
                        <td style={{ padding: '10px 18px', fontWeight: 'bold', color: '#0f172a' }}>
                          <Space size="middle">
                            <span style={{ fontSize: 17 }}>{MODULE_ICONS[modName] || <BranchesOutlined />}</span>
                            <span style={{ fontSize: 13.5, fontWeight: 700, color: '#1e3a8a' }}>
                              {getModuleDisplayName(modName)} ({modName})
                            </span>
                            <Tag color="geekblue" style={{ borderRadius: 12, margin: 0 }}>
                              {permsInMod.length} quyền
                            </Tag>
                          </Space>
                        </td>

                        {roles.map((role) => {
                          const roleSet = draftPermissionsByRole[role.id] || new Set()
                          const activePerms = permsInMod.filter((p) => p.active !== false)
                          const allEnabled =
                            activePerms.length > 0 && activePerms.every((p) => roleSet.has(p.code))
                          const someEnabled =
                            activePerms.some((p) => roleSet.has(p.code)) && !allEnabled

                          return (
                            <td
                              key={role.id}
                              style={{
                                padding: '8px 10px',
                                textAlign: 'center',
                                borderLeft: '1px solid #e2e8f0',
                              }}
                            >
                              {canUpdate && activePerms.length > 1 && (
                                <Tooltip
                                  title={
                                    allEnabled
                                      ? `Bỏ chọn tất cả quyền thuộc ${getModuleDisplayName(modName)} cho ${getRoleDisplayName(role)}`
                                      : `Bật tất cả quyền thuộc ${getModuleDisplayName(modName)} cho ${getRoleDisplayName(role)}`
                                  }
                                >
                                  <Checkbox
                                    checked={allEnabled}
                                    indeterminate={someEnabled}
                                    onChange={(e) =>
                                      handleToggleModuleForRole(role.id, permsInMod, e.target.checked)
                                    }
                                  />
                                </Tooltip>
                              )}
                            </td>
                          )
                        })}
                      </tr>

                      {/* PERMISSION ROWS */}
                      {permsInMod.map((perm, idx) => {
                        const isInactive = perm.active === false
                        const details = getPermissionDetails(perm)

                        return (
                          <tr
                            key={perm.id || perm.code}
                            className="perm-row-hover"
                            style={{
                              borderBottom: '1px solid #f1f5f9',
                              backgroundColor: idx % 2 === 0 ? '#ffffff' : '#fafafa',
                              opacity: isInactive ? 0.6 : 1,
                            }}
                          >
                            <td style={{ padding: '10px 18px' }}>
                              {/* Title in bold clear Vietnamese */}
                              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                                <span style={{ fontSize: 13.5, fontWeight: 600, color: '#0f172a' }}>
                                  {details.title}
                                </span>
                                <Text
                                  code
                                  style={{
                                    color: '#2563eb',
                                    fontSize: 11.5,
                                    backgroundColor: '#eff6ff',
                                    borderColor: '#bfdbfe',
                                    borderRadius: 4,
                                    padding: '1px 6px',
                                  }}
                                >
                                  {perm.code}
                                </Text>
                                {isInactive && <Tag color="default">Không hoạt động</Tag>}
                              </div>

                              {/* Detailed action description */}
                              <div style={{ fontSize: 12, color: '#475569', marginTop: 2, lineHeight: 1.4 }}>
                                {details.desc}
                              </div>
                            </td>

                            {roles.map((role) => {
                              const roleSet = draftPermissionsByRole[role.id] || new Set()
                              const isChecked = roleSet.has(perm.code)
                              const isOrigChecked = (originalPermissionsByRole[role.id] || new Set()).has(perm.code)
                              const isPermDirty = isChecked !== isOrigChecked

                              return (
                                <td
                                  key={role.id}
                                  style={{
                                    padding: '8px 10px',
                                    textAlign: 'center',
                                    borderLeft: '1px solid #e2e8f0',
                                    backgroundColor: isPermDirty ? '#fef3c7' : 'inherit',
                                  }}
                                >
                                  <Tooltip
                                    title={
                                      isInactive
                                        ? 'Quyền này đang bị vô hiệu hóa trong hệ thống'
                                        : isPermDirty
                                        ? `Đã sửa: ${isOrigChecked ? 'Bật' : 'Tắt'} ➔ ${isChecked ? 'Bật' : 'Tắt'} (Chưa lưu)`
                                        : `${isChecked ? 'Đang cấp quyền' : 'Chưa cấp quyền'} "${details.title}" cho ${getRoleDisplayName(role)}`
                                    }
                                  >
                                    <Switch
                                      size="small"
                                      checked={isChecked}
                                      disabled={!canUpdate || isInactive || savingRoleId === role.id}
                                      onChange={() => handleTogglePermission(role.id, perm.code, perm.active)}
                                      style={
                                        isChecked
                                          ? { backgroundColor: isPermDirty ? '#f59e0b' : '#2563eb' }
                                          : {}
                                      }
                                    />
                                  </Tooltip>
                                </td>
                              )
                            })}
                          </tr>
                        )
                      })}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>

            {/* MOBILE / TABLET FRIENDLY VIEW */}
            <div className="visible-mobile" style={{ marginTop: 16 }}>
              <Card size="small" style={{ marginBottom: 14, borderRadius: 10 }}>
                <div style={{ marginBottom: 8, fontWeight: 600, fontSize: 13 }}>
                  Chọn vai trò để phân quyền:
                </div>
                <Select
                  style={{ width: '100%' }}
                  value={mobileRole?.id}
                  onChange={setMobileSelectedRoleId}
                >
                  {roles.map((r) => (
                    <Select.Option key={r.id} value={r.id}>
                      {getRoleDisplayName(r)} ({(draftPermissionsByRole[r.id] || new Set()).size} quyền)
                    </Select.Option>
                  ))}
                </Select>
              </Card>

              {mobileRole && (
                <div>
                  {Object.entries(groupedPermissions).map(([modName, permsInMod]) => (
                    <Card
                      key={modName}
                      size="small"
                      title={
                        <Space>
                          <span>{MODULE_ICONS[modName] || <BranchesOutlined />}</span>
                          <span style={{ fontWeight: 600 }}>{getModuleDisplayName(modName)}</span>
                          <Tag color="blue">{permsInMod.length}</Tag>
                        </Space>
                      }
                      style={{ marginBottom: 12, borderRadius: 10 }}
                    >
                      {permsInMod.map((perm) => {
                        const isChecked = (draftPermissionsByRole[mobileRole.id] || new Set()).has(perm.code)
                        const isInactive = perm.active === false
                        const details = getPermissionDetails(perm)

                        return (
                          <div
                            key={perm.id || perm.code}
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              padding: '10px 0',
                              borderBottom: '1px solid #f1f5f9',
                            }}
                          >
                            <div style={{ maxWidth: '78%' }}>
                              <div style={{ fontWeight: 600, fontSize: 13.5, color: '#0f172a' }}>
                                {details.title}
                              </div>
                              <div style={{ fontSize: 12, color: '#64748b', marginTop: 1 }}>
                                {details.desc}
                              </div>
                              <div style={{ marginTop: 2 }}>
                                <Text code style={{ fontSize: 11 }}>{perm.code}</Text>
                                {isInactive && <Tag color="default" style={{ marginLeft: 6 }}>Không hoạt động</Tag>}
                              </div>
                            </div>

                            <Switch
                              checked={isChecked}
                              disabled={!canUpdate || isInactive || savingRoleId === mobileRole.id}
                              onChange={() => handleTogglePermission(mobileRole.id, perm.code, perm.active)}
                            />
                          </div>
                        )
                      })}
                    </Card>
                  ))}

                  {canUpdate && isRoleDirty(mobileRole.id) && (
                    <div
                      style={{
                        position: 'sticky',
                        bottom: 12,
                        zIndex: 20,
                        backgroundColor: '#ffffff',
                        padding: '12px 16px',
                        borderRadius: 10,
                        boxShadow: '0 6px 16px rgba(0,0,0,0.18)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        border: '1.5px solid #f59e0b',
                      }}
                    >
                      <Text strong style={{ color: '#f59e0b' }}>
                        Có thay đổi chưa lưu!
                      </Text>
                      <Space>
                        <Button onClick={() => handleCancelRole(mobileRole.id)}>Hủy</Button>
                        <Button
                          type="primary"
                          icon={<SaveOutlined />}
                          loading={savingRoleId === mobileRole.id}
                          onClick={() => handleSaveRoleClick(mobileRole)}
                          style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }}
                        >
                          Lưu {getRoleDisplayName(mobileRole)}
                        </Button>
                      </Space>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* STICKY BOTTOM ACTIONS BAR (DESKTOP) */}
            {hasAnyDirtyRole && (
              <div
                className="hidden-mobile"
                style={{
                  position: 'sticky',
                  bottom: 14,
                  zIndex: 30,
                  backgroundColor: '#ffffff',
                  padding: '12px 24px',
                  borderRadius: 12,
                  boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                  border: '1.5px solid #f59e0b',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginTop: 16,
                  backdropFilter: 'blur(8px)',
                }}
              >
                <Space size="middle">
                  <ExclamationCircleOutlined style={{ color: '#f59e0b', fontSize: 20 }} />
                  <div>
                    <Text strong style={{ color: '#b45309', fontSize: 13.5 }}>
                      Bạn có {dirtyRoles.length} vai trò có thay đổi chưa lưu:
                    </Text>
                    <Space size="small" style={{ marginLeft: 8 }}>
                      {dirtyRoles.map((r) => (
                        <Tag key={r.id} color="warning" style={{ margin: 0 }}>
                          {getRoleDisplayName(r)}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                </Space>

                <Space size="small">
                  {dirtyRoles.map((r) => (
                    <Button
                      key={r.id}
                      type="primary"
                      size="middle"
                      icon={<SaveOutlined />}
                      loading={savingRoleId === r.id}
                      onClick={() => handleSaveRoleClick(r)}
                      style={{ backgroundColor: '#16a34a', borderColor: '#16a34a', borderRadius: 6 }}
                    >
                      Lưu {getRoleDisplayName(r)}
                    </Button>
                  ))}
                </Space>
              </div>
            )}
          </div>
        )}
      </Spin>
    </div>
  )
}

export default RolePermissionsPage
