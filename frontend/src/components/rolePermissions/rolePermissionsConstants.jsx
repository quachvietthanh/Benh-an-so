import React from 'react'
import {
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  BarChartOutlined,
  BranchesOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DollarOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  FormOutlined,
  FundViewOutlined,
  HeartOutlined,
  KeyOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  SecurityScanOutlined,
  SettingOutlined,
  ShopOutlined,
  TeamOutlined,
  UnorderedListOutlined,
  UserOutlined,
} from '@ant-design/icons'

// Friendly display mapping for system roles
export const ROLE_DISPLAY_NAMES = {
  ADMIN: 'Quản trị viên',
  DOCTOR: 'Bác sĩ',
  RECEPTIONIST: 'Lễ tân',
  PHARMACIST: 'Dược sĩ',
  MANAGER: 'Quản lý phòng khám',
}

export const ROLE_THEMES = {
  ADMIN: { color: 'purple', bg: '#f5f3ff', border: '#ddd6fe', text: '#6d28d9', icon: '👑' },
  DOCTOR: { color: 'blue', bg: '#eff6ff', border: '#bfdbfe', text: '#1d4ed8', icon: '🩺' },
  RECEPTIONIST: { color: 'orange', bg: '#fff7ed', border: '#fed7aa', text: '#c2410c', icon: '📋' },
  PHARMACIST: { color: 'green', bg: '#f0fdf4', border: '#bbf7d0', text: '#15803d', icon: '💊' },
  MANAGER: { color: 'cyan', bg: '#ecfeff', border: '#a5f3fc', text: '#0e7490', icon: '📊' },
}

export const MODULE_DISPLAY_NAMES = {
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

export const MODULE_ICONS = {
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

export const PERMISSION_DETAILS = {
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
  PERMISSION_READ: { title: 'Xem danh mục quyền hệ thống', desc: 'Tra cứu danh mục quyền chức năng và phân hệ nghiệp vụ' },

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

export const getRoleDisplayName = (role) => {
  if (!role) return '—'
  return ROLE_DISPLAY_NAMES[role.name] || role.description || role.name
}

export const getRoleTheme = (roleName) => {
  return ROLE_THEMES[roleName] || { color: 'default', bg: '#f8fafc', border: '#e2e8f0', text: '#334155', icon: '👤' }
}

export const getModuleDisplayName = (mod) => {
  return MODULE_DISPLAY_NAMES[mod] || mod
}

export const getPermissionDetails = (perm) => {
  if (PERMISSION_DETAILS[perm.code]) {
    return PERMISSION_DETAILS[perm.code]
  }
  const cleanTitle = (perm.name || perm.code).replace(/_/g, ' ')
  return {
    title: cleanTitle.charAt(0).toUpperCase() + cleanTitle.slice(1).toLowerCase(),
    desc: perm.description || 'Quyền hạn thao tác chức năng trong hệ thống',
  }
}
