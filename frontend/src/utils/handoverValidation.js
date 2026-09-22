import dayjs from 'dayjs'

/**
 * Danh mục các lý do bàn giao ca khám mẫu phổ biến
 */
export const PRESET_HANDOVER_REASONS = [
  'Có ca mổ / cấp cứu đột xuất',
  'Hết ca trực, chuyển giao cho bác sĩ ca tiếp theo',
  'Chuyển bác sĩ chuyên khoa phù hợp hơn với tình trạng bệnh',
  'Bác sĩ có công tác đột xuất cần chuyển giao',
]

/**
 * Kiểm tra xem một lượt khám có đủ điều kiện để thực hiện bàn giao không (NCL-04-CN-014)
 * @param {object} visit - Thông tin lượt khám
 * @param {object} medicalRecord - Thông tin bệnh án
 * @param {object} currentUser - Thông tin người dùng hiện tại
 * @returns {{ canHandover: boolean, reasonCode?: string, message?: string }}
 */
export const canHandoverVisit = (visit, medicalRecord, currentUser) => {
  if (!visit) {
    return {
      canHandover: false,
      reasonCode: 'VISIT_NOT_FOUND',
      message: 'Không tìm thấy thông tin lượt khám.',
    }
  }

  // TC-02: Sai trạng thái - Bệnh án đã ký hoặc đã khóa
  const recordStatus = String(medicalRecord?.status || visit?.medicalRecord?.status || '').toUpperCase()
  if (recordStatus === 'SIGNED' || recordStatus === 'LOCKED' || recordStatus === 'ARCHIVED') {
    return {
      canHandover: false,
      reasonCode: 'RECORD_ALREADY_SIGNED',
      message: 'Bệnh án đã được ký số và hoàn tất lượt khám. Không thể bàn giao sang bác sĩ khác.',
    }
  }

  // Trạng thái lượt khám đã kết thúc hoặc hủy
  const visitStatus = String(visit.status || '').toUpperCase()
  if (visitStatus === 'COMPLETED' || visitStatus === 'CANCELLED') {
    return {
      canHandover: false,
      reasonCode: 'VISIT_CLOSED',
      message: 'Lượt khám đã kết thúc hoặc đã hủy, không thể bàn giao.',
    }
  }

  // TC-03: Không có quyền (QTN-17) - Chỉ bác sĩ phụ trách hoặc Admin mới được bàn giao
  if (currentUser) {
    const roles = (currentUser.roles || []).map((r) =>
      String(r).toLowerCase().replace(/^role_/, '')
    )
    const isAdmin = roles.includes('admin')
    const currentUserId = currentUser.id || currentUser.userId
    const doctorId = visit.doctorId || visit.doctor?.id

    if (!isAdmin && doctorId && currentUserId && String(doctorId) !== String(currentUserId)) {
      return {
        canHandover: false,
        reasonCode: 'NOT_ASSIGNED_DOCTOR',
        message: 'Chỉ bác sĩ đang phụ trách ca khám mới có quyền bàn giao.',
      }
    }
  }

  return { canHandover: true }
}

/**
 * Kiểm tra tính hợp lệ của dữ liệu đầu vào khi bàn giao bệnh nhân
 * @param {string} targetDoctorId - ID bác sĩ tiếp nhận
 * @param {string} reason - Lý do bàn giao
 * @param {string} currentDoctorId - ID bác sĩ đang phụ trách hiện tại
 * @returns {{ isValid: boolean, error?: string }}
 */
export const validateHandoverInput = (targetDoctorId, reason, currentDoctorId) => {
  if (!targetDoctorId) {
    return {
      isValid: false,
      error: 'Vui lòng chọn bác sĩ tiếp nhận ca khám.',
    }
  }

  if (currentDoctorId && String(targetDoctorId) === String(currentDoctorId)) {
    return {
      isValid: false,
      error: 'Không thể bàn giao cho chính bác sĩ đang phụ trách.',
    }
  }

  const trimmedReason = String(reason || '').trim()
  if (!trimmedReason) {
    return {
      isValid: false,
      error: 'Vui lòng nhập lý do bàn giao bệnh nhân.',
    }
  }

  if (trimmedReason.length > 500) {
    return {
      isValid: false,
      error: 'Lý do bàn giao không được vượt quá 500 ký tự.',
    }
  }

  return { isValid: true }
}

/**
 * Định dạng ngày giờ bàn giao thuần Việt
 */
export const formatHandoverDateTime = (dateVal, fallback = '---') => {
  if (!dateVal) return fallback
  const parsed = dayjs(dateVal)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY HH:mm') : fallback
}

export const HANDOVERS_STORAGE_KEY = 'clinic_visit_handovers'

export const getStoredHandovers = () => {
  try {
    const raw = localStorage.getItem(HANDOVERS_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredHandover = (handover) => {
  try {
    const current = getStoredHandovers()
    const updated = [
      handover,
      ...current.filter((h) => String(h.visitId) !== String(handover.visitId)),
    ]
    localStorage.setItem(HANDOVERS_STORAGE_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const getLatestHandoverForVisit = (visitId) => {
  if (!visitId) return null
  const handovers = getStoredHandovers()
  return handovers.find((h) => String(h.visitId) === String(visitId)) || null
}

