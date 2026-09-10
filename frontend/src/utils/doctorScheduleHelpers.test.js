import assert from 'node:assert/strict'
import test from 'node:test'
import {
  DAYS_OF_WEEK,
  formatAppointmentStatus,
  formatDuration,
  formatTimeOffStatus,
  getDayLabel,
  getDayShortLabel,
  isTimeWithinClinicHours,
  normalizeTimeDisplay,
  toBackendTime,
} from './doctorScheduleHelpers.js'

test('DAYS_OF_WEEK contains all 7 days from MONDAY to SUNDAY', () => {
  assert.equal(DAYS_OF_WEEK.length, 7)
  assert.equal(DAYS_OF_WEEK[0].key, 'MONDAY')
  assert.equal(DAYS_OF_WEEK[6].key, 'SUNDAY')
  assert.equal(getDayLabel('MONDAY'), 'Thứ Hai')
  assert.equal(getDayShortLabel('MONDAY'), 'T2')
  assert.equal(getDayLabel('SUNDAY'), 'Chủ Nhật')
  assert.equal(getDayShortLabel('SUNDAY'), 'CN')
})

test('normalizeTimeDisplay formats time strings cleanly', () => {
  assert.equal(normalizeTimeDisplay('08:00:00'), '08:00')
  assert.equal(normalizeTimeDisplay('8:30:00'), '08:30')
  assert.equal(normalizeTimeDisplay('17:30'), '17:30')
  assert.equal(normalizeTimeDisplay(''), '')
})

test('toBackendTime normalizes time string to HH:mm:ss', () => {
  assert.equal(toBackendTime('08:00'), '08:00:00')
  assert.equal(toBackendTime('8:30'), '08:30:00')
  assert.equal(toBackendTime('08:00:00'), '08:00:00')
  assert.equal(toBackendTime(''), '')
})

test('isTimeWithinClinicHours accepts valid working hours within clinic opening and closing', () => {
  const result = isTimeWithinClinicHours('08:00', '17:00', '07:30:00', '17:30:00')
  assert.equal(result.valid, true)
  assert.equal(result.message, '')
})

test('isTimeWithinClinicHours rejects endTime before or equal to startTime', () => {
  const result1 = isTimeWithinClinicHours('17:00', '08:00', '07:30:00', '17:30:00')
  assert.equal(result1.valid, false)
  assert.equal(result1.message, 'Giờ kết thúc phải sau giờ bắt đầu.')

  const result2 = isTimeWithinClinicHours('08:00', '08:00', '07:30:00', '17:30:00')
  assert.equal(result2.valid, false)
  assert.equal(result2.message, 'Giờ kết thúc phải sau giờ bắt đầu.')
})

test('isTimeWithinClinicHours rejects startTime earlier than clinic openingTime', () => {
  const result = isTimeWithinClinicHours('07:00', '16:00', '07:30:00', '17:30:00')
  assert.equal(result.valid, false)
  assert.match(result.message, /sớm hơn giờ mở cửa/)
})

test('isTimeWithinClinicHours rejects endTime later than clinic closingTime', () => {
  const result = isTimeWithinClinicHours('08:00', '18:00', '07:30:00', '17:30:00')
  assert.equal(result.valid, false)
  assert.match(result.message, /muộn hơn giờ đóng cửa/)
})

test('formatDuration formats hours and minutes correctly', () => {
  const start = '2026-09-10T08:00:00Z'
  const end1 = '2026-09-10T12:00:00Z'
  assert.equal(formatDuration(start, end1), '4 giờ')

  const end2 = '2026-09-10T09:30:00Z'
  assert.equal(formatDuration(start, end2), '1 giờ 30 phút')

  const end3 = '2026-09-10T08:45:00Z'
  assert.equal(formatDuration(start, end3), '45 phút')
})

test('formatTimeOffStatus returns correct tag labels and colors', () => {
  assert.deepEqual(formatTimeOffStatus('ACTIVE'), { label: 'Đang hiệu lực', color: 'success' })
  assert.deepEqual(formatTimeOffStatus('CANCELLED'), { label: 'Đã hủy', color: 'default' })
})

test('formatAppointmentStatus returns correct tag labels and colors', () => {
  assert.deepEqual(formatAppointmentStatus('SCHEDULED'), { label: 'Đã đặt hẹn', color: 'blue' })
  assert.deepEqual(formatAppointmentStatus('CONFIRMED'), { label: 'Đã xác nhận', color: 'cyan' })
  assert.deepEqual(formatAppointmentStatus('CANCELLED'), { label: 'Đã hủy', color: 'default' })
})
