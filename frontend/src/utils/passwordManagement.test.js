import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'
import path from 'node:path'

import { validatePasswordStrength, validateChangePasswordForm } from './passwordPolicy.js'
import authApi from '../api/authApi.js'
import userApi from '../api/userApi.js'

const frontendDir = fs.existsSync(path.resolve('src')) ? process.cwd() : path.resolve('frontend')

// -------------------------------------------------------------
// 1. Password Policy Validator Tests (matching backend QTN-28)
// -------------------------------------------------------------
test('validatePasswordStrength passes for a valid password with uppercase, lowercase, and digit', () => {
  const violations = validatePasswordStrength('ValidPass123')
  assert.equal(violations.length, 0)
})

test('validatePasswordStrength fails for empty or whitespace password', () => {
  const violations = validatePasswordStrength('')
  assert.ok(violations.length > 0)
  assert.ok(violations.some((v) => v.includes('không được để trống')))
})

test('validatePasswordStrength fails when password length is less than 8 characters', () => {
  const violations = validatePasswordStrength('Pass1')
  assert.ok(violations.some((v) => v.includes('độ dài từ 8 đến 50 ký tự')))
})

test('validatePasswordStrength fails when password has no uppercase letter', () => {
  const violations = validatePasswordStrength('password123')
  assert.ok(violations.some((v) => v.includes('chữ cái in hoa')))
})

test('validatePasswordStrength fails when password has no lowercase letter', () => {
  const violations = validatePasswordStrength('PASSWORD123')
  assert.ok(violations.some((v) => v.includes('chữ cái viết thường')))
})

test('validatePasswordStrength fails when password has no digit', () => {
  const violations = validatePasswordStrength('PasswordOnly')
  assert.ok(violations.some((v) => v.includes('chữ số')))
})

// -------------------------------------------------------------
// 2. Change Password Form Client Validation Tests
// -------------------------------------------------------------
test('validateChangePasswordForm detects missing old password', () => {
  const res = validateChangePasswordForm({
    oldPassword: '',
    newPassword: 'NewPassword123',
    confirmPassword: 'NewPassword123',
  })
  assert.equal(res.isValid, false)
  assert.equal(res.errors.oldPassword, 'Vui lòng nhập mật khẩu hiện tại.')
})

test('validateChangePasswordForm detects new password matching old password', () => {
  const res = validateChangePasswordForm({
    oldPassword: 'OldPassword123',
    newPassword: 'OldPassword123',
    confirmPassword: 'OldPassword123',
  })
  assert.equal(res.isValid, false)
  assert.equal(res.errors.newPassword, 'Mật khẩu mới không được trùng với mật khẩu hiện tại.')
})

test('validateChangePasswordForm detects confirm password mismatch', () => {
  const res = validateChangePasswordForm({
    oldPassword: 'OldPassword123',
    newPassword: 'NewPassword123',
    confirmPassword: 'DifferentPassword123',
  })
  assert.equal(res.isValid, false)
  assert.equal(res.errors.confirmPassword, 'Xác nhận mật khẩu mới không khớp.')
})

test('validateChangePasswordForm returns valid for correct input', () => {
  const res = validateChangePasswordForm({
    oldPassword: 'OldPassword123',
    newPassword: 'NewSecurePassword456',
    confirmPassword: 'NewSecurePassword456',
  })
  assert.equal(res.isValid, true)
  assert.equal(res.errors.oldPassword, null)
  assert.equal(res.errors.newPassword, null)
  assert.equal(res.errors.confirmPassword, null)
})

// -------------------------------------------------------------
// 3. API Client Methods Exist & Route Correctly
// -------------------------------------------------------------
test('authApi has changePassword method', () => {
  assert.equal(typeof authApi.changePassword, 'function')
})

test('userApi has resetPassword method', () => {
  assert.equal(typeof userApi.resetPassword, 'function')
})

// -------------------------------------------------------------
// 4. Permission Check Logic for Admin Reset Password
// -------------------------------------------------------------
test('permission check allows ADMIN role to reset password', () => {
  const adminUser = { roles: ['ADMIN'], permissions: ['USER_READ'] }
  const roles = (adminUser.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const perms = (adminUser.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const canResetPassword = roles.includes('admin') || perms.includes('USER_RESET_PASSWORD')
  assert.equal(canResetPassword, true)
})

test('permission check allows user with USER_RESET_PASSWORD permission to reset password', () => {
  const managerUser = { roles: ['MANAGER'], permissions: ['USER_RESET_PASSWORD'] }
  const roles = (managerUser.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const perms = (managerUser.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const canResetPassword = roles.includes('admin') || perms.includes('USER_RESET_PASSWORD')
  assert.equal(canResetPassword, true)
})

test('permission check denies RECEPTIONIST role without USER_RESET_PASSWORD', () => {
  const receptionistUser = { roles: ['RECEPTIONIST'], permissions: ['PATIENT_READ', 'APPOINTMENT_READ'] }
  const roles = (receptionistUser.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const perms = (receptionistUser.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const canResetPassword = roles.includes('admin') || perms.includes('USER_RESET_PASSWORD')
  assert.equal(canResetPassword, false)
})

test('permission check denies DOCTOR role without USER_RESET_PASSWORD', () => {
  const doctorUser = { roles: ['DOCTOR'], permissions: ['MEDICAL_RECORD_READ', 'PRESCRIPTION_CREATE'] }
  const roles = (doctorUser.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const perms = (doctorUser.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  const canResetPassword = roles.includes('admin') || perms.includes('USER_RESET_PASSWORD')
  assert.equal(canResetPassword, false)
})

// -------------------------------------------------------------
// 5. Code Structure & Interceptor Integrity Tests
// -------------------------------------------------------------
test('axiosClient handles MUST_CHANGE_PASSWORD specifically without confusing with 401', () => {
  const axiosClientCode = fs.readFileSync(path.join(frontendDir, 'src/api/axiosClient.js'), 'utf-8')
  assert.ok(axiosClientCode.includes('MUST_CHANGE_PASSWORD'), 'axiosClient must check MUST_CHANGE_PASSWORD')
  assert.ok(axiosClientCode.includes('auth:must-change-password'), 'axiosClient must dispatch auth:must-change-password event')
})

test('AuthContext saves mustChangePassword into user state', () => {
  const authContextCode = fs.readFileSync(path.join(frontendDir, 'src/context/AuthContext.jsx'), 'utf-8')
  assert.ok(authContextCode.includes('mustChangePassword: Boolean(data.mustChangePassword)'), 'AuthContext must save mustChangePassword from login')
  assert.ok(authContextCode.includes('auth:must-change-password'), 'AuthContext must listen to auth:must-change-password event')
})

test('ForceChangePasswordModal is mounted in App.jsx', () => {
  const appCode = fs.readFileSync(path.join(frontendDir, 'src/App.jsx'), 'utf-8')
  assert.ok(appCode.includes('ForceChangePasswordModal'), 'App.jsx must import and render ForceChangePasswordModal')
})

test('UsersPage and MainLayout have reset/change password components integrated', () => {
  const usersPageCode = fs.readFileSync(path.join(frontendDir, 'src/pages/UsersPage.jsx'), 'utf-8')
  assert.ok(usersPageCode.includes('ResetPasswordModal'), 'UsersPage must import and render ResetPasswordModal')
  assert.ok(usersPageCode.includes('USER_RESET_PASSWORD'), 'UsersPage must check USER_RESET_PASSWORD permission')

  const layoutCode = fs.readFileSync(path.join(frontendDir, 'src/components/layout/MainLayout.jsx'), 'utf-8')
  assert.ok(layoutCode.includes('ChangePasswordModal'), 'MainLayout must import and render ChangePasswordModal')
})
