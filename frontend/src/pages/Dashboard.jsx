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
  getMedicines,
  getPatients,
} from '../services/mockDataService'

const appointmentStatus = {
  SCHEDULED: { label: 'Đã xác nhận', tone: 'blue' },
  CHECKED_IN: { label: 'Đã đến', tone: 'green' },
  CALLED: { label: 'Đang gọi', tone: 'orange' },
  COMPLETED: { label: 'Hoàn tất', tone: 'green' },
  CANCELLED: { label: 'Đã hủy', tone: 'gray' },
  NO_SHOW: { label: 'Không đến', tone: 'orange' },
}

const chartPoints = [
  { x: 70, y: 190, label: '30/05' },
  { x: 172, y: 160, label: '31/05' },
  { x: 274, y: 151, label: '01/06' },
  { x: 376, y: 93, label: '02/06' },
  { x: 478, y: 148, label: '03/06' },
  { x: 580, y: 154, label: '04/06' },
  { x: 682, y: 132, label: '05/06' },
]

const readSettledData = (result, fallback) => (
  result?.status === 'fulfilled' ? result.value.data : fallback
)

function Dashboard() {
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState(getDashboardStats())
  const [appointments, setAppointments] = useState(getAppointments())
  const [patients, setPatients] = useState(getPatients())
  const [medicines, setMedicines] = useState(getMedicines())

  useEffect(() => {
    let mounted = true

    const loadDashboard = async () => {
      setLoading(true)
      const roles = user?.roles || []
      const isAdmin = roles.includes('admin')
      const canReadAppointments = roles.some((role) => ['admin', 'doctor', 'receptionist'].includes(role))
      const canReadPatients = roles.some((role) => ['admin', 'doctor', 'receptionist'].includes(role))
      const canReadPharmacy = roles.some((role) => ['admin', 'manager', 'pharmacist'].includes(role))

      const fallbackStats = getDashboardStats()
      const fallbackAppointments = getAppointments()
      const fallbackPatients = getPatients()
      const fallbackMedicines = getMedicines()

      const results = await Promise.allSettled([
        isAdmin ? reportApi.dashboard() : Promise.resolve({ data: fallbackStats }),
        canReadAppointments ? appointmentApi.getAll() : Promise.resolve({ data: fallbackAppointments }),
        canReadPatients ? patientApi.getAll({ page: 0, size: 5 }) : Promise.resolve({ data: { content: fallbackPatients } }),
        canReadPharmacy ? pharmacyApi.medicines() : Promise.resolve({ data: fallbackMedicines }),
      ])

      if (!mounted) return

      const patientData = readSettledData(results[2], { content: fallbackPatients })
      setStats(readSettledData(results[0], fallbackStats))
      setAppointments(readSettledData(results[1], fallbackAppointments) || [])
      setPatients(patientData?.content || patientData || [])
      setMedicines(readSettledData(results[3], fallbackMedicines) || [])
      setLoading(false)
    }

    loadDashboard()
    return () => { mounted = false }
  }, [user])

  const lowStockMedicines = useMemo(
    () => [...medicines]
      .sort((first, second) => Number(first.stock || 0) - Number(second.stock || 0))
      .slice(0, 4),
    [medicines],
  )

  const statCards = [
    {
      key: 'patients',
      label: 'Tổng bệnh nhân',
      value: Number(stats.totalPatients ?? patients.length).toLocaleString('vi-VN'),
      note: '+18 so với hôm qua',
      icon: TeamOutlined,
      tone: 'blue',
      route: '/patients',
    },
    {
      key: 'appointments',
      label: 'Lịch hẹn hôm nay',
      value: Number(stats.appointmentsToday ?? appointments.length).toLocaleString('vi-VN'),
      note: '+5 so với hôm qua',
      icon: CalendarOutlined,
      tone: 'green',
      route: '/appointments',
    },
    {
      key: 'revenue',
      label: 'Doanh thu hôm nay',
      value: `${Number(stats.revenueToday || 0).toLocaleString('vi-VN')} đ`,
      note: '+8% so với hôm qua',
      icon: DollarCircleOutlined,
      tone: 'orange',
      route: '/billing',
    },
    {
      key: 'medicine',
      label: 'Thuốc sắp hết',
      value: medicines.filter((medicine) => Number(medicine.stock || 0) < Number(medicine.minStock || 0)).length,
      note: 'Xem chi tiết',
      icon: MedicineBoxOutlined,
      tone: 'purple',
      route: '/pharmacy',
    },
  ]

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
              {chartPoints.map((point) => (
                <line key={'vertical-' + point.x} x1={point.x} x2={point.x} y1="38" y2="238" className="compact-grid-line vertical" />
              ))}
              <path d="M70 190 L172 160 L274 151 L376 93 L478 148 L580 154 L682 132 L682 238 L70 238 Z" fill="url(#compactRevenueArea)" />
              <polyline points={chartPoints.map((point) => `${point.x},${point.y}`).join(' ')} className="compact-chart-line" />
              {chartPoints.map((point) => (
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
            {appointments.slice(0, 5).map((appointment, index) => {
              const status = appointmentStatus[appointment.status] || appointmentStatus.SCHEDULED
              const appointmentTime = appointment.appointmentAt ? dayjs(appointment.appointmentAt).format('HH:mm') : appointment.slot
              return (
                <button type="button" className="compact-appointment-row" key={appointment.id} onClick={() => navigate('/appointments')}>
                  <time>{appointmentTime || '—'}</time>
                  <span className={'mini-avatar avatar-' + (index % 4)}>{appointment.patientName?.split(' ').slice(-1)[0]?.[0] || 'B'}</span>
                  <span className="appointment-copy">
                    <strong>{appointment.patientName}</strong>
                    <small>{appointment.doctorName || appointment.department || 'Lịch khám đã đặt'}</small>
                  </span>
                  <span className={'dashboard-status status-' + status.tone}>{status.label}</span>
                </button>
              )
            })}
            {!appointments.length && <div className="dashboard-empty">Chưa có lịch hẹn hôm nay</div>}
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
                <tr><th>ID</th><th>Họ và tên</th><th>SĐT</th><th>Ngày sinh</th><th>Giới tính</th><th>Đăng ký lúc</th></tr>
              </thead>
              <tbody>
                {patients.slice(0, 5).map((patient) => (
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
          <div className="dashboard-pagination">
            <button type="button">‹</button><button type="button" className="active">1</button><button type="button">2</button><button type="button">3</button><button type="button">4</button><button type="button">5</button><button type="button">›</button>
          </div>
        </article>

        <article className="compact-panel medicine-panel">
          <div className="compact-panel-header">
            <h2>Thuốc sắp hết</h2>
            <button type="button" className="compact-link" onClick={() => navigate('/pharmacy')}>Xem tất cả</button>
          </div>
          <div className="compact-medicine-list">
            {lowStockMedicines.map((medicine, index) => (
              <button type="button" className="compact-medicine-row" key={medicine.id} onClick={() => navigate('/pharmacy')}>
                <span className={'medicine-capsule capsule-' + index}><MedicineBoxOutlined /></span>
                <span><strong>{medicine.name}</strong><small>Số lượng: {medicine.stock}</small></span>
                <em>{Number(medicine.stock || 0) < Number(medicine.minStock || 0) ? 'Sắp hết' : 'Theo dõi'}</em>
              </button>
            ))}
            {!lowStockMedicines.length && <div className="dashboard-empty">Chưa có dữ liệu kho thuốc</div>}
          </div>
          <button type="button" className="medicine-more" onClick={() => navigate('/pharmacy')}>Quản lý kho thuốc <RightOutlined /></button>
        </article>
      </section>
    </div>
  )
}

export default Dashboard
