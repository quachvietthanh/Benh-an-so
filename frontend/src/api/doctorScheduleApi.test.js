import assert from 'node:assert/strict'
import test from 'node:test'
import doctorScheduleApi from './doctorScheduleApi.js'
import axiosClient from './axiosClient.js'

test('doctorScheduleApi calls GET /system/doctors/{doctorId}/schedules/weekly', async () => {
  const doctorId = '11111111-1111-1111-1111-111111111111'
  const mockResponse = [
    {
      id: 'ws-1',
      doctorId,
      dayOfWeek: 'MONDAY',
      startTime: '08:00:00',
      endTime: '17:00:00',
      active: true,
    },
  ]

  let capturedUrl
  let capturedMethod
  const originalGet = axiosClient.get
  axiosClient.get = async (url) => {
    capturedUrl = url
    capturedMethod = 'GET'
    return { data: mockResponse }
  }

  try {
    const res = await doctorScheduleApi.getWeeklySchedule(doctorId)
    assert.equal(capturedUrl, `/system/doctors/${doctorId}/schedules/weekly`)
    assert.equal(capturedMethod, 'GET')
    assert.deepEqual(res.data, mockResponse)
  } finally {
    axiosClient.get = originalGet
  }
})

test('doctorScheduleApi calls PUT /system/doctors/{doctorId}/schedules/weekly with schedules payload', async () => {
  const doctorId = '11111111-1111-1111-1111-111111111111'
  const payload = {
    schedules: [
      { dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '12:00:00', active: true },
      { dayOfWeek: 'TUESDAY', startTime: '13:00:00', endTime: '17:00:00', active: true },
    ],
  }

  let capturedUrl
  let capturedData
  const originalPut = axiosClient.put
  axiosClient.put = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: payload.schedules }
  }

  try {
    const res = await doctorScheduleApi.configureWeeklySchedule(doctorId, payload)
    assert.equal(capturedUrl, `/system/doctors/${doctorId}/schedules/weekly`)
    assert.deepEqual(capturedData, payload)
    assert.deepEqual(res.data, payload.schedules)
  } finally {
    axiosClient.put = originalPut
  }
})

test('doctorScheduleApi calls GET /system/doctors/{doctorId}/time-offs', async () => {
  const doctorId = '11111111-1111-1111-1111-111111111111'
  const mockTimeOffs = [
    {
      id: 'to-1',
      doctorId,
      startTime: '2026-09-10T08:00:00Z',
      endTime: '2026-09-10T12:00:00Z',
      reason: 'Hội thảo chuyên môn',
      status: 'ACTIVE',
      affectedAppointments: [],
    },
  ]

  let capturedUrl
  const originalGet = axiosClient.get
  axiosClient.get = async (url) => {
    capturedUrl = url
    return { data: mockTimeOffs }
  }

  try {
    const res = await doctorScheduleApi.getTimeOffs(doctorId)
    assert.equal(capturedUrl, `/system/doctors/${doctorId}/time-offs`)
    assert.deepEqual(res.data, mockTimeOffs)
  } finally {
    axiosClient.get = originalGet
  }
})

test('doctorScheduleApi calls POST /system/doctors/{doctorId}/time-offs and returns affectedAppointments (TC-03)', async () => {
  const doctorId = '11111111-1111-1111-1111-111111111111'
  const payload = {
    startTime: '2026-09-10T08:00:00Z',
    endTime: '2026-09-10T12:00:00Z',
    reason: 'Nghỉ đột xuất',
  }

  const mockCreatedTimeOff = {
    id: 'to-2',
    doctorId,
    startTime: payload.startTime,
    endTime: payload.endTime,
    reason: payload.reason,
    status: 'ACTIVE',
    affectedAppointments: [
      {
        id: 'appt-1',
        appointmentCode: 'APT-20260910-001',
        patientId: 'patient-123',
        startTime: '2026-09-10T08:30:00Z',
        endTime: '2026-09-10T09:00:00Z',
        status: 'SCHEDULED',
        reason: 'Khám tim mạch',
      },
    ],
  }

  let capturedUrl
  let capturedData
  const originalPost = axiosClient.post
  axiosClient.post = async (url, data) => {
    capturedUrl = url
    capturedData = data
    return { data: mockCreatedTimeOff }
  }

  try {
    const res = await doctorScheduleApi.registerTimeOff(doctorId, payload)
    assert.equal(capturedUrl, `/system/doctors/${doctorId}/time-offs`)
    assert.deepEqual(capturedData, payload)
    assert.equal(res.data.affectedAppointments.length, 1)
    assert.equal(res.data.affectedAppointments[0].appointmentCode, 'APT-20260910-001')
  } finally {
    axiosClient.post = originalPost
  }
})

test('doctorScheduleApi calls PATCH /system/doctors/{doctorId}/time-offs/{timeOffId}/cancel', async () => {
  const doctorId = '11111111-1111-1111-1111-111111111111'
  const timeOffId = 'to-99'

  let capturedUrl
  const originalPatch = axiosClient.patch
  axiosClient.patch = async (url) => {
    capturedUrl = url
    return { data: { id: timeOffId, status: 'CANCELLED' } }
  }

  try {
    const res = await doctorScheduleApi.cancelTimeOff(doctorId, timeOffId)
    assert.equal(capturedUrl, `/system/doctors/${doctorId}/time-offs/${timeOffId}/cancel`)
    assert.equal(res.data.status, 'CANCELLED')
  } finally {
    axiosClient.patch = originalPatch
  }
})

test('doctorScheduleApi rethrows 403 Forbidden and does NOT swallow it into fallback (Blocker 1)', async () => {
  const doctorId = '11111111-1111-1111-1111-111111111111'
  const timeOffId = 'to-99'
  const forbiddenError = new Error('Request failed with status code 403')
  forbiddenError.response = { status: 403, data: { message: 'Forbidden: Access Denied' } }

  const originalGet = axiosClient.get
  const originalPut = axiosClient.put
  const originalPost = axiosClient.post
  const originalPatch = axiosClient.patch

  axiosClient.get = async () => { throw forbiddenError }
  axiosClient.put = async () => { throw forbiddenError }
  axiosClient.post = async () => { throw forbiddenError }
  axiosClient.patch = async () => { throw forbiddenError }

  try {
    // 1. getWeeklySchedule
    await assert.rejects(
      async () => await doctorScheduleApi.getWeeklySchedule(doctorId),
      (err) => err.response?.status === 403
    )

    // 2. configureWeeklySchedule
    await assert.rejects(
      async () => await doctorScheduleApi.configureWeeklySchedule(doctorId, { schedules: [] }),
      (err) => err.response?.status === 403
    )

    // 3. getTimeOffs
    await assert.rejects(
      async () => await doctorScheduleApi.getTimeOffs(doctorId),
      (err) => err.response?.status === 403
    )

    // 4. registerTimeOff
    await assert.rejects(
      async () => await doctorScheduleApi.registerTimeOff(doctorId, { startTime: '2026-09-10T08:00:00Z', endTime: '2026-09-10T12:00:00Z' }),
      (err) => err.response?.status === 403
    )

    // 5. cancelTimeOff
    await assert.rejects(
      async () => await doctorScheduleApi.cancelTimeOff(doctorId, timeOffId),
      (err) => err.response?.status === 403
    )
  } finally {
    axiosClient.get = originalGet
    axiosClient.put = originalPut
    axiosClient.post = originalPost
    axiosClient.patch = originalPatch
  }
})
