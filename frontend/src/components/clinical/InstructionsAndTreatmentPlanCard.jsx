import React, { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  Row,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  DownOutlined,
  EditOutlined,
  FormOutlined,
  LockOutlined,
  SaveOutlined,
  UpOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import medicalRecordApi from '../../api/medicalRecordApi'
import {
  DOCTOR_INSTRUCTION_PRESETS,
  DANGER_SIGNS_WARNING,
  QUICK_REVISIT_OFFSETS,
  TREATMENT_PLAN_PRESETS,
  calculateRevisitDate,
  cleanInstructionsErrorMessage,
  formatRevisitDateDisplay,
  validateRevisitDate,
} from '../../utils/instructionsAndTreatmentPlanHelpers'

const { Text } = Typography
const { TextArea } = Input

/**
 * Thẻ ghi Lời dặn của bác sĩ và Kế hoạch điều trị (NCL-04-CN-010)
 * Tuân thủ quy định khóa sửa đổi khi bệnh án đã ký duyệt (QTN-18)
 * và ràng buộc ngày tái khám không trước ngày khám bệnh.
 */
export default function InstructionsAndTreatmentPlanCard({
  form,
  medicalRecordId,
  visitDate,
  isSigned = false,
  onOpenAmendModal,
  onSuccess,
}) {
  const [saving, setSaving] = useState(false)
  const [collapsed, setCollapsed] = useState(false)

  // Theo dõi giá trị revisitDate để render preview ngày tái khám
  const revisitDateValue = Form.useWatch('revisitDate', form)

  // Append preset vào Kế hoạch điều trị
  const handleAppendTreatmentPlan = (preset) => {
    if (isSigned) return
    const current = form.getFieldValue('treatmentPlan') || ''
    if (current.includes(preset)) return
    const updated = current.trim() ? `${current.trim()}\n${preset}` : preset
    form.setFieldsValue({ treatmentPlan: updated })
  }

  // Append preset vào Lời dặn bác sĩ
  const handleAppendDoctorInstruction = (preset) => {
    if (isSigned) return
    const current = form.getFieldValue('doctorInstructions') || ''
    if (current.includes(preset)) return
    const updated = current.trim() ? `${current.trim()}\n${preset}` : preset
    form.setFieldsValue({ doctorInstructions: updated })
  }

  // Chèn khối cảnh báo dấu hiệu nguy hiểm cần tái khám ngay
  const handleInsertDangerSigns = () => {
    if (isSigned) return
    const current = form.getFieldValue('doctorInstructions') || ''
    if (current.includes('CẢNH BÁO NGUY HIỂM')) {
      message.info('Đã có thông tin cảnh báo dấu hiệu nguy hiểm trong lời dặn.')
      return
    }
    const updated = current.trim()
      ? `${current.trim()}\n\n${DANGER_SIGNS_WARNING}`
      : DANGER_SIGNS_WARNING
    form.setFieldsValue({ doctorInstructions: updated })
    message.success('Đã thêm hướng dẫn nhận biết dấu hiệu nguy hiểm.')
  }

  // Chọn nhanh ngày tái khám qua các mốc (+3d, +7d, +14d, +30d)
  const handleQuickRevisitSelect = (days) => {
    if (isSigned) return
    const targetDate = calculateRevisitDate(visitDate, days)
    form.setFieldsValue({ revisitDate: targetDate })
  }

  // Chặn chọn ngày trước ngày khám (hoặc trước hôm nay)
  const disabledRevisitDate = (current) => {
    if (!current) return false
    const base = visitDate && dayjs(visitDate).isValid()
      ? dayjs(visitDate).startOf('day')
      : dayjs().startOf('day')
    return current.startOf('day').isBefore(base)
  }

  // Lưu trực tiếp lời dặn và kế hoạch điều trị qua API chuyên biệt
  const handleDirectSave = async () => {
    if (isSigned) {
      message.warning('Bệnh án đã được ký duyệt và khóa chỉnh sửa theo QTN-18.')
      return
    }

    if (!medicalRecordId) {
      message.warning('Vui lòng lưu thông tin khám bệnh trước khi lưu lời dặn riêng lẻ.')
      return
    }

    const values = form.getFieldsValue(['treatmentPlan', 'doctorInstructions', 'revisitDate'])

    // Kiểm tra tính hợp lệ của mốc tái khám
    const validation = validateRevisitDate(values.revisitDate, visitDate)
    if (!validation.valid) {
      message.error(validation.error)
      return
    }

    const revisitDateStr = values.revisitDate
      ? (typeof values.revisitDate === 'string'
          ? values.revisitDate
          : (values.revisitDate.format ? values.revisitDate.format('YYYY-MM-DD') : String(values.revisitDate)))
      : null

    const payload = {
      treatmentPlan: (values.treatmentPlan || '').trim(),
      doctorInstructions: (values.doctorInstructions || '').trim(),
      revisitDate: revisitDateStr,
    }

    setSaving(true)
    try {
      await medicalRecordApi.updateInstructionsAndTreatmentPlan(medicalRecordId, payload)
      message.success('Đã lưu lời dặn và kế hoạch điều trị thành công.')
      if (typeof onSuccess === 'function') {
        onSuccess(payload)
      }
    } catch (err) {
      const errorMsg = cleanInstructionsErrorMessage(err)
      message.error(errorMsg)
    } finally {
      setSaving(false)
    }
  }

  const revisitPreview = formatRevisitDateDisplay(revisitDateValue, visitDate)

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <span style={{ color: '#0284c7', display: 'inline-flex', alignItems: 'center', gap: 8, fontWeight: 700, fontSize: 15 }}>
            <FormOutlined style={{ fontSize: 17 }} /> Kế hoạch điều trị & Lời dặn của bác sĩ
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {isSigned && (
              <Tag color="gold" icon={<LockOutlined />} style={{ padding: '2px 8px', fontSize: 12, borderRadius: 4 }}>
                Đã ký duyệt (Khóa QTN-18)
              </Tag>
            )}
            <Button
              type="text"
              size="small"
              icon={collapsed ? <DownOutlined /> : <UpOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              title={collapsed ? 'Mở rộng' : 'Thu gọn'}
            />
          </div>
        </div>
      }
      bordered
      style={{
        marginTop: 16,
        borderRadius: 8,
        boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
      }}
      bodyStyle={{
        padding: collapsed ? 0 : '12px 16px',
        display: collapsed ? 'none' : 'block',
      }}
      extra={
        !isSigned && (
          <Button
            type="primary"
            icon={<SaveOutlined style={{ fontSize: 15 }} />}
            loading={saving}
            disabled={!medicalRecordId}
            onClick={handleDirectSave}
            size="small"
            style={{
              background: '#0284c7',
              borderColor: '#0284c7',
              borderRadius: 6,
              height: 32,
              padding: '0 14px',
              fontSize: 13,
              fontWeight: 600,
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            Lưu lời dặn & kế hoạch
          </Button>
        )
      }
    >
      {/* Cảnh báo khóa chỉnh sửa khi hồ sơ đã ký theo QTN-18 */}
      {isSigned && (
        <Alert
          type="warning"
          showIcon
          icon={<LockOutlined style={{ fontSize: 16, color: '#d97706' }} />}
          message="Hồ sơ bệnh án đã được ký duyệt (Khóa sửa đổi QTN-18)"
          description={
            <div style={{ fontSize: 12.5, lineHeight: 1.5 }}>
              <span>
                Toàn bộ nội dung chuyên môn, kế hoạch điều trị và lời dặn đã được niêm phong an toàn.
                Để điều chỉnh hoặc bổ sung theo quy định, vui lòng lập bản đính chính.
              </span>
              {typeof onOpenAmendModal === 'function' && (
                <div style={{ marginTop: 6 }}>
                  <Button
                    size="small"
                    type="primary"
                    danger
                    icon={<EditOutlined />}
                    onClick={onOpenAmendModal}
                    style={{ borderRadius: 4, height: 28, fontSize: 12 }}
                  >
                    Lập bản đính chính hồ sơ bệnh án
                  </Button>
                </div>
              )}
            </div>
          }
          style={{ marginBottom: 12, borderRadius: 6, padding: '8px 12px' }}
        />
      )}

      {/* 1 & 2: Kế hoạch điều trị & Lời dặn của bác sĩ trong Lưới 2 cột */}
      <Row gutter={[16, 12]}>
        <Col xs={24} md={12}>
          {/* 1. Kế hoạch điều trị (treatmentPlan) */}
          <div style={{ marginBottom: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6, flexWrap: 'wrap', gap: 6 }}>
              <Text strong style={{ color: '#1e293b', fontSize: 13 }}>
                1. Kế hoạch điều trị <span style={{ color: '#dc2626' }}>*</span>
              </Text>
              {!isSigned && (
                <Space size={[4, 4]} wrap align="center">
                  <Text type="secondary" style={{ fontSize: 11.5, fontWeight: 500 }}>
                    Mẫu gợi ý:
                  </Text>
                  {TREATMENT_PLAN_PRESETS.map((preset, idx) => (
                    <Tooltip key={idx} title={preset}>
                      <Button
                        size="small"
                        style={{
                          borderRadius: 4,
                          height: 24,
                          padding: '0 8px',
                          fontSize: 11.5,
                          fontWeight: 500,
                          color: '#0891b2',
                          borderColor: '#a5f3fc',
                          background: '#ecfeff',
                        }}
                        onClick={() => handleAppendTreatmentPlan(preset)}
                      >
                        + Mẫu {idx + 1}
                      </Button>
                    </Tooltip>
                  ))}
                </Space>
              )}
            </div>
            <Form.Item
              name="treatmentPlan"
              style={{ marginBottom: 0 }}
            >
              <TextArea
                autoSize={{ minRows: 2, maxRows: 6 }}
                disabled={isSigned}
                placeholder="Nhập phác đồ, theo dõi diễn biến lâm sàng, chỉ định chuyên khoa hoặc hướng chuyển tuyến..."
                style={{ borderRadius: 6, fontSize: 13 }}
              />
            </Form.Item>
          </div>
        </Col>

        <Col xs={24} md={12}>
          {/* 2. Lời dặn của bác sĩ (doctorInstructions) */}
          <div style={{ marginBottom: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6, flexWrap: 'wrap', gap: 6 }}>
              <Text strong style={{ color: '#1e293b', fontSize: 13 }}>
                2. Lời dặn của bác sĩ
              </Text>
              {!isSigned && (
                <Space size={[4, 4]} wrap align="center">
                  <Button
                    size="small"
                    type="dashed"
                    danger
                    icon={<WarningOutlined style={{ fontSize: 12 }} />}
                    onClick={handleInsertDangerSigns}
                    style={{
                      borderRadius: 4,
                      height: 24,
                      padding: '0 8px',
                      fontSize: 11.5,
                      fontWeight: 600,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                      borderColor: '#fca5a5',
                      background: '#fef2f2',
                    }}
                  >
                    + Dấu hiệu nguy hiểm
                  </Button>
                  {DOCTOR_INSTRUCTION_PRESETS.map((preset, idx) => (
                    <Tooltip key={idx} title={preset}>
                      <Button
                        size="small"
                        style={{
                          borderRadius: 4,
                          height: 24,
                          padding: '0 8px',
                          fontSize: 11.5,
                          fontWeight: 500,
                          color: '#2563eb',
                          borderColor: '#bfdbfe',
                          background: '#eff6ff',
                        }}
                        onClick={() => handleAppendDoctorInstruction(preset)}
                      >
                        + Lời dặn {idx + 1}
                      </Button>
                    </Tooltip>
                  ))}
                </Space>
              )}
            </div>
            <Form.Item
              name="doctorInstructions"
              style={{ marginBottom: 0 }}
            >
              <TextArea
                autoSize={{ minRows: 2, maxRows: 6 }}
                disabled={isSigned}
                placeholder="Nhập chế độ ăn, tập luyện, nghỉ ngơi, lưu ý dùng thuốc hoặc các dấu hiệu báo động..."
                style={{ borderRadius: 6, fontSize: 13 }}
              />
            </Form.Item>
          </div>
        </Col>
      </Row>

      {/* 3. Mốc hẹn tái khám (revisitDate) - Thanh ngang tinh gọn */}
      <div style={{ marginTop: 8, padding: '8px 12px', background: '#f8fafc', borderRadius: 6, border: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <Text strong style={{ color: '#1e293b', fontSize: 13, display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <CalendarOutlined style={{ color: '#0284c7' }} />
            3. Hẹn ngày tái khám:
          </Text>
          <Form.Item
            name="revisitDate"
            style={{ marginBottom: 0, minWidth: 160 }}
          >
            <DatePicker
              size="small"
              format="DD/MM/YYYY"
              disabled={isSigned}
              disabledDate={disabledRevisitDate}
              placeholder="Chọn ngày tái khám"
              style={{ width: '100%', borderRadius: 6, height: 28, fontSize: 13 }}
            />
          </Form.Item>
          {revisitPreview && (
            <Tag color="geekblue" style={{ fontSize: 12, padding: '2px 8px', borderRadius: 4, margin: 0 }}>
              {revisitPreview}
            </Tag>
          )}
        </div>

        {!isSigned && (
          <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 6 }}>
            <Text type="secondary" style={{ fontSize: 12, fontWeight: 500, marginRight: 2 }}>
              Chọn nhanh:
            </Text>
            {QUICK_REVISIT_OFFSETS.map((offset) => (
              <Button
                key={offset.days}
                size="small"
                onClick={() => handleQuickRevisitSelect(offset.days)}
                style={{
                  borderRadius: 4,
                  height: 26,
                  padding: '0 8px',
                  fontSize: 12,
                  fontWeight: 500,
                  color: '#334155',
                  borderColor: '#cbd5e1',
                  background: '#ffffff',
                }}
              >
                {offset.label}
              </Button>
            ))}
            {revisitDateValue && (
              <Button
                size="small"
                type="text"
                danger
                onClick={() => form.setFieldsValue({ revisitDate: null })}
                style={{ height: 26, fontSize: 12, padding: '0 6px' }}
              >
                Xóa mốc
              </Button>
            )}
          </div>
        )}
      </div>
    </Card>
  )
}
