import React, { useMemo, useState } from 'react'
import { Alert, Button, Space } from 'antd'
import { CalendarOutlined, InfoCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  formatMoney,
  calculateFinancialMetrics,
  aggregateDailyTimeline,
  aggregatePaymentMethods,
  getPresetDateRange,
} from '../../utils/revenueHelpers'
import RevenueKPICards from './revenue/RevenueKPICards'
import RevenueInteractiveChart from './revenue/RevenueInteractiveChart'
import RevenuePaymentBreakdown from './revenue/RevenuePaymentBreakdown'
import RevenueDetailTables from './revenue/RevenueDetailTables'
import '../../styles/reports.css'

export {
  formatMoney,
  calculateFinancialMetrics,
  aggregateDailyTimeline,
  aggregatePaymentMethods,
  getPresetDateRange,
}

export default function RevenueReportView({
  range = [dayjs().subtract(6, 'day'), dayjs()],
  onRangeChange,
  invoices = [],
  loading = false,
  onRefresh,
}) {
  const [activePreset, setActivePreset] = useState('7days')
  const [chartMode, setChartMode] = useState('trend')
  const [invoiceTypeFilter, setInvoiceTypeFilter] = useState('ALL')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [detailTab, setDetailTab] = useState('timeline')
  const [hoveredPoint, setHoveredPoint] = useState(null)

  const startDate = range[0] || dayjs().subtract(6, 'day')
  const endDate = range[1] || dayjs()
  const daysDiff = Math.max(1, endDate.diff(startDate, 'day') + 1)

  const periodInvoices = useMemo(() => {
    const startStr = startDate.format('YYYY-MM-DD')
    const endStr = endDate.format('YYYY-MM-DD')
    return (invoices || []).filter((inv) => {
      if (!inv.createdAt) return false
      const invDate = dayjs(inv.createdAt).format('YYYY-MM-DD')
      return invDate >= startStr && invDate <= endStr
    })
  }, [invoices, startDate, endDate])

  const metrics = useMemo(() => {
    return calculateFinancialMetrics(periodInvoices, [], daysDiff)
  }, [periodInvoices, daysDiff])

  const dailyTimeline = useMemo(() => {
    return aggregateDailyTimeline(periodInvoices, startDate, endDate)
  }, [periodInvoices, startDate, endDate])

  const paymentBreakdown = useMemo(() => {
    return aggregatePaymentMethods(periodInvoices)
  }, [periodInvoices])

  const handlePresetSelect = (presetKey) => {
    setActivePreset(presetKey)
    const newRange = getPresetDateRange(presetKey)
    if (onRangeChange) {
      onRangeChange(newRange)
    }
  }

  return (
    <div className="revenue-report-container">
      <div className="revenue-filter-bar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <Space wrap size="small">
            <span style={{ fontWeight: 600, color: '#475569', fontSize: 13 }}>
              <CalendarOutlined style={{ marginRight: 4 }} /> Chọn nhanh kỳ:
            </span>
            <Button
              size="small"
              type={activePreset === 'today' ? 'primary' : 'default'}
              onClick={() => handlePresetSelect('today')}
              style={{ borderRadius: 6 }}
            >
              Hôm nay
            </Button>
            <Button
              size="small"
              type={activePreset === '7days' ? 'primary' : 'default'}
              onClick={() => handlePresetSelect('7days')}
              style={{ borderRadius: 6 }}
            >
              7 ngày gần nhất
            </Button>
            <Button
              size="small"
              type={activePreset === '30days' ? 'primary' : 'default'}
              onClick={() => handlePresetSelect('30days')}
              style={{ borderRadius: 6 }}
            >
              30 ngày qua
            </Button>
            <Button
              size="small"
              type={activePreset === 'thisMonth' ? 'primary' : 'default'}
              onClick={() => handlePresetSelect('thisMonth')}
              style={{ borderRadius: 6 }}
            >
              Tháng này
            </Button>
            <Button
              size="small"
              type={activePreset === 'lastMonth' ? 'primary' : 'default'}
              onClick={() => handlePresetSelect('lastMonth')}
              style={{ borderRadius: 6 }}
            >
              Tháng trước
            </Button>
            <Button
              size="small"
              type={activePreset === 'thisQuarter' ? 'primary' : 'default'}
              onClick={() => handlePresetSelect('thisQuarter')}
              style={{ borderRadius: 6 }}
            >
              Quý này
            </Button>
          </Space>
        </div>

        {onRefresh && (
          <Button icon={<ReloadOutlined />} onClick={onRefresh} loading={loading} size="small" style={{ borderRadius: 6 }}>
            Đồng bộ số liệu
          </Button>
        )}
      </div>

      <Alert
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        message="Chuẩn hạch toán tài chính Doanh thu phòng khám"
        description="Doanh thu thuần hiển thị đã được tự động loại trừ các khoản điều chỉnh đơn thuốc, giảm giá và hoàn tiền theo đúng quy tắc kế toán."
        style={{ borderRadius: 10, marginBottom: 20 }}
      />

      <RevenueKPICards metrics={metrics} daysDiff={daysDiff} />

      <RevenueInteractiveChart
        dailyTimeline={dailyTimeline}
        metrics={metrics}
        loading={loading}
        chartMode={chartMode}
        setChartMode={setChartMode}
        hoveredPoint={hoveredPoint}
        setHoveredPoint={setHoveredPoint}
      />

      <RevenuePaymentBreakdown paymentBreakdown={paymentBreakdown} netRevenue={metrics.netRevenue} />

      <RevenueDetailTables
        dailyTimeline={dailyTimeline}
        periodInvoices={periodInvoices}
        invoiceTypeFilter={invoiceTypeFilter}
        setInvoiceTypeFilter={setInvoiceTypeFilter}
        searchKeyword={searchKeyword}
        setSearchKeyword={setSearchKeyword}
        detailTab={detailTab}
        setDetailTab={setDetailTab}
        loading={loading}
      />
    </div>
  )
}
