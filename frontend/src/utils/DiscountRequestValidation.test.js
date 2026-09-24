import assert from 'node:assert/strict'
import test from 'node:test'
import {
  DISCOUNT_TYPES,
  DISCOUNT_TYPE_OPTIONS,
  getStatusTag,
  validateDiscountForm,
  calculatePreview,
  canApproveOrReject,
  mapDiscountErrorMessage,
} from './discountRequestHelpers.js'
import discountRequestApi from '../api/discountRequestApi.js'

// ============================================================================
// 1. Kiểm thử validateDiscountForm cho 3 loại giảm giá
// ============================================================================
test('TC-DISC-01: validateDiscountForm - Bắt buộc nhập lý do đề xuất', () => {
  const res1 = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 10, '', 100000)
  assert.equal(res1.isValid, false)
  assert.match(res1.message, /Lý do đề nghị giảm giá không được để trống/)

  const res2 = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 10, '   ', 100000)
  assert.equal(res2.isValid, false)

  const res3 = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 10, null, 100000)
  assert.equal(res3.isValid, false)
})

test('TC-DISC-02: validateDiscountForm - Kiểm tra loại PERCENTAGE (Tỷ lệ %)', () => {
  // Hợp lệ: 10%, 50%, 100%
  const valid1 = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 10, 'Bệnh nhân nghèo', 200000)
  assert.equal(valid1.isValid, true)
  assert.equal(valid1.message, null)

  const valid2 = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 100, 'Hộ gia đình chính sách', 200000)
  assert.equal(valid2.isValid, true)

  // Không hợp lệ: <= 0, > 100, NaN, rỗng
  const errZero = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 0, 'Lý do', 200000)
  assert.equal(errZero.isValid, false)
  assert.match(errZero.message, /Tỷ lệ giảm giá phải lớn hơn 0%/)

  const errNegative = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, -5, 'Lý do', 200000)
  assert.equal(errNegative.isValid, false)

  const errOver100 = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 101, 'Lý do', 200000)
  assert.equal(errOver100.isValid, false)
  assert.match(errOver100.message, /không vượt quá 100%/)

  const errEmpty = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, '', 'Lý do', 200000)
  assert.equal(errEmpty.isValid, false)

  const errNaN = validateDiscountForm(DISCOUNT_TYPES.PERCENTAGE, 'abc', 'Lý do', 200000)
  assert.equal(errNaN.isValid, false)
})

test('TC-DISC-03: validateDiscountForm - Kiểm tra loại FIXED_AMOUNT (Số tiền cố định)', () => {
  const originalAmount = 500000

  // Hợp lệ: > 0 và <= originalAmount
  const valid1 = validateDiscountForm(DISCOUNT_TYPES.FIXED_AMOUNT, 100000, 'Khách hàng thân thiết', originalAmount)
  assert.equal(valid1.isValid, true)

  const validEqual = validateDiscountForm(DISCOUNT_TYPES.FIXED_AMOUNT, 500000, 'Miễn trừ số tiền bằng viện phí', originalAmount)
  assert.equal(validEqual.isValid, true)

  // Không hợp lệ: <= 0, > originalAmount, rỗng, NaN
  const errZero = validateDiscountForm(DISCOUNT_TYPES.FIXED_AMOUNT, 0, 'Lý do', originalAmount)
  assert.equal(errZero.isValid, false)
  assert.match(errZero.message, /Số tiền giảm giá phải lớn hơn 0/)

  const errNegative = validateDiscountForm(DISCOUNT_TYPES.FIXED_AMOUNT, -10000, 'Lý do', originalAmount)
  assert.equal(errNegative.isValid, false)

  const errExceed = validateDiscountForm(DISCOUNT_TYPES.FIXED_AMOUNT, 500001, 'Lý do', originalAmount)
  assert.equal(errExceed.isValid, false)
  assert.match(errExceed.message, /không được vượt quá tổng viện phí/)

  const errEmpty = validateDiscountForm(DISCOUNT_TYPES.FIXED_AMOUNT, '', 'Lý do', originalAmount)
  assert.equal(errEmpty.isValid, false)
})

test('TC-DISC-04: validateDiscountForm - Kiểm tra loại FULL_FREE (Miễn phí 100%)', () => {
  // Hợp lệ mà không cần nhập discountValue
  const valid1 = validateDiscountForm(DISCOUNT_TYPES.FULL_FREE, 0, 'Bệnh nhân bảo trợ 100%', 300000)
  assert.equal(valid1.isValid, true)
  assert.equal(valid1.message, null)

  const valid2 = validateDiscountForm(DISCOUNT_TYPES.FULL_FREE, undefined, 'Ban giám đốc chỉ đạo', 300000)
  assert.equal(valid2.isValid, true)
})

test('TC-DISC-05: validateDiscountForm - Kiểm tra loại không hợp lệ', () => {
  const res = validateDiscountForm('UNKNOWN_TYPE', 10, 'Lý do', 100000)
  assert.equal(res.isValid, false)
  assert.match(res.message, /Loại giảm giá không hợp lệ/)
})

// ============================================================================
// 2. Kiểm thử calculatePreview tính toán tạm thời
// ============================================================================
test('TC-DISC-06: calculatePreview - PERCENTAGE tính đúng số tiền giảm và còn lại', () => {
  const orig = 1000000
  const preview20 = calculatePreview(DISCOUNT_TYPES.PERCENTAGE, 20, orig)
  assert.equal(preview20.originalAmount, 1000000)
  assert.equal(preview20.discountAmount, 200000)
  assert.equal(preview20.finalAmount, 800000)

  const preview100 = calculatePreview(DISCOUNT_TYPES.PERCENTAGE, 100, orig)
  assert.equal(preview100.discountAmount, 1000000)
  assert.equal(preview100.finalAmount, 0)

  const previewZero = calculatePreview(DISCOUNT_TYPES.PERCENTAGE, 0, orig)
  assert.equal(previewZero.discountAmount, 0)
  assert.equal(previewZero.finalAmount, 1000000)
})

test('TC-DISC-07: calculatePreview - FIXED_AMOUNT tính đúng số tiền giảm và còn lại', () => {
  const orig = 500000
  const preview150 = calculatePreview(DISCOUNT_TYPES.FIXED_AMOUNT, 150000, orig)
  assert.equal(preview150.originalAmount, 500000)
  assert.equal(preview150.discountAmount, 150000)
  assert.equal(preview150.finalAmount, 350000)

  // Vượt quá originalAmount -> kẹp tối đa bằng originalAmount
  const previewOver = calculatePreview(DISCOUNT_TYPES.FIXED_AMOUNT, 600000, orig)
  assert.equal(previewOver.discountAmount, 500000)
  assert.equal(previewOver.finalAmount, 0)
})

test('TC-DISC-08: calculatePreview - FULL_FREE luôn giảm 100% về 0', () => {
  const orig = 750000
  const preview = calculatePreview(DISCOUNT_TYPES.FULL_FREE, 0, orig)
  assert.equal(preview.originalAmount, 750000)
  assert.equal(preview.discountAmount, 750000)
  assert.equal(preview.finalAmount, 0)
})

test('TC-DISC-09: calculatePreview - Xử lý an toàn khi originalAmount = 0 hoặc rỗng', () => {
  const preview = calculatePreview(DISCOUNT_TYPES.PERCENTAGE, 20, 0)
  assert.equal(preview.originalAmount, 0)
  assert.equal(preview.discountAmount, 0)
  assert.equal(preview.finalAmount, 0)
})

// ============================================================================
// 3. Kiểm thử canApproveOrReject (Chặn tự phê duyệt - SoD - QTN-37)
// ============================================================================
test('TC-DISC-10: canApproveOrReject - Trả về false khi người duyệt trùng người đề xuất', () => {
  const request = {
    id: 'req-001',
    requestedBy: 'user-uuid-1111',
  }
  const currentUserId = 'user-uuid-1111'

  assert.equal(canApproveOrReject(request, currentUserId), false, 'Không được phép tự duyệt đề xuất của chính mình')
})

test('TC-DISC-11: canApproveOrReject - Trả về true khi người duyệt khác người đề xuất', () => {
  const request = {
    id: 'req-001',
    requestedBy: 'user-uuid-1111',
  }
  const managerId = 'user-uuid-2222'

  assert.equal(canApproveOrReject(request, managerId), true, 'Quản lý khác người đề xuất được phép duyệt')
})

test('TC-DISC-12: canApproveOrReject - So sánh không phân biệt hoa thường UUID', () => {
  const request = {
    id: 'req-001',
    requestedBy: 'AABBCCDD-1111-2222-3333-444455556666',
  }
  const currentUserId = 'aabbccdd-1111-2222-3333-444455556666'

  assert.equal(canApproveOrReject(request, currentUserId), false)
})

// ============================================================================
// 4. Kiểm thử mapDiscountErrorMessage cho các mã lỗi Backend
// ============================================================================
test('TC-DISC-13: mapDiscountErrorMessage - Ánh xạ đúng các mã lỗi theo contract', () => {
  // 403 SELF_APPROVAL_NOT_ALLOWED
  const errSelf = { response: { status: 403, data: { code: 'SELF_APPROVAL_NOT_ALLOWED' } } }
  assert.match(mapDiscountErrorMessage(errSelf), /không thể tự duyệt\/từ chối đề xuất do chính mình tạo/)

  // 409 PENDING_DISCOUNT_APPROVAL
  const errPending = { response: { status: 409, data: { code: 'PENDING_DISCOUNT_APPROVAL' } } }
  assert.match(mapDiscountErrorMessage(errPending), /Lượt khám này đang có đề xuất giảm giá chờ duyệt/)

  // 409 DISCOUNT_ALREADY_EXISTS
  const errExists = { response: { status: 409, data: { code: 'DISCOUNT_ALREADY_EXISTS' } } }
  assert.match(mapDiscountErrorMessage(errExists), /đã có đề xuất giảm giá đang chờ duyệt hoặc đã được phê duyệt/)

  // 400 DISCOUNT_EXCEEDS_TOTAL
  const errExceed = { response: { status: 400, data: { code: 'DISCOUNT_EXCEEDS_TOTAL' } } }
  assert.match(mapDiscountErrorMessage(errExceed), /không được vượt quá tổng viện phí chưa giảm/)

  // 409 INVALID_DISCOUNT_STATE
  const errState = { response: { status: 409, data: { code: 'INVALID_DISCOUNT_STATE' } } }
  assert.match(mapDiscountErrorMessage(errState), /Chỉ có thể xử lý đề xuất đang ở trạng thái chờ duyệt/)

  // 404 DISCOUNT_REQUEST_NOT_FOUND
  const errNotFound = { response: { status: 404, data: { code: 'DISCOUNT_REQUEST_NOT_FOUND' } } }
  assert.match(mapDiscountErrorMessage(errNotFound), /Không tìm thấy thông tin đề xuất giảm giá/)

  // 403 ACCESS_DENIED
  const errAccess = { response: { status: 403, data: { code: 'ACCESS_DENIED' } } }
  assert.match(mapDiscountErrorMessage(errAccess), /không có quyền thực hiện thao tác này/)
})

// ============================================================================
// 5. Kiểm thử getStatusTag
// ============================================================================
test('TC-DISC-14: getStatusTag - Trả về nhãn và màu sắc tương ứng', () => {
  assert.equal(getStatusTag('PENDING').label, 'Chờ duyệt')
  assert.equal(getStatusTag('PENDING').color, 'orange')

  assert.equal(getStatusTag('APPROVED').label, 'Đã duyệt')
  assert.equal(getStatusTag('APPROVED').color, 'green')

  assert.equal(getStatusTag('REJECTED').label, 'Đã từ chối')
  assert.equal(getStatusTag('REJECTED').color, 'red')
})

// ============================================================================
// 6. Kiểm thử discountRequestApi đầy đủ hàm
// ============================================================================
test('TC-DISC-15: discountRequestApi - Cung cấp đầy đủ các phương thức API theo contract', () => {
  assert.equal(typeof discountRequestApi.create, 'function')
  assert.equal(typeof discountRequestApi.list, 'function')
  assert.equal(typeof discountRequestApi.getById, 'function')
  assert.equal(typeof discountRequestApi.approve, 'function')
  assert.equal(typeof discountRequestApi.reject, 'function')
})
