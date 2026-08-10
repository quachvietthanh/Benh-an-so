import React from 'react'
import {
  AlertOutlined,
  CheckCircleOutlined,
  FileImageOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import axiosClient from '../../api/axiosClient'

// Live Database Category Options Loader from Backend CSDL
export const fetchDatabaseCategoryOptions = async () => {
  try {
    const res = await axiosClient.get('/clinical-services', { params: { size: 100 } })
    const items = res.data?.content || res.data || []
    if (Array.isArray(items) && items.length > 0) {
      const categoriesSet = new Set(items.map((item) => item.category || item.department || item.name))
      return Array.from(categoriesSet).map((cat) => ({
        value: cat,
        label: cat,
      }))
    }
  } catch (err) {
    console.warn('Backend CSDL unavailable, falling back to default categories:', err)
  }
  return CATEGORY_OPTIONS
}

export const CATEGORY_OPTIONS = [
  { value: 'Công thức máu', label: 'Công thức máu (22 thông số)', color: 'red' },
  { value: 'Sinh hóa máu', label: 'Sinh hóa máu (Glucose, Ure, Liver/Kidney)', color: 'gold' },
  { value: 'X-quang', label: 'Chẩn đoán hình ảnh X-quang', color: 'blue' },
  { value: 'Siêu âm', label: 'Siêu âm bụng / tim / giáp', color: 'cyan' },
  { value: 'Nước tiểu', label: 'Xét nghiệm Nước tiểu (10 thông số)', color: 'purple' },
  { value: 'CT Scanner', label: 'Chụp cắt lớp vi tính CT Scanner', color: 'magenta' },
  { value: 'MRI', label: 'Chụp cộng hưởng từ MRI', color: 'geekblue' },
  { value: 'Khác', label: 'Khác / Kết quả ngoại viện', color: 'default' },
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
