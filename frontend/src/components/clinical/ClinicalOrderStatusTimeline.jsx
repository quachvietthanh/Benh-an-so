import React from 'react'
import { Steps } from 'antd'
import {
  FileAddOutlined,
  SyncOutlined,
  FileDoneOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'

const { Step } = Steps

const STATUS_INDEX_MAP = {
  ORDERED: 0,
  PENDING: 0,
  IN_PROGRESS: 1,
  RESULTED: 2,
  PARTIALLY_COMPLETED: 2,
  COMPLETED: 3,
  CANCELLED: -1,
}

export const ClinicalOrderStatusTimeline = ({ status = 'PENDING', createdAt, updatedAt, cancelledBy, cancelReason }) => {
  if (status === 'CANCELLED') {
    const cancelTime = updatedAt ? new Date(updatedAt).toLocaleString('vi-VN') : 'Đã lưu lịch sử'
    return (
      <div style={{ background: '#fff2f0', border: '1px solid #ffccc7', borderRadius: 8, padding: '14px 18px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
          <span style={{ color: '#cf1322', fontWeight: 600, fontSize: 15 }}>
            <CloseCircleOutlined style={{ marginRight: 6 }} /> Đã hủy chỉ định (Lưu lịch sử)
          </span>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>Thực hiện lúc: {cancelTime}</span>
        </div>
        <div style={{ fontSize: 13, color: '#434343' }}>
          <b>Người hủy:</b> {cancelledBy || 'Bác sĩ chỉ định'}
        </div>
        <div style={{ fontSize: 13, color: '#cf1322', marginTop: 4 }}>
          <b>Lý do hủy:</b> {cancelReason || 'Người dùng yêu cầu hủy phiếu chỉ định'}
        </div>
      </div>
    )
  }

  const currentStep = STATUS_INDEX_MAP[status] ?? 0

  return (
    <div style={{ background: '#fafafa', borderRadius: 8, padding: '16px 20px', border: '1px solid #f0f0f0' }}>
      <Steps current={currentStep} size="small" responsive>
        <Step
          title="Tạo chỉ định"
          description={createdAt ? new Date(createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : 'Khởi tạo'}
          icon={<FileAddOutlined />}
        />
        <Step
          title="Đang thực hiện"
          description={currentStep >= 1 ? 'Tại phòng CLS' : 'Chờ tiếp nhận'}
          icon={<SyncOutlined spin={status === 'IN_PROGRESS'} />}
        />
        <Step
          title="Có kết quả"
          description={currentStep >= 2 ? 'Đã nhập KQ' : 'Chờ KQ xét nghiệm'}
          icon={<FileDoneOutlined />}
        />
        <Step
          title="Hoàn tất"
          description={currentStep >= 3 ? 'Bác sĩ kết luận' : 'Chờ hoàn tất'}
          icon={<CheckCircleOutlined />}
        />
      </Steps>
    </div>
  )
}

export default ClinicalOrderStatusTimeline
