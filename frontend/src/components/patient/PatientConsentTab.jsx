import React, { useCallback, useEffect, useState } from 'react'
import {
  Card,
  Typography,
  Tag,
  Button,
  Space,
  Timeline,
  Alert,
  Empty,
  Skeleton,
  Divider,
  Row,
  Col,
  Tooltip,
  Badge,
} from 'antd'
import {
  SafetyCertificateOutlined,
  SlidersOutlined,
  StopOutlined,
  DeleteOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  UserOutlined,
  FileProtectOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  BookOutlined,
} from '@ant-design/icons'

import patientConsentApi from '../../api/patientConsentApi'
import {
  CONSENT_SCOPES,
  getConsentStatusMeta,
  formatDateTime,
  QTN19_PLAIN_EXPLANATION,
} from '../../utils/patientConsentHelpers'
import NarrowConsentScopeModal from './NarrowConsentScopeModal'
import WithdrawConsentModal from './WithdrawConsentModal'
import DataErasureRequestModal from './DataErasureRequestModal'
import PersonalDataConsentModal from './PersonalDataConsentModal'
import './patientConsent.css'

const { Title, Text, Paragraph } = Typography

export default function PatientConsentTab({
  patient,
  canManage = true,
  onPatientUpdated,
}) {
  const [historyList, setHistoryList] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Các Modal
  const [narrowModalOpen, setNarrowModalOpen] = useState(false)
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false)
  const [erasureModalOpen, setErasureModalOpen] = useState(false)
  const [viewFullDocOpen, setViewFullDocOpen] = useState(false)

  // Tải lịch sử các phiên bản phiếu đồng ý
  const fetchConsentHistory = useCallback(async () => {
    if (!patient?.id) return
    setLoadingHistory(true)
    try {
      const res = await patientConsentApi.getConsentHistory(patient.id)
      const data = Array.isArray(res.data) ? res.data : []
      // Sắp xếp phiên bản mới nhất lên đầu
      const sorted = [...data].sort((a, b) => (b.versionNumber || 0) - (a.versionNumber || 0))
      setHistoryList(sorted)
    } catch (err) {
      console.error('Không thể tải lịch sử phiếu đồng ý:', err)
    } finally {
      setLoadingHistory(false)
    }
  }, [patient?.id])

  useEffect(() => {
    fetchConsentHistory()
  }, [fetchConsentHistory])

  // Phiên bản mới nhất / đang hiệu lực
  const latestVersion = historyList[0] || null

  // Danh sách các phạm vi đang hiệu lực hiện tại
  const currentScopes = React.useMemo(() => {
    if (patient?.consentWithdrawn) return []
    if (latestVersion?.scopes) return latestVersion.scopes
    if (patient?.nonMedicalUseRestricted) return ['TREATMENT']
    return ['TREATMENT', 'COMMUNICATION', 'RESEARCH']
  }, [patient, latestVersion])

  // Trạng thái phiếu hiện tại
  const currentStatusMeta = React.useMemo(() => {
    if (patient?.consentWithdrawn) {
      return getConsentStatusMeta('WITHDRAWN')
    }
    if (latestVersion?.status) {
      return getConsentStatusMeta(latestVersion.status)
    }
    if (patient?.nonMedicalUseRestricted) {
      return getConsentStatusMeta('PARTIALLY_WITHDRAWN')
    }
    return getConsentStatusMeta('AGREED')
  }, [patient, latestVersion])

  // Callback khi có thay đổi từ các modal
  const handleActionSuccess = () => {
    fetchConsentHistory()
    if (onPatientUpdated) {
      onPatientUpdated()
    }
  }

  return (
    <div className="patient-consent-container">
      {/* 1. KHỐI HIỂN THỊ PHẠM VI ĐỒNG Ý ĐANG HIỆU LỰC HIỆN TẠI */}
      <Card
        className="consent-current-card"
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <SafetyCertificateOutlined style={{ color: '#16a34a', fontSize: 20 }} />
            <span style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Phiếu đồng ý xử lý dữ liệu đang hiệu lực
            </span>
          </div>
        }
        extra={
          <Space>
            <Button
              size="small"
              icon={<ReloadOutlined />}
              onClick={fetchConsentHistory}
              loading={loadingHistory}
            >
              Làm mới
            </Button>
            <Button
              size="small"
              type="link"
              icon={<FileProtectOutlined />}
              onClick={() => setViewFullDocOpen(true)}
              style={{ fontWeight: 600, color: '#2563eb' }}
            >
              Xem văn bản mẫu v1.0
            </Button>
          </Space>
        }
      >
        <div className="consent-current-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
              <Tag
                color={currentStatusMeta.tagColor}
                style={{ fontSize: 13, padding: '4px 10px', borderRadius: 6, fontWeight: 700 }}
              >
                {currentStatusMeta.label}
              </Tag>
              <Text strong style={{ fontSize: 14, color: '#1e293b' }}>
                Mã văn bản: {patient?.consentVersion || 'v1.0'}
                {latestVersion?.versionNumber ? ` (Phiên bản ${latestVersion.versionNumber})` : ''}
              </Text>
            </div>
            <div style={{ marginTop: 8, fontSize: 13, color: '#475569' }}>
              <Space size={16} wrap>
                <span>
                  <ClockCircleOutlined style={{ marginRight: 5 }} />
                  Thời điểm ký/đồng ý gần nhất:{' '}
                  <strong>
                    {formatDateTime(latestVersion?.createdAt || patient?.consentAgreedAt)}
                  </strong>
                </span>
                <span>
                  <UserOutlined style={{ marginRight: 5 }} />
                  Người ký xác nhận:{' '}
                  <strong>
                    {latestVersion?.signerName || patient?.consentSignerName || patient?.fullName}
                  </strong>
                </span>
              </Space>
            </div>
            {patient?.consentWithdrawn && (
              <Alert
                type="error"
                showIcon
                style={{ marginTop: 12, borderRadius: 8 }}
                message={
                  <span>
                    <strong>Đã rút lại toàn bộ sự đồng ý</strong> vào lúc{' '}
                    {formatDateTime(patient.consentWithdrawnAt)}
                  </span>
                }
                description={`Lý do: ${patient.consentWithdrawnReason || 'Người bệnh yêu cầu rút lại'}`}
              />
            )}
          </div>

          {/* NHÓM NÚT THAO TÁC RÕ RÀNG */}
          {canManage && (
            <Space wrap size={10}>
              <Button
                type="primary"
                icon={<SlidersOutlined />}
                onClick={() => setNarrowModalOpen(true)}
                style={{ background: '#2563eb', borderRadius: 8, fontWeight: 600 }}
              >
                Thu hẹp phạm vi
              </Button>

              <Button
                danger
                icon={<StopOutlined />}
                onClick={() => setWithdrawModalOpen(true)}
                style={{ borderRadius: 8, fontWeight: 600 }}
              >
                Rút lại toàn bộ
              </Button>

              <Button
                danger
                ghost
                icon={<DeleteOutlined />}
                onClick={() => setErasureModalOpen(true)}
                style={{ borderRadius: 8, fontWeight: 600 }}
              >
                Tiếp nhận yêu cầu xóa dữ liệu
              </Button>
            </Space>
          )}
        </div>

        {/* DANH SÁCH CHI TIẾT CÁC PHẠM VI HIỆN TẠI */}
        <div style={{ marginTop: 20 }}>
          <Title level={5} style={{ margin: '0 0 12px', color: '#1e293b' }}>
            Chi tiết các phạm vi được xử lý dữ liệu hiện tại:
          </Title>

          <Row gutter={[16, 16]}>
            {Object.values(CONSENT_SCOPES).map((scope) => {
              const isIncluded = currentScopes.includes(scope.key)
              return (
                <Col xs={24} md={8} key={scope.key}>
                  <div
                    className={`consent-scope-card-item ${
                      isIncluded ? 'active' : 'withdrawn'
                    }`}
                    style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                      <Tag color={isIncluded ? scope.tagColor : 'default'} style={{ fontWeight: 600 }}>
                        {scope.shortName}
                      </Tag>
                      {scope.required && (
                        <Tag color="green" style={{ fontSize: 11, margin: 0 }}>
                          Bắt buộc
                        </Tag>
                      )}
                    </div>
                    <Text strong style={{ fontSize: 13.5, color: '#0f172a', marginBottom: 6 }}>
                      {scope.name}
                    </Text>
                    <Paragraph type="secondary" style={{ fontSize: 12, margin: 0, flex: 1, lineHeight: 1.5 }}>
                      {scope.description}
                    </Paragraph>
                    <div style={{ marginTop: 12, paddingTop: 8, borderTop: '1px solid #f1f5f9' }}>
                      {isIncluded ? (
                        <span style={{ fontSize: 12, color: '#15803d', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 5 }}>
                          <CheckCircleOutlined /> Đang được phép xử lý
                        </span>
                      ) : (
                        <span style={{ fontSize: 12, color: '#dc2626', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 5 }}>
                          <StopOutlined /> Đã dừng / Không áp dụng
                        </span>
                      )}
                    </div>
                  </div>
                </Col>
              )
            })}
          </Row>
        </div>
      </Card>

      {/* 2. KHỐI LỊCH SỬ CÁC PHIÊN BẢN PHIẾU ĐỒNG Ý (AC-02) */}
      <Card
        className="consent-current-card"
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <HistoryOutlined style={{ color: '#2563eb', fontSize: 20 }} />
            <span style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Lịch sử các phiên bản phiếu đồng ý ({historyList.length})
            </span>
          </div>
        }
      >
        {loadingHistory ? (
          <Skeleton active paragraph={{ rows: 4 }} />
        ) : historyList.length === 0 ? (
          <Empty
            description="Chưa có dữ liệu lịch sử phiên bản phiếu đồng ý."
            style={{ padding: '20px 0' }}
          />
        ) : (
          <div>
            <Alert
              type="info"
              showIcon
              message="Nguyên tắc bất biến phiên bản (Audit Trail)"
              description="Hệ thống lưu giữ đầy đủ tất cả các phiên bản phiếu đồng ý theo thứ tự thời gian. Mỗi lần điều chỉnh phạm vi hoặc rút lại sự đồng ý đều được sinh thành một phiên bản độc lập kèm dấu vết người thao tác."
              style={{ marginBottom: 20, borderRadius: 8 }}
            />

            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              {historyList.map((item, index) => {
                const statusMeta = getConsentStatusMeta(item.status)
                const isCurrent = index === 0

                return (
                  <div
                    key={item.id || item.versionNumber}
                    className={`consent-history-item ${isCurrent ? 'is-current' : ''}`}
                  >
                    <div className="consent-history-header">
                      <div className="consent-history-version-badge">
                        <span>Phiên bản {item.versionNumber || historyList.length - index}</span>
                        <Tag color="cyan">{item.versionCode || 'v1.0'}</Tag>
                        {isCurrent && (
                          <Tag color="green" style={{ fontWeight: 700 }}>
                            ĐANG HIỆU LỰC (HIỆN TẠI)
                          </Tag>
                        )}
                        <Tag color={statusMeta.tagColor} style={{ fontWeight: 600 }}>
                          {statusMeta.label}
                        </Tag>
                      </div>

                      <div style={{ fontSize: 12.5, color: '#64748b' }}>
                        Thời điểm: <strong>{formatDateTime(item.createdAt)}</strong>
                      </div>
                    </div>

                    <div style={{ margin: '8px 0', fontSize: 13, color: '#334155' }}>
                      <Text type="secondary">Phạm vi áp dụng tại phiên bản này: </Text>
                      {Array.isArray(item.scopes) && item.scopes.length > 0 ? (
                        <Space size={6} wrap style={{ marginLeft: 6 }}>
                          {item.scopes.map((s) => {
                            const sc = CONSENT_SCOPES[s] || { name: s, tagColor: 'blue' }
                            return (
                              <Tag key={s} color={sc.tagColor} style={{ margin: 0 }}>
                                {sc.shortName || sc.name}
                              </Tag>
                            )
                          })}
                        </Space>
                      ) : (
                        <span style={{ color: '#dc2626', fontStyle: 'italic', marginLeft: 6 }}>
                          Đã rút lại toàn bộ phạm vi
                        </span>
                      )}
                    </div>

                    {item.consentWithdrawnReason && (
                      <div style={{ fontSize: 12.5, color: '#b91c1c', marginTop: 4 }}>
                        Lý do ghi nhận: <em>"{item.consentWithdrawnReason}"</em>
                      </div>
                    )}

                    <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 8 }}>
                      Người ký: <strong>{item.signerName || patient?.fullName}</strong> • Mã bản ghi: {item.id}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </Card>

      {/* 3. KHỐI GIẢI THÍCH PHÁP LÝ & QUYỀN CỦA BỆNH NHÂN */}
      <div className="qtn19-explainer-card">
        <div className="qtn19-explainer-title">
          <BookOutlined /> {QTN19_PLAIN_EXPLANATION.title}
        </div>
        <div className="qtn19-explainer-body">
          <p style={{ margin: '0 0 10px' }}>
            {QTN19_PLAIN_EXPLANATION.summaryForPatient}
          </p>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Tag color="blue">Nghị định 13/2023/NĐ-CP</Tag>
            <Tag color="cyan">Luật Khám bệnh, chữa bệnh</Tag>
            <Tag color="purple">Bảo lưu hồ sơ y tế tối thiểu 10 năm</Tag>
            <Tag color="green">Bảo đảm quyền rút lại sự đồng ý</Tag>
          </div>
        </div>
      </div>

      {/* CÁC MODAL THAO TÁC */}
      <NarrowConsentScopeModal
        open={narrowModalOpen}
        onClose={() => setNarrowModalOpen(false)}
        patient={patient}
        currentScopes={currentScopes}
        onSuccess={handleActionSuccess}
      />

      <WithdrawConsentModal
        open={withdrawModalOpen}
        onClose={() => setWithdrawModalOpen(false)}
        patient={patient}
        currentScopes={currentScopes}
        onSuccess={handleActionSuccess}
      />

      <DataErasureRequestModal
        open={erasureModalOpen}
        onClose={() => setErasureModalOpen(false)}
        patient={patient}
        onSuccess={handleActionSuccess}
      />

      <PersonalDataConsentModal
        open={viewFullDocOpen}
        onClose={() => setViewFullDocOpen(false)}
        patientName={patient?.fullName}
        guardianName={patient?.guardianName}
        agreedAt={patient?.consentAgreedAt}
        version={patient?.consentVersion || 'v1.0'}
      />
    </div>
  )
}
