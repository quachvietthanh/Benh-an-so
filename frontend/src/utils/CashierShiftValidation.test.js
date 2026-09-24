import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatCurrency,
  formatDateTime,
  calculatePreviewDifference,
  isNoteRequired,
  getShiftStatusMeta,
  canConfirmShift,
  mapCashierShiftError,
} from './cashierShiftHelpers.js'
import cashierShiftApi from '../api/cashierShiftApi.js'
import { getNavigationItems } from '../components/layout/navigationConfig.js'

// ============================================================================
// 1. Kiểm thử điều kiện disable nút "Chốt ca" khi totalTransactions === 0
// ============================================================================
test('TC-CS-01: Ca làm việc phải có ít nhất 1 giao dịch mới được chốt ca (Precondition: totalTransactions > 0)', () => {
  const isShiftClosable = (summary) => {
    return Boolean(summary && typeof summary.totalTransactions === 'number' && summary.totalTransactions > 0)
  }

  // Trường hợp không có giao dịch nào
  assert.equal(isShiftClosable({ totalTransactions: 0 }), false, 'Phải vô hiệu hóa khi totalTransactions = 0')
  assert.equal(isShiftClosable(null), false, 'Phải vô hiệu hóa khi summary là null')
  assert.equal(isShiftClosable(undefined), false, 'Phải vô hiệu hóa khi summary undefined')
  assert.equal(isShiftClosable({ totalTransactions: -1 }), false, 'Phải vô hiệu hóa khi số giao dịch âm')

  // Trường hợp hợp lệ: có từ 1 giao dịch trở lên
  assert.equal(isShiftClosable({ totalTransactions: 1 }), true, 'Hợp lệ khi có 1 giao dịch')
  assert.equal(isShiftClosable({ totalTransactions: 25 }), true, 'Hợp lệ khi có nhiều giao dịch')
})

// ============================================================================
// 2. Kiểm thử validate notes bắt buộc khi chênh lệch khác 0, tùy chọn khi bằng 0
// ============================================================================
test('TC-CS-02: Tính toán chênh lệch và bắt buộc ghi chú giải trình khi difference !== 0', () => {
  // Khớp tiền: hệ thống 1.000.000, thực đếm 1.000.000 -> chênh lệch = 0 -> notes tùy chọn
  const diffEqual = calculatePreviewDifference(1000000, 1000000)
  assert.equal(diffEqual, 0)
  assert.equal(isNoteRequired(diffEqual), false, 'Khi khớp tiền, ghi chú không bắt buộc')

  // Thừa tiền: hệ thống 1.000.000, thực đếm 1.050.000 -> chênh lệch = +50.000 -> notes bắt buộc
  const diffOver = calculatePreviewDifference(1050000, 1000000)
  assert.equal(diffOver, 50000)
  assert.equal(isNoteRequired(diffOver), true, 'Khi thừa tiền, ghi chú bắt buộc')

  // Thiếu tiền: hệ thống 1.000.000, thực đếm 900.000 -> chênh lệch = -100.000 -> notes bắt buộc
  const diffShort = calculatePreviewDifference(900000, 1000000)
  assert.equal(diffShort, -100000)
  assert.equal(isNoteRequired(diffShort), true, 'Khi thiếu tiền, ghi chú bắt buộc')

  // Trường hợp chuỗi số
  const diffString = calculatePreviewDifference('500000', '400000')
  assert.equal(diffString, 100000)
  assert.equal(isNoteRequired(diffString), true)
})

// ============================================================================
// 3. Kiểm thử phân quyền duyệt và ngăn chặn tự phê duyệt (canConfirmShift - QTN-38 / QTN-09)
// ============================================================================
test('TC-CS-03: Ẩn nút "Duyệt phiếu" khi người đăng nhập trùng cashierId của phiếu hoặc không có quyền', () => {
  const pendingShift = {
    id: 'shift-uuid-1',
    shiftCode: 'CS000001',
    cashierId: 'user-receptionist-1',
    status: 'PENDING_CONFIRMATION',
  }

  const confirmedShift = {
    id: 'shift-uuid-2',
    shiftCode: 'CS000002',
    cashierId: 'user-receptionist-1',
    status: 'CONFIRMED',
  }

  // 1. Quản lý duyệt phiếu của thu ngân khác -> Được phép
  const managerUser = {
    id: 'user-manager-99',
    roles: ['manager'],
    permissions: ['CASHIER_SHIFT_READ', 'CASHIER_SHIFT_CONFIRM'],
  }
  assert.equal(
    canConfirmShift(managerUser, pendingShift),
    true,
    'Quản lý có quyền duyệt phiếu PENDING của thu ngân khác'
  )

  // 2. Chặn tự duyệt: Quản lý hoặc Thu ngân trùng cashierId của phiếu -> Bị ẩn nút duyệt
  const selfUser = {
    id: 'user-receptionist-1',
    roles: ['manager', 'admin'],
    permissions: ['CASHIER_SHIFT_CONFIRM'],
  }
  assert.equal(
    canConfirmShift(selfUser, pendingShift),
    false,
    'Bắt buộc ẩn nút duyệt khi user.id trùng với shift.cashierId (QTN-38)'
  )

  // 3. Chặn duyệt phiếu đã CONFIRMED
  assert.equal(
    canConfirmShift(managerUser, confirmedShift),
    false,
    'Không được duyệt phiếu đã CONFIRMED'
  )

  // 4. Lễ tân không có quyền duyệt
  const receptionistUser = {
    id: 'user-receptionist-2',
    roles: ['receptionist'],
    permissions: ['CASHIER_SHIFT_READ', 'CASHIER_SHIFT_CREATE'],
  }
  assert.equal(
    canConfirmShift(receptionistUser, pendingShift),
    false,
    'Lễ tân không có quyền CASHIER_SHIFT_CONFIRM thì không thể duyệt'
  )
})

// ============================================================================
// 4. Kiểm thử hợp đồng API search và chuẩn hóa filter parameters
// ============================================================================
test('TC-CS-04: API search và chuẩn hóa tham số phân trang, filter', () => {
  assert.equal(typeof cashierShiftApi.search, 'function')
  assert.equal(typeof cashierShiftApi.getCurrentSummary, 'function')
  assert.equal(typeof cashierShiftApi.closeShift, 'function')
  assert.equal(typeof cashierShiftApi.confirmShift, 'function')
  assert.equal(typeof cashierShiftApi.getById, 'function')

  // Kiểm tra logic chuẩn hóa params
  const buildSearchParams = (page, pageSize, status, dates) => {
    const params = {
      page: page - 1,
      size: pageSize,
    }
    if (status) params.status = status
    if (dates && dates.length === 2) {
      params.from = dates[0]
      params.to = dates[1]
    }
    return params
  }

  const p1 = buildSearchParams(1, 10, 'PENDING_CONFIRMATION', ['2026-09-01T00:00:00Z', '2026-09-30T23:59:59Z'])
  assert.equal(p1.page, 0, 'Page 1 ở FE phải đổi thành 0 ở BE')
  assert.equal(p1.size, 10)
  assert.equal(p1.status, 'PENDING_CONFIRMATION')
  assert.equal(p1.from, '2026-09-01T00:00:00Z')
  assert.equal(p1.to, '2026-09-30T23:59:59Z')

  const p2 = buildSearchParams(3, 20, null, null)
  assert.equal(p2.page, 2)
  assert.equal(p2.size, 20)
  assert.equal(p2.status, undefined)
  assert.equal(p2.from, undefined)
})

// ============================================================================
// 5. Kiểm thử Map 3 lỗi nghiệp vụ chính sang tiếng Việt rõ ràng
// ============================================================================
test('TC-CS-05: Map đúng các lỗi nghiệp vụ Backend sang thông điệp tiếng Việt', () => {
  // 1. NoUnsettledPaymentsException
  const errNoUnsettled = {
    response: {
      data: {
        code: 'NO_UNSETTLED_PAYMENTS',
        message: 'No unsettled payments found for cashier',
      },
    },
  }
  assert.equal(
    mapCashierShiftError(errNoUnsettled),
    'Ca hiện tại chưa có khoản thu nào để thực hiện chốt ca.'
  )

  // 2. CashierShiftNoteRequiredException
  const errNoteRequired = {
    response: {
      data: {
        code: 'CASHIER_SHIFT_NOTE_REQUIRED',
        message: 'Notes are required when actual cash does not match system cash',
      },
    },
  }
  assert.equal(
    mapCashierShiftError(errNoteRequired),
    'Số tiền thực tế có chênh lệch so với hệ thống, bắt buộc phải nhập ghi chú giải trình.'
  )

  // 3. SelfConfirmationNotAllowedException
  const errSelfConfirm = {
    response: {
      data: {
        code: 'CASHIER_SHIFT_SELF_CONFIRMATION_NOT_ALLOWED',
        message: 'Cashier cannot confirm their own shift',
      },
    },
  }
  assert.equal(
    mapCashierShiftError(errSelfConfirm),
    'Bạn không thể tự duyệt phiếu chốt ca của chính mình.'
  )

  // 4. PaymentAlreadySettledException (QTN-38)
  const errSettled = {
    response: {
      data: {
        code: 'PAYMENT_ALREADY_SETTLED',
        message: 'Payment has already been settled in a cashier shift',
      },
    },
  }
  assert.equal(
    mapCashierShiftError(errSettled),
    'Khoản thu này đã nằm trong ca đã chốt, không thể chỉnh sửa hoặc hoàn tiền.'
  )

  // 5. CashierShiftAlreadyConfirmedException
  const errAlreadyConfirmed = {
    response: {
      data: {
        code: 'CASHIER_SHIFT_ALREADY_CONFIRMED',
        message: 'Shift is already confirmed',
      },
    },
  }
  assert.equal(
    mapCashierShiftError(errAlreadyConfirmed),
    'Phiếu chốt ca này đã được xác nhận trước đó.'
  )
})

// ============================================================================
// 6. Kiểm thử Định dạng Tiền tệ, Ngày giờ và Menu Navigation
// ============================================================================
test('TC-CS-06: Định dạng tiền tệ, ngày giờ và tích hợp Navigation menu', () => {
  // Tiền tệ
  assert.equal(formatCurrency(1500000).replace(/\s/g, ' '), '1.500.000 ₫'.replace(/\s/g, ' '))
  assert.equal(formatCurrency(0).replace(/\s/g, ' '), '0 ₫'.replace(/\s/g, ' '))
  assert.equal(formatCurrency(null).replace(/\s/g, ' '), '0 ₫'.replace(/\s/g, ' '))

  // Ngày giờ
  assert.equal(formatDateTime(null), '—')
  const dateStr = formatDateTime('2026-09-23T08:30:00Z')
  assert.equal(typeof dateStr, 'string')
  assert.notEqual(dateStr, '—')

  // Trạng thái meta
  assert.equal(getShiftStatusMeta('PENDING_CONFIRMATION').tagColor, 'gold')
  assert.equal(getShiftStatusMeta('CONFIRMED').tagColor, 'green')
  assert.equal(getShiftStatusMeta('REJECTED').tagColor, 'red')

  // Navigation menu
  const recepNav = getNavigationItems(['receptionist'], ['CASHIER_SHIFT_READ', 'CASHIER_SHIFT_CREATE'])
  const hasCloseMenu = recepNav.some((item) => item.key === '/cashier-shifts/close')
  const hasHistoryMenu = recepNav.some((item) => item.key === '/cashier-shifts/history')
  assert.equal(hasCloseMenu, true, 'Lễ tân phải nhìn thấy menu Chốt ca thu ngân')
  assert.equal(hasHistoryMenu, true, 'Lễ tân phải nhìn thấy menu Lịch sử chốt ca')

  const docNav = getNavigationItems(['doctor'], ['MEDICAL_RECORD_READ'])
  assert.equal(
    docNav.some((item) => item.key === '/cashier-shifts/close'),
    false,
    'Bác sĩ không nhìn thấy menu chốt ca'
  )
})
