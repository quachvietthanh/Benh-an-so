import React from 'react'
import { Button, Descriptions, Modal, Space, Tag, Typography } from 'antd'
import { DownloadOutlined, EyeOutlined } from '@ant-design/icons'
import { getFileIcon, STATUS_MAP } from './attachmentConstants.jsx'

const { Paragraph, Text } = Typography

function AttachmentPreviewModal({ open, attachment, onClose, onDownload }) {
  if (!attachment) return null

  return (
    <Modal
      title={(
        <Space>
          <EyeOutlined style={{ color: '#2563eb' }} />
          <span>Thông tin tệp kết quả</span>
          {attachment.attachmentCode && <Tag color="blue">{attachment.attachmentCode}</Tag>}
        </Space>
      )}
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="download" icon={<DownloadOutlined />} onClick={() => onDownload(attachment)}>
          Tạo đường dẫn tải tệp
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>Đóng</Button>,
      ]}
      width={720}
    >
      <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="Bệnh nhân">
          <strong>{attachment.patientName}</strong> ({attachment.patientCode || '—'})
        </Descriptions.Item>
        <Descriptions.Item label="Lượt khám">{attachment.visitCode || attachment.visitId || '—'}</Descriptions.Item>
        <Descriptions.Item label="Loại tệp">
          <Tag color="purple">{attachment.attachmentType || attachment.category}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Người phụ trách">{attachment.doctorName || '—'}</Descriptions.Item>
        <Descriptions.Item label="Đánh giá kết quả">
          <Tag color={STATUS_MAP[attachment.status]?.color || 'default'}>
            {STATUS_MAP[attachment.status]?.label || attachment.status || '—'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Trạng thái kết quả">{attachment.resultStatus || '—'}</Descriptions.Item>
        <Descriptions.Item label="Kết luận" span={2}>
          <Text style={{ whiteSpace: 'pre-line' }}>{attachment.resultSummary || 'Chưa có kết luận'}</Text>
        </Descriptions.Item>
        {attachment.note && (
          <Descriptions.Item label="Thông tin bổ sung" span={2}>{attachment.note}</Descriptions.Item>
        )}
      </Descriptions>

      <div style={{ background: '#f8fafc', padding: 20, borderRadius: 8, textAlign: 'center' }}>
        <div style={{ marginBottom: 12 }}>{getFileIcon(attachment.fileType, attachment.fileName)}</div>
        <Paragraph style={{ marginBottom: 4 }}><Text strong>{attachment.fileName}</Text></Paragraph>
        <Text type="secondary">{attachment.fileType || 'Không rõ định dạng'} · {attachment.fileSize || 'Không rõ dung lượng'}</Text>
        <Paragraph type="secondary" style={{ margin: '12px 0 0' }}>
          Nội dung tệp không có URL công khai. Nhấn “Tạo đường dẫn tải tệp” để lấy signed URL tạm thời từ máy chủ.
        </Paragraph>
      </div>
    </Modal>
  )
}

export default AttachmentPreviewModal
