import React, { useMemo } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Row,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  HeartOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  SaveOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import {
  ABNORMAL_FLAGS_META,
  VITAL_SIGN_LIMITS,
  calculateBmi,
  evaluateAbnormalFlags,
  getBmiCategory,
} from '../../utils/vitalSignHelpers'

const { Text } = Typography

export function VitalSignCard({
  vitalSigns = {},
  onChange,
  onSave,
  onOpenHistory,
  saving = false,
  readOnly = false,
  backendFlags = [],
  patientId,
}) {
  // Tính BMI thời gian thực
  const currentBmi = useMemo(() => {
    return calculateBmi(vitalSigns.weight, vitalSigns.height)
  }, [vitalSigns.weight, vitalSigns.height])

  const bmiCategory = useMemo(() => {
    return getBmiCategory(currentBmi)
  }, [currentBmi])

  // Đánh giá cờ bất thường (kết hợp cờ từ Backend nếu có hoặc tự động đánh giá real-time)
  const activeFlags = useMemo(() => {
    if (Array.isArray(backendFlags) && backendFlags.length > 0) {
      return backendFlags
    }
    return evaluateAbnormalFlags({
      pulse: vitalSigns.pulse,
      bloodPressureSystolic: vitalSigns.bloodPressureSystolic,
      bloodPressureDiastolic: vitalSigns.bloodPressureDiastolic,
      temperature: vitalSigns.temperature,
      respiratoryRate: vitalSigns.respiratoryRate,
      spo2: vitalSigns.spo2,
      bmi: currentBmi,
    })
  }, [
    backendFlags,
    vitalSigns.pulse,
    vitalSigns.bloodPressureSystolic,
    vitalSigns.bloodPressureDiastolic,
    vitalSigns.temperature,
    vitalSigns.respiratoryRate,
    vitalSigns.spo2,
    currentBmi,
  ])

  const handleChange = (field, value) => {
    if (readOnly || !onChange) return
    onChange({
      ...vitalSigns,
      [field]: value,
    })
  }

  const isAbnormal = activeFlags.length > 0

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <Space align="center">
            <HeartOutlined style={{ color: '#e11d48', fontSize: 18 }} />
            <span style={{ color: '#0f172a', fontWeight: 700, fontSize: 15 }}>
              Chỉ Số Sinh Tồn
            </span>
            {readOnly && (
              <Tag color="default" style={{ fontSize: 11, margin: 0 }}>
                Chỉ đọc (Đã khóa)
              </Tag>
            )}
          </Space>
          <Space>
            {patientId && onOpenHistory && (
              <Button
                size="small"
                icon={<HistoryOutlined />}
                onClick={onOpenHistory}
                style={{ fontSize: 12 }}
              >
                Diễn tiến lịch sử
              </Button>
            )}
            {!readOnly && onSave && (
              <Button
                size="small"
                type="primary"
                ghost
                icon={<SaveOutlined />}
                loading={saving}
                onClick={() => onSave && onSave(vitalSigns)}
                style={{ fontSize: 12 }}
              >
                Lưu chỉ số
              </Button>
            )}
          </Space>
        </div>
      }
      bordered
      style={{
        borderRadius: 8,
        borderColor: isAbnormal ? '#fecaca' : '#e2e8f0',
        backgroundColor: '#ffffff',
        boxShadow: isAbnormal ? '0 0 0 1px #fee2e2' : undefined,
      }}
    >
      {/* Banner cảnh báo bất thường nếu phát hiện (TC-03) */}
      {isAbnormal && (
        <Alert
          type="error"
          showIcon
          icon={<WarningOutlined style={{ color: '#dc2626' }} />}
          message={
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
              <Text strong style={{ color: '#b91c1c', fontSize: 13 }}>
                Phát hiện chỉ số sinh tồn vượt ngưỡng tham chiếu:
              </Text>
              {activeFlags.map((flag) => {
                const meta = ABNORMAL_FLAGS_META[flag] || { label: flag, color: 'red' }
                return (
                  <Tooltip key={flag} title={meta.description}>
                    <Tag
                      color={meta.color}
                      style={{
                        fontWeight: 700,
                        fontSize: 11.5,
                        margin: 0,
                        border: 'none',
                        borderRadius: 4,
                      }}
                    >
                      {meta.label}
                    </Tag>
                  </Tooltip>
                )
              })}
            </div>
          }
          style={{ marginBottom: 14, borderRadius: 6, backgroundColor: '#fef2f2', borderColor: '#fca5a5' }}
        />
      )}

      <Row gutter={[12, 12]}>
        {/* Huyết áp tâm thu & tâm trương - Trải đều toàn bộ hàng ngang (Col 24) */}
        <Col xs={24}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155' }}>
              Huyết áp (Tâm thu / Tâm trương):
            </Text>
            <Text type="secondary" style={{ fontSize: 11 }}>
              Đơn vị: mmHg
            </Text>
          </div>
          <Space.Compact style={{ width: '100%' }}>
            <InputNumber
              style={{ width: '50%' }}
              placeholder="120 (Tâm thu)"
              min={VITAL_SIGN_LIMITS.BP_SYSTOLIC.min}
              max={VITAL_SIGN_LIMITS.BP_SYSTOLIC.max}
              value={vitalSigns.bloodPressureSystolic}
              onChange={(val) => handleChange('bloodPressureSystolic', val)}
              disabled={readOnly}
              addonAfter="mmHg"
            />
            <InputNumber
              style={{ width: '50%' }}
              placeholder="80 (Tâm trương)"
              min={VITAL_SIGN_LIMITS.BP_DIASTOLIC.min}
              max={VITAL_SIGN_LIMITS.BP_DIASTOLIC.max}
              value={vitalSigns.bloodPressureDiastolic}
              onChange={(val) => handleChange('bloodPressureDiastolic', val)}
              disabled={readOnly}
              addonAfter="mmHg"
            />
          </Space.Compact>
        </Col>

        {/* Nhịp mạch & Nhiệt độ - Đối xứng đều 50% - 50% */}
        <Col xs={12}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155', whiteSpace: 'nowrap' }}>
              Nhịp mạch:
            </Text>
          </div>
          <InputNumber
            style={{ width: '100%' }}
            placeholder="75"
            min={VITAL_SIGN_LIMITS.PULSE.min}
            max={VITAL_SIGN_LIMITS.PULSE.max}
            value={vitalSigns.pulse}
            onChange={(val) => handleChange('pulse', val)}
            disabled={readOnly}
            addonAfter="l/p"
          />
        </Col>

        <Col xs={12}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155', whiteSpace: 'nowrap' }}>
              Nhiệt độ:
            </Text>
          </div>
          <InputNumber
            style={{ width: '100%' }}
            placeholder="37.0"
            step={0.1}
            min={VITAL_SIGN_LIMITS.TEMPERATURE.min}
            max={VITAL_SIGN_LIMITS.TEMPERATURE.max}
            value={vitalSigns.temperature}
            onChange={(val) => handleChange('temperature', val)}
            disabled={readOnly}
            addonAfter="°C"
          />
        </Col>

        {/* Nhịp thở & Oxy máu (SpO2) - Đối xứng đều 50% - 50% */}
        <Col xs={12}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155', whiteSpace: 'nowrap' }}>
              Nhịp thở:
            </Text>
          </div>
          <InputNumber
            style={{ width: '100%' }}
            placeholder="16"
            min={VITAL_SIGN_LIMITS.RESPIRATORY_RATE.min}
            max={VITAL_SIGN_LIMITS.RESPIRATORY_RATE.max}
            value={vitalSigns.respiratoryRate}
            onChange={(val) => handleChange('respiratoryRate', val)}
            disabled={readOnly}
            addonAfter="l/p"
          />
        </Col>

        <Col xs={12}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155', whiteSpace: 'nowrap' }}>
              Oxy máu (SpO2):
            </Text>
          </div>
          <InputNumber
            style={{ width: '100%' }}
            placeholder="98"
            min={VITAL_SIGN_LIMITS.SPO2.min}
            max={VITAL_SIGN_LIMITS.SPO2.max}
            value={vitalSigns.spo2}
            onChange={(val) => handleChange('spo2', val)}
            disabled={readOnly}
            addonAfter="%"
          />
        </Col>

        {/* Cân nặng & Chiều cao - Đối xứng đều 50% - 50% */}
        <Col xs={12}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155', whiteSpace: 'nowrap' }}>
              Cân nặng:
            </Text>
          </div>
          <InputNumber
            style={{ width: '100%' }}
            placeholder="60.0"
            step={0.5}
            min={VITAL_SIGN_LIMITS.WEIGHT.min}
            max={VITAL_SIGN_LIMITS.WEIGHT.max}
            value={vitalSigns.weight}
            onChange={(val) => handleChange('weight', val)}
            disabled={readOnly}
            addonAfter="kg"
          />
        </Col>

        <Col xs={12}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Text strong style={{ fontSize: 13, color: '#334155', whiteSpace: 'nowrap' }}>
              Chiều cao:
            </Text>
          </div>
          <InputNumber
            style={{ width: '100%' }}
            placeholder="165.0"
            step={0.5}
            min={VITAL_SIGN_LIMITS.HEIGHT.min}
            max={VITAL_SIGN_LIMITS.HEIGHT.max}
            value={vitalSigns.height}
            onChange={(val) => handleChange('height', val)}
            disabled={readOnly}
            addonAfter="cm"
          />
        </Col>

        {/* Thẻ hiển thị BMI tự động */}
        <Col xs={24}>
          <div
            style={{
              marginTop: 4,
              padding: '10px 14px',
              borderRadius: 6,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              backgroundColor: currentBmi ? '#f8fafc' : '#f8fafc',
              border: currentBmi ? '1px solid #cbd5e1' : '1px dashed #cbd5e1',
            }}
          >
            <Space size={6} wrap>
              <Text strong style={{ color: '#1e293b', fontSize: 13 }}>
                Chỉ số thể trạng (BMI):
              </Text>
              {currentBmi ? (
                <Text strong style={{ color: '#0284c7', fontSize: 14 }}>
                  {currentBmi} <span style={{ fontSize: 12, fontWeight: 'normal', color: '#64748b' }}>kg/m²</span>
                </Text>
              ) : (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  (Nhập đủ chiều cao và cân nặng để tự động tính)
                </Text>
              )}
            </Space>
            {bmiCategory && (
              <Tag
                color={bmiCategory.tone}
                style={{
                  fontWeight: 700,
                  fontSize: 12,
                  margin: 0,
                  padding: '2px 10px',
                  borderRadius: 4,
                }}
              >
                {bmiCategory.label}
              </Tag>
            )}
          </div>
        </Col>

        {/* Ghi chú sinh hiệu */}
        <Col xs={24}>
          <div
            style={{
              height: 22,
              marginBottom: 4,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <Text style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>
              Ghi chú sinh hiệu (tùy chọn):
            </Text>
            {vitalSigns.note && (
              <Text type="secondary" style={{ fontSize: 11 }}>
                {vitalSigns.note.length}/{VITAL_SIGN_LIMITS.NOTE_MAX_LENGTH}
              </Text>
            )}
          </div>
          <Input
            placeholder="Ghi chú lâm sàng nếu có (ví dụ: đo khi bệnh nhân vừa nghỉ ngơi 5 phút...)"
            maxLength={VITAL_SIGN_LIMITS.NOTE_MAX_LENGTH}
            value={vitalSigns.note}
            onChange={(e) => handleChange('note', e.target.value)}
            disabled={readOnly}
            style={{ borderRadius: 6 }}
          />
        </Col>
      </Row>
    </Card>
  )
}

export default VitalSignCard
