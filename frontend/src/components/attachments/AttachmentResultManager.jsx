import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Form, message, Tabs } from 'antd'
import { CloudUploadOutlined, FileTextOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuthContext } from '../../context/AuthContext'
import medicalRecordApi from '../../api/medicalRecordApi'
import patientApi from '../../api/patientApi'
import clinicalResultApi from '../../api/clinicalResultApi'
import AttachmentFilterBar from './AttachmentFilterBar'
import AttachmentPreviewModal from './AttachmentPreviewModal'
import AttachmentStatsCards from './AttachmentStatsCards'
import AttachmentTable from './AttachmentTable'
import AttachmentUploadForm from './AttachmentUploadForm'

const toCollection = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

const formatFileSize = (bytes) => {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value <= 0) return '0 B'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

const toDisplayStatus = (abnormalFlag) => {
  if (abnormalFlag === 'NORMAL') return 'NORMAL'
  if (['LOW', 'HIGH', 'ABNORMAL'].includes(abnormalFlag)) return 'ABNORMAL'
  return 'PENDING'
}

const createResultContext = (result, record, fallbackPatient) => {
  const patient = record?.patient || fallbackPatient || {}
  const visit = record?.visit || {}
  return {
    ...result,
    resultId: result.id,
    visitId: result.visitId || visit.id,
    visitCode: visit.visitCode,
    patientId: patient.id,
    patientName: patient.fullName,
    patientCode: patient.patientCode,
    doctorName: visit.doctorName,
    visitAt: visit.startedAt || visit.visitAt,
  }
}

const createAttachmentRow = (attachment, result) => ({
  id: attachment.id,
  resultId: result.id,
  visitId: result.visitId,
  visitCode: result.visitCode,
  clinicalOrderItemId: result.clinicalOrderItemId,
  attachmentCode: String(attachment.id || '').slice(0, 8).toUpperCase(),
  patientId: result.patientId,
  patientName: result.patientName || 'Bệnh nhân',
  patientCode: result.patientCode,
  category: attachment.attachmentType || result.resultType || 'OTHER',
  testDate: result.visitAt,
  doctorName: result.doctorName || '—',
  status: toDisplayStatus(result.abnormalFlag),
  resultStatus: result.status,
  resultSummary:
    result.conclusion
    || result.textValue
    || (result.numericValue != null
      ? `${result.numericValue}${result.unit ? ` ${result.unit}` : ''}`
      : 'Chưa có kết luận'),
  note: result.referenceRange ? `Khoảng tham chiếu: ${result.referenceRange}` : '',
  fileName: attachment.fileName,
  fileType: attachment.contentType,
  fileSize: formatFileSize(attachment.fileSize),
  attachmentType: attachment.attachmentType,
})

function AttachmentResultManager({ patientIdFilter = null, patientNameFilter = null, compact = false }) {
  const { user } = useAuthContext()
  const roles = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
  const isDoctorOrAdmin = roles.some((role) =>
    ['admin', 'doctor', 'role_admin', 'role_doctor'].includes(String(role).toLowerCase()),
  )

  const [form] = Form.useForm()
  const [attachments, setAttachments] = useState([])
  const [clinicalResults, setClinicalResults] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [downloadingId, setDownloadingId] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [activeTab, setActiveTab] = useState('list')
  const [fileList, setFileList] = useState([])
  const [selectedAttachment, setSelectedAttachment] = useState(null)
  const [previewModalOpen, setPreviewModalOpen] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [selectedStatus, setSelectedStatus] = useState('ALL')

  const loadData = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      let loadedPatients = []
      let medicalRecords = []
      const partialErrors = []

      if (patientIdFilter) {
        const recordResponse = await medicalRecordApi.getByPatient(patientIdFilter)
        medicalRecords = toCollection(recordResponse.data)
        const recordPatient = medicalRecords[0]?.patient
        loadedPatients = [{
          id: recordPatient?.id || patientIdFilter,
          patientCode: recordPatient?.patientCode,
          fullName: recordPatient?.fullName || patientNameFilter || 'Bệnh nhân',
        }]
      } else {
        const patientResponse = await patientApi.getAll({ page: 0, size: 200 })
        loadedPatients = toCollection(patientResponse.data)
        const recordResponses = await Promise.allSettled(
          loadedPatients.map((patient) => medicalRecordApi.getByPatient(patient.id)),
        )
        recordResponses.forEach((response) => {
          if (response.status === 'fulfilled') {
            medicalRecords.push(...toCollection(response.value.data))
          } else {
            partialErrors.push(response.reason)
          }
        })
      }

      const patientById = new Map(loadedPatients.map((patient) => [String(patient.id), patient]))
      const recordByVisitId = new Map()
      medicalRecords.forEach((record) => {
        const visitId = record?.visit?.id || record?.visitId
        if (visitId) recordByVisitId.set(String(visitId), record)
      })

      const visitIds = Array.from(recordByVisitId.keys())
      const resultResponses = await Promise.allSettled(
        visitIds.map((visitId) => clinicalResultApi.getByVisit(visitId, { page: 0, size: 100 })),
      )
      const resultMap = new Map()
      resultResponses.forEach((response, index) => {
        if (response.status === 'rejected') {
          partialErrors.push(response.reason)
          return
        }
        const visitId = visitIds[index]
        const record = recordByVisitId.get(String(visitId))
        const fallbackPatient = patientById.get(String(record?.patient?.id))
        toCollection(response.value.data).forEach((result) => {
          if (result?.id) {
            resultMap.set(String(result.id), createResultContext(result, record, fallbackPatient))
          }
        })
      })

      const nextResults = Array.from(resultMap.values())
      const nextAttachments = nextResults.flatMap((result) =>
        toCollection(result.attachments).map((attachment) => createAttachmentRow(attachment, result)),
      )

      setPatients(loadedPatients)
      setClinicalResults(nextResults)
      setAttachments(nextAttachments)
      if (partialErrors.length > 0) {
        setLoadError(`Không tải được dữ liệu của ${partialErrors.length} lượt khám. Danh sách đang hiển thị có thể chưa đầy đủ.`)
      }
    } catch (error) {
      setPatients([])
      setClinicalResults([])
      setAttachments([])
      setLoadError(getErrorMessage(error, 'Không thể tải kết quả cận lâm sàng từ máy chủ.'))
    } finally {
      setLoading(false)
    }
  }, [patientIdFilter, patientNameFilter])

  useEffect(() => {
    loadData()
  }, [loadData])

  const categoryOptions = useMemo(() => {
    const categories = new Set(attachments.map((attachment) => attachment.category).filter(Boolean))
    return Array.from(categories).sort().map((category) => ({ value: category, label: category }))
  }, [attachments])

  const filteredAttachments = useMemo(() => attachments.filter((attachment) => {
    if (patientIdFilter && String(attachment.patientId) !== String(patientIdFilter)) return false
    if (selectedCategory !== 'ALL' && attachment.category !== selectedCategory) return false
    if (selectedStatus !== 'ALL' && attachment.status !== selectedStatus) return false
    if (!searchText.trim()) return true
    const keyword = searchText.trim().toLowerCase()
    return [
      attachment.patientName,
      attachment.patientCode,
      attachment.attachmentCode,
      attachment.fileName,
      attachment.resultSummary,
      attachment.category,
      attachment.visitCode,
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
  }), [attachments, patientIdFilter, selectedCategory, selectedStatus, searchText])

  const stats = useMemo(() => {
    const list = patientIdFilter
      ? attachments.filter((attachment) => String(attachment.patientId) === String(patientIdFilter))
      : attachments
    return {
      total: list.length,
      abnormal: list.filter((attachment) => attachment.status === 'ABNORMAL').length,
      normal: list.filter((attachment) => attachment.status === 'NORMAL').length,
      today: list.filter((attachment) =>
        attachment.testDate && dayjs(attachment.testDate).isSame(dayjs(), 'day'),
      ).length,
    }
  }, [attachments, patientIdFilter])

  const handleUploadSubmit = async (values) => {
    if (!fileList.length) {
      message.error('Vui lòng chọn ít nhất một tệp PDF, PNG hoặc JPG.')
      return
    }
    const targetResult = clinicalResults.find((result) => String(result.id) === String(values.resultId))
    if (!targetResult || String(targetResult.patientId) !== String(values.patientId)) {
      message.error('Kết quả cận lâm sàng không thuộc bệnh nhân đã chọn.')
      return
    }

    setUploading(true)
    try {
      const uploads = await Promise.allSettled(fileList.map((file) =>
        clinicalResultApi.uploadAttachment(values.resultId, file.originFileObj || file),
      ))
      const succeededCount = uploads.filter((upload) => upload.status === 'fulfilled').length
      const failedFiles = fileList.filter((_, index) => uploads[index].status === 'rejected')

      if (succeededCount > 0) await loadData()

      if (failedFiles.length === 0) {
        message.success(`Đã tải lên ${succeededCount} tệp cho kết quả cận lâm sàng.`)
        form.resetFields()
        if (patientIdFilter) form.setFieldsValue({ patientId: patientIdFilter })
        setFileList([])
        setActiveTab('list')
      } else {
        setFileList(failedFiles)
        const firstFailure = uploads.find((upload) => upload.status === 'rejected')
        const detail = getErrorMessage(firstFailure?.reason, 'Máy chủ từ chối tệp tải lên.')
        if (succeededCount > 0) {
          message.warning(`Đã tải ${succeededCount} tệp; còn ${failedFiles.length} tệp lỗi. ${detail}`)
        } else {
          message.error(detail)
        }
      }
    } finally {
      setUploading(false)
    }
  }

  const handleOpenPreview = (record) => {
    setSelectedAttachment(record)
    setPreviewModalOpen(true)
  }

  const handleDownload = async (record) => {
    if (!record?.id) return
    setDownloadingId(record.id)
    try {
      const response = await clinicalResultApi.getDownloadUrl(record.id)
      const url = response.data?.url
      if (!url) throw new Error('Máy chủ không trả về đường dẫn tải tệp.')
      const link = document.createElement('a')
      link.href = url
      link.target = '_blank'
      link.rel = 'noopener noreferrer'
      link.click()
    } catch (error) {
      message.error(getErrorMessage(error, 'Không thể tạo đường dẫn tải tệp.'))
    } finally {
      setDownloadingId(null)
    }
  }

  return (
    <div className="attachment-result-manager">
      {!compact && <AttachmentStatsCards stats={stats} />}

      {loadError && (
        <Alert
          type="warning"
          showIcon
          message="Dữ liệu kết quả chưa đầy đủ"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Card bodyStyle={{ padding: compact ? 12 : 20 }} style={{ borderRadius: 10, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          tabBarExtraContent={isDoctorOrAdmin && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              disabled={clinicalResults.length === 0}
              onClick={() => {
                setActiveTab('upload')
                form.setFieldsValue({
                  patientId: patientIdFilter || undefined,
                  resultId: undefined,
                })
              }}
            >
              Tải tệp cho kết quả
            </Button>
          )}
          items={[
            {
              key: 'list',
              label: <span><FileTextOutlined /> Tệp kết quả ({filteredAttachments.length})</span>,
              children: (
                <div>
                  <AttachmentFilterBar
                    searchText={searchText}
                    setSearchText={setSearchText}
                    selectedCategory={selectedCategory}
                    setSelectedCategory={setSelectedCategory}
                    selectedStatus={selectedStatus}
                    setSelectedStatus={setSelectedStatus}
                    categoryOptions={categoryOptions}
                    onReload={loadData}
                  />
                  <AttachmentTable
                    attachments={filteredAttachments}
                    loading={loading}
                    compact={compact}
                    patientIdFilter={patientIdFilter}
                    downloadingId={downloadingId}
                    onOpenPreview={handleOpenPreview}
                    onDownload={handleDownload}
                  />
                </div>
              ),
            },
            {
              key: 'upload',
              label: <span><CloudUploadOutlined /> Tải tệp lên kết quả</span>,
              children: (
                <AttachmentUploadForm
                  form={form}
                  patients={patients}
                  clinicalResults={clinicalResults}
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
