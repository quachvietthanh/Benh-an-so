import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getNoShowDeadline,
  getOverdueMinutes,
  isAppointmentOverdue,
  NO_SHOW_GRACE_MINUTES,
} from './appointmentTiming.js'

const scheduledAppointment = {
  status: 'SCHEDULED',
  appointmentAt: '2026-07-23T02:00:00.000Z',
}

test('sets the no-show deadline 15 minutes after the appointment', () => {
  assert.equal(NO_SHOW_GRACE_MINUTES, 15)
  assert.equal(
    getNoShowDeadline(scheduledAppointment.appointmentAt).toISOString(),
    '2026-07-23T02:15:00.000Z',
  )
})

test('does not mark an appointment overdue before the exact threshold', () => {
  assert.equal(
    isAppointmentOverdue(scheduledAppointment, '2026-07-23T02:14:59.999Z'),
    false,
  )
})

test('marks an appointment overdue at and after the exact threshold', () => {
  assert.equal(
    isAppointmentOverdue(scheduledAppointment, '2026-07-23T02:15:00.000Z'),
    true,
  )
  assert.equal(
    isAppointmentOverdue(scheduledAppointment, '2026-07-23T02:16:00.000Z'),
    true,
  )
  assert.equal(
    getOverdueMinutes(scheduledAppointment, '2026-07-23T02:16:00.000Z'),
    16,
  )
})

test('only a valid scheduled appointment can be overdue', () => {
  assert.equal(
    isAppointmentOverdue({ ...scheduledAppointment, status: 'NO_SHOW' }, '2026-07-23T03:00:00.000Z'),
    false,
  )
  assert.equal(
    isAppointmentOverdue({ status: 'SCHEDULED', appointmentAt: 'invalid' }, '2026-07-23T03:00:00.000Z'),
    false,
  )
})
