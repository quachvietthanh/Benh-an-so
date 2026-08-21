import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  BellOutlined,
  FileDoneOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  message,
  Row,
  Select,
  Skeleton,
  Space,
  Statistic,
  Tabs,
  Typography,
} from 'antd'
import followUpReminderApi from '../api/followUpReminderApi'
import patientApi from '../api/patientApi'
import postCareLogApi from '../api/postCareLogApi'
import queueApi from '../api/queueApi'
import CareLogModal from '../components/after-care/CareLogModal'
import CareLogTable from '../components/after-care/CareLogTable'
import FollowUpReminderModal from '../components/after-care/FollowUpReminderModal'
import PatientCareHistoryModal from '../components/after-care/PatientCareHistoryModal'
import ReminderTable from '../components/after-care/ReminderTable'
import { useAuthContext } from '../context/AuthContext'
import {
  CONTACT_CHANNELS,
  REMINDER_STATUSES,
  REMINDER_TYPES,
  buildCareLogPayload,
  buildReminderPayload,
  buildTodayPatients,
  getAftercareErrorMessage,
  getVietnamDateKey,
  hasAftercarePermission,
  isUuid,
  normalizePage,
  selectTodayCompletedVisits,
  vietnamDateTimeToIso,
} from '../utils/aftercareHelpers'
import { unwrapCollection } from '../utils/workflowContract'

const { RangePicker } = DatePicker
const { Paragraph, Text, Title } = Typography
const DEFAULT_PAGE_SIZE = 10

const createSourceState = () => ({
  items: [],
  total: 0,
  page: 0,
  size: DEFAULT_PAGE_SIZE,
  loading: false,
  error: null,
  clientSide: false,
})

const createStatState = () => ({ value: 0, loading: false, error: null })

function SourceAlert({ error, onRetry }) {
  return (
    <Alert
      type="error"
      showIcon
      message="Không thể tải dữ liệu"
      description={getAftercareErrorMessage(error)}
      action={(
        <Button size="small" icon={<ReloadOutlined />} onClick={onRetry}>
          Thử lại
        </Button>
      )}
    />
  )
}

function PermissionAlert() {
  return (
    <Alert
      type="error"
      showIcon
      message="Bạn không có quyền xem dữ liệu này."
      description="Hệ thống đã ẩn thao tác và không gửi API khi token không có permission tương ứng."
    />
  )
}

function StatCard({ title, value, loading, error, allowed, icon, tone }) {
  let displayValue = value
  let hint = null
  if (!allowed) {
    displayValue = '—'
    hint = 'Không có quyền đọc'
  } else if (error) {
    displayValue = '—'
    hint = getAftercareErrorMessage(error)
  }

  return (
    <Card className={`aftercare-stat-card is-${tone}`}>
      <Skeleton loading={allowed && loading} active paragraph={false}>
        <Statistic title={title} value={displayValue} prefix={icon} />
        {hint && <Text type="secondary" ellipsis={{ tooltip: hint }}>{hint}</Text>}
      </Skeleton>
    </Card>
  )
}

function AfterCarePage() {
  const location = useLocation()
  const { user } = useAuthContext()
  const [reminderFilterForm] = Form.useForm()
  const [careFilterForm] = Form.useForm()

  const [dueSource, setDueSource] = useState(createSourceState)
  const [reminderSource, setReminderSource] = useState(createSourceState)
  const [careSource, setCareSource] = useState(createSourceState)
  const [dueStat, setDueStat] = useState(createStatState)
  const [reminderStat, setReminderStat] = useState(createStatState)
  const [careStat, setCareStat] = useState(createStatState)
  const [reminderFilters, setReminderFilters] = useState({})
  const [careFilters, setCareFilters] = useState({})
  const [patients, setPatients] = useState([])
  const [patientsById, setPatientsById] = useState({})
  const [patientsLoading, setPatientsLoading] = useState(false)
  const [patientsError, setPatientsError] = useState(null)
  const [todayVisits, setTodayVisits] = useState([])
  const [todayPatients, setTodayPatients] = useState([])
  const [loadingTodayVisits, setLoadingTodayVisits] = useState(false)
  const [todayVisitsError, setTodayVisitsError] = useState(null)
  const [visitsById, setVisitsById] = useState({})
  const [refreshing, setRefreshing] = useState(false)

  const [reminderModalOpen, setReminderModalOpen] = useState(false)
  const [careModalOpen, setCareModalOpen] = useState(false)
  const [sourceReminder, setSourceReminder] = useState(null)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [historyPatientId, setHistoryPatientId] = useState(null)
  const [submittingReminder, setSubmittingReminder] = useState(false)
  const [submittingCareLog, setSubmittingCareLog] = useState(false)
  const [updatingReminderId, setUpdatingReminderId] = useState(null)

  const patientMapRef = useRef({})
  const visitMapRef = useRef({})
  const requestedPatientIdsRef = useRef(new Set())
  const requestedVisitPatientIdsRef = useRef(new Set())
  const reminderSubmitLockRef = useRef(false)
  const careSubmitLockRef = useRef(false)
  const routePresetHandledRef = useRef(false)
  const dueRequestRef = useRef(0)
  const reminderRequestRef = useRef(0)
  const careRequestRef = useRef(0)
  const todayVisitsRequestRef = useRef(0)
  const createdReminderVisitIdsRef = useRef(new Set())
  const createdReminderPatientIdsRef = useRef(new Set())

  const userRoles = useMemo(() => {
    return (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  }, [user])

  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const permissions = useMemo(() => {
    const isRec = userRoles.includes('receptionist')
    const isAdmin = userRoles.includes('admin')
    const hasPerm = (code) => userPermissions.includes(code)

    return {
      reminderCreate: hasPerm('FOLLOW_UP_REMINDER_CREATE') || isRec,
      reminderRead: hasPerm('FOLLOW_UP_REMINDER_READ') || isRec || isAdmin,
      reminderUpdate: hasPerm('FOLLOW_UP_REMINDER_UPDATE') || isRec,
      careCreate: hasPerm('CARE_LOG_CREATE') || isRec,
      careRead: hasPerm('CARE_LOG_READ') || isRec || isAdmin,
      medicalRecordRead: hasPerm('MEDICAL_RECORD_READ') || isRec || isAdmin,
      isReadOnly: !hasPerm('FOLLOW_UP_REMINDER_CREATE') && !hasPerm('CARE_LOG_CREATE') && !isRec,
    }
  }, [userRoles, userPermissions])

  const performerNames = useMemo(() => {
    if (!user?.id) return {}
    return { [user.id]: user.fullName || user.username }
  }, [user])

  useEffect(() => {
    patientMapRef.current = patientsById
  }, [patientsById])

  useEffect(() => {
    visitMapRef.current = visitsById
  }, [visitsById])

  const mergePatients = useCallback((records) => {
    const valid = (Array.isArray(records) ? records : []).filter((patient) => patient?.id)
    if (!valid.length) return
    setPatientsById((current) => {
      const next = { ...current }
      valid.forEach((patient) => {
        const availableFields = Object.fromEntries(
          Object.entries(patient).filter(([, value]) => value !== null && value !== undefined),
        )
        next[patient.id] = { ...next[patient.id], ...availableFields }
        requestedPatientIdsRef.current.add(patient.id)
      })
      return next
    })
  }, [])

  const ensurePatientReferences = useCallback(async (records) => {
    const ids = Array.from(new Set((Array.isArray(records) ? records : [])
      .map((record) => record?.patientId)
      .filter((id) => id && !patientMapRef.current[id] && !requestedPatientIdsRef.current.has(id))))

    ids.forEach((id) => requestedPatientIdsRef.current.add(id))
    if (!ids.length) return

    const responses = await Promise.allSettled(ids.map((id) => patientApi.getById(id)))
    mergePatients(responses
      .filter((result) => result.status === 'fulfilled')
      .map((result) => result.value?.data)
      .filter(Boolean))
  }, [mergePatients])

  const registerVisits = useCallback((visits) => {
    const valid = (Array.isArray(visits) ? visits : []).filter((visit) => visit?.visitId)
    if (!valid.length) return
    setVisitsById((current) => {
      const next = { ...current }
      valid.forEach((visit) => { next[visit.visitId] = visit })
      return next
    })
  }, [])

  const ensureVisitReferences = useCallback(async (records) => {
    if (!permissions.medicalRecordRead) return
    const patientIds = Array.from(new Set((Array.isArray(records) ? records : [])
      .filter((record) => record?.patientId && record?.visitId && !visitMapRef.current[record.visitId])
      .map((record) => record.patientId)
      .filter((patientId) => !requestedVisitPatientIdsRef.current.has(patientId))))
    patientIds.forEach((patientId) => requestedVisitPatientIdsRef.current.add(patientId))
    if (!patientIds.length) return

    const histories = await Promise.allSettled(patientIds.map(async (patientId) => {
      const firstResponse = await patientApi.getHistory(patientId, { page: 0, size: 100 })
      const firstPage = normalizePage(firstResponse.data, 0, 100)
      const remainingPageNumbers = Array.from(
        { length: Math.max(0, firstPage.totalPages - 1) },
        (_, index) => index + 1,
      )
      const remainingResponses = await Promise.all(remainingPageNumbers.map((page) => (
        patientApi.getHistory(patientId, { page, size: 100 })
      )))
      return [
        ...firstPage.content,
        ...remainingResponses.flatMap((response, index) => (
          normalizePage(response.data, index + 1, 100).content
        )),
      ]
    }))
    registerVisits(histories
      .filter((result) => result.status === 'fulfilled')
      .flatMap((result) => result.value))
  }, [permissions.medicalRecordRead, registerVisits])

  const loadPatients = useCallback(async () => {
    setPatientsLoading(true)
    setPatientsError(null)
    try {
      const firstResponse = await patientApi.getAll({ page: 0, size: 100, sort: 'fullName,asc' })
      const firstPage = normalizePage(firstResponse.data, 0, 100)
      const remainingPageNumbers = Array.from(
        { length: Math.max(0, firstPage.totalPages - 1) },
        (_, index) => index + 1,
      )
      const remainingResponses = await Promise.all(remainingPageNumbers.map((page) => (
        patientApi.getAll({ page, size: 100, sort: 'fullName,asc' })
      )))
      const allPatients = [
        ...firstPage.content,
        ...remainingResponses.flatMap((response, index) => (
          normalizePage(response.data, index + 1, 100).content
        )),
      ]

      setPatients(allPatients)
      mergePatients(allPatients)
    } catch (error) {
      setPatientsError(error)
    } finally {
      setPatientsLoading(false)
    }
  }, [mergePatients])

  const loadTodayCompletedVisits = useCallback(async () => {
    const requestId = ++todayVisitsRequestRef.current
    const todayKey = getVietnamDateKey(new Date())

    setLoadingTodayVisits(true)
    setTodayVisitsError(null)
    setTodayVisits([])
    setTodayPatients([])

    try {
      if (!todayKey) throw new Error('Không thể xác định ngày hiện tại theo múi giờ Việt Nam.')

      const [queueRes, reminderRes, dueRes] = await Promise.allSettled([
        queueApi.getQueues({ date: todayKey }),
        followUpReminderApi.search({ page: 0, size: 200 }),
        followUpReminderApi.getDue({ page: 0, size: 200 }),
      ])

      if (queueRes.status === 'rejected') {
        throw queueRes.reason
      }

      const completedVisits = selectTodayCompletedVisits(queueRes.value.data, todayKey)

      const activeReminderVisitIds = new Set(createdReminderVisitIdsRef.current)
      const activeReminderPatientIds = new Set(createdReminderPatientIdsRef.current)

      const extractReminderMeta = (res) => {
        if (res.status === 'fulfilled' && res.value?.data) {
          const list = unwrapCollection(res.value.data)
          list.forEach((r) => {
            if (r && r.status !== 'CANCELLED') {
              if (r.visitId || r.id) activeReminderVisitIds.add(String(r.visitId || r.id))
              if (r.patientId) activeReminderPatientIds.add(String(r.patientId))
            }
          })
        }
      }
      extractReminderMeta(reminderRes)
      extractReminderMeta(dueRes)

      ;(reminderSource.items || []).forEach((r) => {
        if (r && r.status !== 'CANCELLED') {
          if (r.visitId || r.id) activeReminderVisitIds.add(String(r.visitId || r.id))
          if (r.patientId) activeReminderPatientIds.add(String(r.patientId))
        }
      })
      ;(dueSource.items || []).forEach((r) => {
        if (r && r.status !== 'CANCELLED') {
          if (r.visitId || r.id) activeReminderVisitIds.add(String(r.visitId || r.id))
          if (r.patientId) activeReminderPatientIds.add(String(r.patientId))
        }
      })

      const availableVisits = completedVisits.filter((visit) => {
        const vId = String(visit.visitId || visit.id)
        const pId = String(visit.patientId)
        return !activeReminderVisitIds.has(vId) && !activeReminderPatientIds.has(pId)
      })

      const patientIds = Array.from(new Set(availableVisits.map((visit) => visit.patientId)))
      const patientResponses = await Promise.allSettled(
        patientIds.map((patientId) => patientApi.getById(patientId)),
      )
      const patientDetails = patientResponses
        .filter((result) => result.status === 'fulfilled' && result.value?.data)
        .map((result) => result.value.data)
      const availablePatients = buildTodayPatients(availableVisits, patientDetails)

      if (requestId !== todayVisitsRequestRef.current) return

      setTodayVisits(availableVisits)
      setTodayPatients(availablePatients)
      mergePatients(availablePatients)
      registerVisits(availableVisits)
    } catch (error) {
      if (requestId !== todayVisitsRequestRef.current) return
      setTodayVisits([])
      setTodayPatients([])
      setTodayVisitsError(error)
    } finally {
      if (requestId === todayVisitsRequestRef.current) setLoadingTodayVisits(false)
    }
  }, [mergePatients, registerVisits])

  const loadDue = useCallback(async (page = 0, size = DEFAULT_PAGE_SIZE) => {
    const requestId = ++dueRequestRef.current
    setDueSource((current) => ({ ...current, page, size, loading: true, error: null }))
    try {
      const response = await followUpReminderApi.getDue({ page, size })
      const normalized = normalizePage(response.data, page, size)
      if (requestId !== dueRequestRef.current) return
      setDueSource((current) => ({
        ...current,
        items: normalized.content,
        total: normalized.totalElements,
        page: normalized.number,
        size: normalized.size,
        loading: false,
        error: null,
      }))
      ensurePatientReferences(normalized.content)
      ensureVisitReferences(normalized.content)
    } catch (error) {
      if (requestId !== dueRequestRef.current) return
      setDueSource((current) => ({ ...current, loading: false, error }))
    }
  }, [ensurePatientReferences, ensureVisitReferences])

  const loadDueStat = useCallback(async () => {
    setDueStat((current) => ({ ...current, loading: true, error: null }))
    try {
      const response = await followUpReminderApi.getDue({ page: 0, size: 1 })
      const normalized = normalizePage(response.data, 0, 1)
      setDueStat({ value: normalized.totalElements, loading: false, error: null })
    } catch (error) {
      setDueStat((current) => ({ ...current, loading: false, error }))
    }
  }, [])

  const loadReminders = useCallback(async (
    filters = {},
    page = 0,
    size = DEFAULT_PAGE_SIZE,
  ) => {
    const requestId = ++reminderRequestRef.current
    setReminderSource((current) => ({ ...current, page, size, loading: true, error: null }))
    const params = {}
    if (filters.patientId) params.patientId = filters.patientId
    if (filters.status) params.status = filters.status
    if (filters.from) params.from = filters.from
    if (filters.to) params.to = filters.to

    try {
      if (filters.reminderType) {
        const firstResponse = await followUpReminderApi.search({ ...params, page: 0, size: 100 })
        const firstPage = normalizePage(firstResponse.data, 0, 100)
        const remainingPageNumbers = Array.from(
          { length: Math.max(0, firstPage.totalPages - 1) },
          (_, index) => index + 1,
        )
        const remainingResponses = await Promise.all(remainingPageNumbers.map((backendPage) => (
          followUpReminderApi.search({ ...params, page: backendPage, size: 100 })
        )))
        const allRecords = [
          ...firstPage.content,
          ...remainingResponses.flatMap((response, index) => (
            normalizePage(response.data, index + 1, 100).content
          )),
        ]
        const filteredRecords = allRecords.filter((record) => (
          record.reminderType === filters.reminderType
        ))
        const pageStart = page * size
        const visibleRecords = filteredRecords.slice(pageStart, pageStart + size)
        if (requestId !== reminderRequestRef.current) return
        setReminderSource((current) => ({
          ...current,
          items: visibleRecords,
          total: filteredRecords.length,
          page,
          size,
          clientSide: true,
          loading: false,
          error: null,
        }))
        ensurePatientReferences(visibleRecords)
        ensureVisitReferences(visibleRecords)
        return
      }

      const response = await followUpReminderApi.search({ ...params, page, size })
      const normalized = normalizePage(response.data, page, size)
      if (requestId !== reminderRequestRef.current) return
      setReminderSource((current) => ({
        ...current,
        items: normalized.content,
        total: normalized.totalElements,
        page: normalized.number,
        size: normalized.size,
        clientSide: false,
        loading: false,
        error: null,
      }))
      ensurePatientReferences(normalized.content)
      ensureVisitReferences(normalized.content)
    } catch (error) {
      if (requestId !== reminderRequestRef.current) return
      setReminderSource((current) => ({ ...current, loading: false, error }))
    }
  }, [ensurePatientReferences, ensureVisitReferences])

  const loadReminderStat = useCallback(async () => {
    setReminderStat((current) => ({ ...current, loading: true, error: null }))
    try {
      const response = await followUpReminderApi.search({ page: 0, size: 1 })
      const normalized = normalizePage(response.data, 0, 1)
      setReminderStat({ value: normalized.totalElements, loading: false, error: null })
    } catch (error) {
      setReminderStat((current) => ({ ...current, loading: false, error }))
    }
  }, [])

  const loadCareLogs = useCallback(async (
    filters = {},
    page = 0,
    size = DEFAULT_PAGE_SIZE,
  ) => {
    const requestId = ++careRequestRef.current
    setCareSource((current) => ({ ...current, page, size, loading: true, error: null }))
    try {
      if (filters.patientId) {
        const response = await postCareLogApi.getForPatient(filters.patientId)
        if (!Array.isArray(response.data)) {
          throw new Error('Dữ liệu lịch sử chăm sóc từ Backend không đúng định dạng.')
        }
        const patientRecords = response.data
        const fromMillis = filters.from ? new Date(filters.from).getTime() : null
        const toMillis = filters.to ? new Date(filters.to).getTime() : null
        const filtered = patientRecords.filter((record) => {
          const contactedAt = new Date(record.contactedAt).getTime()
          return (!filters.channel || record.contactChannel === filters.channel)
            && (fromMillis === null || contactedAt >= fromMillis)
            && (toMillis === null || contactedAt < toMillis)
        })
        const pageStart = page * size
        if (requestId !== careRequestRef.current) return
        setCareSource((current) => ({
          ...current,
          items: filtered.slice(pageStart, pageStart + size),
          total: filtered.length,
          page,
          size,
          clientSide: true,
          loading: false,
          error: null,
        }))
        ensurePatientReferences(filtered)
        ensureVisitReferences(filtered)
        return
      }

      const params = { page, size }
      if (filters.channel) params.channel = filters.channel
      if (filters.from) params.from = filters.from
      if (filters.to) params.to = filters.to
      const response = await postCareLogApi.search(params)
      const normalized = normalizePage(response.data, page, size)
      if (requestId !== careRequestRef.current) return
      setCareSource((current) => ({
        ...current,
        items: normalized.content,
        total: normalized.totalElements,
        page: normalized.number,
        size: normalized.size,
        clientSide: false,
        loading: false,
        error: null,
      }))
      ensurePatientReferences(normalized.content)
      ensureVisitReferences(normalized.content)
    } catch (error) {
      if (requestId !== careRequestRef.current) return
      setCareSource((current) => ({ ...current, loading: false, error }))
    }
  }, [ensurePatientReferences, ensureVisitReferences])

  const loadCareStat = useCallback(async () => {
    setCareStat((current) => ({ ...current, loading: true, error: null }))
    try {
      const response = await postCareLogApi.search({ page: 0, size: 1 })
      const normalized = normalizePage(response.data, 0, 1)
      setCareStat({ value: normalized.totalElements, loading: false, error: null })
    } catch (error) {
      setCareStat((current) => ({ ...current, loading: false, error }))
    }
  }, [])

  const openReminderModal = useCallback(() => {
    setReminderModalOpen(true)
    loadTodayCompletedVisits()
  }, [loadTodayCompletedVisits])

  useEffect(() => {
    loadPatients()
    if (permissions.reminderCreate) loadTodayCompletedVisits()
    if (permissions.reminderRead) {
      loadDue()
      loadReminders()
      loadDueStat()
      loadReminderStat()
    }
    if (permissions.careRead) {
      loadCareLogs()
      loadCareStat()
    }
  }, [
    loadCareLogs,
    loadCareStat,
    loadDue,
    loadDueStat,
    loadPatients,
    loadTodayCompletedVisits,
    loadReminderStat,
    loadReminders,
    permissions.careRead,
    permissions.reminderCreate,
    permissions.reminderRead,
  ])

  useEffect(() => {
    if (routePresetHandledRef.current) return
    const presetPatientId = location.state?.patientId
    if (permissions.reminderCreate && isUuid(presetPatientId)) {
      routePresetHandledRef.current = true
      openReminderModal()
    }
  }, [location.state, openReminderModal, permissions.reminderCreate])

  const handleRefresh = async () => {
    setRefreshing(true)
    const jobs = [loadPatients()]
    if (permissions.reminderCreate) jobs.push(loadTodayCompletedVisits())
    if (permissions.reminderRead) {
      jobs.push(loadDue(dueSource.page, dueSource.size))
      jobs.push(loadReminders(reminderFilters, reminderSource.page, reminderSource.size))
      jobs.push(loadDueStat())
      jobs.push(loadReminderStat())
    }
    if (permissions.careRead) {
      jobs.push(loadCareLogs(careFilters, careSource.page, careSource.size))
      jobs.push(loadCareStat())
    }
    await Promise.allSettled(jobs)
    setRefreshing(false)
  }

  const handleReminderFilter = (values) => {
    const next = {
      patientId: values.patientId,
      status: values.status,
      reminderType: values.reminderType,
      from: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      to: values.dateRange?.[1]?.format('YYYY-MM-DD'),
    }
    setReminderFilters(next)
    loadReminders(next, 0, reminderSource.size)
  }

  const resetReminderFilter = () => {
    reminderFilterForm.resetFields()
    setReminderFilters({})
    loadReminders({}, 0, reminderSource.size)
  }

  const handleCareFilter = (values) => {
    const next = {
      patientId: values.patientId,
      channel: values.channel,
      from: values.dateRange?.[0]
        ? vietnamDateTimeToIso(values.dateRange[0].startOf('day'))
        : undefined,
      // Backend uses an exclusive upper bound, so UI's inclusive date ends at
      // the start of the following Vietnam day.
      to: values.dateRange?.[1]
        ? vietnamDateTimeToIso(values.dateRange[1].add(1, 'day').startOf('day'))
        : undefined,
    }
    setCareFilters(next)
    loadCareLogs(next, 0, careSource.size)
  }

  const resetCareFilter = () => {
    careFilterForm.resetFields()
    setCareFilters({})
    loadCareLogs({}, 0, careSource.size)
  }

  // NCL-10-CN-001: Create follow-up reminder
  const handleCreateReminder = async (values) => {
    if (reminderSubmitLockRef.current) return
    reminderSubmitLockRef.current = true
    setSubmittingReminder(true)
    try {
      const payload = buildReminderPayload(values)
      if (!isUuid(payload.patientId) || !isUuid(payload.visitId)) {
        throw new Error('Bệnh nhân hoặc lượt khám không dùng UUID hợp lệ.')
      }
      await followUpReminderApi.create(payload)
      if (payload.visitId) createdReminderVisitIdsRef.current.add(String(payload.visitId))
      if (payload.patientId) createdReminderPatientIdsRef.current.add(String(payload.patientId))
      message.success('Đã tạo lịch nhắc tái khám thành công.')
      setReminderModalOpen(false)
      await Promise.allSettled([
        loadDue(0, dueSource.size),
        loadReminders(reminderFilters, 0, reminderSource.size),
        loadDueStat(),
        loadReminderStat(),
        loadTodayCompletedVisits(),
      ])
    } catch (error) {
      message.error(getAftercareErrorMessage(error, 'Không thể tạo lịch nhắc tái khám.'))
    } finally {
      setSubmittingReminder(false)
      reminderSubmitLockRef.current = false
    }
  }

  const handleCreateCareLog = async (values) => {
    if (careSubmitLockRef.current) return
    careSubmitLockRef.current = true
    setSubmittingCareLog(true)
    try {
      const payload = buildCareLogPayload(values)
      if (!isUuid(payload.patientId)) throw new Error('Bệnh nhân không dùng UUID hợp lệ.')
      await postCareLogApi.create(payload)
      message.success('Đã ghi nhận chăm sóc sau khám thành công.')
      setCareModalOpen(false)
      setSourceReminder(null)
      const reloads = []
      if (permissions.careRead) {
        reloads.push(loadCareLogs(careFilters, 0, careSource.size))
        reloads.push(loadCareStat())
      }
      // Care-log service may transition a linked reminder. Reload, never PATCH
      // a second time from the frontend.
      if (permissions.reminderRead) {
        reloads.push(loadDue(0, dueSource.size))
        reloads.push(loadReminders(reminderFilters, reminderSource.page, reminderSource.size))
        reloads.push(loadDueStat())
        reloads.push(loadReminderStat())
      }
      await Promise.allSettled(reloads)
    } catch (error) {
      message.error(getAftercareErrorMessage(error, 'Không thể lưu ghi nhận chăm sóc.'))
    } finally {
      setSubmittingCareLog(false)
      careSubmitLockRef.current = false
    }
  }

  const handleStatusChange = async (reminder, status) => {
    if (updatingReminderId) return
    setUpdatingReminderId(reminder.id)
    try {
      await followUpReminderApi.updateStatus(reminder.id, status)
      message.success('Đã cập nhật trạng thái lịch nhắc.')
      await Promise.allSettled([
        loadDue(dueSource.page, dueSource.size),
        loadReminders(reminderFilters, reminderSource.page, reminderSource.size),
        loadDueStat(),
        loadReminderStat(),
      ])
    } catch (error) {
      message.error(getAftercareErrorMessage(error, 'Không thể cập nhật trạng thái lịch nhắc.'))
    } finally {
      setUpdatingReminderId(null)
    }
  }

  const openCareLogFromReminder = (reminder) => {
    setSourceReminder(reminder)
    setCareModalOpen(true)
    loadTodayCompletedVisits()
  }

  const openIndependentCareLog = () => {
    setSourceReminder(null)
    setCareModalOpen(true)
    loadTodayCompletedVisits()
  }

  const openHistory = (patientId) => {
    if (!isUuid(patientId)) return
    setHistoryPatientId(patientId)
    setHistoryOpen(true)
  }

  const patientOptions = patients.map((patient) => ({
    value: patient.id,
    label: `${patient.fullName} (${patient.patientCode})`,
  }))

  const duePanel = permissions.reminderRead ? (
    dueSource.error ? (
      <SourceAlert error={dueSource.error} onRetry={() => loadDue(dueSource.page, dueSource.size)} />
    ) : (
      <ReminderTable
        mode="due"
        records={dueSource.items}
        loading={dueSource.loading}
        pagination={{ page: dueSource.page, size: dueSource.size, total: dueSource.total }}
        onPageChange={loadDue}
        patientsById={patientsById}
        visitsById={visitsById}
        canCreateCareLog={permissions.careCreate}
        canReadCareLog={permissions.careRead}
        canUpdate={permissions.reminderUpdate}
        updatingId={updatingReminderId}
        onOpenCareLog={openCareLogFromReminder}
        onOpenHistory={openHistory}
        onStatusChange={handleStatusChange}
      />
    )
  ) : <PermissionAlert />

  const remindersPanel = permissions.reminderRead ? (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Form
        form={reminderFilterForm}
        layout="inline"
        className="aftercare-filter-form"
        onFinish={handleReminderFilter}
      >
        <Form.Item name="patientId" label="Bệnh nhân">
          <Select
            showSearch
            allowClear
            optionFilterProp="label"
            options={patientOptions}
            loading={patientsLoading}
            placeholder="Chọn bệnh nhân"
          />
        </Form.Item>
        <Form.Item name="status" label="Trạng thái">
          <Select allowClear options={REMINDER_STATUSES} placeholder="Tất cả" />
        </Form.Item>
        <Form.Item name="reminderType" label="Loại nhắc">
          <Select allowClear options={REMINDER_TYPES} placeholder="Tất cả" />
        </Form.Item>
        <Form.Item name="dateRange" label="Ngày tái khám">
          <RangePicker format="DD/MM/YYYY" />
        </Form.Item>
        <Form.Item className="aftercare-filter-actions">
          <Space>
            <Button type="primary" htmlType="submit">Lọc</Button>
            <Button onClick={resetReminderFilter}>Xóa lọc</Button>
          </Space>
        </Form.Item>
      </Form>
      {reminderFilters.reminderType && (
        <Alert
          type="info"
          showIcon
          message="Backend chưa hỗ trợ query loại nhắc; frontend tổng hợp các trang Backend rồi mới lọc để bảo đảm kết quả đầy đủ."
        />
      )}
      {reminderSource.error ? (
        <SourceAlert
          error={reminderSource.error}
          onRetry={() => loadReminders(reminderFilters, reminderSource.page, reminderSource.size)}
        />
      ) : (
        <ReminderTable
          records={reminderSource.items}
          loading={reminderSource.loading}
          pagination={{
            page: reminderSource.page,
            size: reminderSource.size,
            total: reminderSource.total,
          }}
          onPageChange={(page, size) => loadReminders(reminderFilters, page, size)}
          patientsById={patientsById}
          visitsById={visitsById}
          canCreateCareLog={permissions.careCreate}
          canReadCareLog={permissions.careRead}
          canUpdate={permissions.reminderUpdate}
          updatingId={updatingReminderId}
          onOpenCareLog={openCareLogFromReminder}
          onOpenHistory={openHistory}
          onStatusChange={handleStatusChange}
        />
      )}
    </Space>
  ) : <PermissionAlert />

  const carePanel = permissions.careRead ? (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Form
        form={careFilterForm}
        layout="inline"
        className="aftercare-filter-form"
        onFinish={handleCareFilter}
      >
        <Form.Item name="patientId" label="Bệnh nhân">
          <Select
            showSearch
            allowClear
            optionFilterProp="label"
            options={patientOptions}
            loading={patientsLoading}
            placeholder="Chọn bệnh nhân"
          />
        </Form.Item>
        <Form.Item name="channel" label="Kênh liên hệ">
          <Select allowClear options={CONTACT_CHANNELS} placeholder="Tất cả" />
        </Form.Item>
        <Form.Item name="dateRange" label="Thời gian liên hệ">
          <RangePicker format="DD/MM/YYYY" />
        </Form.Item>
        <Form.Item className="aftercare-filter-actions">
          <Space>
            <Button type="primary" htmlType="submit">Lọc</Button>
            <Button onClick={resetCareFilter}>Xóa lọc</Button>
          </Space>
        </Form.Item>
      </Form>
      {careFilters.patientId && (
        <Alert
          type="info"
          showIcon
          message="Đang dùng API lịch sử chăm sóc theo UUID bệnh nhân; kênh và thời gian được áp dụng trên toàn bộ lịch sử trả về."
        />
      )}
      {careSource.error ? (
        <SourceAlert
          error={careSource.error}
          onRetry={() => loadCareLogs(careFilters, careSource.page, careSource.size)}
        />
      ) : (
        <CareLogTable
          records={careSource.items}
          loading={careSource.loading}
          pagination={{
            page: careSource.page,
            size: careSource.size,
            total: careSource.total,
            clientSide: careSource.clientSide,
          }}
          onPageChange={(page, size) => loadCareLogs(careFilters, page, size)}
          patientsById={patientsById}
          visitsById={visitsById}
          performerNames={performerNames}
          onOpenHistory={openHistory}
        />
      )}
    </Space>
  ) : <PermissionAlert />

  return (
    <main className="aftercare-page">
      <header className="aftercare-page-header">
        <div>
          <Text className="aftercare-eyebrow">Theo dõi sau điều trị</Text>
          <Title level={2}>Chăm sóc sau khám</Title>
          <Paragraph>
            Theo dõi lịch nhắc tái khám và quá trình chăm sóc bệnh nhân sau điều trị.
          </Paragraph>
        </div>
        <Space wrap className="aftercare-header-actions">
          <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefresh}>
            Làm mới
          </Button>
          {permissions.careCreate && (
            <Button icon={<PlusOutlined />} onClick={openIndependentCareLog}>
              Ghi nhận chăm sóc
            </Button>
          )}
          {permissions.reminderCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={openReminderModal}>
              Tạo lịch nhắc tái khám
            </Button>
          )}
        </Space>
      </header>

      {permissions.isReadOnly && (
        <Alert
          type="info"
          showIcon
          message="Chế độ giám sát (Quản trị viên)"
          description="Tài khoản Quản trị viên (Admin) có quyền xem và giám sát toàn bộ dữ liệu Chăm sóc sau khám, không có quyền tạo hoặc chỉnh sửa lịch nhắc."
          style={{ marginBottom: 16 }}
        />
      )}

      {patientsError && (
        <Alert
          className="aftercare-patient-alert"
          type="warning"
          showIcon
          message="Không thể tải danh mục bệnh nhân"
          description={getAftercareErrorMessage(patientsError)}
          action={<Button size="small" onClick={loadPatients}>Thử lại</Button>}
        />
      )}

      <Row gutter={[16, 16]} className="aftercare-stats">
        <Col xs={24} md={8}>
          <StatCard
            title="Lịch nhắc đến hạn"
            value={dueStat.value}
            loading={dueStat.loading}
            error={dueStat.error}
            allowed={permissions.reminderRead}
            icon={<BellOutlined />}
            tone="warning"
          />
        </Col>
        <Col xs={24} md={8}>
          <StatCard
            title="Nhật ký chăm sóc đã lưu"
            value={careStat.value}
            loading={careStat.loading}
            error={careStat.error}
            allowed={permissions.careRead}
            icon={<FileDoneOutlined />}
            tone="success"
          />
        </Col>
        <Col xs={24} md={8}>
          <StatCard
            title="Tổng số lịch nhắc"
            value={reminderStat.value}
            loading={reminderStat.loading}
            error={reminderStat.error}
            allowed={permissions.reminderRead}
            icon={<TeamOutlined />}
            tone="primary"
          />
        </Col>
      </Row>

      <Card className="aftercare-workspace">
        <Tabs
          defaultActiveKey={permissions.reminderRead ? 'due' : 'care'}
          items={[
            { key: 'due', label: 'ĐẾN HẠN CHĂM SÓC', children: duePanel },
            { key: 'reminders', label: 'TẤT CẢ LỊCH NHẮC', children: remindersPanel },
            { key: 'care', label: 'NHẬT KÝ CHĂM SÓC', children: carePanel },
          ]}
        />
      </Card>

      <FollowUpReminderModal
        open={reminderModalOpen}
        onCancel={() => setReminderModalOpen(false)}
        onSubmit={handleCreateReminder}
        submitting={submittingReminder}
        todayPatients={todayPatients}
        todayVisits={todayVisits}
        loadingTodayVisits={loadingTodayVisits}
        todayVisitsError={todayVisitsError}
        presetPatientId={isUuid(location.state?.patientId) ? location.state.patientId : undefined}
      />

      <CareLogModal
        open={careModalOpen}
        onCancel={() => {
          if (!submittingCareLog) {
            setCareModalOpen(false)
            setSourceReminder(null)
          }
        }}
        onSubmit={handleCreateCareLog}
        submitting={submittingCareLog}
        todayPatients={todayPatients}
        todayVisits={todayVisits}
        loadingTodayVisits={loadingTodayVisits}
        todayVisitsError={todayVisitsError}
        sourceReminder={sourceReminder}
        canReadReminders={permissions.reminderRead}
      />

      <PatientCareHistoryModal
        open={historyOpen}
        onCancel={() => setHistoryOpen(false)}
        patientId={historyPatientId}
        patient={patientsById[historyPatientId]}
        patientsById={patientsById}
        performerNames={performerNames}
      />
    </main>
  )
}

export default AfterCarePage
