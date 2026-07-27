const FOLLOWUP_REMINDERS_KEY = 'app_followup_reminders'
const AFTERCARE_NOTES_KEY = 'app_aftercare_notes'

export const getStoredFollowupReminders = () => {
  try {
    const raw = localStorage.getItem(FOLLOWUP_REMINDERS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveFollowupReminder = (reminder) => {
  try {
    const current = getStoredFollowupReminders()
    const newReminder = {
      id: reminder.id || `rem-${Date.now()}`,
      status: reminder.status || 'PENDING',
      createdAt: reminder.createdAt || new Date().toISOString(),
      ...reminder,
    }
    const updated = [newReminder, ...current.filter((r) => r.id !== newReminder.id)]
    localStorage.setItem(FOLLOWUP_REMINDERS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const updateReminderStatus = (reminderId, status) => {
  try {
    const current = getStoredFollowupReminders()
    const updated = current.map((r) => (r.id === reminderId ? { ...r, status } : r))
    localStorage.setItem(FOLLOWUP_REMINDERS_KEY, JSON.stringify(updated))
    return updated
  } catch {
    return []
  }
}

export const getStoredAftercareNotes = () => {
  try {
    const raw = localStorage.getItem(AFTERCARE_NOTES_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const saveAftercareNote = (note) => {
  try {
    const current = getStoredAftercareNotes()
    const newNote = {
      id: note.id || `note-${Date.now()}`,
      recordedAt: new Date().toISOString(),
      ...note,
    }
    const updated = [newNote, ...current]
    localStorage.setItem(AFTERCARE_NOTES_KEY, JSON.stringify(updated))

    if (note.reminderId) {
      updateReminderStatus(note.reminderId, 'COMPLETED')
    }

    return updated
  } catch {
    return []
  }
}
