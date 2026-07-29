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


export const getStoredMedicalRecords = () => {
  try {
    const raw = localStorage.getItem(MEDICAL_RECORDS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredMedicalRecord = (record) => {
  try {
    const current = getStoredMedicalRecords()
    const updated = [record, ...current.filter((r) => r.id !== record.id)]
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
    apiRecords.forEach((item) => map.set(item.id, item))
  }
  localRecords.forEach((item) => map.set(item.id, item))

  return Array.from(map.values())
}

export const getStoredPrescriptions = () => {
  try {
    const raw = localStorage.getItem(PRESCRIPTIONS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredPrescription = (prescription) => {
  try {
    const current = getStoredPrescriptions()
    const updated = [prescription, ...current.filter((p) => p.id !== prescription.id)]
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
    apiPrescriptions.forEach((item) => map.set(item.id, item))
  }
  localPrescriptions.forEach((item) => map.set(item.id, item))

  return Array.from(map.values())
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
    apiMedicines.forEach((item) => map.set(item.id, item))
  }
  localMeds.forEach((item) => map.set(item.id, item))

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
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredInvoice = (invoice) => {
  try {
    const current = getStoredInvoices()
    const updated = [invoice, ...current.filter((i) => i.id !== invoice.id)]
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
    apiInvoices.forEach((item) => map.set(item.id, item))
  }
  localInvoices.forEach((item) => map.set(item.id, item))

  return Array.from(map.values())
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
    if (!paidPrescriptionIds.has(p.id)) {
      payableList.push({
        id: `payable-p-${p.id}`,
        prescriptionId: p.id,
        medicalRecordId: p.medicalRecordId,
        patientName: p.patientName || 'Bệnh nhân',
        prescriptionCode: p.prescriptionCode,
        status: p.status,
        medicineFee: 150000,
        examFee: 100000,
      })
    }
  })

  // Add medical records awaiting payment
  records.forEach((r) => {
    if (!paidRecordIds.has(r.id) && !payableList.some((p) => p.medicalRecordId === r.id)) {
      payableList.push({
        id: `payable-r-${r.id}`,
        prescriptionId: `pr-${r.id}`,
        medicalRecordId: r.id,
        patientName: r.patientName || 'Bệnh nhân',
        prescriptionCode: r.recordCode,
        status: 'COMPLETED',
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
