import assert from 'node:assert/strict'
import test from 'node:test'
import {
  TIME_PREFERENCES,
  TIME_PREFERENCE_LABELS,
  TIME_PREFERENCE_OPTIONS,
  WAITLIST_STATUSES,
  getWaitlistStatusTag,
  mapWaitlistErrorMessage,
  validateAddToWaitlistForm,
  extractWaitlistSuggestion,
} from './appointmentWaitlistHelpers.js'
import appointmentWaitlistApi from '../api/appointmentWaitlistApi.js'

// ============================================================================
// 1. Kiểm thử Contract của appointmentWaitlistApi
// ============================================================================
test('TC-WAITLIST-API-01: appointmentWaitlistApi có đủ 4 phương thức theo contract', () => {
  assert.equal(typeof appointmentWaitlistApi.add, 'function')
  assert.equal(typeof appointmentWaitlistApi.list, 'function')
  assert.equal(typeof appointmentWaitlistApi.getSuggestion, 'function')
  assert.equal(typeof appointmentWaitlistApi.cancel, 'function')
})

// ============================================================================
// 2. Kiểm thử validateAddToWaitlistForm
// ============================================================================
test('TC-WAITLIST-VAL-01: Bắt lỗi khi thiếu các trường bắt buộc (patientId, doctorId, desiredDate)', () => {
  const result = validateAddToWaitlistForm('', '', '', '')
  assert.equal(result.isValid, false)
  assert.ok(result.errors.patientId)
  assert.ok(result.errors.doctorId)
  assert.ok(result.errors.desiredDate)
})

test('TC-WAITLIST-VAL-02: Bắt lỗi khi desiredDate ở quá khứ', () => {
  const today = '2026-10-20'
  const pastDate = '2026-10-19'
  const result = validateAddToWaitlistForm('pat-1', 'doc-1', pastDate, 'Ghi chú', today)
  assert.equal(result.isValid, false)
  assert.match(result.errors.desiredDate, /quá khứ/i)
})

test('TC-WAITLIST-VAL-03: Hợp lệ khi desiredDate là hôm nay hoặc tương lai', () => {
  const today = '2026-10-20'
  const futureDate = '2026-10-21'
  const resToday = validateAddToWaitlistForm('pat-1', 'doc-1', today, 'Ghi chú', today)
  assert.equal(resToday.isValid, true)
  assert.deepEqual(resToday.errors, {})

  const resFuture = validateAddToWaitlistForm('pat-1', 'doc-1', futureDate, 'Ghi chú', today)
  assert.equal(resFuture.isValid, true)
  assert.deepEqual(resFuture.errors, {})
})

test('TC-WAITLIST-VAL-04: Bắt lỗi khi note vượt quá 500 ký tự', () => {
  const longNote = 'A'.repeat(501)
  const result = validateAddToWaitlistForm('pat-1', 'doc-1', '2026-10-25', longNote, '2026-10-20')
  assert.equal(result.isValid, false)
  assert.match(result.errors.note, /500 ký tự/i)
})

test('TC-WAITLIST-VAL-05: Hợp lệ khi note <= 500 ký tự hoặc để trống', () => {
  const validNote = 'Bệnh nhân có triệu chứng đau ngực nhẹ, mong muốn được khám sớm'
  const result = validateAddToWaitlistForm('pat-1', 'doc-1', '2026-10-25', validNote, '2026-10-20')
  assert.equal(result.isValid, true)

  const emptyNoteResult = validateAddToWaitlistForm('pat-1', 'doc-1', '2026-10-25', '', '2026-10-20')
  assert.equal(emptyNoteResult.isValid, true)
})

// ============================================================================
// 3. Kiểm thử getWaitlistStatusTag cho đủ 4 trạng thái
// ============================================================================
test('TC-WAITLIST-TAG-01: Trạng thái WAITING hiển thị tag Đang chờ (màu blue)', () => {
  const tag = getWaitlistStatusTag(WAITLIST_STATUSES.WAITING)
  assert.equal(tag.label, 'Đang chờ')
  assert.equal(tag.color, 'blue')
})

test('TC-WAITLIST-TAG-02: Trạng thái SCHEDULED hiển thị tag Đã đặt lịch (màu green)', () => {
  const tag = getWaitlistStatusTag(WAITLIST_STATUSES.SCHEDULED)
  assert.equal(tag.label, 'Đã đặt lịch')
  assert.equal(tag.color, 'green')
})

test('TC-WAITLIST-TAG-03: Trạng thái CANCELLED hiển thị tag Đã hủy (màu default)', () => {
  const tag = getWaitlistStatusTag(WAITLIST_STATUSES.CANCELLED)
  assert.equal(tag.label, 'Đã hủy')
  assert.equal(tag.color, 'default')
})

test('TC-WAITLIST-TAG-04: Trạng thái EXPIRED hiển thị tag Hết hạn (màu gray)', () => {
  const tag = getWaitlistStatusTag(WAITLIST_STATUSES.EXPIRED)
  assert.equal(tag.label, 'Hết hạn')
  assert.equal(tag.color, 'gray')
})

test('TC-WAITLIST-TAG-05: Trạng thái không xác định fallback an toàn', () => {
  const tag = getWaitlistStatusTag('UNKNOWN')
  assert.equal(tag.label, 'UNKNOWN')
  assert.equal(tag.color, 'default')
})

// ============================================================================
// 4. Kiểm thử mapWaitlistErrorMessage (đặc biệt DoctorHasAvailableSlotsException)
// ============================================================================
test('TC-WAITLIST-ERR-01: DoctorHasAvailableSlotsException trả về hướng dẫn đặt lịch trực tiếp', () => {
  const errWithCode = {
    response: {
      status: 400,
      data: {
        code: 'DOCTOR_HAS_AVAILABLE_SLOTS',
        message: 'DoctorHasAvailableSlotsException: Bác sĩ vẫn còn khung giờ trống',
      },
    },
  }
  const msg1 = mapWaitlistErrorMessage(errWithCode)
  assert.equal(
    msg1,
    'Bác sĩ vẫn còn khung giờ trống trong ngày này. Vui lòng đặt lịch trực tiếp thay vì thêm vào danh sách chờ.'
  )

  const errWithString = 'DoctorHasAvailableSlotsException: Bác sĩ vẫn còn khung giờ trống trong khoảng thời gian mong muốn'
  const msg2 = mapWaitlistErrorMessage(errWithString)
  assert.equal(
    msg2,
    'Bác sĩ vẫn còn khung giờ trống trong ngày này. Vui lòng đặt lịch trực tiếp thay vì thêm vào danh sách chờ.'
  )
})

test('TC-WAITLIST-ERR-02: PatientAlreadyInWaitlistException (409) thông báo đã có tên trong danh sách chờ', () => {
  const err = {
    response: {
      status: 409,
      data: {
        code: 'PATIENT_ALREADY_IN_WAITLIST',
        message: 'Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này.',
      },
    },
  }
  const msg = mapWaitlistErrorMessage(err)
  assert.equal(msg, 'Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này.')
})

test('TC-WAITLIST-ERR-03: DoctorNotWorkingException thông báo bác sĩ không có lịch làm việc', () => {
  const err = {
    response: {
      status: 400,
      data: {
        code: 'DOCTOR_NOT_WORKING',
        message: 'DoctorNotWorkingException: Bác sĩ không có lịch làm việc vào ngày 2026-11-20.',
      },
    },
  }
  const msg = mapWaitlistErrorMessage(err)
  assert.equal(msg, 'Bác sĩ không có lịch làm việc vào ngày này.')
})

test('TC-WAITLIST-ERR-04: Lỗi ngày quá khứ hiển thị thông báo rõ ràng', () => {
  const err = {
    response: {
      status: 400,
      data: {
        message: 'ValidationException: Ngày mong muốn khám không được ở trong quá khứ.',
      },
    },
  }
  const msg = mapWaitlistErrorMessage(err)
  assert.equal(msg, 'Ngày mong muốn khám không được ở trong quá khứ.')
})

test('TC-WAITLIST-ERR-05: Lỗi 403 Forbidden hiển thị thông báo phân quyền', () => {
  const err = {
    response: {
      status: 403,
      data: {},
    },
  }
  const msg = mapWaitlistErrorMessage(err)
  assert.equal(msg, 'Bạn không có quyền thực hiện thao tác trên danh sách chờ.')
})

// ============================================================================
// 5. Kiểm thử xử lý 204 No Content cho extractWaitlistSuggestion (không throw lỗi)
// ============================================================================
test('TC-WAITLIST-204-01: Status 204 trả về null đại diện không có gợi ý, không phải lỗi', () => {
  const response204 = {
    status: 204,
    data: '',
  }
  const result = extractWaitlistSuggestion(response204)
  assert.equal(result, null)
})

test('TC-WAITLIST-204-02: Status 200 trả về dữ liệu bệnh nhân chờ gợi ý', () => {
  const suggestionData = {
    waitlistId: 'wl-123',
    patientId: 'pat-456',
    patientName: 'Trần Văn Một',
    patientPhone: '0911111111',
    desiredDate: '2026-11-20',
    timePreference: 'ANYTIME',
    note: 'Khám tiêu hóa',
    createdAt: '2026-11-01T08:00:00Z',
  }
  const response200 = {
    status: 200,
    data: suggestionData,
  }
  const result = extractWaitlistSuggestion(response200)
  assert.deepEqual(result, suggestionData)
  assert.equal(result.patientName, 'Trần Văn Một')
  assert.equal(result.patientPhone, '0911111111')
})

test('TC-WAITLIST-204-03: Null hoặc undefined response trả về null an toàn', () => {
  assert.equal(extractWaitlistSuggestion(null), null)
  assert.equal(extractWaitlistSuggestion(undefined), null)
})

// ============================================================================
// 6. Kiểm thử Khung thời gian mong muốn (TimePreference)
// ============================================================================
test('TC-WAITLIST-TIME-01: TIME_PREFERENCE_OPTIONS đủ 3 lựa chọn chuẩn', () => {
  assert.equal(TIME_PREFERENCE_OPTIONS.length, 3)
  assert.deepEqual(
    TIME_PREFERENCE_OPTIONS.map((o) => o.value),
    ['ANYTIME', 'MORNING', 'AFTERNOON']
  )
  assert.equal(TIME_PREFERENCE_LABELS.ANYTIME, 'Bất kỳ lúc nào')
  assert.equal(TIME_PREFERENCE_LABELS.MORNING, 'Buổi sáng')
  assert.equal(TIME_PREFERENCE_LABELS.AFTERNOON, 'Buổi chiều')
})
