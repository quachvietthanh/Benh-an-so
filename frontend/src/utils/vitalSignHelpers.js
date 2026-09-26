/**
 * Vital Sign constants, validation rules, abnormal flag definitions, and helpers.
 * Fully aligned with NCL-04-CN-007 specifications and Backend REST API / Domain model.
 */

export const VITAL_SIGN_LIMITS = Object.freeze({
  PULSE: { min: 30, max: 250, unit: 'lần/phút', label: 'Mạch' },
  BP_SYSTOLIC: { min: 50, max: 260, unit: 'mmHg', label: 'Huyết áp tâm thu' },
  BP_DIASTOLIC: { min: 30, max: 150, unit: 'mmHg', label: 'Huyết áp tâm trương' },
  TEMPERATURE: { min: 30.0, max: 45.0, unit: '°C', label: 'Nhiệt độ' },
  RESPIRATORY_RATE: { min: 5, max: 60, unit: 'lần/phút', label: 'Nhịp thở' },
  WEIGHT: { min: 0.5, max: 300.0, unit: 'kg', label: 'Cân nặng' },
  HEIGHT: { min: 20.0, max: 250.0, unit: 'cm', label: 'Chiều cao' },
  SPO2: { min: 50, max: 100, unit: '%', label: 'SpO2' },
  NOTE_MAX_LENGTH: 500,
})

export const ABNORMAL_FLAGS_META = Object.freeze({
  HYPERTENSION: {
    key: 'HYPERTENSION',
    label: 'Huyết áp cao',
    color: 'red',
    tagColor: '#dc2626',
    description: 'Huyết áp tâm thu ≥ 140 hoặc tâm trương ≥ 90 mmHg',
  },
  HYPOTENSION: {
    key: 'HYPOTENSION',
    label: 'Huyết áp thấp',
    color: 'orange',
    tagColor: '#ea580c',
    description: 'Huyết áp tâm thu < 90 hoặc tâm trương < 60 mmHg',
  },
  TACHYCARDIA: {
    key: 'TACHYCARDIA',
    label: 'Nhịp tim nhanh',
    color: 'volcano',
    tagColor: '#d97706',
    description: 'Mạch > 100 lần/phút',
  },
  BRADYCARDIA: {
    key: 'BRADYCARDIA',
    label: 'Nhịp tim chậm',
    color: 'cyan',
    tagColor: '#0891b2',
    description: 'Mạch < 60 lần/phút',
  },
  FEVER: {
    key: 'FEVER',
    label: 'Sốt',
    color: 'red',
    tagColor: '#e11d48',
    description: 'Nhiệt độ > 37.5 °C',
  },
  HYPOTHERMIA: {
    key: 'HYPOTHERMIA',
    label: 'Hạ thân nhiệt',
    color: 'blue',
    tagColor: '#2563eb',
    description: 'Nhiệt độ < 36.0 °C',
  },
  TACHYPNEA: {
    key: 'TACHYPNEA',
    label: 'Nhịp thở nhanh',
    color: 'volcano',
    tagColor: '#d97706',
    description: 'Nhịp thở > 20 lần/phút',
  },
  BRADYPNEA: {
    key: 'BRADYPNEA',
    label: 'Nhịp thở chậm',
    color: 'orange',
    tagColor: '#ea580c',
    description: 'Nhịp thở < 12 lần/phút',
  },
  HYPOXEMIA: {
    key: 'HYPOXEMIA',
    label: 'Giảm nồng độ oxy máu',
    color: 'magenta',
    tagColor: '#c026d3',
    description: 'SpO2 < 95%',
  },
  UNDERWEIGHT: {
    key: 'UNDERWEIGHT',
    label: 'Thiếu cân',
    color: 'gold',
    tagColor: '#ca8a04',
    description: 'Chỉ số BMI < 18.5 kg/m²',
  },
  OVERWEIGHT: {
    key: 'OVERWEIGHT',
    label: 'Thừa cân / Béo phì',
    color: 'volcano',
    tagColor: '#dc2626',
    description: 'Chỉ số BMI ≥ 25.0 kg/m²',
  },
})

/**
 * Calculate BMI from weight (kg) and height (cm)
 * Formula: weight / (height / 100)^2
 * @param {number|string} weight
 * @param {number|string} height
 * @returns {number|null} rounded to 1 decimal place
 */
export const calculateBmi = (weight, height) => {
  const w = parseFloat(weight)
  const h = parseFloat(height)
  if (isNaN(w) || isNaN(h) || w <= 0 || h <= 0) {
    return null
  }
  const heightInMeters = h / 100
  const bmi = w / (heightInMeters * heightInMeters)
  if (!isFinite(bmi) || bmi <= 0 || bmi > 999.9) {
    return null
  }
  return Math.round(bmi * 10) / 10
}

/**
 * Classify BMI into category string and color
 * @param {number|null} bmi
 */
export const getBmiCategory = (bmi) => {
  if (bmi == null || isNaN(bmi)) return null
  if (bmi < 18.5) {
    return { label: 'Thiếu cân', tone: 'gold', color: '#ca8a04', tag: 'UNDERWEIGHT' }
  }
  if (bmi < 25.0) {
    return { label: 'Bình thường', tone: 'green', color: '#16a34a', tag: 'NORMAL' }
  }
  if (bmi < 30.0) {
    return { label: 'Thừa cân', tone: 'volcano', color: '#ea580c', tag: 'OVERWEIGHT' }
  }
  return { label: 'Béo phì', tone: 'red', color: '#dc2626', tag: 'OBESE' }
}

/**
 * Evaluate abnormal flags according to clinical guidelines
 * @param {object} values
 * @returns {string[]} array of abnormal flag keys
 */
export const evaluateAbnormalFlags = (values = {}) => {
  const flags = []

  const pulse = values.pulse != null && values.pulse !== '' ? parseInt(values.pulse, 10) : null
  const sys = values.bloodPressureSystolic != null && values.bloodPressureSystolic !== ''
    ? parseInt(values.bloodPressureSystolic, 10)
    : null
  const dia = values.bloodPressureDiastolic != null && values.bloodPressureDiastolic !== ''
    ? parseInt(values.bloodPressureDiastolic, 10)
    : null
  const temp = values.temperature != null && values.temperature !== '' ? parseFloat(values.temperature) : null
  const resp = values.respiratoryRate != null && values.respiratoryRate !== ''
    ? parseInt(values.respiratoryRate, 10)
    : null
  const spo2 = values.spo2 != null && values.spo2 !== '' ? parseInt(values.spo2, 10) : null
  const bmi = values.bmi != null && values.bmi !== ''
    ? parseFloat(values.bmi)
    : calculateBmi(values.weight, values.height)

  // Blood pressure
  if ((sys != null && sys >= 140) || (dia != null && dia >= 90)) {
    flags.push('HYPERTENSION')
  } else if ((sys != null && sys < 90) || (dia != null && dia < 60)) {
    flags.push('HYPOTENSION')
  }

  // Pulse
  if (pulse != null) {
    if (pulse > 100) {
      flags.push('TACHYCARDIA')
    } else if (pulse < 60) {
      flags.push('BRADYCARDIA')
    }
  }

  // Temperature
  if (temp != null) {
    if (temp > 37.5) {
      flags.push('FEVER')
    } else if (temp < 36.0) {
      flags.push('HYPOTHERMIA')
    }
  }

  // Respiratory rate
  if (resp != null) {
    if (resp > 20) {
      flags.push('TACHYPNEA')
    } else if (resp < 12) {
      flags.push('BRADYPNEA')
    }
  }

  // SpO2
  if (spo2 != null && spo2 < 95) {
    flags.push('HYPOXEMIA')
  }

  // BMI
  if (bmi != null) {
    if (bmi < 18.5) {
      flags.push('UNDERWEIGHT')
    } else if (bmi >= 25.0) {
      flags.push('OVERWEIGHT')
    }
  }

  return flags
}

/**
 * Validate input values for vital sign form
 * Returns an object with { valid: boolean, errors: { [field]: string } }
 * @param {object} values
 */
export const validateVitalSignForm = (values = {}) => {
  const errors = {}

  const isEmpty = (v) => v == null || String(v).trim() === ''

  const allEmpty =
    isEmpty(values.pulse) &&
    isEmpty(values.bloodPressureSystolic) &&
    isEmpty(values.bloodPressureDiastolic) &&
    isEmpty(values.temperature) &&
    isEmpty(values.respiratoryRate) &&
    isEmpty(values.weight) &&
    isEmpty(values.height) &&
    isEmpty(values.spo2)

  if (allEmpty) {
    errors.general = 'Cần nhập ít nhất một chỉ số sinh tồn.'
    return { valid: false, errors }
  }

  // Pulse (30 - 250)
  if (!isEmpty(values.pulse)) {
    const val = parseInt(values.pulse, 10)
    if (isNaN(val) || val < VITAL_SIGN_LIMITS.PULSE.min || val > VITAL_SIGN_LIMITS.PULSE.max) {
      errors.pulse = `Mạch ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.PULSE.min} - ${VITAL_SIGN_LIMITS.PULSE.max} lần/phút).`
    }
  }

  // Blood pressure systolic (50 - 260)
  let sysVal = null
  if (!isEmpty(values.bloodPressureSystolic)) {
    sysVal = parseInt(values.bloodPressureSystolic, 10)
    if (isNaN(sysVal) || sysVal < VITAL_SIGN_LIMITS.BP_SYSTOLIC.min || sysVal > VITAL_SIGN_LIMITS.BP_SYSTOLIC.max) {
      errors.bloodPressureSystolic = `Huyết áp tâm thu ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.BP_SYSTOLIC.min} - ${VITAL_SIGN_LIMITS.BP_SYSTOLIC.max} mmHg).`
    }
  }

  // Blood pressure diastolic (30 - 150)
  let diaVal = null
  if (!isEmpty(values.bloodPressureDiastolic)) {
    diaVal = parseInt(values.bloodPressureDiastolic, 10)
    if (isNaN(diaVal) || diaVal < VITAL_SIGN_LIMITS.BP_DIASTOLIC.min || diaVal > VITAL_SIGN_LIMITS.BP_DIASTOLIC.max) {
      errors.bloodPressureDiastolic = `Huyết áp tâm trương ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.BP_DIASTOLIC.min} - ${VITAL_SIGN_LIMITS.BP_DIASTOLIC.max} mmHg).`
    }
  }

  // Systolic must be greater than diastolic
  if (sysVal != null && diaVal != null && !errors.bloodPressureSystolic && !errors.bloodPressureDiastolic) {
    if (sysVal <= diaVal) {
      errors.bloodPressure = 'Huyết áp tâm thu phải lớn hơn huyết áp tâm trương.'
    }
  }

  // Temperature (30.0 - 45.0)
  if (!isEmpty(values.temperature)) {
    const val = parseFloat(values.temperature)
    if (isNaN(val) || val < VITAL_SIGN_LIMITS.TEMPERATURE.min || val > VITAL_SIGN_LIMITS.TEMPERATURE.max) {
      errors.temperature = `Nhiệt độ ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.TEMPERATURE.min.toFixed(1)} - ${VITAL_SIGN_LIMITS.TEMPERATURE.max.toFixed(1)} °C).`
    }
  }

  // Respiratory rate (5 - 60)
  if (!isEmpty(values.respiratoryRate)) {
    const val = parseInt(values.respiratoryRate, 10)
    if (isNaN(val) || val < VITAL_SIGN_LIMITS.RESPIRATORY_RATE.min || val > VITAL_SIGN_LIMITS.RESPIRATORY_RATE.max) {
      errors.respiratoryRate = `Nhịp thở ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.RESPIRATORY_RATE.min} - ${VITAL_SIGN_LIMITS.RESPIRATORY_RATE.max} lần/phút).`
    }
  }

  // Weight (0.5 - 300.0)
  let wVal = null
  if (!isEmpty(values.weight)) {
    wVal = parseFloat(values.weight)
    if (isNaN(wVal) || wVal < VITAL_SIGN_LIMITS.WEIGHT.min || wVal > VITAL_SIGN_LIMITS.WEIGHT.max) {
      errors.weight = `Cân nặng ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.WEIGHT.min.toFixed(1)} - ${VITAL_SIGN_LIMITS.WEIGHT.max.toFixed(1)} kg).`
    }
  }

  // Height (20.0 - 250.0)
  let hVal = null
  if (!isEmpty(values.height)) {
    hVal = parseFloat(values.height)
    if (isNaN(hVal) || hVal < VITAL_SIGN_LIMITS.HEIGHT.min || hVal > VITAL_SIGN_LIMITS.HEIGHT.max) {
      errors.height = `Chiều cao ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.HEIGHT.min.toFixed(1)} - ${VITAL_SIGN_LIMITS.HEIGHT.max.toFixed(1)} cm).`
    }
  }

  // SpO2 (50 - 100)
  if (!isEmpty(values.spo2)) {
    const val = parseInt(values.spo2, 10)
    if (isNaN(val) || val < VITAL_SIGN_LIMITS.SPO2.min || val > VITAL_SIGN_LIMITS.SPO2.max) {
      errors.spo2 = `SpO2 ngoài khoảng hợp lệ (${VITAL_SIGN_LIMITS.SPO2.min} - ${VITAL_SIGN_LIMITS.SPO2.max} %).`
    }
  }

  // Note (max 500 chars)
  if (values.note && values.note.length > VITAL_SIGN_LIMITS.NOTE_MAX_LENGTH) {
    errors.note = `Ghi chú tối đa ${VITAL_SIGN_LIMITS.NOTE_MAX_LENGTH} ký tự.`
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
  }
}

/**
 * Format blood pressure for display: "120/80 mmHg" or "—"
 */
export const formatBloodPressure = (systolic, diastolic) => {
  if (systolic == null && diastolic == null) return '—'
  if (systolic != null && diastolic != null) return `${systolic}/${diastolic} mmHg`
  if (systolic != null) return `${systolic}/-- mmHg`
  return `--/${diastolic} mmHg`
}

/**
 * Parse Blood Pressure string (e.g. "120/80" or "120 / 80") into systolic & diastolic numbers
 */
export const parseBloodPressureString = (bpStr) => {
  if (!bpStr || typeof bpStr !== 'string') return { systolic: null, diastolic: null }
  const match = bpStr.trim().match(/^(\d{2,3})\s*[\/\-]\s*(\d{2,3})$/)
  if (match) {
    return {
      systolic: parseInt(match[1], 10),
      diastolic: parseInt(match[2], 10),
    }
  }
  return { systolic: null, diastolic: null }
}

/**
 * Map error responses from Backend to user-friendly Vietnamese messages
 */
export const mapVitalSignErrorMessage = (error, defaultMsg = 'Không thể lưu chỉ số sinh tồn. Vui lòng thử lại.') => {
  if (!error) return defaultMsg

  const code = error?.response?.data?.code || error?.code || ''
  const status = error?.response?.status || error?.status
  const backendMsg = error?.response?.data?.message || error?.message || ''

  if (code === 'VISIT_INVALID_STATUS' || backendMsg.includes('Chỉ được ghi nhận chỉ số sinh tồn khi lượt khám đang diễn ra')) {
    return 'Chỉ được ghi nhận chỉ số sinh tồn khi lượt khám đang diễn ra (IN_PROGRESS hoặc WAITING_FOR_RESULT).'
  }

  if (code === 'MEDICAL_RECORD_LOCKED' || backendMsg.includes('locked') || backendMsg.includes('đã được ký')) {
    return 'Bệnh án đã được ký số hoặc bị khóa nội dung (QTN-07), không thể thay đổi chỉ số sinh tồn.'
  }

  if (status === 403 || code === 'FORBIDDEN' || code === 'ACCESS_DENIED' || backendMsg.includes('Chỉ bác sĩ phụ trách')) {
    return backendMsg || 'Bạn không có quyền ghi hoặc sửa chỉ số sinh tồn cho lượt khám này.'
  }

  if (code === 'VALIDATION_FAILED' || status === 400) {
    return backendMsg || 'Dữ liệu chỉ số sinh tồn không hợp lệ. Vui lòng kiểm tra lại các giá trị đo.'
  }

  if (code === 'VISIT_NOT_FOUND' || code === 'PATIENT_NOT_FOUND') {
    return 'Không tìm thấy thông tin lượt khám hoặc hồ sơ bệnh nhân trong hệ thống.'
  }

  if (backendMsg && typeof backendMsg === 'string' && backendMsg !== 'Validation error') {
    return backendMsg
  }

  return defaultMsg
}

/**
 * Check if the current user can manage (record/update) vital signs for a visit
 * Role DOCTOR (matching visit doctor) or ADMIN
 */
export const canUserManageVitalSigns = (user, visitDoctorId) => {
  if (!user) return false
  const roles = (Array.isArray(user.roles) ? user.roles : [user.role]).filter(Boolean).map((r) => String(r).toUpperCase())
  const isAdmin = roles.some((r) => r.includes('ADMIN'))
  if (isAdmin) return true

  const isDoctor = roles.some((r) => r.includes('DOCTOR'))
  if (!isDoctor) return false

  if (!visitDoctorId) return true // doctor assigned later or open
  const currentUserId = user.id || user.userId
  return String(currentUserId) === String(visitDoctorId)
}
