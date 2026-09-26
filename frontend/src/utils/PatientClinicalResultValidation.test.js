import test from 'node:test'
import assert from 'node:assert/strict'
import dayjs from 'dayjs'

import {
  calculateValuePercentage,
  filterConfirmedResults,
  formatDate,
  formatDateTime,
  formatTime,
  getAbnormalFlagInfo,
  getSecuritySafeErrorMessage,
  getServiceCategory,
  isPostVisitResult,
  isRecentResult,
  isValidUuid,
} from './patientClinicalResultHelpers.js'

test('TC-CR-01: isValidUuid - Nhận diện UUID hợp lệ và từ chối mã tuần tự (Bảo mật QTN-23)', () => {
  // UUID v4 hợp lệ
  assert.equal(isValidUuid('b2c3d4e5-f6a7-4890-8cde-f12345678901'), true)
  assert.equal(isValidUuid('E1F2A3B4-C5D6-4890-ABCD-EF1234567890'), true)

  // Mã tuần tự hoặc chuỗi rác có thể bị dò quét URL (Phải bị từ chối)
  assert.equal(isValidUuid('1'), false)
  assert.equal(isValidUuid('123'), false)
  assert.equal(isValidUuid('KB-001'), false)
  assert.equal(isValidUuid(''), false)
  assert.equal(isValidUuid(null), false)
  assert.equal(isValidUuid(undefined), false)
  assert.equal(isValidUuid('b2c3d4e5-invalid-id'), false)
})

test('TC-CR-02: filterConfirmedResults - RÀNG BUỘC BẮT BUỘC: CHỈ giữ lại kết quả status === FINAL (TC-02)', () => {
  const mixedResults = [
    {
      clinicalResultId: 'res-1',
      serviceName: 'Tổng phân tích tế bào máu',
      status: 'FINAL',
      numericValue: 14.2,
    },
    {
      clinicalResultId: 'res-2',
      serviceName: 'Glucose máu',
      status: 'DRAFT', // Tuyệt đối không được lộ
      numericValue: 8.5,
    },
    {
      clinicalResultId: 'res-3',
      serviceName: 'Men gan AST',
      status: 'PENDING',
      numericValue: 45,
    },
    {
      clinicalResultId: 'res-4',
      serviceName: 'X-quang ngực thẳng',
      status: 'FINAL',
      conclusion: 'Bình thường',
    },
    {
      clinicalResultId: 'res-5',
      serviceName: 'Điện tim',
      status: null,
    },
  ]

  const filtered = filterConfirmedResults(mixedResults)

  assert.equal(filtered.length, 2)
  assert.equal(filtered[0].clinicalResultId, 'res-1')
  assert.equal(filtered[0].status, 'FINAL')
  assert.equal(filtered[1].clinicalResultId, 'res-4')
  assert.equal(filtered[1].status, 'FINAL')

  // Không chứa DRAFT
  const hasDraft = filtered.some((r) => r.status === 'DRAFT')
  assert.equal(hasDraft, false)
})

test('TC-CR-03: filterConfirmedResults - Trả về mảng rỗng an toàn khi không có kết quả hợp lệ', () => {
  assert.deepEqual(filterConfirmedResults([]), [])
  assert.deepEqual(filterConfirmedResults(null), [])
  assert.deepEqual(filterConfirmedResults(undefined), [])
  assert.deepEqual(filterConfirmedResults([{ status: 'DRAFT' }]), [])
})

test('TC-CR-04: getAbnormalFlagInfo - Phân loại cờ bất thường và sinh thẻ thông tin chuẩn xác', () => {
  const normal = getAbnormalFlagInfo('NORMAL')
  assert.equal(normal.isAbnormal, false)
  assert.equal(normal.text, 'Chỉ số bình thường')
  assert.equal(normal.badgeColor, '#16a34a')

  const high = getAbnormalFlagInfo('HIGH')
  assert.equal(high.isAbnormal, true)
  assert.equal(high.direction, 'HIGH')
  assert.equal(high.text, 'Cao hơn bình thường')
  assert.equal(high.badgeColor, '#dc2626')

  const low = getAbnormalFlagInfo('LOW')
  assert.equal(low.isAbnormal, true)
  assert.equal(low.direction, 'LOW')
  assert.equal(low.text, 'Thấp hơn bình thường')
  assert.equal(low.badgeColor, '#d97706')

  const abnormal = getAbnormalFlagInfo('ABNORMAL')
  assert.equal(abnormal.isAbnormal, true)
  assert.equal(abnormal.text, 'Chỉ số bất thường')

  // Không phân biệt chữ hoa thường
  const lowerCase = getAbnormalFlagInfo('high')
  assert.equal(lowerCase.isAbnormal, true)
  assert.equal(lowerCase.direction, 'HIGH')
})

test('TC-CR-05: isPostVisitResult - Nhận diện kết quả trả sau (sau giờ khám từ 2 giờ trở lên)', () => {
  const visitAt = '2026-09-20T08:00:00Z'
  const sameHourEntered = '2026-09-20T08:45:00Z'
  const delayedEntered = '2026-09-20T14:30:00Z' // Sau 6.5 giờ
  const nextDayEntered = '2026-09-22T09:00:00Z' // Sau 2 ngày

  assert.equal(isPostVisitResult(sameHourEntered, visitAt), false)
  assert.equal(isPostVisitResult(delayedEntered, visitAt), true)
  assert.equal(isPostVisitResult(nextDayEntered, visitAt), true)
  assert.equal(isPostVisitResult(null, visitAt), false)
})

test('TC-CR-06: isRecentResult - Nhận diện kết quả mới trong vòng 7 ngày', () => {
  const now = dayjs()
  const today = now.toISOString()
  const twoDaysAgo = now.subtract(2, 'day').toISOString()
  const tenDaysAgo = now.subtract(10, 'day').toISOString()

  assert.equal(isRecentResult(today), true)
  assert.equal(isRecentResult(twoDaysAgo), true)
  assert.equal(isRecentResult(tenDaysAgo), false)
  assert.equal(isRecentResult(null), false)
})

test('TC-CR-07: calculateValuePercentage - Tính vị trí thanh thước đo chỉ số trực quan', () => {
  // Khoảng 10 - 20 (midpoint là 15 -> ~50%)
  const midPct = calculateValuePercentage(15, 10, 20)
  assert.equal(midPct, 50)

  // Giá trị thấp hơn cận dưới
  const lowPct = calculateValuePercentage(8, 10, 20)
  assert.ok(lowPct < 50)

  // Giá trị cao hơn cận trên
  const highPct = calculateValuePercentage(22, 10, 20)
  assert.ok(highPct > 50)

  // Fallback an toàn khi dữ liệu không hợp lệ
  assert.equal(calculateValuePercentage('invalid', 10, 20), 50)
  assert.equal(calculateValuePercentage(15, 20, 10), 50) // min >= max
})

test('TC-CR-08: getSecuritySafeErrorMessage - Thông báo lỗi bảo mật không tiết lộ sự tồn tại của hồ sơ', () => {
  const err403 = { response: { status: 403 } }
  const msg403 = getSecuritySafeErrorMessage(err403)
  assert.equal(
    msg403,
    'Không tìm thấy kết quả cận lâm sàng của lượt khám này hoặc bạn không có quyền truy cập.'
  )

  const err404 = { response: { status: 404 } }
  const msg404 = getSecuritySafeErrorMessage(err404)
  assert.equal(
    msg404,
    'Không tìm thấy kết quả cận lâm sàng của lượt khám này hoặc bạn không có quyền truy cập.'
  )

  const err401 = { response: { status: 401 } }
  const msg401 = getSecuritySafeErrorMessage(err401)
  assert.equal(msg401, 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.')

  const errOther = { response: { data: { message: 'Lỗi máy chủ' } } }
  assert.equal(getSecuritySafeErrorMessage(errOther), 'Lỗi máy chủ')
})

test('TC-CR-09: getServiceCategory - Phân loại danh mục dịch vụ cận lâm sàng chính xác', () => {
  const lab1 = getServiceCategory('XN-MAU-01', 'Tổng phân tích tế bào máu')
  assert.equal(lab1.category, 'LAB')
  assert.equal(lab1.label, 'Xét nghiệm')

  const imaging1 = getServiceCategory('CDHA-XQ-01', 'Chụp X-quang phổi thẳng')
  assert.equal(imaging1.category, 'IMAGING')
  assert.equal(imaging1.label, 'Chẩn đoán hình ảnh')

  const imaging2 = getServiceCategory('US-01', 'Siêu âm màu ổ bụng tổng quát')
  assert.equal(imaging2.category, 'IMAGING')

  const functional = getServiceCategory('TDCN-01', 'Điện tâm đồ 12 chuyển đạo')
  assert.equal(functional.category, 'FUNCTIONAL')
  assert.equal(functional.label, 'Thăm dò chức năng')
})

test('TC-CR-10: formatDate và formatDateTime - Định dạng ngày giờ chuẩn Việt Nam', () => {
  const isoStr = '2026-09-24T08:30:00Z'
  const formattedD = formatDate(isoStr)
  assert.match(formattedD, /^\d{2}\/\d{2}\/\d{4}$/)

  const formattedT = formatTime(isoStr)
  assert.match(formattedT, /^\d{2}:\d{2}$/)

  const formattedDT = formatDateTime(isoStr)
  assert.match(formattedDT, /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/)

  assert.equal(formatDate(null), '—')
  assert.equal(formatTime(null), '—')
  assert.equal(formatDateTime(null), '—')
})
