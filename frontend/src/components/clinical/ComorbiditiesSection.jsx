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
      width: 55,
      align: 'center',
      render: (_, __, index) => (
        <span style={{ fontWeight: 600, color: '#64748B' }}>
          {index + 1}
        </span>
      ),
    },
    {
      title: 'Mã ICD-10',
      dataIndex: 'code',
      key: 'code',
      width: 105,
      align: 'center',
      render: (code) =>
        code ? (
          <Tag color="purple" style={{ fontWeight: 700, fontSize: 12.5, padding: '2px 8px', borderRadius: 6, margin: 0 }}>
            {code}
          </Tag>
        ) : (
          <Tag color="default" style={{ fontStyle: 'italic', fontSize: 11.5 }}>
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
          <Text strong style={{ fontSize: 13.5, color: '#1E293B', display: 'block' }}>
            {fixMojibake(name || record.rawName || '')}
          </Text>
          {record.diseaseGroup && (
            <span style={{ fontSize: 11.5, color: '#64748B' }}>
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
      width: 140,
      align: 'center',
      render: (_, record) => {
        const groupName = record.diseaseGroup || getDiseaseGroupName(record.code, record.diseaseGroup)
        const meta = categoryMeta[record.category] || categoryMeta.GENERAL
        return (
          <Tag color={meta.color} style={{ margin: 0, fontSize: 11.5, borderRadius: 4 }}>
            {groupName || meta.label}
          </Tag>
        )
      },
    },
    {
      title: 'Ghi chú lâm sàng / Diễn giải',
      dataIndex: 'note',
      key: 'note',
      width: 260,
      render: (note, record, index) => {
        if (!canEdit) {
          return (
            <span style={{ color: note ? '#334155' : '#94A3B8', fontStyle: note ? 'normal' : 'italic' }}>
              {note || 'Không có ghi chú'}
            </span>
          )
        }

        return (
          <Input
            size="small"
            placeholder="Ghi chú lâm sàng (mức độ, biến chứng...)"
            defaultValue={note || ''}
            onBlur={(e) => onUpdateSecondaryNote(record.code || record.id || index, e.target.value)}
            onPressEnter={(e) => onUpdateSecondaryNote(record.code || record.id || index, e.target.value)}
            style={{ borderRadius: 6, fontSize: 12.5 }}
            maxLength={500}
            allowClear
          />
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 155,
      align: 'center',
      render: (_, record) => {
        if (!canEdit) {
          return <span style={{ color: '#94A3B8', fontSize: 12 }}>Chỉ xem</span>
        }

        return (
          <Space size={6}>
            <Tooltip title="Đổi mã này thành chẩn đoán chính (hoán đổi)">
              <Button
                size="small"
                type="text"
                icon={<SwapOutlined style={{ color: '#2563eb' }} />}
                onClick={() => onSwitchToPrimary(record)}
                style={{ color: '#2563eb', fontWeight: 500, fontSize: 12 }}
              >
                CĐ chính
              </Button>
            </Tooltip>

            <Tooltip title="Xóa bệnh mắc kèm này">
              <Button
                size="small"
                type="text"
                danger
                icon={<DeleteOutlined />}
                onClick={() => onRemoveSecondary(record.code || record.id || record.name)}
                style={{ fontWeight: 500 }}
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
        borderRadius: 10,
        border: '1px solid #E2E8F0',
        backgroundColor: '#FFFFFF',
        boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
        marginBottom: 16,
      }}
      bodyStyle={{ padding: '14px 16px' }}
    >
      {/* Header khu vực bệnh mắc kèm */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MedicineBoxOutlined style={{ color: '#7C3AED', fontSize: 17 }} />
          <Text strong style={{ fontSize: 14.5, color: '#0F172A' }}>
            Bệnh mắc kèm (Chẩn đoán kèm theo)
          </Text>
          <Badge
            count={secondaryIcds.length}
            overflowCount={99}
            style={{
              backgroundColor: secondaryIcds.length > 0 ? '#7C3AED' : '#94A3B8',
              fontWeight: 600,
            }}
          />
          <Tooltip title="Theo quy tắc QTN-22: Mỗi bệnh án bắt buộc phải có 1 chẩn đoán chính và có thể ghi thêm nhiều bệnh mắc kèm. Dữ liệu được lưu tách bạch để phục vụ phân tích mô hình bệnh tật.">
            <InfoCircleOutlined style={{ color: '#64748B', cursor: 'pointer', fontSize: 13 }} />
          </Tooltip>
        </div>

        {canEdit && (
          <Button
            size="small"
            icon={<PlusCircleOutlined />}
            onClick={() => setIsFreeTextModalOpen(true)}
            style={{ fontSize: 12, borderRadius: 6, fontWeight: 500 }}
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
          message="Quy tắc QTN-22: Vui lòng chọn mã bệnh chẩn đoán chính trước khi thêm bệnh mắc kèm."
          style={{ marginBottom: 12, fontSize: 12.5, borderRadius: 6 }}
        />
      )}

      {/* Thanh tìm kiếm & thêm bệnh kèm qua Autocomplete */}
      {canEdit && (
        <div style={{ marginBottom: 12 }}>
          <DiagnosisCatalogAutocomplete
            placeholder={
              primaryIcd
                ? '🔍 Tra cứu mã hoặc tên bệnh kèm theo (nhập mã ICD: I10, E11... hoặc tên bệnh có dấu/không dấu)'
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
            <Text type="secondary" style={{ fontSize: 11.5, marginRight: 2 }}>
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
                    fontSize: 11.5,
                    padding: '2px 8px',
                    borderRadius: 6,
                    opacity: isDisabled ? 0.6 : 1,
                    margin: '2px 0',
                    border: isAdded ? '1px solid #C084FC' : '1px solid #E2E8F0',
                  }}
                  onClick={() => {
                    if (!isDisabled) {
                      onAddSecondary(suggested)
                    }
                  }}
                >
                  <b>{suggested.code}</b>: {suggested.name.split('(')[0].trim()}
                  {isAdded && ' ✓'}
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
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <span style={{ color: '#94A3B8', fontSize: 12.5 }}>
              Chưa có bệnh mắc kèm nào. Hãy sử dụng thanh tìm kiếm hoặc gợi ý phía trên để thêm mã bệnh.
            </span>
          }
          style={{ margin: '14px 0' }}
        />
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
