import React from 'react'
import {
  AlertOutlined,
  CheckCircleOutlined,
  FileImageOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  ReloadOutlined,
} from '@ant-design/icons'

export const CATEGORY_OPTIONS = [
  { value: 'Công thức máu', label: '🩸 Công thức máu 18-24 chỉ số', color: 'red' },
  { value: 'Sinh hóa máu', label: '🧪 Sinh hóa máu (Glucose, Ure, Liver/Kidney)', color: 'gold' },
  { value: 'X-quang', label: '🩻 X-quang (Ngực, Xương, Khớp)', color: 'blue' },
  { value: 'Siêu âm', label: '🖥️ Siêu âm (Bụng, Tim, Giáp, Mạch)', color: 'cyan' },
  { value: 'Nước tiểu', label: '🧪 Xét nghiệm Nước tiểu', color: 'purple' },
  { value: 'CT Scanner', label: '🧠 Cắt lớp vi tính CT Scanner', color: 'magenta' },
  { value: 'MRI', label: '🧲 Cộng hưởng từ MRI', color: 'geekblue' },
  { value: 'Khác', label: '📋 Khác / Hồ sơ kết quả ngoài', color: 'default' },
]

export const STATUS_MAP = {
  NORMAL: { label: 'Bình thường', color: 'success', icon: <CheckCircleOutlined /> },
  ABNORMAL: { label: 'Cần chú ý / Bất thường', color: 'error', icon: <AlertOutlined /> },
  PENDING: { label: 'Chờ bác sĩ đọc', color: 'warning', icon: <ReloadOutlined /> },
}

export const getFileIcon = (fileType = '', fileName = '') => {
  const isImage = fileType.startsWith('image/') || /\.(jpg|jpeg|png|webp|gif)$/i.test(fileName)
  const isPdf = fileType.includes('pdf') || fileName.endsWith('.pdf')
  if (isImage) return <FileImageOutlined style={{ color: '#0284c7', fontSize: 22 }} />
  if (isPdf) return <FilePdfOutlined style={{ color: '#dc2626', fontSize: 22 }} />
  return <FileTextOutlined style={{ color: '#64748b', fontSize: 22 }} />
}
