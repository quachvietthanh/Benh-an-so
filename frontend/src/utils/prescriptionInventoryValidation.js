export const getAvailableStock = (med) => {
  if (!med) return 0
  const stock = med.availableStock ?? med.stockQuantity ?? med.availableQuantity ?? med.stock
  const num = Number(stock)
  return !isNaN(num) && num >= 0 ? num : 0
}

export const sortMedicinesByStockAvailability = (medicines = []) => {
  if (!Array.isArray(medicines)) return []
  return [...medicines].sort((a, b) => {
    const stockA = getAvailableStock(a)
    const stockB = getAvailableStock(b)
    const isAvailA = stockA > 0 ? 1 : 0
    const isAvailB = stockB > 0 ? 1 : 0

    if (isAvailA !== isAvailB) {
      return isAvailB - isAvailA
    }

    const nameA = String(a.medicineName || a.name || '').toLowerCase()
    const nameB = String(b.medicineName || b.name || '').toLowerCase()
    return nameA.localeCompare(nameB, 'vi')
  })
}

export const validateItemStock = (item, medicinesData) => {
  if (!item || !item.medicineId) {
    return { isValid: true, error: null, availableStock: 0, medicineName: '' }
  }

  let medicine = null
  if (medicinesData instanceof Map) {
    medicine = medicinesData.get(String(item.medicineId))
  } else if (Array.isArray(medicinesData)) {
    medicine = medicinesData.find((m) => String(m.id) === String(item.medicineId))
  }

  const name = medicine?.medicineName || medicine?.name || `Mã ${item.medicineId}`
  const unit = medicine?.unit || 'viên'

  if (!medicine) {
    return {
      isValid: false,
      error: `Không tìm thấy thông tin thuốc (${name}) trong hệ thống.`,
      availableStock: 0,
      medicineName: name,
    }
  }

  const availableStock = getAvailableStock(medicine)
  const qty = Number(item.quantity || 0)

  if (availableStock <= 0) {
    return {
      isValid: false,
      error: `Thuốc "${name}" hiện đã hết hàng (tồn 0 ${unit}).`,
      availableStock,
      medicineName: name,
    }
  }

  if (qty > availableStock) {
    return {
      isValid: false,
      error: `Số lượng kê (${qty} ${unit}) vượt quá tồn kho khả dụng (${availableStock} ${unit}) của thuốc "${name}".`,
      availableStock,
      medicineName: name,
    }
  }

  return { isValid: true, error: null, availableStock, medicineName: name }
}

export const validatePrescriptionStock = (items = [], medicinesData = []) => {
  const errors = []
  const outOfStockItems = []
  const insufficientStockItems = []

  const validItems = (items || []).filter((i) => Boolean(i.medicineId))

  if (validItems.length === 0) {
    return { isValid: true, errors: [], outOfStockItems: [], insufficientStockItems: [] }
  }

  for (let idx = 0; idx < validItems.length; idx++) {
    const item = validItems[idx]
    const result = validateItemStock(item, medicinesData)
    if (!result.isValid) {
      errors.push(result.error)
      if (result.availableStock <= 0) {
        outOfStockItems.push({ item, result })
      } else {
        insufficientStockItems.push({ item, result })
      }
    }
  }

  return {
    isValid: errors.length === 0,
    errors,
    outOfStockItems,
    insufficientStockItems,
  }
}
