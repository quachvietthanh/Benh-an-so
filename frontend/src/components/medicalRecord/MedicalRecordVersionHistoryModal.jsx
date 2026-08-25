import React, { useCallback, useEffect, useState } from 'react'
import {
  Modal,
  Timeline,
  Tag,
  Descriptions,
  Typography,
  Space,
  Button,
  Spin,
  Alert,
  Empty,
  Card,
  Divider,
  Badge,
  Tooltip,
} from 'antd'
import {
  HistoryOutlined,
  UserOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  EditOutlined,
  SolutionOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import medicalRecordApi from '../../api/medicalRecordApi'
import { normalizeVersionHistoryData, validateVersionHistoryQuery } from '../../utils/medicalRecordVersionHelpers'
import { getApiErrorMessage } from '../../utils/apiError'

const { Title, Text, Paragraph } = Typography

const MedicalRecordVersionHistoryModal = ({
  open,
  onClose,
  medicalRecordId,
  recordCode,
  patientName,
  patientCode,
}) => {
  const [loading, setLoading] = useState(false)
  const [historyData, setHistoryData] = useState(null)
  const [errorMsg, setErrorMsg] = useState(null)

  const fetchVersionHistory = useCallback(async () => {
    const { valid, recordId, error } = validateVersionHistoryQuery(medicalRecordId)
    if (!valid || !recordId) {
      if (open) setErrorMsg(error || 'Thiếu thông tin mã hồ sơ bệnh án.')
      return
    }

    setLoading(true)
    setErrorMsg(null)
    try {
      const response = await medicalRecordApi.getVersionHistory(recordId)
      const data = response?.data || response
      const normalized = normalizeVersionHistoryData(data)
      setHistoryData(normalized)
    } catch (err) {
      const status = err?.response?.status
      if (status === 403) {
        setErrorMsg('Bạn không có quyền truy cập lịch sử phiên bản của bệnh án này (Chỉ dành cho Quản trị viên và Quản lý phòng khám).')
      } else if (status === 404) {
        setErrorMsg('Không tìm thấy thông tin phiên bản cho hồ sơ bệnh án được yêu cầu.')
      } else {
        const msg = getApiErrorMessage(err, 'Không thể tải lịch sử phiên bản bệnh án.')
        setErrorMsg(msg)
      }
      setHistoryData(null)
    } finally {
      setLoading(false)
    }
  }, [medicalRecordId, open])

  useEffect(() => {
    if (open && medicalRecordId) {
      fetchVersionHistory()
    } else {
      setHistoryData(null)
      setErrorMsg(null)
    }
  }, [open, medicalRecordId, fetchVersionHistory])

  const renderClinicalSnapshot = (snapshot) => {
    if (!snapshot) return null

    const diagnosesList = Array.isArray(snapshot.diagnoses) ? snapshot.diagnoses : []

    return (
      <Card
        size="small"
        style={{
          marginTop: 12,
          backgroundColor: '#f8fafc',
          border: '1px solid #e2e8f0',
          borderRadius: 8,
        }}
      >
        <div style={{ marginBottom: 8, fontWeight: 600, color: '#1e293b', display: 'flex', alignItems: 'center', gap: 6 }}>
          <SolutionOutlined style={{ color: '#0284c7' }} />
          <span>Thông tin khám lâm sàng (Bản gốc ban đầu):</span>
        </div>

        <Descriptions size="small" column={1} bordered style={{ backgroundColor: '#ffffff' }}>
          {snapshot.chiefComplaint && (
            <Descriptions.Item label={<Text strong>Lý do đến khám</Text>}>
              {snapshot.chiefComplaint}
            </Descriptions.Item>
          )}
          {snapshot.symptoms && (
            <Descriptions.Item label={<Text strong>Triệu chứng lâm sàng</Text>}>
              {snapshot.symptoms}
            </Descriptions.Item>
          )}
          {snapshot.medicalHistory && (
            <Descriptions.Item label={<Text strong>Tiền sử bệnh</Text>}>
              {snapshot.medicalHistory}
            </Descriptions.Item>
          )}
          {snapshot.physicalExamination && (
            <Descriptions.Item label={<Text strong>Khám thực thể</Text>}>
              {snapshot.physicalExamination}
            </Descriptions.Item>
          )}
          {diagnosesList.length > 0 && (
            <Descriptions.Item label={<Text strong>Chẩn đoán bệnh (ICD-10)</Text>}>
              <Space direction="vertical" size={2}>
                {diagnosesList.map((diag, i) => (
                  <Tag key={i} color="blue" style={{ margin: '2px 0' }}>
                    {diag}
                  </Tag>
                ))}
              </Space>
            </Descriptions.Item>
          )}
          {snapshot.clinicalProgress && (
            <Descriptions.Item label={<Text strong>Diễn biến bệnh</Text>}>
              {snapshot.clinicalProgress}
            </Descriptions.Item>
          )}
          {snapshot.treatmentPlan && (
            <Descriptions.Item label={<Text strong>Hướng điều trị</Text>}>
              {snapshot.treatmentPlan}
            </Descriptions.Item>
          )}
          {snapshot.doctorInstructions && (
            <Descriptions.Item label={<Text strong>Lời dặn của bác sĩ</Text>}>
              {snapshot.doctorInstructions}
            </Descriptions.Item>
          )}
          {snapshot.conclusion && (
            <Descriptions.Item label={<Text strong>Kết luận</Text>}>
              <Text strong style={{ color: '#059669' }}>{snapshot.conclusion}</Text>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>
    )
  }

  const timelineItems = (historyData?.allVersions || []).map((ver) => {
    const isOriginal = ver.isOriginal
    return {
      dot: isOriginal ? (
        <CheckCircleOutlined style={{ fontSize: 18, color: '#10b981' }} />
      ) : (
        <EditOutlined style={{ fontSize: 18, color: '#6366f1' }} />
      ),
      children: (
        <div
          style={{
            marginBottom: 20,
            padding: 14,
            backgroundColor: isOriginal ? '#f0fdf4' : '#f5f3ff',
            border: isOriginal ? '1px solid #bbf7d0' : '1px solid #ddd6fe',
            borderRadius: 8,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <Space>
              <Tag
                color={isOriginal ? 'success' : 'purple'}
                style={{ fontWeight: 700, fontSize: 13, padding: '2px 8px' }}
              >
                Phiên bản v{ver.versionNumber} {isOriginal ? '(Bản gốc khởi tạo)' : `(Bản đính chính #${ver.versionNumber - 1})`}
              </Tag>
            </Space>
            <Space size="middle">
              <span style={{ fontSize: 13, color: '#475569' }}>
                <UserOutlined style={{ marginRight: 4, color: '#64748b' }} />
                <strong>Người thực hiện:</strong> <Text type="secondary">{ver.modifiedBy}</Text>
              </span>
              <span style={{ fontSize: 13, color: '#475569' }}>
                <ClockCircleOutlined style={{ marginRight: 4, color: '#64748b' }} />
                <strong>Thời điểm:</strong> <Text type="secondary">{ver.formattedModifiedAt}</Text>
              </span>
            </Space>
          </div>

          <Divider style={{ margin: '10px 0' }} />

          {!isOriginal && (
            <div style={{ marginBottom: 8 }}>
              <div style={{ marginBottom: 4 }}>
                <Text strong style={{ color: '#4338ca' }}>Lý do đính chính / thay đổi:</Text>
              </div>
              <div
                style={{
                  padding: '6px 12px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #e0e7ff',
                  borderRadius: 6,
                  color: '#3730a3',
                  fontWeight: 500,
                }}
              >
                {ver.reason}
              </div>
            </div>
          )}

          {!isOriginal && (
            <div>
              <div style={{ marginBottom: 4 }}>
                <Text strong style={{ color: '#1e293b' }}>Nội dung chi tiết đính chính / bổ sung:</Text>
              </div>
              <div
                style={{
                  padding: '8px 12px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: 6,
                  color: '#334155',
                  whiteSpace: 'pre-wrap',
                }}
              >
                {ver.content}
              </div>
            </div>
          )}

          {isOriginal && renderClinicalSnapshot(ver.snapshot)}
        </div>
      ),
    }
  })

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <HistoryOutlined style={{ color: '#4f46e5', fontSize: 20 }} />
          <div>
            <Title level={5} style={{ margin: 0, color: '#1e293b' }}>
              Lịch sử các phiên bản bệnh án
            </Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Dành cho Quản lý phòng khám đối soát và chứng minh tính minh bạch hồ sơ
            </Text>
          </div>
        </div>
      }
      footer={[
        <Button key="reload" icon={<ReloadOutlined />} onClick={fetchVersionHistory} loading={loading}>
          Làm mới
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      width={850}
      destroyOnClose
    >
      <div style={{ marginTop: 8 }}>
        <Card
          size="small"
          style={{
            marginBottom: 16,
            background: 'linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%)',
            border: '1px solid #cbd5e1',
          }}
        >
          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
            <Descriptions.Item label={<Text strong>Mã bệnh án</Text>}>
              <Tag color="cyan" style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                {recordCode || (medicalRecordId ? `BA-${String(medicalRecordId).substring(0, 8).toUpperCase()}` : '---')}
              </Tag>
            </Descriptions.Item>
            {patientName && (
              <Descriptions.Item label={<Text strong>Bệnh nhân</Text>}>
                <Text strong style={{ color: '#0f172a' }}>{patientName}</Text>
                {patientCode && <Tag color="blue" style={{ marginLeft: 6 }}>{patientCode}</Tag>}
              </Descriptions.Item>
            )}
            <Descriptions.Item label={<Text strong>Tổng số phiên bản</Text>}>
              {loading ? (
                <Spin size="small" />
              ) : (
                <Badge
                  count={historyData?.totalVersions || 0}
                  showZero
                  style={{ backgroundColor: (historyData?.totalVersions || 0) > 1 ? '#7c3aed' : '#10b981' }}
                />
              )}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {loading && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin size="large" tip="Đang tải lịch sử các phiên bản bệnh án..." />
          </div>
        )}

        {errorMsg && !loading && (
          <Alert
            type="error"
            showIcon
            icon={<ExclamationCircleOutlined />}
            message="Không thể truy vấn lịch sử phiên bản"
            description={errorMsg}
            style={{ marginBottom: 16 }}
          />
        )}

        {!loading && !errorMsg && historyData && (
          <div>
            {historyData.originalOnly ? (
              <Alert
                type="info"
                showIcon
                icon={<SafetyCertificateOutlined style={{ color: '#10b981' }} />}
                message="Hồ sơ bệnh án ở phiên bản gốc ban đầu"
                description="Bệnh án này chưa có bất kỳ bản đính chính nào kể từ thời điểm bác sĩ lập hồ sơ. Toàn bộ thông tin được giữ nguyên vẹn."
                style={{ marginBottom: 16, backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0' }}
              />
            ) : (
              <Alert
                type="warning"
                showIcon
                icon={<HistoryOutlined style={{ color: '#6366f1' }} />}
                message={`Hồ sơ đã qua ${historyData.amendments.length} lần đính chính / bổ sung`}
                description="Dưới đây là toàn bộ dòng thời gian ghi nhận các lần thay đổi, bao gồm người thực hiện, thời điểm và lý do đính chính rõ ràng."
                style={{ marginBottom: 16, backgroundColor: '#faf5ff', border: '1px solid #e9d5ff' }}
              />
            )}

            <div style={{ maxHeight: '55vh', overflowY: 'auto', paddingRight: 8 }}>
              <Timeline items={timelineItems} />
            </div>
          </div>
        )}

        {!loading && !errorMsg && !historyData && (
          <Empty description="Không có dữ liệu lịch sử phiên bản cho hồ sơ này." />
        )}
      </div>
    </Modal>
  )
}

export default MedicalRecordVersionHistoryModal
