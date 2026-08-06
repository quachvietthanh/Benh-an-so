const PATIENTS_KEY = 'app_patients'
const APPOINTMENTS_KEY = 'app_appointments'
const MEDICAL_RECORDS_KEY = 'app_medical_records'
const PRESCRIPTIONS_KEY = 'app_prescriptions'
const MEDICINES_KEY = 'app_medicines'
const BATCHES_KEY = 'app_batches'
const INVOICES_KEY = 'app_invoices'
const AUDIT_LOGS_KEY = 'app_audit_logs'

export const demoBatches = []
export const demoInvoices = []
export const demoAuditLogs = []

export const QUEUES_KEY = 'app_queues'

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
    apiAppointments.forEach((item) => {
      if (item.id) map.set(String(item.id), item)
    })
  }
  localApps.forEach((item) => {
    if (item.id) {
      const existing = map.get(String(item.id))
      map.set(String(item.id), existing ? { ...item, ...existing } : item)
    }
  })

  return Array.from(map.values())
}

export const getStoredQueueItems = () => {
  try {
    const raw = localStorage.getItem(QUEUES_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveStoredQueueItem = (item) => {
  try {
    const current = getStoredQueueItems()
    const updated = [item, ...current.filter((q) => String(q.id) !== String(item.id))]
    localStorage.setItem(QUEUES_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const removeStoredQueueItemByPatient = (patientId) => {
  try {
    const current = getStoredQueueItems()
    const cleanPId = String(patientId || '').toLowerCase().replace(/-/g, '')
    const updated = current.filter((q) => {
      const qPId = String(q.patientId || '').toLowerCase().replace(/-/g, '')
      return qPId !== cleanPId && String(q.patientId) !== String(patientId)
    })
    localStorage.setItem(QUEUES_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeQueues = (apiQueues = []) => {
  const localQueues = getStoredQueueItems()
  const map = new Map()

  if (Array.isArray(apiQueues) && apiQueues.length) {
    apiQueues.forEach((item) => {
      const key = String(item.id || item.medicalQueueId)
      if (key && key !== 'undefined') map.set(key, item)
    })
  }
  localQueues.forEach((item) => {
    const key = String(item.id || item.medicalQueueId)
    if (key && key !== 'undefined') {
      const existing = map.get(key)
      map.set(key, existing ? { ...item, ...existing } : item)
    }
  })

  return Array.from(map.values()).sort((a, b) => {
    const numA = Number(a.queueNumber !== undefined && a.queueNumber !== null ? a.queueNumber : 999999)
    const numB = Number(b.queueNumber !== undefined && b.queueNumber !== null ? b.queueNumber : 999999)
    if (numA !== numB) return numA - numB
    return new Date(a.checkedInAt || 0) - new Date(b.checkedInAt || 0)
  })
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
    apiPatients.forEach((item) => {
      if (item.id) map.set(String(item.id), item)
    })
  }
  localPatients.forEach((item) => {
    if (item.id) {
      const existing = map.get(String(item.id))
      map.set(String(item.id), existing ? { ...item, ...existing } : item)
    }
  })
  // Tự động đồng bộ toàn bộ bệnh nhân từ Hàng đợi (Queue) vào danh sách Bệnh nhân
  const queues = getStoredQueueItems()
  queues.forEach((q) => {
    if (q.patientId && q.patientName) {
      const pIdStr = String(q.patientId)
      const existing = map.get(pIdStr) || Array.from(map.values()).find(p => p.fullName === q.patientName || p.patientCode === q.patientCode)
      if (!existing && !pIdStr.includes('bbbbbbbbb')) {
        const pObj = {
          id: q.patientId,
          patientCode: q.patientCode || `BN${String(q.patientId).slice(-6).toUpperCase()}`,
          fullName: q.patientName,
          phone: q.phone || '0912000000',
          gender: q.gender || 'Nam',
          age: q.age || 30,
          dob: '1995-01-01',
        }
        map.set(String(pObj.id), pObj)
      } else if (!existing && pIdStr.includes('bbbbbbbbb')) {
        const pObj = {
          id: q.patientId,
          patientCode: q.patientCode || 'BN000007',
          fullName: q.patientName || 'Do Quang Huy',
          phone: '0910000007',
          gender: 'Nam',
          age: 28,
          dob: '1998-05-15',
        }
        map.set(String(pObj.id), pObj)
      }
    }
  })

  const mergedList = Array.from(map.values())
  try {
    localStorage.setItem(PATIENTS_KEY, JSON.stringify(mergedList))
  } catch (e) {
    console.warn('Cannot save merged patients:', e)
  }

  return mergedList
}


export const getStoredMedicalRecords = () => {
  try {
    const raw = localStorage.getItem(MEDICAL_RECORDS_KEY)
    if (!raw) return []
    const list = JSON.parse(raw)
    if (!Array.isArray(list)) return []
    const validList = list.filter((r) => r && (r.recordCode || r.patientName || (r.status && r.status !== 'DRAFT')))
    if (validList.length !== list.length) {
      localStorage.setItem(MEDICAL_RECORDS_KEY, JSON.stringify(validList))
    }
    return validList
  } catch {
    return []
  }
}

export const saveStoredMedicalRecord = (record) => {
  try {
    const current = getStoredMedicalRecords()
    const updated = [record, ...current.filter((r) => String(r.id) !== String(record.id))]
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
    apiRecords.forEach((item) => {
      if (item && item.id && (item.recordCode || item.patientName || item.diagnosis)) {
        map.set(String(item.id), item)
      }
    })
  }
  localRecords.forEach((item) => {
    if (item && item.id && (item.recordCode || item.patientName || item.diagnosis)) {
      const existing = map.get(String(item.id))
      map.set(String(item.id), existing ? { ...item, ...existing } : item)
    }
  })

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

export const DEFAULT_MEDICINES = [
  { id: '16000000-0000-0000-0000-000000000001', code: 'MED-PARA-500', name: 'Paracetamol 500 mg', activeIngredient: 'Paracetamol', category: 'Hạ sốt, giảm đau', unit: 'vien', route: 'ORAL', stock: 1500, minStock: 200, active: true, price: 1500 },
  { id: '16000000-0000-0000-0000-000000000003', code: 'MED-AMOX-500', name: 'Amoxicillin 500 mg', activeIngredient: 'Amoxicillin', category: 'Kháng sinh', unit: 'vien', route: 'ORAL', stock: 800, minStock: 150, active: true, price: 3500 },
  { id: '16000000-0000-0000-0000-000000000006', code: 'MED-CEFU-500', name: 'Cefuroxime 500 mg', activeIngredient: 'Cefuroxime', category: 'Kháng sinh', unit: 'vien', route: 'ORAL', stock: 450, minStock: 100, active: true, price: 8500 },
  { id: '16000000-0000-0000-0000-000000000007', code: 'MED-OMEP-20', name: 'Omeprazole 20 mg', activeIngredient: 'Omeprazole', category: 'Dạ dày - Tiêu hóa', unit: 'vien', route: 'ORAL', stock: 600, minStock: 100, active: true, price: 2500 },
  { id: '16000000-0000-0000-0000-000000000019', code: 'MED-AMLO-5', name: 'Amlodipine 5 mg', activeIngredient: 'Amlodipine', category: 'Tim mạch - Huyết áp', unit: 'vien', route: 'ORAL', stock: 900, minStock: 200, active: true, price: 2000 },
  { id: '16000000-0000-0000-0000-000000000016', code: 'MED-METFO-500', name: 'Metformin 500 mg', activeIngredient: 'Metformin', category: 'Nội tiết - Đái tháo đường', unit: 'vien', route: 'ORAL', stock: 1200, minStock: 200, active: true, price: 3000 },
  { id: '16000000-0000-0000-0000-000000000015', code: 'MED-VENTO', name: 'Salbutamol inhaler', activeIngredient: 'Salbutamol', category: 'Hô hấp', unit: 'binh', route: 'INHALATION', stock: 45, minStock: 20, active: true, price: 65000 },
  { id: '16000000-0000-0000-0000-000000000002', code: 'MED-IBU-400', name: 'Ibuprofen 400 mg', activeIngredient: 'Ibuprofen', category: 'Giảm đau, kháng viêm', unit: 'vien', route: 'ORAL', stock: 800, minStock: 100, active: true, price: 2500 },
]

export const DEFAULT_BATCHES = [
  { id: 'batch-1', batchNumber: 'LO-202607-01', lotNumber: 'LO-202607-01', medicineId: '16000000-0000-0000-0000-000000000001', medicineName: 'Paracetamol 500 mg', quantity: 1000, unitCost: 1200, price: 1200, manufacturedDate: '2026-01-15', expiryDate: '2028-01-15', supplier: 'Dược Hậu Giang', importDate: '2026-07-01' },
  { id: 'batch-2', batchNumber: 'LO-202607-02', lotNumber: 'LO-202607-02', medicineId: '16000000-0000-0000-0000-000000000003', medicineName: 'Amoxicillin 500 mg', quantity: 500, unitCost: 2800, price: 2800, manufacturedDate: '2026-02-10', expiryDate: '2027-08-10', supplier: 'Mekophar', importDate: '2026-07-05' },
  { id: 'batch-3', batchNumber: 'LO-202607-03', lotNumber: 'LO-202607-03', medicineId: '16000000-0000-0000-0000-000000000007', medicineName: 'Omeprazole 20 mg', quantity: 600, unitCost: 2000, price: 2000, manufacturedDate: '2026-03-01', expiryDate: '2028-03-01', supplier: 'Traphaco', importDate: '2026-07-10' },
  { id: 'batch-4', batchNumber: 'LO-202607-04', lotNumber: 'LO-202607-04', medicineId: '16000000-0000-0000-0000-000000000019', medicineName: 'Amlodipine 5 mg', quantity: 900, unitCost: 1600, price: 1600, manufacturedDate: '2026-04-12', expiryDate: '2028-04-12', supplier: 'Dược Hà Tây', importDate: '2026-07-12' },
  { id: 'batch-5', batchNumber: 'LO-202607-05', lotNumber: 'LO-202607-05', medicineId: '16000000-0000-0000-0000-000000000016', medicineName: 'Metformin 500 mg', quantity: 1200, unitCost: 2400, price: 2400, manufacturedDate: '2026-05-20', expiryDate: '2028-05-20', supplier: 'Pymepharco', importDate: '2026-07-15' },
]

export const DEFAULT_PRESCRIPTIONS = []

export const mergePrescriptions = (apiPrescriptions = []) => {
  const localPrescriptions = getStoredPrescriptions()
  const map = new Map()

  if (Array.isArray(apiPrescriptions) && apiPrescriptions.length) {
    apiPrescriptions.forEach((item) => {
      const key = String(item.id || item.prescriptionCode)
      if (key && key !== 'undefined') map.set(key, item)
    })
  }
  localPrescriptions.forEach((item) => {
    const key = String(item.id || item.prescriptionCode)
    if (key && key !== 'undefined') {
      const existing = map.get(key)
      map.set(key, existing ? { ...item, ...existing } : item)
    }
  })

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
    const updated = [medicine, ...current.filter((m) => String(m.id) !== String(medicine.id))]
    localStorage.setItem(MEDICINES_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const mergeMedicines = (apiMedicines = []) => {
  const localMeds = getStoredMedicines()
  const map = new Map()

  DEFAULT_MEDICINES.forEach((item) => map.set(String(item.id), item))
  localMeds.forEach((item) => {
    const existing = map.get(String(item.id))
    map.set(String(item.id), existing ? { ...existing, ...item } : item)
  })
  if (Array.isArray(apiMedicines) && apiMedicines.length) {
    apiMedicines.forEach((item) => map.set(String(item.id), item))
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
    const updated = [batch, ...current.filter((b) => String(b.id) !== String(batch.id))]
    localStorage.setItem(BATCHES_KEY, JSON.stringify(updated))

    const allMedicines = mergeMedicines([])
    const targetMed = allMedicines.find((m) => String(m.id) === String(batch.medicineId))
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

  DEFAULT_BATCHES.forEach((item) => map.set(String(item.id), item))
  localBatches.forEach((item) => {
    const existing = map.get(String(item.id))
    map.set(String(item.id), existing ? { ...existing, ...item } : item)
  })
  if (Array.isArray(apiBatches) && apiBatches.length) {
    apiBatches.forEach((item) => map.set(String(item.id), item))
  }

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

  if (Array.isArray(defaultOrders) && defaultOrders.length) {
    defaultOrders.forEach((item) => {
      if (item.id) map.set(String(item.id), item)
    })
  }
  if (Array.isArray(localOrders) && localOrders.length) {
    localOrders.forEach((item) => {
      if (item.id) {
        const existing = map.get(String(item.id))
        map.set(String(item.id), existing ? { ...existing, ...item } : item)
      }
    })
  }

  return Array.from(map.values())
}

const APPOINTMENT_LOGS_KEY = 'app_appointment_logs'
const NOTIFICATION_LOGS_KEY = 'app_notification_logs'

export const getStoredAppointmentLogs = () => {
  try {
    const raw = localStorage.getItem(APPOINTMENT_LOGS_KEY)
    return raw ? JSON.parse(raw) : [
      {
        id: 'alog-1',
        appointmentId: 'a1',
        appointmentCode: 'LH-20260715-001',
        action: 'CREATE',
        operatorName: 'Lê Thị Hạnh (Lễ tân)',
        details: 'Khởi tạo lịch hẹn mới trạng thái ĐÃ ĐẶT',
        timestamp: '2026-07-15T07:30:00',
      },
      {
        id: 'alog-2',
        appointmentId: 'a2',
        appointmentCode: 'LH-20260715-002',
        action: 'CHECK_IN',
        operatorName: 'Lê Thị Hạnh (Lễ tân)',
        details: 'Check-in bệnh nhân vào hàng đợi khám',
        timestamp: '2026-07-15T08:15:00',
      },
    ]
  } catch {
    return []
  }
}

export const saveAppointmentLog = (logData) => {
  try {
    const current = getStoredAppointmentLogs()
    const newLog = {
      id: `alog-${Date.now()}`,
      timestamp: new Date().toISOString(),
      ...logData,
    }
    const updated = [newLog, ...current]
    localStorage.setItem(APPOINTMENT_LOGS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const getStoredNotificationLogs = () => {
  try {
    const raw = localStorage.getItem(NOTIFICATION_LOGS_KEY)
    return raw ? JSON.parse(raw) : [
      {
        id: 'nlog-1',
        appointmentId: 'a1',
        patientName: 'Nguyễn Văn An',
        phone: '0908123456',
        channel: 'SMS/Zalo',
        message: 'Kính gửi Nguyễn Văn An, nhắc lịch hẹn khám BS. Phạm Hồng Anh lúc 08:00 ngày 15/07/2026.',
        status: 'SENT',
        sentAt: '2026-07-15T07:00:00',
      },
    ]
  } catch {
    return []
  }
}

export const saveNotificationLog = (notifData) => {
  try {
    const current = getStoredNotificationLogs()
    const newNotif = {
      id: `nlog-${Date.now()}`,
      sentAt: new Date().toISOString(),
      ...notifData,
    }
    const updated = [newNotif, ...current]
    localStorage.setItem(NOTIFICATION_LOGS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

