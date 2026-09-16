import React, { useEffect, useRef, useState } from 'react'
import {
  Button,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd'
import { MedicineBoxOutlined, PlusCircleOutlined } from '@ant-design/icons'
import diagnosisCatalogApi from '../../api/diagnosisCatalogApi.js'
import patientChronicDiseaseApi from '../../api/patientChronicDiseaseApi.js'
import {
  getCurrentYear,
  mapChronicDiseaseErrorMessage,
  MIN_YEAR_DETECTED,
  validateYearDetected,
} from '../../utils/chronicDiseaseHelpers.js'
import { fixMojibake } from '../../utils/serviceCatalogValidation.js'

const { Text, Paragraph } = Typography
const { TextArea } = Input

const isUuid = (val) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(val || ''))

export default function AddChronicDiseaseModal({
  open,
  onClose,
  patientId,
  patientName = '',
  visitId = null,
  onSuccess,
}) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [searching, setSearching] = useState(false)
  const [diagnosisOptions, setDiagnosisOptions] = useState([])
  const searchTimeoutRef = useRef(null)
  const currentYear = getCurrentYear()

  // Load default/popular diagnosis suggestions when modal opens
  const loadSuggestions = async () => {
    try {
      setSearching(true)
      const res = await diagnosisCatalogApi.getSuggestions()
      const data = res?.data || {}
      const list = [
        ...(Array.isArray(data.recent) ? data.recent : []),
        ...(Array.isArray(data.popular) ? data.popular : []),
      ]

      const seen = new Set()
      const filtered = list
        .filter((item) => {
          if (!item || !item.id || item.active === false) return false
          if (seen.has(item.id)) return false
          seen.add(item.id)
          return true
        })
        .map((item) => ({
          value: item.id,
          label: `${item.code} - ${fixMojibake(item.name || '')}`,
          code: item.code,
          name: fixMojibake(item.name || ''),
        }))

      if (filtered.length > 0) {
        setDiagnosisOptions(filtered)
      } else {
        // Fallback search empty
        const fallbackRes = await diagnosisCatalogApi.search({ search: '' })
        const rawList = Array.isArray(fallbackRes?.data)
          ? fallbackRes.data
          : Array.isArray(fallbackRes)
            ? fallbackRes
            : []
        const mapped = rawList
          .filter((item) => item && item.id && item.active !== false)
          .slice(0, 20)
          .map((item) => ({
            value: item.id,
            label: `${item.code} - ${fixMojibake(item.name || '')}`,
            code: item.code,
            name: fixMojibake(item.name || ''),
          }))
        setDiagnosisOptions(mapped)
      }
    } catch (err) {
      console.warn('Không thể tải gợi ý danh mục chẩn đoán:', err)
    } finally {
      setSearching(false)
    }
  }

  useEffect(() => {
    if (open) {
      console.log('[AddChronicDiseaseModal Mount] patientId:', patientId, 'visitId:', visitId)
      form.resetFields()
      loadSuggestions()
    }
    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current)
      }
    }
  }, [open, patientId, visitId])

  // Debounced search for diagnosis catalog
  const handleSearchDiagnosis = (searchText) => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current)
    }

    const keyword = (searchText || '').trim()
    if (!keyword) {
      loadSuggestions()
      return
    }

    searchTimeoutRef.current = setTimeout(async () => {
      try {
        setSearching(true)
        const res = await diagnosisCatalogApi.search({ search: keyword })
        const list = Array.isArray(res?.data)
          ? res.data
          : Array.isArray(res)
            ? res
            : []

        const mapped = list
          .filter((item) => item && item.id && item.active !== false)
          .map((item) => ({
            value: item.id,
            label: `${item.code} - ${fixMojibake(item.name || '')}`,
            code: item.code,
            name: fixMojibake(item.name || ''),
          }))

        setDiagnosisOptions(mapped)
      } catch (err) {
        console.warn('Lỗi tìm kiếm danh mục chẩn đoán:', err)
      } finally {
        setSearching(false)
      }
    }, 300)
  }

  const handleSubmit = async (values) => {
    if (!patientId) {
      message.error('Không xác định được mã bệnh nhân.')
      return
    }

    const payload = {
      diagnosisCatalogId: values.diagnosisCatalogId,
      ...(values.yearDetected !== undefined && values.yearDetected !== null
        ? { yearDetected: Number(values.yearDetected) }
        : {}),
      ...(values.notes?.trim() ? { notes: values.notes.trim() } : {}),
      ...(visitId && isUuid(visitId) ? { visitId } : {}),
    }

    setSubmitting(true)
    try {
      let createdRecord = null
      try {
        const response = await patientChronicDiseaseApi.add(patientId, payload)
        createdRecord = response?.data || response
      } catch (postErr) {
        // Fallback: Nếu visitId truyền lên không hợp lệ hoặc đã đóng (404 VISIT_NOT_FOUND)
        if (
          payload.visitId &&
          (postErr.response?.status === 404 &&
            postErr.response?.data?.code === 'VISIT_NOT_FOUND')
        ) {
          const { visitId: _omitted, ...fallbackPayload } = payload
          const retryRes = await patientChronicDiseaseApi.add(patientId, fallbackPayload)
          createdRecord = retryRes?.data || retryRes
        } else {
          throw postErr
        }
      }

      message.success('Đã ghi nhận tiền sử bệnh mạn tính thành công!')
      form.resetFields()
      if (onSuccess) {
        onSuccess(createdRecord)
      }
      onClose()
    } catch (err) {
      const errorMsg = mapChronicDiseaseErrorMessage(err)
      message.error(errorMsg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              fontSize: 18,
            }}
          >
            <MedicineBoxOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Ghi nhận Tiền sử Bệnh mạn tính
            </div>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              Bệnh nhân: <strong>{patientName || 'Hồ sơ bệnh nhân'}</strong>
              {visitId && isUuid(visitId) && (
                <span> • Gắn ngữ cảnh lượt khám hiện tại</span>
              )}
            </div>
          </div>
        </div>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      width={640}
      destroyOnClose
    >
      <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 16, fontSize: 13 }}>
        Ghi nhận tiền sử bệnh nền theo mã chẩn đoán ICD-10. Dữ liệu này sẽ được lưu cố định vào hồ sơ bệnh nhân để phục vụ cảnh báo tương tác thuốc và theo dõi bệnh mạn tính.
      </Paragraph>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          yearDetected: null,
          notes: '',
        }}
      >
        {/* Chọn mã bệnh ICD-10 */}
        <Form.Item
          name="diagnosisCatalogId"
          label={
            <span style={{ fontWeight: 600 }}>
              Mã bệnh mạn tính (ICD-10) <span style={{ color: '#dc2626' }}>*</span>
            </span>
          }
          rules={[
            {
              required: true,
              message: 'Vui lòng chọn mã bệnh chẩn đoán từ danh mục',
            },
          ]}
        >
          <Select
            showSearch
            placeholder="🔍 Nhập mã ICD (E11, I10...) hoặc tên bệnh (tiểu đường, huyết áp...)"
            filterOption={false}
            onSearch={handleSearchDiagnosis}
            loading={searching}
            notFoundContent={
              searching ? (
                <div style={{ padding: '8px 12px', textAlign: 'center' }}>
                  <Spin size="small" /> Đang tìm kiếm...
                </div>
              ) : (
                <div style={{ padding: '8px 12px', color: '#64748b', textAlign: 'center' }}>
                  Không tìm thấy mã bệnh phù hợp
                </div>
              )
            }
            options={diagnosisOptions.map((opt) => ({
              value: opt.value,
              label: (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%' }}>
                  <Tag color="blue" style={{ fontWeight: 700, margin: 0 }}>
                    {opt.code}
                  </Tag>
                  <span style={{ color: '#1f2937' }}>{opt.name}</span>
                </div>
              ),
              searchText: `${opt.code} ${opt.name}`,
            }))}
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Row gutter={[16, 0]}>
          <Col xs={24} sm={12}>
            {/* Năm phát hiện */}
            <Form.Item
              name="yearDetected"
              label={<span style={{ fontWeight: 600 }}>Năm phát hiện</span>}
              tooltip={`Tùy chọn. Nếu nhập, phải từ năm ${MIN_YEAR_DETECTED} đến năm ${currentYear}`}
              rules={[
                {
                  validator: (_, value) => {
                    const validation = validateYearDetected(value, currentYear)
                    if (!validation.valid) {
                      return Promise.reject(new Error(validation.error))
                    }
                    return Promise.resolve()
                  },
                },
              ]}
            >
              <InputNumber
                placeholder={`Ví dụ: 2018 (${MIN_YEAR_DETECTED} - ${currentYear})`}
                min={MIN_YEAR_DETECTED}
                max={currentYear}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </Col>
        </Row>

        {/* Ghi chú tình trạng hiện tại */}
        <Form.Item
          name="notes"
          label={<span style={{ fontWeight: 600 }}>Tình trạng hiện tại / Ghi chú lâm sàng</span>}
          tooltip="Mô tả tình trạng kiểm soát bệnh, phác đồ duy trì hoặc lưu ý chuyên môn (tối đa 500 ký tự)"
          rules={[{ max: 500, message: 'Ghi chú không được vượt quá 500 ký tự' }]}
        >
          <TextArea
            rows={3}
            maxLength={500}
            showCount
            placeholder="Ví dụ: Đang điều trị bằng thuốc duy trì, chỉ số đường huyết/huyết áp kiểm soát ổn định..."
          />
        </Form.Item>

        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            gap: 8,
            marginTop: 20,
            paddingTop: 12,
            borderTop: '1px solid #f1f5f9',
          }}
        >
          <Button onClick={onClose} disabled={submitting}>
            Hủy
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            loading={submitting}
            icon={<PlusCircleOutlined />}
            style={{ fontWeight: 600 }}
          >
            Lưu tiền sử bệnh mạn tính
          </Button>
        </div>
      </Form>
    </Modal>
  )
}
