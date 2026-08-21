import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Space,
  Tabs,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  DashboardOutlined,
  DollarCircleOutlined,
  DownloadOutlined,
  FileSearchOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../api/reportApi'
import billingApi from '../api/billingApi'
import { useAuthContext } from '../context/AuthContext'
import ReportStatCards from '../components/reporting/ReportStatCards'
import ManagerPermissionAlert from '../components/reporting/ManagerPermissionAlert'
import OverviewReportView from '../components/reporting/OverviewReportView'
import VisitReportView from '../components/reporting/VisitReportView'
import DoctorVisitsReportView from '../components/reporting/DoctorVisitsReportView'
import RevenueReportView from '../components/reporting/RevenueReportView'
import TopMedicinesReportView from '../components/reporting/TopMedicinesReportView'
import AuditLogsReportView from '../components/reporting/AuditLogsReportView'
import {
  getStoredAuditLogs,
  getStoredInvoices,
  getStoredMedicalRecords,
  getStoredMedicines,
  getStoredPrescriptions,
  mergeInvoices,
} from '../utils/storageHelpers'

const { RangePicker } = DatePicker
const { Title, Paragraph } = Typography

function ReportsPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuthContext()

  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const canViewReports = userPermissions.includes('REPORT_VIEW')
  const canExportReports = userPermissions.includes('REPORT_EXPORT')

  const [range, setRange] = useState([dayjs().subtract(29, 'day'), dayjs()])
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [activeTab, setActiveTab] = useState('visits')
  const [loadError, setLoadError] = useState('')

  const [summary, setSummary] = useState({
    visitCount: 0,
    revenue: 0,
    dispensedCount: 0,
    auditCount: 0,
    currency: 'VND',
  })
  const [timeline, setTimeline] = useState([])
  const [topMedicines, setTopMedicines] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [invoicesList, setInvoicesList] = useState([])

  const fromStr = useMemo(
    () => (range?.[0] ? range[0].format('YYYY-MM-DD') : dayjs().subtract(29, 'day').format('YYYY-MM-DD')),
    [range],
  )
  const toStr = useMemo(
    () => (range?.[1] ? range[1].format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')),
    [range],
  )

  const getParams = useCallback(() => ({
    from: fromStr,
    to: toStr,
  }), [fromStr, toStr])

  const loadData = useCallback(async () => {
    if (!fromStr || !toStr) return

    setLoading(true)
    setLoadError('')
    const params = getParams()

    try {
      const [summaryRes, timelineRes, medicinesRes, auditRes, invoicesRes] = await Promise.allSettled([
        reportApi.summary(params),
        reportApi.timeline(params),
        reportApi.topMedicines(params),
        reportApi.audit(params),
        billingApi.getAll(),
      ])

      const apiSummary = summaryRes.status === 'fulfilled' ? summaryRes.value.data : null
      const apiTimeline = timelineRes.status === 'fulfilled' ? timelineRes.value.data : null
      const apiTopMedicines = medicinesRes.status === 'fulfilled' ? medicinesRes.value.data : null
      const apiAudit = auditRes.status === 'fulfilled' ? auditRes.value.data : null
      const apiInvoicesRaw = invoicesRes.status === 'fulfilled' ? invoicesRes.value.data : null
      const apiInvoicesList = (apiInvoicesRaw && Array.isArray(apiInvoicesRaw.content))
        ? apiInvoicesRaw.content
        : (Array.isArray(apiInvoicesRaw) ? apiInvoicesRaw : [])

      const storedRecords = getStoredMedicalRecords()
      const storedInvoices = getStoredInvoices()
      const storedPrescriptions = getStoredPrescriptions()
      const storedMedicines = getStoredMedicines()
      const storedLogs = getStoredAuditLogs()

      const apiRecords = (apiSummary && Array.isArray(apiSummary.records)) ? apiSummary.records : []
      const apiInvs = (apiSummary && Array.isArray(apiSummary.invoices)) ? apiSummary.invoices : apiInvoicesList
      const apiPrescs = (apiSummary && Array.isArray(apiSummary.prescriptions)) ? apiSummary.prescriptions : []

      const realRecords = [...storedRecords, ...apiRecords]
      const realInvoices = mergeInvoices([...storedInvoices, ...apiInvs])
      const realPrescriptions = [...storedPrescriptions, ...apiPrescs]
      const realLogs = (apiAudit && Array.isArray(apiAudit)) ? [...storedLogs, ...apiAudit] : storedLogs

      setInvoicesList(realInvoices)

      const totalVisits = apiSummary?.visitCount != null ? Number(apiSummary.visitCount) : realRecords.length
      const totalRevenue = apiSummary?.revenue != null ? Number(apiSummary.revenue) : realInvoices.reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)
      const dispensedCount = realPrescriptions.filter((p) => p.status === 'DISPENSED' || p.status === 'COMPLETED').length
      const auditCount = realLogs.length

      let timelineList = []
      if (apiTimeline && Array.isArray(apiTimeline.items) && apiTimeline.items.length > 0) {
        timelineList = apiTimeline.items.map((item) => ({
          date: item.date,
          reportDate: dayjs(item.date).format('DD/MM/YYYY'),
          visitCount: Number(item.visitCount || 0),
          revenue: Number(item.revenue || 0),
        }))
      } else {
        const startDate = range[0] || dayjs().subtract(29, 'day')
        const endDate = range[1] || dayjs()
        const diffDays = Math.max(0, endDate.diff(startDate, 'day'))
        const datesCount = Math.min(diffDays + 1, 366)

        for (let i = 0; i < datesCount; i++) {
          const currentDate = startDate.add(i, 'day')
          const formattedDate = currentDate.format('DD/MM/YYYY')
          const dateISO = currentDate.format('YYYY-MM-DD')

          const dayVisits = realRecords.filter((r) => (
            r.createdAt && dayjs(r.createdAt).format('YYYY-MM-DD') === dateISO
          )).length

          const dayRevenue = realInvoices
            .filter((inv) => inv.createdAt && dayjs(inv.createdAt).format('YYYY-MM-DD') === dateISO)
            .reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0)

          timelineList.push({
            date: dateISO,
            reportDate: formattedDate,
            visitCount: dayVisits,
            revenue: dayRevenue,
          })
        }
      }

      let rankedMeds = []
      if (apiTopMedicines && Array.isArray(apiTopMedicines.items)) {
        rankedMeds = apiTopMedicines.items.map((m) => ({
          name: m.medicineName || m.name,
          category: 'Dược phẩm',
          dispensedQuantity: m.totalDispensedQuantity || m.dispensedQuantity || 0,
        }))
      } else {
        const medMap = new Map()
        realPrescriptions.forEach((p) => {
          let items = []
          try {
            items = typeof p.items === 'string' ? JSON.parse(p.items) : (p.items || [])
          } catch {
            items = []
          }
          items.forEach((item) => {
            const medName = storedMedicines.find((m) => m.id === item.medicineId)?.name || item.medicineId || 'Thuốc'
            const currentQty = medMap.get(medName) || 0
            medMap.set(medName, currentQty + Number(item.quantity || 1))
          })
        })

        rankedMeds = Array.from(medMap.entries())
          .map(([name, dispensedQuantity]) => ({ name, category: 'Dược phẩm', dispensedQuantity }))
          .sort((a, b) => b.dispensedQuantity - a.dispensedQuantity)
      }

      setSummary({
        visitCount: totalVisits,
        revenue: totalRevenue,
        dispensedCount,
        auditCount,
        currency: apiSummary?.currency || 'VND',
      })

      setTimeline(timelineList)
      setTopMedicines(rankedMeds)
      setAuditLogs(realLogs)
    } catch (err) {
      console.error('Lỗi tải báo cáo vận hành:', err)
      const status = err?.response?.status
      if (status === 403) {
        setLoadError('PERMISSION_DENIED_NOT_MANAGER')
      } else {
        setLoadError(err?.response?.data?.message || err?.message || 'Không thể tải báo cáo.')
      }
    } finally {
      setLoading(false)
    }
  }, [fromStr, toStr, getParams, range])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleExportCSV = async () => {
    if (!canExportReports) {
      message.error('Bạn không có quyền xuất báo cáo (Yêu cầu quyền REPORT_EXPORT).')
      return
    }
    setExporting(true)
    try {
      const response = await reportApi.export({ from: fromStr, to: toStr })
      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `Bao-cao-van-hanh-${fromStr}-${toStr}.csv`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      message.success('Đã xuất báo cáo CSV thành công!')
    } catch {
      message.error('Không thể xuất báo cáo')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div style={{ background: '#f8fafc', minHeight: '100vh', padding: '16px 20px', paddingBottom: 40 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 16,
        }}
      >
        <div>
          <Title level={2} style={{ margin: 0, color: '#0f172a' }}>
            <LineChartOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Báo cáo Vận hành & Doanh thu
          </Title>
          <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
            Theo dõi số liệu khám bệnh, doanh thu tài chính, thuốc và nhật ký truy cập y tế theo thời gian thực.
          </Paragraph>
        </div>

        <Space wrap size="middle">
          <RangePicker
            value={range}
            format="DD/MM/YYYY"
            onChange={(val) => val && setRange(val)}
            allowClear={false}
            presets={[
              { label: '7 ngày qua', value: [dayjs().subtract(6, 'day'), dayjs()] },
              { label: '30 ngày qua', value: [dayjs().subtract(29, 'day'), dayjs()] },
              { label: 'Tháng này', value: [dayjs().startOf('month'), dayjs().endOf('month')] },
              {
                label: 'Tháng trước',
                value: [
                  dayjs().subtract(1, 'month').startOf('month'),
                  dayjs().subtract(1, 'month').endOf('month'),
                ],
              },
              { label: 'Quý này', value: [dayjs().startOf('quarter'), dayjs().endOf('quarter')] },
            ]}
            style={{ borderRadius: 8, padding: '6px 12px' }}
          />

          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
            Làm mới
          </Button>

          <Button
            type="primary"
            icon={<DownloadOutlined />}
            loading={exporting}
            disabled={!canExportReports || exporting}
            onClick={handleExportCSV}
            style={{ borderRadius: 8, background: canExportReports ? '#1677ff' : undefined, fontWeight: 600 }}
          >
            Xuất báo cáo (CSV)
          </Button>
        </Space>
      </div>

      {!canViewReports && (
        <Alert
          type="error"
          showIcon
          message="Không có quyền xem báo cáo"
          description="Bạn không có quyền truy cập module Báo cáo vận hành (Yêu cầu quyền REPORT_VIEW)."
          style={{ marginBottom: 16 }}
        />
      )}

      {loadError && canViewReports ? (
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu báo cáo"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      ) : null}

      <ReportStatCards summary={summary} />

      <Card
        style={{
          borderRadius: 14,
          boxShadow: '0 2px 10px rgba(0,0,0,0.02)',
          border: '1px solid #f1f5f9',
          marginBottom: 20,
        }}
        bodyStyle={{ padding: 0 }}
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          style={{ paddingLeft: 16, paddingRight: 16 }}
          items={[
            {
              key: 'overview',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <DashboardOutlined /> Tổng quan vận hành
                </span>
              ),
            },
            {
              key: 'visits',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <LineChartOutlined /> Báo cáo lượt khám
                </span>
              ),
            },
            {
              key: 'doctor-visits',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <UserOutlined /> Lượt khám theo bác sĩ
                </span>
              ),
            },
            {
              key: 'revenue',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <DollarCircleOutlined /> Báo cáo doanh thu
                </span>
              ),
            },
            {
              key: 'medicines',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <MedicineBoxOutlined /> Báo cáo thuốc dùng nhiều
                </span>
              ),
            },
            {
              key: 'audit',
              label: (
                <span style={{ fontWeight: 600, fontSize: 14 }}>
                  <FileSearchOutlined /> Nhật ký truy cập bệnh án
                </span>
              ),
            },
          ]}
        />
      </Card>

      <div>
        {activeTab === 'overview' && (
          <OverviewReportView summary={summary} />
        )}

        {activeTab === 'visits' && (
          <VisitReportView
            timeline={timeline}
            summary={summary}
            loading={loading}
          />
        )}

        {activeTab === 'doctor-visits' && (
          <DoctorVisitsReportView initialRange={range} />
        )}

        {activeTab === 'revenue' && (
          <RevenueReportView
            range={range}
            onRangeChange={setRange}
            invoices={invoicesList}
            loading={loading}
            onRefresh={loadData}
          />
        )}

        {activeTab === 'medicines' && (
          <TopMedicinesReportView
            topMedicines={topMedicines}
            loading={loading}
          />
        )}

        {activeTab === 'audit' && (
          <AuditLogsReportView
            auditLogs={auditLogs}
            loading={loading}
          />
        )}
      </div>
    </div>
  )
}

export default ReportsPage
