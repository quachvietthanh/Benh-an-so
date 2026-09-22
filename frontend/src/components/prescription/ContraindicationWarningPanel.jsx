import React from 'react'
import { Alert, Button, Space, Tag, Typography } from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  HeartOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  StopOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import {
  areAllContraindicationsHandled,
  CONTRAINDICATION_SEVERITY_META,
  CONTRAINDICATION_TYPE_META,
  getUnhandledContraindications,
} from '../../utils/contraindicationValidation'

const { Text, Paragraph } = Typography

function getSeverityBadge(severity) {
  const meta = CONTRAINDICATION_SEVERITY_META[severity] || {
    label: severity,
    color: '#dc2626',
    tagColor: 'red',
  }
  return (
    <Tag color={meta.tagColor} style={{ fontWeight: 700, margin: 0 }}>
      {meta.label}
    </Tag>
  )
}

function getTypeBadge(type) {
  const meta = CONTRAINDICATION_TYPE_META[type] || {
    label: type,
    tagColor: 'default',
  }
  return (
    <Tag color={meta.tagColor} style={{ fontWeight: 600, margin: 0 }}>
      {meta.label}
    </Tag>
  )
}

export default function ContraindicationWarningPanel({
  warnings = [],
  missingData = [],
  overrides = [],
  onOpenOverrideModal,
  onOpenQuickUpdatePregnancy,
  editingPrescription = false,
}) {
  const allHandled = areAllContraindicationsHandled(warnings, overrides)
  const unhandledList = getUnhandledContraindications(warnings, overrides)

  return (
    <div style={{ marginTop: 16 }}>
      {/* 1. Khối cảnh báo Dữ liệu còn thiếu (Màu trung tính) */}
      {missingData && missingData.length > 0 && (
        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined style={{ color: '#475569', fontSize: 18 }} />}
          style={{
            marginBottom: 12,
            background: '#f8fafc',
            borderColor: '#cbd5e1',
            borderRadius: 8,
          }}
          message={
            <span style={{ color: '#334155', fontWeight: 600, fontSize: 14 }}>
              CẦN BỔ SUNG DỮ LIỆU ĐỂ KIỂM TRA CHỐNG CHỈ ĐỊNH ({missingData.length} trường hợp)
            </span>
          }
          description={
            <div style={{ marginTop: 4 }}>
              <Paragraph style={{ color: '#475569', marginBottom: 8, fontSize: 13 }}>
                Hệ thống chưa đủ thông tin bệnh nhân để đối chiếu an toàn cho một số thuốc trong đơn.
                Vui lòng bổ sung để tránh kê thuốc không an toàn:
              </Paragraph>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 8 }}>
                {missingData.map((item, idx) => (
                  <div
                    key={idx}
                    style={{
                      background: '#ffffff',
                      border: '1px solid #e2e8f0',
                      borderRadius: 6,
                      padding: '8px 12px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 8,
                    }}
                  >
                    <div>
                      <Space size="small" wrap style={{ marginBottom: 2 }}>
                        <Text strong style={{ color: '#1e293b' }}>
                          {item.medicineName || 'Thuốc'}
                        </Text>
                        {getTypeBadge(item.type)}
                      </Space>
                      <div style={{ color: '#64748b', fontSize: 12.5 }}>
                        {item.message}
                      </div>
                    </div>

                    {item.type === 'PREGNANCY' && onOpenQuickUpdatePregnancy && (
                      <Button
                        size="small"
                        icon={<HeartOutlined />}
                        onClick={onOpenQuickUpdatePregnancy}
                        style={{ borderColor: '#f472b6', color: '#be185d' }}
                      >
                        Bổ sung thai kỳ
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          }
        />
      )}

      {/* 2. Khối cảnh báo Chống chỉ định lâm sàng */}
      {warnings && warnings.length > 0 && (
        <div>
          {allHandled ? (
            <Alert
              type="warning"
              showIcon
              icon={<CheckCircleOutlined style={{ color: '#16a34a', fontSize: 18 }} />}
              style={{
                borderRadius: 8,
                background: '#fefce8',
                borderColor: '#fef08a',
              }}
              message={
                <span style={{ color: '#854d0e', fontWeight: 700, fontSize: 14 }}>
                  Đã ghi nhận lý do chuyên môn bỏ qua cho toàn bộ {warnings.length} cảnh báo chống chỉ định
                </span>
              }
              description={
                <div style={{ marginTop: 8 }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 10 }}>
                    {warnings.map((w, idx) => {
                      const ov = (overrides || []).find(
                        (o) =>
                          String(o.ruleId) === String(w.ruleId) &&
                          String(o.medicineId) === String(w.medicineId),
                      )
                      return (
                        <div
                          key={idx}
                          style={{
                            background: '#ffffff',
                            border: '1px solid #fef08a',
                            borderRadius: 6,
                            padding: '8px 12px',
                          }}
                        >
                          <Space size="small" wrap style={{ marginBottom: 4 }}>
                            <Text strong style={{ color: '#0f172a' }}>
                              {w.medicineName || 'Thuốc'}
                            </Text>
                            {getSeverityBadge(w.severity)}
                            {getTypeBadge(w.type)}
                          </Space>
                          <div style={{ fontSize: 13, color: '#334155', marginBottom: 2 }}>
                            {w.message}
                          </div>
                          {w.recommendation && (
                            <div style={{ fontSize: 12, color: '#0284c7', fontStyle: 'italic', marginBottom: 4 }}>
                              💡 Khuyến cáo: {w.recommendation}
                            </div>
                          )}
                          <div style={{ fontSize: 12.5, color: '#166534', background: '#f0fdf4', padding: '4px 8px', borderRadius: 4, marginTop: 4 }}>
                            <strong>Lý do bỏ qua:</strong> "{ov?.overrideReason}"
                          </div>
                        </div>
                      )
                    })}
                  </div>
                  <Button size="small" onClick={onOpenOverrideModal}>
                    Xem / Thay đổi lý do bỏ qua chống chỉ định
                  </Button>
                </div>
              }
            />
          ) : (
            <Alert
              type="error"
              showIcon
              icon={<StopOutlined style={{ color: '#b91c1c', fontSize: 18 }} />}
              style={{
                borderRadius: 8,
                background: '#fff1f2',
                borderColor: '#fecdd3',
              }}
              message={
                <span style={{ color: '#991b1b', fontWeight: 700, fontSize: 14 }}>
                  CẢNH BÁO CHỐNG CHỈ ĐỊNH THUỐC: Phát hiện {warnings.length} trường hợp ({unhandledList.length} chưa xử lý)
                </span>
              }
              description={
                <div style={{ marginTop: 8 }}>
                  <Paragraph style={{ marginBottom: 8, color: '#991b1b', fontSize: 13 }}>
                    Nút "{editingPrescription ? 'Lưu điều chỉnh đơn thuốc' : 'Tạo đơn thuốc'}" bị khóa.
                    Bác sĩ phải điều chỉnh bỏ/đổi thuốc an toàn hoặc bấm "Xem cảnh báo & Nhập lý do bỏ qua" để tiếp tục kê đơn.
                  </Paragraph>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 10 }}>
                    {warnings.map((w, idx) => (
                      <div
                        key={idx}
                        style={{
                          background: '#ffffff',
                          border: '1px solid #fecdd3',
                          borderLeft: `4px solid ${
                            w.severity === 'CONTRAINDICATED'
                              ? '#991b1b'
                              : w.severity === 'SEVERE'
                              ? '#dc2626'
                              : w.severity === 'MODERATE'
                              ? '#d97706'
                              : '#0284c7'
                          }`,
                          borderRadius: 6,
                          padding: '8px 12px',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                          <Space size="small" wrap>
                            <Text strong style={{ fontSize: 13.5, color: '#0f172a' }}>
                              {w.medicineName || 'Thuốc'}
                            </Text>
                            {getSeverityBadge(w.severity)}
                            {getTypeBadge(w.type)}
                          </Space>
                        </div>

                        {/* Tách biệt rõ ràng 2 nội dung message và recommendation */}
                        <div style={{ fontSize: 13, color: '#1e293b', marginBottom: 4, lineHeight: 1.4 }}>
                          <strong>Thông điệp:</strong> {w.message}
                        </div>

                        {w.recommendation && (
                          <div style={{ fontSize: 12.5, color: '#0369a1', background: '#f0f9ff', padding: '4px 8px', borderRadius: 4, borderLeft: '3px solid #0ea5e9' }}>
                            <strong>Khuyến cáo lâm sàng:</strong> {w.recommendation}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>

                  <Button
                    size="small"
                    type="primary"
                    danger
                    onClick={onOpenOverrideModal}
                    style={{ fontWeight: 600 }}
                  >
                    Xem cảnh báo & Nhập lý do bỏ qua
                  </Button>
                </div>
              }
            />
          )}
        </div>
      )}
    </div>
  )
}
