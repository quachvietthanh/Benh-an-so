import React from 'react'
import {
  Alert,
  Drawer,
  Empty,
  Tag,
  Typography,
} from 'antd'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  HistoryOutlined,
  ReloadOutlined,
} from '@ant-design/icons'

const { Title, Text } = Typography

function ServicePriceHistoryDrawer({
  open,
  onClose,
  service,
  priceHistory,
  loading,
}) {
  return (
    <Drawer
      className="price-history-drawer"
      title={
        <div>
          <Title level={5} style={{ margin: 0, color: '#0f172a' }}>
            <HistoryOutlined style={{ marginRight: 8, color: '#2563eb' }} />
            Lịch sử các phiên bản giá
          </Title>
          {service && (
            <div className="price-history-header-meta">
              <div>
                <span className="service-code-badge" style={{ marginRight: 8 }}>
                  {service.serviceCode}
                </span>
                <Text strong>{service.name}</Text>
              </div>
              <Tag color={service.active ? 'success' : 'default'}>
                {service.active ? 'Đang hiệu lực' : 'Ngừng áp dụng'}
              </Tag>
            </div>
          )}
        </div>
      }
      placement="right"
      width={480}
      open={open}
      onClose={onClose}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <ReloadOutlined spin style={{ fontSize: 24, color: '#2563eb' }} />
          <div style={{ marginTop: 12, color: '#64748b' }}>Đang tải lịch sử giá...</div>
        </div>
      ) : priceHistory.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Chưa có dữ liệu lịch sử giá cho dịch vụ này"
        />
      ) : (
        <div>
          <Alert
            className="service-notice-banner"
            type="info"
            showIcon
            message="Nguyên tắc áp dụng giá"
            description="Hóa đơn thu viện phí sẽ tự động tra cứu và áp dụng mức giá có ngày hiệu lực gần nhất tính đến thời điểm lập chỉ định khám."
          />

          <div style={{ marginTop: 16 }}>
            {priceHistory.map((item, index) => {
              const isCurrent = item.priceStatus === 'CURRENT'
              const isUpcoming = item.priceStatus === 'UPCOMING'
              const cardClass = isCurrent
                ? 'price-history-card-item is-current'
                : isUpcoming
                  ? 'price-history-card-item is-upcoming'
                  : 'price-history-card-item'

              return (
                <div key={item.id || index} className={cardClass}>
                  <div className="price-history-card-top">
                    <span
                      className={`price-history-amount ${
                        isCurrent ? 'is-current' : isUpcoming ? 'is-upcoming' : ''
                      }`}
                    >
                      {item.formattedPrice}
                    </span>
                    <Tag color={item.statusColor}>{item.statusLabel}</Tag>
                  </div>
                  <div className="price-history-card-details">
                    <div className="price-history-detail-row">
                      <CalendarOutlined style={{ color: '#94a3b8' }} />
                      <span>
                        <strong>Ngày bắt đầu hiệu lực:</strong> {item.formattedEffectiveFrom}
                      </span>
                    </div>
                    {item.formattedCreatedAt && item.formattedCreatedAt !== '—' && (
                      <div className="price-history-detail-row">
                        <ClockCircleOutlined style={{ color: '#94a3b8' }} />
                        <span>
                          <strong>Thời điểm thiết lập:</strong> {item.formattedCreatedAt}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </Drawer>
  )
}

export default ServicePriceHistoryDrawer
