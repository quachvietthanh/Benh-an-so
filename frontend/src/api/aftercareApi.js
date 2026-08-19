import followUpReminderApi from './followUpReminderApi'
import careLogApi from './careLogApi'

const aftercareApi = {
  getReminders: (params) => followUpReminderApi.search(params),
  getDueReminders: (params) => followUpReminderApi.getDue(params),
  createReminder: (data) => followUpReminderApi.create(data),
  updateReminderStatus: (id, status) => followUpReminderApi.updateStatus(id, status),
  getNotes: (params) => careLogApi.search(params),
  getPatientNotes: (patientId) => careLogApi.getForPatient(patientId),
  createNote: (data) => careLogApi.create(data),
}

export default aftercareApi
