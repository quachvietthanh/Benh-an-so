const PATIENTS_KEY = 'app_patients'
const APPOINTMENTS_KEY = 'app_appointments'
const QUEUE_KEY = 'app_queue'
const MEDICAL_RECORDS_KEY = 'app_medical_records'
const PRESCRIPTIONS_KEY = 'app_prescriptions'
const MEDICINES_KEY = 'app_medicines'
const BATCHES_KEY = 'app_batches'
const INVOICES_KEY = 'app_invoices'
const AUDIT_LOGS_KEY = 'app_audit_logs'

export const demoBatches = []
export const demoInvoices = []
export const demoAuditLogs = []

export const getStoredAppointments = () => {
  try {
    const raw = localStorage.getItem(APPOINTMENTS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredAppointment = (appointment) => {
  try {
    const current = getStoredAppointments()
    const updated = [appointment, ...current.filter((a) => String(a.id) !== String(appointment.id))]
    localStorage.setItem(APPOINTMENTS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeAppointments = (apiAppointments = []) => {
  const localApps = getStoredAppointments()
  const map = new Map()

  if (Array.isArray(apiAppointments) && apiAppointments.length) {
    apiAppointments.forEach((item) => map.set(String(item.id), item))
  }
  localApps.forEach((item) => {
    const existing = map.get(String(item.id))
    map.set(String(item.id), existing ? { ...existing, ...item } : item)
  })

  return Array.from(map.values())
}

export const getStoredQueue = () => {
  try {
    const raw = localStorage.getItem(QUEUE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredQueueItem = (item) => {
  try {
    const current = getStoredQueue()
    const updated = [item, ...current.filter((q) => String(q.id) !== String(item.id))]
    localStorage.setItem(QUEUE_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeQueue = (apiQueue = []) => {
  const localQueue = getStoredQueue()
  const map = new Map()

  if (Array.isArray(apiQueue) && apiQueue.length) {
    apiQueue.forEach((item) => map.set(String(item.id), item))
  }
  localQueue.forEach((item) => {
    const existing = map.get(String(item.id))
    map.set(String(item.id), existing ? { ...existing, ...item } : item)
  })

  return Array.from(map.values())
}


export const getStoredPatients = () => {
  try {
    const raw = localStorage.getItem(PATIENTS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredPatient = (patient) => {
  try {
    const current = getStoredPatients()
    const updated = [patient, ...current.filter((p) => String(p.id) !== String(patient.id) && String(p.patientCode) !== String(patient.patientCode))]
    localStorage.setItem(PATIENTS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergePatients = (apiPatients = []) => {
  const localPatients = getStoredPatients()
  const map = new Map()

  if (Array.isArray(apiPatients) && apiPatients.length) {
    apiPatients.forEach((item) => map.set(String(item.id), item))
  }
  localPatients.forEach((item) => {
    const existing = map.get(String(item.id))
    map.set(String(item.id), existing ? { ...existing, ...item } : item)
  })

  return Array.from(map.values())
}


const sanitizePatientRecord = (item, index = 0) => {
  if (!item) return item
  const name = item.patientName || ''
  const code = item.patientCode || ''

  let updated = { ...item }

  if (name.includes('Nguyen Van An') || name.includes('Nguyễn Văn An') || code === 'BN000001' || code === 'BN-2026001') {
    const activePatient = index % 2 === 0
      ? { id: 'p1', patientCode: 'BN000009', fullName: 'Nguyen Tuan Long' }
      : { id: 'p2', patientCode: 'BN-2026002', fullName: 'Trần Thị Bình' }

    updated = {
      ...updated,
      patientId: activePatient.id,
      patientName: activePatient.fullName,
      patientCode: activePatient.patientCode,
    }
  }

  if (updated.doctorName === 'admin' || !updated.doctorName) {
    updated.doctorName = 'BS. Phạm Hồng Anh'
  }

  return updated
}

export const getStoredMedicalRecords = () => {
  try {
    const raw = localStorage.getItem(MEDICAL_RECORDS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    const sanitized = parsed.map((item, idx) => sanitizePatientRecord(item, idx))
    localStorage.setItem(MEDICAL_RECORDS_KEY, JSON.stringify(sanitized))
    return sanitized
  } catch {
    return []
  }
}

export const saveStoredMedicalRecord = (record) => {
  try {
    const current = getStoredMedicalRecords()
    const sanitizedRecord = sanitizePatientRecord(record)
    const updated = [sanitizedRecord, ...current.filter((r) => r.id !== record.id)]
    localStorage.setItem(MEDICAL_RECORDS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeMedicalRecords = (apiRecords = []) => {
  const localRecords = getStoredMedicalRecords()
  const map = new Map()

  if (Array.isArray(apiRecords) && apiRecords.length) {
    apiRecords.forEach((item, idx) => map.set(item.id, sanitizePatientRecord(item, idx)))
  }
  localRecords.forEach((item, idx) => map.set(item.id, sanitizePatientRecord(item, idx)))

  return Array.from(map.values()).map((item, idx) => sanitizePatientRecord(item, idx))
}

export const getStoredPrescriptions = () => {
  try {
    const raw = localStorage.getItem(PRESCRIPTIONS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    const sanitized = parsed.map((item, idx) => sanitizePatientRecord(item, idx))
    localStorage.setItem(PRESCRIPTIONS_KEY, JSON.stringify(sanitized))
    return sanitized
  } catch {
    return []
  }
}

export const saveStoredPrescription = (prescription) => {
  try {
    const current = getStoredPrescriptions()
    const sanitizedPresc = sanitizePatientRecord(prescription)
    const updated = [sanitizedPresc, ...current.filter((p) => p.id !== prescription.id)]
    localStorage.setItem(PRESCRIPTIONS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const deleteStoredPrescription = (id) => {
  try {
    const current = getStoredPrescriptions()
    const updated = current.filter((p) => p.id !== id && p.prescriptionCode !== id)
    localStorage.setItem(PRESCRIPTIONS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergePrescriptions = (apiPrescriptions = []) => {
  const localPrescriptions = getStoredPrescriptions()
  const map = new Map()

  if (Array.isArray(apiPrescriptions) && apiPrescriptions.length) {
    apiPrescriptions.forEach((item, idx) => map.set(item.id, sanitizePatientRecord(item, idx)))
  }
  localPrescriptions.forEach((item, idx) => map.set(item.id, sanitizePatientRecord(item, idx)))

  return Array.from(map.values()).map((item, idx) => sanitizePatientRecord(item, idx))
}

export const getStoredMedicines = () => {
  try {
    const raw = localStorage.getItem(MEDICINES_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredMedicine = (medicine) => {
  try {
    const current = getStoredMedicines()
    const updated = [medicine, ...current.filter((m) => m.id !== medicine.id)]
    localStorage.setItem(MEDICINES_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeMedicines = (apiMedicines = []) => {
  const localMeds = getStoredMedicines()
  const map = new Map()

  if (Array.isArray(apiMedicines) && apiMedicines.length) {
    apiMedicines.forEach((item) => map.set(String(item.id), item))
    localMeds.forEach((item) => {
      if (String(item.id).startsWith('med-') && !map.has(String(item.id))) {
        map.set(String(item.id), item)
      }
    })
  } else {
    localMeds.forEach((item) => map.set(String(item.id), item))
  }

  return Array.from(map.values())
}

export const getStoredBatches = () => {
  try {
    const raw = localStorage.getItem(BATCHES_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredBatch = (batch) => {
  try {
    const current = getStoredBatches()
    const updated = [batch, ...current.filter((b) => b.id !== batch.id)]
    localStorage.setItem(BATCHES_KEY, JSON.stringify(updated))

    const allMedicines = mergeMedicines([])
    const targetMed = allMedicines.find((m) => m.id === batch.medicineId)
    if (targetMed) {
      const newStock = Number(targetMed.stock || 0) + Number(batch.quantity || 0)
      saveStoredMedicine({ ...targetMed, stock: newStock })
    }

    return updated
  } catch {
    return []
  }
}

export const mergeBatches = (apiBatches = []) => {
  const localBatches = getStoredBatches()
  const map = new Map()

  if (Array.isArray(apiBatches) && apiBatches.length) {
    apiBatches.forEach((item) => map.set(item.id, item))
  }
  localBatches.forEach((item) => map.set(item.id, item))

  return Array.from(map.values())
}

export const dispensePrescriptionHelper = (prescriptionId) => {
  const prescriptions = mergePrescriptions([])
  const target = prescriptions.find((p) => p.id === prescriptionId)
  if (!target) throw new Error('Không tìm thấy đơn thuốc')

  if (target.status === 'DISPENSED' || target.status === 'COMPLETED') {
    throw new Error('Đơn thuốc đã được cấp phát trước đó')
  }

  let items = []
  try {
    items = typeof target.items === 'string' ? JSON.parse(target.items) : (target.items || [])
  } catch {
    items = []
  }

  const allMeds = mergeMedicines([])

  for (const item of items) {
    const med = allMeds.find((m) => m.id === item.medicineId)
    if (med && Number(med.stock) < Number(item.quantity)) {
      throw new Error(`Thuốc ${med.name} không đủ tồn kho để cấp phát (tồn: ${med.stock}, cần: ${item.quantity})`)
    }
  }

  for (const item of items) {
    const med = allMeds.find((m) => m.id === item.medicineId)
    if (med) {
      const newStock = Math.max(0, Number(med.stock) - Number(item.quantity))
      saveStoredMedicine({ ...med, stock: newStock })
    }
  }

  const updatedPrescription = { ...target, status: 'DISPENSED', dispensedAt: new Date().toISOString() }
  saveStoredPrescription(updatedPrescription)

  return updatedPrescription
}

export const getStoredInvoices = () => {
  try {
    const raw = localStorage.getItem(INVOICES_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    const filtered = parsed.filter((inv) => !inv.invoiceCode?.includes('20260730083221') && !inv.invoiceCode?.includes('HD-0001') && !inv.invoiceCode?.includes('HD-0002'))
    if (filtered.length !== parsed.length) {
      localStorage.setItem(INVOICES_KEY, JSON.stringify(filtered))
    }
    return filtered
  } catch {
    return []
  }
}

export const saveStoredInvoice = (invoice) => {
  try {
    const current = getStoredInvoices()
    const sanitizedInvoice = sanitizePatientRecord(invoice)
    const updated = [sanitizedInvoice, ...current.filter((i) => i.id !== invoice.id)]
    localStorage.setItem(INVOICES_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeInvoices = (apiInvoices = []) => {
  const localInvoices = getStoredInvoices()
  const map = new Map()

  if (Array.isArray(apiInvoices) && apiInvoices.length) {
    apiInvoices.forEach((item, idx) => map.set(item.id, sanitizePatientRecord(item, idx)))
  }
  localInvoices.forEach((item, idx) => map.set(item.id, sanitizePatientRecord(item, idx)))

  return Array.from(map.values()).map((item, idx) => sanitizePatientRecord(item, idx))
}

export const clearLegacyMockInvoices = () => {
  try {
    localStorage.setItem(INVOICES_KEY, JSON.stringify([]))
  } catch {
    // silent
  }
}

export const getPayableItems = () => {
  const records = getStoredMedicalRecords()
  const prescriptions = getStoredPrescriptions()
  const invoices = getStoredInvoices()

  const paidRecordIds = new Set(invoices.map((inv) => inv.medicalRecordId).filter(Boolean))
  const paidPrescriptionIds = new Set(invoices.map((inv) => inv.prescriptionId).filter(Boolean))

  const payableList = []

  // Add prescriptions awaiting payment
  prescriptions.forEach((p) => {
    if (!paidPrescriptionIds.has(p.id) && (p.status === 'PENDING_DISPENSING' || p.status === 'PENDING')) {
      payableList.push({
        id: `payable-p-${p.id}`,
        prescriptionId: p.id,
        medicalRecordId: p.medicalRecordId,
        patientId: p.patientId,
        patientName: p.patientName || 'Bệnh nhân',
        patientCode: p.patientCode || 'BN-001',
        doctorName: p.doctorName || 'BS. Phạm Hồng Anh',
        department: p.department || 'Khoa Khám Bệnh',
        encounterCode: p.prescriptionCode,
        prescriptionCode: p.prescriptionCode,
        status: p.status,
        medicineFee: 150000,
        examFee: 100000,
      })
    }
  })

  return payableList
}

export const payEncounterHelper = ({ prescriptionId, examFee = 100000, medicineFee = 150000, paymentMethod = 'CASH' }) => {
  const payables = getPayableItems()
  const payable = payables.find((p) => p.prescriptionId === prescriptionId) || payables[0]

  const totalAmount = Number(examFee || 0) + Number(medicineFee || 0)
  const methodLabels = { CASH: 'Tiền mặt', TRANSFER: 'Chuyển khoản', CARD: 'Thẻ ATM / Thẻ tín dụng' }

  const newInvoice = {
    id: `inv-${Date.now()}`,
    invoiceCode: `HD-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`,
    prescriptionId,
    medicalRecordId: payable?.medicalRecordId,
    patientName: payable?.patientName || 'Bệnh nhân',
    invoiceType: 'ORIGINAL',
    originalInvoiceCode: null,
    examFee: Number(examFee),
    medicineFee: Number(medicineFee),
    totalAmount,
    paymentMethod,
    paymentMethodLabel: methodLabels[paymentMethod] || 'Tiền mặt',
    status: 'PAID',
    createdAt: new Date().toISOString(),
    adjustmentReason: null,
  }

  saveStoredInvoice(newInvoice)
  return newInvoice
}

export const adjustInvoiceHelper = (originalInvoice, { adjustmentAmount, reason }) => {
  const amount = Number(adjustmentAmount)
  const newInvoice = {
    id: `inv-adj-${Date.now()}`,
    invoiceCode: `HD-DC-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`,
    prescriptionId: originalInvoice.prescriptionId,
    medicalRecordId: originalInvoice.medicalRecordId,
    patientName: originalInvoice.patientName,
    invoiceType: 'ADJUSTMENT',
    originalInvoiceCode: originalInvoice.invoiceCode,
    examFee: 0,
    medicineFee: 0,
    totalAmount: amount,
    paymentMethod: originalInvoice.paymentMethod,
    paymentMethodLabel: originalInvoice.paymentMethodLabel,
    status: 'PAID',
    createdAt: new Date().toISOString(),
    adjustmentReason: reason,
  }

  saveStoredInvoice(newInvoice)
  return newInvoice
}

// Audit Logs Helpers
export const getStoredAuditLogs = () => {
  try {
    const raw = localStorage.getItem(AUDIT_LOGS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const logMedicalAccess = ({ userName, patientName, recordCode, action }) => {
  try {
    const current = getStoredAuditLogs()
    const newLog = {
      id: `log-${Date.now()}`,
      userName: userName || 'Người dùng',
      patientName: patientName || 'Bệnh nhân',
      recordCode: recordCode || 'BA-0001',
      action: action || 'Xem bệnh án',
      accessedAt: new Date().toISOString(),
    }
    const updated = [newLog, ...current]
    localStorage.setItem(AUDIT_LOGS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeAuditLogs = (apiLogs = []) => {
  const localLogs = getStoredAuditLogs()
  const map = new Map()

  if (Array.isArray(apiLogs) && apiLogs.length) {
    apiLogs.forEach((item) => map.set(item.id, item))
  }
  localLogs.forEach((item) => map.set(item.id, item))

  return Array.from(map.values())
}

const CLINICAL_ORDERS_KEY = 'app_clinical_orders'

export const getStoredClinicalOrders = () => {
  try {
    const raw = localStorage.getItem(CLINICAL_ORDERS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredClinicalOrder = (order) => {
  try {
    const current = getStoredClinicalOrders()
    const updated = [order, ...current.filter((o) => o.id !== order.id && o.orderCode !== order.orderCode)]
    localStorage.setItem(CLINICAL_ORDERS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const deleteStoredClinicalOrder = (id) => {
  try {
    const current = getStoredClinicalOrders()
    const updated = current.filter((o) => o.id !== id && o.orderCode !== id)
    localStorage.setItem(CLINICAL_ORDERS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeClinicalOrders = (defaultOrders = []) => {
  const localOrders = getStoredClinicalOrders()
  const map = new Map()

  if (Array.isArray(localOrders) && localOrders.length) {
    localOrders.forEach((item) => map.set(item.id, item))
  }
  if (Array.isArray(defaultOrders) && defaultOrders.length) {
    defaultOrders.forEach((item) => {
      if (!map.has(item.id)) {
        map.set(item.id, item)
      }
    })
  }

  return Array.from(map.values())
}

