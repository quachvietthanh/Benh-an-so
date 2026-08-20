import assert from 'node:assert/strict'
import test from 'node:test'
import dayjs from 'dayjs'
import {
  buildCareLogPayload,
  buildReminderPayload,
  buildTodayPatients,
  formatVietnamDateTime,
  getAllowedStatusActions,
  getTodayVisitsForPatient,
  getVietnamDateKey,
  hasAftercarePermission,
  isUuid,
  isDoctorInstructionError,
  isTodayVisitSelectionValid,
  normalizePage,
  selectTodayCompletedVisits,
  vietnamDateTimeToIso,
  vietnamNowForPicker,
} from './aftercareHelpers.js'

const PATIENT_ID = '11111111-1111-4111-8111-111111111111'
const VISIT_ID = '22222222-2222-4222-8222-222222222222'
const REMINDER_ID = '33333333-3333-4333-8333-333333333333'
const SECOND_PATIENT_ID = '44444444-4444-4444-4444-444444444444'
const SECOND_VISIT_ID = '55555555-5555-5555-5555-555555555555'

test('converts Vietnam wall-clock time to UTC exactly once', () => {
  assert.equal(
    vietnamDateTimeToIso(dayjs('2026-09-01T09:00:00')),
    '2026-09-01T02:00:00.000Z',
  )
  assert.match(formatVietnamDateTime('2026-09-01T02:00:00.000Z'), /09:00/)
})

test('creates DatePicker defaults from the Vietnam wall clock', () => {
  const pickerValue = vietnamNowForPicker(new Date('2026-09-01T02:00:00.000Z'))
  assert.equal(pickerValue.format('YYYY-MM-DD HH:mm:ss'), '2026-09-01 09:00:00')
})

test('builds reminder DTO with UUID values and backend field names', () => {
  assert.deepEqual(buildReminderPayload({
    patientId: PATIENT_ID,
    visitId: VISIT_ID,
    followUpDate: dayjs('2026-09-10'),
    remindAt: dayjs('2026-09-01T09:00:00'),
    reminderType: 'REVISIT',
    notes: '  Nhắc mang kết quả xét nghiệm  ',
  }), {
    patientId: PATIENT_ID,
    visitId: VISIT_ID,
    appointmentId: null,
    followUpDate: '2026-09-10',
    remindAt: '2026-09-01T02:00:00.000Z',
    reminderType: 'REVISIT',
    notes: 'Nhắc mang kết quả xét nghiệm',
  })
})

test('accepts UUID identifiers and rejects display codes', () => {
  assert.equal(isUuid(PATIENT_ID), true)
  assert.equal(isUuid(VISIT_ID), true)
  assert.equal(isUuid('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), true)
  assert.equal(isUuid('d0000000-0000-0000-0000-000000000001'), true)
  assert.equal(isUuid('BN000002'), false)
  assert.equal(isUuid('VIS000007'), false)
})

test('builds care-log DTO without server-owned fields', () => {
  assert.deepEqual(buildCareLogPayload({
    patientId: PATIENT_ID,
    visitId: VISIT_ID,
    reminderId: REMINDER_ID,
    contactChannel: 'PHONE',
    contactedAt: dayjs('2026-09-01T09:00:00'),
    patientCondition: 'RECOVERING',
    contactOutcome: 'REACHED',
    careNotes: '  Bệnh nhân đỡ đau.  ',
  }), {
    patientId: PATIENT_ID,
    reminderId: REMINDER_ID,
    visitId: VISIT_ID,
    contactChannel: 'PHONE',
    contactedAt: '2026-09-01T02:00:00.000Z',
    patientCondition: 'RECOVERING',
    careNotes: 'Bệnh nhân đỡ đau.',
    contactOutcome: 'REACHED',
  })
})

test('normalizes both Spring Page and list responses without hiding API errors', () => {
  assert.deepEqual(normalizePage([], 0, 10).content, [])
  assert.equal(normalizePage([], 0, 10).totalElements, 0)
  const page = normalizePage({ content: [{ id: 1 }], totalElements: 12, number: 1, size: 10 })
  assert.equal(page.totalElements, 12)
  assert.equal(page.number, 1)
  assert.throws(() => normalizePage({}), /không đúng định dạng/)
})

test('recognizes doctor-instruction business validation', () => {
  assert.equal(isDoctorInstructionError({
    response: { data: { message: 'Cannot create reminder: Visit has no follow-up indication from doctor.' } },
  }), true)
})

test('uses JWT permissions rather than usernames or inferred frontend roles', () => {
  assert.equal(hasAftercarePermission({
    roles: ['receptionist'],
    permissions: ['FOLLOW_UP_REMINDER_CREATE'],
  }, 'FOLLOW_UP_REMINDER_CREATE'), true)
  assert.equal(hasAftercarePermission({ roles: ['receptionist'] }, 'FOLLOW_UP_REMINDER_CREATE'), false)
  assert.equal(hasAftercarePermission({
    roles: ['receptionist'],
    permissions: ['PERMISSION_CARE_LOG_READ'],
  }, 'FOLLOW_UP_REMINDER_CREATE'), false)
})

test('only exposes backend-valid reminder status transitions', () => {
  assert.deepEqual(getAllowedStatusActions('PENDING'), ['SENT', 'COMPLETED', 'CANCELLED'])
  assert.deepEqual(getAllowedStatusActions('SENT'), ['COMPLETED', 'CANCELLED'])
  assert.deepEqual(getAllowedStatusActions('COMPLETED'), [])
})

test('selects only real completed queue-backed visits for the Vietnam date', () => {
  const visits = selectTodayCompletedVisits([
    {
      patientId: PATIENT_ID,
      visitId: VISIT_ID,
      visitCode: 'VIS000001',
      status: 'COMPLETED',
      queueDate: '2026-08-20',
      checkedInAt: '2026-08-20T01:00:00.000Z',
      completedAt: '2026-08-20T02:00:00.000Z',
    },
    {
      patientId: PATIENT_ID,
      visitId: SECOND_VISIT_ID,
      status: 'WAITING',
      queueDate: '2026-08-20',
    },
    {
      patientId: SECOND_PATIENT_ID,
      visitId: SECOND_VISIT_ID,
      status: 'COMPLETED',
      queueDate: '2026-08-19',
    },
    {
      patientId: SECOND_PATIENT_ID,
      appointmentId: '66666666-6666-6666-6666-666666666666',
      status: 'COMPLETED',
      queueDate: '2026-08-20',
    },
  ], '2026-08-20')

  assert.equal(visits.length, 1)
  assert.equal(visits[0].visitId, VISIT_ID)
  assert.equal(visits[0].id, VISIT_ID)
})

test('dedupes today patients by UUID while preserving every visit', () => {
  const visits = selectTodayCompletedVisits([
    {
      patientId: PATIENT_ID,
      patientName: 'Nguyen Van A',
      visitId: VISIT_ID,
      status: 'COMPLETED',
      queueDate: '2026-08-20',
      completedAt: '2026-08-20T02:00:00.000Z',
    },
    {
      patientId: PATIENT_ID,
      patientName: 'Nguyen Van A',
      visitId: SECOND_VISIT_ID,
      status: 'COMPLETED',
      queueDate: '2026-08-20',
      completedAt: '2026-08-20T03:00:00.000Z',
    },
  ], '2026-08-20')
  const patients = buildTodayPatients(visits, [
    { id: PATIENT_ID, patientCode: 'BN000001', fullName: 'Nguyen Van A', phone: '0900000001' },
    { id: SECOND_PATIENT_ID, patientCode: 'BN999999', fullName: 'Outside source' },
  ])

  assert.equal(patients.length, 1)
  assert.equal(patients[0].id, PATIENT_ID)
  assert.equal(patients[0].patientCode, 'BN000001')
  assert.equal(patients[0].latestVisitAt, '2026-08-20T03:00:00.000Z')
  assert.equal(getTodayVisitsForPatient(visits, PATIENT_ID).length, 2)
  assert.deepEqual(getTodayVisitsForPatient(visits, PATIENT_ID).map((visit) => visit.visitId), [
    SECOND_VISIT_ID,
    VISIT_ID,
  ])
})

test('does not dedupe patients by duplicate names', () => {
  const patients = buildTodayPatients([
    { patientId: PATIENT_ID, patientName: 'Cung Ten', visitId: VISIT_ID },
    { patientId: SECOND_PATIENT_ID, patientName: 'Cung Ten', visitId: SECOND_VISIT_ID },
  ])

  assert.deepEqual(patients.map((patient) => patient.id), [PATIENT_ID, SECOND_PATIENT_ID])
})

test('keeps empty and malformed today-visit responses distinct', () => {
  assert.deepEqual(selectTodayCompletedVisits([], '2026-08-20'), [])
  assert.throws(
    () => selectTodayCompletedVisits({ content: [] }, '2026-08-20'),
    /không đúng định dạng/,
  )
})

test('computes the runtime day at the UTC to Vietnam boundary', () => {
  assert.equal(getVietnamDateKey(new Date('2026-08-19T17:00:00.000Z')), '2026-08-20')
  assert.equal(getVietnamDateKey(new Date('2026-08-20T16:59:59.999Z')), '2026-08-20')
  assert.equal(getVietnamDateKey(new Date('2026-08-20T17:00:00.000Z')), '2026-08-21')
})

test('rejects a stale visit UUID after the today source changes', () => {
  const visits = [{ patientId: PATIENT_ID, visitId: VISIT_ID }]

  assert.equal(isTodayVisitSelectionValid(visits, PATIENT_ID, VISIT_ID), true)
  assert.equal(isTodayVisitSelectionValid(visits, PATIENT_ID, SECOND_VISIT_ID), false)
  assert.equal(isTodayVisitSelectionValid([], PATIENT_ID, VISIT_ID), false)
  assert.equal(isTodayVisitSelectionValid(visits, SECOND_PATIENT_ID, VISIT_ID), false)
})
