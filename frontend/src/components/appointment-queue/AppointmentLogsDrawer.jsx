import React from 'react'
import { Drawer, Table, Tabs, Tag, Typography } from 'antd'
import dayjs from 'dayjs'

const { Text } = Typography

export default function AppointmentLogsDrawer({
  open,
  onClose,
  appointmentLogs = [],
  notificationLogs = [],
}) {
  return (
    <Drawer
      title="Nhật ký lịch hẹn & thông báo"
      width={650}
      open={open}
      onClose={onClose}
    >
      <Tabs
        items={[
          {
            key: 'app_logs',
            label: 'Nhật ký Lịch hẹn',
            children: (
              <Table
                dataSource={appointmentLogs}
                rowKey="id"
                pagination={{ pageSize: 8 }}
                columns={[
                  { title: 'Thời điểm', dataIndex: 'timestamp', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                  { title: 'Thời điểm', dataIndex: 'timestamp', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                  { title: 'Mã LH', dataIndex: 'appointmentCode', render: (c) => <Text code>{c}</Text> },
                  { title: 'Người thao tác', dataIndex: 'operatorName' },
                  { title: 'Chi tiết thao tác', dataIndex: 'details' },
                ]}
              />
            ),
          },
          {
            key: 'notif_logs',
            label: 'Nhật ký Thông báo / Nhắc lịch',
            children: (
              <Table
                dataSource={notificationLogs}
                rowKey="id"
                pagination={{ pageSize: 8 }}
                columns={[
                  { title: 'Thời điểm', dataIndex: 'sentAt', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                  { title: 'Thời điểm', dataIndex: 'sentAt', render: (t) => dayjs(t).format('HH:mm - DD/MM/YYYY') },
                  { title: 'Bệnh nhân', dataIndex: 'patientName' },
                  {
                    title: 'Kênh',
                    dataIndex: 'channel',
                    render: (channel) => (
                      <Tag color="blue">
                        {String(channel || 'Hệ thống').replace(/System/gi, 'Hệ thống')}
                      </Tag>
                    ),
                  },
                  { title: 'Nội dung nhắc', dataIndex: 'message' },
                ]}
              />
            ),
          },
        ]}
      />
    </Drawer>
  )
}
