import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * 1. Build & Validate Stock Receipt Request Payload matching Spring Boot DTO (CreateInventoryReceiptRequest / ReceiptItemRequest)
 */
export const buildStockReceiptPayload = ({ medicineId, lotNumber, expiryDateStr, quantity, unitCost, note }) => {
  if (!medicineId) throw new Error('Medicine id is required.')
  if (!lotNumber || !lotNumber.trim()) throw new Error('Batch number is required.')
  if (!expiryDateStr) throw new Error('Expiry date is required.')

  const expDate = new Date(expiryDateStr)
  const now = new Date()
  if (isNaN(expDate.getTime()) || expDate <= now) {
    throw new Error('Expiry date must be in the future.')
  }

  const qty = Number(quantity)
  if (isNaN(qty) || qty <= 0) throw new Error('Quantity must be greater than 0.')

  const price = Number(unitCost || 0)
  if (isNaN(price) || price < 0) throw new Error('Import price must be non-negative.')

  return {
    note: note || `Nhập lô thuốc ${lotNumber.trim()}`,
    items: [
      {
        medicineId,
        batchNumber: lotNumber.trim(),
        expiryDate: expiryDateStr,
        quantity: qty,
        importPrice: price,
      },
    ],
  }
}

/**
 * 2. Role-Based Authorization Helper for Stock Receipt & Catalog Management
 */
export const checkPharmacyPermissions = (roles = []) => {
  const normalized = roles.map((r) => String(r).toLowerCase().replace(/^role_/, ''))
  const isPharmacist = normalized.includes('pharmacist')
  const isAdmin = normalized.includes('admin')
  const isManager = normalized.includes('manager')

  return {
    canWriteCatalogAndStock: isPharmacist,
    canViewAndMonitor: isPharmacist || isAdmin || isManager,
  }
}

/**
 * 3. Batch & Inventory Stock Calculation Helper
 */
export const calculateUpdatedStock = (currentStock, receivedQuantity) => {
  const cur = Number(currentStock || 0)
  const qty = Number(receivedQuantity || 0)
  return cur + qty
}

// =========================================================================
// --- AUTOMATED TEST SUITE: PHARMACY STOCK RECEIPT & BACKEND SYNC ---
// =========================================================================

test('1. KIỂM THỬ CẤU TRÚC PAYLOAD VÀ VALIDATION DTO KHI NHẬP KHO (Backend DTO Sync)', () => {
  const sampleRequest = {
    medicineId: '16000000-0000-0000-0000-000000000001',
    lotNumber: 'LOT-2028-099',
    expiryDateStr: '2028-12-31',
    quantity: 500,
    unitCost: 1500,
    note: 'Nhập bổ sung kho quý 3',
  }

  const payload = buildStockReceiptPayload(sampleRequest)

  // Kiểm tra thông tin Endpoint & DTO Backend POST /inventory/receipts
  assert.equal(payload.note, 'Nhập bổ sung kho quý 3')
  assert.equal(payload.items.length, 1)
  assert.equal(payload.items[0].medicineId, '16000000-0000-0000-0000-000000000001')
  assert.equal(payload.items[0].batchNumber, 'LOT-2028-099')
  assert.equal(payload.items[0].expiryDate, '2028-12-31')
  assert.equal(payload.items[0].quantity, 500)
  assert.equal(payload.items[0].importPrice, 1500)
})

test('2. KIỂM THỬ BẮT LỖI RÀNG BUỘC THAM SỐ NHẬP KHO (Validation Constraints Test)', () => {
  // TH 2.1: Số lượng <= 0 -> Phải báo lỗi Validation
  assert.throws(() => {
    buildStockReceiptPayload({
      medicineId: '16000000-0000-0000-0000-000000000001',
      lotNumber: 'LOT-001',
      expiryDateStr: '2028-12-31',
      quantity: 0,
    })
  }, /Quantity must be greater than 0/)

  // TH 2.2: Hạn sử dụng trong quá khứ -> Phải báo lỗi Validation
  assert.throws(() => {
    buildStockReceiptPayload({
      medicineId: '16000000-0000-0000-0000-000000000001',
      lotNumber: 'LOT-001',
      expiryDateStr: '2020-01-01',
      quantity: 100,
    })
  }, /Expiry date must be in the future/)
})

test('3. KIỂM THỬ PHÂN QUYỀN VAI TRÒ TÀI KHOẢN (Role-Based Authorization Test)', () => {
  // Dược sĩ (PHARMACIST): Toàn quyền ghi dữ liệu & Nhập kho
  const pharmacistPerms = checkPharmacyPermissions(['ROLE_PHARMACIST'])
  assert.equal(pharmacistPerms.canWriteCatalogAndStock, true)
  assert.equal(pharmacistPerms.canViewAndMonitor, true)

  // Quản trị viên (ADMIN): Quyền xem/giám sát, không có quyền ghi trực tiếp kho
  const adminPerms = checkPharmacyPermissions(['admin'])
  assert.equal(adminPerms.canWriteCatalogAndStock, false)
  assert.equal(adminPerms.canViewAndMonitor, true)

  // Khách (GUEST): Bị từ chối hoàn toàn
  const guestPerms = checkPharmacyPermissions([])
  assert.equal(guestPerms.canWriteCatalogAndStock, false)
  assert.equal(guestPerms.canViewAndMonitor, false)
})

test('4. KIỂM THỬ CỘNG DỒN TỒN KHO KHI NHẬP LÔ THUỐC MỚI (Stock Quantity Increment Test)', () => {
  const initialStock = 1500
  const importQuantity = 500
  const updatedStock = calculateUpdatedStock(initialStock, importQuantity)

  assert.equal(updatedStock, 2000)
})
