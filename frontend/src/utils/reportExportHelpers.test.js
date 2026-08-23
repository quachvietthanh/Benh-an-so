import test from 'node:test'
import assert from 'node:assert/strict'
import {
  REPORT_TYPES,
  validateExportParams,
  getExportFilename,
  getExportErrorMessage,
} from './reportExportHelpers.js'

test('reportExportHelpers - REPORT_TYPES contains all 3 required report types', () => {
  assert.deepEqual(REPORT_TYPES, [
    { value: 'VISIT_REPORT', label: 'Báo cáo lượt khám' },
    { value: 'REVENUE_REPORT', label: 'Báo cáo doanh thu' },
    { value: 'OPERATIONAL_REPORT', label: 'Báo cáo tổng hợp vận hành' },
  ])
})

test('reportExportHelpers - validateExportParams returns valid when dates are correct and within 366 days', () => {
  const result = validateExportParams('2026-08-01', '2026-08-20')
  assert.equal(result.isValid, true)
  assert.equal(result.from, '2026-08-01')
  assert.equal(result.to, '2026-08-20')
  assert.equal(result.message, null)
})

test('reportExportHelpers - validateExportParams returns error when missing from or to', () => {
  assert.equal(validateExportParams(null, '2026-08-20').isValid, false)
  assert.equal(validateExportParams('2026-08-01', null).isValid, false)
  assert.equal(validateExportParams('', '').message, 'Vui lòng chọn khoảng thời gian.')
})

test('reportExportHelpers - validateExportParams returns error when from is after to', () => {
  const result = validateExportParams('2026-08-20', '2026-08-01')
  assert.equal(result.isValid, false)
  assert.equal(result.message, 'Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.')
})

test('reportExportHelpers - validateExportParams returns error when range exceeds 366 days', () => {
  const result = validateExportParams('2025-01-01', '2026-01-03')
  assert.equal(result.isValid, false)
  assert.equal(result.message, 'Khoảng thời gian xuất báo cáo không được vượt quá 366 ngày.')
})

test('reportExportHelpers - validateExportParams returns valid when range is exactly 366 days', () => {
  const result = validateExportParams('2024-01-01', '2024-12-31')
  assert.equal(result.isValid, true)
})

test('reportExportHelpers - getExportFilename extracts filename correctly', () => {
  const headerQuotes = 'attachment; filename="custom-report-2026.csv"'
  assert.equal(getExportFilename(headerQuotes, 'VISIT_REPORT', '2026-08-01', '2026-08-20'), 'custom-report-2026.csv')

  const headerUtf8 = "attachment; filename*=UTF-8''bao-cao-kham-2026.csv"
  assert.equal(getExportFilename(headerUtf8, 'VISIT_REPORT', '2026-08-01', '2026-08-20'), 'bao-cao-kham-2026.csv')

  assert.equal(getExportFilename(null, 'VISIT_REPORT', '2026-08-01', '2026-08-20'), 'visit-report-2026-08-01-to-2026-08-20.csv')
  assert.equal(getExportFilename(null, 'REVENUE_REPORT', '2026-08-01', '2026-08-20'), 'revenue-report-2026-08-01-to-2026-08-20.csv')
  assert.equal(getExportFilename(null, 'OPERATIONAL_REPORT', '2026-08-01', '2026-08-20'), 'operational-report-2026-08-01-to-2026-08-20.csv')
})

test('reportExportHelpers - getExportErrorMessage handles HTTP status codes properly', async () => {
  const err403 = { response: { status: 403 } }
  assert.equal(await getExportErrorMessage(err403), 'Bạn không có quyền xuất báo cáo.')

  const err404 = { response: { status: 404, data: { code: 'REPORT_DATA_EMPTY' } } }
  assert.equal(await getExportErrorMessage(err404), 'Không có dữ liệu trong khoảng thời gian đã chọn.')

  const err400 = { response: { status: 400 } }
  assert.equal(await getExportErrorMessage(err400), 'Khoảng thời gian không hợp lệ.')

  const err500 = { response: { status: 500 } }
  assert.equal(await getExportErrorMessage(err500), 'Không thể xuất báo cáo. Vui lòng thử lại.')
})
