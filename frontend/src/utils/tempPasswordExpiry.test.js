import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'
import path from 'node:path'
import { DOMAIN_ERROR_MESSAGES } from './apiError.js'

const frontendDir = fs.existsSync(path.resolve('src')) ? process.cwd() : path.resolve('frontend')

// Helper reproducing calculation logic in ResetPasswordModal
function calculateExpiresInHours(preset, customValue, customUnit) {
  if (preset !== 'custom') {
    return Number(preset)
  }
  const val = Number(customValue)
  if (!val || isNaN(val) || val < 1) return null
  return customUnit === 'days' ? val * 24 : val
}

function isCustomExpiryValid(preset, customValue, customUnit) {
  if (preset !== 'custom') return true
  const total = calculateExpiresInHours(preset, customValue, customUnit)
  return total !== null && total >= 1 && total <= 720
}

test('DOMAIN_ERROR_MESSAGES has proper Vietnamese message for TEMP_PASSWORD_EXPIRED', () => {
  assert.equal(
    DOMAIN_ERROR_MESSAGES.TEMP_PASSWORD_EXPIRED,
    'Mật khẩu tạm thời đã hết hạn. Vui lòng liên hệ Quản trị viên để được cấp lại.'
  )
})

test('calculateExpiresInHours returns correct hours for presets', () => {
  assert.equal(calculateExpiresInHours('1', 24, 'hours'), 1)
  assert.equal(calculateExpiresInHours('24', 24, 'hours'), 24)
  assert.equal(calculateExpiresInHours('72', 24, 'hours'), 72)
  assert.equal(calculateExpiresInHours('168', 24, 'hours'), 168)
})

test('calculateExpiresInHours converts days to hours correctly for custom preset', () => {
  assert.equal(calculateExpiresInHours('custom', 1, 'days'), 24)
  assert.equal(calculateExpiresInHours('custom', 3, 'days'), 72)
  assert.equal(calculateExpiresInHours('custom', 7, 'days'), 168)
  assert.equal(calculateExpiresInHours('custom', 30, 'days'), 720)
  assert.equal(calculateExpiresInHours('custom', 12, 'hours'), 12)
})

test('isCustomExpiryValid validates 1 <= hours <= 720', () => {
  // Valid
  assert.equal(isCustomExpiryValid('24', 24, 'hours'), true)
  assert.equal(isCustomExpiryValid('custom', 1, 'hours'), true)
  assert.equal(isCustomExpiryValid('custom', 720, 'hours'), true)
  assert.equal(isCustomExpiryValid('custom', 30, 'days'), true)

  // Invalid
  assert.equal(isCustomExpiryValid('custom', 0, 'hours'), false)
  assert.equal(isCustomExpiryValid('custom', -5, 'hours'), false)
  assert.equal(isCustomExpiryValid('custom', null, 'hours'), false)
  assert.equal(isCustomExpiryValid('custom', 721, 'hours'), false)
  assert.equal(isCustomExpiryValid('custom', 31, 'days'), false) // 31 * 24 = 744 > 720
})

test('ResetPasswordModal contains expiration UI elements and payload handling', () => {
  const modalCode = fs.readFileSync(
    path.join(frontendDir, 'src/components/users/ResetPasswordModal.jsx'),
    'utf-8'
  )
  assert.ok(modalCode.includes('Thời gian hiệu lực'), 'Modal must have "Thời gian hiệu lực" section')
  assert.ok(modalCode.includes('expiresInHours'), 'Modal must send expiresInHours')
  assert.ok(modalCode.includes('tempPasswordExpiresAt'), 'Modal must receive and display tempPasswordExpiresAt')
  assert.ok(modalCode.includes('Tùy chỉnh'), 'Modal must offer custom expiration option')
  assert.ok(
    modalCode.includes('Sau khi hết hạn, nhân viên sẽ không thể đăng nhập'),
    'Modal must display warning about expiration'
  )
})

test('Login.jsx and AuthContext.jsx specifically handle TEMP_PASSWORD_EXPIRED', () => {
  const loginCode = fs.readFileSync(path.join(frontendDir, 'src/pages/Login.jsx'), 'utf-8')
  assert.ok(loginCode.includes('TEMP_PASSWORD_EXPIRED'), 'Login.jsx must check TEMP_PASSWORD_EXPIRED')
  assert.ok(loginCode.includes('Mật khẩu tạm thời đã hết hạn'), 'Login.jsx must display expired message')

  const authCode = fs.readFileSync(path.join(frontendDir, 'src/context/AuthContext.jsx'), 'utf-8')
  assert.ok(authCode.includes('TEMP_PASSWORD_EXPIRED'), 'AuthContext.jsx must handle TEMP_PASSWORD_EXPIRED')
  assert.ok(authCode.includes('isTempPasswordExpired'), 'AuthContext.jsx must return isTempPasswordExpired flag')
})
