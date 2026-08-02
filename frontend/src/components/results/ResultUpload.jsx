import React, { useRef } from 'react'
import { Button, Typography, Tooltip, message } from 'antd'
import {
  UploadOutlined,
  FilePdfOutlined,
  FileImageOutlined,
  DeleteOutlined,
  LockOutlined,
} from '@ant-design/icons'

const { Text } = Typography

const ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg']
const MAX_SIZE_MB = 10

export const ResultUpload = ({ fileList = [], onChange, error, disabled = false }) => {
  const fileInputRef = useRef(null)

  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 KB'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const handleFileSelect = (e) => {
    if (disabled) return
    const selectedFiles = Array.from(e.target.files || [])
    if (!selectedFiles.length) return

    let validFiles = []

    selectedFiles.forEach((file) => {
      if (!ALLOWED_TYPES.includes(file.type) && !file.name.match(/\.(pdf|jpg|jpeg|png)$/i)) {
        message.error(`File ${file.name} không thuộc định dạng cho phép (.pdf, .jpg, .png)!`)
        return
      }

      if (file.size > MAX_SIZE_MB * 1024 * 1024) {
        message.error(`File ${file.name} vượt quá dung lượng cho phép (${MAX_SIZE_MB}MB)!`)
        return
      }

      const previewUrl = file.type.startsWith('image/') ? URL.createObjectURL(file) : null

      validFiles.push({
        id: `file-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
        file,
        name: file.name,
        size: file.size,
        type: file.type,
        previewUrl,
      })
    })

    if (validFiles.length > 0) {
      onChange([...fileList, ...validFiles])
    }

    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleRemoveFile = (id) => {
    if (disabled) return
    const updated = fileList.filter((f) => f.id !== id)
    onChange(updated)
  }

  return (
    <div style={{ marginTop: 8 }}>
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept=".pdf,.jpg,.jpeg,.png"
        onChange={handleFileSelect}
        style={{ display: 'none' }}
        disabled={disabled}
      />

      {!disabled ? (
        <div
          onClick={() => fileInputRef.current?.click()}
          style={{
            border: error ? '2px dashed #ff4d4f' : '2px dashed #93c5fd',
            borderRadius: '12px',
            padding: '16px',
            textAlign: 'center',
            background: '#f8fafc',
            cursor: 'pointer',
            transition: 'all 0.2s',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#2563eb')}
          onMouseLeave={(e) => (e.currentTarget.style.borderColor = error ? '#ff4d4f' : '#93c5fd')}
        >
          <UploadOutlined style={{ fontSize: 24, color: '#2563eb', marginBottom: 6 }} />
          <div style={{ fontWeight: 600, color: '#1e293b', fontSize: 14 }}>
            Tải lên tài liệu hoặc ảnh chụp kết quả
          </div>
          <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>
            Định dạng hỗ trợ: <b>PDF, JPG, PNG</b> (Dung lượng tối đa: 10MB/file)
          </div>
        </div>
      ) : (
        <div
          style={{
            border: '1px solid #cbd5e1',
            borderRadius: '12px',
            padding: '12px 16px',
            background: '#f1f5f9',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            color: '#64748b',
          }}
        >
          <LockOutlined style={{ fontSize: 18, color: '#64748b' }} />
          <span style={{ fontSize: 13, fontWeight: 500 }}>
            Hệ thống đang ở chế độ <b>Khóa chỉnh sửa</b>. Không thể tải lên thêm file mới.
          </span>
        </div>
      )}

      {error && (
        <div style={{ color: '#ff4d4f', fontSize: 12, marginTop: 4 }}>
          {error}
        </div>
      )}

      {/* File List & Preview Area */}
      {Array.isArray(fileList) && fileList.length > 0 && (
        <div style={{ marginTop: 12 }}>
          <Text style={{ fontWeight: 600, color: '#475569', fontSize: 13 }}>
            Tệp đã đính kèm ({fileList.length}):
          </Text>
          <div style={{ marginTop: 6, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {fileList.map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 12px',
                  borderRadius: '8px',
                  border: '1px solid #e2e8f0',
                  background: '#ffffff',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, overflow: 'hidden' }}>
                  {item.previewUrl ? (
                    <img
                      src={item.previewUrl}
                      alt={item.name}
                      style={{ width: 36, height: 36, objectFit: 'cover', borderRadius: 6, border: '1px solid #cbd5e1' }}
                    />
                  ) : item.name?.endsWith('.pdf') || item.type?.includes('pdf') ? (
                    <FilePdfOutlined style={{ fontSize: 28, color: '#ff4d4f' }} />
                  ) : (
                    <FileImageOutlined style={{ fontSize: 28, color: '#1890ff' }} />
                  )}

                  <div style={{ overflow: 'hidden' }}>
                    <div
                      style={{
                        fontWeight: 600,
                        fontSize: 13,
                        color: '#1e293b',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        maxWidth: 320,
                      }}
                      title={item.name}
                    >
                      {item.name}
                    </div>
                    <div style={{ fontSize: 11, color: '#94a3b8' }}>
                      {formatFileSize(item.size)}
                    </div>
                  </div>
                </div>

                {!disabled && (
                  <Tooltip title="Xóa file này">
                    <Button
                      type="text"
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={() => handleRemoveFile(item.id)}
                    />
                  </Tooltip>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default ResultUpload
