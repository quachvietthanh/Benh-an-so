import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * 1. Helper kiểm tra quyền xem bệnh án theo Vai trò người dùng (Role Authorization)
 */
export const canViewMedicalRecord = (roles = []) => {
  const allowedRoles = ['admin', 'doctor', 'nurse', 'role_admin', 'role_doctor', 'role_nurse']
  return roles.some((role) => allowedRoles.includes(String(role).toLowerCase()))
}

/**
 * 2. Helper định dạng & ghi nhật ký truy cập hồ sơ bệnh án (Audit Log / Access Logging)
 */
export const createAccessLog = ({ userName, patientName, recordCode, action = 'Xem thông tin hồ sơ bệnh án điện tử' }) => {
  if (!userName || !recordCode) {
    throw new Error('Thiếu thông tin người truy cập hoặc mã bệnh án')
  }
  return {
    id: `log-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
    userName,
    patientName: patientName || 'Bệnh nhân',
    recordCode,
    action,
    timestamp: new Date().toISOString(),
  }
}

/**
 * 3. Helper kiểm tra tính toàn vẹn thông tin chi tiết bệnh án khi xem
 */
export const validateMedicalRecordDetail = (record) => {
  const errors = []
  if (!record?.medicalRecordId && !record?.id) errors.push('Thiếu medicalRecordId')
  if (!record?.visitId && !record?.visit?.id) errors.push('Thiếu visitId')
  if (!record?.patientName && !record?.patientId && !record?.patient?.id) errors.push('Thiếu thông tin bệnh nhân')
  if (!record?.diagnosis && !record?.primaryIcdCode && !record?.diagnoses?.length) errors.push('Thiếu thông tin chẩn đoán ICD-10')

  return {
    isValid: errors.length === 0,
    errors,
  }
}

// =========================================================================
// --- BỘ KIỂM THỬ TỰ ĐỘNG KIỂM THỬ XEM BỆNH ÁN VÀ GHI NHẬT KÝ (AUTOMATED SUITE) ---
// =========================================================================

test('1. KIỂM THỬ QUYỀN XEM BỆNH ÁN (Role-Based Authorization Test)', () => {
  // TH 1.1: Bác sĩ, Quản trị viên và Điều dưỡng CÓ QUYỀN xem bệnh án
  assert.equal(canViewMedicalRecord(['doctor']), true)
  assert.equal(canViewMedicalRecord(['ADMIN']), true)
  assert.equal(canViewMedicalRecord(['Nurse']), true)
  assert.equal(canViewMedicalRecord(['ROLE_DOCTOR']), true)

  // TH 1.2: Vai trò không đủ thẩm quyền -> BỊ TỪ CHỐI xem
  assert.equal(canViewMedicalRecord(['pharmacist']), false)
  assert.equal(canViewMedicalRecord(['guest']), false)
  assert.equal(canViewMedicalRecord([]), false)
})

test('2. KIỂM THỬ HIỂN THỊ NỘI DUNG CHI TIẾT BỆNH ÁN (Record Content Integrity)', () => {
  const sampleRecord = {
    medicalRecordId: 'mr-101',
    visitId: 'visit-101',
    patientName: 'Nguyễn Văn An (BN000001)',
    doctorName: 'BS. Phạm Hồng Anh',
    symptoms: 'Đau ngực, ho kéo dài',
    diagnosis: '[J00] Viêm mũi họng cấp (Kèm theo: [K29] Viêm dạ dày)',
    primaryIcdCode: 'J00',
    vitalSigns: { bp: '120/80', temp: '37.0', pulse: '75' },
    clinicalOrders: ['Công thức máu 18 chỉ số (Thường)'],
    attachments: [{ id: 'att-1', fileName: 'ket_qua_xet_nghiem.pdf' }],
  }

  const result = validateMedicalRecordDetail(sampleRecord)
  assert.equal(result.isValid, true)
  assert.equal(result.errors.length, 0)
  assert.equal(sampleRecord.medicalRecordId, 'mr-101')
  assert.equal(sampleRecord.attachments.length, 1)
})

test('3. KIỂM THỬ TỰ ĐỘNG GHI NHẬT KÝ TRUY CẬP HỒ SƠ (Audit Log Recording Test)', () => {
  const logEntry = createAccessLog({
    userName: 'BS. Phạm Hồng Anh',
    patientName: 'Nguyễn Văn An',
    recordCode: 'BA-20260731',
    action: 'Xem thông tin hồ sơ bệnh án điện tử',
  })

  // Kiểm tra thông tin nhật ký ghi nhận đầy đủ
  assert.ok(logEntry.id.startsWith('log-'))
  assert.equal(logEntry.userName, 'BS. Phạm Hồng Anh')
  assert.equal(logEntry.patientName, 'Nguyễn Văn An')
  assert.equal(logEntry.recordCode, 'BA-20260731')
  assert.equal(logEntry.action, 'Xem thông tin hồ sơ bệnh án điện tử')
  assert.ok(logEntry.timestamp !== undefined)
})
