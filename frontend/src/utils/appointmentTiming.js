import dayjs from 'dayjs'

export const NO_SHOW_GRACE_MINUTES = 15

export const getAppointmentSlotTimes = (appointment) => {
  if (!appointment) return { startTime: null, endTime: null, overdueThreshold: null }

  let dateStr = dayjs().format('YYYY-MM-DD')
  if (appointment.date) {
    dateStr = dayjs(appointment.date).format('YYYY-MM-DD')
  } else if (appointment.appointmentAt) {
    const parsedDate = dayjs(appointment.appointmentAt)
    if (parsedDate.isValid()) dateStr = parsedDate.format('YYYY-MM-DD')
  }

  let startStr = '08:45'
  let endStr = '09:15'

  if (appointment.slot) {
    const parts = String(appointment.slot).split('-')
    startStr = parts[0]?.trim() || '08:45'
    if (parts[1]?.trim()) {
      endStr = parts[1].trim()
    } else {
      const sDay = dayjs(`${dateStr}T${startStr.length === 5 ? startStr + ':00' : startStr}`)
      endStr = sDay.isValid() ? sDay.add(30, 'minute').format('HH:mm') : '09:15'
    }
  } else if (appointment.startTime) {
    startStr = dayjs(appointment.startTime).format('HH:mm')
    if (appointment.endTime) {
      endStr = dayjs(appointment.endTime).format('HH:mm')
    } else {
      endStr = dayjs(appointment.startTime).add(30, 'minute').format('HH:mm')
    }
  } else if (appointment.appointmentAt && String(appointment.appointmentAt).includes('T')) {
    const appAt = dayjs(appointment.appointmentAt)
    startStr = appAt.format('HH:mm')
    endStr = appAt.add(30, 'minute').format('HH:mm')
  }

  if (startStr.length === 5) startStr = `${startStr}:00`
  if (endStr.length === 5) endStr = `${endStr}:00`

  const startTime = dayjs(`${dateStr}T${startStr}`)
  const endTime = dayjs(`${dateStr}T${endStr}`)
  const overdueThreshold = endTime.isValid() ? endTime.add(NO_SHOW_GRACE_MINUTES, 'minute') : null

  return { startTime, endTime, overdueThreshold }
}

export const getAppointmentDateTime = (appointment) => {
  const { endTime, startTime } = getAppointmentSlotTimes(appointment)
  return endTime || startTime
}

export const getNoShowDeadline = (appointmentAt) => {
  const value = dayjs(appointmentAt)
  return value.isValid() ? value.add(NO_SHOW_GRACE_MINUTES, 'minute') : null
}

export const isAppointmentPast15Mins = (appointment, now = dayjs()) => {
  if (!appointment) return false
  const activeStatuses = ['SCHEDULED', 'CHECKED_IN', 'WAITING']
  if (!activeStatuses.includes(appointment.status)) return false

  const { overdueThreshold } = getAppointmentSlotTimes(appointment)
  if (!overdueThreshold || !overdueThreshold.isValid()) return false

  return dayjs(now).isAfter(overdueThreshold) || dayjs(now).isSame(overdueThreshold)
}

export const isAppointmentOverdue = (appointment, now = dayjs()) => {
  return isAppointmentPast15Mins(appointment, now)
}

export const getOverdueMinutes = (appointment, now = dayjs()) => {
  const { overdueThreshold, endTime } = getAppointmentSlotTimes(appointment)
  if (!overdueThreshold || !overdueThreshold.isValid()) return 0

  const diff = dayjs(now).diff(endTime || overdueThreshold, 'minute')
  return Math.max(0, diff)
}
