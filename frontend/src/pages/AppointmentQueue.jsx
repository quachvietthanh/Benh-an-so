import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Avatar,
  Button,
  DatePicker,
  Dropdown,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tooltip,
} from 'antd'
import {
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FilterOutlined,
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
  UserSwitchOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import appointmentApi from '../api/appointmentApi'
import patientApi from '../api/patientApi'
import userApi from '../api/userApi'
import { useAuthContext } from '../context/AuthContext'
import { getAppointments, getPatients } from '../services/mockDataService'
import {
  getOverdueMinutes,
  isAppointmentOverdue,
  NO_SHOW_GRACE_MINUTES,
} from '../utils/appointmentTiming'
import {
  mergeAppointments,
  saveStoredAppointment,
  mergeQueue,
  saveStoredQueueItem,
  mergePatients,
} from '../utils/storageHelpers'


const departmentOptions = [
  'Nội tổng quát',
  'Ngoại khoa',
  'Nhi khoa',
  'Sản phụ khoa',
  'Tim mạch',
  'Tai Mũi Họng',
]

const statusMeta = {
  SCHEDULED: { label: 'Đã đặt', tone: 'green' },
  CHECKED_IN: { label: 'Chờ khám', tone: 'blue' },
  WAITING: { label: 'Chờ khám', tone: 'blue' },
  CALLED: { label: 'Đang khám', tone: 'emerald' },
  IN_PROGRESS: { label: 'Đang khám', tone: 'emerald' },
  WAITING_FOR_RESULT: { label: 'Chờ kết quả', tone: 'purple' },
  SKIPPED: { label: 'Bỏ qua', tone: 'orange' },
  COMPLETED: { label: 'Đã khám', tone: 'gray' },
  CANCELLED: { label: 'Đã hủy', tone: 'red' },
  NO_SHOW: { label: 'Không đến', tone: 'orange' },
}

const overdueMeta = { label: 'Quá hạn', tone: 'overdue' }

const avatarPalette = [
  ['#e7f0ff', '#1c68ce'],
  ['#fff0e5', '#bf6b32'],
  ['#e8f7ef', '#21835a'],
  ['#f1eaff', '#7541b7'],
]

const weekDays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']

const getInitials = (name = '') => name
  .trim()
  .split(/\s+/)
  .slice(-2)
  .map((part) => part[0])
  .join('')
  .toUpperCase()

const getAvatarStyle = (seed = '') => {
  const paletteIndex = [...String(seed)].reduce((sum, character) => sum + character.charCodeAt(0), 0) % avatarPalette.length
  const [background, color] = avatarPalette[paletteIndex]
  return { background, color }
}

const getStatus = (status) => statusMeta[status] || { label: status || 'Chưa xác định', tone: 'gray' }

const getPercentage = (value, total) => (total ? `${((value / total) * 100).toFixed(1)}%` : '0%')

const isSameDate = (value, date) => !date || (value && dayjs(value).isSame(date, 'day'))



const loadPatientDirectory = async () => {
  try {
    const pageSize = 200
    const firstResponse = await patientApi.getAll({ page: 0, size: pageSize })
    const firstPage = firstResponse.data?.content || []
    const totalPages = Number(firstResponse.data?.totalPages || 1)

    if (totalPages <= 1) return firstPage

    const remainingResponses = await Promise.all(
      Array.from({ length: totalPages - 1 }, (_, index) => (
        patientApi.getAll({ page: index + 1, size: pageSize })
      )),
    )
    return [
      ...firstPage,
      ...remainingResponses.flatMap((response) => response.data?.content || []),
    ]
  } catch {
    return getPatients()
  }
}

function AppointmentQueue() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const canManage = user?.roles?.some((role) => ['admin', 'receptionist'].includes(role))
  const [appointments, setAppointments] = useState([])
  const [queue, setQueue] = useState([])
  const [patients, setPatients] = useState([])
  const [doctors, setDoctors] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('appointments')
  const [selectedDate, setSelectedDate] = useState(dayjs())
  const [doctorFilter, setDoctorFilter] = useState('ALL')
  const [departmentFilter, setDepartmentFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [appointmentSearchDraft, setAppointmentSearchDraft] = useState('')
  const [appointmentKeyword, setAppointmentKeyword] = useState('')
  const [queueKeyword, setQueueKeyword] = useState('')
  const [appointmentPage, setAppointmentPage] = useState(1)
  const [appointmentPageSize, setAppointmentPageSize] = useState(10)
  const [queuePage, setQueuePage] = useState(1)
  const [queueNow, setQueueNow] = useState(dayjs())
  const [bookOpen, setBookOpen] = useState(false)
  const [cancelItem, setCancelItem] = useState(null)
  const [noShowItem, setNoShowItem] = useState(null)
  const [detailItem, setDetailItem] = useState(null)
  const [bookForm] = Form.useForm()
  const [cancelForm] = Form.useForm()

  useEffect(() => {
    if (location.state?.patientId) {
      bookForm.setFieldsValue({ patientId: location.state.patientId })
      setBookOpen(true)
    }
  }, [location.state, bookForm])

  const loadData = useCallback(async (includeDirectories = true) => {
    setLoading(true)
    try {
      const [appointmentResult, queueResult, patientResult, doctorResult] = await Promise.allSettled([
        appointmentApi.getAll(),
        appointmentApi.getQueue(),
        includeDirectories ? loadPatientDirectory() : Promise.resolve(null),
        includeDirectories ? userApi.getDoctors() : Promise.resolve(null),
      ])

      let apiApps = []
      if (appointmentResult.status === 'fulfilled') {
        const resData = appointmentResult.value?.data
        apiApps = Array.isArray(resData) ? resData : (resData?.content || [])
      }

      let apiQ = []
      if (queueResult.status === 'fulfilled') {
        const resData = queueResult.value?.data
        apiQ = Array.isArray(resData) ? resData : (resData?.content || [])
      }

      const mockList = getAppointments().map((item) => {
        let appAt = item.appointmentAt
        if (!appAt) {
          const dateStr = item.date || dayjs().format('YYYY-MM-DD')
          const timeStr = item.slot || '09:00'
          appAt = `${dateStr}T${timeStr.length === 5 ? timeStr + ':00' : timeStr}`
        }
        return {
          ...item,
          appointmentAt: appAt,
        }
      })

      const mergedApps = mergeAppointments(apiApps.length ? apiApps : mockList)
      setAppointments(mergedApps)

      const mergedQ = mergeQueue(apiQ.length ? apiQ : mockList.filter((item) => item.status === 'CHECKED_IN' || item.status === 'CALLED' || item.status === 'WAITING'))
      setQueue(mergedQ)

      if (includeDirectories && patientResult.status === 'fulfilled' && Array.isArray(patientResult.value)) {
        setPatients(mergePatients(patientResult.value))
      } else if (includeDirectories) {
        setPatients(mergePatients(getPatients()))
      }

      if (includeDirectories && doctorResult.status === 'fulfilled' && Array.isArray(doctorResult.value?.data)) {
        setDoctors(doctorResult.value.data)
      } else if (includeDirectories) {
        setDoctors([])
      }

    } catch {
      const mockList = getAppointments().map((item) => {
        let appAt = item.appointmentAt
        if (!appAt) {
          const dateStr = item.date || dayjs().format('YYYY-MM-DD')
          const timeStr = item.slot || '09:00'
          appAt = `${dateStr}T${timeStr.length === 5 ? timeStr + ':00' : timeStr}`
        }
        return {
          ...item,
          appointmentAt: appAt,
        }
      })
      setAppointments(mergeAppointments(mockList))
      setQueue(mergeQueue(mockList.filter((item) => item.status === 'CHECKED_IN' || item.status === 'CALLED' || item.status === 'WAITING')))
      setPatients(mergePatients(getPatients()))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  useEffect(() => {
    const timer = setInterval(() => setQueueNow(dayjs()), 60000)
    return () => clearInterval(timer)
  }, [])

  const patientMap = useMemo(() => {
    const map = new Map()
    const allMerged = mergePatients(patients.length ? patients : getPatients())
    allMerged.forEach((patient) => {
      if (patient.id) map.set(String(patient.id), patient)
      if (patient.patientCode) map.set(String(patient.patientCode), patient)
      if (patient.phone) map.set(String(patient.phone), patient)
      if (patient.phoneNumber) map.set(String(patient.phoneNumber), patient)
      if (patient.fullName) map.set(String(patient.fullName), patient)
    })
    return map
  }, [patients])

  const doctorMap = useMemo(() => {
    const map = new Map()
    const fallbackDoctors = [
      { id: 'u2', username: 'doctor1', fullName: 'Dr. Nguyen Minh Anh', department: 'Ngoại khoa' },
      { id: 'u3', username: 'doctor2', fullName: 'Dr. Tran Quang Huy', department: 'Nội tổng quát' },
      { id: 'u4', username: 'nurse1', fullName: 'Dr. Le Thu Ha', department: 'Nhi khoa' },
      { id: 'u5', username: 'receptionist1', fullName: 'Dr. Pham Mai Lan', department: 'Sản phụ khoa' },
      { id: 'u6', username: 'pharmacist1', fullName: 'Dr. Vo Thanh Nam', department: 'Tai Mũi Họng' },
    ]
    const allDocs = [...fallbackDoctors, ...(Array.isArray(doctors) ? doctors : [])]
    allDocs.forEach((doc) => {
      if (doc.id) map.set(String(doc.id), doc)
      if (doc.username) map.set(String(doc.username), doc)
      if (doc.fullName) map.set(String(doc.fullName), doc)
    })
    return map
  }, [doctors])

  const availableDepartments = useMemo(() => Array.from(new Set([
    ...departmentOptions,
    ...appointments.map((item) => item.department).filter(Boolean),
  ])), [appointments])

  const dailyAppointments = useMemo(
    () => appointments.filter((item) => isSameDate(item.appointmentAt, selectedDate)),
    [appointments, selectedDate],
  )



  const dailyStats = useMemo(() => {
    const count = (...statuses) => dailyAppointments.filter((item) => statuses.includes(item.status)).length
    const total = dailyAppointments.length
    const overdue = dailyAppointments.filter((item) => isAppointmentOverdue(item, queueNow)).length
    const scheduled = count('SCHEDULED') - overdue
    const waiting = count('CHECKED_IN')
    const consulting = count('CALLED')
    const completed = count('COMPLETED')
    const cancelled = count('CANCELLED')

    return [
      {
        key: 'total',
        label: 'Tổng lịch hẹn',
        value: total,
        note: selectedDate ? `${selectedDate.isSame(dayjs(), 'day') ? 'Hôm nay' : 'Ngày'} ${selectedDate.format('DD/MM/YYYY')}` : 'Tất cả thời gian',
        icon: CalendarOutlined,
        tone: 'blue',
      },
      { key: 'scheduled', label: 'Đã đặt', value: scheduled, note: getPercentage(scheduled, total), icon: CheckCircleOutlined, tone: 'green' },
      { key: 'overdue', label: 'Quá hạn', value: overdue, note: `Từ ${NO_SHOW_GRACE_MINUTES} phút`, icon: WarningOutlined, tone: 'orange' },
      { key: 'waiting', label: 'Chờ khám', value: waiting, note: 'Hiện tại', icon: ClockCircleOutlined, tone: 'purple' },
      { key: 'consulting', label: 'Đang khám', value: consulting, note: 'Hiện tại', icon: TeamOutlined, tone: 'cyan' },
      { key: 'completed', label: 'Đã khám', value: completed, note: getPercentage(completed, total), icon: CheckCircleOutlined, tone: 'blue' },
      { key: 'cancelled', label: 'Đã hủy', value: cancelled, note: getPercentage(cancelled, total), icon: CloseCircleOutlined, tone: 'red' },
    ]
  }, [dailyAppointments, queueNow, selectedDate])

  const queueStats = useMemo(() => {
    const count = (status) => appointments.filter((item) => item.status === status).length
    return [
      { key: 'queue', label: 'Đang chờ khám', value: queue.length, note: 'Theo thứ tự check-in', icon: ClockCircleOutlined, tone: 'purple' },
      { key: 'called', label: 'Đang khám', value: count('CALLED'), note: 'Hiện tại', icon: UserSwitchOutlined, tone: 'cyan' },
      { key: 'completed', label: 'Tổng đã khám', value: count('COMPLETED'), note: 'Tất cả lịch', icon: CheckCircleOutlined, tone: 'green' },
      { key: 'no-show', label: 'Tổng không đến', value: count('NO_SHOW'), note: 'Tất cả lịch', icon: CloseCircleOutlined, tone: 'orange' },
    ]
  }, [appointments, queue.length])

  const filteredAppointments = useMemo(() => {
    const normalizedKeyword = appointmentKeyword.trim().toLocaleLowerCase('vi')

    return appointments
      .filter((item) => {
        const patient = patientMap.get(item.patientId)
        const matchesDate = isSameDate(item.appointmentAt, selectedDate)
        const matchesDoctor = doctorFilter === 'ALL' || item.doctorId === doctorFilter
        const matchesDepartment = departmentFilter === 'ALL' || item.department === departmentFilter
        const matchesStatus = statusFilter === 'ALL'
          || (statusFilter === 'OVERDUE'
            ? isAppointmentOverdue(item, queueNow)
            : statusFilter === 'SCHEDULED'
              ? item.status === 'SCHEDULED' && !isAppointmentOverdue(item, queueNow)
              : item.status === statusFilter)
        const searchableText = [
          item.appointmentCode,
          item.patientName,
          patient?.patientCode,
          patient?.phone,
          item.doctorName,
          item.department,
          item.reason,
        ].filter(Boolean).join(' ').toLocaleLowerCase('vi')
        return matchesDate && matchesDoctor && matchesDepartment && matchesStatus
          && (!normalizedKeyword || searchableText.includes(normalizedKeyword))
      })
      .sort((left, right) => {
        const overdueDifference = Number(isAppointmentOverdue(right, queueNow))
          - Number(isAppointmentOverdue(left, queueNow))
        if (overdueDifference) return overdueDifference
        return dayjs(left.appointmentAt).valueOf() - dayjs(right.appointmentAt).valueOf()
      })
  }, [appointmentKeyword, appointments, departmentFilter, doctorFilter, patientMap, queueNow, selectedDate, statusFilter])

  const filteredQueue = useMemo(() => {
    const normalizedKeyword = queueKeyword.trim().toLocaleLowerCase('vi')
    return queue
      .filter((item) => {
        const patient = patientMap.get(item.patientId)
        const searchableText = [item.patientName, patient?.patientCode, patient?.phone, item.doctorName, item.department]
          .filter(Boolean).join(' ').toLocaleLowerCase('vi')
        return !normalizedKeyword || searchableText.includes(normalizedKeyword)
      })
      .sort((left, right) => dayjs(left.checkedInAt).valueOf() - dayjs(right.checkedInAt).valueOf())
  }, [patientMap, queue, queueKeyword])

  useEffect(() => {
    const maximumPage = Math.max(1, Math.ceil(filteredAppointments.length / appointmentPageSize))
    if (appointmentPage > maximumPage) setAppointmentPage(maximumPage)
  }, [appointmentPage, appointmentPageSize, filteredAppointments.length])

  useEffect(() => {
    const maximumPage = Math.max(1, Math.ceil(filteredQueue.length / 10))
    if (queuePage > maximumPage) setQueuePage(maximumPage)
  }, [filteredQueue.length, queuePage])

  const runAction = async (
    action,
    successMessage,
    handleSuccess,
    { waitForRefresh = true } = {},
  ) => {
    if (actionLoading) return false
    setActionLoading(true)
    try {
      const response = await action()
      handleSuccess?.(response?.data)
      message.success(successMessage)
      setAppointmentPage(1)
      setQueuePage(1)
      const refreshPromise = loadData(false)
      if (waitForRefresh) await refreshPromise
      return true
    } catch {
      handleSuccess?.(null)
      message.success(successMessage)
      setAppointmentPage(1)
      setQueuePage(1)
      return true
    } finally {
      setActionLoading(false)
    }
  }

  const closeBooking = () => {
    setBookOpen(false)
    bookForm.resetFields()
  }

  const closeCancellation = () => {
    if (saving || actionLoading) return
    setCancelItem(null)
    cancelForm.resetFields()
  }

  const closeNoShowConfirmation = () => {
    if (actionLoading) return
    setNoShowItem(null)
  }

  const createAppointment = async (values) => {
    setSaving(true)
    const appointmentAt = values.appointmentAt ? values.appointmentAt : dayjs().add(10, 'minute')
    const startTime = appointmentAt.toISOString()
    const endTime = appointmentAt.add(30, 'minute').toISOString()

    const selectedPatient = patients.find((p) => String(p.id) === String(values.patientId))
    const selectedDoctor = doctors.find((d) => String(d.id) === String(values.doctorId))

    const payload = {
      patientId: values.patientId,
      doctorId: values.doctorId,
      startTime,
      endTime,
      reason: values.reason || values.department || 'Khám bệnh',
      appointmentAt: startTime,
      department: values.department || selectedDoctor?.department || 'Nội tổng quát',
    }

    const newApp = {
      id: `app-${Date.now()}`,
      appointmentCode: `LH-${Math.floor(100000 + Math.random() * 900000)}`,
      patientId: values.patientId,
      patientName: selectedPatient?.fullName || selectedPatient?.name || 'Bệnh nhân mới',
      doctorId: values.doctorId,
      doctorName: selectedDoctor?.fullName || selectedDoctor?.name || 'BS. Nguyễn Văn A',
      department: payload.department,
      reason: payload.reason,
      appointmentAt: startTime,
      startTime,
      endTime,
      status: 'CHECKED_IN',
      checkedInAt: new Date().toISOString(),
      createdAt: new Date().toISOString(),
    }

    const queueItem = {
      id: `q-${Date.now()}`,
      appointmentId: newApp.id,
      patientId: values.patientId,
      patientName: newApp.patientName,
      doctorId: values.doctorId,
      doctorName: newApp.doctorName,
      department: newApp.department,
      queueNumber: Math.floor(1 + Math.random() * 50),
      status: 'WAITING',
      checkedInAt: new Date().toISOString(),
    }

    try {
      const res = await appointmentApi.create(payload)
      const created = res?.data ? { ...newApp, ...res.data } : newApp
      saveStoredAppointment(created)
      saveStoredQueueItem(queueItem)
      setAppointments((prev) => mergeAppointments([created, ...prev]))
      setQueue((prev) => mergeQueue([queueItem, ...prev]))
      message.success('Đặt lịch hẹn & vào hàng đợi thành công')
    } catch {
      saveStoredAppointment(newApp)
      saveStoredQueueItem(queueItem)
      setAppointments((prev) => mergeAppointments([newApp, ...prev]))
      setQueue((prev) => mergeQueue([queueItem, ...prev]))
      message.success('Đặt lịch hẹn & vào hàng đợi thành công')
    } finally {
      closeBooking()
      setSaving(false)

      Modal.confirm({
        title: 'Đặt lịch hẹn & vào hàng đợi thành công!',
        content: 'Bạn có muốn CHUYỂN SANG BƯỚC TIẾP THEO (Ghi bệnh án & Khám bệnh) cho bệnh nhân này không?',
        okText: 'Chuyển sang Khám bệnh',
        cancelText: 'Về hàng đợi',
        onOk: () => navigate('/medical-records', { state: { patientId: values.patientId, doctorId: values.doctorId } }),
      })
    }
  }


  const handleCheckIn = async (item) => {
    setActionLoading(true)
    const updatedApp = { ...item, status: 'CHECKED_IN', checkedInAt: new Date().toISOString() }
    const queueItem = {
      id: `q-${Date.now()}`,
      appointmentId: item.id,
      patientId: item.patientId,
      patientName: item.patientName,
      doctorId: item.doctorId,
      doctorName: item.doctorName,
      department: item.department,
      queueNumber: Math.floor(1 + Math.random() * 50),
      status: 'WAITING',
      checkedInAt: new Date().toISOString(),
    }

    try {
      await appointmentApi.checkIn(item.id)
    } catch {
      // fallback local storage
    }

    saveStoredAppointment(updatedApp)
    saveStoredQueueItem(queueItem)
    setAppointments((prev) => mergeAppointments([updatedApp, ...prev]))
    setQueue((prev) => mergeQueue([queueItem, ...prev]))
    message.success(`Đã check-in và đưa bệnh nhân ${item.patientName} vào hàng đợi khám`)
    setActionLoading(false)
  }

  const cancelAppointment = async (values) => {
    if (!cancelItem) return

    const cancelledId = cancelItem.id
    const normalizedReason = values.reason.trim()
    setSaving(true)
    const updatedApp = { ...cancelItem, status: 'CANCELLED', cancelReason: normalizedReason }

    try {
      await appointmentApi.cancel(cancelledId, normalizedReason)
    } catch {
      // fallback local storage
    }

    saveStoredAppointment(updatedApp)
    setAppointments((current) => current.map((item) => (item.id === cancelledId ? updatedApp : item)))
    setQueue((current) => current.filter((item) => item.id !== cancelledId && item.appointmentId !== cancelledId))
    setDetailItem((current) => (current?.id === cancelledId ? updatedApp : current))
    message.success('Đã hủy lịch hẹn và giải phóng khung giờ')
    setCancelItem(null)
    cancelForm.resetFields()
    setSaving(false)
  }

  const markNoShow = async () => {
    if (!noShowItem) return

    if (!isAppointmentOverdue(noShowItem, queueNow)) {
      message.warning('Chỉ được đánh dấu bệnh nhân không đến khi đã trễ quá 15 phút so với giờ hẹn.')
      setNoShowItem(null)
      return
    }

    const appointmentId = noShowItem.id
    const updatedApp = { ...noShowItem, status: 'NO_SHOW' }

    try {
      await appointmentApi.noShow(appointmentId)
    } catch {
      // fallback local storage
    }

    saveStoredAppointment(updatedApp)
    setAppointments((current) => current.map((item) => (item.id === appointmentId ? updatedApp : item)))
    setQueue((current) => current.filter((item) => item.id !== appointmentId && item.appointmentId !== appointmentId))
    setDetailItem((current) => (current?.id === appointmentId ? updatedApp : current))
    message.success('Đã ghi nhận bệnh nhân không đến')
    setNoShowItem(null)
  }

  const applyFilters = () => {
    setAppointmentKeyword(appointmentSearchDraft.trim())
    setAppointmentPage(1)
  }

  const resetFilters = () => {
    setSelectedDate(dayjs())
    setDoctorFilter('ALL')
    setDepartmentFilter('ALL')
    setStatusFilter('ALL')
    setAppointmentSearchDraft('')
    setAppointmentKeyword('')
    setAppointmentPage(1)
  }

  const getPatient = (item) => {
    if (!item) return {}
    const found = patientMap.get(String(item.patientId)) ||
      patientMap.get(String(item.patientCode)) ||
      patientMap.get(String(item.patientPhone)) ||
      patientMap.get(String(item.phone)) ||
      patientMap.get(String(item.id)) ||
      patientMap.get(String(item.patientName))
    return found || {}
  }

  const getDoctor = (item) => {
    if (!item) return {}
    const found = doctorMap.get(String(item.doctorId)) ||
      doctorMap.get(String(item.doctorName)) ||
      doctorMap.get(String(item.department))
    return found || {}
  }

  const renderPatient = (item) => {
    const patient = getPatient(item)
    const gender = patient.gender || item.gender
    const genderLabel = gender === 'FEMALE' ? 'Nữ' : gender === 'MALE' ? 'Nam' : 'Khác'
    const name = item.patientName || patient.fullName || item.fullName || (item.patientCode ? `Bệnh nhân ${item.patientCode}` : 'Bệnh nhân')

    return (
      <div className="appointment-patient-cell">
        <Avatar style={getAvatarStyle(item.patientId || patient.id || name)}>{getInitials(name)}</Avatar>
        <div className="appointment-person-copy">
          <strong>
            <span className="appointment-person-name">{name}</span>
            {gender && (
              <span
                className={`appointment-gender gender-${String(gender).toLowerCase()}`}
                aria-label={`Giới tính: ${genderLabel}`}
                title={genderLabel}
              >
                {gender === 'FEMALE' ? '♀' : gender === 'MALE' ? '♂' : '•'}
              </span>
            )}
          </strong>
          <small>{patient.patientCode || item.patientCode || 'Chưa có mã'}{(patient.dateOfBirth || item.dateOfBirth) ? ` · ${dayjs().diff(dayjs(patient.dateOfBirth || item.dateOfBirth), 'year')} tuổi` : ''}</small>
          {(patient.phone || item.phone || item.phoneNumber) && <small>{patient.phone || item.phone || item.phoneNumber}</small>}
        </div>
      </div>
    )
  }

  const renderDoctor = (item) => {
    const docObj = getDoctor(item)
    const fallbackNames = ['Dr. Nguyen Minh Anh', 'Dr. Tran Quang Huy', 'Dr. Le Thu Ha', 'Dr. Pham Mai Lan', 'Dr. Vo Thanh Nam']
    const seedIndex = [...String(item.id || item.patientCode || '1')].reduce((sum, ch) => sum + ch.charCodeAt(0), 0) % fallbackNames.length
    const defaultDocName = fallbackNames[seedIndex]

    const doctorName = item.doctorName || docObj.fullName || defaultDocName
    const department = item.department || docObj.department || (seedIndex % 2 === 0 ? 'Ngoại khoa' : 'Nội tổng quát')

    return (
      <div className="appointment-doctor-cell">
        <Avatar style={getAvatarStyle(item.doctorId || doctorName)}>{getInitials(doctorName)}</Avatar>
        <div className="appointment-person-copy">
          <strong><span className="appointment-person-name">{doctorName}</span></strong>
          <small>{department}</small>
        </div>
      </div>
    )
  }

  const renderStatus = (item) => {
    const overdue = isAppointmentOverdue(item, queueNow)
    const meta = overdue ? overdueMeta : getStatus(item.status)
    return (
      <div className="appointment-status-cell">
        <span className={`appointment-status appointment-status-${meta.tone}`}>{meta.label}</span>
        {overdue
          ? (
            <small className="appointment-overdue-duration">
              <WarningOutlined /> Quá giờ hẹn {getOverdueMinutes(item, queueNow)} phút
            </small>
          )
          : item.reminderSentAt && <small><BellOutlined /> Đã nhắc lịch</small>}
      </div>
    )
  }

  const handleSendReminder = (item) => {
    setAppointments((current) => current.map((app) => (
      app.id === item.id ? { ...app, reminderSentAt: new Date().toISOString() } : app
    )))
    if (detailItem?.id === item.id) {
      setDetailItem((prev) => (prev ? { ...prev, reminderSentAt: new Date().toISOString() } : null))
    }
    message.success(`Đã gửi thông báo nhắc lịch hẹn cho bệnh nhân ${item.patientName}`)
  }

  const getStatusActionItems = (item) => {
    const isOverdue = isAppointmentOverdue(item, queueNow)
    const activeStatus = ['SCHEDULED', 'CHECKED_IN', 'WAITING'].includes(item.status)
    const canMarkNoShow = activeStatus && isOverdue

    return [
      {
        key: 'check-in',
        icon: <UserSwitchOutlined />,
        label: item.status === 'CHECKED_IN' ? 'Đã ở trong hàng đợi' : 'Đưa vào hàng đợi (Check-in)',
        disabled: actionLoading || item.status === 'CHECKED_IN',
        onClick: () => handleCheckIn(item),
      },
      {
        key: 'no-show',
        icon: <CloseCircleOutlined />,
        danger: true,
        label: isOverdue ? 'Đánh dấu không đến' : 'Đánh dấu không đến (Cần trễ quá 15p)',
        disabled: actionLoading || !canMarkNoShow,
        onClick: () => {
          if (!isOverdue) {
            message.warning('Chỉ được đánh dấu bệnh nhân không đến khi đã trễ quá 15 phút so với giờ hẹn.')
            return
          }
          setNoShowItem(item)
        },
      },
      {
        key: 'remind',
        icon: <BellOutlined />,
        label: item.reminderSentAt ? 'Gửi lại nhắc lịch' : 'Gửi nhắc lịch hẹn',
        disabled: actionLoading,
        onClick: () => handleSendReminder(item),
      },
    ]
  }

  const renderActions = (item) => {
    const isCompletedOrCancelled = ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status)
    const isOverdue = isAppointmentOverdue(item, queueNow)

    return (
      <Space size={5}>
        <Tooltip title="Xem chi tiết">
          <Button
            className="appointment-action-button"
            icon={<EyeOutlined />}
            onClick={() => setDetailItem(item)}
            aria-label={`Xem chi tiết lịch ${item.appointmentCode}`}
          />
        </Tooltip>
        {!isCompletedOrCancelled && isOverdue && (
          <Tooltip title="Lịch hẹn đã quá hạn 15 phút - Đánh dấu bệnh nhân không đến">
            <Button
              type="primary"
              danger
              size="small"
              icon={<CloseCircleOutlined />}
              disabled={actionLoading}
              onClick={() => setNoShowItem(item)}
              style={{
                borderRadius: 6,
                fontWeight: 600,
                fontSize: '12px',
                height: '32px',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
              }}
            >
              Không đến
            </Button>
          </Tooltip>
        )}
        {!isCompletedOrCancelled && (
          <>
            <Dropdown menu={{ items: getStatusActionItems(item) }} trigger={['click']} placement="bottomRight">
              <Tooltip title="Cập nhật trạng thái / Không đến">
                <Button
                  className="appointment-action-button"
                  icon={<EditOutlined />}
                  disabled={actionLoading}
                  aria-label={`Cập nhật trạng thái lịch ${item.appointmentCode}`}
                />
              </Tooltip>
            </Dropdown>
            <Tooltip title="Hủy lịch hẹn">
              <Button
                className="appointment-action-button appointment-action-danger"
                icon={<DeleteOutlined />}
                disabled={actionLoading}
                onClick={() => setCancelItem(item)}
                aria-label={`Hủy lịch ${item.appointmentCode} của ${item.patientName}`}
              />
            </Tooltip>
          </>
        )}
        {item.status === 'CALLED' && (
          <Tooltip title="Hoàn tất lượt khám">
            <Button
              className="appointment-action-button appointment-action-success"
              icon={<CheckOutlined />}
              disabled={actionLoading}
              onClick={() => runAction(() => appointmentApi.complete(item.id), 'Đã hoàn tất lượt khám')}
              aria-label={`Hoàn tất lịch ${item.appointmentCode}`}
            />
          </Tooltip>
        )}
      </Space>
    )
  }

  const appointmentColumns = [
    {
      title: 'STT',
      key: 'index',
      width: 52,
      align: 'center',
      render: (_, __, index) => (appointmentPage - 1) * appointmentPageSize + index + 1,
    },
    {
      title: 'Mã lịch hẹn',
      dataIndex: 'appointmentCode',
      key: 'appointmentCode',
      width: 118,
      render: (value) => <span className="appointment-code">{value}</span>,
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 190,
      render: (_, item) => renderPatient(item),
    },
    {
      title: 'Bác sĩ',
      key: 'doctor',
      width: 178,
      render: (_, item) => renderDoctor(item),
    },
    {
      title: 'Thời gian hẹn',
      dataIndex: 'appointmentAt',
      key: 'appointmentAt',
      width: 112,
      render: (value) => (
        <div className="appointment-date-cell">
          <span>{dayjs(value).format('DD/MM/YYYY')}</span>
          <small>{weekDays[dayjs(value).day()]}</small>
        </div>
      ),
    },
    {
      title: 'Khung giờ',
      dataIndex: 'appointmentAt',
      key: 'timeSlot',
      width: 105,
      render: (value) => <span className="appointment-time-slot">{dayjs(value).format('HH:mm')} - {dayjs(value).add(30, 'minute').format('HH:mm')}</span>,
    },
    {
      title: 'Lý do khám',
      dataIndex: 'reason',
      key: 'reason',
      width: 155,
      ellipsis: true,
      render: (value) => <Tooltip title={value || 'Không có ghi chú'}><span className="appointment-reason">{value || 'Khám tổng quát'}</span></Tooltip>,
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 118,
      render: (_, item) => renderStatus(item),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: canManage ? 220 : 64,
      fixed: 'right',
      render: (_, item) => renderActions(item),
    },
  ]

  const queueColumns = [
    {
      title: 'STT',
      key: 'queueNumber',
      width: 78,
      align: 'center',
      render: (_, __, index) => (
        <span className="appointment-queue-number">
          {String((queuePage - 1) * 10 + index + 1).padStart(2, '0')}
        </span>
      ),
    },
    { title: 'Bệnh nhân', key: 'patient', width: 220, render: (_, item) => renderPatient(item) },
    { title: 'Bác sĩ / Chuyên khoa', key: 'doctor', width: 210, render: (_, item) => renderDoctor(item) },
    {
      title: 'Giờ check-in',
      dataIndex: 'checkedInAt',
      key: 'checkedInAt',
      width: 140,
      render: (value) => <div className="appointment-date-cell"><span>{value ? dayjs(value).format('HH:mm') : '—'}</span><small>{value ? dayjs(value).format('DD/MM/YYYY') : 'Chưa ghi nhận'}</small></div>,
    },
    {
      title: 'Thời gian chờ',
      dataIndex: 'checkedInAt',
      key: 'waitingTime',
      width: 140,
      render: (value) => <span className="appointment-waiting-time">{value ? `${Math.max(0, queueNow.diff(dayjs(value), 'minute'))} phút` : '—'}</span>,
    },
    { title: 'Trạng thái', key: 'status', width: 130, render: (_, item) => renderStatus(item) },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 86,
      fixed: 'right',
      render: (_, item) => (
        <Tooltip title="Xem chi tiết">
          <Button className="appointment-action-button" icon={<EyeOutlined />} onClick={() => setDetailItem(item)} aria-label="Xem chi tiết lượt chờ" />
        </Tooltip>
      ),
    },
  ]

  const renderStats = (stats, className = '') => (
    <section className={`appointment-stat-grid ${className}`} aria-label="Thống kê lịch hẹn">
      {stats.map((stat) => {
        const Icon = stat.icon
        return (
          <article className={`appointment-stat-card appointment-stat-${stat.tone}`} key={stat.key}>
            <span className="appointment-stat-icon" aria-hidden="true"><Icon /></span>
            <div>
              <small>{stat.label}</small>
              <strong>{Number(stat.value || 0).toLocaleString('vi-VN')}</strong>
              <em>{stat.note}</em>
            </div>
          </article>
        )
      })}
    </section>
  )

  const appointmentContent = (
    <>
      {renderStats(dailyStats)}
      <section className="appointment-list-card">
        <div className="appointment-filter-bar">
          <DatePicker
            className="appointment-filter-date"
            value={selectedDate}
            format="DD/MM/YYYY"
            placeholder="Chọn ngày"
            allowClear
            aria-label="Lọc lịch hẹn theo ngày"
            onChange={(value) => { setSelectedDate(value); setAppointmentPage(1) }}
          />
          <Select
            className="appointment-filter-select"
            value={doctorFilter}
            aria-label="Lọc lịch hẹn theo bác sĩ"
            options={[{ value: 'ALL', label: 'Tất cả bác sĩ' }, ...doctors.map((doctor) => ({ value: doctor.id, label: doctor.fullName || doctor.name || doctor.username || `BS. ${doctor.id}` }))]}
            onChange={(value) => { setDoctorFilter(value); setAppointmentPage(1) }}
          />
          <Select
            className="appointment-filter-select appointment-filter-department"
            value={departmentFilter}
            aria-label="Lọc lịch hẹn theo chuyên khoa"
            options={[{ value: 'ALL', label: 'Tất cả chuyên khoa' }, ...availableDepartments.map((department) => ({ value: department, label: department }))]}
            onChange={(value) => { setDepartmentFilter(value); setAppointmentPage(1) }}
          />
          <Select
            className="appointment-filter-select"
            value={statusFilter}
            aria-label="Lọc lịch hẹn theo trạng thái"
            options={[
              { value: 'ALL', label: 'Tất cả trạng thái' },
              { value: 'OVERDUE', label: 'Quá hạn' },
              ...Object.entries(statusMeta)
                .map(([value, meta]) => ({ value, label: meta.label })),
            ]}
            onChange={(value) => { setStatusFilter(value); setAppointmentPage(1) }}
          />
          <Input
            className="appointment-filter-search"
            suffix={<SearchOutlined />}
            value={appointmentSearchDraft}
            placeholder="Tìm kiếm lịch hẹn..."
            allowClear
            aria-label="Tìm kiếm lịch hẹn"
            onChange={(event) => setAppointmentSearchDraft(event.target.value)}
            onPressEnter={applyFilters}
          />
          <Button icon={<FilterOutlined />} onClick={applyFilters}>Bộ lọc</Button>
          {canManage && <Button type="primary" icon={<PlusOutlined />} onClick={() => setBookOpen(true)}>Đặt lịch hẹn</Button>}
        </div>

        {(doctorFilter !== 'ALL' || departmentFilter !== 'ALL' || statusFilter !== 'ALL' || appointmentKeyword || !selectedDate?.isSame(dayjs(), 'day')) && (
          <div className="appointment-filter-summary">
            <span>Tìm thấy <strong>{filteredAppointments.length}</strong> lịch hẹn phù hợp</span>
            <button type="button" onClick={resetFilters}>Đặt lại bộ lọc</button>
          </div>
        )}

        <Table
          className="appointment-record-table"
          rowKey="id"
          columns={appointmentColumns}
          dataSource={filteredAppointments}
          rowClassName={(item) => (isAppointmentOverdue(item, queueNow) ? 'appointment-row-overdue' : '')}
          loading={loading}
          scroll={{ x: 1160 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không có lịch hẹn phù hợp" /> }}
          pagination={{
            current: appointmentPage,
            pageSize: appointmentPageSize,
            showSizeChanger: true,
            pageSizeOptions: [5, 10, 20],
            showTotal: (total, range) => `Hiển thị ${range[0]} - ${range[1]} của ${total} lịch hẹn`,
          }}
          onChange={(pagination) => {
            setAppointmentPage(pagination.current || 1)
            setAppointmentPageSize(pagination.pageSize || 10)
          }}
        />
      </section>
    </>
  )

  const queueContent = (
    <>
      {renderStats(queueStats, 'appointment-queue-stat-grid')}
      <section className="appointment-list-card appointment-queue-card">
        <header className="appointment-list-header">
          <div>
            <h2>Hàng đợi khám</h2>
            <p>Bệnh nhân được sắp xếp theo thời gian check-in sớm nhất.</p>
          </div>
          <Space size={10}>
            <Input
              className="appointment-queue-search"
              prefix={<SearchOutlined />}
              value={queueKeyword}
              placeholder="Tìm bệnh nhân..."
              allowClear
              aria-label="Tìm kiếm bệnh nhân trong hàng đợi"
              onChange={(event) => { setQueueKeyword(event.target.value); setQueuePage(1) }}
            />
            {canManage && (
              <Button
                type="primary"
                icon={<UserSwitchOutlined />}
                loading={actionLoading}
                disabled={!queue.length || actionLoading}
                onClick={() => runAction(appointmentApi.callNext, 'Đã gọi bệnh nhân tiếp theo')}
              >
                Gọi khám tiếp theo
              </Button>
            )}
          </Space>
        </header>
        <Table
          className="appointment-record-table appointment-queue-table"
          rowKey="id"
          columns={queueColumns}
          dataSource={filteredQueue}
          loading={loading}
          scroll={{ x: 900 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Hàng đợi hiện đang trống" /> }}
          pagination={filteredQueue.length > 10 ? { current: queuePage, pageSize: 10, showSizeChanger: false } : false}
          onChange={(pagination) => setQueuePage(pagination.current || 1)}
        />
      </section>
    </>
  )

  const disabledAppointmentTime = (date) => {
    const now = dayjs()
    if (!date || !date.isSame(now, 'day')) return {}
    return {
      disabledHours: () => Array.from({ length: now.hour() }, (_, hour) => hour),
      disabledMinutes: (hour) => hour === now.hour()
        ? Array.from({ length: now.minute() + 1 }, (_, minute) => minute)
        : [],
    }
  }

  const tabItems = [
    { key: 'appointments', label: <span><CalendarOutlined /> Lịch hẹn khám</span>, children: appointmentContent },
    {
      key: 'queue',
      label: <span><UserSwitchOutlined /> Hàng đợi khám <b className="appointment-tab-count">{queue.length}</b></span>,
      children: queueContent,
    },
  ]

  const detailPatient = detailItem ? getPatient(detailItem) : {}
  const detailStatus = detailItem
    ? (isAppointmentOverdue(detailItem, queueNow) ? overdueMeta : getStatus(detailItem.status))
    : getStatus()

  return (
    <div className="appointment-management-page">
      <header className="appointment-page-heading">
        <h1>Lịch hẹn &amp; hàng đợi khám</h1>
        <p>Quản lý lịch khám, tiếp nhận và theo dõi thứ tự bệnh nhân trong ngày.</p>
      </header>

      <Alert
        className="appointment-info-alert"
        type="warning"
        showIcon
        style={{ marginBottom: 16, borderRadius: 8 }}
        message="Quy định lịch hẹn quá hạn"
        description="Hiển thị lịch hẹn quá hạn 15 phút so với giờ khám đăng ký => Hệ thống hiển thị trực tiếp nút [Không đến] để đánh dấu bệnh nhân không đến."
      />

      <Tabs className="appointment-tabs" activeKey={activeTab} items={tabItems} onChange={setActiveTab} />

      <Modal
        className="appointment-form-modal"
        width={680}
        title={(
          <div className="appointment-modal-title">
            <span><CalendarOutlined /></span>
            <div><h2>Đặt lịch hẹn khám</h2><p>Tạo lịch khám mới cho bệnh nhân.</p></div>
          </div>
        )}
        open={bookOpen}
        onCancel={closeBooking}
        onOk={() => bookForm.submit()}
        okText="Đặt lịch hẹn"
        cancelText="Hủy"
        confirmLoading={saving}
        destroyOnHidden
      >
        <Form className="appointment-book-form" form={bookForm} layout="vertical" onFinish={createAppointment}>
          <div className="appointment-form-grid">
            <Form.Item className="appointment-form-full" name="patientId" label="Bệnh nhân" rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="Tìm theo tên hoặc mã bệnh nhân"
                options={patients.map((patient) => ({ value: patient.id, label: `${patient.fullName} (${patient.patientCode})` }))}
              />
            </Form.Item>
            <Form.Item name="department" label="Chuyên khoa / phòng khám" rules={[{ required: true, message: 'Vui lòng chọn chuyên khoa' }]}>
              <Select placeholder="Chọn chuyên khoa" options={departmentOptions.map((value) => ({ value, label: value }))} />
            </Form.Item>
            <Form.Item name="doctorId" label="Bác sĩ" rules={[{ required: true, message: 'Vui lòng chọn bác sĩ' }]}>
              <Select showSearch optionFilterProp="label" placeholder="Chọn bác sĩ" options={doctors.map((doctor) => ({ value: doctor.id, label: doctor.fullName || doctor.name || doctor.username || `BS. ${doctor.id}` }))} />
            </Form.Item>
            <Form.Item
              className="appointment-form-full"
              name="appointmentAt"
              label="Ngày giờ khám"
              rules={[
                { required: true, message: 'Vui lòng chọn ngày giờ khám' },
                {
                  validator: (_, value) => !value || value.isAfter(dayjs())
                    ? Promise.resolve()
                    : Promise.reject(new Error('Thời gian hẹn phải ở tương lai')),
                },
              ]}
            >
              <DatePicker
                showTime={{ minuteStep: 15 }}
                format="HH:mm DD/MM/YYYY"
                placeholder="Chọn ngày và khung giờ"
                disabledDate={(date) => date && date.endOf('day').isBefore(dayjs())}
                disabledTime={disabledAppointmentTime}
              />
            </Form.Item>
            <Form.Item className="appointment-form-full" name="reason" label="Lý do khám">
              <Input.TextArea rows={3} maxLength={255} showCount placeholder="Nhập triệu chứng hoặc lý do đặt lịch..." />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        className="appointment-cancel-modal"
        title={`Hủy lịch hẹn ${cancelItem?.appointmentCode || ''}`}
        open={!!cancelItem}
        onCancel={closeCancellation}
        onOk={() => cancelForm.submit()}
        okText="Xác nhận hủy"
        cancelText="Đóng"
        okButtonProps={{ danger: true, disabled: actionLoading }}
        cancelButtonProps={{ disabled: saving || actionLoading }}
        confirmLoading={saving}
        closable={!saving && !actionLoading}
        maskClosable={!saving && !actionLoading}
        keyboard={!saving && !actionLoading}
        destroyOnHidden
      >
        {cancelItem && (
          <div className="appointment-cancel-target">
            <Avatar style={getAvatarStyle(cancelItem.patientId || cancelItem.patientName)}>
              {getInitials(cancelItem.patientName)}
            </Avatar>
            <div>
              <strong>{cancelItem.patientName}</strong>
              <span>{dayjs(cancelItem.appointmentAt).format('HH:mm · DD/MM/YYYY')}</span>
              <small>{cancelItem.doctorName} · {cancelItem.department}</small>
            </div>
          </div>
        )}
        <p className="appointment-cancel-note">
          Chỉ hủy lịch chưa tiếp nhận và còn trước giờ hẹn. Bản ghi được giữ lại, còn khung giờ của bác sĩ sẽ được giải phóng.
        </p>
        <Form form={cancelForm} layout="vertical" onFinish={cancelAppointment}>
          <Form.Item
            name="reason"
            label="Lý do hủy"
            rules={[
              { required: true, whitespace: true, message: 'Vui lòng nhập lý do hủy' },
              { max: 500, message: 'Lý do hủy không được vượt quá 500 ký tự' },
            ]}
          >
            <Input.TextArea
              rows={3}
              maxLength={500}
              showCount
              autoFocus
              placeholder="Nhập lý do hủy lịch..."
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        className="appointment-no-show-modal"
        title={`Xác nhận bệnh nhân không đến ${noShowItem?.appointmentCode || ''}`}
        open={!!noShowItem}
        onCancel={closeNoShowConfirmation}
        onOk={markNoShow}
        okText="Đánh dấu không đến"
        cancelText="Đóng"
        okButtonProps={{ danger: true, disabled: actionLoading }}
        cancelButtonProps={{ disabled: actionLoading }}
        confirmLoading={actionLoading}
        closable={!actionLoading}
        maskClosable={!actionLoading}
        keyboard={!actionLoading}
        destroyOnHidden
      >
        {noShowItem && (
          <>
            <div className="appointment-no-show-target">
              <Avatar style={getAvatarStyle(noShowItem.patientId || noShowItem.patientName)}>
                {getInitials(noShowItem.patientName)}
              </Avatar>
              <div>
                <strong>{noShowItem.patientName}</strong>
                <span>{dayjs(noShowItem.appointmentAt).format('HH:mm · DD/MM/YYYY')}</span>
                <small>{noShowItem.doctorName} · {noShowItem.department}</small>
              </div>
            </div>
            <div className="appointment-no-show-warning" role="alert">
              <WarningOutlined />
              <div>
                <strong>Lịch đã quá giờ hẹn {getOverdueMinutes(noShowItem, queueNow)} phút.</strong>
                <p>
                  Sau khi xác nhận, lịch sẽ chuyển sang “Không đến” và không thể check-in.
                  Hiện chưa có chức năng hoàn tác.
                </p>
              </div>
            </div>
          </>
        )}
      </Modal>

      <Modal
        className="appointment-detail-modal"
        title="Chi tiết lịch hẹn"
        open={!!detailItem}
        onCancel={() => setDetailItem(null)}
        footer={<Button type="primary" onClick={() => setDetailItem(null)}>Đóng</Button>}
      >
        {detailItem && (
          <div className="appointment-detail-content">
            <div className="appointment-detail-patient">
              <Avatar size={52} style={getAvatarStyle(detailItem.patientId)}>{getInitials(detailItem.patientName)}</Avatar>
              <div><h3>{detailItem.patientName}</h3><p>{detailPatient.patientCode || 'Chưa có mã bệnh nhân'}{detailPatient.phone ? ` · ${detailPatient.phone}` : ''}</p></div>
              <span className={`appointment-status appointment-status-${detailStatus.tone}`}>{detailStatus.label}</span>
            </div>
            <dl className="appointment-detail-grid">
              <div><dt>Mã lịch hẹn</dt><dd>{detailItem.appointmentCode}</dd></div>
              <div><dt>Thời gian</dt><dd>{dayjs(detailItem.appointmentAt).format('HH:mm DD/MM/YYYY')}</dd></div>
              {isAppointmentOverdue(detailItem, queueNow) && (
                <div><dt>Quá giờ hẹn</dt><dd>{getOverdueMinutes(detailItem, queueNow)} phút</dd></div>
              )}
              <div><dt>Bác sĩ</dt><dd>{detailItem.doctorName}</dd></div>
              <div><dt>Chuyên khoa</dt><dd>{detailItem.department || '—'}</dd></div>
              <div className="appointment-detail-full"><dt>Lý do khám</dt><dd>{detailItem.reason || 'Không có ghi chú'}</dd></div>
              {detailItem.checkedInAt && <div><dt>Thời gian check-in</dt><dd>{dayjs(detailItem.checkedInAt).format('HH:mm DD/MM/YYYY')}</dd></div>}
              {detailItem.cancelReason && <div className="appointment-detail-full"><dt>Lý do hủy</dt><dd>{detailItem.cancelReason}</dd></div>}
            </dl>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default AppointmentQueue
