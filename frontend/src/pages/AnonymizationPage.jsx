import React from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Divider,
  Popconfirm,
  Row,
  Space,
  Spin,
  Switch,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  AuditOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuthContext } from '../context/AuthContext'
import { useAnonymization } from '../context/AnonymizationContext'

const { Title, Text, Paragraph } = Typography

function AnonymizationPage() {
  const { user } = useAuthContext()
  const {
    anonymizationEnabled,
    updatedAt,
    loading,
    saving,
    toggleAnonymization,
  } = useAnonymization()

  const userRoles = (user?.roles || []).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  const userPermissions = (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))

  const isAdmin = userRoles.includes('admin')
  const canUpdate = userPermissions.includes('SYSTEM_CONFIG_UPDATE') || isAdmin

  return (
    <div className="anonymization-page-wrapper">
      {/* 1. Header Block */}
      <div className="anon-header-block">
        <div className="anon-header-badge">
          <SafetyCertificateOutlined /> An toàn thông tin y tế
        </div>
        <Title level={2} className="anon-header-title">
          Chế độ ẩn danh dữ liệu bệnh nhân
        </Title>
      </div>

      {/* 2. Mini KPI Stats */}
      <Row gutter={[16, 16]} className="anon-stats-row">
        <Col xs={12} sm={6}>
          <div className="anon-stat-card">
            <div className="anon-stat-label">
              <EyeInvisibleOutlined style={{ color: anonymizationEnabled ? '#d97706' : '#2563eb' }} />
              Trạng thái
            </div>
            <div className="anon-stat-value">
              {loading ? (
                <Spin size="small" />
              ) : anonymizationEnabled ? (
                <span style={{ color: '#d97706' }}>Đang bật (demo)</span>
              ) : (
                <span style={{ color: '#16a34a' }}>Đang tắt (thật)</span>
              )}
            </div>
          </div>
        </Col>
        <Col xs={12} sm={6}>
          <div className="anon-stat-card">
            <div className="anon-stat-label">
              <UserOutlined style={{ color: '#2563eb' }} />
              Phạm vi làm mờ
            </div>
            <div className="anon-stat-value" style={{ color: '#1e293b' }}>
              3 trường dữ liệu
            </div>
          </div>
        </Col>
        <Col xs={12} sm={6}>
          <div className="anon-stat-card">
            <div className="anon-stat-label">
              <DatabaseOutlined style={{ color: '#059669' }} />
              Cơ sở dữ liệu
            </div>
            <div className="anon-stat-value" style={{ color: '#059669' }}>
              Bảo toàn 100%
            </div>
          </div>
        </Col>
        <Col xs={12} sm={6}>
          <div className="anon-stat-card">
            <div className="anon-stat-label">
              <ClockCircleOutlined style={{ color: '#6366f1' }} />
              Đồng bộ lần cuối
            </div>
            <div className="anon-stat-value" style={{ fontSize: 13, color: '#475569', fontWeight: 600 }}>
              {updatedAt ? dayjs(updatedAt).format('HH:mm DD/MM/YYYY') : 'Khởi tạo hệ thống'}
            </div>
          </div>
        </Col>
      </Row>

      {/* 3. Master Control Hero Hub */}
      <Card
        className={`anon-control-card ${anonymizationEnabled ? 'active-mode' : 'inactive-mode'}`}
        bordered={false}
      >
        <Row gutter={[24, 16]} align="middle" justify="space-between" wrap={false}>
          <Col flex="1" style={{ minWidth: 0 }}>
            <div className="anon-hero-status-box">
              <div className="anon-status-icon-bubble">
                {anonymizationEnabled ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <Space wrap align="center" style={{ marginBottom: 4 }}>
                  <Title level={4} className="anon-hero-title">
                    {anonymizationEnabled
                      ? 'Chế độ trình diễn đang kích hoạt'
                      : 'Chế độ tiêu chuẩn (hiển thị dữ liệu gốc)'}
                  </Title>
                  {anonymizationEnabled ? (
                    <Tag color="warning" icon={<EyeInvisibleOutlined />} style={{ fontWeight: 600 }}>
                      Chế độ demo
                    </Tag>
                  ) : (
                    <Tag color="success" icon={<CheckCircleOutlined />} style={{ fontWeight: 600 }}>
                      Dữ liệu thật
                    </Tag>
                  )}
                </Space>

                <div className="anon-hero-desc">
                  {anonymizationEnabled ? (
                    <span>
                      Dữ liệu định danh bệnh nhân (họ tên, số điện thoại, địa chỉ) đang được tự động thay thế bằng dữ liệu ẩn danh trên toàn hệ thống. Mọi thao tác lưu bệnh nhân đều được bảo vệ để không ghi đè dữ liệu giả vào cơ sở dữ liệu.
                    </span>
                  ) : (
                    <span>
                      Hệ thống đang hoạt động ở chế độ nghiệp vụ thực tế. Dữ liệu bệnh nhân được hiển thị đầy đủ và chính xác theo hồ sơ gốc đã lưu trong hệ thống.
                    </span>
                  )}
                </div>

                {!canUpdate && (
                  <Alert
                    type="warning"
                    showIcon
                    icon={<LockOutlined />}
                    message="Chế độ chỉ xem cấu hình"
                    description="Tài khoản của bạn có quyền xem (SYSTEM_CONFIG_READ) nhưng không có quyền thao tác đổi trạng thái (SYSTEM_CONFIG_UPDATE)."
                    style={{ borderRadius: 8, marginTop: 8 }}
                  />
                )}
              </div>
            </div>
          </Col>

          <Col flex="none" className="anon-switch-action-col">
            <Popconfirm
              title={
                anonymizationEnabled
                  ? 'Tắt chế độ ẩn danh dữ liệu bệnh nhân?'
                  : 'Bật chế độ ẩn danh dữ liệu bệnh nhân?'
              }
              description={
                anonymizationEnabled
                  ? 'Hệ thống sẽ chuyển về hiển thị dữ liệu gốc và tự động làm mới bộ nhớ tạm trên toàn bộ các màn hình.'
                  : 'Hệ thống sẽ làm mờ họ tên, số điện thoại, địa chỉ trên tất cả các màn hình để đảm bảo an toàn trình diễn.'
              }
              onConfirm={() => toggleAnonymization(!anonymizationEnabled)}
              okText="Xác nhận"
              cancelText="Hủy"
              disabled={!canUpdate || loading || saving}
            >
              <Button
                type={anonymizationEnabled ? 'default' : 'primary'}
                danger={anonymizationEnabled}
                size="middle"
                icon={anonymizationEnabled ? <EyeOutlined /> : <EyeInvisibleOutlined />}
                loading={saving}
                disabled={!canUpdate || loading}
                className={anonymizationEnabled ? 'anon-action-btn-danger' : 'anon-action-btn-primary'}
              >
                {anonymizationEnabled ? 'Tắt ẩn danh' : 'Bật ẩn danh'}
              </Button>
            </Popconfirm>
          </Col>
        </Row>
      </Card>

      {/* 4. Live Comparison Demo (Trước vs Sau) */}
      <Card
        className="anon-comparison-card"
        title={
          <div className="anon-preview-header">
            <Space>
              <InfoCircleOutlined style={{ color: '#2563eb' }} />
              <span style={{ fontWeight: 700, fontSize: 16 }}>Minh họa so sánh trực quan trước và sau khi ẩn danh</span>
            </Space>
            <Tag color="blue">Mẫu hồ sơ: BN2026-08892</Tag>
          </div>
        }
        bordered={false}
      >
        <Row gutter={[20, 20]}>
          <Col xs={24} md={12}>
            <div className="anon-preview-box raw-box">
              <div className="anon-preview-title" style={{ color: '#1e293b' }}>
                <Space>
                  <EyeOutlined style={{ color: '#2563eb' }} />
                  <span>Dữ liệu thực tế (khi tắt ẩn danh)</span>
                </Space>
                <Tag color="default">Gốc</Tag>
              </div>

              <div className="anon-field-row">
                <span className="anon-field-label">Họ và tên bệnh nhân:</span>
                <span className="anon-field-val">Nguyễn Thị Minh Anh</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Số điện thoại liên lạc:</span>
                <span className="anon-field-val">0912 845 678</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Địa chỉ thường trú:</span>
                <span className="anon-field-val">Số 45 Tràng Tiền, Hoàn Kiếm, Hà Nội</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Số CCCD / CMND:</span>
                <span className="anon-field-val">001198005432</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Mã số thẻ BHYT:</span>
                <span className="anon-field-val">DN4010120894567</span>
              </div>
            </div>
          </Col>

          <Col xs={24} md={12}>
            <div className="anon-preview-box masked-box">
              <div className="anon-preview-title" style={{ color: '#854d0e' }}>
                <Space>
                  <EyeInvisibleOutlined style={{ color: '#d97706' }} />
                  <span>Dữ liệu đã ẩn danh (khi bật demo)</span>
                </Space>
                <Tag color="warning">Đã bảo vệ</Tag>
              </div>

              <div className="anon-field-row">
                <span className="anon-field-label">Họ và tên bệnh nhân:</span>
                <span className="anon-badge-masked">Bệnh nhân #BN2026-08892</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Số điện thoại liên lạc:</span>
                <span className="anon-badge-masked">09******78</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Địa chỉ thường trú:</span>
                <span className="anon-badge-masked">[Địa chỉ đã ẩn danh]</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Số CCCD / CMND:</span>
                <span className="anon-badge-unmasked">001198005432 (giữ nguyên)</span>
              </div>
              <div className="anon-field-row">
                <span className="anon-field-label">Mã số thẻ BHYT:</span>
                <span className="anon-badge-unmasked">DN4010120894567 (giữ nguyên)</span>
              </div>
            </div>
          </Col>
        </Row>
      </Card>

      {/* 5. Three Security Architecture Pillars */}
      <Row gutter={[18, 18]}>
        <Col xs={24} md={8}>
          <Card className="anon-pillar-card" bordered={false}>
            <div className="anon-pillar-icon" style={{ background: '#ecfdf5', color: '#059669' }}>
              <DatabaseOutlined />
            </div>
            <div className="anon-pillar-title">Bảo toàn dữ liệu gốc</div>
            <div className="anon-pillar-body">
              Quá trình làm mờ dữ liệu chỉ diễn ra tại tầng phản hồi API (REST Mapper). Dữ liệu gốc trong cơ sở dữ liệu luôn được lưu giữ nguyên vẹn. Máy chủ có bộ lọc tự phát hiện và chặn việc ghi đè dữ liệu giả khi người dùng cập nhật hồ sơ bệnh nhân.
            </div>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="anon-pillar-card" bordered={false}>
            <div className="anon-pillar-icon" style={{ background: '#eff6ff', color: '#2563eb' }}>
              <ThunderboltOutlined />
            </div>
            <div className="anon-pillar-title">Xóa cache tự động</div>
            <div className="anon-pillar-body">
              Ngay khi chuyển đổi trạng thái bật/tắt, hệ thống tự động dọn sạch 8 bộ nhớ tạm client-side (hồ sơ bệnh nhân, hàng đợi khám, chi tiết bệnh án, đơn thuốc, hóa đơn, lịch hẹn, lệnh CLS) và phát sự kiện đồng bộ ngay lập tức mà không cần F5.
            </div>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="anon-pillar-card" bordered={false}>
            <div className="anon-pillar-icon" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
              <AuditOutlined />
            </div>
            <div className="anon-pillar-title">Kiểm toán truy cập y tế</div>
            <div className="anon-pillar-body">
              Mọi hành động bật hoặc tắt chế độ ẩn danh đều được hệ thống tự động ghi lại vào nhật ký kiểm toán (Audit Trail) với đầy đủ thông tin tài khoản thao tác, thời gian chính xác và trạng thái chuyển giao để đáp ứng các quy chuẩn an toàn dữ liệu y tế.
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default AnonymizationPage
