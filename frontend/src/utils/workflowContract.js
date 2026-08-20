import dayjs from 'dayjs'

const compactText = (parts) => parts.filter(Boolean).join('; ')

export const unwrapCollection = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

export const getQueueInProgressBlockReason = (queueItem, action = 'tiếp tục') => {
  if (!queueItem?.id) {
    return `Không có queueItemId thật để ${action}.`
  }

  if (!queueItem.status) {
    return `Queue item ${queueItem.id} không có trạng thái để ${action}.`
  }

  if (queueItem.status === 'WAITING_FOR_RESULT') {
    return `Lượt khám đang chờ kết quả cận lâm sàng (WAITING_FOR_RESULT). Chỉ có thể ${action} sau khi kết quả được xác nhận và lượt khám trở lại IN_PROGRESS.`
  }

  if (queueItem.status !== 'IN_PROGRESS') {
    return `Chỉ có thể ${action} khi queue item ở IN_PROGRESS (hiện tại: ${queueItem.status}).`
  }

  return null
}

export const buildMedicalRecordPayload = ({ visitId, values = {}, vitalSigns = {} }) => {
  if (!visitId) throw new Error('visitId is required')

  const vitalNarrative = compactText([
    vitalSigns.bp && `Huyết áp ${vitalSigns.bp} mmHg`,
    vitalSigns.pulse && `Mạch ${vitalSigns.pulse} lần/phút`,
    vitalSigns.temp && `Nhiệt độ ${vitalSigns.temp} °C`,
    vitalSigns.respRate && `Nhịp thở ${vitalSigns.respRate} lần/phút`,
    vitalSigns.spO2 && `SpO2 ${vitalSigns.spO2}%`,
    vitalSigns.weight && `Cân nặng ${vitalSigns.weight} kg`,
    vitalSigns.height && `Chiều cao ${vitalSigns.height} cm`,
  ])

  return {
    visitId,
    chiefComplaint: values.chiefComplaint || values.symptoms || '',
    symptoms: values.symptoms || '',
    medicalHistory: values.medicalHistory || '',
    physicalExamination: compactText([values.examinationNote, vitalNarrative]),
    clinicalProgress: values.clinicalProgress || '',
    treatmentPlan: values.treatmentPlan || '',
    doctorInstructions: values.doctorInstructions || values.treatmentPlan || '',
    conclusion: values.conclusion || '',
  }
}

export const buildDiagnosisPayload = ({ primaryDiagnosis, secondaryDiagnoses = [], note = '' }) => {
  if (!primaryDiagnosis?.id) throw new Error('primary diagnosisCatalogId is required')

  const toDiagnosis = (diagnosis) => ({
    diagnosisCatalogId: diagnosis.id,
    code: diagnosis.code,
    name: diagnosis.name,
    note: diagnosis.note || note || '',
  })

  return {
    primaryDiagnosis: toDiagnosis(primaryDiagnosis),
    secondaryDiagnoses: secondaryDiagnoses.map(toDiagnosis),
  }
}

export const buildClinicalOrderPayload = ({ clinicalReason = '', orders = [] }) => ({
  clinicalReason,
  items: orders.map((order) => ({
    serviceId: order.id,
    instruction: order.note || (order.isUrgent ? 'CẤP CỨU' : ''),
  })),
})

const win1252Map = {
  0x80: 0x20AC, 0x82: 0x201A, 0x83: 0x0192, 0x84: 0x201E, 0x85: 0x2026,
  0x86: 0x2020, 0x87: 0x2021, 0x88: 0x02C6, 0x89: 0x2030, 0x8A: 0x0160,
  0x8B: 0x2039, 0x8C: 0x0152, 0x8E: 0x017D, 0x91: 0x2018, 0x92: 0x2019,
  0x93: 0x201C, 0x94: 0x201D, 0x95: 0x2022, 0x96: 0x2013, 0x97: 0x2014,
  0x98: 0x02DC, 0x99: 0x2122, 0x9A: 0x0161, 0x9B: 0x203A, 0x9C: 0x0153,
  0x9E: 0x017E, 0x9F: 0x0178,
}
const revMap = {}
for (let b = 0; b < 256; b++) {
  const unicode = win1252Map[b] || b
  revMap[unicode] = b
}

export const fixMojibake = (str) => {
  if (!str || typeof str !== 'string') return str || ''
  if (!/[ÃÄÅÆáàâãèéêìíòóôõùúý»¿º]/i.test(str)) return str

  try {
    const bytes = []
    for (let i = 0; i < str.length; i++) {
      const code = str.charCodeAt(i)
      if (revMap[code] !== undefined) {
        bytes.push(revMap[code])
      } else if (code < 256) {
        bytes.push(code)
      } else {
        return str
      }
    }
    const decoded = new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes))
    return decoded || str
  } catch {
    return str
  }
}

export const normalizeMedicalRecordDetail = (detail) => {
  if (!detail) return null
  const diagnoses = Array.isArray(detail.diagnoses) ? detail.diagnoses : []
  const primary = diagnoses.find((item) => item.diagnosisType === 'PRIMARY') || diagnoses[0]

  const rawName = primary ? (primary.diagnosisName || primary.name) : detail.primaryIcdName
  const cleanName = fixMojibake(rawName)
  const code = primary ? (primary.diagnosisCode || primary.code) : detail.primaryIcdCode

  return {
    ...detail,
    id: detail.medicalRecordId || detail.id,
    medicalRecordId: detail.medicalRecordId || detail.id,
    visitId: detail.visit?.id || detail.visitId,
    visitCode: detail.visit?.visitCode || detail.visitCode,
    patientId: detail.patient?.id || detail.patientId,
    patientName: detail.patient?.fullName || detail.patientName,
    patientCode: detail.patient?.patientCode || detail.patientCode,
    doctorId: detail.visit?.doctorId || detail.doctorId,
    doctorName: detail.visit?.doctorName || detail.doctorName,
    diagnosis: code
      ? `[${code}] ${cleanName}`
      : cleanName,
    recordCode: detail.recordCode || detail.medicalRecordId || detail.id,
    createdAt: detail.visit?.startedAt || detail.visit?.visitAt || detail.createdAt,
  }
}

export const buildFefoPreview = (prescriptionItems = [], batches = []) =>
  prescriptionItems.map((item) => {
    const eligibleBatches = batches
      .filter(
        (batch) =>
          String(batch.medicineId) === String(item.medicineId) &&
          batch.eligibleForDispense !== false &&
          batch.status !== 'EXPIRED' &&
          Number(batch.quantity) > 0 &&
          (!batch.expiryDate || !dayjs(batch.expiryDate).isBefore(dayjs(), 'day')),
      )
      .sort((a, b) => String(a.expiryDate).localeCompare(String(b.expiryDate)))

    let remaining = Number(item.quantity) || 0
    const allocations = []
    for (const batch of eligibleBatches) {
      if (remaining <= 0) break
      const quantity = Math.min(remaining, Number(batch.quantity) || 0)
      if (quantity > 0) {
        allocations.push({
          batchId: batch.batchId,
          batchNumber: batch.batchNumber,
          expiryDate: batch.expiryDate,
          quantity,
        })
        remaining -= quantity
      }
    }

    const availableQuantity = eligibleBatches.reduce(
      (sum, batch) => sum + (Number(batch.quantity) || 0),
      0,
    )

    return {
      ...item,
      requiredQuantity: Number(item.quantity) || 0,
      availableQuantity,
      shortageQuantity: Math.max(remaining, 0),
      allocations,
    }
  })
