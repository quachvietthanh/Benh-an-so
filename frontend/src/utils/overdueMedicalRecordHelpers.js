/**
 * Helper utilities for Overdue Medical Record Signing & Reminders (NCL-11-CN-006 / QTN-29).
 */

export const SYSTEM_DOCTORS = [
  {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
    fullName: 'BS. Dr. Nguyen Minh Anh',
    name: 'Dr. Nguyen Minh Anh',
    username: 'doctor1',
    email: 'doctor1@benhsoan.com',
    phone: '0901000001',
  },
  {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
    fullName: 'BS. Dr. Tran Quang Huy',
    name: 'Dr. Tran Quang Huy',
    username: 'doctor2',
    email: 'doctor2@benhsoan.com',
    phone: '0901000002',
  },
]

export const getDoctorDisplayName = (doctorId, customList = [], currentUser = null) => {
  if (!doctorId) return ''
  const strId = String(doctorId).toLowerCase()

  // 1. Check in customList
  const foundInList = (Array.isArray(customList) ? customList : []).find(
    (d) => String(d?.id).toLowerCase() === strId
  )
  if (foundInList?.fullName || foundInList?.name) {
    return foundInList.fullName || foundInList.name
  }

  // 2. Check in SYSTEM_DOCTORS
  const foundSystem = SYSTEM_DOCTORS.find((d) => String(d.id).toLowerCase() === strId)
  if (foundSystem?.fullName) {
    return foundSystem.fullName
  }

  // 3. Check currentUser
  if (currentUser && String(currentUser.id).toLowerCase() === strId) {
    if (currentUser.fullName && currentUser.fullName !== currentUser.username) {
      return currentUser.fullName.startsWith('BS') ? currentUser.fullName : `BS. ${currentUser.fullName}`
    }
    return `BS. ${currentUser.username || 'phụ trách'}`
  }

  return `Bác sĩ (${String(doctorId).slice(0, 8)}...)`
}

export const canViewOverdueSigning = (roles = [], permissions = []) => {
  const normalizedRoles = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  const normalizedPerms = (Array.isArray(permissions) ? permissions : [permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
    .filter(Boolean)

  if (
    normalizedRoles.includes('receptionist') ||
    normalizedRoles.includes('pharmacist')
  ) {
    return false
  }

  if (
    normalizedRoles.includes('admin') ||
    normalizedRoles.includes('manager') ||
    normalizedRoles.includes('clinic_manager') ||
    normalizedRoles.includes('doctor')
  ) {
    return true
  }

  return (
    normalizedPerms.includes('MEDICAL_RECORD_OVERDUE_READ') ||
    normalizedPerms.includes('MEDICAL_RECORD_REMIND_SIGN')
  )
}

export const canSendSigningReminder = (roles = [], permissions = []) => {
  const normalizedRoles = (Array.isArray(roles) ? roles : [roles])
    .map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    .filter(Boolean)
  const normalizedPerms = (Array.isArray(permissions) ? permissions : [permissions])
    .map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
    .filter(Boolean)

  if (
    normalizedRoles.includes('admin') ||
    normalizedRoles.includes('manager') ||
    normalizedRoles.includes('clinic_manager')
  ) {
    return true
  }

  return normalizedPerms.includes('MEDICAL_RECORD_REMIND_SIGN')
}

export const formatOverdueHours = (hours) => {
  const num = Number(hours) || 0
  if (num <= 0) return '0 giờ'
  if (num < 24) return `${num} giờ`
  const days = Math.floor(num / 24)
  const rem = num % 24
  return rem > 0 ? `${days} ngày ${rem} giờ` : `${days} ngày`
}

export const getOverdueSeverity = (hours) => {
  const num = Number(hours) || 0
  if (num >= 72) {
    return {
      level: 'CRITICAL',
      text: 'Trễ hạn nghiêm trọng (>72h)',
      color: '#cf1322',
      bg: '#fff1f0',
      border: '#ffa39e',
      tagColor: 'magenta',
    }
  }
  if (num >= 24) {
    return {
      level: 'HIGH',
      text: 'Trễ hạn cao (24h - 72h)',
      color: '#d4380d',
      bg: '#fff2e8',
      border: '#ffbb96',
      tagColor: 'volcano',
    }
  }
  return {
    level: 'MODERATE',
    text: 'Quá hạn (<24h)',
    color: '#d48806',
    bg: '#fffbe6',
    border: '#ffe58f',
    tagColor: 'warning',
  }
}

export const formatReminderChannel = (channel) => {
  const raw = String(channel || '').toUpperCase()
  switch (raw) {
    case 'SYSTEM':
      return { text: 'Hệ thống nội bộ', color: 'blue' }
    case 'EMAIL':
      return { text: 'Email bác sĩ', color: 'cyan' }
    case 'SMS':
      return { text: 'Tin nhắn SMS', color: 'green' }
    case 'ZALO':
      return { text: 'Tin nhắn Zalo', color: 'purple' }
    case 'MOCK':
    default:
      return { text: raw || 'Hệ thống', color: 'default' }
  }
}

export const formatReminderStatus = (status) => {
  const raw = String(status || '').toUpperCase()
  switch (raw) {
    case 'SENT':
      return { text: 'Đã gửi thành công', color: 'success' }
    case 'FAILED':
      return { text: 'Gửi thất bại', color: 'error' }
    case 'PENDING':
      return { text: 'Đang xử lý', color: 'processing' }
    default:
      return { text: raw || 'Đã gửi', color: 'default' }
  }
}

export const validateSendReminderForm = ({ notes }) => {
  const errors = []
  if (notes && notes.length > 500) {
    errors.push('Ghi chú nhắc nhở không được vượt quá 500 ký tự.')
  }
  return {
    isValid: errors.length === 0,
    errors,
  }
}

export const calculateOverdueKpis = (records = [], serverTotalElements = null) => {
  const safeRecords = Array.isArray(records) ? records : []
  const parsedServerTotal = serverTotalElements != null && Number.isFinite(Number(serverTotalElements))
    ? Number(serverTotalElements)
    : null
  const total = parsedServerTotal != null
    ? Math.max(parsedServerTotal, safeRecords.length)
    : safeRecords.length
  let criticalCount = 0
  let totalRemindersSent = 0
  const doctorIdSet = new Set()

  safeRecords.forEach((r) => {
    const hours = Number(r?.overdueHours) || 0
    if (hours >= 24) {
      criticalCount += 1
    }
    const remCount = Number(r?.reminderCount) || 0
    totalRemindersSent += remCount
    if (r?.doctorId) {
      doctorIdSet.add(r.doctorId)
    }
  })

  return {
    totalRecords: total,
    criticalRecords: criticalCount,
    uniqueDoctors: doctorIdSet.size,
    totalRemindersSent,
  }
}

export const filterOverdueRecords = (
  records = [],
  { searchQuery = '', doctorId = '', severityLevel = 'ALL' } = {}
) => {
  const safeRecords = Array.isArray(records) ? records : []
  const q = String(searchQuery || '').trim().toLowerCase()
  const targetDoctorId = String(doctorId || '').trim()
  const targetSeverity = String(severityLevel || '').toUpperCase()

  return safeRecords.filter((rec) => {
    // 1. Filter out signed records if any (TC-03)
    if (rec.status === 'SIGNED') {
      return false
    }

    // 2. Doctor Filter
    if (targetDoctorId && String(rec.doctorId) !== targetDoctorId) {
      return false
    }

    // 3. Severity Filter
    if (targetSeverity && targetSeverity !== 'ALL') {
      const severity = getOverdueSeverity(rec.overdueHours).level
      if (severity !== targetSeverity) {
        return false
      }
    }

    // 4. Search Query Match
    if (q) {
      const visitCode = String(rec.visitCode || '').toLowerCase()
      const patientCode = String(rec.patientCode || '').toLowerCase()
      const patientName = String(rec.patientFullName || '').toLowerCase()
      const doctorName = String(rec.doctorFullName || '').toLowerCase()
      const recordId = String(rec.medicalRecordId || '').toLowerCase()

      const match =
        visitCode.includes(q) ||
        patientCode.includes(q) ||
        patientName.includes(q) ||
        doctorName.includes(q) ||
        recordId.includes(q)

      if (!match) return false
    }

    return true
  })
}

export const formatMedicalRecordStatus = (status) => {
  const upper = String(status || '').toUpperCase()
  if (upper === 'OPEN') {
    return { label: 'Đang mở (OPEN)', color: 'cyan' }
  }
  if (upper === 'DRAFT') {
    return { label: 'Chưa ký số (DRAFT)', color: 'orange' }
  }
  return { label: `Chưa ký số (${upper || 'DRAFT'})`, color: 'orange' }
}

