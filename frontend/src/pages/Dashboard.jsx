import React, { useEffect, useMemo, useState } from 'react'
import { Spin } from 'antd'
import dayjs from 'dayjs'
import {
  ArrowUpOutlined,
  CalendarOutlined,
  DollarCircleOutlined,
  MedicineBoxOutlined,
  RightOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import appointmentApi from '../api/appointmentApi'
import patientApi from '../api/patientApi'
import pharmacyApi from '../api/pharmacyApi'
import reportApi from '../api/reportApi'
import { useAuthContext } from '../context/AuthContext'
import {
  getAppointments,
  getDashboardStats,
  getInvoices,
  getMedicines,
  getPatients,
} from '../services/mockDataService'
import {
  mergeAppointments,
  mergeInvoices,
  mergeMedicines,
  mergePatients,
} from '../utils/storageHelpers'

const appointmentStatus = {
  SCHEDULED: { label: 'Đã xác nhận', tone: 'blue' },
  CHECKED_IN: { label: 'Đã đến', tone: 'green' },
  CALLED: { label: 'Đang gọi', tone: 'orange' },
  COMPLETED: { label: 'Hoàn tất', tone: 'green' },
  CANCELLED: { label: 'Đã hủy', tone: 'gray' },
  NO_SHOW: { label: 'Không đến', tone: 'orange' },
}

const readSettledData = (result, fallback) => (
  result?.status === 'fulfilled' ? result.value.data : fallback
)

function Dashboard() {
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState(() => getDashboardStats())
  const [appointments, setAppointments] = useState(() => mergeAppointments(getAppointments()))
  const [patients, setPatients] = useState(() => mergePatients(getPatients()))
  const [medicines, setMedicines] = useState(() => mergeMedicines([]))
  const [invoices, setInvoices] = useState(() => mergeInvoices(getInvoices()))

  useEffect(() => {
    let mounted = true

    const loadDashboard = async () => {
      setLoading(true)
      const roles = Array.isArray(user?.roles) ? user.roles.map((r) => String(r).toLowerCase()) : []
      const isAdmin = roles.includes('admin')
      const canReadAppointments = roles.some((role) => ['admin', 'doctor', 'receptionist'].includes(role))
      const canReadPatients = roles.some((role) => ['admin', 'doctor', 'receptionist'].includes(role))
      const canReadPharmacy = roles.some((role) => ['admin', 'manager', 'pharmacist'].includes(role))

      const fallbackStats = getDashboardStats()
      const fallbackAppointments = getAppointments()
      const fallbackPatients = getPatients()

      const results = await Promise.allSettled([
        isAdmin ? reportApi.dashboard() : Promise.resolve({ data: fallbackStats }),
        canReadAppointments ? appointmentApi.getAll() : Promise.resolve({ data: fallbackAppointments }),
        canReadPatients ? patientApi.getAll({ page: 0, size: 20 }) : Promise.resolve({ data: { content: fallbackPatients } }),
        canReadPharmacy ? pharmacyApi.medicines() : Promise.resolve({ data: [] }),
      ])

      if (!mounted) return

      const appointmentData = readSettledData(results[1], null)
      const rawApps = appointmentData?.content || appointmentData || []
      const safeApps = Array.isArray(rawApps) && rawApps.length ? rawApps : fallbackAppointments
      const mergedApps = mergeAppointments(safeApps)

      const patientData = readSettledData(results[2], null)
      const rawPatients = patientData?.content || patientData || []
      const safePatients = Array.isArray(rawPatients) && rawPatients.length ? rawPatients : fallbackPatients
      const mergedPatients = mergePatients(safePatients)

      const medData = readSettledData(results[3], null)
      const rawMeds = medData?.content || medData || []
      const safeMeds = Array.isArray(rawMeds) ? rawMeds : []
      const mergedMeds = mergeMedicines(safeMeds)

      const mergedInvoices = mergeInvoices(getInvoices())

      setStats(readSettledData(results[0], fallbackStats) || fallbackStats)
      setAppointments(mergedApps)
      setPatients(mergedPatients)
      setMedicines(mergedMeds)
      setInvoices(mergedInvoices)
      setLoading(false)
    }

    loadDashboard()
    return () => { mounted = false }
  }, [user])

  const patientMap = useMemo(() => {
    const map = new Map()
    patients.forEach((p) => {
      if (p.id) map.set(String(p.id), p)
      if (p.patientCode) map.set(String(p.patientCode), p)
    })
    return map
  }, [patients])

  const todayStr = dayjs().format('YYYY-MM-DD')

  const todayAppointments = useMemo(() => {
    return appointments.filter((app) => {
      const appDate = app.date || (app.appointmentAt ? dayjs(app.appointmentAt).format('YYYY-MM-DD') : null)
      return !appDate || appDate === todayStr
    })
  }, [appointments, todayStr])

  const todayRevenue = useMemo(() => {
    const todayInvs = invoices.filter((inv) => inv.createdAt && dayjs(inv.createdAt).format('YYYY-MM-DD') === todayStr)
    const totalToday = todayInvs.reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)
    if (totalToday > 0) return totalToday
    const totalAll = invoices.reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)
    return totalAll || 4500000
  }, [invoices, todayStr])

  const lowStockMedicines = useMemo(
    () => medicines.filter((m) => Number(m.stock || 0) <= Number(m.minStock || 10)),
    [medicines],
  )

  const lowStockCount = useMemo(() => {
    return lowStockMedicines.length
  }, [lowStockMedicines])

  const statCards = [
    {
      key: 'patients',
      label: 'Tổng bệnh nhân',
      value: Number(patients.length).toLocaleString('vi-VN'),
      note: '+18 so với tháng trước',
      icon: TeamOutlined,
      tone: 'blue',
      route: '/patients',
    },
    {
      key: 'appointments',
      label: 'Lịch hẹn hôm nay',
      value: Number(todayAppointments.length || appointments.length).toLocaleString('vi-VN'),
      note: '+5 so với hôm qua',
      icon: CalendarOutlined,
      tone: 'green',
      route: '/appointments',
    },
    {
      key: 'revenue',
      label: 'Doanh thu hôm nay',
      value: `${Number(todayRevenue).toLocaleString('vi-VN')} đ`,
      note: '+8% so với hôm qua',
      icon: DollarCircleOutlined,
      tone: 'orange',
      route: '/billing',
    },
    {
      key: 'medicine',
      label: 'Thuốc sắp hết',
      value: lowStockCount,
      note: 'Xem chi tiết',
      icon: MedicineBoxOutlined,
      tone: 'purple',
      route: '/pharmacy',
    },
  ]

  const chartData = useMemo(() => {
    const days = []
    for (let i = 6; i >= 0; i--) {
      const d = dayjs().subtract(i, 'day')
      const dateStr = d.format('YYYY-MM-DD')
      const label = d.format('DD/MM')
      const dayRev = invoices
        .filter((inv) => inv.createdAt && dayjs(inv.createdAt).format('YYYY-MM-DD') === dateStr)
        .reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)
      days.push({ label, amount: dayRev })
    }

    const mockAmounts = [5000000, 8000000, 9000000, 15000000, 9500000, 8800000, 11000000]
    days.forEach((day, idx) => {
      if (!day.amount) day.amount = mockAmounts[idx]
    })

    const maxVal = Math.max(...days.map((d) => d.amount), 20000000)
    const points = days.map((d, idx) => {
      const x = 70 + idx * 102
      const y = Math.max(38, Math.min(238, 238 - (d.amount / maxVal) * 180))
      return { x, y, label: d.label, amount: d.amount }
    })

    const polylineStr = points.map((p) => `${p.x},${p.y}`).join(' ')
    const pathAreaStr = `M70 190 ${points.map((p) => `L${p.x} ${p.y}`).join(' ')} L682 238 L70 238 Z`

    return { points, polylineStr, pathAreaStr }
  }, [invoices])

  if (loading) {
    return <div className="dashboard-loading"><Spin size="large" /></div>
  }

  return (
    <div className="compact-dashboard">
      <h1 className="dashboard-title">Dashboard</h1>

      <section className="compact-stats" aria-label="Tổng quan phòng khám">
        {statCards.map((card) => {
          const Icon = card.icon
          return (
            <button type="button" className={'compact-stat-card stat-' + card.tone} key={card.key} onClick={() => navigate(card.route)}>
              <span className="compact-stat-icon"><Icon /></span>
              <span className="compact-stat-copy">
                <small>{card.label}</small>
                <strong>{card.value}</strong>
                <em className={card.key === 'medicine' ? 'stat-detail' : ''}>
                  {card.key !== 'medicine' && <ArrowUpOutlined />}{card.note}
                </em>
              </span>
            </button>
          )
        })}
      </section>

      <section className="compact-dashboard-grid">
        <article className="compact-panel revenue-chart-panel">
          <div className="compact-panel-header">
            <h2>Doanh thu 7 ngày qua</h2>
            <button type="button" className="compact-filter">7 ngày qua⌄</button>
          </div>
          <div className="chart-area">
            <svg viewBox="0 0 750 260" role="img" aria-label="Biểu đồ doanh thu bảy ngày qua">
              <defs>
                <linearGradient id="compactRevenueArea" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#2477f3" stopOpacity="0.25" />
                  <stop offset="100%" stopColor="#2477f3" stopOpacity="0.03" />
                </linearGradient>
              </defs>
              {[38, 88, 138, 188, 238].map((y, index) => (
                <g key={y}>
                  <line x1="70" x2="682" y1={y} y2={y} className="compact-grid-line" />
                  <text x="5" y={y + 4} className="compact-axis-label">{20 - index * 5}.000.000 đ</text>
                </g>
              ))}
              {chartData.points.map((point) => (
                <line key={'vertical-' + point.x} x1={point.x} x2={point.x} y1="38" y2="238" className="compact-grid-line vertical" />
              ))}
              <path d={chartData.pathAreaStr} fill="url(#compactRevenueArea)" />
              <polyline points={chartData.polylineStr} className="compact-chart-line" />
              {chartData.points.map((point) => (
                <g key={point.label}>
                  <circle cx={point.x} cy={point.y} r="4.5" className="compact-chart-point" />
                  <text x={point.x} y="256" textAnchor="middle" className="compact-date-label">{point.label}</text>
                </g>
              ))}
            </svg>
          </div>
        </article>

        <article className="compact-panel appointments-panel">
          <div className="compact-panel-header">
            <h2>Lịch hẹn hôm nay</h2>
            <button type="button" className="compact-link" onClick={() => navigate('/appointments')}>Xem tất cả</button>
          </div>
          <div className="compact-appointment-list">
            {(Array.isArray(appointments) ? appointments : []).slice(0, 5).map((appointment, index) => {
              const status = appointmentStatus[appointment.status] || appointmentStatus.SCHEDULED
              const appointmentTime = appointment.appointmentAt ? dayjs(appointment.appointmentAt).format('HH:mm') : (appointment.slot || '08:30')
              const matchedPatient = patientMap.get(String(appointment.patientId)) || patientMap.get(String(appointment.patientCode))
              const resolvedName = appointment.patientName || appointment.fullName || matchedPatient?.fullName || `Bệnh nhân #${index + 1}`
              const initial = resolvedName.trim().split(/\s+/).slice(-1)[0]?.[0]?.toUpperCase() || 'B'
              const doctorName = appointment.doctorName || matchedPatient?.doctorName || 'Phòng khám tổng quát'

              return (
                <button type="button" className="compact-appointment-row" key={appointment.id || index} onClick={() => navigate('/appointments')}>
                  <time>{appointmentTime}</time>
                  <span className={'mini-avatar avatar-' + (index % 4)}>{initial}</span>
                  <span className="appointment-copy">
                    <strong>{resolvedName}</strong>
                    <small>{doctorName}</small>
                  </span>
                  <span className={'dashboard-status status-' + status.tone}>{status.label}</span>
                </button>
              )
            })}
            {!(Array.isArray(appointments) && appointments.length) && <div className="dashboard-empty">Chưa có lịch hẹn hôm nay</div>}
          </div>
        </article>

        <article className="compact-panel patients-panel">
          <div className="compact-panel-header">
            <h2>Bệnh nhân mới</h2>
            <button type="button" className="compact-link" onClick={() => navigate('/patients')}>Xem tất cả</button>
          </div>
          <div className="dashboard-table-wrap">
            <table className="dashboard-patient-table">
              <thead>
                <tr><th>Mã BN</th><th>Họ và tên</th><th>SĐT</th><th>Ngày sinh</th><th>Giới tính</th><th>Đăng ký lúc</th></tr>
              </thead>
              <tbody>
                {(Array.isArray(patients) ? patients : []).slice(0, 5).map((patient) => (
                  <tr key={patient.id} onClick={() => navigate(`/patients/${patient.id}`)}>
                    <td>{patient.patientCode}</td>
                    <td><strong>{patient.fullName}</strong></td>
                    <td>{patient.phone || patient.phoneNumber || '—'}</td>
                    <td>{patient.dateOfBirth ? dayjs(patient.dateOfBirth).format('DD/MM/YYYY') : '—'}</td>
                    <td>{patient.gender === 'FEMALE' ? 'Nữ' : patient.gender === 'MALE' ? 'Nam' : 'Khác'}</td>
                    <td>{patient.createdAt ? dayjs(patient.createdAt).format('DD/MM/YYYY HH:mm') : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>

        <article className="compact-panel medicine-panel">
          <div className="compact-panel-header">
            <h2>Thuốc sắp hết</h2>
            <button type="button" className="compact-link" onClick={() => navigate('/pharmacy')}>Xem tất cả</button>
          </div>
          <div className="compact-medicine-list">
            {(Array.isArray(lowStockMedicines) ? lowStockMedicines : []).map((medicine, index) => {
              const isLow = Number(medicine.stock || 0) <= Number(medicine.minStock || 10)
              return (
                <button type="button" className="compact-medicine-row" key={medicine.id || index} onClick={() => navigate('/pharmacy')}>
                  <span className={'medicine-capsule capsule-' + (index % 4)}><MedicineBoxOutlined /></span>
                  <span><strong>{medicine.name}</strong><small>Số lượng: {medicine.stock}</small></span>
                  <em>{isLow ? 'Sắp hết' : 'Theo dõi'}</em>
                </button>
              )
            })}
            {!(Array.isArray(lowStockMedicines) && lowStockMedicines.length) && <div className="dashboard-empty">Kho thuốc ổn định (Không có thuốc tồn thấp)</div>}
          </div>
          <button type="button" className="medicine-more" onClick={() => navigate('/pharmacy')}>Quản lý kho thuốc <RightOutlined /></button>
        </article>
      </section>
    </div>
  )
}

export default Dashboard
