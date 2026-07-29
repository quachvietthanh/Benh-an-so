import dayjs from 'dayjs'

export const NO_SHOW_GRACE_MINUTES = 15

export const getAppointmentDateTime = (appointment) => {
  if (!appointment) return null
  if (appointment.appointmentAt) {
    const parsed = dayjs(appointment.appointmentAt)
    if (parsed.isValid() && String(appointment.appointmentAt).includes('T')) {
      return parsed
    }
  }

  let dateStr = dayjs().format('YYYY-MM-DD')
  if (appointment.date) {
    dateStr = dayjs(appointment.date).format('YYYY-MM-DD')
  } else if (appointment.appointmentAt) {
    const parsedDate = dayjs(appointment.appointmentAt)
    if (parsedDate.isValid()) dateStr = parsedDate.format('YYYY-MM-DD')
  }

  let timeStr = '08:45'
  if (appointment.slot) {
    const parts = String(appointment.slot).split('-')
    timeStr = parts[0].trim()
  } else if (appointment.startTime) {
    timeStr = dayjs(appointment.startTime).format('HH:mm')
  } else if (appointment.appointmentAt && String(appointment.appointmentAt).includes('T')) {
    timeStr = dayjs(appointment.appointmentAt).format('HH:mm')
  }

  if (timeStr.length === 5) {
    timeStr = `${timeStr}:00`
  }

  const combined = dayjs(`${dateStr}T${timeStr}`)
  return combined.isValid() ? combined : null
}

export const getNoShowDeadline = (appointmentAt) => {
  const value = dayjs(appointmentAt)
  return value.isValid() ? value.add(NO_SHOW_GRACE_MINUTES, 'minute') : null
}

export const isAppointmentPast15Mins = (appointment, now = dayjs()) => {
  if (!appointment) return false
  const activeStatuses = ['SCHEDULED', 'CHECKED_IN', 'WAITING']
  if (!activeStatuses.includes(appointment.status)) return false

  const appTime = getAppointmentDateTime(appointment)
  if (!appTime) return false

  const diffMinutes = dayjs(now).diff(appTime, 'minute')
  return diffMinutes >= NO_SHOW_GRACE_MINUTES
}

export const isAppointmentOverdue = (appointment, now = dayjs()) => {
  return isAppointmentPast15Mins(appointment, now)
}

export const getOverdueMinutes = (appointment, now = dayjs()) => {
  const appTime = getAppointmentDateTime(appointment)
  if (!appTime) return 0

  const diff = dayjs(now).diff(appTime, 'minute')
  return Math.max(0, diff)
}
