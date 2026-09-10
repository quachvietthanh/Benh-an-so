import assert from 'node:assert/strict'
import test from 'node:test'
import {
  DAYS_OF_WEEK,
  cleanDoctorScheduleErrorMessage,
  detectAffectedAppointments,
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

test('cleanDoctorScheduleErrorMessage removes technical codes and status code prefixes', () => {
  // Removes (403 Forbidden), (403), (400)
  assert.equal(
    cleanDoctorScheduleErrorMessage('Bạn không có quyền cấu hình lịch làm việc (403 Forbidden).'),
    'Bạn không có quyền cấu hình lịch làm việc.'
  )
  assert.equal(
    cleanDoctorScheduleErrorMessage('Bạn không có quyền xem lịch (403).'),
    'Bạn không có quyền xem lịch.'
  )
  // Removes 409 - prefix (like the user example)
  assert.equal(
    cleanDoctorScheduleErrorMessage('409 - Khoảng thời gian nghỉ trùng lặp với khoảng nghỉ đã đăng ký.'),
    'Khoảng thời gian nghỉ trùng lặp với khoảng nghỉ đã đăng ký.'
  )
  assert.equal(
    cleanDoctorScheduleErrorMessage('[403]: Bạn không có quyền truy cập'),
    'Bạn không có quyền truy cập'
  )
  assert.equal(
    cleanDoctorScheduleErrorMessage('400 - Giờ kết thúc phải sau giờ bắt đầu.'),
    'Giờ kết thúc phải sau giờ bắt đầu.'
  )
  // Removes raw domain codes
  assert.equal(
    cleanDoctorScheduleErrorMessage('Lỗi đăng ký (DOCTOR_TIMEOFF_CONFLICT)'),
    'Lỗi đăng ký'
  )
  // Preserves already clean Vietnamese messages
  assert.equal(
    cleanDoctorScheduleErrorMessage('Cập nhật lịch làm việc thành công!'),
    'Cập nhật lịch làm việc thành công!'
  )
  // Handles null/undefined safely
  assert.equal(cleanDoctorScheduleErrorMessage(null), '')
  assert.equal(cleanDoctorScheduleErrorMessage(undefined), '')
})

test('detectAffectedAppointments accurately finds appointments conflicting with new weekly schedule', () => {
  const weeklySchedules = [
    { dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '12:00', active: true },
    { dayOfWeek: 'TUESDAY', startTime: '08:00', endTime: '17:00', active: true },
    { dayOfWeek: 'SATURDAY', startTime: '08:00', endTime: '12:00', active: false }, // off
  ]

  // Saturday appointment (doctor is now off on Saturday) -> Affected
  const apptSaturday = {
    id: 'apt-1',
    appointmentCode: 'APT-SAT',
    startTime: '2026-08-29T09:00:00+07:00', // Saturday
    endTime: '2026-08-29T09:30:00+07:00',
    status: 'SCHEDULED',
  }

  // Monday afternoon appointment (doctor only works 08:00 - 12:00) -> Affected
  const apptMondayPM = {
    id: 'apt-2',
    appointmentCode: 'APT-MON-PM',
    startTime: '2026-08-31T14:00:00+07:00', // Monday 14:00
    endTime: '2026-08-31T14:30:00+07:00',
    status: 'CONFIRMED',
  }

  // Monday morning appointment within 08:00 - 12:00 -> NOT affected
  const apptMondayAM = {
    id: 'apt-3',
    appointmentCode: 'APT-MON-AM',
    startTime: '2026-08-31T09:00:00+07:00', // Monday 09:00
    endTime: '2026-08-31T09:30:00+07:00',
    status: 'SCHEDULED',
  }

  // Cancelled appointment -> NOT affected
  const apptCancelled = {
    id: 'apt-4',
    appointmentCode: 'APT-CANCELLED',
    startTime: '2026-08-29T10:00:00+07:00',
    endTime: '2026-08-29T10:30:00+07:00',
    status: 'CANCELLED',
  }

  const affected = detectAffectedAppointments(
    [apptSaturday, apptMondayPM, apptMondayAM, apptCancelled],
    weeklySchedules
  )

  assert.equal(affected.length, 2)
  assert.equal(affected[0].appointmentCode, 'APT-SAT')
  assert.equal(affected[1].appointmentCode, 'APT-MON-PM')
})

test('detectAffectedAppointments handles edge cases and invalid inputs gracefully', () => {
  assert.deepEqual(detectAffectedAppointments(null, null), [])
  assert.deepEqual(detectAffectedAppointments([], []), [])
  assert.deepEqual(detectAffectedAppointments(undefined, []), [])

  const weeklySchedules = [
    { dayOfWeek: 'WEDNESDAY', startTime: '08:30', endTime: '16:30', active: true },
  ]

  // Invalid date string appointment
  const apptInvalidDate = {
    id: 'apt-invalid',
    appointmentCode: 'APT-INVALID',
    startTime: 'invalid-date-string',
    endTime: 'invalid-date-string',
    status: 'SCHEDULED',
  }

  // Already checked in or completed appointments are not flagged for reschedule
  const apptCompleted = {
    id: 'apt-done',
    appointmentCode: 'APT-DONE',
    startTime: '2026-09-02T17:00:00+07:00', // Wednesday after 16:30
    endTime: '2026-09-02T17:30:00+07:00',
    status: 'COMPLETED',
  }

  // Appointment starts too early (08:00 vs doctor starts at 08:30)
  const apptEarly = {
    id: 'apt-early',
    appointmentCode: 'APT-EARLY',
    startTime: '2026-09-02T08:00:00+07:00', // Wednesday 08:00
    endTime: '2026-09-02T08:30:00+07:00',
    status: 'SCHEDULED',
  }

  // Appointment ends too late (ends 17:00 vs doctor ends at 16:30)
  const apptLate = {
    id: 'apt-late',
    appointmentCode: 'APT-LATE',
    startTime: '2026-09-02T16:00:00+07:00', // Wednesday 16:00
    endTime: '2026-09-02T17:00:00+07:00',
    status: 'CONFIRMED',
  }

  const result = detectAffectedAppointments(
    [apptInvalidDate, apptCompleted, apptEarly, apptLate],
    weeklySchedules
  )

  assert.equal(result.length, 2)
  assert.equal(result[0].appointmentCode, 'APT-EARLY')
  assert.equal(result[1].appointmentCode, 'APT-LATE')
})


