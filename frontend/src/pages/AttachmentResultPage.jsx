import React from 'react'
import { Card, Typography } from 'antd'
import AttachmentResultManager from '../components/attachments/AttachmentResultManager'

const { Title, Text } = Typography

function AttachmentResultPage() {
  return (
    <div>
      <div className="page-header" style={{ marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            Quản lý Kết quả cận lâm sàng & Tệp đính kèm
          </Title>
          <Text type="secondary">
            Tải lên, xem danh sách kết quả xét nghiệm, X-quang, siêu âm và đính kèm hồ sơ bệnh án
          </Text>
        </div>
      </div>

      <AttachmentResultManager />
    </div>
  )
}

export default AttachmentResultPage
