import dayjs from 'dayjs'

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
  if (visitStatus === 'COMPLETED' || visitStatus === 'CANCELLED') {
    return {
      canSign: false,
      reason: 'Lượt khám đã kết thúc hoặc đã bị hủy, không thể ký bệnh án.',
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

export const parseSignatureData = (signatureData, fallbackSignedAt = null, fallbackDoctorName = '') => {
  if (!signatureData) {
    return {
      isSigned: Boolean(fallbackSignedAt),
      type: 'STANDARD',
      signedAt: fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : null,
      rawSignedAt: fallbackSignedAt,
      doctorName: fallbackDoctorName || '',
      certHash: 'SIMULATED-E-SIGNATURE',
      drawing: null,
      raw: signatureData,
    }
  }

  if (signatureData.startsWith('{') && signatureData.endsWith('}')) {
    try {
      const parsed = JSON.parse(signatureData)
      return {
        isSigned: true,
        type: parsed.type || 'CANVAS_DIGITAL_SIGNATURE',
        signedAt: parsed.signedAt ? dayjs(parsed.signedAt).format('DD/MM/YYYY HH:mm:ss') : (fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : null),
        rawSignedAt: parsed.signedAt || fallbackSignedAt,
        doctorName: parsed.doctorName || fallbackDoctorName || '',
        certHash: parsed.certHash || 'CERT-SECURE-STAMP',
        drawing: parsed.signatureDrawing || null,
        raw: signatureData,
      }
    } catch {
    }
  }

  if (signatureData.startsWith('SIMULATED_SIGNATURE:')) {
    const parts = signatureData.split(':')
    const epoch = Number(parts[2])
    const decodedName = parts[3] ? decodeURIComponent(parts[3]) : fallbackDoctorName
    const certHash = parts[4] || (epoch ? `CERT-${epoch}` : 'CERT-SIMULATED')

    return {
      isSigned: true,
      type: 'SIMULATED_DIGITAL_SEAL',
      signedAt: epoch ? dayjs(epoch).format('DD/MM/YYYY HH:mm:ss') : (fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : null),
      rawSignedAt: epoch ? new Date(epoch).toISOString() : fallbackSignedAt,
      doctorName: decodedName || fallbackDoctorName || '',
      certHash,
      drawing: null,
      raw: signatureData,
    }
  }

  return {
    isSigned: true,
    type: 'CUSTOM_DIGITAL_SIGNATURE',
    signedAt: fallbackSignedAt ? dayjs(fallbackSignedAt).format('DD/MM/YYYY HH:mm:ss') : null,
    rawSignedAt: fallbackSignedAt,
    doctorName: fallbackDoctorName || '',
    certHash: signatureData.length > 30 ? `${signatureData.substring(0, 16)}...` : signatureData,
    drawing: null,
    raw: signatureData,
  }
}

export const isMedicalRecordSigned = (recordOrStatus) => {
  const status = typeof recordOrStatus === 'string' ? recordOrStatus : recordOrStatus?.status
  return status === 'SIGNED' || status === 'LOCKED' || status === 'ARCHIVED'
}

export const isMedicalRecordContentLocked = (recordOrStatus) => {
  return isMedicalRecordSigned(recordOrStatus)
}
