import dayjs from 'dayjs'

export const NO_SHOW_GRACE_MINUTES = 15

export const getNoShowDeadline = (appointmentAt) => {
  const value = dayjs(appointmentAt)
  return value.isValid() ? value.add(NO_SHOW_GRACE_MINUTES, 'minute') : null
}

export const isAppointmentOverdue = (appointment, now = dayjs()) => {
  if (!appointment || appointment.status !== 'SCHEDULED') return false

  const deadline = getNoShowDeadline(appointment.appointmentAt)
  const currentTime = dayjs(now)
  return Boolean(deadline && currentTime.isValid() && !currentTime.isBefore(deadline))
}

export const getOverdueMinutes = (appointment, now = dayjs()) => {
  if (!isAppointmentOverdue(appointment, now)) return 0

  return Math.max(
    NO_SHOW_GRACE_MINUTES,
    dayjs(now).diff(dayjs(appointment.appointmentAt), 'minute'),
  )
}
