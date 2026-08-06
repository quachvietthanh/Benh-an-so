import {
  getStoredAftercareNotes,
  getStoredFollowupReminders,
  saveAftercareNote,
  saveFollowupReminder,
  updateReminderStatus,
} from '../utils/aftercareHelpers.js'

const aftercareApi = {
  getReminders: async () => {
    return { data: getStoredFollowupReminders() }
  },
  createReminder: async (data) => {
    const updated = saveFollowupReminder(data)
    return { data: updated[0] }
  },
  updateReminderStatus: async (id, status) => {
    const updated = updateReminderStatus(id, status)
    return { data: updated }
  },
  getNotes: async () => {
    return { data: getStoredAftercareNotes() }
  },
  createNote: async (data) => {
    const updated = saveAftercareNote(data)
    return { data: updated[0] }
  },
}

export default aftercareApi
