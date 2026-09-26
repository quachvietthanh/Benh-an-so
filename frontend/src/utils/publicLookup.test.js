import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getLookupErrorMessage,
  getPublicLookupStatus,
  normalizeAppointmentCode,
  validateLookupInput,
} from './publicLookup.js'

test('normalizes appointment codes before lookup', () => {
  assert.equal(normalizeAppointmentCode('  lh-1234567890  '), 'LH-1234567890')
})

test('accepts a valid appointment code and date of birth', () => {
  assert.deepEqual(validateLookupInput({
    appointmentCode: 'LH-1234567890',
    dateOfBirth: '1990-05-12',
  }), {})
})

test('rejects missing or malformed lookup input', () => {
  assert.deepEqual(validateLookupInput({ appointmentCode: '', dateOfBirth: '' }), {
    appointmentCode: 'Vui lòng nhập mã lịch hẹn.',
    dateOfBirth: 'Vui lòng chọn ngày sinh.',
  })
  assert.deepEqual(validateLookupInput({
    appointmentCode: 'mã hẹn không hợp lệ',
    dateOfBirth: '2999-01-01',
  }), {
    appointmentCode: 'Mã lịch hẹn gồm 4–20 ký tự chữ, số hoặc dấu gạch ngang.',
    dateOfBirth: 'Ngày sinh không hợp lệ.',
  })
})

test('rejects impossible calendar dates', () => {
  assert.deepEqual(validateLookupInput({
    appointmentCode: 'LH-1234567890',
    dateOfBirth: '2026-02-31',
  }), {
    dateOfBirth: 'Ngày sinh không hợp lệ.',
  })
})

test('maps every public care state to patient-facing copy', () => {
  assert.equal(getPublicLookupStatus('SCHEDULED').label, 'Đã đặt lịch')
  assert.equal(getPublicLookupStatus('IN_PROGRESS').label, 'Đang tiếp nhận')
  assert.equal(getPublicLookupStatus('COMPLETED').label, 'Đã hoàn tất')
  assert.equal(getPublicLookupStatus('UNAVAILABLE').label, 'Không còn hiệu lực')
  assert.equal(getPublicLookupStatus('UNKNOWN').label, 'Chưa xác định')
})

test('maps lookup failures without exposing whether a code exists', () => {
  assert.equal(
    getLookupErrorMessage(404),
    getLookupErrorMessage(422),
  )
  assert.match(getLookupErrorMessage(429), /quá nhiều lần/)
  assert.match(getLookupErrorMessage(500), /tạm thời gián đoạn/)
  assert.match(getLookupErrorMessage(undefined), /Không thể kết nối/)
})
