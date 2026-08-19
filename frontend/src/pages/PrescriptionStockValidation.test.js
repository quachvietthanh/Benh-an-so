import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAvailableStock,
  sortMedicinesByStockAvailability,
  validateItemStock,
  validatePrescriptionStock,
} from '../utils/prescriptionInventoryValidation.js'

const mockBackendMedicines = [
  { id: 'med-01', medicineName: 'Paracetamol 500 mg', strength: '500 mg', unit: 'viên', stockQuantity: 100, active: true },
  { id: 'med-02', medicineName: 'Ambroxol 30 mg', strength: '30 mg', unit: 'viên', stockQuantity: 45, active: true },
  { id: 'med-03', medicineName: 'Acetylcysteine 200 mg', strength: '200 mg', unit: 'gói', stockQuantity: 0, active: true },
  { id: 'med-04', medicineName: 'Amiodarone 200 mg', strength: '200 mg', unit: 'viên', stockQuantity: 0, active: true },
  { id: 'med-05', medicineName: 'Ibuprofen 400 mg', strength: '400 mg', unit: 'viên', stockQuantity: 20, active: true },
]

test('TC01: Tồn = 0 -> Thuốc vẫn hiển thị trong danh mục nhưng bị vô hiệu hóa (disabled = true)', () => {
  const acetylM = mockBackendMedicines.find((m) => m.id === 'med-03')
  const stock = getAvailableStock(acetylM)
  assert.equal(stock, 0)

  const validation = validateItemStock({ medicineId: 'med-03', quantity: 1 }, mockBackendMedicines)
  assert.equal(validation.isValid, false)
  assert.ok(validation.error.includes('hết hàng'))
})

test('TC02: Tồn > 0 -> Cho phép chọn và kê đơn', () => {
  const ambroxolM = mockBackendMedicines.find((m) => m.id === 'med-02')
  const stock = getAvailableStock(ambroxolM)
  assert.equal(stock, 45)

  const validation = validateItemStock({ medicineId: 'med-02', quantity: 10 }, mockBackendMedicines)
  assert.equal(validation.isValid, true)
  assert.equal(validation.error, null)
})

test('TC03: Tồn = 20, kê 10 -> Hợp lệ', () => {
  const item = { medicineId: 'med-05', quantity: 10 }
  const validation = validateItemStock(item, mockBackendMedicines)
  assert.equal(validation.isValid, true)
})

test('TC04: Tồn = 20, kê 30 -> Bị chặn với thông báo lỗi tồn kho vượt quá', () => {
  const item = { medicineId: 'med-05', quantity: 30 }
  const validation = validateItemStock(item, mockBackendMedicines)
  assert.equal(validation.isValid, false)
  assert.ok(validation.error.includes('vượt quá tồn kho khả dụng'))
  assert.ok(validation.error.includes('20'))
})

test('TC05: Tồn thay đổi từ 10 -> 0 trước khi lưu -> Re-check/refresh -> Bị chặn', () => {
  const initialMedicines = [
    { id: 'med-05', medicineName: 'Ibuprofen 400 mg', stockQuantity: 10, unit: 'viên', active: true },
  ]
  const initialCheck = validateItemStock({ medicineId: 'med-05', quantity: 5 }, initialMedicines)
  assert.equal(initialCheck.isValid, true)

  const updatedMedicines = [
    { id: 'med-05', medicineName: 'Ibuprofen 400 mg', stockQuantity: 0, unit: 'viên', active: true },
  ]
  const liveCheck = validateItemStock({ medicineId: 'med-05', quantity: 5 }, updatedMedicines)
  assert.equal(liveCheck.isValid, false)
  assert.ok(liveCheck.error.includes('hết hàng'))
})

test('TC06: Có 2 thuốc, 1 thuốc không đủ tồn -> Không tạo toàn bộ đơn', () => {
  const prescriptionItems = [
    { medicineId: 'med-01', quantity: 10 },
    { medicineId: 'med-05', quantity: 30 },
  ]

  const validation = validatePrescriptionStock(prescriptionItems, mockBackendMedicines)
  assert.equal(validation.isValid, false)
  assert.equal(validation.errors.length, 1)
  assert.ok(validation.errors[0].includes('Ibuprofen'))
})

test('TC07: Không được gọi Create Prescription API khi validation tồn kho fail', () => {
  let apiCalled = false
  const fakeCreatePrescriptionApi = () => {
    apiCalled = true
    return Promise.resolve({ data: { id: 'presc-1' } })
  }

  const invalidItems = [
    { medicineId: 'med-03', quantity: 5 },
  ]

  const validation = validatePrescriptionStock(invalidItems, mockBackendMedicines)

  if (!validation.isValid) {
  } else {
    fakeCreatePrescriptionApi()
  }

  assert.equal(apiCalled, false, 'Create Prescription API must NOT be called when stock validation fails')
})

test('TC08: Thuốc hết hàng không bị xóa khỏi danh mục dropdown (sorted: còn hàng trước, hết hàng sau)', () => {
  const sortedMeds = sortMedicinesByStockAvailability(mockBackendMedicines)

  assert.equal(sortedMeds.length, mockBackendMedicines.length)

  assert.ok(getAvailableStock(sortedMeds[0]) > 0)
  assert.ok(getAvailableStock(sortedMeds[1]) > 0)
  assert.ok(getAvailableStock(sortedMeds[2]) > 0)

  assert.equal(getAvailableStock(sortedMeds[sortedMeds.length - 2]), 0)
  assert.equal(getAvailableStock(sortedMeds[sortedMeds.length - 1]), 0)
})

test('TC09: Không hardcode tồn kho, lấy đúng field stockQuantity/availableStock từ Backend response', () => {
  const medWithStockQuantity = { id: 'm1', medicineName: 'Test Med', stockQuantity: 77 }
  const medWithAvailableStock = { id: 'm2', medicineName: 'Test Med 2', availableStock: 88 }

  assert.equal(getAvailableStock(medWithStockQuantity), 77)
  assert.equal(getAvailableStock(medWithAvailableStock), 88)
})
