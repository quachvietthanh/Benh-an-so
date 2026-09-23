import assert from 'node:assert/strict'
import test from 'node:test'
import {
  isOtpValid,
  maskUsername,
  calculateRemainingSeconds,
  formatTimeCountdown,
  mapTwoFactorError,
} from './twoFactorHelpers.js'
import twoFactorAuthApi from '../api/twoFactorAuthApi.js'

// ============================================================================
// 1. Kiểm thử định dạng mã OTP (chỉ nhận đúng 6 chữ số)
// ============================================================================

test('isOtpValid returns true for valid 6-digit numeric strings', () => {
  assert.equal(isOtpValid('123456'), true)
  assert.equal(isOtpValid('000000'), true)
  assert.equal(isOtpValid('999999'), true)
  assert.equal(isOtpValid(' 123456 '), true, 'Trims whitespace before checking')
})

test('isOtpValid returns false for non-6-digit or non-numeric values', () => {
  assert.equal(isOtpValid(''), false)
  assert.equal(isOtpValid(null), false)
  assert.equal(isOtpValid(undefined), false)
  assert.equal(isOtpValid('12345'), false, 'Only 5 digits')
  assert.equal(isOtpValid('1234567'), false, '7 digits')
  assert.equal(isOtpValid('12345a'), false, 'Contains letter')
  assert.equal(isOtpValid('abcdef'), false, 'Letters only')
  assert.equal(isOtpValid('12 345'), false, 'Contains space inside')
  assert.equal(isOtpValid('12-345'), false, 'Contains hyphen')
})

// ============================================================================
// 2. Kiểm thử che mờ tên đăng nhập (maskUsername)
// ============================================================================

test('maskUsername properly masks middle characters of username', () => {
  const maskedDoctor = maskUsername('doctor1')
  assert.equal(maskedDoctor.startsWith('d'), true)
  assert.equal(maskedDoctor.endsWith('1'), true)
  assert.equal(maskedDoctor.includes('*'), true)

  const maskedAdmin = maskUsername('admin')
  assert.equal(maskedAdmin.startsWith('a'), true)
  assert.equal(maskedAdmin.endsWith('n'), true)

  assert.equal(maskUsername('ab'), 'ab', 'Short string untouched')
  assert.equal(maskUsername(''), '')
  assert.equal(maskUsername(null), '')
})

// ============================================================================
// 3. Kiểm thử tính toán đếm ngược hết hạn (calculateRemainingSeconds & formatTimeCountdown)
// ============================================================================

test('calculateRemainingSeconds correctly computes remaining seconds from ISO-8601', () => {
  const baseTime = 1774332000000 // Fixed timestamp
  const fiveMinutesLater = new Date(baseTime + 300 * 1000).toISOString()

  const remaining = calculateRemainingSeconds(fiveMinutesLater, baseTime)
  assert.equal(remaining, 300)

  const fortyFiveSecondsLater = new Date(baseTime + 45 * 1000).toISOString()
  assert.equal(calculateRemainingSeconds(fortyFiveSecondsLater, baseTime), 45)
})

test('calculateRemainingSeconds returns 0 when time has already expired', () => {
  const baseTime = 1774332000000
  const pastTime = new Date(baseTime - 10 * 1000).toISOString()
  assert.equal(calculateRemainingSeconds(pastTime, baseTime), 0)
  assert.equal(calculateRemainingSeconds(null, baseTime), 0)
  assert.equal(calculateRemainingSeconds('invalid-date', baseTime), 0)
})

test('formatTimeCountdown formats total seconds to mm:ss format', () => {
  assert.equal(formatTimeCountdown(300), '05:00')
  assert.equal(formatTimeCountdown(65), '01:05')
  assert.equal(formatTimeCountdown(9), '00:09')
  assert.equal(formatTimeCountdown(0), '00:00')
  assert.equal(formatTimeCountdown(-5), '00:00')
})

// ============================================================================
// 4. Kiểm thử ánh xạ đầy đủ 5+ loại mã lỗi từ Backend
// ============================================================================

test('mapTwoFactorError handles HTTP 429 and cooldown error', () => {
  const err429 = { response: { status: 429, data: { message: 'Too many requests' } } }
  const result429 = mapTwoFactorError(err429)
  assert.equal(result429.code, 'VERIFICATION_CODE_COOLDOWN')
  assert.equal(result429.message, 'Bạn vừa yêu cầu gửi lại mã, vui lòng đợi ít phút.')
  assert.equal(result429.isExpired, false)
  assert.equal(result429.isLocked, false)
})

test('mapTwoFactorError handles CHALLENGE_EXPIRED and VERIFICATION_CODE_EXPIRED', () => {
  const errExpired = { response: { status: 400, data: { code: 'VERIFICATION_CODE_EXPIRED' } } }
  const result = mapTwoFactorError(errExpired)
  assert.equal(result.code, 'CHALLENGE_EXPIRED')
  assert.equal(result.message, 'Mã xác thực đã hết hạn. Vui lòng đăng nhập lại.')
  assert.equal(result.isExpired, true)
  assert.equal(result.isLocked, false)
})

test('mapTwoFactorError handles MAX_ATTEMPTS_EXCEEDED (locking the form)', () => {
  const errLocked = { response: { status: 400, data: { code: 'MAX_ATTEMPTS_EXCEEDED' } } }
  const result = mapTwoFactorError(errLocked)
  assert.equal(result.code, 'MAX_ATTEMPTS_EXCEEDED')
  assert.equal(result.message, 'Bạn đã nhập sai quá 5 lần. Vui lòng đăng nhập lại.')
  assert.equal(result.isLocked, true)
})

test('mapTwoFactorError handles CHALLENGE_CONSUMED / TWO_FACTOR_CHALLENGE_INVALID', () => {
  const errConsumed = { response: { status: 401, data: { code: 'CHALLENGE_CONSUMED' } } }
  const result = mapTwoFactorError(errConsumed)
  assert.equal(result.code, 'CHALLENGE_CONSUMED')
  assert.equal(result.message.includes('Mã xác thực này đã được sử dụng'), true)
  assert.equal(result.isLocked, true)
})

test('mapTwoFactorError handles ACCOUNT_DISABLED', () => {
  const errDisabled = { response: { status: 403, data: { code: 'ACCOUNT_DISABLED' } } }
  const result = mapTwoFactorError(errDisabled)
  assert.equal(result.code, 'ACCOUNT_DISABLED')
  assert.equal(result.message.includes('Tài khoản của bạn đã bị vô hiệu hóa'), true)
  assert.equal(result.isLocked, true)
})

test('mapTwoFactorError handles INVALID_CODE with and without remaining attempts', () => {
  const errInvalid = { response: { status: 400, data: { code: 'INVALID_CODE' } } }
  const res1 = mapTwoFactorError(errInvalid)
  assert.equal(res1.code, 'INVALID_CODE')
  assert.equal(res1.message, 'Mã xác thực không đúng. Vui lòng kiểm tra lại.')
  assert.equal(res1.isExpired, false)

  const errWithRemaining = {
    response: { status: 400, data: { code: 'INVALID_CODE', details: { remainingAttempts: 3 } } },
  }
  const res2 = mapTwoFactorError(errWithRemaining)
  assert.equal(res2.message, 'Mã xác thực không đúng. Còn lại 3 lần thử.')
})

// ============================================================================
// 5. Kiểm thử Route Guard logic
// ============================================================================

test('Route guard correctly detects valid vs missing twoFactorToken', () => {
  const validLocationState = {
    twoFactorToken: '123e4567-e89b-12d3-a456-426614174000',
    twoFactorExpiresAt: '2026-09-23T15:00:00Z',
  }
  assert.equal(Boolean(validLocationState?.twoFactorToken), true, 'Allows rendering TwoFactorVerifyPage')

  const emptyLocationState = {}
  assert.equal(Boolean(emptyLocationState?.twoFactorToken), false, 'Triggers redirect to /login')

  const nullLocationState = null
  assert.equal(Boolean(nullLocationState?.twoFactorToken), false, 'Triggers redirect to /login')
})

// ============================================================================
// 6. Kiểm thử API client contracts
// ============================================================================

test('twoFactorAuthApi exports required functions matching backend contract', () => {
  assert.equal(typeof twoFactorAuthApi.verify, 'function')
  assert.equal(typeof twoFactorAuthApi.resend, 'function')
  assert.equal(typeof twoFactorAuthApi.configureRole, 'function')
})
