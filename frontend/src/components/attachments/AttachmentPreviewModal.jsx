import React from 'react'
import { Button, Descriptions, Divider, Image, Modal, Space, Tag, Typography } from 'antd'
import { DownloadOutlined, EyeOutlined, FilePdfOutlined } from '@ant-design/icons'
import { getFileIcon, STATUS_MAP } from './attachmentConstants.jsx'

const { Text } = Typography

function AttachmentPreviewModal({ open, attachment, onClose, onDownload }) {
  if (!attachment) return null

  return (
    <Modal
      title={
        <Space>
          <EyeOutlined style={{ color: '#2563eb' }} />
          <span>Chi tiết Tệp đính kèm & Kết quả</span>
          {attachment.attachmentCode && <Tag color="blue">{attachment.attachmentCode}</Tag>}
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="download" icon={<DownloadOutlined />} onClick={() => onDownload(attachment)}>
          Tải tệp về
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      width={760}
    >
      <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="Bệnh nhân">
          <strong>{attachment.patientName}</strong> ({attachment.patientCode || 'N/A'})
        </Descriptions.Item>
        <Descriptions.Item label="Loại kết quả">
          <Tag color="purple">{attachment.category}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Thời gian thực hiện">{attachment.testDate}</Descriptions.Item>
        <Descriptions.Item label="Người tải lên / BS">{attachment.doctorName}</Descriptions.Item>
        <Descriptions.Item label="Trạng thái chỉ số" span={2}>
          <Tag color={STATUS_MAP[attachment.status]?.color || 'success'}>
            {STATUS_MAP[attachment.status]?.label || 'Bình thường'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Tóm tắt kết quả" span={2}>
          <Text style={{ whiteSpace: 'pre-line' }}>{attachment.resultSummary}</Text>
        </Descriptions.Item>
        {attachment.note && (
          <Descriptions.Item label="Ghi chú" span={2}>
            <Text type="danger">{attachment.note}</Text>
          </Descriptions.Item>
        )}
      </Descriptions>

      <Divider orientation="left" style={{ margin: '12px 0' }}>
        <Space>
          {getFileIcon(attachment.fileType, attachment.fileName)}
          <span>Xem trước tệp: {attachment.fileName}</span>
        </Space>
      </Divider>

      <div
        style={{
          background: '#f8fafc',
          padding: 16,
          borderRadius: 8,
          textAlign: 'center',
          minHeight: 200,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexDirection: 'column',
        }}
      >
        {attachment.fileType?.startsWith('image/') ||
        /\.(jpg|jpeg|png|webp|gif)$/i.test(attachment.fileName) ? (
          <Image
            src={
              attachment.fileUrl ||
              'https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80'
            }
            alt={attachment.fileName}
            style={{ maxHeight: 380, objectFit: 'contain', borderRadius: 6 }}
          />
        ) : (
          <div>
            <FilePdfOutlined style={{ fontSize: 64, color: '#dc2626', marginBottom: 12 }} />
            <Typography.Paragraph>
              Tệp văn bản PDF: <strong>{attachment.fileName}</strong> ({attachment.fileSize})
            </Typography.Paragraph>
            <Button type="primary" icon={<DownloadOutlined />} onClick={() => onDownload(attachment)}>
              Mở / Tải tệp PDF
            </Button>
          </div>
        )}
      </div>
    </Modal>
  )
}

export default AttachmentPreviewModal
