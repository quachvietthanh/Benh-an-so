import React, { useMemo } from 'react'
import { Card, Empty, Input, Progress, Select, Space, Table, Tabs, Tag, Typography } from 'antd'
import {
  CalendarOutlined,
  DollarCircleOutlined,
  FileTextOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { formatMoney } from '../../../utils/revenueHelpers'

const { Text } = Typography

export default function RevenueDetailTables({
  dailyTimeline = [],
  periodInvoices = [],
  invoiceTypeFilter = 'ALL',
  setInvoiceTypeFilter,
  searchKeyword = '',
  setSearchKeyword,
  detailTab = 'timeline',
  setDetailTab,
  loading = false,
}) {
  const maxDayRevenue = useMemo(() => {
    return Math.max(...dailyTimeline.map((d) => d.netRevenue || 0), 1)
  }, [dailyTimeline])

  const filteredInvoices = useMemo(() => {
    return periodInvoices.filter((inv) => {
      if (invoiceTypeFilter !== 'ALL') {
        const type = inv.invoiceType || inv.type || 'ORIGINAL'
        if (type !== invoiceTypeFilter) return false
      }
      if (searchKeyword.trim()) {
        const kw = searchKeyword.toLowerCase()
        const codeMatch = (inv.invoiceCode || inv.code || inv.id || '').toLowerCase().includes(kw)
        const patientMatch = (inv.patientName || inv.patient?.fullName || '').toLowerCase().includes(kw)
        const noteMatch = (inv.note || inv.adjustmentReason || '').toLowerCase().includes(kw)
        if (!codeMatch && !patientMatch && !noteMatch) return false
      }
      return true
    })
  }, [periodInvoices, invoiceTypeFilter, searchKeyword])

  const timelineColumns = [
    {
      title: 'Ngày báo cáo',
      dataIndex: 'reportDate',
      key: 'reportDate',
      width: 180,
      render: (val, record) => (
        <Space>
          <strong>{val}</strong>
          <Tag color="default" style={{ fontSize: 11 }}>
            {record.weekday}
          </Tag>
        </Space>
      ),
    },
    {
      title: 'Số hóa đơn',
      dataIndex: 'invoiceCount',
      key: 'invoiceCount',
      width: 130,
      align: 'center',
      render: (v) => <Tag color="blue">{v} HĐ</Tag>,
    },
    {
      title: 'Doanh thu gốc',
      dataIndex: 'grossRevenue',
      key: 'grossRevenue',
      width: 160,
      align: 'right',
      render: (v) => <span style={{ color: '#2563eb' }}>{formatMoney(v)}</span>,
    },
    {
      title: 'Khoản điều chỉnh',
      dataIndex: 'adjustmentTotal',
      key: 'adjustmentTotal',
      width: 160,
      align: 'right',
      render: (v) => (
        <span style={{ color: v !== 0 ? '#ea580c' : '#94a3b8', fontWeight: v !== 0 ? 600 : 400 }}>
          {v !== 0 ? formatMoney(v) : '0 đ'}
        </span>
      ),
    },
    {
      title: 'Doanh thu thuần',
      dataIndex: 'netRevenue',
      key: 'netRevenue',
      width: 200,
      align: 'right',
      render: (v) => (
        <div>
          <strong style={{ color: '#059669', fontSize: 14 }}>{formatMoney(v)}</strong>
          <Progress
            percent={Math.min(100, Math.max(0, (v / maxDayRevenue) * 100))}
            showInfo={false}
            size="small"
            strokeColor="#10b981"
          />
        </div>
      ),
    },
  ]

  const invoiceColumns = [
    {
      title: 'Mã HĐ',
      key: 'code',
      width: 150,
      render: (_, r) => <strong>{r.invoiceCode || r.code || r.id?.substring(0, 8) || 'HĐ-N/A'}</strong>,
    },
    {
      title: 'Loại',
      key: 'type',
      width: 140,
      render: (_, r) => {
        const type = r.invoiceType || r.type || 'ORIGINAL'
        return type === 'ADJUSTMENT' ? (
          <Tag color="warning">ĐIỀU CHỈNH</Tag>
        ) : (
          <Tag color="success">HÓA ĐƠN GỐC</Tag>
        )
      },
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      render: (_, r) => r.patientName || r.patient?.fullName || 'Bệnh nhân',
    },
    {
      title: 'PTTT',
      dataIndex: 'paymentMethod',
      key: 'paymentMethod',
      width: 120,
      render: (v) => <Tag color="cyan">{v || 'Tiền mặt'}</Tag>,
    },
    {
      title: 'Số tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 160,
      align: 'right',
      render: (v, r) => {
        const isAdj = (r.invoiceType || r.type) === 'ADJUSTMENT' || Number(v) < 0
        return (
          <strong style={{ color: isAdj ? '#ea580c' : '#059669', fontSize: 14 }}>
            {formatMoney(v)}
          </strong>
        )
      },
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (v) => (v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '—'),
    },
  ]

  return (
    <Card style={{ borderRadius: 14, border: '1px solid #f1f5f9' }}>
      <Tabs
        activeKey={detailTab}
        onChange={setDetailTab}
        tabBarExtraContent={
          detailTab === 'invoices' ? (
            <Space wrap size="small">
              <Select
                value={invoiceTypeFilter}
                onChange={setInvoiceTypeFilter}
                style={{ width: 160 }}
                options={[
                  { value: 'ALL', label: 'Tất cả loại HĐ' },
                  { value: 'ORIGINAL', label: 'Hóa đơn gốc' },
                  { value: 'ADJUSTMENT', label: 'Hóa đơn điều chỉnh' },
                ]}
              />
              <Input
                placeholder="Tìm mã HĐ, tên bệnh nhân..."
                prefix={<SearchOutlined />}
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                style={{ width: 220 }}
                allowClear
              />
            </Space>
          ) : null
        }
        items={[
          {
            key: 'timeline',
            label: (
              <span style={{ fontWeight: 600 }}>
                <CalendarOutlined /> Bảng doanh thu theo ngày ({dailyTimeline.length} ngày)
              </span>
            ),
            children: (
              <Table
                rowKey="date"
                columns={timelineColumns}
                dataSource={dailyTimeline}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: true }}
                locale={{ emptyText: <Empty description="Chưa có dữ liệu ngày" /> }}
              />
            ),
          },
          {
            key: 'invoices',
            label: (
              <span style={{ fontWeight: 600 }}>
                <FileTextOutlined /> Danh sách hóa đơn chi tiết ({filteredInvoices.length} HĐ)
              </span>
            ),
            children: (
              <Table
                rowKey="id"
                columns={invoiceColumns}
                dataSource={filteredInvoices}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: true }}
                locale={{ emptyText: <Empty description="Không tìm thấy hóa đơn phù hợp" /> }}
              />
            ),
          },
        ]}
      />
    </Card>
  )
}
