import test from 'node:test'
import assert from 'node:assert/strict'
import dayjs from 'dayjs'
import {
  calculateServiceStats,
  categorizePriceHistory,
  checkServiceManagementPermission,
  formatVND,
  normalizeServiceList,
  validateCreateServicePayload,
  validateUpdateServicePayload,
} from '../utils/serviceCatalogHelpers.js'

// =========================================================================
// TEST SUITE: QUẢN LÝ DANH MỤC DỊCH VỤ VÀ BẢNG GIÁ (SERVICE PRICE MANAGEMENT)
// =========================================================================

test('1. Phân quyền quản trị: Chỉ ADMIN và MANAGER có quyền quản lý dịch vụ và bảng giá', () => {
  // Admin & Manager được phép
  assert.equal(checkServiceManagementPermission(['ROLE_ADMIN']), true)
  assert.equal(checkServiceManagementPermission(['admin']), true)
  assert.equal(checkServiceManagementPermission(['ROLE_MANAGER']), true)
  assert.equal(checkServiceManagementPermission(['manager']), true)
  assert.equal(checkServiceManagementPermission(['admin', 'doctor']), true)

  // Các vai trò khác không được phép
  assert.equal(checkServiceManagementPermission(['doctor']), false)
  assert.equal(checkServiceManagementPermission(['ROLE_DOCTOR']), false)
  assert.equal(checkServiceManagementPermission(['nurse']), false)
  assert.equal(checkServiceManagementPermission(['receptionist']), false)
  assert.equal(checkServiceManagementPermission(['pharmacist']), false)
  assert.equal(checkServiceManagementPermission([]), false)
  assert.equal(checkServiceManagementPermission(null), false)
})

test('2. Chuẩn hóa dữ liệu danh mục: Hỗ trợ Spring Data Page và mảng danh sách', () => {
  const springPageData = {
    content: [
      { id: '1', serviceCode: 'KHAM-TQ', name: 'Khám tổng quát', price: 150000, active: true },
      { id: '2', serviceCode: 'XN-MAU', name: 'Xét nghiệm máu', price: 200000, active: true },
    ],
    totalElements: 2,
    totalPages: 1,
  }

  const normalizedFromPage = normalizeServiceList(springPageData)
  assert.equal(normalizedFromPage.length, 2)
  assert.equal(normalizedFromPage[0].serviceCode, 'KHAM-TQ')

  const rawArray = [
    { id: '3', serviceCode: 'SA-TIM', name: 'Siêu âm tim', price: 350000, active: true },
  ]
  const normalizedFromArray = normalizeServiceList(rawArray)
  assert.equal(normalizedFromArray.length, 1)

  assert.deepEqual(normalizeServiceList(null), [])
  assert.deepEqual(normalizeServiceList({}), [])
})

test('3. Validate tạo dịch vụ mới: Bắt buộc mã dịch vụ, tên, đơn giá >= 0 và ngày hiệu lực', () => {
  // Case 1: Hợp lệ
  const validPayload = {
    serviceCode: 'KHAM-NOI',
    name: 'Khám chuyên khoa nội',
    price: 120000,
    effectiveFrom: '2026-08-01',
  }
  const result1 = validateCreateServicePayload(validPayload)
  assert.equal(result1.isValid, true)
  assert.equal(result1.payload.serviceCode, 'KHAM-NOI')
  assert.equal(result1.payload.price, 120000)
  assert.equal(result1.payload.effectiveFrom, '2026-08-01')

  // Case 2: Thiếu mã dịch vụ
  const missingCode = validateCreateServicePayload({
    serviceCode: '   ',
    name: 'Khám nội',
    price: 100000,
    effectiveFrom: '2026-08-01',
  })
  assert.equal(missingCode.isValid, false)
  assert.equal(missingCode.errors.serviceCode, 'Vui lòng nhập mã dịch vụ')

  // Case 3: Mã dịch vụ quá 50 ký tự
  const longCode = validateCreateServicePayload({
    serviceCode: 'A'.repeat(51),
    name: 'Khám nội',
    price: 100000,
    effectiveFrom: '2026-08-01',
  })
  assert.equal(longCode.isValid, false)
  assert.equal(longCode.errors.serviceCode, 'Mã dịch vụ không được vượt quá 50 ký tự')

  // Case 4: Thiếu tên dịch vụ
  const missingName = validateCreateServicePayload({
    serviceCode: 'KHAM-01',
    name: '',
    price: 100000,
    effectiveFrom: '2026-08-01',
  })
  assert.equal(missingName.isValid, false)
  assert.equal(missingName.errors.name, 'Vui lòng nhập tên dịch vụ')

  // Case 5: Đơn giá âm hoặc không hợp lệ
  const negativePrice = validateCreateServicePayload({
    serviceCode: 'KHAM-01',
    name: 'Khám nhi',
    price: -5000,
    effectiveFrom: '2026-08-01',
  })
  assert.equal(negativePrice.isValid, false)
  assert.equal(negativePrice.errors.price, 'Đơn giá phải lớn hơn hoặc bằng 0')

  // Case 6: Thiếu ngày hiệu lực
  const missingDate = validateCreateServicePayload({
    serviceCode: 'KHAM-01',
    name: 'Khám nhi',
    price: 100000,
    effectiveFrom: null,
  })
  assert.equal(missingDate.isValid, false)
  assert.equal(missingDate.errors.effectiveFrom, 'Vui lòng chọn ngày bắt đầu hiệu lực')
})

test('4. Validate cập nhật dịch vụ và giá: Tên, đơn giá, ngày hiệu lực và trạng thái', () => {
  // Case 1: Cập nhật hợp lệ
  const validUpdate = {
    name: 'Khám chuyên khoa nội tổng quát (cập nhật)',
    price: 150000,
    effectiveFrom: '2026-09-01',
    active: true,
  }
  const res1 = validateUpdateServicePayload(validUpdate)
  assert.equal(res1.isValid, true)
  assert.equal(res1.payload.name, 'Khám chuyên khoa nội tổng quát (cập nhật)')
  assert.equal(res1.payload.price, 150000)
  assert.equal(res1.payload.effectiveFrom, '2026-09-01')
  assert.equal(res1.payload.active, true)

  // Case 2: Tên quá dài
  const longName = validateUpdateServicePayload({
    name: 'B'.repeat(256),
    price: 150000,
    effectiveFrom: '2026-09-01',
  })
  assert.equal(longName.isValid, false)
  assert.equal(longName.errors.name, 'Tên dịch vụ không được vượt quá 255 ký tự')

  // Case 3: Đơn giá 0 đồng (miễn phí) là hợp lệ
  const freePrice = validateUpdateServicePayload({
    name: 'Tư vấn sức khỏe định kỳ',
    price: 0,
    effectiveFrom: '2026-09-01',
  })
  assert.equal(freePrice.isValid, true)
  assert.equal(freePrice.payload.price, 0)
})

test('5. Định dạng tiền tệ VND hiển thị nhất quán trên hóa đơn và bảng giá', () => {
  assert.equal(formatVND(150000), '150.000 ₫')
  assert.equal(formatVND(0), '0 ₫')
  assert.equal(formatVND(2500000), '2.500.000 ₫')
  assert.equal(formatVND(null), '0 ₫')
  assert.equal(formatVND(undefined), '0 ₫')
})

test('6. Phân loại lịch sử giá theo thời điểm: Hiện hành (CURRENT), Tương lai (UPCOMING), Quá khứ (PAST)', () => {
  const referenceToday = dayjs('2026-08-19')

  const priceHistoryList = [
    {
      id: 'p1',
      price: 100000,
      effectiveFrom: '2026-01-01',
      createdAt: '2026-01-01T08:00:00Z',
    },
    {
      id: 'p2',
      price: 150000,
      effectiveFrom: '2026-08-01',
      createdAt: '2026-07-25T10:00:00Z',
    },
    {
      id: 'p3',
      price: 200000,
      effectiveFrom: '2026-09-01', // Tương lai
      createdAt: '2026-08-15T09:00:00Z',
    },
  ]

  const categorized = categorizePriceHistory(priceHistoryList, referenceToday)

  assert.equal(categorized.length, 3)

  // Mốc 2026-09-01 (tương lai) -> UPCOMING
  const upcoming = categorized.find((p) => p.effectiveFrom === '2026-09-01')
  assert.equal(upcoming.status, 'UPCOMING')
  assert.equal(upcoming.statusLabel, 'Sắp áp dụng')
  assert.equal(upcoming.badgeColor, 'warning')

  // Mốc 2026-08-01 (gần nhất trước hôm nay) -> CURRENT
  const current = categorized.find((p) => p.effectiveFrom === '2026-08-01')
  assert.equal(current.status, 'CURRENT')
  assert.equal(current.statusLabel, 'Đang áp dụng')
  assert.equal(current.badgeColor, 'success')

  // Mốc 2026-01-01 (cũ hơn) -> PAST
  const past = categorized.find((p) => p.effectiveFrom === '2026-01-01')
  assert.equal(past.status, 'PAST')
  assert.equal(past.statusLabel, 'Lịch sử')
  assert.equal(past.badgeColor, 'default')
})

test('7. Tính toán thống kê KPI danh mục dịch vụ chính xác', () => {
  const serviceList = [
    { id: '1', name: 'Khám nội', price: 100000, active: true },
    { id: '2', name: 'Khám ngoại', price: 200000, active: true },
    { id: '3', name: 'Khám mắt', price: 300000, active: true },
    { id: '4', name: 'Dịch vụ cũ ngừng dùng', price: 50000, active: false },
  ]

  const stats = calculateServiceStats(serviceList)
  assert.equal(stats.total, 4)
  assert.equal(stats.activeCount, 3)
  assert.equal(stats.inactiveCount, 1)
  // Giá trung bình các dịch vụ đang hiệu lực: (100k + 200k + 300k) / 3 = 200k
  assert.equal(stats.avgPrice, 200000)

  // Danh sách rỗng
  const emptyStats = calculateServiceStats([])
  assert.equal(emptyStats.total, 0)
  assert.equal(emptyStats.activeCount, 0)
  assert.equal(emptyStats.inactiveCount, 0)
  assert.equal(emptyStats.avgPrice, 0)
})
