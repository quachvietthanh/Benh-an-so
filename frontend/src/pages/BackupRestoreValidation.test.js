import test from 'node:test'
import assert from 'node:assert/strict'

// Helper validation logic for Backup & Restore module

export function validateBackupCreationAccess(userRoles = []) {
  const normalized = userRoles.map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isAdmin = normalized.includes('admin')

  if (!isAdmin) {
    return { allowed: false, error: 'Bạn không có quyền thực hiện tạo bản sao lưu. Chức năng chỉ dành cho Quản trị viên (ADMIN).' }
  }
  return { allowed: true }
}

export function validateBackupRestoreEligibility(backup, userRoles = []) {
  const normalized = userRoles.map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const isAdmin = normalized.includes('admin')

  if (!isAdmin) {
    return { eligible: false, error: 'Bạn không có quyền phục hồi dữ liệu hệ thống (403 Forbidden).' }
  }

  if (!backup || !backup.id) {
    return { eligible: false, error: 'Bản sao lưu không tồn tại hoặc mã bản sao không hợp lệ.' }
  }

  const status = String(backup.status || 'COMPLETED').toUpperCase()
  if (status === 'PROCESSING' || status === 'PENDING') {
    return { eligible: false, error: 'Bản sao lưu đang trong quá trình xử lý, không thể phục hồi.' }
  }

  if (status === 'FAILED' || status === 'ERROR') {
    return { eligible: false, error: 'Bản sao lưu ở trạng thái thất bại, không thể phục hồi.' }
  }

  return { eligible: true }
}

// ==========================================
// TEST SUITE: BACKUP & RESTORE VALIDATION
// ==========================================

test('TC-BCK-01: Cho phép Quản trị viên (ADMIN) tạo bản sao lưu', () => {
  const result = validateBackupCreationAccess(['ADMIN'])
  assert.strictEqual(result.allowed, true)
})

test('TC-BCK-02: Chặn các role khác (RECEPTIONIST, DOCTOR, MANAGER) tạo bản sao lưu', () => {
  const doctorResult = validateBackupCreationAccess(['DOCTOR'])
  assert.strictEqual(doctorResult.allowed, false)
  assert.match(doctorResult.error, /chỉ dành cho Quản trị viên/)

  const receptionistResult = validateBackupCreationAccess(['RECEPTIONIST'])
  assert.strictEqual(receptionistResult.allowed, false)

  const managerResult = validateBackupCreationAccess(['MANAGER'])
  assert.strictEqual(managerResult.allowed, false)
})

test('TC-BCK-03: Cho phép Phục hồi đối với bản sao lưu COMPLETED khi đăng nhập ADMIN', () => {
  const backup = { id: 'bck-001', status: 'COMPLETED' }
  const result = validateBackupRestoreEligibility(backup, ['ADMIN'])
  assert.strictEqual(result.eligible, true)
})

test('TC-BCK-04: Chặn Phục hồi đối với bản sao lưu đang PROCESSING hoặc PENDING', () => {
  const backup = { id: 'bck-002', status: 'PROCESSING' }
  const result = validateBackupRestoreEligibility(backup, ['ADMIN'])
  assert.strictEqual(result.eligible, false)
  assert.match(result.error, /đang trong quá trình xử lý/)
})

test('TC-BCK-05: Chặn Phục hồi đối với bản sao lưu FAILED', () => {
  const backup = { id: 'bck-003', status: 'FAILED' }
  const result = validateBackupRestoreEligibility(backup, ['ADMIN'])
  assert.strictEqual(result.eligible, false)
  assert.match(result.error, /trạng thái thất bại/)
})

test('TC-BCK-06: Chặn Phục hồi đối với backupId không tồn tại hoặc rỗng', () => {
  const result = validateBackupRestoreEligibility(null, ['ADMIN'])
  assert.strictEqual(result.eligible, false)
  assert.match(result.error, /không tồn tại/)
})
