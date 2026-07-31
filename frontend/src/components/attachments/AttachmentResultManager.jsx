import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Form, message, Tabs } from 'antd'
import { CloudUploadOutlined, FileTextOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuthContext } from '../../context/AuthContext'
import medicalRecordApi from '../../api/medicalRecordApi'
import patientApi from '../../api/patientApi'
import { getPatients } from '../../services/mockDataService'
import {
  deleteStoredAttachment,
  getStoredAttachments,
  mergeAttachments,
  saveStoredAttachment,
} from '../../utils/attachmentHelpers'
import { mergePatients } from '../../utils/storageHelpers'

import { CATEGORY_OPTIONS, STATUS_MAP } from './attachmentConstants.jsx'
import AttachmentFilterBar from './AttachmentFilterBar'
import AttachmentPreviewModal from './AttachmentPreviewModal'
import AttachmentStatsCards from './AttachmentStatsCards'
import AttachmentTable from './AttachmentTable'
import AttachmentUploadForm from './AttachmentUploadForm'

function AttachmentResultManager({ patientIdFilter = null, patientNameFilter = null, compact = false }) {
  const { user } = useAuthContext()
  const isDoctorOrAdmin = user?.roles?.some((role) =>
    ['admin', 'doctor', 'role_admin', 'role_doctor', 'manager'].includes(String(role).toLowerCase())
  )

  const [form] = Form.useForm()
  const [attachments, setAttachments] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [activeTab, setActiveTab] = useState('list')
  const [fileList, setFileList] = useState([])
  const [selectedAttachment, setSelectedAttachment] = useState(null)
  const [previewModalOpen, setPreviewModalOpen] = useState(false)

  // Filters state
  const [searchText, setSearchText] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [selectedStatus, setSelectedStatus] = useState('ALL')

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [patientRes] = await Promise.allSettled([patientApi.getAll({ page: 0, size: 200 })])
      let loadedPatients = []
      if (patientRes.status === 'fulfilled') {
        loadedPatients = patientRes.value.data?.content || []
      }
      const combinedPatients = mergePatients(loadedPatients.length ? loadedPatients : getPatients())
      setPatients(combinedPatients)

      let apiAttachments = []
      try {
        const recordRes = await medicalRecordApi.getAll()
        const records = recordRes.data || []
        records.forEach((rec) => {
          if (Array.isArray(rec.attachments)) {
            rec.attachments.forEach((att) => {
              apiAttachments.push({
                id: att.id || `att-${rec.id}`,
                attachmentCode: att.attachmentCode || `KQ-${rec.recordCode || rec.id}`,
                patientId: rec.patientId,
                patientName: rec.patientName || 'Bệnh nhân',
                category: att.category || 'Khác',
                testDate: rec.createdAt || dayjs().format('YYYY-MM-DD HH:mm'),
                doctorName: rec.doctorName || user?.fullName || 'Bác sĩ',
                status: att.status || 'NORMAL',
                resultSummary: att.description || rec.diagnosis || 'Kết quả cận lâm sàng đính kèm',
                fileName: att.fileName || 'tep_dinh_kem.pdf',
                fileType: att.fileType || 'application/pdf',
                fileSize: att.fileSize || '1.5 MB',
                fileUrl: att.fileUrl || '',
              })
            })
          }
        })
      } catch {
        // Fallback to local stored items
      }

      setAttachments(mergeAttachments(apiAttachments))
    } catch (error) {
      console.error('Error loading attachments:', error)
      setAttachments(getStoredAttachments())
    } finally {
      setLoading(false)
    }
  }, [user?.fullName])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Filtered list
  const filteredAttachments = useMemo(() => {
    return attachments.filter((att) => {
      if (patientIdFilter && String(att.patientId) !== String(patientIdFilter)) return false
      if (selectedCategory !== 'ALL' && att.category !== selectedCategory) return false
      if (selectedStatus !== 'ALL' && att.status !== selectedStatus) return false
      if (searchText.trim()) {
        const kw = searchText.trim().toLowerCase()
        const matchName = (att.patientName || '').toLowerCase().includes(kw)
        const matchCode = (att.patientCode || att.attachmentCode || '').toLowerCase().includes(kw)
        const matchFile = (att.fileName || '').toLowerCase().includes(kw)
        const matchSummary = (att.resultSummary || '').toLowerCase().includes(kw)
        const matchCat = (att.category || '').toLowerCase().includes(kw)
        if (!matchName && !matchCode && !matchFile && !matchSummary && !matchCat) return false
      }
      return true
    })
  }, [attachments, patientIdFilter, selectedCategory, selectedStatus, searchText])

  // Stats
  const stats = useMemo(() => {
    const list = patientIdFilter
      ? attachments.filter((a) => String(a.patientId) === String(patientIdFilter))
      : attachments
    return {
      total: list.length,
      abnormal: list.filter((a) => a.status === 'ABNORMAL').length,
      normal: list.filter((a) => a.status === 'NORMAL').length,
      today: list.filter((a) => dayjs(a.createdAt || a.testDate).isSame(dayjs(), 'day')).length,
    }
  }, [attachments, patientIdFilter])

  // Handlers
  const handleUploadSubmit = async (values) => {
    if (!fileList.length) {
      message.error('Vui lòng chọn ít nhất 01 tệp đính kèm (PDF, PNG, JPG)')
      return
    }

    setUploading(true)
    const selectedPatient = patients.find((p) => String(p.id) === String(values.patientId))
    const uploadedFilesCreated = []

    for (const fileObj of fileList) {
      const rawFile = fileObj.originFileObj || fileObj
      let objectUrl = ''
      if (rawFile && rawFile instanceof Blob) {
        try {
          objectUrl = URL.createObjectURL(rawFile)
        } catch {
          objectUrl = ''
        }
      }

      const newAttachment = {
        id: `att-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
        attachmentCode: `KQ-${dayjs().format('YYYYMMDD-HHmmss')}`,
        patientId: values.patientId,
        patientName: selectedPatient ? selectedPatient.fullName : (patientNameFilter || 'Bệnh nhân'),
        patientCode: selectedPatient ? selectedPatient.patientCode : '',
        category: values.category,
        categoryLabel: CATEGORY_OPTIONS.find((c) => c.value === values.category)?.label || values.category,
        testDate: dayjs().format('YYYY-MM-DD HH:mm'),
        doctorName: user?.fullName || user?.username || 'Bác sĩ / KTV',
        status: values.status || 'NORMAL',
        statusLabel: STATUS_MAP[values.status || 'NORMAL']?.label || 'Bình thường',
        resultSummary: values.resultSummary || 'Đã ghi nhận kết quả',
        note: values.note || '',
        fileName: fileObj.name,
        fileType: fileObj.type || (fileObj.name.endsWith('.pdf') ? 'application/pdf' : 'image/jpeg'),
        fileSize: fileObj.size ? `${(fileObj.size / (1024 * 1024)).toFixed(1)} MB` : '1.2 MB',
        fileUrl: objectUrl || 'https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80',
        createdAt: dayjs().toISOString(),
      }

      try {
        if (values.medicalRecordId) {
          await medicalRecordApi.attach(values.medicalRecordId, rawFile)
        }
      } catch {
        // Fallback save local
      }

      saveStoredAttachment(newAttachment)
      uploadedFilesCreated.push(newAttachment)
    }

    setUploading(false)
    message.success(`Đã tải lên và lưu ${uploadedFilesCreated.length} tệp kết quả thành công!`)
    form.resetFields()
    setFileList([])
    setAttachments(getStoredAttachments())
    setActiveTab('list')
  }

  const handleDeleteAttachment = (id) => {
    deleteStoredAttachment(id)
    setAttachments(getStoredAttachments())
    message.success('Đã xóa tệp đính kèm khỏi hệ thống')
  }

  const handleOpenPreview = (record) => {
    setSelectedAttachment(record)
    setPreviewModalOpen(true)
  }

  const handleDownload = (record) => {
    if (record?.fileUrl && record.fileUrl.startsWith('blob:')) {
      const link = document.createElement('a')
      link.href = record.fileUrl
      link.download = record.fileName
      link.click()
    } else {
      message.info(`Đang tải tệp: ${record?.fileName}`)
      const link = document.createElement('a')
      link.href = record?.fileUrl || '#'
      link.target = '_blank'
      link.download = record?.fileName || 'download'
      link.click()
    }
  }

  return (
    <div className="attachment-result-manager">
      {!compact && <AttachmentStatsCards stats={stats} />}

      <Card bodyStyle={{ padding: compact ? 12 : 20 }} style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          tabBarExtraContent={
            isDoctorOrAdmin && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setActiveTab('upload')
                  if (patientIdFilter) {
                    form.setFieldsValue({ patientId: patientIdFilter })
                  }
                }}
              >
                Tải lên tệp kết quả mới
              </Button>
            )
          }
          items={[
            {
              key: 'list',
              label: (
                <span>
                  <FileTextOutlined /> Danh sách kết quả & Tệp đính kèm ({filteredAttachments.length})
                </span>
              ),
              children: (
                <div>
                  <AttachmentFilterBar
                    searchText={searchText}
                    setSearchText={setSearchText}
                    selectedCategory={selectedCategory}
                    setSelectedCategory={setSelectedCategory}
                    selectedStatus={selectedStatus}
                    setSelectedStatus={setSelectedStatus}
                    onReload={loadData}
                  />

                  <AttachmentTable
                    attachments={filteredAttachments}
                    loading={loading}
                    compact={compact}
                    patientIdFilter={patientIdFilter}
                    isDoctorOrAdmin={isDoctorOrAdmin}
                    onOpenPreview={handleOpenPreview}
                    onDownload={handleDownload}
                    onDelete={handleDeleteAttachment}
                  />
                </div>
              ),
            },
            {
              key: 'upload',
              label: (
                <span>
                  <CloudUploadOutlined /> Tải lên & Nhập kết quả
                </span>
              ),
              children: (
                <AttachmentUploadForm
                  form={form}
                  patients={patients}
                  patientIdFilter={patientIdFilter}
                  fileList={fileList}
                  setFileList={setFileList}
                  uploading={uploading}
                  onSubmit={handleUploadSubmit}
                  onCancel={() => setActiveTab('list')}
                />
              ),
            },
          ]}
        />
      </Card>

      <AttachmentPreviewModal
        open={previewModalOpen}
        attachment={selectedAttachment}
        onClose={() => setPreviewModalOpen(false)}
        onDownload={handleDownload}
      />
    </div>
  )
}

export default AttachmentResultManager
