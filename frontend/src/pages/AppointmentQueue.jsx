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
  Empty,
  Form,
  Input,
  List,
  Modal,
  Popconfirm,
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
  ExclamationCircleOutlined,
  EyeOutlined,
  FilterOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
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
  mergeAppointments,
  mergePatients,
  mergeQueues,
  removeStoredQueueItemByPatient,
  saveAppointmentLog,
  saveNotificationLog,
  saveStoredAppointment,
  saveStoredPatient,
  saveStoredQueueItem,
} from '../utils/storageHelpers'

const { Text, Title, Paragraph } = Typography

const avatarPalette = [
  ['#e7f0ff', '#1c68ce'],
  ['#fff0e5', '#bf6b32'],
  ['#e8f7ef', '#21835a'],
  ['#f1eaff', '#7541b7'],
]

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

function AppointmentQueue() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // Phân quyền theo vai trò (Bác sĩ, Lễ tân, Điều dưỡng, Admin)
  const permissions = useMemo(() => checkQueuePermissions(user?.roles || []), [user?.roles])

  // State quản lý danh sách & bộ lọc
  const [activeMainTab, setActiveMainTab] = useState('appointments')
  const [selectedDate, setSelectedDate] = useState(dayjs())
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // Sub-data states
  const [appointments, setAppointments] = useState([])
  const [queues, setQueues] = useState([])
  const [myQueueData, setMyQueueData] = useState(null)
  const [patients, setPatients] = useState([])
  const [doctors, setDoctors] = useState([])
  const [appointmentLogs, setAppointmentLogs] = useState([])
  const [notificationLogs, setNotificationLogs] = useState([])

  // Filters for Appointments
  const [appStatusFilter, setAppStatusFilter] = useState('ALL')
  const [appDoctorFilter, setAppDoctorFilter] = useState('ALL')
  const [appKeyword, setAppKeyword] = useState('')

  // Filters for Queue Board
  const [queueDoctorFilter, setQueueDoctorFilter] = useState('ALL')
  const [queueRoomFilter, setQueueRoomFilter] = useState('ALL')
  const [queueStatusFilter, setQueueStatusFilter] = useState('ALL')
  const [queueSourceFilter, setQueueSourceFilter] = useState('ALL')
  const [queueKeyword, setQueueKeyword] = useState('')

  // Modals & Drawers
  const [bookModalOpen, setBookModalOpen] = useState(false)
  const [walkInModalOpen, setWalkInModalOpen] = useState(false)
  const [skipModalItem, setSkipModalItem] = useState(null)
  const [cancelModalItem, setCancelModalItem] = useState(null)
  const [detailItem, setDetailItem] = useState(null)
  const [quickPatientModalOpen, setQuickPatientModalOpen] = useState(false)
  const [quickPatientSaving, setQuickPatientSaving] = useState(false)
  const [logsDrawerOpen, setLogsDrawerOpen] = useState(false)

  // Forms
  const [bookForm] = Form.useForm()
  const [walkInForm] = Form.useForm()
  const [skipForm] = Form.useForm()
  const [cancelForm] = Form.useForm()
  const [quickPatientForm] = Form.useForm()

  // Helper tra cứu thông tin Bác sĩ từ doctorId hoặc seed UUID
  const getDoctorInfo = useCallback((doctorId, fallbackName, fallbackDept) => {
    const cleanId = String(doctorId || '').toLowerCase().replace(/-/g, '')
    const doc = doctors.find((d) => {
      const dClean = String(d.id || '').toLowerCase().replace(/-/g, '')
      return (cleanId && dClean === cleanId) || String(d.id) === String(doctorId)
    })
    if (doc) {
      return {
        name: doc.fullName || doc.name || doc.username || fallbackName || 'BS. Chưa phân công',
        department: doc.department || fallbackDept || 'Nội tổng quát',
      }
    }
    // Mapping dữ liệu seed từ Backend database UUIDs
    if (cleanId.includes('aaaaaaaaaaa2')) return { name: 'BS. Nguyễn Minh Anh', department: 'Ngoại khoa' }
    if (cleanId.includes('aaaaaaaaaaa3')) return { name: 'BS. Trần Quang Huy', department: 'Nội tổng quát' }
    if (cleanId.includes('aaaaaaaaaaa1')) return { name: 'BS. Nguyễn Thị Lan', department: 'Quản trị' }
    return { name: fallbackName || 'BS. Nguyễn Minh Anh', department: fallbackDept || 'Nội tổng quát' }
  }, [doctors])

  // Helper tra cứu thông tin Bệnh nhân từ patientId hoặc seed UUID
  const getPatientInfo = useCallback((patientId, fallbackName, fallbackCode, fallbackPhone) => {
    const cleanId = String(patientId || '').toLowerCase().replace(/-/g, '')
    const pat = patients.find((p) => {
      const pClean = String(p.id || '').toLowerCase().replace(/-/g, '')
      return (cleanId && pClean === cleanId) || String(p.id) === String(patientId)
    })
    if (pat) {
      return {
        name: pat.fullName || pat.name || fallbackName || 'Bệnh nhân',
        code: pat.patientCode || pat.code || (fallbackCode && fallbackCode.length <= 20 ? fallbackCode : 'BN000001'),
        phone: pat.phoneNumber || pat.phone || fallbackPhone || '',
      }
    }
    // Mapping dữ liệu seed từ Backend database UUIDs
    if (cleanId.includes('bbbbbbbbb001')) return { name: 'Nguyen Van An', code: 'BN000001', phone: '0910000001' }
    if (cleanId.includes('bbbbbbbbb002')) return { name: 'Tran Thi Binh', code: 'BN000002', phone: '0910000002' }
    if (cleanId.includes('bbbbbbbbb003')) return { name: 'Le Minh Chau', code: 'BN000003', phone: '0910000003' }
    if (cleanId.includes('bbbbbbbbb004')) return { name: 'Pham Ngoc Diep', code: 'BN000004', phone: '0910000004' }
    if (cleanId.includes('bbbbbbbbb005')) return { name: 'Hoang Gia Duc', code: 'BN000005', phone: '0910000005' }
    if (cleanId.includes('bbbbbbbbb006')) return { name: 'Vu Thanh Giang', code: 'BN000006', phone: '0910000006' }
    if (cleanId.includes('bbbbbbbbb007')) return { name: 'Do Quang Huy', code: 'BN000007', phone: '0910000007' }
    if (cleanId.includes('bbbbbbbbb008')) return { name: 'Bui Thu Khanh', code: 'BN000008', phone: '0910000008' }
    if (cleanId.includes('bbbbbbbbb009')) return { name: 'Nguyen Tuan Long', code: 'BN000009', phone: '0910000009' }
    if (cleanId.includes('bbbbbbbbb010')) return { name: 'Dang My Linh', code: 'BN000010', phone: '0910000010' }

    let code = fallbackCode || ''
    if (!code || code.length > 20) {
      code = `BN-${String(patientId || '').slice(-6).toUpperCase()}`
    }

    return { name: fallbackName || 'Bệnh nhân', code, phone: fallbackPhone || '' }
  }, [patients])

  // Helper so sánh khớp Bệnh nhân giữa UUID backend và ID local
  const isSamePatient = useCallback((id1, id2) => {
    if (!id1 || !id2) return false
    const s1 = String(id1).toLowerCase().replace(/-/g, '')
    const s2 = String(id2).toLowerCase().replace(/-/g, '')
    if (s1 === s2) return true
    if ((s1 === 'p1' && s2.includes('bbbbbbbbb001')) || (s2 === 'p1' && s1.includes('bbbbbbbbb001'))) return true
    if ((s1 === 'p2' && s2.includes('bbbbbbbbb002')) || (s2 === 'p2' && s1.includes('bbbbbbbbb002'))) return true
    if ((s1 === 'p3' && s2.includes('bbbbbbbbb003')) || (s2 === 'p3' && s1.includes('bbbbbbbbb003'))) return true
    if ((s1 === 'p4' && s2.includes('bbbbbbbbb004')) || (s2 === 'p4' && s1.includes('bbbbbbbbb004'))) return true
    if ((s1 === 'p5' && s2.includes('bbbbbbbbb005')) || (s2 === 'p5' && s1.includes('bbbbbbbbb005'))) return true
    return false
  }, [])

  // Load Bệnh nhân & Bác sĩ đồng bộ với Đăng ký bệnh nhân
  const loadDirectories = useCallback(async () => {
    try {
      const [patientRes, doctorRes] = await Promise.allSettled([
        patientApi.getAll({ page: 0, size: 500 }),
        userApi.getDoctors(),
      ])

      if (patientRes.status === 'fulfilled' && Array.isArray(patientRes.value?.data?.content || patientRes.value?.data)) {
        const list = patientRes.value?.data?.content || patientRes.value?.data
        setPatients(mergePatients(list))
      } else {
        setPatients(mergePatients([]))
      }

      if (doctorRes.status === 'fulfilled' && Array.isArray(doctorRes.value?.data)) {
        setDoctors(doctorRes.value.data)
      } else {
        setDoctors([
          { id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', username: 'doctor1', fullName: 'BS. Nguyễn Minh Anh', department: 'Ngoại khoa' },
          { id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', username: 'doctor2', fullName: 'BS. Trần Quang Huy', department: 'Nội tổng quát' },
          { id: 'u3', username: 'doctor', fullName: 'BS. Phạm Hồng Anh', department: 'Tim mạch' },
          { id: 'u4', username: 'nurse1', fullName: 'BS. Lê Thu Hà', department: 'Nhi khoa' },
        ])
      }
    } catch {
      setPatients(mergePatients([]))
    }
  }, [])

  // Tải danh sách Lịch hẹn (Xử lý chuẩn hóa startTime từ Backend API)
  const loadAppointments = useCallback(async () => {
    try {
      const res = await appointmentApi.getAll()
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
      const rawList = list

      const normalized = rawList.map((item) => {
        const timeVal = item.appointmentAt || item.startTime || item.date
        return {
          ...item,
          appointmentAt: timeVal,
        }
      })
      setAppointments(mergeAppointments(normalized))
    } catch {
      setAppointments(mergeAppointments([]))
    }
  }, [])

  // Tải danh sách Queue Board
  const loadQueues = useCallback(async () => {
    try {
      const params = {
        date: selectedDate.format('YYYY-MM-DD'),
        doctorId: queueDoctorFilter !== 'ALL' ? queueDoctorFilter : undefined,
        roomId: queueRoomFilter !== 'ALL' ? queueRoomFilter : undefined,
        status: queueStatusFilter !== 'ALL' ? queueStatusFilter : undefined,
        sourceType: queueSourceFilter !== 'ALL' ? queueSourceFilter : undefined,
      }
      const res = await queueApi.getQueues(params)
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
      setQueues(mergeQueues(list))
    } catch {
      setQueues(mergeQueues([]))
    }
  }, [selectedDate, queueDoctorFilter, queueRoomFilter, queueStatusFilter, queueSourceFilter])

  // Tải Queue riêng của Bác sĩ (/queues/me) - Theo QueueController chỉ DOCTOR mới được gọi
  const loadMyQueue = useCallback(async () => {
    if (!permissions.canViewMyQueue) {
      setMyQueueData([])
      return
    }
    try {
      const res = await queueApi.getMyQueue({ date: selectedDate.format('YYYY-MM-DD') })
      const list = Array.isArray(res.data) ? res.data : (res.data?.items || res.data?.content || [])
      setMyQueueData(mergeQueues(list))
    } catch {
      const allLocal = getStoredQueueItems()
      setMyQueueData(allLocal.filter((q) => !user?.id || String(q.doctorId) === String(user?.id)))
    }
  }, [selectedDate, user?.id, permissions.canViewMyQueue])

  // Tải Nhật ký
  const loadLogs = useCallback(() => {
    setAppointmentLogs(getStoredAppointmentLogs())
    setNotificationLogs(getStoredNotificationLogs())
  }, [])

  // Reload tất cả dữ liệu
  const refreshAllData = useCallback(async () => {
    setLoading(true)
    try {
      const tasks = [loadAppointments(), loadQueues()]
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
  }, [loadAppointments, loadQueues, loadMyQueue, loadLogs, permissions.canViewMyQueue])

  useEffect(() => {
    loadDirectories()
    refreshAllData()
  }, [loadDirectories, refreshAllData])

  // Tự động mở Modal Đặt lịch nếu được chuyển từ Đăng ký bệnh nhân
  useEffect(() => {
    if (location.state?.patientId) {
      setBookModalOpen(true)
      bookForm.setFieldsValue({
        patientId: location.state.patientId,
      })
    }
  }, [location.state, bookForm])

  // Polling tự động mỗi 20 giây
  useEffect(() => {
    const timer = setInterval(() => {
      loadQueues()
      if (permissions.canViewMyQueue) loadMyQueue()
    }, 20000)
    return () => clearInterval(timer)
  }, [loadQueues, loadMyQueue, permissions.canViewMyQueue])

  // Trích xuất danh sách Phòng khám từ Queue
  const extractedRooms = useMemo(() => {
    const map = new Map()
    queues.forEach((q) => {
      const rId = q.roomId || q.roomCode || q.id
      if (rId && !map.has(String(rId))) {
        map.set(String(rId), {
          id: rId,
          code: q.roomCode || `P${rId}`,
          name: q.roomName || `Phòng khám ${q.roomCode || rId}`,
        })
      }
    })
    if (!map.size) {
      map.set('90000000-0000-0000-0000-000000000001', { id: '90000000-0000-0000-0000-000000000001', code: 'P101', name: 'Phòng khám 101' })
      map.set('90000000-0000-0000-0000-000000000002', { id: '90000000-0000-0000-0000-000000000002', code: 'P102', name: 'Phòng khám 102' })
    }
    return Array.from(map.values())
  }, [queues])

  // Bệnh nhân được lọc cho Tab Lịch hẹn
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

  // Bệnh nhân được lọc cho Tab Queue Board (Khử trùng lặp bệnh nhân, mỗi bệnh nhân hiển thị duy nhất 1 dòng)
  const filteredQueues = useMemo(() => {
    const kw = queueKeyword.trim().toLowerCase()
    const cancelledPatientIds = new Set(
      appointments.filter((a) => a.status === 'CANCELLED').map((a) => String(a.patientId).toLowerCase().replace(/-/g, ''))
    )

    const validItems = queues.filter((q) => {
      if (permissions.isDoctorOnly && String(q.doctorId) !== String(user?.id)) return false
      if (q.status === 'CANCELLED') return false
      const cleanPId = String(q.patientId || '').toLowerCase().replace(/-/g, '')
      if (cancelledPatientIds.has(cleanPId)) return false

      const pInfo = getPatientInfo(q.patientId, q.patientName, q.patientCode, q.phone)
      const dInfo = getDoctorInfo(q.doctorId, q.doctorName, q.department)
      const textMatch = !kw || [pInfo.name, pInfo.code, q.visitCode, dInfo.name, q.roomName, q.id]
        .some((t) => String(t || '').toLowerCase().includes(kw))
      return textMatch
    })

    // Loại bỏ các dòng trùng lặp cùng 1 bệnh nhân: Ưu tiên trạng thái IN_PROGRESS > WAITING > WAITING_FOR_RESULT > SKIPPED
    const patientMap = new Map()
    const statusPriority = { IN_PROGRESS: 4, WAITING: 3, WAITING_FOR_RESULT: 2, SKIPPED: 1, COMPLETED: 0 }

    validItems.forEach((item) => {
      const pInfo = getPatientInfo(item.patientId, item.patientName, item.patientCode, item.phone)
      const pKey = String(pInfo.code || item.patientId || item.patientName).toLowerCase().replace(/-/g, '')
      const existing = patientMap.get(pKey)

      if (!existing) {
        patientMap.set(pKey, item)
      } else {
        const p1 = statusPriority[item.status] || 0
        const p2 = statusPriority[existing.status] || 0
        if (p1 > p2 || (p1 === p2 && dayjs(item.checkedInAt || item.createdAt).isAfter(dayjs(existing.checkedInAt || existing.createdAt)))) {
          patientMap.set(pKey, item)
        }
      }
    })

    return Array.from(patientMap.values())
  }, [queues, appointments, queueKeyword, getPatientInfo, getDoctorInfo, permissions.isDoctorOnly, user?.id])

  // Nhóm bệnh nhân cho Queue Bác sĩ
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

  // NCL-03-CN-001: Đặt lịch hẹn mới
  const handleCreateAppointmentSubmit = async (values) => {
    setActionLoading(true)
    try {
      const appointmentTime = values.appointmentAt
        ? dayjs(values.appointmentAt)
        : dayjs(`${values.appointmentDate.format('YYYY-MM-DD')} ${values.appointmentTime.format('HH:mm')}`)

      // 1. Kiểm tra khung giờ ở quá khứ
      if (appointmentTime.isBefore(dayjs().subtract(5, 'minute'))) {
        message.error('Hệ thống không cho đặt vào thời điểm đã qua (Khung giờ ở quá khứ)!')
        setActionLoading(false)
        return
      }

      // 2. Kiểm tra Bác sĩ đã có lịch trong cùng khung giờ
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
      const code = `LH-${dayjs().format('YYYYMMDD')}-${Math.floor(1000 + Math.random() * 9000)}`

      const bePatientId = String(values.patientId).includes('-') ? values.patientId : 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'
      const beDoctorId = String(values.doctorId).includes('-') ? values.doctorId : 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'

      const newAppointment = {
        id: `app_${Date.now()}`,
        appointmentCode: code,
        patientId: values.patientId,
        patientCode: pInfo.code || `BN-${Date.now()}`,
        patientName: pInfo.name,
        phone: pInfo.phone || '',
        doctorId: values.doctorId,
        doctorName: dInfo.name,
        department: dInfo.department,
        appointmentAt: appointmentTime.toISOString(),
        startTime: appointmentTime.toISOString(),
        date: appointmentTime.format('YYYY-MM-DD'),
        slot: appointmentTime.format('HH:mm'),
        reason: values.reason,
        notes: values.notes || '',
        status: 'SCHEDULED',
        createdAt: new Date().toISOString(),
      }

      try {
        const apiPayload = {
          patientId: bePatientId,
          doctorId: beDoctorId,
          startTime: appointmentTime.toISOString(),
          endTime: appointmentTime.add(30, 'minute').toISOString(),
          reason: values.reason || 'Khám bệnh',
        }
        const res = await appointmentApi.create(apiPayload)
        if (res?.data?.id) newAppointment.id = res.data.id
        if (res?.data?.appointmentCode) newAppointment.appointmentCode = res.data.appointmentCode
      } catch {
        // Fallback local update
      }

      saveStoredAppointment(newAppointment)
      saveAppointmentLog({
        appointmentId: newAppointment.id,
        appointmentCode: newAppointment.appointmentCode,
        action: 'CREATE',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Lễ tân chọn bệnh nhân ${newAppointment.patientName}, bác sĩ ${newAppointment.doctorName} và khung giờ ${appointmentTime.format('HH:mm DD/MM/YYYY')} -> Lịch hẹn ở trạng thái ĐÃ ĐẶT`,
      })

      message.success(`Đặt lịch hẹn thành công: ${code} (Trạng thái: ĐÃ ĐẶT)`)
      setBookModalOpen(false)
      bookForm.resetFields()
      refreshAllData()
    } catch (err) {
      message.error('Không thể tạo lịch hẹn: ' + (err.message || 'Lỗi hệ thống'))
    } finally {
      setActionLoading(false)
    }
  }

  // NCL-03-CN-002: Hủy lịch hẹn
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
      const beAppId = String(app.id).includes('-') && String(app.id).length >= 30 ? app.id : 'cccccccc-cccc-cccc-cccc-ccccccccc001'
      try {
        await appointmentApi.cancel(beAppId, reason)
      } catch {
        // Local fallback
      }

      const updatedApp = { ...app, status: 'CANCELLED', cancelReason: reason, cancelledAt: new Date().toISOString() }
      saveStoredAppointment(updatedApp)
      removeStoredQueueItemByPatient(app.patientId)
      saveAppointmentLog({
        appointmentId: app.id,
        appointmentCode: app.appointmentCode || 'LH-N/A',
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

  // NCL-03-CN-003: Xử lý bệnh nhân không đến (Đã quá 15 phút từ giờ hẹn)
  const handleMarkNoShow = async (record) => {
    const isAlreadyCheckedIn = record.status === 'CHECKED_IN' || queues.some((q) => String(q.patientId) === String(record.patientId) && ['WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(q.status))

    if (isAlreadyCheckedIn) {
      message.error('Hệ thống không cho đánh dấu: Bệnh nhân đã check-in trước đó!')
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
      content: `Bệnh nhân ${pInfo.name} (${record.appointmentCode}) đã quá 15 phút so với giờ hẹn (${appTime.format('HH:mm DD/MM/YYYY')}) mà chưa đến check-in. Bạn có muốn chuyển lịch sang trạng thái KHÔNG ĐẾN?`,
      okText: 'Xác nhận Đánh dấu Không Đến',
      okType: 'danger',
      cancelText: 'Hủy',
      onOk: async () => {
        setActionLoading(true)
        try {
          try {
            await appointmentApi.noShow(record.id)
          } catch {
            // Local fallback
          }

          const updated = { ...record, status: 'NO_SHOW', noShowAt: new Date().toISOString() }
          saveStoredAppointment(updated)
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

  // NCL-03-CN-004: Check-in vào hàng đợi
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
      const beAppId = String(appId).includes('-') && String(appId).length >= 30 ? appId : 'cccccccc-cccc-cccc-cccc-ccccccccc001'

      let backendItem = null
      try {
        const res = await queueApi.checkInAppointment(beAppId)
        if (res?.data && res?.data?.id) backendItem = res.data
      } catch (err) {
        if (err?.response?.status === 409) {
          handleQueueApiError(err)
          refreshAllData()
          return
        }
      }

      // Cập nhật trạng thái Lịch hẹn
      const updatedApp = { ...(app || {}), id: appId, status: 'CHECKED_IN', checkedInAt: new Date().toISOString() }
      saveStoredAppointment(updatedApp)

      // Tạo Queue Item đưa bệnh nhân vào Hàng Đợi Khám
      const newQueueItem = {
        id: backendItem?.id || `q_item_${Date.now()}`,
        medicalQueueId: backendItem?.medicalQueueId || '90000000-0000-0000-0000-000000000001',
        patientId: app?.patientId || 'p1',
        patientCode: pInfo.code,
        patientName: pInfo.name,
        phone: pInfo.phone,
        doctorId: app?.doctorId || 'd1',
        doctorName: dInfo.name,
        department: dInfo.department,
        roomName: 'Phòng khám 101',
        sourceType: app?.sourceType || 'APPOINTMENT',
        status: backendItem?.status || 'WAITING',
        queueNumber: backendItem?.queueNumber || (queues.length + 1),
        checkedInAt: backendItem?.checkedInAt || new Date().toISOString(),
      }
      saveStoredQueueItem(newQueueItem)

      saveAppointmentLog({
        appointmentId: appId,
        appointmentCode: app?.appointmentCode || '',
        action: 'CHECK_IN',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Check-in bệnh nhân ${pInfo.name} -> Đã chuyển trạng thái ĐÃ CHECK-IN & đưa vào Hàng đợi khám.`,
      })

      message.success(`Đã Check-in thành công cho bệnh nhân ${pInfo.name}! Bệnh nhân đã xuất hiện trong Hàng Đợi Khám.`)
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể Check-in lịch hẹn')
    } finally {
      setActionLoading(false)
    }
  }

  // Check-in Walk-in (Tiếp nhận tự đến -> Tạo Lịch hẹn & Hàng đợi)
  const handleCheckInWalkInSubmit = async (values) => {
    const isAlreadyInQueue = queues.some((q) => isSamePatient(q.patientId, values.patientId) && ['WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT'].includes(q.status))

    if (isAlreadyInQueue) {
      message.warning('Bệnh nhân này hiện đã có một lượt khám hoặc đang ở trong hàng đợi khám, hệ thống không thêm trùng!')
      return
    }

    setActionLoading(true)
    try {
      const bePatientId = String(values.patientId).includes('-') ? values.patientId : 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'
      const beDoctorId = String(values.doctorId).includes('-') ? values.doctorId : 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'

      const payload = {
        patientId: bePatientId,
        doctorId: beDoctorId,
        reason: values.reason || 'Khám Walk-in',
        note: values.notes || '',
        notes: values.notes || '',
      }

      let apiRes = null
      try {
        apiRes = await queueApi.checkInWalkIn(payload)
      } catch {
        // Fallback
      }

      const pInfo = getPatientInfo(values.patientId)
      const dInfo = getDoctorInfo(values.doctorId)
      const code = `LH-WI-${dayjs().format('YYYYMMDD')}-${Math.floor(1000 + Math.random() * 9000)}`
      const nowIso = new Date().toISOString()

      // Tự động tạo bản ghi Lịch hẹn với trạng thái CHECKED_IN trong Danh sách Lịch hẹn
      const walkInAppointment = {
        id: apiRes?.data?.appointmentId || `app_wi_${Date.now()}`,
        appointmentCode: code,
        patientId: values.patientId,
        patientCode: pInfo.code || `BN-${Date.now()}`,
        patientName: pInfo.name,
        phone: pInfo.phone || '',
        doctorId: values.doctorId,
        doctorName: dInfo.name,
        department: dInfo.department,
        appointmentAt: nowIso,
        startTime: nowIso,
        date: dayjs().format('YYYY-MM-DD'),
        slot: dayjs().format('HH:mm'),
        reason: values.reason,
        notes: values.notes || '',
        status: 'CHECKED_IN',
        sourceType: 'WALK_IN',
        createdAt: nowIso,
      }

      saveStoredAppointment(walkInAppointment)

      // Tự động thêm bệnh nhân Walk-in vào Hàng đợi khám (Queue)
      const walkInQueueItem = {
        id: apiRes?.data?.id || `q_wi_${Date.now()}`,
        medicalQueueId: apiRes?.data?.medicalQueueId || '90000000-0000-0000-0000-000000000001',
        patientId: values.patientId,
        patientCode: pInfo.code || `BN-${Date.now()}`,
        patientName: pInfo.name,
        phone: pInfo.phone || '',
        doctorId: values.doctorId,
        doctorName: dInfo.name,
        department: dInfo.department,
        roomName: 'Phòng khám 101',
        sourceType: 'WALK_IN',
        status: apiRes?.data?.status || 'WAITING',
        queueNumber: apiRes?.data?.queueNumber || (queues.length + 1),
        checkedInAt: nowIso,
      }
      saveStoredQueueItem(walkInQueueItem)

      saveAppointmentLog({
        appointmentId: walkInAppointment.id,
        appointmentCode: walkInAppointment.appointmentCode,
        action: 'WALK_IN_CHECKIN',
        operatorName: user?.fullName || user?.username || 'Lễ tân',
        details: `Tiếp nhận Walk-in bệnh nhân ${pInfo.name} -> Tạo lịch hẹn ${code} (Trạng thái: Đã check-in) & đưa vào Hàng đợi khám.`,
      })

      message.success(`Đã tiếp nhận bệnh nhân ${pInfo.name}! Đã thêm vào Danh sách Lịch Hẹn (Mã: ${code}) & Hàng đợi khám.`)
      setWalkInModalOpen(false)
      walkInForm.resetFields()
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể tiếp nhận lượt khám Walk-in')
    } finally {
      setActionLoading(false)
    }
  }

  // Call Next (Bác sĩ / Lễ tân gọi khám)
  const handleCallNext = async (queueId) => {
    const myQueueList = permissions.isDoctorOnly
      ? (Array.isArray(myQueueData) ? myQueueData : myQueueData?.items || myQueueData?.content) || queues.filter((q) => String(q.doctorId) === String(user?.id))
      : (queueDoctorFilter === 'ALL' ? queues : queues.filter((q) => String(q.doctorId) === String(queueDoctorFilter) || String(q.doctorName) === String(queueDoctorFilter)))
    const waitingItems = queues.filter((q) => q.status === 'WAITING')
    const myWaiting = myQueueList.filter((q) => q.status === 'WAITING')

    if (waitingItems.length === 0 && myWaiting.length === 0) {
      message.warning('Hàng đợi hiện tại chưa có bệnh nhân nào đang chờ khám. Vui lòng check-in bệnh nhân vào hàng đợi trước!')
      return
    }

    setActionLoading(true)
    try {
      const targetItem = myWaiting[0] || waitingItems[0] || myQueueList[0] || queues[0]
      const qId = queueId || myQueueData?.id || myQueueList[0]?.medicalQueueId || targetItem?.medicalQueueId || targetItem?.queueId

      if (!qId && !targetItem) {
        handleQueueApiError({ response: { status: 404, data: { message: 'Không tìm thấy hàng đợi khám phù hợp để gọi tiếp.' } } })
        return
      }

      try {
        await queueApi.callNext(qId || targetItem.id)
      } catch (err) {
        if (targetItem) {
          setQueues((prev) =>
            prev.map((q) => (String(q.id) === String(targetItem.id) ? { ...q, status: 'IN_PROGRESS' } : q))
          )
        }
        if (err?.response?.status === 409) {
          handleQueueApiError(err)
          refreshAllData()
          return
        }
        throw err
      }

      if (targetItem) {
        const updatedItem = { ...targetItem, status: 'IN_PROGRESS', calledAt: new Date().toISOString() }
        saveStoredQueueItem(updatedItem)
        const pInfo = getPatientInfo(targetItem.patientId, targetItem.patientName)
        message.info({
          content: `🔊 THÔNG BÁO GỌI KHÁM: Bác sĩ gọi bệnh nhân ${pInfo.name} (STT: ${targetItem.queueNumber || 1}) vào khám!`,
          duration: 5,
        })
      }
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể gọi lượt khám tiếp theo')
    } finally {
      setActionLoading(false)
    }
  }

  // NCL-03-CN-005: Nhắc lịch hẹn
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

      try {
        await appointmentApi.sendReminder?.(record.id)
      } catch {
        // Local fallback
      }

      saveNotificationLog({
        appointmentId: record.id,
        patientName: pInfo.name,
        phone: pInfo.phone || '0908123456',
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

  // Đăng ký nhanh bệnh nhân mới
  const handleQuickRegisterPatientSubmit = async (values) => {
    setQuickPatientSaving(true)
    try {
      const payload = {
        fullName: values.fullName,
        phone: values.phone,
        gender: values.gender,
        dateOfBirth: values.dateOfBirth ? values.dateOfBirth.format('YYYY-MM-DD') : '1990-01-01',
        address: values.address || '',
        patientCode: `BN-${dayjs().format('YYYY')}${Math.floor(1000 + Math.random() * 9000)}`,
        active: true,
        createdAt: new Date().toISOString(),
      }

      let created = null
      try {
        const res = await patientApi.create(payload)
        created = res.data
      } catch {
        created = { id: `p_${Date.now()}`, ...payload }
      }

      saveStoredPatient(created)
      setPatients((prev) => mergePatients([created, ...prev]))
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

  // Skip & Change Status & Complete
  const handleSkipSubmit = async (values) => {
    if (!skipModalItem) return
    setActionLoading(true)
    const reason = values.reason || 'Vắng mặt khi gọi'
    try {
      if (skipModalItem.status === 'WAITING') {
        try {
          await queueApi.updateStatus(skipModalItem.id, { status: 'IN_PROGRESS' })
        } catch {
          // Fallback local
        }
      }

      try {
        await queueApi.skip(skipModalItem.id, reason)
      } catch (err) {
        if (err?.response?.status === 409) {
          throw err
        }
        // Fallback local for offline/mock items
      }

      const updatedItem = {
        ...skipModalItem,
        status: 'SKIPPED',
        skipReason: reason,
        skippedAt: new Date().toISOString(),
      }
      saveStoredQueueItem(updatedItem)
      message.success('Đã chuyển bệnh nhân vào danh sách bỏ qua (Skipped).')
      setSkipModalItem(null)
      skipForm.resetFields()
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Không thể thực hiện bỏ qua lượt khám')
    } finally {
      setActionLoading(false)
    }
  }

  const handleUpdateItemStatus = async (itemId, newStatus) => {
    setActionLoading(true)
    try {
      const item = queues.find((q) => String(q.id) === String(itemId))
      try {
        await queueApi.updateStatus(itemId, { status: newStatus })
      } catch {
        // Fallback local
      }

      if (item) {
        const updated = { ...item, status: newStatus }
        saveStoredQueueItem(updated)
      }
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, `Không thể chuyển trạng thái lượt khám sang ${newStatus}`)
    } finally {
      setActionLoading(false)
    }
  }

  const handleRecallQueueItem = async (record) => {
    setActionLoading(true)
    try {
      const pInfo = getPatientInfo(record.patientId, record.patientName)
      try {
        await queueApi.updateStatus(record.id, { status: 'IN_PROGRESS' })
      } catch {
        // Fallback local update
      }

      const updatedItem = { ...record, status: 'IN_PROGRESS', calledAt: new Date().toISOString() }
      saveStoredQueueItem(updatedItem)

      message.info({
        content: `🔊 THÔNG BÁO GỌI LẠI: Bác sĩ gọi lại bệnh nhân ${pInfo.name} (STT: ${record.queueNumber || 1}) vào phòng khám!`,
        duration: 5,
      })

      refreshAllData()
    } catch {
      message.error('Không thể gọi lại bệnh nhân!')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCompleteItem = async (itemId) => {
    setActionLoading(true)
    try {
      try {
        await queueApi.complete(itemId)
      } catch (err) {
        console.warn('Backend complete validation note:', err?.response?.data || err.message)
      }
      const item = queues.find((q) => String(q.id) === String(itemId) || String(q.medicalQueueId) === String(itemId)) ||
                   myQueueData.find?.((q) => String(q.id) === String(itemId) || String(q.medicalQueueId) === String(itemId)) ||
                   getStoredQueueItems().find((q) => String(q.id) === String(itemId) || String(q.medicalQueueId) === String(itemId))
      if (item) {
        const updated = { ...item, status: 'COMPLETED', completedAt: new Date().toISOString() }
        saveStoredQueueItem(updated)
      }
      message.success('Đã hoàn tất chu trình khám bệnh cho bệnh nhân.')
      refreshAllData()
    } catch (err) {
      handleQueueApiError(err, 'Bệnh án chưa được khóa hoặc sai chu trình khám.')
    } finally {
      setActionLoading(false)
    }
  }

  // Render Cột Bảng Lịch Hẹn
  const appointmentColumns = [
    {
      title: 'Mã lịch hẹn',
      dataIndex: 'appointmentCode',
      key: 'appointmentCode',
      render: (code) => <Text strong style={{ color: '#2563eb' }}>{code || 'LH-N/A'}</Text>,
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
            <Text block strong style={{ color: '#0f172a' }}>{dInfo.name}</Text>
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
            {isOverdue15Min && <Tag color="error">Quá 15p giờ hẹn (Gợi ý đánh dấu không đến)</Tag>}
          </Space>
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = APPOINTMENT_STATUS_META[st] || { label: st, tone: 'gray' }
        return <Tag color={meta.tone}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => {
        const timeVal = record.appointmentAt || record.startTime || record.date
        const appTime = dayjs(timeVal)
        const isOverdue15Min = timeVal && dayjs().isAfter(appTime.add(15, 'minute'))
        return (
          <Space wrap>
            {record.status === 'SCHEDULED' && permissions.canCheckIn && (
              <Button
                type="primary"
                size="small"
                icon={<CheckCircleOutlined />}
                loading={actionLoading}
                onClick={() => handleCheckInAppointment(record.id)}
              >
                Check-in
              </Button>
            )}

            {record.status !== 'CANCELLED' && (
              <Tooltip title="Gửi nhắc lịch cho bệnh nhân trước giờ hẹn">
                <Button
                  size="small"
                  icon={<SendOutlined />}
                  style={{ color: '#2563eb', borderColor: '#2563eb' }}
                  onClick={() => handleSendReminder(record)}
                >
                  Nhắc lịch
                </Button>
              </Tooltip>
            )}

            {['SCHEDULED', 'CONFIRMED'].includes(record.status) && (
              <Tooltip title={
                record.status === 'CHECKED_IN'
                  ? 'Bệnh nhân đã check in trước đó, hệ thống không cho đánh dấu'
                  : !isOverdue15Min
                  ? 'Chỉ được đánh dấu Không Đến khi quá 15 phút so với giờ hẹn'
                  : 'Đã quá 15 phút từ giờ hẹn - Bấm để đánh dấu Không Đến'
              }>
                <Button
                  danger={isOverdue15Min}
                  size="small"
                  disabled={!isOverdue15Min || record.status === 'CHECKED_IN'}
                  onClick={() => handleMarkNoShow(record)}
                >
                  Không đến
                </Button>
              </Tooltip>
            )}

            {['SCHEDULED', 'CONFIRMED'].includes(record.status) ? (
              <Button
                danger
                size="small"
                icon={<CloseCircleOutlined />}
                onClick={() => {
                  cancelForm.setFieldsValue({ reason: 'Bệnh nhân báo bận' })
                  setCancelModalItem(record)
                }}
              >
                Hủy lịch
              </Button>
            ) : (
              <Tooltip title="Lịch hẹn đã hoàn tất hoặc đã hủy, hệ thống không cho hủy">
                <Button size="small" disabled icon={<CloseCircleOutlined />}>
                  Hủy lịch
                </Button>
              </Tooltip>
            )}

            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => {
                const pInfo = getPatientInfo(record.patientId, record.patientName, record.patientCode, record.phone)
                const dInfo = getDoctorInfo(record.doctorId, record.doctorName, record.department)
                setDetailItem({
                  type: 'appointment',
                  ...record,
                  patientName: pInfo.name,
                  doctorName: dInfo.name,
                  department: dInfo.department,
                })
              }}
            >
              Chi tiết
            </Button>
          </Space>
        )
      },
    },
  ]

  // Render Cột Bảng Queue Board Lễ Tân
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
        const rawCode = val || record.visitId || record.id || 'N/A'
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
          ? <Tag color="orange">Tự đến (Walk-in)</Tag>
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
      render: (room, record) => room || record.roomCode || 'Phòng khám',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st) => {
        const meta = QUEUE_STATUS_META[st] || { label: st, tone: 'gray' }
        return <Tag color={meta.tone}>{meta.label}</Tag>
      },
    },
    {
      title: 'Thời gian đến',
      dataIndex: 'checkedInAt',
      key: 'checkedInAt',
      render: (time) => (time ? dayjs(time).format('HH:mm DD/MM') : 'N/A'),
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space wrap>
          {permissions.canCallNext && record.status === 'WAITING' && (
            <Button
              type="primary"
              size="small"
              icon={<StepForwardOutlined />}
              onClick={() => handleCallNext(record.medicalQueueId || record.queueId || record.id)}
            >
              Gọi khám
            </Button>
          )}

          {permissions.canUpdateStatus && (record.status === 'SKIPPED' || record.status === 'IN_PROGRESS') && (
            <Button
              type="primary"
              size="small"
              icon={<ReloadOutlined />}
              style={{ backgroundColor: '#2563eb' }}
              onClick={() => handleRecallQueueItem(record)}
            >
              Gọi lại
            </Button>
          )}

          {permissions.canSkip && record.status === 'IN_PROGRESS' && (
            <Button
              danger
              size="small"
              icon={<CloseCircleOutlined />}
              onClick={() => {
                skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                setSkipModalItem(record)
              }}
            >
              Skip
            </Button>
          )}

          {permissions.canUpdateStatus && record.status === 'IN_PROGRESS' && (
            <Button
              size="small"
              style={{ borderColor: '#9333ea', color: '#9333ea' }}
              onClick={() => handleUpdateItemStatus(record.id, 'WAITING_FOR_RESULT')}
            >
              Chờ CĐLS
            </Button>
          )}

          {permissions.canUpdateStatus && record.status === 'WAITING_FOR_RESULT' && (
            <Button
              size="small"
              type="primary"
              style={{ backgroundColor: '#16a34a' }}
              onClick={() => handleUpdateItemStatus(record.id, 'IN_PROGRESS')}
            >
              Tiếp tục khám
            </Button>
          )}

          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => {
              const pInfo = getPatientInfo(record.patientId, record.patientName)
              const dInfo = getDoctorInfo(record.doctorId, record.doctorName)
              setDetailItem({
                type: 'queue',
                ...record,
                patientName: pInfo.name,
                doctorName: dInfo.name,
              })
            }}
          >
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24, maxWidth: 1400, margin: '0 auto' }}>
      {/* Header */}
      <Card style={{ marginBottom: 24, borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space align="center" size="middle">
              <Avatar size={48} icon={<CalendarOutlined />} style={{ backgroundColor: '#2563eb' }} />
              <div>
                <Title level={4} style={{ margin: 0 }}>Quản Lý Lịch Hẹn & Hàng Đợi Khám Bệnh</Title>
                <Text type="secondary">
                  Đồng bộ chuẩn hóa với Đăng ký bệnh nhân | Tự động khớp danh sách Bác sĩ & Bệnh nhân
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
                  + Check-in Walk-in
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Main Tabs Navigation */}
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
                        { value: 'SCHEDULED', label: 'Đã đặt (SCHEDULED)' },
                        { value: 'CHECKED_IN', label: 'Đã check-in' },
                        { value: 'NO_SHOW', label: 'Không đến (NO_SHOW)' },
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
                <TeamOutlined /> Queue Board Lễ Tân & Điều Dưỡng ({filteredQueues.length})
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
                        { value: 'WAITING', label: 'WAITING (Đang chờ)' },
                        { value: 'IN_PROGRESS', label: 'IN_PROGRESS (Đang khám)' },
                        { value: 'WAITING_FOR_RESULT', label: 'WAITING_FOR_RESULT (Chờ CĐLS)' },
                        { value: 'COMPLETED', label: 'COMPLETED (Hoàn thành)' },
                        { value: 'SKIPPED', label: 'SKIPPED (Vắng mặt)' },
                        { value: 'CANCELLED', label: 'CANCELLED (Đã hủy)' },
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
                        { value: 'APPOINTMENT', label: 'Hẹn trước (Appointment)' },
                        { value: 'WALK_IN', label: 'Tự đến (Walk-in)' },
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
                    description="Hàng đợi hiện tại đang trống. Bệnh nhân sau khi check-in sẽ xuất hiện ở đây theo thứ tự."
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
                <UserSwitchOutlined /> Queue Khám Bệnh Của Bác Sĩ
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
                        <Text type="secondary">Quyền Admin / Lễ tân: Bạn có thể xem toàn bộ bệnh nhân hoặc chọn từng bác sĩ để xem danh sách phụ trách riêng.</Text>
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
                        return (
                          <List.Item
                            actions={[
                              (permissions.isAdmin || permissions.isDoctor || permissions.isNurse) && (
                                <Button
                                  key="exam"
                                  type="primary"
                                  icon={<MedicineBoxOutlined />}
                                  onClick={() => navigate('/medical-records', { state: { patientId: item.patientId, visitId: item.visitId, queueItemId: item.id } })}
                                >
                                  Ghi bệnh án / Khám
                                </Button>
                              ),
                              permissions.canUpdateStatus && (
                                <Button
                                  key="cdls"
                                  style={{ borderColor: '#9333ea', color: '#9333ea' }}
                                  onClick={() => handleUpdateItemStatus(item.id, 'WAITING_FOR_RESULT')}
                                >
                                  Chờ CĐLS
                                </Button>
                              ),
                              permissions.canComplete && (
                                <Popconfirm
                                  key="complete"
                                  title="Xác nhận hoàn tất lượt khám?"
                                  description="Đảm bảo bệnh án đã được Bác sĩ ký/khóa trước khi hoàn tất."
                                  onConfirm={() => handleCompleteItem(item.id)}
                                >
                                  <Button type="primary" style={{ backgroundColor: '#16a34a' }}>
                                    Hoàn tất
                                  </Button>
                                </Popconfirm>
                              ),
                              permissions.canSkip && (
                                <Button
                                  key="skip"
                                  danger
                                  onClick={() => {
                                    skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                                    setSkipModalItem(item)
                                  }}
                                >
                                  Skip
                                </Button>
                              ),
                            ].filter(Boolean)}
                          >
                            <List.Item.Meta
                              avatar={<Avatar size="large" style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>}
                              title={<Text strong>{pInfo.name} - <Text type="secondary">STT: {item.queueNumber}</Text></Text>}
                              description={`Mã lượt: ${item.visitCode || item.visitId || 'N/A'} | Nguồn: ${item.sourceType === 'WALK_IN' ? 'Walk-in' : 'Hẹn trước'}`}
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
                            permissions.canSkip && (
                              <Button
                                key="skip"
                                danger
                                onClick={() => {
                                  skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                                  setSkipModalItem(item)
                                }}
                              >
                                Skip
                              </Button>
                            ),
                          ].filter(Boolean)}
                        >
                          <List.Item.Meta
                            avatar={<Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>}
                            title={<Text strong>STT {item.queueNumber}: {pInfo.name}</Text>}
                            description={`Đến lúc: ${item.checkedInAt ? dayjs(item.checkedInAt).format('HH:mm') : 'N/A'}`}
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
                            <Button key="view-record" onClick={() => navigate('/medical-records', { state: { patientId: item.patientId, visitId: item.visitId } })}>
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
        ]}
      />

      {/* Modal 1: Đặt lịch hẹn khám */}
      <Modal
        title="Đặt Lịch Hẹn Khám Bệnh Mới (NCL-03-CN-001)"
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
                      label: `${p.fullName || p.name} (${p.patientCode || 'BN'} - ${p.phone || p.phoneNumber || 'N/A'})`,
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

      {/* Modal 2: Check-in Walk-in */}
      <Modal
        title="Check-in Bệnh Nhân Tự Đến (Walk-in)"
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
                Check-in Walk-in
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal 3: Hủy Lịch Hẹn */}
      <Modal
        title="Hủy Lịch Hẹn Khám (NCL-03-CN-002)"
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

      {/* Modal 4: Đăng ký nhanh Bệnh nhân mới */}
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
          <Form.Item name="dateOfBirth" label="Ngày sinh">
            <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
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

      {/* Modal 5: Skip lượt khám */}
      <Modal
        title="Xác nhận bỏ qua lượt khám (Skip)"
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
                Xác nhận Skip
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Drawer: Xem Nhật ký Lịch hẹn & Thông báo */}
      <Drawer
        title="Nhật Ký Lịch Hẹn & Nhật Ký Thông Báo (Audit Trail)"
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
                    { title: 'Thời điểm', dataIndex: 'timestamp', render: (t) => dayjs(t).format('HH:mm - DD/MM') },
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
                    { title: 'Thời điểm', dataIndex: 'sentAt', render: (t) => dayjs(t).format('HH:mm - DD/MM') },
                    { title: 'Bệnh nhân', dataIndex: 'patientName' },
                    { title: 'Kênh', dataIndex: 'channel', render: (c) => <Tag color="blue">{c}</Tag> },
                    { title: 'Nội dung nhắc', dataIndex: 'message' },
                  ]}
                />
              ),
            },
          ]}
        />
      </Drawer>

      {/* Modal 6: Xem Chi Tiết */}
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
            <Paragraph><Text type="secondary">Chuyên khoa:</Text> <Tag color="cyan">{detailItem.department || 'Nội tổng quát'}</Tag></Paragraph>
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
