import React from 'react'
import { Card, Table, Tag, Typography } from 'antd'

const { Text } = Typography

export default function TopMedicinesReportView({ topMedicines = [], loading = false }) {
  return (
    <Card style={{ borderRadius: 14, border: '1px solid #f1f5f9' }} title="Báo cáo số lượng thuốc đã cấp phát thực tế">
      <Table
        rowKey="name"
        dataSource={topMedicines}
        loading={loading}
        locale={{ emptyText: 'Chưa có dữ liệu cấp phát thuốc' }}
        columns={[
          {
            title: 'Thứ hạng',
            key: 'rank',
            width: 100,
            render: (_, __, idx) => (
              <Tag color={idx < 3 ? 'volcano' : 'blue'}>Top {idx + 1}</Tag>
            ),
          },
          {
            title: 'Tên thuốc',
            dataIndex: 'name',
            key: 'name',
            render: (v) => <strong>{v}</strong>,
          },
          {
            title: 'Nhóm thuốc',
            dataIndex: 'category',
            key: 'category',
            render: (v) => v || 'Dược phẩm',
          },
          {
            title: 'Số lượng đã cấp phát',
            dataIndex: 'dispensedQuantity',
            key: 'dispensedQuantity',
            align: 'right',
            render: (v) => <Text type="danger" strong>{v || 0} đơn vị</Text>,
          },
        ]}
      />
    </Card>
  )
}
