import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Descriptions,
  Divider,
  Empty,
  Modal,
  Radio,
  Space,
  Spin,
  Steps,
  Tabs,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  EditOutlined,
  FileProtectOutlined,
  FileTextOutlined,
  HistoryOutlined,
  PlusOutlined,
  SafetyCertificateFilled,
  UserOutlined,
} from '@ant-design/icons'
import medicalRecordApi from '../../api/medicalRecordApi'
import { formatDateTime } from '../../utils/helpers'
import { fixMojibake } from '../../utils/serviceCatalogValidation'

const { Text, Paragraph, Title } = Typography

export default function MedicalRecordVersionHistoryModal({
  open,
  onClose,
  recordId,
  onOpenAmendModal,
  canAmend = false,
}) {
  const [loading, setLoading] = useState(false)
  const [versionData, setVersionData] = useState(null)
  const [selectedVersionIndex, setSelectedVersionIndex] = useState(0)

  const loadVersionHistory = useCallback(async () => {
    if (!recordId || !open) return
    setLoading(true)
    try {
      const response = await medicalRecordApi.getVersionHistory(recordId)
      setVersionData(response.data)
      // Default to the latest amendment version if available, otherwise original (0)
      const amendments = response.data?.amendmentVersions || []
      if (amendments.length > 0) {
        setSelectedVersionIndex(amendments.length) // latest version index
      } else {
        setSelectedVersionIndex(0)
      }
    } catch (err) {
      console.warn('Lỗi khi tải lịch sử phiên bản bệnh án (có thể chưa phân quyền đọc version history):', err)
      setVersionData(null)
    } finally {
      setLoading(false)
    }
  }, [recordId, open])

  useEffect(() => {
    loadVersionHistory()
  }, [loadVersionHistory])

  const originalVersion = versionData?.originalVersion
  const amendmentVersions = versionData?.amendmentVersions || []
  const allVersions = originalVersion
    ? [
        { ...originalVersion, isOriginal: true, versionNumber: 1 },
        ...amendmentVersions.map((item, idx) => ({ ...item, isOriginal: false, versionNumber: idx + 2 })),
      ]
    : []

  const currentVersion = allVersions[selectedVersionIndex] || allVersions[0]

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', paddingRight: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                background: '#e0f2fe',
                color: '#0284c7',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 18,
              }}
            >
              <HistoryOutlined />
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#1e293b' }}>
                Lịch Sử Phiên Bản & Đính Chính Bệnh Án
              </div>
              <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
                Tra cứu bản gốc ban đầu và các bản đính chính chuyên môn đã lập
              </div>
            </div>
          </div>
          {canAmend && (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => {
                onClose()
                if (onOpenAmendModal) onOpenAmendModal()
              }}
              style={{
                background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
                borderColor: '#d97706',
                fontWeight: 600,
              }}
            >
              Lập bản đính chính mới
            </Button>
          )}
        </div>
      }
      open={open}
      onCancel={onClose}
      width={850}
      footer={[
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
    >
      <Spin spinning={loading}>
        {allVersions.length === 0 ? (
          <div style={{ margin: '30px 16px' }}>
            <Alert
              type="info"
              showIcon
              icon={<SafetyCertificateFilled style={{ color: '#0284c7' }} />}
              message="Đã ghi nhận bản đính chính vào hồ sơ bệnh án"
              description={
                <div style={{ fontSize: 13, marginTop: 6, lineHeight: 1.6 }}>
                  <p style={{ margin: '0 0 6px' }}>
                    Các bản đính chính chuyên môn do bác sĩ lập đã được hệ thống lưu vết kiểm toán vĩnh viễn và gắn liền với hồ sơ gốc.
                  </p>
                  <p style={{ margin: 0, color: '#64748b' }}>
                    Theo quy định phân quyền hồ sơ bệnh án điện tử (<strong>NCL-11-CN-003</strong>), tính năng tra cứu chi tiết toàn bộ cây lịch sử kiểm toán các phiên bản được phân quyền cho <strong>Quản lý phòng khám (Manager)</strong> và <strong>Quản trị viên (Admin)</strong>.
                  </p>
                </div>
              }
              style={{ background: '#f0fdf4', borderColor: '#bbf7d0' }}
            />
          </div>
        ) : (
          <div>
            {/* Header Tổng quan phiên bản */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: 12,
                padding: '12px 16px',
                background: '#f8fafc',
                borderRadius: 8,
                border: '1px solid #e2e8f0',
                marginTop: 8,
                marginBottom: 16,
              }}
            >
              <Space size="middle" wrap>
                <div>
                  <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>Tổng số phiên bản</Text>
                  <Text strong style={{ fontSize: 14 }}>
                    {allVersions.length} phiên bản (1 bản gốc{amendmentVersions.length > 0 ? `, ${amendmentVersions.length} bản đính chính` : ''})
                  </Text>
                </div>
                <Divider type="vertical" style={{ height: 28 }} />
                <div>
                  <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>Ngày khởi tạo gốc</Text>
                  <Text strong style={{ fontSize: 14 }}>{formatDateTime(originalVersion?.createdAt)}</Text>
                </div>
              </Space>

              <Tag color={amendmentVersions.length > 0 ? 'orange' : 'green'} style={{ fontSize: 12.5, padding: '4px 10px', borderRadius: 6 }}>
                {amendmentVersions.length > 0
                  ? `ĐÃ CÓ ${amendmentVersions.length} BẢN ĐÍNH CHÍNH`
                  : 'BẢN GỐC CHƯA CÓ ĐÍNH CHÍNH'}
              </Tag>
            </div>

            {/* Bộ chọn Version (Tabs / Segmented) */}
            <div style={{ marginBottom: 16 }}>
              <Radio.Group
                value={selectedVersionIndex}
                onChange={(e) => setSelectedVersionIndex(e.target.value)}
                buttonStyle="solid"
                style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}
              >
                {allVersions.map((v, idx) => (
                  <Radio.Button
                    key={v.versionNumber}
                    value={idx}
                    style={{
                      borderRadius: 6,
                      fontWeight: 600,
                      height: 36,
                      display: 'flex',
                      alignItems: 'center',
                    }}
                  >
                    {v.isOriginal ? (
                      <span>
                        <SafetyCertificateFilled style={{ color: '#16a34a', marginRight: 4 }} />
                        Phiên bản 1 (Bản gốc)
                      </span>
                    ) : (
                      <span>
                        <EditOutlined style={{ color: '#d97706', marginRight: 4 }} />
                        Bản đính chính {v.versionNumber - 1} (V{v.versionNumber})
                      </span>
                    )}
                  </Radio.Button>
                ))}
              </Radio.Group>
            </div>

            {/* Chi tiết nội dung của phiên bản đang chọn */}
            {currentVersion && (
              <Card
                bordered
                style={{
                  borderRadius: 10,
                  borderColor: currentVersion.isOriginal ? '#bbf7d0' : '#fed7aa',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                }}
              >
                {/* Meta thông tin phiên bản */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: 8,
                    marginBottom: 16,
                    paddingBottom: 12,
                    borderBottom: '1px solid #f1f5f9',
                  }}
                >
                  <Space size="small">
                    <Tag
                      color={currentVersion.isOriginal ? 'success' : 'orange'}
                      style={{ fontSize: 13, fontWeight: 700, padding: '4px 10px' }}
                    >
                      {currentVersion.isOriginal
                        ? 'PHIÊN BẢN 1 (BẢN GỐC BAN ĐẦU)'
                        : `PHIÊN BẢN ${currentVersion.versionNumber} (BẢN ĐÍNH CHÍNH #${currentVersion.versionNumber - 1})`}
                    </Tag>
                  </Space>
                  <Space size="middle">
                    <Text type="secondary" style={{ fontSize: 12.5 }}>
                      <UserOutlined style={{ marginRight: 4 }} />
                      Người thực hiện: <Text strong style={{ color: '#1e293b' }}>{currentVersion.authorName || 'Bác sĩ'}</Text>
                    </Text>
                    <Text type="secondary" style={{ fontSize: 12.5 }}>
                      <ClockCircleOutlined style={{ marginRight: 4 }} />
                      Thời gian: <Text strong style={{ color: '#1e293b' }}>{formatDateTime(currentVersion.createdAt)}</Text>
                    </Text>
                  </Space>
                </div>

                {/* Nội dung bản gốc (Clinical Snapshot) */}
                {currentVersion.isOriginal && currentVersion.clinicalSnapshot && (
                  <div>
                    <Descriptions
                      bordered
                      size="small"
                      column={{ xs: 1, sm: 2 }}
                      style={{ borderRadius: 8, overflow: 'hidden' }}
                    >
                      <Descriptions.Item label="Lý do khám / Triệu chứng" span={2}>
                        {fixMojibake(currentVersion.clinicalSnapshot.symptoms || currentVersion.clinicalSnapshot.chiefComplaint) || '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="Tiền sử bệnh" span={2}>
                        {fixMojibake(currentVersion.clinicalSnapshot.medicalHistory) || 'Không ghi nhận'}
                      </Descriptions.Item>
                      <Descriptions.Item label="Khám thực thể / Lâm sàng" span={2}>
                        {fixMojibake(currentVersion.clinicalSnapshot.physicalExamination) || '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="Chẩn đoán ICD-10" span={2}>
                        {Array.isArray(currentVersion.clinicalSnapshot.diagnoses) && currentVersion.clinicalSnapshot.diagnoses.length > 0 ? (
                          <Space wrap size={[4, 6]}>
                            {currentVersion.clinicalSnapshot.diagnoses.map((diag, i) => (
                              <Tag key={i} color="blue" style={{ fontWeight: 600 }}>
                                {fixMojibake(diag)}
                              </Tag>
                            ))}
                          </Space>
                        ) : (
                          fixMojibake(currentVersion.clinicalSnapshot.conclusion) || 'Chưa ghi nhận'
                        )}
                      </Descriptions.Item>
                      <Descriptions.Item label="Kế hoạch điều trị & Lời dặn" span={2}>
                        {fixMojibake(currentVersion.clinicalSnapshot.treatmentPlan || currentVersion.clinicalSnapshot.doctorInstructions) || '—'}
                      </Descriptions.Item>
                      {currentVersion.clinicalSnapshot.clinicalProgress && (
                        <Descriptions.Item label="Diễn tiến lâm sàng" span={2}>
                          {fixMojibake(currentVersion.clinicalSnapshot.clinicalProgress)}
                        </Descriptions.Item>
                      )}
                    </Descriptions>
                  </div>
                )}

                {/* Nội dung bản đính chính */}
                {!currentVersion.isOriginal && (
                  <div>
                    <Alert
                      type="warning"
                      showIcon
                      icon={<EditOutlined style={{ fontSize: 18 }} />}
                      message={
                        <div style={{ fontSize: 13.5, fontWeight: 700, color: '#92400e' }}>
                          Lý do đính chính: {currentVersion.reason || 'Không có lý do cụ thể'}
                        </div>
                      }
                      style={{
                        marginBottom: 16,
                        borderRadius: 8,
                        background: '#fffbeb',
                        borderColor: '#fde68a',
                      }}
                    />

                    <div style={{ marginBottom: 8, fontWeight: 600, fontSize: 13.5, color: '#1e293b' }}>
                      Nội dung đính chính / Bổ sung chi tiết:
                    </div>
                    <div
                      style={{
                        background: '#ffffff',
                        border: '1px solid #cbd5e1',
                        borderRadius: 8,
                        padding: 16,
                        minHeight: 120,
                        whiteSpace: 'pre-wrap',
                        lineHeight: '1.7',
                        fontSize: 14,
                        color: '#0f172a',
                      }}
                    >
                      {currentVersion.amendmentContent || 'Không có nội dung đính chính.'}
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* Timeline lịch sử thu nhỏ ở dưới */}
            <div style={{ marginTop: 24, padding: '0 8px' }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginBottom: 12 }}>
                Dòng thời gian biến động hồ sơ bệnh án:
              </div>
              <Timeline
                items={allVersions.map((v, idx) => ({
                  color: v.isOriginal ? 'green' : 'orange',
                  dot: v.isOriginal ? <SafetyCertificateFilled style={{ fontSize: 16 }} /> : <EditOutlined style={{ fontSize: 16 }} />,
                  children: (
                    <div
                      style={{
                        cursor: 'pointer',
                        padding: '4px 8px',
                        borderRadius: 6,
                        background: selectedVersionIndex === idx ? '#f1f5f9' : 'transparent',
                      }}
                      onClick={() => setSelectedVersionIndex(idx)}
                    >
                      <Space>
                        <Text strong style={{ color: selectedVersionIndex === idx ? '#0284c7' : '#334155' }}>
                          {v.isOriginal ? 'Khởi tạo & Ký xác nhận bản gốc' : `Bản đính chính #${v.versionNumber - 1}`}
                        </Text>
                        <Tag color={v.isOriginal ? 'success' : 'orange'} style={{ fontSize: 11 }}>
                          V{v.versionNumber}
                        </Tag>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          ({formatDateTime(v.createdAt)} - bởi {v.authorName || 'Bác sĩ'})
                        </Text>
                      </Space>
                      {!v.isOriginal && v.reason && (
                        <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                          Lý do: {v.reason}
                        </div>
                      )}
                    </div>
                  ),
                }))}
              />
            </div>
          </div>
        )}
      </Spin>
    </Modal>
  )
}
