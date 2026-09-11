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
})

test('getNavigationItems - includes /doctor-schedules for doctor, manager, and admin', () => {
  // 1. Doctor
  const doctorNav = getNavigationItems(['ROLE_DOCTOR'], ['MEDICAL_RECORD_READ'])
  const hasDoctorScheduleDoc = doctorNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleDoc, true, 'Doctor must see doctor schedule menu item')

  // 2. Admin
  const adminNav = getNavigationItems(['ROLE_ADMIN'], [])
  const hasDoctorScheduleAdmin = adminNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleAdmin, true, 'Admin must see doctor schedule menu item')

  // 3. Manager
  const managerNav = getNavigationItems(['ROLE_MANAGER'], [])
  const hasDoctorScheduleMgr = managerNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleMgr, true, 'Manager must see doctor schedule menu item')

  // 4. Clinic Manager
  const clinicMgrNav = getNavigationItems(['ROLE_CLINIC_MANAGER'], [])
  const hasDoctorScheduleClinicMgr = clinicMgrNav.some((item) => item.key === '/doctor-schedules')
  assert.equal(hasDoctorScheduleClinicMgr, true, 'Clinic Manager must see doctor schedule menu item')
})
