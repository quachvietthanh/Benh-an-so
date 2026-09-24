import assert from 'node:assert/strict'
import test from 'node:test'
import {
  PAYMENT_METHODS,
  PAYMENT_METHOD_OPTIONS,
  getPaymentMethodMeta,
  formatCurrency,
  validatePaymentMethods,
  calculateEqualSplit,
  mapPaymentErrorMessage,
} from './paymentMethodHelpers.js'
import invoiceApi from '../api/invoiceApi.js'
import cashierShiftApi from '../api/cashierShiftApi.js'

// ============================================================================
// 1. Kiểm thử định dạng tiền tệ VNĐ (formatCurrency)
// ============================================================================
test('TC-PM-01: formatCurrency định dạng tiền VNĐ chuẩn và xử lý an toàn các giá trị biên', () => {
  // Chuẩn hóa khoảng trắng unicode (tránh lệch non-breaking space của Intl)
  const normalize = (str) => String(str).replace(/\s/g, ' ')

  assert.equal(normalize(formatCurrency(250000)), normalize('250.000 ₫'))
  assert.equal(normalize(formatCurrency(1500000)), normalize('1.500.000 ₫'))
  assert.equal(normalize(formatCurrency(0)), normalize('0 ₫'))
  assert.equal(normalize(formatCurrency(null)), normalize('0 ₫'))
  assert.equal(normalize(formatCurrency(undefined)), normalize('0 ₫'))
  assert.equal(normalize(formatCurrency('')), normalize('0 ₫'))
  assert.equal(normalize(formatCurrency('invalid')), normalize('0 ₫'))
  assert.equal(normalize(formatCurrency(-50000)), normalize('-50.000 ₫'))
})

// ============================================================================
// 2. Kiểm thử validatePaymentMethods: Khớp chính xác 1 phương thức đơn
// ============================================================================
test('TC-PM-02: validatePaymentMethods - Hợp lệ khi thanh toán 1 phương thức tiền mặt đủ số tiền', () => {
  const methods = [
    { paymentMethod: 'CASH', amount: 250000, referenceNumber: null },
  ]
  const result = validatePaymentMethods(methods, 250000)

  assert.equal(result.isValid, true, 'Thanh toán đủ tiền mặt phải hợp lệ')
  assert.equal(result.difference, 0, 'Chênh lệch phải bằng 0')
  assert.equal(result.totalEntered, 250000)
  assert.equal(result.isExact, true)
  assert.equal(result.errors.length, 0)
})

// ============================================================================
// 3. Kiểm thử validatePaymentMethods: Đa phương thức (CASH + BANK_TRANSFER) khớp số tiền
// ============================================================================
test('TC-PM-03: validatePaymentMethods - Hợp lệ khi chia nhiều phương thức và có mã giao dịch', () => {
  const methods = [
    { paymentMethod: 'CASH', amount: 100000, referenceNumber: null },
    { paymentMethod: 'BANK_TRANSFER', amount: 150000, referenceNumber: 'TXN-20260923-001' },
  ]
  const result = validatePaymentMethods(methods, 250000)

  assert.equal(result.isValid, true, 'Chia 2 phương thức khớp tiền phải hợp lệ')
  assert.equal(result.difference, 0)
  assert.equal(result.totalEntered, 250000)
  assert.equal(result.errors.length, 0)
})

// ============================================================================
// 4. Kiểm thử validatePaymentMethods: Lệch tiền - Còn thiếu (difference > 0)
// ============================================================================
test('TC-PM-04: validatePaymentMethods - Không hợp lệ khi tổng tiền các phương thức còn thiếu', () => {
  const methods = [
    { paymentMethod: 'CASH', amount: 100000 },
    { paymentMethod: 'BANK_TRANSFER', amount: 100000, referenceNumber: 'TXN-123' },
  ]
  // Tổng cần thu là 250.000, mới nhập 200.000 -> Thiếu 50.000
  const result = validatePaymentMethods(methods, 250000)

  assert.equal(result.isValid, false, 'Còn thiếu tiền không được submit')
  assert.equal(result.difference, 50000, 'Chênh lệch phải là +50.000 (dương = thiếu)')
  assert.equal(result.isUnder, true)
  assert.equal(result.isExact, false)
  assert.match(result.errors[0], /còn thiếu/i)
})

// ============================================================================
// 5. Kiểm thử validatePaymentMethods: Lệch tiền - Vượt quá (difference < 0)
// ============================================================================
test('TC-PM-05: validatePaymentMethods - Không hợp lệ khi tổng tiền các phương thức vượt quá', () => {
  const methods = [
    { paymentMethod: 'CASH', amount: 200000 },
    { paymentMethod: 'CARD', amount: 100000 },
  ]
  // Tổng cần thu là 250.000, nhập 300.000 -> Thừa 50.000
  const result = validatePaymentMethods(methods, 250000)

  assert.equal(result.isValid, false, 'Thừa tiền không được submit')
  assert.equal(result.difference, -50000, 'Chênh lệch phải là -50.000 (âm = thừa)')
  assert.equal(result.isOver, true)
  assert.equal(result.isExact, false)
  assert.match(result.errors[0], /vượt quá/i)
})

// ============================================================================
// 6. Kiểm thử validatePaymentMethods: Bắt buộc số tham chiếu cho BANK_TRANSFER
// ============================================================================
test('TC-PM-06: validatePaymentMethods - Bắt buộc số tham chiếu/mã GD khi chọn BANK_TRANSFER', () => {
  // Trường hợp không có referenceNumber
  const methodsWithoutRef = [
    { paymentMethod: 'BANK_TRANSFER', amount: 250000, referenceNumber: '' },
  ]
  const res1 = validatePaymentMethods(methodsWithoutRef, 250000)
  assert.equal(res1.isValid, false, 'BANK_TRANSFER thiếu referenceNumber phải bị chặn')
  assert.ok(res1.errors.some((e) => e.includes('mã tham chiếu') || e.includes('Chuyển khoản')))

  // Trường hợp referenceNumber chỉ toàn khoảng trắng
  const methodsWithWhitespaceRef = [
    { paymentMethod: 'BANK_TRANSFER', amount: 250000, referenceNumber: '   ' },
  ]
  const res2 = validatePaymentMethods(methodsWithWhitespaceRef, 250000)
  assert.equal(res2.isValid, false, 'Khoảng trắng không được tính là mã tham chiếu hợp lệ')

  // Trường hợp phương thức khác (CASH) thì KHÔNG bắt buộc referenceNumber
  const methodsCashNoRef = [
    { paymentMethod: 'CASH', amount: 250000, referenceNumber: null },
  ]
  const res3 = validatePaymentMethods(methodsCashNoRef, 250000)
  assert.equal(res3.isValid, true, 'Tiền mặt không yêu cầu mã tham chiếu')
})

// ============================================================================
// 7. Kiểm thử validatePaymentMethods: Chặn số tham chiếu vượt quá 100 ký tự
// ============================================================================
test('TC-PM-07: validatePaymentMethods - Chặn số tham chiếu dài quá 100 ký tự', () => {
  const longRef = 'A'.repeat(101)
  const methods = [
    { paymentMethod: 'BANK_TRANSFER', amount: 250000, referenceNumber: longRef },
  ]
  const result = validatePaymentMethods(methods, 250000)

  assert.equal(result.isValid, false, 'referenceNumber > 100 ký tự phải bị chặn')
  assert.ok(result.errors.some((e) => e.includes('100 ký tự')))

  // 100 ký tự chính xác thì phải hợp lệ
  const exact100Ref = 'B'.repeat(100)
  const methodsExact100 = [
    { paymentMethod: 'BANK_TRANSFER', amount: 250000, referenceNumber: exact100Ref },
  ]
  const resExact = validatePaymentMethods(methodsExact100, 250000)
  assert.equal(resExact.isValid, true, 'referenceNumber 100 ký tự đúng chuẩn phải hợp lệ')
})

// ============================================================================
// 8. Kiểm thử validatePaymentMethods: Bắt buộc amount phải > 0
// ============================================================================
test('TC-PM-08: validatePaymentMethods - Bắt buộc số tiền mỗi phương thức phải > 0', () => {
  // Amount = 0
  const zeroMethods = [
    { paymentMethod: 'CASH', amount: 0 },
    { paymentMethod: 'BANK_TRANSFER', amount: 250000, referenceNumber: 'TXN123' },
  ]
  const resZero = validatePaymentMethods(zeroMethods, 250000)
  assert.equal(resZero.isValid, false)
  assert.ok(resZero.errors.some((e) => e.includes('lớn hơn 0')))

  // Amount âm
  const negMethods = [
    { paymentMethod: 'CASH', amount: -50000 },
    { paymentMethod: 'BANK_TRANSFER', amount: 300000, referenceNumber: 'TXN123' },
  ]
  const resNeg = validatePaymentMethods(negMethods, 250000)
  assert.equal(resNeg.isValid, false)
  assert.ok(resNeg.errors.some((e) => e.includes('lớn hơn 0')))

  // Amount rỗng / NaN
  const emptyMethods = [
    { paymentMethod: 'CASH', amount: '' },
    { paymentMethod: 'BANK_TRANSFER', amount: 250000, referenceNumber: 'TXN123' },
  ]
  const resEmpty = validatePaymentMethods(emptyMethods, 250000)
  assert.equal(resEmpty.isValid, false)
})

// ============================================================================
// 9. Kiểm thử calculateEqualSplit: Chia đều không sai lệch số lẻ
// ============================================================================
test('TC-PM-09: calculateEqualSplit - Chia đều số tiền không bị lệch số lẻ', () => {
  // Chia chẵn
  const split2 = calculateEqualSplit(250000, 2)
  assert.equal(split2.length, 2)
  assert.equal(split2[0] + split2[1], 250000)
  assert.equal(split2[0], 125000)
  assert.equal(split2[1], 125000)

  // Chia lẻ: 100.000 cho 3 dòng
  const split3 = calculateEqualSplit(100000, 3)
  assert.equal(split3.length, 3)
  const sum3 = split3.reduce((a, b) => a + b, 0)
  assert.equal(sum3, 100000, 'Tổng các phần chia đều phải khớp chính xác 100.000 đ')

  // Trường hợp 0 hoặc âm
  assert.deepEqual(calculateEqualSplit(0, 2), [0, 0])
  assert.deepEqual(calculateEqualSplit(100000, 0), [])
})

// ============================================================================
// 10. Kiểm thử mapPaymentErrorMessage: Ánh xạ lỗi Backend chuẩn
// ============================================================================
test('TC-PM-10: mapPaymentErrorMessage - Xử lý đầy đủ các mã lỗi từ Backend', () => {
  // 1. 400 PAYMENT_AMOUNT_MISMATCH
  const errMismatch = {
    response: {
      status: 400,
      data: {
        code: 'PAYMENT_AMOUNT_MISMATCH',
        message: 'Payment amount must equal the amount due. Expected: 250000, actual: 200000, missing: 50000',
      },
    },
  }
  const msgMismatch = mapPaymentErrorMessage(errMismatch)
  assert.ok(msgMismatch.includes('không khớp'), 'Phải báo rõ không khớp số tiền')
  assert.ok(msgMismatch.includes('Expected: 250000'), 'Phải hiển thị chi tiết số tiền từ Backend')

  // 2. 409 PAYMENT_ALREADY_EXISTS
  const errAlreadyPaid = {
    response: {
      status: 409,
      data: {
        code: 'PAYMENT_ALREADY_EXISTS',
        message: 'Payment already exists for visit.',
      },
    },
  }
  const msgAlreadyPaid = mapPaymentErrorMessage(errAlreadyPaid)
  assert.ok(msgAlreadyPaid.includes('từ trước'))

  // 3. 409 PAYMENT_NOT_ALLOWED
  const errNotAllowed = {
    response: {
      status: 409,
      data: {
        code: 'PAYMENT_NOT_ALLOWED',
        message: 'Payment cannot be recorded for cancelled visits.',
      },
    },
  }
  const msgNotAllowed = mapPaymentErrorMessage(errNotAllowed)
  assert.ok(msgNotAllowed.includes('hủy') || msgNotAllowed.includes('cấp phát'))

  // 4. 409 PENDING_DISCOUNT_APPROVAL
  const errPendingDiscount = {
    response: {
      status: 409,
      data: {
        code: 'PENDING_DISCOUNT_APPROVAL',
        message: 'Pending discount approval.',
      },
    },
  }
  const msgPendingDiscount = mapPaymentErrorMessage(errPendingDiscount)
  assert.ok(msgPendingDiscount.includes('miễn giảm'))

  // 5. 403 Forbidden
  const errForbidden = {
    response: {
      status: 403,
    },
  }
  const msgForbidden = mapPaymentErrorMessage(errForbidden)
  assert.ok(msgForbidden.includes('INVOICE_CREATE'))
})

// ============================================================================
// 11. Kiểm thử Contract API methods
// ============================================================================
test('TC-PM-11: invoiceApi và cashierShiftApi có đủ method theo contract yêu cầu', () => {
  assert.equal(typeof invoiceApi.getQuote, 'function', 'invoiceApi.getQuote phải tồn tại')
  assert.equal(typeof invoiceApi.recordPayment, 'function', 'invoiceApi.recordPayment phải tồn tại')
  assert.equal(typeof invoiceApi.getById, 'function', 'invoiceApi.getById phải tồn tại')
  assert.equal(typeof invoiceApi.createInvoice, 'function', 'invoiceApi.createInvoice phải tồn tại')

  assert.equal(typeof cashierShiftApi.getCurrentShift, 'function', 'cashierShiftApi.getCurrentShift phải tồn tại')
  assert.equal(typeof cashierShiftApi.getCurrentSummary, 'function', 'cashierShiftApi.getCurrentSummary phải tồn tại')
  assert.equal(typeof cashierShiftApi.closeShift, 'function', 'cashierShiftApi.closeShift phải tồn tại')
})

// ============================================================================
// 12. Kiểm thử getPaymentMethodMeta
// ============================================================================
test('TC-PM-12: getPaymentMethodMeta hiển thị đúng cho các phương thức và MULTIPLE', () => {
  assert.equal(getPaymentMethodMeta('CASH').label, 'Tiền mặt')
  assert.equal(getPaymentMethodMeta('BANK_TRANSFER').label, 'Chuyển khoản')
  assert.equal(getPaymentMethodMeta('MULTIPLE').label, 'Nhiều phương thức')
  assert.equal(getPaymentMethodMeta('UNKNOWN').label, 'UNKNOWN')
})
