import dayjs from 'dayjs'

/**
 * Kiểm tra điều kiện tiên quyết để Bác sĩ có thể ký xác nhận bệnh án
 * @param {Object} params
 * @param {string|UUID} params.currentUserId - ID của user đăng nhập
 * @param {Array<string>} params.userRoles - Danh sách vai trò của user
 * @param {Object} params.encounterContext - Ngữ cảnh lượt khám (visit, queueItem, doctor)
 * @param {Object} params.formValues - Dữ liệu form khám bệnh hiện tại
 * @param {Object} params.primaryIcd - Chẩn đoán ICD-10 chính
 * @param {string} params.recordStatus - Trạng thái bệnh án hiện tại
 * @returns {{ canSign: boolean, reason: string|null, missingFields: string[] }}
 */
export const validateMedicalRecordForSigning = ({
  currentUserId,
  userRoles = [],
  encounterContext,
  formValues = {},
  primaryIcd,
  recordStatus,
}) => {
  const missingFields = []
  const roles = (userRoles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isDoctorOrAdmin = roles.includes('doctor') || roles.includes('admin')

  if (!isDoctorOrAdmin) {
    return {
      canSign: false,
      reason: 'Chỉ người dùng có vai trò Bác sĩ mới có quyền ký xác nhận bệnh án.',
      missingFields: [],
    }
  }

  const visitDoctorId = encounterContext?.visit?.doctorId || encounterContext?.doctor?.id
  if (visitDoctorId && currentUserId && String(visitDoctorId) !== String(currentUserId) && !roles.includes('admin')) {
    return {
      canSign: false,
      reason: 'Chỉ Bác sĩ phụ trách lượt khám này mới có quyền ký xác nhận bệnh án.',
      missingFields: [],
    }
  }

  const currentStatus = recordStatus || encounterContext?.medicalRecord?.status
  if (currentStatus === 'SIGNED' || currentStatus === 'LOCKED' || currentStatus === 'ARCHIVED') {
    return {
      canSign: false,
      reason: 'Bệnh án đã được ký xác nhận hoặc đã khóa. Không thể ký lại.',
      missingFields: [],
    }
  }

  const visitStatus = encounterContext?.visit?.status
  if (visitStatus && visitStatus !== 'IN_PROGRESS') {
    return {
      canSign: false,
      reason: `Lượt khám không ở trạng thái đang khám (hiện tại: ${visitStatus}).`,
      missingFields: [],
    }
  }

  const symptoms = (formValues.symptoms || formValues.chiefComplaint || encounterContext?.visit?.reason || '').trim()
  if (!symptoms) {
    missingFields.push('Lý do khám / Triệu chứng bệnh')
  }

  if (!primaryIcd || !primaryIcd.code) {
    missingFields.push('Chẩn đoán chính (Mã ICD-10)')
  }

  const conclusion = (formValues.conclusion || formValues.diagnosisText || '').trim()
  if (!conclusion && !primaryIcd?.name) {
    missingFields.push('Kết luận khám / Chẩn đoán bệnh')
  }

  if (missingFields.length > 0) {
    return {
      canSign: false,
      reason: `Bệnh án chưa đủ thông tin bắt buộc: ${missingFields.join(', ')}.`,
      missingFields,
    }
  }

  return {
    canSign: true,
    reason: null,
    missingFields: [],
  }
}

/**
 * Sinh mã dữ liệu chữ ký số mô phỏng (Simulated Digital Signature)
 * @param {Object} params
 * @param {string|UUID} params.doctorId - ID bác sĩ ký
 * @param {string} params.doctorName - Tên bác sĩ ký
 * @param {string} [params.customSignature] - Chữ ký vẽ tay base64 (nếu có)
 * @param {number|string} [params.timestamp] - Thời gian ký
 * @returns {string} Chuỗi signatureData
 */
export const generateSimulatedSignatureData = ({
  doctorId,
  doctorName = '',
  customSignature = '',
  timestamp = Date.now(),
}) => {
  const ts = timestamp || Date.now()
  const docId = doctorId || 'ANONYMOUS_DOCTOR'
  const certHash = Math.random().toString(36).substring(2, 10).toUpperCase()

  if (customSignature && customSignature.trim().length > 0) {
    return JSON.stringify({
      type: 'CANVAS_DIGITAL_SIGNATURE',
      doctorId: docId,
      doctorName: doctorName.trim(),
      signedAt: new Date(ts).toISOString(),
      certHash: `CERT-${certHash}-${ts}`,
      signatureDrawing: customSignature.trim(),
    })
  }

  return `SIMULATED_SIGNATURE:${docId}:${ts}:${encodeURIComponent(doctorName.trim())}:CERT-${certHash}`
}

/**
 * Phân tích dữ liệu chữ ký signatureData để hiển thị thông tin con dấu điện tử
 * @param {string} signatureData - Chuỗi chữ ký lưu trong cơ sở dữ liệu
 * @param {string|Date} [fallbackSignedAt] - Thời gian ký dự phòng
 * @param {string} [fallbackDoctorName] - Tên bác sĩ dự phòng
 * @returns {Object} Thông tin chi tiết con dấu ký số
 */
export const parseSignatureData = (signatureData, fallbackSignedAt = null, fallbackDoctorName = '') => {
  if (!signatureData) {
    return {
      isSigned: Boolean(fallbackSignedAt),
      type: 'STANDARD',
      signedAt: fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : null,
      rawSignedAt: fallbackSignedAt,
      doctorName: fallbackDoctorName || 'Bác sĩ phụ trách',
      certHash: 'SIMULATED-E-SIGNATURE',
      drawing: null,
      raw: signatureData,
    }
  }

  // Trường hợp là JSON lưu canvas signature
  if (signatureData.startsWith('{') && signatureData.endsWith('}')) {
    try {
      const parsed = JSON.parse(signatureData)
      return {
        isSigned: true,
        type: parsed.type || 'CANVAS_DIGITAL_SIGNATURE',
        signedAt: parsed.signedAt ? dayjs(parsed.signedAt).format('DD/MM/YYYY HH:mm:ss') : dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss'),
        rawSignedAt: parsed.signedAt || fallbackSignedAt,
        doctorName: parsed.doctorName || fallbackDoctorName || 'Bác sĩ phụ trách',
        certHash: parsed.certHash || 'CERT-SECURE-STAMP',
        drawing: parsed.signatureDrawing || null,
        raw: signatureData,
      }
    } catch {
      // JSON parse fallback
    }
  }

  // Trường hợp dạng chuỗi SIMULATED_SIGNATURE:doctorId:epochMillis:name:certHash
  if (signatureData.startsWith('SIMULATED_SIGNATURE:')) {
    const parts = signatureData.split(':')
    const epoch = Number(parts[2])
    const decodedName = parts[3] ? decodeURIComponent(parts[3]) : fallbackDoctorName
    const certHash = parts[4] || (epoch ? `CERT-${epoch}` : 'CERT-SIMULATED')

    return {
      isSigned: true,
      type: 'SIMULATED_DIGITAL_SEAL',
      signedAt: epoch ? dayjs(epoch).format('DD/MM/YYYY HH:mm:ss') : (fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : dayjs().format('DD/MM/YYYY HH:mm:ss')),
      rawSignedAt: epoch ? new Date(epoch).toISOString() : fallbackSignedAt,
      doctorName: decodedName || fallbackDoctorName || 'Bác sĩ phụ trách',
      certHash,
      drawing: null,
      raw: signatureData,
    }
  }

  return {
    isSigned: true,
    type: 'CUSTOM_DIGITAL_SIGNATURE',
    signedAt: fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : dayjs().format('DD/MM/YYYY HH:mm:ss'),
    rawSignedAt: fallbackSignedAt,
    doctorName: fallbackDoctorName || 'Bác sĩ phụ trách',
    certHash: signatureData.length > 30 ? `${signatureData.substring(0, 16)}...` : signatureData,
    drawing: null,
    raw: signatureData,
  }
}

/**
 * Kiểm tra xem bệnh án có đang ở trạng thái đã ký hoặc đã khóa không
 * @param {string|Object} recordOrStatus
 * @returns {boolean}
 */
export const isMedicalRecordSigned = (recordOrStatus) => {
  const status = typeof recordOrStatus === 'string' ? recordOrStatus : recordOrStatus?.status
  return status === 'SIGNED' || status === 'LOCKED' || status === 'ARCHIVED'
}

/**
 * Kiểm tra xem bệnh án có đang bị khóa nội dung không
 * @param {string|Object} recordOrStatus
 * @returns {boolean}
 */
export const isMedicalRecordContentLocked = (recordOrStatus) => {
  return isMedicalRecordSigned(recordOrStatus)
}
