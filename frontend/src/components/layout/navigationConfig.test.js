import assert from 'node:assert/strict'
import test from 'node:test'
import { getNavigationItems } from './navigationConfig.js'

test('getNavigationItems - excludes /doctor-schedules for irrelevant roles like receptionist and pharmacist', () => {
  // 1. Receptionist without schedule perms
  const receptionNav = getNavigationItems(['ROLE_RECEPTIONIST'], ['PATIENT_READ', 'APPOINTMENT_READ'])
  const hasDoctorScheduleRecep = receptionNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleRecep, false, 'Receptionist must not see doctor schedule menu item')

  // 2. Receptionist even if DB grants DOCTOR_SCHEDULE_READ permission
  const receptionNavWithRead = getNavigationItems(['ROLE_RECEPTIONIST'], ['PATIENT_READ', 'APPOINTMENT_READ', 'DOCTOR_SCHEDULE_READ'])
  const hasDoctorScheduleRecep2 = receptionNavWithRead.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleRecep2, false, 'Receptionist must not see doctor schedule menu even with DOCTOR_SCHEDULE_READ')

  // 3. Pharmacist
  const pharmacistNav = getNavigationItems(['ROLE_PHARMACIST'], ['PHARMACY_READ'])
  const hasDoctorSchedulePharm = pharmacistNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorSchedulePharm, false, 'Pharmacist must not see doctor schedule menu item')

  // 4. Patient
  const patientNav = getNavigationItems(['ROLE_PATIENT'], [])
  const hasDoctorSchedulePatient = patientNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorSchedulePatient, false, 'Patient must not see doctor schedule menu item')

  // 5. Doctor (disabled on UI)
  const doctorNav = getNavigationItems(['ROLE_DOCTOR'], ['MEDICAL_RECORD_READ', 'DOCTOR_TIMEOFF_CREATE'])
  const hasDoctorScheduleDoc = doctorNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleDoc, false, 'Doctor must not see doctor schedule menu item')
})

test('getNavigationItems - includes /doctor-schedules for manager and admin', () => {
  // 1. Admin
  const adminNav = getNavigationItems(['ROLE_ADMIN'], [])
  const hasDoctorScheduleAdmin = adminNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleAdmin, true, 'Admin must see doctor schedule menu item')

  // 2. Manager
  const managerNav = getNavigationItems(['ROLE_MANAGER'], [])
  const hasDoctorScheduleMgr = managerNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleMgr, true, 'Manager must see doctor schedule menu item')

  // 3. Clinic Manager
  const clinicMgrNav = getNavigationItems(['ROLE_CLINIC_MANAGER'], [])
  const hasDoctorScheduleClinicMgr = clinicMgrNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleClinicMgr, true, 'Clinic Manager must see doctor schedule menu item')
})

test('getNavigationItems - restricts /users and /system-management to admin only', () => {
  // 1. Receptionist with USER_READ permission (must NOT see /users or /system-management)
  const receptionNav = getNavigationItems(['ROLE_RECEPTIONIST'], ['PATIENT_READ', 'USER_READ', 'APPOINTMENT_READ'])
  assert.equal(receptionNav.some((item) => item.key === '/users'), false, 'Receptionist must not see /users')
  assert.equal(receptionNav.some((item) => item.key === '/system-management'), false, 'Receptionist must not see /system-management')

  // 2. Doctor (must NOT see /users or /system-management)
  const doctorNav = getNavigationItems(['ROLE_DOCTOR'], ['USER_READ', 'MEDICAL_RECORD_READ'])
  assert.equal(doctorNav.some((item) => item.key === '/users'), false, 'Doctor must not see /users')
  assert.equal(doctorNav.some((item) => item.key === '/system-management'), false, 'Doctor must not see /system-management')

  // 3. Manager (must NOT see /users or /system-management)
  const managerNav = getNavigationItems(['ROLE_MANAGER'], ['USER_READ'])
  assert.equal(managerNav.some((item) => item.key === '/users'), false, 'Manager must not see /users')
  assert.equal(managerNav.some((item) => item.key === '/system-management'), false, 'Manager must not see /system-management')

  // 4. Admin (MUST see /users and /system-management)
  const adminNav = getNavigationItems(['ROLE_ADMIN'], [])
  assert.equal(adminNav.some((item) => item.key === '/users'), true, 'Admin must see /users')
  assert.equal(adminNav.some((item) => item.key === '/system-management'), true, 'Admin must see /system-management')
})

test('getNavigationItems - weekly schedule menu and admin protection', () => {
  // 1. Lễ tân thấy mục Lịch tuần theo bác sĩ
  const recepNav = getNavigationItems(['ROLE_RECEPTIONIST'], ['PATIENT_READ', 'APPOINTMENT_READ'])
  const hasWeeklyScheduleRecep = recepNav.some((item) => item.key === '/appointments/weekly-schedule')
  assert.equal(hasWeeklyScheduleRecep, true, 'Receptionist must see /appointments/weekly-schedule in navigation')

  // 2. Lễ tân KHÔNG thấy Quản trị tài khoản và Quản trị hệ thống
  const hasUsers = recepNav.some((item) => item.key === '/users')
  const hasSystem = recepNav.some((item) => item.key === '/system-management')
  assert.equal(hasUsers, false, 'Receptionist must not see /users')
  assert.equal(hasSystem, false, 'Receptionist must not see /system-management')

  // 3. Dược sĩ KHÔNG thấy Lịch tuần theo bác sĩ
  const pharmNav = getNavigationItems(['ROLE_PHARMACIST'], ['PHARMACY_READ'])
  const hasWeeklySchedulePharm = pharmNav.some((item) => item.key === '/appointments/weekly-schedule')
  assert.equal(hasWeeklySchedulePharm, false, 'Pharmacist must not see /appointments/weekly-schedule')

  // 4. Bác sĩ KHÔNG thấy Lịch tuần theo bác sĩ
  const doctorNav = getNavigationItems(['ROLE_DOCTOR'], ['MEDICAL_RECORD_READ', 'APPOINTMENT_READ'])
  const hasWeeklyScheduleDoc = doctorNav.some((item) => item.key === '/appointments/weekly-schedule')
  assert.equal(hasWeeklyScheduleDoc, false, 'Doctor must not see /appointments/weekly-schedule')

  // 5. Quản lý phòng khám thấy Lịch tuần theo bác sĩ để điều phối
  const managerNav = getNavigationItems(['ROLE_MANAGER'], ['APPOINTMENT_READ'])
  const hasWeeklyScheduleMgr = managerNav.some((item) => item.key === '/appointments/weekly-schedule')
  assert.equal(hasWeeklyScheduleMgr, true, 'Manager must see /appointments/weekly-schedule')
})

test('getNavigationItems - Báo cáo xuất nhập tồn kho dược: hiển thị cho Pharmacist, Manager, Admin; ẩn với Doctor, Receptionist', () => {
  // 1. Dược sĩ thấy Báo cáo xuất nhập tồn
  const pharmNav = getNavigationItems(['ROLE_PHARMACIST'], ['PHARMACY_READ'])
  assert.equal(pharmNav.some((item) => item.key === '/inventory/stock-report'), true, 'Pharmacist must see /inventory/stock-report')

  // 2. Quản lý thấy Báo cáo xuất nhập tồn
  const mgrNav = getNavigationItems(['ROLE_MANAGER'], [])
  assert.equal(mgrNav.some((item) => item.key === '/inventory/stock-report'), true, 'Manager must see /inventory/stock-report')

  // 3. Admin thấy Báo cáo xuất nhập tồn
  const adminNav = getNavigationItems(['ROLE_ADMIN'], [])
  assert.equal(adminNav.some((item) => item.key === '/inventory/stock-report'), true, 'Admin must see /inventory/stock-report')

  // 4. Bác sĩ KHÔNG thấy Báo cáo xuất nhập tồn
  const docNav = getNavigationItems(['ROLE_DOCTOR'], ['MEDICAL_RECORD_READ'])
  assert.equal(docNav.some((item) => item.key === '/inventory/stock-report'), false, 'Doctor must not see /inventory/stock-report')

  // 5. Lễ tân KHÔNG thấy Báo cáo xuất nhập tồn
  const recepNav = getNavigationItems(['ROLE_RECEPTIONIST'], ['PATIENT_READ'])
  assert.equal(recepNav.some((item) => item.key === '/inventory/stock-report'), false, 'Receptionist must not see /inventory/stock-report')

  // 6. Quyền INVENTORY_REPORT_VIEW
  const permNav = getNavigationItems([], ['INVENTORY_REPORT_VIEW'])
  assert.equal(permNav.some((item) => item.key === '/inventory/stock-report'), true, 'User with INVENTORY_REPORT_VIEW must see /inventory/stock-report')
})

