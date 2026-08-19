import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import dayjs from 'dayjs'
import {
  AlertOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DollarCircleOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  RightOutlined,
  SyncOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import dashboardApi from '../api/dashboardApi'
import reportApi from '../api/reportApi'
import {
  buildOperationalSnapshotFromReports,
  getActiveQueueCount,
  getVisitPercentage,
  normalizeOperationalDashboard,
} from '../utils/operationalDashboard'
import {
  getStoredBatches,
  getStoredInvoices,
  getStoredMedicalRecords,
  getStoredMedicines,
} from '../utils/storageHelpers'

const AUTO_REFRESH_MS = 60_000

const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const numberFormatter = new Intl.NumberFormat('vi-VN')

const formatToday = () => {
  const label = new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(new Date())

  return label.charAt(0).toUpperCase() + label.slice(1)
}

const formatUpdatedAt = (value) => {
  if (!value || !dayjs(value).isValid()) return 'Chưa xác định'
  const updatedAt = dayjs(value)
  return updatedAt.isSame(dayjs(), 'day')
    ? updatedAt.format('HH:mm:ss')
    : updatedAt.format('HH:mm:ss · DD/MM/YYYY')
}

const getDashboardError = (error) => {
  const status = error?.response?.status

  if (status === 403) {
    return {
      title: 'Tài khoản chưa có quyền xem tổng quan vận hành',
      description: 'Dashboard này dành cho quản trị viên và quản lý phòng khám. Vui lòng kiểm tra lại vai trò được cấp.',
    }
  }

  if (status === 404) {
    return {
      title: 'Chưa tìm thấy API tổng quan vận hành',
      description: 'Không thể kết nối tới nguồn dữ liệu dashboard. Vui lòng kiểm tra cấu hình phiên bản backend.',
    }
  }

  return {
    title: 'Chưa thể cập nhật dữ liệu vận hành',
    description: 'Dữ liệu gần nhất chưa tải được. Hãy kiểm tra kết nối và thử làm mới lại.',
  }
}

function DashboardSkeleton() {
  return (
    <div className="ops-dashboard ops-dashboard-skeleton" aria-busy="true" aria-label="Đang tải tổng quan vận hành">
      <div className="ops-skeleton-heading">
        <span className="ops-skeleton-block skeleton-title" />
        <span className="ops-skeleton-block skeleton-action" />
      </div>
      <div className="ops-kpi-grid">
        {[0, 1, 2, 3].map((item) => <span className="ops-skeleton-block skeleton-kpi" key={item} />)}
      </div>
      <div className="ops-main-grid">
        <span className="ops-skeleton-block skeleton-panel skeleton-panel-wide" />
        <span className="ops-skeleton-block skeleton-panel" />
      </div>
    </div>
  )
}

function Dashboard() {
  const navigate = useNavigate()
  const mountedRef = useRef(true)
  const requestInFlightRef = useRef(false)
  const snapshotRef = useRef(null)
  const [snapshot, setSnapshot] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState(null)

  const loadDashboard = useCallback(async ({ silent = false } = {}) => {
    if (requestInFlightRef.current) return

    requestInFlightRef.current = true
    if (silent && snapshotRef.current) setRefreshing(true)
    if (!snapshotRef.current) setLoading(true)

    try {
      let nextSnapshot = null
      const todayStr = dayjs().format('YYYY-MM-DD')
      const storedRecords = getStoredMedicalRecords()
      const storedInvoices = getStoredInvoices()
      const storedMedicines = getStoredMedicines()
      const storedBatches = getStoredBatches()

      try {
        const response = await dashboardApi.getOperational()
        nextSnapshot = normalizeOperationalDashboard(response.data)
      } catch (apiError) {
        const status = apiError?.response?.status

        const [summaryRes, timelineRes] = await Promise.allSettled([
          reportApi.summary({ from: todayStr, to: todayStr }),
          reportApi.timeline({ from: todayStr, to: todayStr }),
        ])

        const summaryData = summaryRes.status === 'fulfilled' ? summaryRes.value?.data : null
        const timelineData = timelineRes.status === 'fulfilled' ? timelineRes.value?.data : null

        if (summaryData || storedRecords.length > 0 || storedInvoices.length > 0 || storedMedicines.length > 0) {
          nextSnapshot = buildOperationalSnapshotFromReports({
            summary: summaryData,
            timeline: timelineData,
            records: storedRecords,
            invoices: storedInvoices,
            medicines: storedMedicines,
            batches: storedBatches,
            todayStr,
          })
        } else if (status === 403) {
          throw apiError
        } else {
          throw apiError
        }
      }

      if (!mountedRef.current) return
      snapshotRef.current = nextSnapshot
      setSnapshot(nextSnapshot)
      setError(null)
    } catch (requestError) {
      if (mountedRef.current) setError(getDashboardError(requestError))
    } finally {
      requestInFlightRef.current = false
      if (mountedRef.current) {
        setLoading(false)
        setRefreshing(false)
      }
    }
  }, [])

  useEffect(() => {
    mountedRef.current = true
    loadDashboard()

    const refreshInterval = setInterval(() => {
      loadDashboard({ silent: true })
    }, AUTO_REFRESH_MS)

    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') loadDashboard({ silent: true })
    }
    document.addEventListener('visibilitychange', refreshWhenVisible)

    return () => {
      mountedRef.current = false
      clearInterval(refreshInterval)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
    }
  }, [loadDashboard])

  const visitSummary = snapshot?.visitSummary
  const revenueSummary = snapshot?.revenueSummary
  const inventorySummary = snapshot?.inventoryAlertSummary

  const completionRate = getVisitPercentage(visitSummary?.completed, visitSummary?.total)
  const activeQueueCount = getActiveQueueCount(visitSummary)

  const statusItems = useMemo(() => [
    {
      key: 'waiting',
      label: 'Đang chờ',
      description: 'Chờ được tiếp nhận khám',
      value: visitSummary?.waiting || 0,
      tone: 'amber',
      icon: ClockCircleOutlined,
    },
    {
      key: 'inProgress',
      label: 'Đang khám',
      description: 'Gồm cả lượt chờ kết quả',
      value: visitSummary?.inProgress || 0,
      tone: 'blue',
      icon: SyncOutlined,
    },
    {
      key: 'completed',
      label: 'Hoàn tất',
      description: 'Đã kết thúc quy trình khám',
      value: visitSummary?.completed || 0,
      tone: 'green',
      icon: CheckCircleOutlined,
    },
    {
      key: 'cancelled',
      label: 'Đã hủy',
      description: 'Không tiếp tục lượt khám',
      value: visitSummary?.cancelled || 0,
      tone: 'slate',
      icon: CloseCircleOutlined,
    },
  ], [visitSummary])

  const kpis = useMemo(() => [
    {
      key: 'visits',
      label: 'Lượt khám hôm nay',
      value: numberFormatter.format(visitSummary?.total || 0),
      note: `${numberFormatter.format(visitSummary?.completed || 0)} lượt đã hoàn tất`,
      tone: 'blue',
      icon: TeamOutlined,
    },
    {
      key: 'waiting',
      label: 'Đang chờ',
      value: numberFormatter.format(visitSummary?.waiting || 0),
      note: activeQueueCount
        ? `${numberFormatter.format(activeQueueCount)} lượt đang trong quy trình`
        : 'Hàng đợi hiện đang trống',
      tone: 'amber',
      icon: ClockCircleOutlined,
    },
    {
      key: 'progress',
      label: 'Đang khám',
      value: numberFormatter.format(visitSummary?.inProgress || 0),
      note: 'Bao gồm lượt đang chờ kết quả',
      tone: 'teal',
      icon: SyncOutlined,
    },
    {
      key: 'revenue',
      label: 'Doanh thu hôm nay',
      value: currencyFormatter.format(revenueSummary?.totalRevenueToday || 0),
      note: 'Khoản thu đã được ghi nhận',
      tone: 'violet',
      icon: DollarCircleOutlined,
    },
  ], [activeQueueCount, revenueSummary, visitSummary])

  if (loading && !snapshot) return <DashboardSkeleton />

  if (!snapshot) {
    return (
      <div className="ops-dashboard">
        <header className="ops-dashboard-heading">
          <div>
            <span className="ops-live-pill"><i /> Dữ liệu trong ngày</span>
            <h1>Tổng quan vận hành</h1>
            <p>{formatToday()}</p>
          </div>
        </header>
        <section className="ops-full-error" role="alert">
          <span className="ops-error-icon"><ExclamationCircleOutlined /></span>
          <h2>{error?.title || 'Chưa thể tải dashboard'}</h2>
          <p>{error?.description || 'Vui lòng thử lại sau ít phút.'}</p>
          <button type="button" className="ops-primary-button" onClick={() => loadDashboard()}>
            <ReloadOutlined /> Thử lại
          </button>
        </section>
      </div>
    )
  }

  return (
    <div className="ops-dashboard">
      <header className="ops-dashboard-heading">
        <div className="ops-heading-copy">
          <span className="ops-live-pill"><i /> Dữ liệu trong ngày</span>
          <h1>Tổng quan vận hành</h1>
          <p>{formatToday()} · Theo dõi lượt khám, hàng đợi và doanh thu</p>
        </div>

        <div className="ops-sync-actions">
          <div className="ops-sync-meta" aria-live="polite">
            <SyncOutlined spin={refreshing} />
            <span>
              <small>Cập nhật gần nhất</small>
              <strong>{formatUpdatedAt(snapshot.asOf)}</strong>
            </span>
          </div>
          <button
            type="button"
            className="ops-refresh-button"
            onClick={() => loadDashboard({ silent: true })}
            disabled={refreshing}
          >
            <ReloadOutlined spin={refreshing} />
            <span>{refreshing ? 'Đang cập nhật' : 'Làm mới'}</span>
          </button>
        </div>
      </header>

      {error && (
        <div className="ops-sync-warning" role="alert">
          <ExclamationCircleOutlined />
          <span><strong>{error.title}.</strong> Đang hiển thị dữ liệu cập nhật lúc {formatUpdatedAt(snapshot.asOf)}.</span>
          <button type="button" onClick={() => loadDashboard({ silent: true })}>Thử lại</button>
        </div>
      )}

      <section className="ops-kpi-grid" aria-label="Các chỉ số vận hành chính">
        {kpis.map((kpi) => {
          const Icon = kpi.icon
          return (
            <article className={`ops-kpi-card ops-tone-${kpi.tone}`} key={kpi.key}>
              <div className="ops-kpi-topline">
                <span className="ops-kpi-icon"><Icon /></span>
                <span className="ops-kpi-label">{kpi.label}</span>
              </div>
              <strong className="ops-kpi-value">{kpi.value}</strong>
              <span className="ops-kpi-note">{kpi.note}</span>
            </article>
          )
        })}
      </section>

      <section className="ops-main-grid">
        <article className="ops-card ops-flow-card">
          <header className="ops-card-header">
            <div>
              <span className="ops-section-icon blue"><BarChartOutlined /></span>
              <div>
                <h2>Tiến độ lượt khám</h2>
                <p>Phân bổ trạng thái tính đến thời điểm hiện tại</p>
              </div>
            </div>
            <span className="ops-rate-badge">{completionRate}% hoàn tất</span>
          </header>

          <div className="ops-flow-summary">
            <span>
              <small>Tổng lượt trong ngày</small>
              <strong>{numberFormatter.format(visitSummary.total)}</strong>
            </span>
            <span>
              <small>Còn trong quy trình</small>
              <strong>{numberFormatter.format(activeQueueCount)}</strong>
            </span>
          </div>

          {visitSummary.total > 0 ? (
            <div
              className="ops-status-track"
              role="img"
              aria-label={`Trong ${visitSummary.total} lượt khám: ${visitSummary.waiting} đang chờ, ${visitSummary.inProgress} đang khám, ${visitSummary.completed} hoàn tất và ${visitSummary.cancelled} đã hủy`}
            >
              {statusItems.filter((item) => item.value > 0).map((item) => (
                <span
                  className={`ops-track-segment segment-${item.tone}`}
                  style={{ width: `${getVisitPercentage(item.value, visitSummary.total)}%` }}
                  key={item.key}
                />
              ))}
            </div>
          ) : (
            <div className="ops-empty-track">Chưa có lượt khám nào được ghi nhận hôm nay</div>
          )}

          <div className="ops-status-list">
            {statusItems.map((item) => {
              const Icon = item.icon
              return (
                <div className="ops-status-item" key={item.key}>
                  <span className={`ops-status-icon status-${item.tone}`}><Icon /></span>
                  <span className="ops-status-copy">
                    <strong>{item.label}</strong>
                    <small>{item.description}</small>
                  </span>
                  <span className="ops-status-number">
                    <strong>{numberFormatter.format(item.value)}</strong>
                    <small>{getVisitPercentage(item.value, visitSummary.total)}%</small>
                  </span>
                </div>
              )
            })}
          </div>
        </article>

        <aside className="ops-card ops-queue-card">
          <header className="ops-card-header compact">
            <div>
              <span className="ops-section-icon amber"><ClockCircleOutlined /></span>
              <div>
                <h2>Hàng đợi hiện tại</h2>
                <p>Khối lượng cần điều phối</p>
              </div>
            </div>
          </header>

          <div className={`ops-queue-focus ${visitSummary.waiting === 0 ? 'is-clear' : ''}`}>
            <span className="ops-queue-pulse"><i /></span>
            <strong>{numberFormatter.format(visitSummary.waiting)}</strong>
            <span>{visitSummary.waiting === 0 ? 'Không có lượt đang chờ' : 'Lượt đang chờ khám'}</span>
          </div>

          <div className="ops-queue-details">
            <div>
              <span><SyncOutlined /> Đang khám</span>
              <strong>{numberFormatter.format(visitSummary.inProgress)}</strong>
            </div>
            <div>
              <span><TeamOutlined /> Đang trong quy trình</span>
              <strong>{numberFormatter.format(activeQueueCount)}</strong>
            </div>
          </div>

          <div className="ops-queue-note">
            <CalendarOutlined />
            <span>Số liệu được làm mới tự động mỗi 60 giây.</span>
          </div>
        </aside>
      </section>

      <section className="ops-secondary-grid">
        <article className="ops-card ops-alerts-card">
          <header className="ops-card-header compact">
            <div>
              <span className="ops-section-icon rose"><AlertOutlined /></span>
              <div>
                <h2>Cảnh báo cần chú ý</h2>
                <p>Tồn kho thuốc trong ngày</p>
              </div>
            </div>
          </header>

          <div className="ops-alert-list">
            <div className={`ops-alert-item ${inventorySummary.lowStockCount > 0 ? 'has-alert' : 'is-safe'}`}>
              <span className="ops-alert-item-icon"><MedicineBoxOutlined /></span>
              <span>
                <small>Thuốc tồn kho thấp</small>
                <strong>{numberFormatter.format(inventorySummary.lowStockCount)}</strong>
              </span>
            </div>
            <div className={`ops-alert-item ${inventorySummary.expiryAlertCount > 0 ? 'has-alert' : 'is-safe'}`}>
              <span className="ops-alert-item-icon"><ExclamationCircleOutlined /></span>
              <span>
                <small>Lô sắp hoặc đã hết hạn</small>
                <strong>{numberFormatter.format(inventorySummary.expiryAlertCount)}</strong>
              </span>
            </div>
          </div>
        </article>

        <article className="ops-card ops-outcome-card">
          <header className="ops-card-header compact">
            <div>
              <span className="ops-section-icon green"><CheckCircleOutlined /></span>
              <div>
                <h2>Kết quả trong ngày</h2>
                <p>Theo dõi tiến độ xử lý lượt khám</p>
              </div>
            </div>
          </header>

          <div className="ops-outcome-list">
            <div>
              <span className="outcome-dot green" />
              <span><small>Đã hoàn tất</small><strong>{numberFormatter.format(visitSummary.completed)} lượt</strong></span>
            </div>
            <div>
              <span className="outcome-dot slate" />
              <span><small>Đã hủy</small><strong>{numberFormatter.format(visitSummary.cancelled)} lượt</strong></span>
            </div>
          </div>

          <div className="ops-card-actions">
            <button type="button" onClick={() => navigate('/reports')}>
              Xem báo cáo <RightOutlined />
            </button>
            <button type="button" onClick={() => navigate('/billing')}>
              Mở thu phí <RightOutlined />
            </button>
          </div>
        </article>
      </section>

      <footer className="ops-dashboard-footnote">
        <SyncOutlined /> Dữ liệu được tổng hợp từ hoạt động khám và các khoản thu đã ghi nhận đến {formatUpdatedAt(snapshot.asOf)}.
      </footer>
    </div>
  )
}

export default Dashboard
