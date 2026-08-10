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

export const normalizeMedicalRecordDetail = (detail) => {
  if (!detail) return null
  const diagnoses = Array.isArray(detail.diagnoses) ? detail.diagnoses : []
  const primary = diagnoses.find((item) => item.diagnosisType === 'PRIMARY') || diagnoses[0]

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
    diagnosis: primary
      ? `[${primary.diagnosisCode || primary.code}] ${primary.diagnosisName || primary.name}`
      : compactText([
          detail.primaryIcdCode && `[${detail.primaryIcdCode}]`,
          detail.primaryIcdName,
        ]),
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
          Number(batch.quantity) > 0,
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
