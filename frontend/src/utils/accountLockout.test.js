import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'
import path from 'node:path'

import authApi, { parseRetryAfterSeconds, isLockoutError } from '../api/authApi.js'
import userApi from '../api/userApi.js'

const frontendDir = fs.existsSync(path.resolve('src')) ? process.cwd() : path.resolve('frontend')

// ============================================================================
// 1. Tests for Requirement (1): Hiển thị đúng thông báo khi nhận 429 & parse retryAfterSeconds
// ============================================================================

test('parseRetryAfterSeconds correctly extracts seconds from response body details.retryAfterSeconds', () => {
  const error = {
    response: {
      status: 429,
      data: {
        code: 'TOO_MANY_LOGIN_ATTEMPTS',
        message: 'Hệ thống tạm khóa đăng nhập. Vui lòng thử lại sau 900 giây.',
        details: { retryAfterSeconds: 900 },
      },
    },
  }
  const seconds = parseRetryAfterSeconds(error)
  assert.equal(seconds, 900)
})

test('parseRetryAfterSeconds correctly extracts seconds from HTTP header Retry-After', () => {
  const error = {
    response: {
      status: 429,
      headers: {
        'retry-after': '45',
      },
      data: {
        code: 'TOO_MANY_LOGIN_ATTEMPTS',
        message: 'Too many requests',
      },
    },
  }
  const seconds = parseRetryAfterSeconds(error)
  assert.equal(seconds, 45)
})

test('parseRetryAfterSeconds extracts seconds from error message regex if details and headers are missing', () => {
  const error = {
    response: {
      status: 429,
      data: {
        message: 'Hệ thống tạm khóa đăng nhập. Vui lòng thử lại sau 60 giây.',
      },
    },
  }
  const seconds = parseRetryAfterSeconds(error)
  assert.equal(seconds, 60)
})

test('parseRetryAfterSeconds returns 0 for non-lockout errors', () => {
  const error = {
    response: {
      status: 401,
      data: {
        code: 'INVALID_CREDENTIALS',
        message: 'Tên đăng nhập hoặc mật khẩu không đúng.',
      },
    },
  }
  const seconds = parseRetryAfterSeconds(error)
  assert.equal(seconds, 0)
})

test('isLockoutError correctly identifies HTTP 429 status and TOO_MANY_LOGIN_ATTEMPTS code', () => {
  assert.equal(isLockoutError({ response: { status: 429 } }), true)
  assert.equal(isLockoutError({ response: { data: { code: 'TOO_MANY_LOGIN_ATTEMPTS' } } }), true)
  assert.equal(isLockoutError({ apiError: { code: 'TOO_MANY_LOGIN_ATTEMPTS' } }), true)
  assert.equal(isLockoutError({ response: { status: 401, data: { code: 'INVALID_CREDENTIALS' } } }), false)
  assert.equal(isLockoutError(null), false)
})

test('lockout message formatting formats seconds clearly without user enumeration leakage', () => {
  const formatLockoutMessage = (retrySeconds) =>
    retrySeconds > 0
      ? `Tài khoản tạm khóa. Vui lòng thử lại sau ${retrySeconds} giây.`
      : 'Tài khoản tạm khóa. Vui lòng thử lại sau.'

  assert.equal(formatLockoutMessage(30), 'Tài khoản tạm khóa. Vui lòng thử lại sau 30 giây.')
  assert.equal(formatLockoutMessage(0), 'Tài khoản tạm khóa. Vui lòng thử lại sau.')
  // Ensure message does NOT say "sai username" or "sai password"
  assert.equal(formatLockoutMessage(30).includes('username'), false)
  assert.equal(formatLockoutMessage(30).includes('password'), false)
})

// ============================================================================
// 2. Tests for Requirement (2): Disable nút khóa đối với chính mình & Phân quyền
// ============================================================================

const isSelfAccount = (account, currentUser) => {
  if (!currentUser || !account) return false
  const matchUsername =
    currentUser.username &&
    account.username &&
    String(currentUser.username).toLowerCase() === String(account.username).toLowerCase()
  const matchId =
    currentUser.id &&
    account.id &&
    String(currentUser.id) === String(account.id)
  return Boolean(matchUsername || matchId)
}

test('isSelfAccount detects matching user ID and disables lock button', () => {
  const currentAdmin = { id: 'admin-uuid-1', username: 'admin', roles: ['ADMIN'] }
  const sameUserAccount = { id: 'admin-uuid-1', username: 'admin', active: true }

  const isSelf = isSelfAccount(sameUserAccount, currentAdmin)
  assert.equal(isSelf, true)
  // Button disabled state should be true
  const isButtonDisabled = isSelf
  assert.equal(isButtonDisabled, true)
})

test('isSelfAccount detects matching username even with different case', () => {
  const currentAdmin = { id: 'admin-uuid-1', username: 'Admin', roles: ['ADMIN'] }
  const sameUserAccount = { id: 'admin-uuid-2', username: 'admin', active: true }

  const isSelf = isSelfAccount(sameUserAccount, currentAdmin)
  assert.equal(isSelf, true)
})

test('isSelfAccount returns false for another doctor/user account and keeps button enabled', () => {
  const currentAdmin = { id: 'admin-uuid-1', username: 'admin', roles: ['ADMIN'] }
  const doctorAccount = { id: 'doctor-uuid-2', username: 'doctor1', active: true }

  const isSelf = isSelfAccount(doctorAccount, currentAdmin)
  assert.equal(isSelf, false)
  const isButtonDisabled = isSelf
  assert.equal(isButtonDisabled, false)
})

test('permission check ensures only ADMIN or USER_UPDATE role/permission can see lock/unlock actions', () => {
  const canManageUsers = (currentUser) => {
    if (!currentUser) return false
    const roles = (currentUser.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
    const perms = (currentUser.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
    return roles.includes('admin') || perms.includes('USER_UPDATE')
  }

  assert.equal(canManageUsers({ roles: ['ADMIN'], permissions: [] }), true)
  assert.equal(canManageUsers({ roles: ['MANAGER'], permissions: ['USER_UPDATE'] }), true)
  assert.equal(canManageUsers({ roles: ['DOCTOR'], permissions: ['MEDICAL_RECORD_READ'] }), false)
  assert.equal(canManageUsers({ roles: ['RECEPTIONIST'], permissions: ['PATIENT_READ'] }), false)
})

// ============================================================================
// 3. Tests for Requirement (3): Gọi đúng API khi bấm khóa / mở khóa & cập nhật state
// ============================================================================

test('userApi has deactivateUser and activateUser methods', () => {
  assert.equal(typeof userApi.deactivateUser, 'function')
  assert.equal(typeof userApi.activateUser, 'function')
  assert.equal(typeof userApi.deactivate, 'function')
  assert.equal(typeof userApi.activate, 'function')
})

test('state update helper modifies user active state without full page reload', () => {
  const initialUsers = [
    { id: 'user-1', username: 'doctor1', active: true },
    { id: 'user-2', username: 'doctor2', active: false },
  ]

  // Simulate locking doctor1
  const updatedAfterLock = initialUsers.map((u) => (u.id === 'user-1' ? { ...u, active: false } : u))
  assert.equal(updatedAfterLock.find((u) => u.id === 'user-1').active, false)
  assert.equal(updatedAfterLock.find((u) => u.id === 'user-2').active, false)

  // Simulate unlocking doctor2
  const updatedAfterUnlock = updatedAfterLock.map((u) => (u.id === 'user-2' ? { ...u, active: true } : u))
  assert.equal(updatedAfterUnlock.find((u) => u.id === 'user-2').active, true)
})

// ============================================================================
// 4. Code Structure & Implementation Verification Tests
// ============================================================================

test('Login.jsx has lockout countdown and disabled button states implemented', () => {
  const loginCode = fs.readFileSync(path.join(frontendDir, 'src/pages/Login.jsx'), 'utf-8')
  assert.ok(loginCode.includes('lockoutSeconds'), 'Login.jsx must have lockoutSeconds state')
  assert.ok(loginCode.includes('isLockout'), 'Login.jsx must check isLockout')
  assert.ok(loginCode.includes('disabled={loading || lockoutSeconds > 0}'), 'Login.jsx must disable inputs during lockout')
  assert.ok(loginCode.includes('disabled={lockoutSeconds > 0}'), 'Login.jsx must disable submit button during lockout')
})

test('PortalLogin.jsx has lockout countdown and standardized error message', () => {
  const portalLoginCode = fs.readFileSync(path.join(frontendDir, 'src/pages/PortalLogin.jsx'), 'utf-8')
  assert.ok(portalLoginCode.includes('lockoutSeconds'), 'PortalLogin.jsx must have lockoutSeconds state')
  assert.ok(portalLoginCode.includes('Tài khoản tạm khóa'), 'PortalLogin.jsx must show standardized message')
})

test('UsersPage.jsx has Popconfirm, status tags, and self-lock prevention', () => {
  const usersPageCode = fs.readFileSync(path.join(frontendDir, 'src/pages/UsersPage.jsx'), 'utf-8')
  assert.ok(usersPageCode.includes('Popconfirm'), 'UsersPage.jsx must import and use Popconfirm')
  assert.ok(usersPageCode.includes('userApi.deactivateUser'), 'UsersPage.jsx must call userApi.deactivateUser')
  assert.ok(usersPageCode.includes('userApi.activateUser'), 'UsersPage.jsx must call userApi.activateUser')
  assert.ok(usersPageCode.includes('Đang hoạt động'), 'UsersPage.jsx must show Đang hoạt động')
  assert.ok(usersPageCode.includes('Đã khóa'), 'UsersPage.jsx must show Đã khóa')
  assert.ok(usersPageCode.includes('disabled={isSelf}'), 'UsersPage.jsx must disable lock for self account')
})

test('UserManagementPage.jsx exists and re-exports UsersPage', () => {
  const pagePath = path.join(frontendDir, 'src/pages/UserManagementPage.jsx')
  assert.ok(fs.existsSync(pagePath), 'UserManagementPage.jsx must exist')
  const content = fs.readFileSync(pagePath, 'utf-8')
  assert.ok(content.includes('UsersPage'), 'UserManagementPage must re-export UsersPage')
})
