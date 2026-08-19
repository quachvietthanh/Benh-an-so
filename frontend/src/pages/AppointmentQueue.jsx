import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Dropdown,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  TimePicker,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EllipsisOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FilterOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  MoreOutlined,
  PlusOutlined,
  ReloadOutlined,
  RightCircleOutlined,
  SearchOutlined,
  SendOutlined,
  StepForwardOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentApi from '../api/appointmentApi'
import patientApi from '../api/patientApi'
import queueApi from '../api/queueApi'
import userApi from '../api/userApi'
import { useAuthContext } from '../context/AuthContext'
import {
  APPOINTMENT_STATUS_META,
  QUEUE_STATUS_META,
  checkQueuePermissions,
  handleQueueApiError,
} from '../utils/queueHelpers'
import {
  getStoredAppointmentLogs,
  getStoredNotificationLogs,
  saveAppointmentLog,
  saveNotificationLog,
  saveStoredQueueItem,
} from '../utils/storageHelpers'

const { Text, Title, Paragraph } = Typography

const avatarPalette = [
  ['#e7f0ff', '#1c68ce'],
  ['#fff0e5', '#bf6b32'],
  ['#e8f7ef', '#21835a'],
  ['#f1eaff', '#7541b7'],
]

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const isUuid = (value) => UUID_PATTERN.test(String(value || ''))
const isDemoSession = () => localStorage.getItem('token') === 'demo-token'
const getDisplayStatus = (item) => {
  const meta = item?.type === 'appointment'
    ? APPOINTMENT_STATUS_META[item?.status]
    : QUEUE_STATUS_META[item?.status]
  return meta || { label: 'Không xác định', tone: 'default' }
}

const getInitials = (name = '') =>
  name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()

const getAvatarStyle = (seed = '') => {
  const paletteIndex =
    [...String(seed)].reduce((sum, character) => sum + character.charCodeAt(0), 0) % avatarPalette.length
  const [background, color] = avatarPalette[paletteIndex]
  return { background, color }
}

const normalizeQueueItem = (item = {}) => ({
  ...item,
  id: item.id || item.queueItemId,
  status: item.status || item.queueItemStatus,
  roomName: item.roomName || item.roomNumber,
})

const normalizeQueueList = (payload) => {
  const list = Array.isArray(payload) ? payload : (payload?.content || payload?.items || [])
  return list.map(normalizeQueueItem).filter((item) => item.id)
}

const replaceQueueItem = (items, queueItem) => {
  const list = Array.isArray(items) ? items : []
  const exists = list.some((item) => String(item.id) === String(queueItem.id))
  return exists
    ? list.map((item) => (String(item.id) === String(queueItem.id) ? queueItem : item))
    : [queueItem, ...list]
}

function AppointmentQueue() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  const permissions = useMemo(() => checkQueuePermissions(user?.roles || []), [user?.roles])

  const [activeMainTab, setActiveMainTab] = useState(() =>
    permissions.isDoctorOnly ? 'doctor_queue' : 'appointments',
  )
  const [selectedDate, setSelectedDate] = useState(dayjs())
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  const [appointments, setAppointments] = useState([])
  const [queues, setQueues] = useState([])
  const [myQueueData, setMyQueueData] = useState(null)
  const [patients, setPatients] = useState([])
  const [doctors, setDoctors] = useState([])
  const [appointmentLogs, setAppointmentLogs] = useState([])
  const [notificationLogs, setNotificationLogs] = useState([])

  const [appStatusFilter, setAppStatusFilter] = useState('ALL')
  const [appDoctorFilter, setAppDoctorFilter] = useState('ALL')
  const [appKeyword, setAppKeyword] = useState('')

  const [queueDoctorFilter, setQueueDoctorFilter] = useState('ALL')
  const [queueRoomFilter, setQueueRoomFilter] = useState('ALL')
  const [queueStatusFilter, setQueueStatusFilter] = useState('ALL')
  const [queueSourceFilter, setQueueSourceFilter] = useState('ALL')
  const [queueKeyword, setQueueKeyword] = useState('')

  const [bookModalOpen, setBookModalOpen] = useState(false)
  const [walkInModalOpen, setWalkInModalOpen] = useState(false)
  const [skipModalItem, setSkipModalItem] = useState(null)
  const [cancelModalItem, setCancelModalItem] = useState(null)
  const [detailItem, setDetailItem] = useState(null)
  const [quickPatientModalOpen, setQuickPatientModalOpen] = useState(false)
  const [quickPatientSaving, setQuickPatientSaving] = useState(false)
  const [logsDrawerOpen, setLogsDrawerOpen] = useState(false)

  const [bookForm] = Form.useForm()
  const [walkInForm] = Form.useForm()
  const [skipForm] = Form.useForm()
  const [cancelForm] = Form.useForm()
  const [quickPatientForm] = Form.useForm()

  const getDoctorInfo = useCallback((doctorId, fallbackName, fallbackDept) => {
    const cleanId = String(doctorId || '').toLowerCase().replace(/-/g, '')
    const doc = doctors.find((d) => {
      const dClean = String(d.id || '').toLowerCase().replace(/-/g, '')
      return (cleanId && dClean === cleanId) || String(d.id) === String(doctorId)
    })
    if (doc) {
      return {
        name: doc.fullName || doc.name || doc.username || fallbackName || 'BS. Chưa phân công',
        department: doc.department || fallbackDept || '—',
      }
    }
    return { name: fallbackName || 'Bác sĩ chưa xác định', department: fallbackDept || '—' }
  }, [doctors])

  const getPatientInfo = useCallback((patientId, fallbackName, fallbackCode, fallbackPhone) => {
    const cleanId = String(patientId || '').toLowerCase().replace(/-/g, '')
    const pat = patients.find((p) => {
      const pClean = String(p.id || '').toLowerCase().replace(/-/g, '')
      return (cleanId && pClean === cleanId) || String(p.id) === String(patientId)
    })
    if (pat) {
      return {
        name: pat.fullName || pat.name || fallbackName || 'Bệnh nhân',
        code: pat.patientCode || pat.code || fallbackCode || '—',
        phone: pat.phoneNumber || pat.phone || fallbackPhone || '',
      }
    }
    return { name: fallbackName || 'Bệnh nhân', code: fallbackCode || '—', phone: fallbackPhone || '' }
  }, [patients])

  const isSamePatient = useCallback((id1, id2) => {
    if (!id1 || !id2) return false
    return String(id1) === String(id2)
  }, [])

  const loadDirectories = useCallback(async () => {
    try {
      const [patientRes, doctorRes] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 500 }),
        userApi.getDoctors(),
      ])

      if (patientRes.status === 'fulfilled' && Array.isArray(patientRes.value?.data?.content || patientRes.value?.data)) {
        const list = patientRes.value?.data?.content || patientRes.value?.data
        setPatients(list)
      } else {
        setPatients([])
      }

      if (doctorRes.status === 'fulfilled' && Array.isArray(doctorRes.value?.data)) {
        setDoctors(doctorRes.value.data)
      } else {
        setDoctors([])
      }
    } catch {
      setPatients([])
      setDoctors([])
    }
  }, [])

  const loadAppointments = useCallback(async () => {
    try {
      const res = await appointmentApi.getAll({
        startDate: selectedDate.startOf('day').toISOString(),
        endDate: selectedDate.endOf('day').toISOString(),
      })
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
      const rawList = list

      const normalized = rawList.map((item) => {
        const timeVal = item.appointmentAt || item.startTime || item.date
        return {
          ...item,
          appointmentAt: timeVal,
        }
      })
      setAppointments(normalized)
    } catch {
      setAppointments([])
    }
  }, [selectedDate])

  const loadQueues = useCallback(async () => {
    try {
      const params = {
        date: selectedDate.format('YYYY-MM-DD'),
        doctorId: queueDoctorFilter !== 'ALL' ? queueDoctorFilter : undefined,
        roomId: queueRoomFilter !== 'ALL' ? queueRoomFilter : undefined,
      }
      const res = await queueApi.getQueues(params)
      setQueues(normalizeQueueList(res.data))
    } catch (err) {
      setQueues([])
      console.error('Error loading queue board:', err)
    }
  }, [selectedDate, queueDoctorFilter, queueRoomFilter])

  const loadMyQueue = useCallback(async () => {
    if (!permissions.canViewMyQueue) {
      setMyQueueData([])
      return
    }
    try {
      const res = await queueApi.getMyQueue({ date: selectedDate.format('YYYY-MM-DD') })
      setMyQueueData(normalizeQueueList(res.data))
    } catch (err) {
      setMyQueueData([])
      console.error('Error loading doctor queue:', err)
    }
  }, [selectedDate, permissions.canViewMyQueue])

  const loadLogs = useCallback(() => {
    setAppointmentLogs(getStoredAppointmentLogs())
    setNotificationLogs(getStoredNotificationLogs())
  }, [])

  const refreshAllData = useCallback(async () => {
    setLoading(true)
    try {
      const tasks = [loadAppointments()]
      if (!permissions.isDoctorOnly) {
        tasks.push(loadQueues())
      }
      if (permissions.canViewMyQueue) {
        tasks.push(loadMyQueue())
      }
      await Promise.allSettled(tasks)
      loadLogs()
    } catch (err) {
      console.error('Error refreshing queue data:', err)
    } finally {
      setLoading(false)
    }
  }, [loadAppointments, loadQueues, loadMyQueue, loadLogs, permissions.canViewMyQueue, permissions.isDoctorOnly])

  useEffect(() => {
    loadDirectories()
    refreshAllData()
  }, [loadDirectories, refreshAllData])

  useEffect(() => {
    if (location.state?.patientId) {
      setBookModalOpen(true)
      bookForm.setFieldsValue({
        patientId: location.state.patientId,
      })
    }
  }, [location.state, bookForm])

  useEffect(() => {
    const timer = setInterval(() => {
      if (!permissions.isDoctorOnly) loadQueues()
      if (permissions.canViewMyQueue) loadMyQueue()
    }, 20000)
    return () => clearInterval(timer)
  }, [loadQueues, loadMyQueue, permissions.canViewMyQueue, permissions.isDoctorOnly])

  const extractedRooms = useMemo(() => {
    const map = new Map()
    queues.forEach((q) => {
      const rId = q.roomId
      if (rId && !map.has(String(rId))) {
        map.set(String(rId), {
          id: rId,
          code: q.roomNumber || q.roomName || 'Phòng khám',
          name: q.roomName || q.roomNumber || 'Phòng khám',
        })
      }
    })
    return Array.from(map.values())
  }, [queues])

  const filteredAppointments = useMemo(() => {
    const kw = appKeyword.trim().toLowerCase()
    return appointments.filter((app) => {
      if (permissions.isDoctorOnly && String(app.doctorId) !== String(user?.id)) return false
      const pInfo = getPatientInfo(app.patientId, app.patientName, app.patientCode, app.phone)
      const dInfo = getDoctorInfo(app.doctorId, app.doctorName, app.department)

      const timeVal = app.appointmentAt || app.startTime || app.date
      const isDateMatch = !timeVal || dayjs(timeVal).isSame(selectedDate, 'day')
      const isDocMatch = appDoctorFilter === 'ALL' || String(app.doctorId) === String(appDoctorFilter) || String(dInfo.name) === String(appDoctorFilter)
      const isStatusMatch = appStatusFilter === 'ALL' || app.status === appStatusFilter
      const textMatch = !kw || [app.appointmentCode, pInfo.name, pInfo.code, pInfo.phone, dInfo.name, dInfo.department, app.reason]
        .some((t) => String(t || '').toLowerCase().includes(kw))

      return isDateMatch && isDocMatch && isStatusMatch && textMatch
    })
  }, [appointments, selectedDate, appDoctorFilter, appStatusFilter, appKeyword, getPatientInfo, getDoctorInfo, permissions.isDoctorOnly, user?.id])

  const filteredQueues = useMemo(() => {
    const kw = queueKeyword.trim().toLowerCase()

    const validItems = queues.filter((q) => {
      if (permissions.isDoctorOnly && String(q.doctorId) !== String(user?.id)) return false
      if (q.status === 'CANCELLED') return false
      if (queueStatusFilter !== 'ALL' && q.status !== queueStatusFilter) return false
      if (queueSourceFilter !== 'ALL' && q.sourceType !== queueSourceFilter) return false
      const pInfo = getPatientInfo(q.patientId, q.patientName, q.patientCode, q.phone)
      const dInfo = getDoctorInfo(q.doctorId, q.doctorName, q.department)
      const textMatch = !kw || [pInfo.name, pInfo.code, q.visitCode, dInfo.name, q.roomName, q.id]
        .some((t) => String(t || '').toLowerCase().includes(kw))
      return textMatch
    })

    return validItems.sort((first, second) =>
      Number(first.queueNumber || 999999) - Number(second.queueNumber || 999999),
    )
  }, [queues, queueKeyword, queueStatusFilter, queueSourceFilter, getPatientInfo, getDoctorInfo, permissions.isDoctorOnly, user?.id])

  const doctorQueueGroups = useMemo(() => {
    let items = []
    if (permissions.isDoctorOnly) {
      items = (Array.isArray(myQueueData) ? myQueueData : myQueueData?.items || myQueueData?.content) || queues.filter((q) => String(q.doctorId) === String(user?.id))
    } else {
      items = queueDoctorFilter === 'ALL'
        ? queues
        : queues.filter((q) => String(q.doctorId) === String(queueDoctorFilter) || String(q.doctorName) === String(queueDoctorFilter))
    }
    const sortByNumber = (list) => [...list].sort((a, b) => Number(a.queueNumber || 999999) - Number(b.queueNumber || 999999))
    return {
      inProgress: sortByNumber(items.filter((q) => q.status === 'IN_PROGRESS')),
      waiting: sortByNumber(items.filter((q) => q.status === 'WAITING')),
      waitingForResult: sortByNumber(items.filter((q) => q.status === 'WAITING_FOR_RESULT')),
      finished: sortByNumber(items.filter((q) => ['COMPLETED', 'SKIPPED', 'CANCELLED'].includes(q.status))),
    }
  }, [permissions.isDoctorOnly, myQueueData, queues, user?.id, queueDoctorFilter])

  const handleCreateAppointmentSubmit = async (values) => {
    setActionLoading(true)
    try {
      const appointmentTime = values.appointmentAt
        ? dayjs(values.appointmentAt)
        : dayjs(`${values.appointmentDate.format('YYYY-MM-DD')} ${values.appointmentTime.format('HH:mm')}`)

      if (appointmentTime.isBefore(dayjs().subtract(5, 'minute'))) {
        message.error('Hệ thống không cho đặt vào thời điểm đã qua (Khung giờ ở quá khứ)!')
        setActionLoading(false)
        return
      }

      const doctorId = values.doctorId
      const isDoctorBusy = appointments.some((app) => {
        if (['CANCELLED', 'NO_SHOW'].includes(app.status)) return false
        if (String(app.doctorId) !== String(doctorId)) return false
        const existingTime = dayjs(app.appointmentAt || app.startTime || app.date)
        return existingTime.isSame(appointmentTime, 'day') && Math.abs(existingTime.diff(appointmentTime, 'minute')) < 30
      })

      if (isDoctorBusy) {
        message.error('Hệ thống từ chối: Bác sĩ đã có lịch trong cùng khung giờ!')
        setActionLoading(false)
        return
      }

      const pInfo = getPatientInfo(values.patientId)
      const dInfo = getDoctorInfo(values.doctorId)
      const apiPayload = {
        patientId: values.patientId,
        doctorId: values.doctorId,
        startTime: appointmentTime.toISOString(),
        endTime: appointmentTime.add(30, 'minute').toISOString(),
        reason: values.reason || 'Khám bệnh',
      }
      const response = await appointmentApi.create(apiPayload)
      const newAppointment = response.data
      if (!newAppointment?.id || !newAppointment?.appointmentCode) {
        throw new Error('Backend không trả id và appointmentCode của lịch hẹn.')
      }

      saveAppointmentLog({
        appointmentId: newAppointment.id,
        appointmentCode: newAppointment.appointmentCode,
        action: 'CREATE',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Lễ tân chọn bệnh nhân ${pInfo.name}, bác sĩ ${dInfo.name} và khung giờ ${appointmentTime.format('HH:mm DD/MM/YYYY')} -> Lịch hẹn ở trạng thái ĐÃ ĐẶT`,
      })

      message.success(`Đặt lịch hẹn thành công: ${newAppointment.appointmentCode} (Trạng thái: ĐÃ ĐẶT)`)
      setBookModalOpen(false)
      bookForm.resetFields()
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể tạo lịch hẹn. Vui lòng kiểm tra lại thông tin và thử lại.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCancelAppointmentSubmit = async (values) => {
    if (!cancelModalItem) return
    const app = cancelModalItem

    if (['COMPLETED', 'CANCELLED'].includes(app.status)) {
      message.error('Hệ thống không cho hủy: Lịch hẹn đã hoàn tất hoặc đã hủy!')
      setCancelModalItem(null)
      return
    }

    setActionLoading(true)
    try {
      const reason = values.reason || 'Bệnh nhân báo bận'
      await appointmentApi.cancel(app.id, reason)
      saveAppointmentLog({
        appointmentId: app.id,
        appointmentCode: app.appointmentCode || 'Chưa có mã',
        action: 'CANCEL',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Lễ tân hủy lịch hẹn. Lý do: ${reason}. Khung giờ được giải phóng.`,
      })

      message.success(`Lịch hẹn ${app.appointmentCode || ''} đã chuyển sang ĐÃ HỦY thành công!`)
      setCancelModalItem(null)
      cancelForm.resetFields()
      refreshAllData()
    } catch {
      message.error('Không thể hủy lịch hẹn!')
    } finally {
      setActionLoading(false)
    }
  }

  const handleMarkNoShow = async (record) => {
    const isAlreadyCheckedIn = record.status === 'CHECKED_IN' || queues.some((q) => String(q.patientId) === String(record.patientId) && ['WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(q.status))

    if (isAlreadyCheckedIn) {
      message.error('Không thể đánh dấu: bệnh nhân đã được tiếp nhận trước đó.')
      return
    }

    const timeVal = record.appointmentAt || record.startTime || record.date
    const appTime = dayjs(timeVal)
    const isOverdue15Min = appTime.isValid() && dayjs().isAfter(appTime.add(15, 'minute'))

    if (!isOverdue15Min) {
      message.warning('Chưa đủ điều kiện: Chỉ được đánh dấu KHÔNG ĐẾN khi bệnh nhân quá 15 phút so với giờ hẹn!')
      return
    }

    const pInfo = getPatientInfo(record.patientId, record.patientName)
    Modal.confirm({
      title: 'Xác nhận đánh dấu Bệnh nhân KHÔNG ĐẾN?',
      content: `Bệnh nhân ${pInfo.name} (${record.appointmentCode}) đã quá 15 phút so với giờ hẹn (${appTime.format('HH:mm DD/MM/YYYY')}) mà chưa đến tiếp nhận. Bạn có muốn chuyển lịch sang trạng thái KHÔNG ĐẾN?`,
      okText: 'Xác nhận Đánh dấu Không Đến',
      okType: 'danger',
      cancelText: 'Hủy',
      onOk: async () => {
        setActionLoading(true)
        try {
          await appointmentApi.noShow(record.id)
          saveAppointmentLog({
            appointmentId: record.id,
            appointmentCode: record.appointmentCode,
            action: 'NO_SHOW',
            operatorName: user?.fullName || user?.username || 'Lễ tân',
            details: `Đánh dấu bệnh nhân không đến (Đã quá 15 phút so với giờ hẹn ${appTime.format('HH:mm DD/MM/YYYY')}).`,
          })

          message.success('Lịch hẹn đã chuyển sang trạng thái KHÔNG ĐẾN')
          refreshAllData()
        } catch {
          message.error('Không thể đánh dấu không đến')
        } finally {
          setActionLoading(false)
        }
      },
    })
  }

  const handleCheckInAppointment = async (appId) => {
    const app = appointments.find((a) => String(a.id) === String(appId))
    const isAlreadyInQueue = queues.some((q) => isSamePatient(q.patientId, app?.patientId) && ['WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(q.status))

    if (isAlreadyInQueue) {
      message.warning('Bệnh nhân này hiện đã có một lượt khám hoặc đang ở trong hàng đợi khám, hệ thống không thêm trùng!')
      return
    }

    setActionLoading(true)
    try {
      const pInfo = getPatientInfo(app?.patientId, app?.patientName)
      const dInfo = getDoctorInfo(app?.doctorId, app?.doctorName, app?.department)
      const res = await queueApi.checkInAppointment(appId)
      const backendItem = normalizeQueueItem(res?.data)
      if (!backendItem.id || !backendItem.visitId) {
        throw new Error('Backend did not return queueItemId and visitId after check-in.')
      }

      const newQueueItem = {
        ...backendItem,
        id: backendItem.id,
        patientId: backendItem.patientId || app?.patientId,
        patientCode: pInfo.code,
        patientName: backendItem.patientName || pInfo.name,
        phone: pInfo.phone,
        doctorId: backendItem.doctorId || app?.doctorId,
        doctorName: backendItem.doctorName || dInfo.name,
        department: dInfo.department,
        roomName: backendItem.roomName || 'Phòng khám',
        sourceType: backendItem.sourceType || 'APPOINTMENT',
      }
      saveStoredQueueItem(newQueueItem)
      setQueues((prev) => replaceQueueItem(prev, newQueueItem))

      saveAppointmentLog({
        appointmentId: appId,
        appointmentCode: app?.appointmentCode || '',
        action: 'CHECK_IN',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Tiếp nhận bệnh nhân ${pInfo.name} và đưa vào hàng đợi khám.`,
      })

      message.success(`Đã Check-in thành công cho bệnh nhân ${pInfo.name}! Bệnh nhân đã xuất hiện trong Hàng Đợi Khám.`)
      await refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể tiếp nhận lịch hẹn')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCheckInWalkInSubmit = async (values) => {
    const isAlreadyInQueue = queues.some((q) => isSamePatient(q.patientId, values.patientId) && ['WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(q.status))

    if (isAlreadyInQueue) {
      message.warning('Bệnh nhân này hiện đã có một lượt khám hoặc đang ở trong hàng đợi khám, hệ thống không thêm trùng!')
      return
    }

    setActionLoading(true)
    try {
      const payload = {
        patientId: values.patientId,
        doctorId: values.doctorId,
        reason: values.reason || 'Khám Walk-in',
        note: values.notes || '',
      }

      const apiRes = await queueApi.checkInWalkIn(payload)
      const backendItem = normalizeQueueItem(apiRes?.data)
      if (!backendItem.id || !backendItem.visitId) {
        throw new Error('Backend did not return queueItemId and visitId after walk-in check-in.')
      }

      const pInfo = getPatientInfo(values.patientId)
      const dInfo = getDoctorInfo(values.doctorId)

      const walkInQueueItem = {
        ...backendItem,
        id: backendItem.id,
        patientId: backendItem.patientId || values.patientId,
        patientCode: pInfo.code,
        patientName: backendItem.patientName || pInfo.name,
        phone: pInfo.phone || '',
        doctorId: backendItem.doctorId || values.doctorId,
        doctorName: backendItem.doctorName || dInfo.name,
        department: dInfo.department,
        roomName: backendItem.roomName || 'Phòng khám',
        sourceType: backendItem.sourceType || 'WALK_IN',
      }
      saveStoredQueueItem(walkInQueueItem)
      setQueues((prev) => replaceQueueItem(prev, walkInQueueItem))

      saveAppointmentLog({
        appointmentId: backendItem.appointmentId || backendItem.id,
        appointmentCode: backendItem.visitCode,
        action: 'WALK_IN_CHECKIN',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Tiếp nhận Walk-in bệnh nhân ${pInfo.name} -> Tạo lượt khám ${backendItem.visitCode} & đưa vào Hàng đợi khám.`,
      })

      message.success(`Đã tiếp nhận bệnh nhân ${pInfo.name} vào lượt khám ${backendItem.visitCode}.`)
      setWalkInModalOpen(false)
      walkInForm.resetFields()
      await refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể tiếp nhận bệnh nhân tự đến')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCallNext = async (queueId) => {
    const myQueueList = permissions.isDoctorOnly
      ? (Array.isArray(myQueueData) ? myQueueData : myQueueData?.items || myQueueData?.content) || queues.filter((q) => String(q.doctorId) === String(user?.id))
      : (queueDoctorFilter === 'ALL' ? queues : queues.filter((q) => String(q.doctorId) === String(queueDoctorFilter) || String(q.doctorName) === String(queueDoctorFilter)))
    const waitingItems = queues.filter((q) => q.status === 'WAITING')
    const myWaiting = myQueueList.filter((q) => q.status === 'WAITING')

    if (waitingItems.length === 0 && myWaiting.length === 0) {
      message.warning('Hàng đợi hiện chưa có bệnh nhân. Vui lòng tiếp nhận bệnh nhân trước khi gọi khám.')
      return
    }

    setActionLoading(true)
    try {
      const targetItem = myWaiting[0] || waitingItems[0] || myQueueList[0] || queues[0]
      const qId = queueId || myQueueData?.id || myQueueList[0]?.medicalQueueId || targetItem?.medicalQueueId || targetItem?.queueId
      if (!qId) {
        throw new Error('Không xác định được hàng đợi cần gọi.')
      }

      const response = await queueApi.callNext(qId)
      const calledItem = normalizeQueueItem(response?.data)
      if (!calledItem.id) {
        throw new Error('Backend did not return the called queue item.')
      }

      setQueues((prev) => replaceQueueItem(prev, calledItem))
      setMyQueueData((prev) => replaceQueueItem(prev, calledItem))
      saveStoredQueueItem(calledItem)
      const pInfo = getPatientInfo(calledItem.patientId, calledItem.patientName)
      message.info({
        content: `🔊 THÔNG BÁO GỌI KHÁM: Bác sĩ gọi bệnh nhân ${pInfo.name} (STT: ${calledItem.queueNumber || 1}) vào khám!`,
        duration: 5,
      })
      await refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể gọi lượt khám tiếp theo')
    } finally {
      setActionLoading(false)
    }
  }

  const handleSendReminder = async (record) => {
    if (record.status === 'CANCELLED') {
      message.error('Hệ thống không gửi nhắc: Lịch hẹn đã ở trạng thái ĐÃ HỦY!')
      return
    }

    setActionLoading(true)
    try {
      const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
      const dInfo = getDoctorInfo(record.doctorId, record.doctorName)
      const timeVal = record.appointmentAt || record.startTime || record.date

      const msgText = `[Nhắc lịch hẹn] Kính gửi bệnh nhân ${pInfo.name}, quý khách có lịch hẹn khám với ${dInfo.name} vào lúc ${timeVal ? dayjs(timeVal).format('HH:mm DD/MM/YYYY') : 'khung giờ hẹn'}. Vui lòng có mặt trước 15 phút.`

      await appointmentApi.sendReminder(record.id)

      saveNotificationLog({
        appointmentId: record.id,
        patientName: pInfo.name,
        phone: pInfo.phone || '',
        channel: 'SMS / Zalo / System',
        message: msgText,
        status: 'SENT',
      })

      message.success(`Đã gửi nhắc lịch hẹn cho bệnh nhân ${pInfo.name}!`)
      loadLogs()
    } catch {
      message.error('Không thể gửi nhắc lịch hẹn!')
    } finally {
      setActionLoading(false)
    }
  }

  const handleQuickRegisterPatientSubmit = async (values) => {
    setQuickPatientSaving(true)
    try {
      const payload = {
        fullName: values.fullName,
        phone: values.phone,
        gender: values.gender,
        dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'),
        address: values.address || '',
      }

      const response = await patientApi.create(payload)
      const created = response.data
      if (!created?.id || !created?.patientCode) {
        throw new Error('Backend không trả id và patientCode của bệnh nhân.')
      }

      setPatients((prev) => [created, ...prev.filter((patient) => patient.id !== created.id)])
      message.success(`Đã đăng ký bệnh nhân mới thành công: ${created.fullName} (${created.patientCode})`)

      if (bookModalOpen) {
        bookForm.setFieldsValue({ patientId: created.id })
      }
      if (walkInModalOpen) {
        walkInForm.setFieldsValue({ patientId: created.id })
      }
      setQuickPatientModalOpen(false)
      quickPatientForm.resetFields()
    } catch {
      message.error('Không thể tạo bệnh nhân mới!')
    } finally {
      setQuickPatientSaving(false)
    }
  }

  const handleSkipSubmit = async (values) => {
    if (!skipModalItem) return
    setActionLoading(true)
    const reason = values.reason || 'Vắng mặt khi gọi'
    try {
      if (skipModalItem.status !== 'IN_PROGRESS') {
        throw new Error('Chỉ có thể bỏ qua queue item đang IN_PROGRESS.')
      }

      const response = await queueApi.skip(skipModalItem.id, reason)
      const updatedItem = normalizeQueueItem(response?.data)
      if (!updatedItem.id) {
        throw new Error('Backend did not return the skipped queue item.')
      }

      setQueues((prev) => replaceQueueItem(prev, updatedItem))
      setMyQueueData((prev) => replaceQueueItem(prev, updatedItem))
      saveStoredQueueItem(updatedItem)
      message.success('Đã chuyển bệnh nhân vào danh sách bỏ qua.')
      setSkipModalItem(null)
      skipForm.resetFields()
      await refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể thực hiện bỏ qua lượt khám')
    } finally {
      setActionLoading(false)
    }
  }

  const handleUpdateItemStatus = async (itemId, newStatus) => {
    setActionLoading(true)
    try {
      const response = await queueApi.updateStatus(itemId, newStatus)
      const updatedItem = normalizeQueueItem(response?.data)
      if (!updatedItem.id) {
        throw new Error('Backend did not return the updated queue item.')
      }

      setQueues((prev) => replaceQueueItem(prev, updatedItem))
      setMyQueueData((prev) => replaceQueueItem(prev, updatedItem))
      saveStoredQueueItem(updatedItem)
      await refreshAllData()
    } catch (err) {
      const targetStatusLabel = QUEUE_STATUS_META[newStatus]?.label || 'trạng thái được yêu cầu'
      handleQueueApiError(err, `Không thể chuyển lượt khám sang ${targetStatusLabel}`)
    } finally {
      setActionLoading(false)
    }
  }

  const openEncounter = (item) => {
    if (!item?.visitId) {
      message.error('Lượt khám chưa có visitId từ backend.')
      return
    }
    navigate(`/medical-records/visits/${item.visitId}`, {
      state: {
        patientId: item.patientId,
        visitId: item.visitId,
        queueItemId: item.id,
      },
    })
  }

  const handleCompleteItem = async (itemId) => {
    setActionLoading(true)
    try {
      const response = await queueApi.complete(itemId)
      const completedItem = normalizeQueueItem(response?.data)
      if (!completedItem.id) {
        throw new Error('Backend did not return the completed queue item.')
      }

      setQueues((prev) => replaceQueueItem(prev, completedItem))
      setMyQueueData((prev) => replaceQueueItem(prev, completedItem))
      saveStoredQueueItem(completedItem)

      message.success('Đã hoàn tất chu trình khám bệnh cho bệnh nhân.')
      await refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Bệnh án chưa được khóa hoặc sai chu trình khám.')
    } finally {
      setActionLoading(false)
    }
  }

  const appointmentColumns = [
    {
      title: 'Mã lịch hẹn',
      dataIndex: 'appointmentCode',
      key: 'appointmentCode',
      render: (code) => <Text strong style={{ color: '#2563eb' }}>{code || 'Chưa có'}</Text>,
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      width: 240,
      render: (_, record) => {
        const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
        return (
          <Space align="center" size="small">
            <Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 150 }}>
              <Text strong style={{ fontSize: 14, color: '#0f172a', lineHeight: '1.4' }}>{pInfo.name}</Text>
              <Text type="secondary" style={{ fontSize: 12, lineHeight: '1.2' }}>Mã: {pInfo.code}</Text>
            </div>
          </Space>
        )
      },
    },
    {
      title: 'Bác sĩ & Chuyên khoa',
      dataIndex: 'doctorName',
      key: 'doctorName',
      render: (_, record) => {
        const dInfo = getDoctorInfo(record.doctorId, record.doctorName, record.department)
        return (
          <div>
            <Text strong style={{ color: '#0f172a', display: 'block' }}>{dInfo.name}</Text>
            <Tag color="cyan">{dInfo.department}</Tag>
          </div>
        )
      },
    },
    {
      title: 'Khung giờ hẹn',
      dataIndex: 'appointmentAt',
      key: 'appointmentAt',
      render: (at, record) => {
        const timeVal = at || record.startTime || record.date
        const appTime = dayjs(timeVal)
        const timeStr = timeVal ? appTime.format('HH:mm - DD/MM/YYYY') : record.slot || 'Trong ngày'
        const isOverdue15Min = timeVal && dayjs().isAfter(appTime.add(15, 'minute')) && record.status === 'SCHEDULED'
        return (
          <Space direction="vertical" size={2}>
            <Text>{timeStr}</Text>
            {isOverdue15Min && <Tag color="error">Quá 15p giờ hẹn</Tag>}
          </Space>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = APPOINTMENT_STATUS_META[st] || { label: 'Không xác định', tone: 'gray' }
        return <Tag color={meta.tone}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 150,
      render: (_, record) => {
        const timeVal = record.appointmentAt || record.startTime || record.date
        const appTime = dayjs(timeVal)
        const isOverdue15Min = timeVal && dayjs().isAfter(appTime.add(15, 'minute'))

        const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
        const dInfo = getDoctorInfo(record.doctorId, record.doctorName, record.department)

        const openDetail = () => {
          setDetailItem({
            type: 'appointment',
            ...record,
            patientName: pInfo.name,
            doctorName: dInfo.name,
            department: dInfo.department,
          })
        }

        const menuItems = [
          {
            key: 'detail',
            icon: <EyeOutlined />,
            label: 'Xem chi tiết lịch hẹn',
            onClick: openDetail,
          },
          record.status === 'SCHEDULED' && permissions.canCheckIn && {
            key: 'checkin',
            icon: <CheckCircleOutlined />,
            label: 'Tiếp nhận khám (Check-in)',
            onClick: () => handleCheckInAppointment(record.id),
          },
          record.status !== 'CANCELLED' && {
            key: 'remind',
            icon: <SendOutlined />,
            label: 'Gửi nhắc lịch hẹn',
            onClick: () => handleSendReminder(record),
          },
          ['SCHEDULED', 'CONFIRMED'].includes(record.status) && {
            key: 'noshow',
            icon: <CloseCircleOutlined />,
            disabled: !isOverdue15Min || record.status === 'CHECKED_IN',
            label: 'Đánh dấu Không đến (Quá 15p)',
            onClick: () => handleMarkNoShow(record),
          },
          ['SCHEDULED', 'CONFIRMED'].includes(record.status) && {
            type: 'divider',
          },
          ['SCHEDULED', 'CONFIRMED'].includes(record.status) && {
            key: 'cancel',
            icon: <CloseCircleOutlined />,
            danger: true,
            label: 'Hủy lịch hẹn này',
            onClick: () => {
              cancelForm.setFieldsValue({ reason: 'Bệnh nhân báo bận' })
              setCancelModalItem(record)
            },
          },
        ].filter(Boolean)

        return (
          <Space size="small">
            <Button
              size="small"
              type="primary"
              ghost
              icon={<EyeOutlined />}
              onClick={openDetail}
            >
              Chi tiết
            </Button>
            <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
              <Button size="small" icon={<EllipsisOutlined />} title="Thao tác khác" />
            </Dropdown>
          </Space>
        )
      },
    },
  ]

  const queueBoardColumns = [
    {
      title: 'STT',
      dataIndex: 'queueNumber',
      key: 'queueNumber',
      width: 70,
      render: (num, _, idx) => <Badge count={num || idx + 1} style={{ backgroundColor: '#2563eb' }} />,
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      width: 240,
      render: (_, record) => {
        const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
        return (
          <Space align="center" size="small">
            <Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 150 }}>
              <Text strong style={{ fontSize: 14, color: '#0f172a', lineHeight: '1.4' }}>{pInfo.name}</Text>
              <Text type="secondary" style={{ fontSize: 12, lineHeight: '1.2' }}>Mã: {pInfo.code}</Text>
            </div>
          </Space>
        )
      },
    },
    {
      title: 'Mã lượt khám',
      dataIndex: 'visitCode',
      key: 'visitCode',
      width: 140,
      render: (val, record) => {
        const rawCode = val || record.visitId || record.id || 'Chưa có'
        let displayCode = rawCode
        if (displayCode.length > 20) {
          displayCode = `VIS-${String(rawCode).slice(-6).toUpperCase()}`
        }
        return <Text code style={{ whiteSpace: 'nowrap', display: 'inline-block', fontSize: 13 }}>{displayCode}</Text>
      },
    },
    {
      title: 'Nguồn',
      dataIndex: 'sourceType',
      key: 'sourceType',
      render: (src) => (
        src === 'WALK_IN'
          ? <Tag color="orange">Bệnh nhân tự đến</Tag>
          : <Tag color="blue">Hẹn trước</Tag>
      ),
    },
    {
      title: 'Bác sĩ',
      dataIndex: 'doctorName',
      key: 'doctorName',
      render: (_, record) => {
        const dInfo = getDoctorInfo(record.doctorId, record.doctorName, record.department)
        return dInfo.name
      },
    },
    {
      title: 'Phòng',
      dataIndex: 'roomName',
      key: 'roomName',
      render: (room, record) => room || record.roomNumber || record.roomCode || 'Chưa phân phòng',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = QUEUE_STATUS_META[st] || { label: 'Không xác định', tone: 'gray' }
        return <Tag color={meta.tone}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thời gian đến',
      dataIndex: 'checkedInAt',
      key: 'checkedInAt',
      render: (time) => (time ? dayjs(time).format('HH:mm DD/MM/YYYY') : '—'),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 150,
      render: (_, record) => {
        const pInfo = getPatientInfo(record.patientId, record.patientName)
        const dInfo = getDoctorInfo(record.doctorId, record.doctorName)

        const openDetail = () => {
          setDetailItem({
            type: 'queue',
            ...record,
            patientName: pInfo.name,
            doctorName: dInfo.name,
          })
        }

        const menuItems = [
          {
            key: 'detail',
            icon: <EyeOutlined />,
            label: 'Xem chi tiết lượt khám',
            onClick: openDetail,
          },
          permissions.canCallNext && record.status === 'WAITING' && {
            key: 'call',
            icon: <StepForwardOutlined />,
            label: 'Gọi vào khám ngay',
            onClick: () => handleCallNext(record.medicalQueueId || record.queueId || record.id),
          },
          permissions.canUpdateStatus && record.status === 'IN_PROGRESS' && {
            key: 'wait_cdls',
            label: 'Chuyển sang Chờ kết quả CĐLS',
            onClick: () => handleUpdateItemStatus(record.id, 'WAITING_FOR_RESULT'),
          },
          permissions.canUpdateStatus && record.status === 'WAITING_FOR_RESULT' && {
            key: 'resume',
            label: 'Tiếp tục khám bệnh',
            onClick: () => handleUpdateItemStatus(record.id, 'IN_PROGRESS'),
          },
          permissions.canSkip && record.status === 'IN_PROGRESS' && {
            type: 'divider',
          },
          permissions.canSkip && record.status === 'IN_PROGRESS' && {
            key: 'skip',
            icon: <CloseCircleOutlined />,
            danger: true,
            label: 'Bỏ qua lượt (Vắng mặt)',
            onClick: () => {
              skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
              setSkipModalItem(record)
            },
          },
        ].filter(Boolean)

        return (
          <Space size="small">
            {permissions.canCallNext && record.status === 'WAITING' ? (
              <Button
                type="primary"
                size="small"
                icon={<StepForwardOutlined />}
                onClick={() => handleCallNext(record.medicalQueueId || record.queueId || record.id)}
              >
                Gọi khám
              </Button>
            ) : (
              <Button
                size="small"
                type="primary"
                ghost
                icon={<EyeOutlined />}
                onClick={openDetail}
              >
                Chi tiết
              </Button>
            )}
            <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
              <Button size="small" icon={<EllipsisOutlined />} title="Thao tác khác" />
            </Dropdown>
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ padding: 24, maxWidth: 1400, margin: '0 auto' }}>
      <Card style={{ marginBottom: 24, borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space align="center" size="middle">
              <Avatar size={48} icon={<CalendarOutlined />} style={{ backgroundColor: '#2563eb' }} />
              <div>
                <Title level={4} style={{ margin: 0 }}>Quản Lý Lịch Hẹn & Hàng Đợi Khám Bệnh</Title>
                <Text type="secondary">
                  Theo dõi và điều phối danh sách khám bệnh theo thời gian thực.
                </Text>
              </div>
            </Space>
          </Col>
          <Col>
            <Space wrap>
              <DatePicker
                value={selectedDate}
                onChange={(date) => date && setSelectedDate(date)}
                format="DD/MM/YYYY"
                allowClear={false}
              />
              <Button icon={<ReloadOutlined />} onClick={refreshAllData} loading={loading}>
                Làm mới
              </Button>
              <Button icon={<HistoryOutlined />} onClick={() => setLogsDrawerOpen(true)}>
                Nhật ký & Thông báo
              </Button>
              {permissions.canBook && (
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setBookModalOpen(true)}
                  style={{ backgroundColor: '#2563eb' }}
                >
                  + Đặt lịch hẹn mới
                </Button>
              )}
              {permissions.canManageWalkIn && (
                <Button
                  type="primary"
                  icon={<UserAddOutlined />}
                  onClick={() => setWalkInModalOpen(true)}
                  style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }}
                >
                  + Tiếp nhận tự đến
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      <Tabs
        activeKey={activeMainTab}
        onChange={setActiveMainTab}
        type="card"
        items={[
          {
            key: 'appointments',
            label: (
              <span>
                <CalendarOutlined /> Danh sách Lịch Hẹn ({filteredAppointments.length})
              </span>
            ),
            children: (
              <Card style={{ borderRadius: 12 }}>
                <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={8} md={6}>
                    <Input
                      placeholder="Tìm mã LH, tên bệnh nhân..."
                      prefix={<SearchOutlined />}
                      value={appKeyword}
                      onChange={(e) => setAppKeyword(e.target.value)}
                      allowClear
                    />
                  </Col>
                  <Col xs={12} sm={8} md={6}>
                    <Select
                      style={{ width: '100%' }}
                      value={appDoctorFilter}
                      onChange={setAppDoctorFilter}
                      options={
                        permissions.isDoctorOnly
                          ? [{ value: 'ALL', label: `Bác sĩ: ${user?.fullName || user?.username || 'Bạn'}` }]
                          : [
                            { value: 'ALL', label: 'Tất cả Bác sĩ' },
                            ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                          ]
                      }
                      disabled={permissions.isDoctorOnly}
                    />
                  </Col>
                  <Col xs={12} sm={8} md={6}>
                    <Select
                      style={{ width: '100%' }}
                      value={appStatusFilter}
                      onChange={setAppStatusFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả trạng thái' },
                        { value: 'SCHEDULED', label: 'Đã đặt hẹn' },
                        { value: 'CHECKED_IN', label: 'Đã tiếp nhận' },
                        { value: 'NO_SHOW', label: 'Không đến khám' },
                        { value: 'COMPLETED', label: 'Đã hoàn thành' },
                        { value: 'CANCELLED', label: 'Đã hủy' },
                      ]}
                    />
                  </Col>
                </Row>

                <Table
                  dataSource={filteredAppointments}
                  columns={appointmentColumns}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 10, showSizeChanger: true }}
                />
              </Card>
            ),
          },
          {
            key: 'reception_queue',
            label: (
              <span>
                <TeamOutlined /> Hàng Đợi Khám (Lễ Tân & Điều Dưỡng) ({filteredQueues.length})
              </span>
            ),
            children: (
              <Card style={{ borderRadius: 12 }}>
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={6} md={5}>
                    <Input
                      placeholder="Tìm tên, mã BN, mã lượt..."
                      prefix={<SearchOutlined />}
                      value={queueKeyword}
                      onChange={(e) => setQueueKeyword(e.target.value)}
                      allowClear
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueDoctorFilter}
                      onChange={setQueueDoctorFilter}
                      options={
                        permissions.isDoctorOnly
                          ? [{ value: 'ALL', label: `Bác sĩ: ${user?.fullName || user?.username || 'Bạn'}` }]
                          : [
                            { value: 'ALL', label: 'Tất cả Bác sĩ' },
                            ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                          ]
                      }
                      disabled={permissions.isDoctorOnly}
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueRoomFilter}
                      onChange={setQueueRoomFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Phòng khám' },
                        ...extractedRooms.map((r) => ({ value: r.id, label: `${r.code} - ${r.name}` })),
                      ]}
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueStatusFilter}
                      onChange={setQueueStatusFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Trạng thái' },
                        { value: 'WAITING', label: 'Chờ khám' },
                        { value: 'IN_PROGRESS', label: 'Đang khám' },
                        { value: 'WAITING_FOR_RESULT', label: 'Chờ kết quả CĐLS' },
                        { value: 'COMPLETED', label: 'Đã hoàn thành' },
                        { value: 'SKIPPED', label: 'Đã bỏ qua (Vắng mặt)' },
                        { value: 'CANCELLED', label: 'Đã hủy' },
                      ]}
                    />
                  </Col>
                  <Col xs={12} sm={6} md={4}>
                    <Select
                      style={{ width: '100%' }}
                      value={queueSourceFilter}
                      onChange={setQueueSourceFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Nguồn' },
                        { value: 'APPOINTMENT', label: 'Hẹn trước' },
                        { value: 'WALK_IN', label: 'Bệnh nhân tự đến' },
                        { value: 'APPOINTMENT', label: 'Hẹn trước' },
                        { value: 'WALK_IN', label: 'Bệnh nhân tự đến' },
                      ]}
                    />
                  </Col>
                  <Col xs={24} sm={24} md={3} style={{ textAlign: 'right' }}>
                    {permissions.canCallNext && (
                      <Button
                        type="primary"
                        icon={<StepForwardOutlined />}
                        loading={actionLoading}
                        onClick={() => handleCallNext()}
                      >
                        Gọi tiếp
                      </Button>
                    )}
                  </Col>
                </Row>

                {filteredQueues.length === 0 ? (
                  <Empty
                    description="Hàng đợi hiện đang trống. Bệnh nhân sau khi tiếp nhận sẽ xuất hiện tại đây theo thứ tự."
                    style={{ margin: '40px 0' }}
                  />
                ) : (
                  <Table
                    dataSource={filteredQueues}
                    columns={queueBoardColumns}
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10, showSizeChanger: true }}
                  />
                )}
              </Card>
            ),
          },
          {
            key: 'doctor_queue',
            label: (
              <span>
                <UserSwitchOutlined /> Hàng Đợi Khám Phòng Bác Sĩ
              </span>
            ),
            children: (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {!permissions.isDoctorOnly && (
                  <Card style={{ borderRadius: 12, backgroundColor: '#f8fafc', borderColor: '#e2e8f0' }} bodyStyle={{ padding: 12 }}>
                    <Row gutter={[16, 16]} align="middle">
                      <Col xs={24} sm={12} md={8}>
                        <Text strong style={{ marginRight: 8 }}>Bác sĩ phụ trách (Hôm nay):</Text>
                        <Select
                          style={{ width: '220px' }}
                          value={queueDoctorFilter}
                          onChange={setQueueDoctorFilter}
                          options={[
                            { value: 'ALL', label: 'Tất cả Bác sĩ (Hôm nay)' },
                            ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                          ]}
                        />
                      </Col>
                      <Col xs={24} sm={12} md={16}>
                        <Text type="secondary">Quản trị viên và lễ tân có thể xem toàn bộ bệnh nhân hoặc lọc theo bác sĩ phụ trách.</Text>
                      </Col>
                    </Row>
                  </Card>
                )}
                <Card
                  title={<Text strong style={{ color: '#16a34a' }}>🔴 BỆNH NHÂN ĐANG KHÁM ({doctorQueueGroups.inProgress.length})</Text>}
                  style={{ borderRadius: 12, borderColor: '#bbf7d0' }}
                >
                  {doctorQueueGroups.inProgress.length === 0 ? (
                    <Text type="secondary">Chưa có bệnh nhân nào đang trong phòng khám.</Text>
                  ) : (
                    <List
                      dataSource={doctorQueueGroups.inProgress}
                      renderItem={(item) => {
                        const pInfo = getPatientInfo(item.patientId, item.patientName)
                        const secondaryActions = [
                          ...(permissions.canUpdateStatus
                            ? [{
                              key: 'wait-result',
                              label: 'Chờ kết quả cận lâm sàng',
                              onClick: () => handleUpdateItemStatus(item.id, 'WAITING_FOR_RESULT'),
                            }]
                            : []),
                          ...(permissions.canComplete
                            ? [{
                              key: 'complete',
                              label: 'Hoàn tất lượt khám',
                              icon: <CheckCircleOutlined />,
                              onClick: () => Modal.confirm({
                                title: 'Xác nhận hoàn tất lượt khám?',
                                content: 'Đảm bảo bệnh án đã được bác sĩ ký hoặc khóa trước khi hoàn tất.',
                                okText: 'Hoàn tất',
                                cancelText: 'Hủy',
                                onOk: () => handleCompleteItem(item.id),
                              }),
                            }]
                            : []),
                          ...(permissions.canSkip
                            ? [{
                              key: 'skip',
                              label: 'Bỏ qua lượt khám',
                              icon: <CloseCircleOutlined />,
                              danger: true,
                              onClick: () => {
                                skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                                setSkipModalItem(item)
                              },
                            }]
                            : []),
                        ]
                        return (
                          <List.Item
                            actions={[
                              (permissions.isAdmin || permissions.isDoctor) && (
                                <Button
                                  key="exam"
                                  type="primary"
                                  icon={<MedicineBoxOutlined />}
                                  onClick={() => openEncounter(item)}
                                >
                                  Ghi bệnh án / Khám
                                </Button>
                              ),
                              secondaryActions.length > 0 && (
                                <Dropdown key="more" menu={{ items: secondaryActions }} trigger={['click']} placement="bottomRight">
                                  <Button icon={<MoreOutlined />} aria-label={`Thao tác khác với bệnh nhân ${pInfo.name}`}>
                                    Chi tiết
                                  </Button>
                                </Dropdown>
                              ),
                            ].filter(Boolean)}
                          >
                            <List.Item.Meta
                              avatar={<Avatar size="large" style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>}
                              title={<Text strong>{pInfo.name} - <Text type="secondary">STT: {item.queueNumber}</Text></Text>}
                              description={`Mã lượt: ${item.visitCode || item.visitId || 'Chưa có'} | Nguồn: ${item.sourceType === 'WALK_IN' ? 'Tự đến' : 'Hẹn trước'}`}
                            />
                          </List.Item>
                        )
                      }}
                    />
                  )}
                </Card>

                <Card
                  title={<Text strong style={{ color: '#2563eb' }}>🟡 BỆNH NHÂN ĐANG CHỜ ({doctorQueueGroups.waiting.length})</Text>}
                  style={{ borderRadius: 12 }}
                >
                  <List
                    dataSource={doctorQueueGroups.waiting}
                    pagination={{ pageSize: 5 }}
                    renderItem={(item) => {
                      const pInfo = getPatientInfo(item.patientId, item.patientName)
                      const secondaryActions = permissions.canSkip
                        ? [{
                          key: 'skip',
                          label: 'Bỏ qua lượt khám',
                          icon: <CloseCircleOutlined />,
                          danger: true,
                          onClick: () => {
                            skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                            setSkipModalItem(item)
                          },
                        }]
                        : []
                      return (
                        <List.Item
                          actions={[
                            permissions.canCallNext && (
                              <Button
                                key="call-next"
                                type="primary"
                                icon={<StepForwardOutlined />}
                                onClick={() => handleCallNext(item.medicalQueueId || item.queueId)}
                              >
                                Gọi vào khám
                              </Button>
                            ),
                          ].filter(Boolean)}
                        >
                          <List.Item.Meta
                            avatar={<Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>}
                            title={<Text strong>STT {item.queueNumber}: {pInfo.name}</Text>}
                            description={`Đến lúc: ${item.checkedInAt ? dayjs(item.checkedInAt).format('HH:mm DD/MM/YYYY') : 'Chưa ghi nhận'}`}
                          />
                        </List.Item>
                      )
                    }}
                  />
                </Card>

                <Card
                  title={<Text strong style={{ color: '#9333ea' }}>🟣 BỆNH NHÂN CHỜ KẾT QUẢ CĐLS ({doctorQueueGroups.waitingForResult.length})</Text>}
                  style={{ borderRadius: 12, borderColor: '#e9d5ff' }}
                >
                  <List
                    dataSource={doctorQueueGroups.waitingForResult}
                    pagination={{ pageSize: 5 }}
                    renderItem={(item) => {
                      const pInfo = getPatientInfo(item.patientId, item.patientName)
                      const secondaryActions = [{
                        key: 'record',
                        label: 'Xem bệnh án',
                        icon: <EyeOutlined />,
                        onClick: () => navigate('/medical-records', { state: { patientId: item.patientId, visitId: item.visitId } }),
                      }]
                      return (
                        <List.Item
                          actions={[
                            permissions.canUpdateStatus && (
                              <Button
                                key="resume"
                                type="primary"
                                style={{ backgroundColor: '#16a34a' }}
                                onClick={() => handleUpdateItemStatus(item.id, 'IN_PROGRESS')}
                              >
                                Tiếp tục khám
                              </Button>
                            ),
                            <Button key="view-record" onClick={() => openEncounter(item)}>
                              Xem bệnh án
                            </Button>,
                          ].filter(Boolean)}
                        >
                          <List.Item.Meta
                            avatar={<Avatar style={{ backgroundColor: '#9333ea' }}>{getInitials(pInfo.name)}</Avatar>}
                            title={<Text strong>{pInfo.name}</Text>}
                            description="Đã gửi chỉ định CĐLS, đang chờ phòng xét nghiệm / CĐHA trả kết quả."
                          />
                        </List.Item>
                      )
                    }}
                  />
                </Card>
              </div>
            ),
          },
        ].filter((item) => {
          if (permissions.isAdmin) return true
          if (permissions.isDoctor) return item.key === 'doctor_queue'
          if (permissions.isNurse) return item.key === 'reception_queue'
          return ['appointments', 'reception_queue'].includes(item.key)
        })}
      />

      <Modal
        title="Đặt Lịch Hẹn Khám Bệnh Mới"
        open={bookModalOpen}
        onCancel={() => setBookModalOpen(false)}
        footer={null}
        destroyOnClose
        width={600}
      >
        <Form form={bookForm} layout="vertical" onFinish={handleCreateAppointmentSubmit}>
          <Form.Item label="Bệnh nhân" required style={{ marginBottom: 8 }}>
            <Row gutter={8}>
              <Col span={18}>
                <Form.Item
                  name="patientId"
                  noStyle
                  rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân!' }]}
                >
                  <Select
                    showSearch
                    placeholder="Chọn từ Đăng ký bệnh nhân..."
                    optionFilterProp="children"
                    options={patients.map((p) => ({
                      value: p.id,
                      label: `${p.fullName || p.name} (${p.patientCode || 'BN'} - ${p.phone || p.phoneNumber || 'Chưa có SĐT'})`,
                    }))}
                  />
                </Form.Item>
              </Col>
              <Col span={6}>
                <Button
                  type="dashed"
                  icon={<UserAddOutlined />}
                  onClick={() => setQuickPatientModalOpen(true)}
                  style={{ width: '100%' }}
                >
                  Tạo mới
                </Button>
              </Col>
            </Row>
          </Form.Item>

          <Form.Item
            name="doctorId"
            label="Bác sĩ khám"
            rules={[{ required: true, message: 'Vui lòng chọn bác sĩ!' }]}
          >
            <Select
              placeholder="Chọn bác sĩ phụ trách..."
              options={doctors.map((d) => ({
                value: d.id,
                label: `${d.fullName || d.username} - ${d.department || 'Chuyên khoa'}`,
              }))}
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="appointmentDate"
                label="Ngày hẹn"
                initialValue={dayjs()}
                rules={[{ required: true, message: 'Vui lòng chọn ngày hẹn!' }]}
              >
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="appointmentTime"
                label="Khung giờ hẹn"
                initialValue={dayjs().add(1, 'hour')}
                rules={[{ required: true, message: 'Vui lòng chọn khung giờ!' }]}
              >
                <TimePicker format="HH:mm" minuteStep={15} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="reason"
            label="Lý do khám"
            rules={[{ required: true, message: 'Vui lòng nhập lý do khám!' }]}
          >
            <Input placeholder="VD: Khám định kỳ, đau ngực, ho kéo dài..." />
          </Form.Item>

          <Alert
            type="warning"
            showIcon
            message="Ràng buộc kiểm tra tự động"
            description="Hệ thống sẽ kiểm tra: (1) Khung giờ không được ở quá khứ. (2) Bác sĩ không có lịch trùng trong cùng khung giờ. Lịch hẹn tạo thành công sẽ ở trạng thái ĐÃ ĐẶT."
            style={{ marginBottom: 16 }}
          />

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setBookModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit" loading={actionLoading}>
                Xác nhận Đặt Lịch Hẹn
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Tiếp nhận Bệnh nhân Tự đến"
        open={walkInModalOpen}
        onCancel={() => setWalkInModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={walkInForm} layout="vertical" onFinish={handleCheckInWalkInSubmit}>
          <Form.Item label="Chọn Bệnh nhân" required style={{ marginBottom: 8 }}>
            <Row gutter={8}>
              <Col span={18}>
                <Form.Item
                  name="patientId"
                  noStyle
                  rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân!' }]}
                >
                  <Select
                    showSearch
                    placeholder="Tìm theo tên hoặc SĐT..."
                    optionFilterProp="children"
                    options={patients.map((p) => ({
                      value: p.id,
                      label: `${p.fullName || p.name} (${p.phone || p.patientCode || 'BN'})`,
                    }))}
                  />
                </Form.Item>
              </Col>
              <Col span={6}>
                <Button
                  type="dashed"
                  icon={<UserAddOutlined />}
                  onClick={() => setQuickPatientModalOpen(true)}
                  style={{ width: '100%' }}
                >
                  Tạo mới
                </Button>
              </Col>
            </Row>
          </Form.Item>

          <Form.Item
            name="doctorId"
            label="Chọn Bác sĩ khám"
            rules={[{ required: true, message: 'Vui lòng chọn bác sĩ!' }]}
          >
            <Select
              placeholder="Chọn bác sĩ phụ trách..."
              options={doctors.map((d) => ({
                value: d.id,
                label: `${d.fullName || d.username} (${d.department || 'Phòng khám'})`,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Lý do khám"
            rules={[{ required: true, message: 'Vui lòng nhập lý do khám!' }]}
          >
            <Input placeholder="VD: Đau đầu, sốt nhẹ..." />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setWalkInModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit" loading={actionLoading}>
                Tiếp nhận tự đến
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Hủy Lịch Hẹn Khám"
        open={!!cancelModalItem}
        onCancel={() => setCancelModalItem(null)}
        footer={null}
        destroyOnClose
      >
        <Form form={cancelForm} layout="vertical" onFinish={handleCancelAppointmentSubmit}>
          <Paragraph>
            Lịch hẹn: <Text strong>{cancelModalItem?.appointmentCode}</Text> | Bệnh nhân: <Text strong>{getPatientInfo(cancelModalItem?.patientId, cancelModalItem?.patientName).name}</Text>
          </Paragraph>
          <Form.Item
            name="reason"
            label="Lý do hủy lịch"
            rules={[{ required: true, message: 'Vui lòng chọn hoặc nhập lý do hủy!' }]}
          >
            <Select
              options={[
                { value: 'Bệnh nhân báo bận đột xuất', label: 'Bệnh nhân báo bận đột xuất' },
                { value: 'Bệnh nhân muốn đổi ngày/giờ khám', label: 'Bệnh nhân muốn đổi ngày/giờ khám' },
                { value: 'Bác sĩ bận lịch công tác đột xuất', label: 'Bác sĩ bận lịch công tác đột xuất' },
                { value: 'Nhập trùng thông tin lịch hẹn', label: 'Nhập trùng thông tin lịch hẹn' },
              ]}
            />
          </Form.Item>
          <Alert
            type="info"
            showIcon
            message="Xác nhận giải phóng khung giờ"
            description="Khi chuyển lịch hẹn sang trạng thái ĐÃ HỦY, hệ thống sẽ giải phóng khung giờ của Bác sĩ để người khác có thể đăng ký."
            style={{ marginBottom: 16 }}
          />
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setCancelModalItem(null)}>Quay lại</Button>
              <Button type="primary" danger htmlType="submit" loading={actionLoading}>
                Xác nhận Hủy Lịch Hẹn
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Đăng Ký Nhanh Bệnh Nhân Mới (Đồng bộ Đăng ký bệnh nhân)"
        open={quickPatientModalOpen}
        onCancel={() => setQuickPatientModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={quickPatientForm} layout="vertical" onFinish={handleQuickRegisterPatientSubmit}>
          <Form.Item
            name="fullName"
            label="Họ và tên bệnh nhân"
            rules={[{ required: true, message: 'Vui lòng nhập họ và tên!' }]}
          >
            <Input placeholder="VD: Nguyễn Văn Nam" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="phone"
                label="Số điện thoại"
                rules={[{ required: true, message: 'Vui lòng nhập SĐT!' }]}
              >
                <Input placeholder="0901234567" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="gender"
                label="Giới tính"
                initialValue="MALE"
              >
                <Radio.Group>
                  <Radio value="MALE">Nam</Radio>
                  <Radio value="FEMALE">Nữ</Radio>
                </Radio.Group>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="dateOfBirth"
            label="Ngày sinh"
            rules={[{ required: true, message: 'Vui lòng chọn ngày sinh!' }]}
          >
            <DatePicker
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              disabledDate={(date) => date && date.isAfter(dayjs(), 'day')}
            />
          </Form.Item>
          <Form.Item name="address" label="Địa chỉ">
            <Input placeholder="Số nhà, tên đường, quận/huyện..." />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setQuickPatientModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit" loading={quickPatientSaving}>
                Lưu Bệnh Nhân Mới
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Xác nhận bỏ qua lượt khám"
        open={!!skipModalItem}
        onCancel={() => setSkipModalItem(null)}
        footer={null}
      >
        <Form form={skipForm} layout="vertical" onFinish={handleSkipSubmit}>
          <Paragraph>
            Bệnh nhân: <Text strong>{getPatientInfo(skipModalItem?.patientId, skipModalItem?.patientName).name}</Text> (STT: {skipModalItem?.queueNumber})
          </Paragraph>
          <Form.Item
            name="reason"
            label="Lý do bỏ qua lượt khám"
            rules={[{ required: true, message: 'Vui lòng chọn lý do!' }]}
          >
            <Select
              options={[
                { value: 'Vắng mặt khi gọi', label: 'Đã gọi 3 lần nhưng không có mặt' },
                { value: 'Khách hàng xin lùi lượt', label: 'Bệnh nhân có việc bận đột xuất' },
                { value: 'Bệnh nhân ra về', label: 'Bệnh nhân ra về không khám nữa' },
              ]}
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setSkipModalItem(null)}>Hủy</Button>
              <Button type="primary" danger htmlType="submit" loading={actionLoading}>
                Xác nhận bỏ qua
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="Nhật ký lịch hẹn & thông báo"
        width={650}
        open={logsDrawerOpen}
        onClose={() => setLogsDrawerOpen(false)}
      >
        <Tabs
          items={[
            {
              key: 'app_logs',
              label: 'Nhật ký Lịch hẹn',
              children: (
                <Table
                  dataSource={appointmentLogs}
                  rowKey="id"
                  pagination={{ pageSize: 8 }}
                  columns={[
                    { title: 'Thời điểm', dataIndex: 'timestamp', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                    { title: 'Thời điểm', dataIndex: 'timestamp', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                    { title: 'Mã LH', dataIndex: 'appointmentCode', render: (c) => <Text code>{c}</Text> },
                    { title: 'Người thao tác', dataIndex: 'operatorName' },
                    { title: 'Chi tiết thao tác', dataIndex: 'details' },
                  ]}
                />
              ),
            },
            {
              key: 'notif_logs',
              label: 'Nhật ký Thông báo / Nhắc lịch',
              children: (
                <Table
                  dataSource={notificationLogs}
                  rowKey="id"
                  pagination={{ pageSize: 8 }}
                  columns={[
                    { title: 'Thời điểm', dataIndex: 'sentAt', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                    { title: 'Thời điểm', dataIndex: 'sentAt', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                    { title: 'Bệnh nhân', dataIndex: 'patientName' },
                    {
                      title: 'Kênh',
                      dataIndex: 'channel',
                      render: (channel) => (
                        <Tag color="blue">
                          {String(channel || 'Hệ thống').replace(/System/gi, 'Hệ thống')}
                        </Tag>
                      ),
                    },
                    { title: 'Nội dung nhắc', dataIndex: 'message' },
                  ]}
                />
              ),
            },
          ]}
        />
      </Drawer>

      <Modal
        title="Chi Tiết Lịch Hẹn & Hàng Đợi"
        open={!!detailItem}
        onCancel={() => setDetailItem(null)}
        footer={[
          <Button key="close" onClick={() => setDetailItem(null)}>Đóng</Button>,
        ]}
      >
        {detailItem && (
          <div>
            <Paragraph><Text type="secondary">Mã lịch hẹn / Lượt khám:</Text> <Text code>{detailItem.appointmentCode || detailItem.visitCode || detailItem.id}</Text></Paragraph>
            <Paragraph><Text type="secondary">Bệnh nhân:</Text> <Text strong>{detailItem.patientName}</Text></Paragraph>
            <Paragraph><Text type="secondary">Bác sĩ phụ trách:</Text> <Text strong>{detailItem.doctorName || 'Chưa gán'}</Text></Paragraph>
            <Paragraph><Text type="secondary">Chuyên khoa:</Text> <Tag color="cyan">{detailItem.department || '—'}</Tag></Paragraph>
            <Paragraph><Text type="secondary">Trạng thái:</Text> <Tag color="blue">{detailItem.status}</Tag></Paragraph>
            {detailItem.reason && <Paragraph><Text type="secondary">Lý do khám:</Text> {detailItem.reason}</Paragraph>}
            {detailItem.cancelReason && <Paragraph><Text type="secondary">Lý do hủy:</Text> <Text type="danger">{detailItem.cancelReason}</Text></Paragraph>}
            {(detailItem.appointmentAt || detailItem.startTime) && <Paragraph><Text type="secondary">Thời gian hẹn:</Text> {dayjs(detailItem.appointmentAt || detailItem.startTime).format('HH:mm - DD/MM/YYYY')}</Paragraph>}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default AppointmentQueue
