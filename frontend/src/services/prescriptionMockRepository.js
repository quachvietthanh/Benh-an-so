import { drugInteractions, demoPrescriptions } from '../mock-data/mockData'
import { getStoredPrescriptions, saveStoredPrescription, getStoredMedicalRecords, mergeMedicines } from '../utils/storageHelpers'

const PRESCRIPTION_HISTORY_KEY = 'app_prescription_history'
const deepClone = (data) => JSON.parse(JSON.stringify(data))

export const getMockPrescriptions = () => {
  const stored = getStoredPrescriptions()
  if (stored && stored.length > 0) {
    return deepClone(stored)
  }
  return deepClone(demoPrescriptions)
}

export const getMockPrescriptionById = (id) => {
  const all = getMockPrescriptions()
  return all.find((p) => String(p.id) === String(id) || String(p.prescriptionCode) === String(id)) || null
}

export const getMockPrescriptionsByVisitOrRecord = (recordIdOrVisitId) => {
  const all = getMockPrescriptions()
  return all.filter((p) => 
    String(p.medicalRecordId) === String(recordIdOrVisitId) || 
    String(p.visitId) === String(recordIdOrVisitId)
  )
}

export const getMockPrescriptionHistory = (prescriptionId) => {
  try {
    const raw = localStorage.getItem(PRESCRIPTION_HISTORY_KEY)
    const logs = raw ? JSON.parse(raw) : []
    return logs.filter((log) => String(log.prescriptionId) === String(prescriptionId))
  } catch {
    return []
  }
}

export const saveMockPrescriptionHistory = (historyLog) => {
  try {
    const raw = localStorage.getItem(PRESCRIPTION_HISTORY_KEY)
    const current = raw ? JSON.parse(raw) : []
    const updated = [historyLog, ...current]
    localStorage.setItem(PRESCRIPTION_HISTORY_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const checkMockDrugInteractions = (medicineIds = []) => {
  const cleanIds = [...new Set(medicineIds.filter(Boolean).map(String))]
  if (cleanIds.length < 2) return []

  const detected = []
  const rules = drugInteractions || []
  const allMeds = mergeMedicines([])

  for (let i = 0; i < cleanIds.length; i++) {
    for (let j = i + 1; j < cleanIds.length; j++) {
      const drugA = cleanIds[i]
      const drugB = cleanIds[j]

      const medAObj = allMeds.find((m) => String(m.id) === drugA)
      const medBObj = allMeds.find((m) => String(m.id) === drugB)

      const matchedRule = rules.find((rule) => {
        const ruleDrugs = (rule.drugs || []).map(String)
        const hasIdMatch = (ruleDrugs.includes(drugA) && ruleDrugs.includes(drugB))

        const ingA = (rule.ingredientA || '').toLowerCase()
        const ingB = (rule.ingredientB || '').toLowerCase()
        const medAIng = (medAObj?.activeIngredient || medAObj?.name || '').toLowerCase()
        const medBIng = (medBObj?.activeIngredient || medBObj?.name || '').toLowerCase()

        const hasIngMatch =
          (medAIng.includes(ingA) && medBIng.includes(ingB)) ||
          (medAIng.includes(ingB) && medBIng.includes(ingA))

        return hasIdMatch || (ingA && ingB && hasIngMatch)
      })

      if (matchedRule) {
        const medAObj = allMeds.find((m) => String(m.id) === drugA)
        const medBObj = allMeds.find((m) => String(m.id) === drugB)

        detected.push({
          ruleId: matchedRule.id || `rule-${drugA}-${drugB}`,
          drugIdA: drugA,
          drugIdB: drugB,
          drugNameA: medAObj?.name || medAObj?.medicineName || drugA,
          drugNameB: medBObj?.name || medBObj?.medicineName || drugB,
          severity: matchedRule.severity || 'Cảnh báo (Nghiêm trọng)',
          description: matchedRule.description || `Phát hiện tương tác giữa ${medAObj?.name || drugA} và ${medBObj?.name || drugB}`,
          clinicalRecommendation: matchedRule.clinicalRecommendation || 'Cân nhắc điều chỉnh liều hoặc thay thế thuốc.',
          isDemoRule: true,
        })
      }
    }
  }

  return detected
}

export const saveMockPrescription = (data, currentUser) => {
  const now = new Date().toISOString()
  const newPrescription = {
    id: data.id || `presc-${Date.now()}`,
    prescriptionCode: data.prescriptionCode || `DT-${Date.now().toString().slice(-8)}`,
    visitId: data.visitId,
    medicalRecordId: data.medicalRecordId || data.visitId,
    patientId: data.patientId,
    patientName: data.patientName || 'Bệnh nhân',
    doctorId: data.doctorId || currentUser?.id,
    doctorName: data.doctorName || currentUser?.fullName || currentUser?.username || 'Bác sĩ',
    status: 'PENDING_DISPENSE',
    note: data.note || '',
    items: deepClone(data.items || []),
    interactionOverrides: deepClone(data.interactionOverrides || []),
    createdAt: now,
    createdBy: currentUser?.username || currentUser?.fullName || 'Doctor',
    updatedAt: now,
    updatedBy: currentUser?.username || currentUser?.fullName || 'Doctor',
  }

  saveStoredPrescription(newPrescription)

  saveMockPrescriptionHistory({
    id: `hist-${Date.now()}`,
    prescriptionId: newPrescription.id,
    action: 'CREATE',
    changedBy: currentUser?.fullName || currentUser?.username || 'Bác sĩ',
    changedAt: now,
    before: null,
    after: deepClone(newPrescription),
    changeReason: 'Khởi tạo đơn thuốc mới',
  })

  return newPrescription
}

export const updateMockPrescription = (id, data, currentUser) => {
  const existing = getMockPrescriptionById(id)
  if (!existing) {
    throw new Error('Không tìm thấy đơn thuốc để điều chỉnh')
  }

  if (existing.status === 'DISPENSED') {
    throw new Error('Đơn thuốc đã được cấp phát và không thể điều chỉnh.')
  }

  const beforeState = deepClone(existing)
  const now = new Date().toISOString()

  const updatedPrescription = {
    ...existing,
    note: data.note !== undefined ? data.note : existing.note,
    items: deepClone(data.items),
    interactionOverrides: deepClone(data.interactionOverrides || existing.interactionOverrides || []),
    updatedAt: now,
    updatedBy: currentUser?.fullName || currentUser?.username || 'Bác sĩ',
  }

  saveStoredPrescription(updatedPrescription)

  saveMockPrescriptionHistory({
    id: `hist-${Date.now()}`,
    prescriptionId: updatedPrescription.id,
    action: 'UPDATE',
    changedBy: currentUser?.fullName || currentUser?.username || 'Bác sĩ',
    changedAt: now,
    before: beforeState,
    after: deepClone(updatedPrescription),
    changeReason: data.changeReason || 'Điều chỉnh liều/số lượng/danh mục thuốc',
  })

  return updatedPrescription
}
