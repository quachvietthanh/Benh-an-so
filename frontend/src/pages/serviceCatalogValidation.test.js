import test from 'node:test'
import assert from 'node:assert/strict'
import {
  checkServiceManagePermission,
  formatMoney,
  formatDate,
  validateServicePayload,
  processPriceHistory,
  calculateServiceStats,
  translateServiceErrorMessage,
  fixVietnameseEncoding,
  normalizeServiceItem,
} from '../utils/serviceCatalogHelpers.js'

// ============================================================================
// TEST SUITE: QUẢN LÝ DANH MỤC DỊCH VỤ VÀ BẢNG GIÁ HIỆU LỰC (SERVICE CATALOG)
// ============================================================================

test('1. Phân quyền: Chỉ Quản trị viên (ADMIN) và Quản lý (MANAGER) mới có quyền quản lý danh mục & bảng giá', () => {
  assert.equal(checkServiceManagePermission(['admin']), true)
  assert.equal(checkServiceManagePermission(['ROLE_ADMIN']), true)
  assert.equal(checkServiceManagePermission(['manager']), true)
  assert.equal(checkServiceManagePermission(['ROLE_MANAGER']), true)
  assert.equal(checkServiceManagePermission(['doctor']), false)
  assert.equal(checkServiceManagePermission(['nurse']), false)
  assert.equal(checkServiceManagePermission(['receptionist']), false)
  assert.equal(checkServiceManagePermission(['pharmacist']), false)
  assert.equal(checkServiceManagePermission([]), false)
  assert.equal(checkServiceManagePermission(null), false)
})

test('2. Định dạng tiền tệ VND chuẩn xác (formatMoney)', () => {
  assert.equal(formatMoney(150000), '150.000 ₫')
  assert.equal(formatMoney(0), '0 ₫')
  assert.equal(formatMoney('500000'), '500.000 ₫')
  assert.equal(formatMoney(null), '0 ₫')
  assert.equal(formatMoney(undefined), '0 ₫')
})

test('3. Định dạng ngày tháng DD/MM/YYYY chuẩn xác (formatDate)', () => {
  assert.equal(formatDate('2026-08-19'), '19/08/2026')
  assert.equal(formatDate('2026-08-01T00:00:00Z'), '01/08/2026')
  assert.equal(formatDate(null), '—')
  assert.equal(formatDate(''), '—')
})

test('4. Validate Payload tạo mới dịch vụ (Create Service Validation)', () => {
  // Thiếu mã dịch vụ
  const r1 = validateServicePayload({ serviceCode: '', name: 'Khám Nội', price: 100000, effectiveFrom: '2026-08-19' }, false)
  assert.equal(r1.valid, false)
  assert.equal(r1.error, 'Mã dịch vụ không được để trống.')

  // Mã dịch vụ quá 50 ký tự
  const longCode = 'A'.repeat(51)
  const r2 = validateServicePayload({ serviceCode: longCode, name: 'Khám Nội', price: 100000, effectiveFrom: '2026-08-19' }, false)
  assert.equal(r2.valid, false)
  assert.equal(r2.error, 'Mã dịch vụ không được vượt quá 50 ký tự.')

  // Thiếu tên dịch vụ
  const r3 = validateServicePayload({ serviceCode: 'DV01', name: '   ', price: 100000, effectiveFrom: '2026-08-19' }, false)
  assert.equal(r3.valid, false)
  assert.equal(r3.error, 'Tên dịch vụ không được để trống.')

  // Đơn giá âm
  const r4 = validateServicePayload({ serviceCode: 'DV01', name: 'Khám Nội', price: -5000, effectiveFrom: '2026-08-19' }, false)
  assert.equal(r4.valid, false)
  assert.equal(r4.error, 'Đơn giá dịch vụ phải lớn hơn hoặc bằng 0.')

  // Thiếu ngày hiệu lực
  const r5 = validateServicePayload({ serviceCode: 'DV01', name: 'Khám Nội', price: 100000, effectiveFrom: '' }, false)
  assert.equal(r5.valid, false)
  assert.equal(r5.error, 'Ngày hiệu lực của giá không được để trống.')

  // Payload hợp lệ
  const r6 = validateServicePayload({
    serviceCode: '  kham-01  ',
    name: '   Khám   Tổng   Quát   ',
    price: 150000,
    effectiveFrom: '2026-08-19',
  }, false)
  assert.equal(r6.valid, true)
  assert.equal(r6.sanitizedData.serviceCode, 'KHAM-01')
  assert.equal(r6.sanitizedData.name, 'Khám Tổng Quát')
  assert.equal(r6.sanitizedData.price, 150000)
  assert.equal(r6.sanitizedData.effectiveFrom, '2026-08-19')
})

test('5. Validate Payload cập nhật dịch vụ (Update Service Validation)', () => {
  // Khi sửa, không yêu cầu truyền serviceCode (vì mã là bất biến)
  const r1 = validateServicePayload({
    name: 'Khám Chuyên Khoa Mắt',
    price: 200000,
    effectiveFrom: '2026-09-01',
    active: true,
  }, true)
  assert.equal(r1.valid, true)
  assert.equal(r1.sanitizedData.name, 'Khám Chuyên Khoa Mắt')
  assert.equal(r1.sanitizedData.price, 200000)
  assert.equal(r1.sanitizedData.effectiveFrom, '2026-09-01')
  assert.equal(r1.sanitizedData.active, true)
  assert.equal(r1.sanitizedData.serviceCode, undefined)
})

test('6. Xử lý và phân loại Lịch sử Bảng giá (Price History Versioning)', () => {
  const mockPrices = [
    { id: 'p1', price: 100000, effectiveFrom: '2026-01-01', createdAt: '2026-01-01T08:00:00Z' },
    { id: 'p2', price: 120000, effectiveFrom: '2026-06-01', createdAt: '2026-05-20T08:00:00Z' },
    { id: 'p3', price: 150000, effectiveFrom: '2026-08-15', createdAt: '2026-08-10T08:00:00Z' },
    { id: 'p4', price: 180000, effectiveFrom: '2026-09-01', createdAt: '2026-08-18T08:00:00Z' },
  ]

  // Giả sử ngày hiện tại là 2026-08-19
  const processed = processPriceHistory(mockPrices, '2026-08-19')

  assert.equal(processed.length, 4)

  // p4 (2026-09-01): Sắp áp dụng (FUTURE_SCHEDULED)
  assert.equal(processed[0].id, 'p4')
  assert.equal(processed[0].status, 'FUTURE_SCHEDULED')
  assert.equal(processed[0].statusLabel, 'Sắp áp dụng')
  assert.equal(processed[0].diffAmount, 30000) // 180k - 150k
  assert.equal(processed[0].diffPercent, 20) // +20%

  // p3 (2026-08-15): Đang áp dụng hiện tại (CURRENT_ACTIVE)
  assert.equal(processed[1].id, 'p3')
  assert.equal(processed[1].status, 'CURRENT_ACTIVE')
  assert.equal(processed[1].statusLabel, 'Đang áp dụng')
  assert.equal(processed[1].isCurrentActive, true)
  assert.equal(processed[1].diffAmount, 30000) // 150k - 120k
  assert.equal(processed[1].diffPercent, 25) // +25%

  // p2 (2026-06-01): Lịch sử cũ (SUPERSEDED)
  assert.equal(processed[2].id, 'p2')
  assert.equal(processed[2].status, 'SUPERSEDED')
  assert.equal(processed[2].statusLabel, 'Lịch sử')
  assert.equal(processed[2].diffAmount, 20000) // 120k - 100k
  assert.equal(processed[2].diffPercent, 20) // +20%

  // p1 (2026-01-01): Bản đầu tiên
  assert.equal(processed[3].id, 'p1')
  assert.equal(processed[3].status, 'SUPERSEDED')
  assert.equal(processed[3].diffAmount, null)
})

test('7. Tính toán Thống kê KPI Danh mục dịch vụ (calculateServiceStats)', () => {
  const mockServices = [
    { id: '1', name: 'Khám bệnh 1', price: 100000, active: true },
    { id: '2', name: 'Khám bệnh 2', price: 200000, active: true },
    { id: '3', name: 'Khám bệnh 3', price: 300000, active: false },
    { id: '4', name: 'Khám bệnh 4', price: 0, active: true },
  ]

  const stats = calculateServiceStats(mockServices)
  assert.equal(stats.total, 4)
  assert.equal(stats.activeCount, 3)
  assert.equal(stats.inactiveCount, 1)
  assert.equal(stats.avgPrice, 150000) // (100k + 200k + 300k + 0) / 4 = 150k
  assert.equal(stats.minPrice, 0)
  assert.equal(stats.maxPrice, 300000)
})

test('8. Chuyển đổi mã lỗi Backend thành thông báo tiếng Việt (translateServiceErrorMessage)', () => {
  const errCode = { response: { data: { message: 'Service code already exists.' } } }
  assert.equal(
    translateServiceErrorMessage(errCode),
    'Mã dịch vụ đã tồn tại trong hệ thống. Vui lòng nhập mã khác.'
  )

  const errName = { response: { data: { message: 'Service name already exists.' } } }
  assert.equal(
    translateServiceErrorMessage(errName),
    'Tên dịch vụ đã tồn tại trong hệ thống. Vui lòng chọn tên khác.'
  )

  const errPrice = {
    response: { data: { message: 'A different service price already exists for this effective date.' } },
  }
  assert.equal(
    translateServiceErrorMessage(errPrice),
    'Đã tồn tại một mức giá khác cho ngày hiệu lực này. Vui lòng chọn ngày khác hoặc giữ nguyên giá.'
  )

  const err403 = { response: { status: 403 } }
  assert.equal(
    translateServiceErrorMessage(err403),
    'Bạn không có quyền thực hiện thao tác quản lý dịch vụ/bảng giá.'
  )
})

test('9. Tự động khắc phục lỗi font tiếng Việt (Mojibake UTF-8 to ISO-8859-1 decoding)', () => {
  assert.equal(
    fixVietnameseEncoding('Ä\u0090á»\u008bnh lÆ°á»£ng Creatinin mÃ¡u'),
    'Định lượng Creatinin máu'
  )
  assert.equal(
    fixVietnameseEncoding('Ä\u0090á»\u008bnh lÆ°á»£ng Hemoglobin'),
    'Định lượng Hemoglobin'
  )
  assert.equal(
    fixVietnameseEncoding('Ä\u0090á»\u008bnh lÆ°á»£ng Ure mÃ¡u'),
    'Định lượng Ure máu'
  )
  assert.equal(
    fixVietnameseEncoding('Ä\u0090áº¿m sá»\u0091 lÆ°á»£ng tiá»\u0083u cáº§u'),
    'Đếm số lượng tiểu cầu'
  )
  assert.equal(
    fixVietnameseEncoding('Acid uric mÃ¡u'),
    'Acid uric máu'
  )
  assert.equal(
    fixVietnameseEncoding('Ä\u0090iá»\u0087n tÃ¢m Ä\u0091á»\u0093'),
    'Điện tâm đồ'
  )
  assert.equal(
    fixVietnameseEncoding('Ä\u0090o chá»\u00a9c nÄ\u0083ng hÃ´ háº¥p'),
    'Đo chức năng hô hấp'
  )
  assert.equal(
    fixVietnameseEncoding('Ä\u0090o huyáº¿t Ã¡p lÆ°u Ä\u0091á»\u0099ng 24 giá»\u009d'),
    'Đo huyết áp lưu động 24 giờ'
  )

  // Chuỗi tiếng Việt chuẩn không bị thay đổi
  assert.equal(fixVietnameseEncoding('Khám Nội tổng quát'), 'Khám Nội tổng quát')
  assert.equal(fixVietnameseEncoding('Siêu âm tim'), 'Siêu âm tim')

  // Chuẩn hóa object qua normalizeServiceItem
  const rawItem = {
    id: 's1',
    serviceCode: 'LAB-CREA',
    name: 'Ä\u0090á»\u008bnh lÆ°á»£ng Creatinin mÃ¡u',
    price: 45000,
  }
  const normalized = normalizeServiceItem(rawItem)
  assert.equal(normalized.name, 'Định lượng Creatinin máu')
  assert.equal(normalized.serviceCode, 'LAB-CREA')
})

