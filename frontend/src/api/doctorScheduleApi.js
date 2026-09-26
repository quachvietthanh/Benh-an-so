import axiosClient from './axiosClient.js'

const LOCAL_STORAGE_WEEKLY_PREFIX = 'bsa_doctor_weekly_schedule_'
const LOCAL_STORAGE_TIMEOFF_PREFIX = 'bsa_doctor_time_offs_'

const getLocalWeekly = (doctorId) => {
  try {
    const raw = localStorage.getItem(`${LOCAL_STORAGE_WEEKLY_PREFIX}${doctorId}`)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const saveLocalWeekly = (doctorId, schedules) => {
  try {
    localStorage.setItem(`${LOCAL_STORAGE_WEEKLY_PREFIX}${doctorId}`, JSON.stringify(schedules))
  } catch {
    // ignore
  }
}

const getLocalTimeOffs = (doctorId) => {
  try {
    const raw = localStorage.getItem(`${LOCAL_STORAGE_TIMEOFF_PREFIX}${doctorId}`)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const saveLocalTimeOffs = (doctorId, timeOffs) => {
  try {
    localStorage.setItem(`${LOCAL_STORAGE_TIMEOFF_PREFIX}${doctorId}`, JSON.stringify(timeOffs))
  } catch {
    // ignore
  }
}

/**
 * API client for doctor weekly schedules and time-off intervals (NCL-03-CN-006 / QTN-30).
 * Includes seamless local fallback when backend is waiting to be restarted (e.g. 404 / 403).
 */
const doctorScheduleApi = {
  /**
   * Get recurring weekly schedule for a doctor.
   * GET /system/doctors/{doctorId}/schedules/weekly
   */
  getWeeklySchedule: async (doctorId) => {
    try {
      const res = await axiosClient.get(`/system/doctors/${doctorId}/schedules/weekly`)
      return res
    } catch (err) {
      if (err.response?.status === 403) {
        throw err
      }
      if (err.response?.status === 404 || !err.response) {
        const local = getLocalWeekly(doctorId)
        return { data: local || [], isFallback: true, errorStatus: err.response?.status }
      }
      throw err
    }
  },

  /**
   * Configure recurring weekly schedule for a doctor (PUT collection replacement).
   * PUT /system/doctors/{doctorId}/schedules/weekly
   */
  configureWeeklySchedule: async (doctorId, data) => {
    try {
      const res = await axiosClient.put(`/system/doctors/${doctorId}/schedules/weekly`, data)
      saveLocalWeekly(doctorId, data.schedules)
      return res
    } catch (err) {
      if (err.response?.status === 403) {
        throw err
      }
      if (err.response?.status === 404 || !err.response) {
        saveLocalWeekly(doctorId, data.schedules)
        return { data: data.schedules, isFallback: true, errorStatus: err.response?.status }
      }
      throw err
    }
  },

  /**
   * Get unexpected time-off intervals for a doctor.
   * GET /system/doctors/{doctorId}/time-offs
   */
  getTimeOffs: async (doctorId) => {
    try {
      const res = await axiosClient.get(`/system/doctors/${doctorId}/time-offs`)
      return res
    } catch (err) {
      if (err.response?.status === 403) {
        throw err
      }
      if (err.response?.status === 404 || !err.response) {
        const local = getLocalTimeOffs(doctorId)
        return { data: local || [], isFallback: true, errorStatus: err.response?.status }
      }
      throw err
    }
  },

  /**
   * Register an unexpected time-off interval for a doctor.
   * POST /system/doctors/{doctorId}/time-offs
   */
  registerTimeOff: async (doctorId, data) => {
    try {
      const res = await axiosClient.post(`/system/doctors/${doctorId}/time-offs`, data)
      return res
    } catch (err) {
      if (err.response?.status === 403) {
        throw err
      }
      if (err.response?.status === 404 || !err.response) {
        const currentList = getLocalTimeOffs(doctorId)
        const newTimeOff = {
          id: 'to-' + Date.now(),
          doctorId,
          startTime: data.startTime,
          endTime: data.endTime,
          reason: data.reason,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          affectedAppointments: [],
        }
        const updated = [newTimeOff, ...currentList]
        saveLocalTimeOffs(doctorId, updated)
        return { data: newTimeOff, isFallback: true, errorStatus: err.response?.status }
      }
      throw err
    }
  },

  /**
   * Cancel an active time-off interval for a doctor.
   * PATCH /system/doctors/{doctorId}/time-offs/{timeOffId}/cancel
   */
  cancelTimeOff: async (doctorId, timeOffId) => {
    try {
      const res = await axiosClient.patch(`/system/doctors/${doctorId}/time-offs/${timeOffId}/cancel`)
      return res
    } catch (err) {
      if (err.response?.status === 403) {
        throw err
      }
      if (err.response?.status === 404 || !err.response) {
        const currentList = getLocalTimeOffs(doctorId)
        const updated = currentList.map((t) =>
          t.id === timeOffId ? { ...t, status: 'CANCELLED' } : t
        )
        saveLocalTimeOffs(doctorId, updated)
        return { data: { id: timeOffId, status: 'CANCELLED' }, isFallback: true }
      }
      throw err
    }
  },
}

export default doctorScheduleApi
