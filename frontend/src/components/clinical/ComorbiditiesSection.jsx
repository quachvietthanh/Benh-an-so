import React, { useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Modal,
  Popover,
  Row,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  PlusCircleOutlined,
  PlusOutlined,
  SwapOutlined,
} from '@ant-design/icons'
import DiagnosisCatalogAutocomplete from '../diagnosis-catalog/DiagnosisCatalogAutocomplete'
import { fixMojibake } from '../../utils/serviceCatalogValidation'
import { getDiseaseGroupName } from '../../utils/icd10Data'
import {
  COMMON_COMORBIDITIES_SUGGESTIONS,
  validateCanAddComorbidity,
} from '../../utils/comorbiditiesHelpers'

const { Text } = Typography

const categoryMeta = {
  RESPIRATORY: { label: 'Hô hấp', color: 'cyan' },
  CIRCULATORY: { label: 'Tim mạch', color: 'red' },
  DIGESTIVE: { label: 'Tiêu hóa', color: 'orange' },
  ENDOCRINE: { label: 'Nội tiết', color: 'gold' },
  MUSCULOSKELETAL: { label: 'Cơ xương khớp', color: 'geekblue' },
  NERVOUS: { label: 'Thần kinh', color: 'purple' },
  INFECTIOUS: { label: 'Nhiễm trùng', color: 'magenta' },
  GENITOURINARY: { label: 'Tiết niệu', color: 'blue' },
  SYMPTOMS: { label: 'Triệu chứng', color: 'volcano' },
  GENERAL: { label: 'Đa khoa', color: 'default' },
}

function ComorbiditiesSection({
  primaryIcd = null,
  secondaryIcds = [],
  onAddSecondary = () => {},
  onRemoveSecondary = () => {},
  onUpdateSecondaryNote = () => {},
  onSwitchToPrimary = () => {},
  isSigned = false,
  isDoctor = true,
  diagnosisOptions = [],
}) {
  const [isFreeTextModalOpen, setIsFreeTextModalOpen] = useState(false)
  const [freeTextName, setFreeTextName] = useState('')
  const [freeTextNote, setFreeTextNote] = useState('')
  const [freeTextError, setFreeTextError] = useState('')

  const canEdit = isDoctor && !isSigned

  // Xử lý thêm từ danh mục
  const handleSelectFromCatalog = (catalogItem) => {
    if (!catalogItem) return
    onAddSecondary(catalogItem)
  }

  // Xử lý thêm chẩn đoán tự do (không mã ICD)
  const handleAddFreeText = () => {
    const trimmedName = freeTextName.trim()
    if (!trimmedName) {
      setFreeTextError('Vui lòng nhập tên bệnh mắc kèm.')
      return
    }

    const candidate = {
      name: trimmedName,
      rawName: trimmedName,
      code: '',
      note: freeTextNote.trim(),
    }

    const validation = validateCanAddComorbidity({
      primaryIcd,
      secondaryIcds,
      candidateIcd: candidate,
    })

    if (!validation.valid) {
      setFreeTextError(validation.error)
      return
    }

    onAddSecondary(candidate)
    setIsFreeTextModalOpen(false)
    setFreeTextName('')
    setFreeTextNote('')
    setFreeTextError('')
  }

  // Cấu hình các cột trong bảng bệnh mắc kèm
  const columns = [
    {
      title: 'STT',
      key: 'index',
      width: 60,
      align: 'center',
      render: (_, __, index) => (
        <span style={{ fontWeight: 600, color: '#64748B', fontSize: 14 }}>
          {index + 1}
        </span>
      ),
    },
    {
      title: 'Mã ICD-10',
      dataIndex: 'code',
      key: 'code',
      width: 120,
      align: 'center',
      render: (code) =>
        code ? (
          <Tag color="purple" style={{ fontWeight: 700, fontSize: 13.5, padding: '3px 10px', borderRadius: 6, margin: 0 }}>
            {code}
          </Tag>
        ) : (
          <Tag color="default" style={{ fontStyle: 'italic', fontSize: 12.5, padding: '3px 8px' }}>
            Tự do
          </Tag>
        ),
    },
    {
      title: 'Tên bệnh / Chẩn đoán kèm theo',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <div>
          <Text strong style={{ fontSize: 14.5, color: '#0F172A', display: 'block', lineHeight: 1.4 }}>
            {fixMojibake(name || record.rawName || '')}
          </Text>
          {record.diseaseGroup && (
            <span style={{ fontSize: 12.5, color: '#64748B', marginTop: 2, display: 'inline-block' }}>
              Nhóm: {record.diseaseGroup}
            </span>
          )}
        </div>
      ),
    },
    {
      title: 'Chuyên khoa',
      dataIndex: 'category',
      key: 'category',
      width: 150,
      align: 'center',
      render: (_, record) => {
        const groupName = record.diseaseGroup || getDiseaseGroupName(record.code, record.diseaseGroup)
        const meta = categoryMeta[record.category] || categoryMeta.GENERAL
        return (
          <Tag color={meta.color} style={{ margin: 0, fontSize: 12.5, padding: '3px 10px', borderRadius: 6 }}>
            {groupName || meta.label}
          </Tag>
        )
      },
    },
    {
      title: 'Ghi chú lâm sàng / Diễn giải',
      dataIndex: 'note',
      key: 'note',
      width: 280,
      render: (note, record, index) => {
        if (!canEdit) {
          return (
            <span style={{ color: note ? '#334155' : '#94A3B8', fontStyle: note ? 'normal' : 'italic', fontSize: 13.5 }}>
              {note || 'Không có ghi chú'}
            </span>
          )
        }

        return (
          <Input
            size="middle"
            placeholder="Ghi chú lâm sàng (mức độ, diễn tiến...)"
            defaultValue={note || ''}
            onBlur={(e) => onUpdateSecondaryNote(record.code || record.id || index, e.target.value)}
            onPressEnter={(e) => onUpdateSecondaryNote(record.code || record.id || index, e.target.value)}
            style={{ borderRadius: 6, fontSize: 13.5, height: 36 }}
            maxLength={500}
            allowClear
          />
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 180,
      align: 'center',
      render: (_, record) => {
        if (!canEdit) {
          return <span style={{ color: '#94A3B8', fontSize: 13 }}>Chỉ xem</span>
        }

        return (
          <Space size={8}>
            <Tooltip title="Đổi mã này thành chẩn đoán chính (hoán đổi)">
              <Button
                size="middle"
                icon={<SwapOutlined style={{ fontSize: 14, color: '#1D4ED8' }} />}
                onClick={() => onSwitchToPrimary(record)}
                style={{
                  backgroundColor: '#EFF6FF',
                  borderColor: '#BFDBFE',
                  color: '#1D4ED8',
                  fontWeight: 600,
                  fontSize: 13,
                  height: 34,
                  padding: '0 12px',
                  borderRadius: 6,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 4,
                }}
              >
                CĐ chính
              </Button>
            </Tooltip>

            <Tooltip title="Xóa bệnh mắc kèm này">
              <Button
                size="middle"
                danger
                icon={<DeleteOutlined style={{ fontSize: 15 }} />}
                onClick={() => onRemoveSecondary(record.code || record.id || record.name)}
                style={{
                  height: 34,
                  width: 34,
                  borderRadius: 6,
                  backgroundColor: '#FEF2F2',
                  borderColor: '#FECACA',
                  color: '#DC2626',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: 0,
                }}
              />
            </Tooltip>
          </Space>
        )
      },
    },
  ]

  return (
    <Card
      size="small"
      style={{
        borderRadius: 8,
        border: '1px solid #E2E8F0',
        backgroundColor: '#FFFFFF',
        boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
        marginBottom: 12,
      }}
      bodyStyle={{ padding: '10px 14px' }}
    >
      {/* Header khu vực bệnh mắc kèm */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10, flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MedicineBoxOutlined style={{ color: '#7C3AED', fontSize: 16 }} />
          <Text strong style={{ fontSize: 14, color: '#0F172A' }}>
            Bệnh mắc kèm (Chẩn đoán kèm theo)
          </Text>
          <Badge
            count={secondaryIcds.length}
            overflowCount={99}
            style={{
              backgroundColor: secondaryIcds.length > 0 ? '#7C3AED' : '#94A3B8',
              fontWeight: 600,
              fontSize: 12,
            }}
          />
          <Tooltip title="Mỗi bệnh án bắt buộc phải có 1 chẩn đoán chính và có thể ghi thêm nhiều bệnh mắc kèm. Dữ liệu được lưu tách bạch để phục vụ phân tích mô hình bệnh tật.">
            <InfoCircleOutlined style={{ color: '#64748B', cursor: 'pointer', fontSize: 14 }} />
          </Tooltip>
        </div>

        {canEdit && (
          <Button
            size="small"
            icon={<PlusCircleOutlined style={{ fontSize: 13 }} />}
            onClick={() => setIsFreeTextModalOpen(true)}
            style={{
              fontSize: 12,
              height: 28,
              padding: '0 10px',
              borderRadius: 6,
              fontWeight: 600,
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            Thêm chẩn đoán tự do
          </Button>
        )}
      </div>

      {/* Thông báo nếu chưa chọn chẩn đoán chính */}
      {!primaryIcd && canEdit && (
        <Alert
          type="info"
          showIcon
          message="Vui lòng chọn mã bệnh chẩn đoán chính trước khi thêm bệnh mắc kèm."
          style={{ marginBottom: 10, padding: '4px 10px', fontSize: 12, borderRadius: 6 }}
        />
      )}

      {/* Thanh tìm kiếm & thêm bệnh kèm qua Autocomplete */}
      {canEdit && (
        <div style={{ marginBottom: 10 }}>
          <DiagnosisCatalogAutocomplete
            size="middle"
            placeholder={
              primaryIcd
                ? '🔍 Tra cứu mã hoặc tên bệnh kèm theo (I10, E11...)'
                : 'Vui lòng chọn chẩn đoán chính ở mục trên trước'
            }
            value={null}
            disabled={!primaryIcd}
            style={{ width: '100%' }}
            fallbackSuggestions={diagnosisOptions}
            onSelect={handleSelectFromCatalog}
          />

          {/* Gợi ý các bệnh mắc kèm thường gặp */}
          <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
            <Text type="secondary" style={{ fontSize: 12, fontWeight: 500, marginRight: 2, color: '#475569' }}>
              Gợi ý bệnh kèm thường gặp:
            </Text>
            {COMMON_COMORBIDITIES_SUGGESTIONS.slice(0, 8).map((suggested) => {
              const isPrimary = primaryIcd?.code === suggested.code
              const isAdded = secondaryIcds.some((s) => s.code === suggested.code)
              const isDisabled = !primaryIcd || isPrimary || isAdded

              return (
                <Tag
                  key={suggested.code}
                  color={isAdded ? 'purple' : isPrimary ? 'blue' : 'default'}
                  style={{
                    cursor: isDisabled ? 'not-allowed' : 'pointer',
                    fontSize: 12,
                    padding: '2px 8px',
                    borderRadius: 4,
                    opacity: isDisabled ? 0.6 : 1,
                    margin: '2px 0',
                    fontWeight: 500,
                    border: isAdded ? '1px solid #C084FC' : '1px solid #CBD5E1',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 3,
                  }}
                  onClick={() => {
                    if (!isDisabled) {
                      onAddSecondary(suggested)
                    }
                  }}
                >
                  <b style={{ fontWeight: 700 }}>{suggested.code}</b>: {suggested.name.split('(')[0].trim()}
                  {isAdded && <span style={{ color: '#7C3AED', fontWeight: 700, marginLeft: 2 }}>✓</span>}
                  {isPrimary && ' (CĐ chính)'}
                </Tag>
              )
            })}
          </div>
        </div>
      )}

      {/* Bảng danh sách bệnh mắc kèm */}
      {secondaryIcds.length > 0 ? (
        <Table
          size="small"
          rowKey={(record, idx) => record.code || record.id || `sec-${idx}`}
          dataSource={secondaryIcds}
          columns={columns}
          pagination={false}
          style={{ border: '1px solid #F1F5F9', borderRadius: 6 }}
        />
      ) : (
        <div style={{ textAlign: 'center', padding: '10px 0', color: '#94A3B8', fontSize: 12.5 }}>
          Chưa có bệnh mắc kèm nào. Sử dụng thanh tìm kiếm hoặc gợi ý phía trên để thêm mã bệnh.
        </div>
      )}

      {/* Modal thêm chẩn đoán tự do khi chưa có trong danh mục ICD-10 */}
      <Modal
        title="Thêm bệnh mắc kèm tự do (Chưa có trong danh mục ICD-10)"
        open={isFreeTextModalOpen}
        onOk={handleAddFreeText}
        onCancel={() => {
          setIsFreeTextModalOpen(false)
          setFreeTextError('')
        }}
        okText="Thêm bệnh kèm"
        cancelText="Hủy bỏ"
        destroyOnClose
      >
        <div style={{ marginTop: 12 }}>
          {freeTextError && (
            <Alert
              type="error"
              showIcon
              message={freeTextError}
              style={{ marginBottom: 12, fontSize: 12.5 }}
            />
          )}

          <div style={{ marginBottom: 12 }}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              Tên bệnh / Chẩn đoán lâm sàng <span style={{ color: 'red' }}>*</span>
            </Text>
            <Input
              placeholder="Nhập tên bệnh hoặc diễn giải tình trạng mắc kèm..."
              value={freeTextName}
              onChange={(e) => {
                setFreeTextName(e.target.value)
                setFreeTextError('')
              }}
              maxLength={255}
              autoFocus
            />
          </div>

          <div>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              Ghi chú / Mức độ / Diễn giải thêm:
            </Text>
            <Input.TextArea
              rows={3}
              placeholder="Nhập chi tiết ghi chú, giai đoạn, thời gian mắc..."
              value={freeTextNote}
              onChange={(e) => setFreeTextNote(e.target.value)}
              maxLength={500}
            />
          </div>
        </div>
      </Modal>
    </Card>
  )
}

export default ComorbiditiesSection
