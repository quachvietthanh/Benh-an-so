import React from 'react'
import {
  DollarCircleOutlined,
  LineChartOutlined,
  PercentageOutlined,
  SwapOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { formatMoney } from '../../../utils/revenueHelpers'

export default function RevenueKPICards({ metrics = {}, daysDiff = 1 }) {
  return (
    <div className="revenue-kpi-grid">
      {/* Card 1: Doanh thu thuần */}
      <div className="revenue-kpi-card net">
        <div>
          <div className="revenue-kpi-header">
            <span className="revenue-kpi-label">Doanh thu thuần (Net)</span>
            <div className="revenue-kpi-icon net"><DollarCircleOutlined /></div>
          </div>
          <div className="revenue-kpi-value net">{formatMoney(metrics.netRevenue)}</div>
        </div>
        <div className="revenue-kpi-sub">
          <span className="revenue-kpi-badge success">Thực thu</span>
          <span>= Doanh thu gốc - Điều chỉnh</span>
        </div>
      </div>

      {/* Card 2: Doanh thu gốc */}
      <div className="revenue-kpi-card gross">
        <div>
          <div className="revenue-kpi-header">
            <span className="revenue-kpi-label">Doanh thu gốc (Gross)</span>
            <div className="revenue-kpi-icon gross"><WalletOutlined /></div>
          </div>
          <div className="revenue-kpi-value gross">{formatMoney(metrics.grossRevenue)}</div>
        </div>
        <div className="revenue-kpi-sub">
          <span className="revenue-kpi-badge info">{metrics.originalCount || 0} hóa đơn</span>
          <span>Hóa đơn phát sinh gốc</span>
        </div>
      </div>

      {/* Card 3: Khoản điều chỉnh */}
      <div className="revenue-kpi-card adjustment">
        <div>
          <div className="revenue-kpi-header">
            <span className="revenue-kpi-label">Khoản điều chỉnh</span>
            <div className="revenue-kpi-icon adjustment"><SwapOutlined /></div>
          </div>
          <div className="revenue-kpi-value adjustment">
            {metrics.adjustmentTotal !== 0 ? formatMoney(metrics.adjustmentTotal) : '0 đ'}
          </div>
        </div>
        <div className="revenue-kpi-sub">
          <span className="revenue-kpi-badge warning">{metrics.adjustmentCount || 0} điều chỉnh</span>
          <span>Giảm trừ / hoàn tiền</span>
        </div>
      </div>

      {/* Card 4: Trung bình / ngày */}
      <div className="revenue-kpi-card average">
        <div>
          <div className="revenue-kpi-header">
            <span className="revenue-kpi-label">Trung bình / ngày</span>
            <div className="revenue-kpi-icon average"><LineChartOutlined /></div>
          </div>
          <div className="revenue-kpi-value average">{formatMoney(metrics.averageDaily)}</div>
        </div>
        <div className="revenue-kpi-sub">
          <span>Tính trên <strong>{daysDiff}</strong> ngày kỳ này</span>
        </div>
      </div>

      {/* Card 5: Tỷ lệ điều chỉnh */}
      <div className="revenue-kpi-card rate">
        <div>
          <div className="revenue-kpi-header">
            <span className="revenue-kpi-label">Tỷ lệ điều chỉnh</span>
            <div className="revenue-kpi-icon rate"><PercentageOutlined /></div>
          </div>
          <div className="revenue-kpi-value" style={{ color: metrics.adjustmentRate > 5 ? '#c2410c' : '#0891b2' }}>
            {Number(metrics.adjustmentRate || 0).toFixed(2)}%
          </div>
        </div>
        <div className="revenue-kpi-sub">
          <span className={`revenue-kpi-badge ${metrics.adjustmentRate > 5 ? 'warning' : 'success'}`}>
            {metrics.adjustmentRate > 5 ? 'Cần lưu ý' : 'An toàn (<5%)'}
          </span>
          <span>Tỷ lệ giảm trừ</span>
        </div>
      </div>
    </div>
  )
}
