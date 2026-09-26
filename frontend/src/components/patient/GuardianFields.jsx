import React, { useEffect } from 'react'
import { Form, Input, Select, Row, Col, Alert, Tag, Space, Typography } from 'antd'
import { UserOutlined, PhoneOutlined, IdcardOutlined, TeamOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { isMinorPatient, GUARDIAN_RELATIONSHIP_PRESETS } from '../../utils/patientGuardianValidation'

const { Text } = Typography

/**
 * Khối trường nhập thông tin Người giám hộ (NCL-02-CN-008 / QTN-44)
 * Bắt buộc khi bệnh nhân dưới 18 tuổi.
 */
export default function GuardianFields({ form, layoutGrid = true, showAlways = false }) {
  const dateOfBirth = Form.useWatch('dateOfBirth', form)
  const isMinor = isMinorPatient(dateOfBirth)

  // Nếu bệnh nhân không phải là trẻ em và không bật showAlways thì có thể ẩn hoặc hiển thị tùy chọn
  const isRequired = isMinor

  useEffect(() => {
    // Nếu bệnh nhân đủ 18 tuổi và form chưa có người giám hộ thì không ép buộc
    if (!isMinor && !showAlways) {
      // Giữ nguyên dữ liệu nếu đã có
    }
  }, [isMinor, showAlways])

  return (
    <div
      style={{
        background: isMinor ? '#fff7ed' : '#f8fafc',
        border: `1px solid ${isMinor ? '#fdba74' : '#e2e8f0'}`,
        borderRadius: 8,
        padding: '16px 20px',
        marginBottom: 16,
        transition: 'all 0.25s ease',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
          marginBottom: 12,
        }}
      >
        <Space align="center" size={8}>
          <TeamOutlined style={{ fontSize: 18, color: isMinor ? '#ea580c' : '#475569' }} />
          <Text strong style={{ fontSize: 14, color: isMinor ? '#9a3412' : '#334155' }}>
            Thông tin Người giám hộ hợp pháp
          </Text>
          {isMinor ? (
            <Tag color="volcano" style={{ fontWeight: 600 }}>
              Dưới 18 tuổi - Bắt buộc
            </Tag>
          ) : (
            <Tag color="default">
              Dành cho bệnh nhân dưới 18 tuổi
            </Tag>
          )}
        </Space>
      </div>

      {isMinor && (
        <Alert
          type="warning"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message="Yêu cầu đối với bệnh nhân dưới 18 tuổi"
          description="Bệnh nhân chưa đủ 18 tuổi bắt buộc phải khai báo thông tin người giám hộ hợp pháp. Phiếu đồng ý xử lý dữ liệu cá nhân sẽ tự động đứng tên người giám hộ này."
          style={{ marginBottom: 14, background: '#fff', borderColor: '#fed7aa' }}
        />
      )}

      <Row gutter={[16, 12]}>
        <Col xs={24} sm={12}>
          <Form.Item
            name="guardianName"
            label="Họ và tên người giám hộ"
            rules={[
              {
                required: isRequired,
                message: 'Vui lòng nhập họ tên người giám hộ (bắt buộc cho bệnh nhân dưới 18 tuổi)!',
              },
              {
                max: 100,
                message: 'Họ tên người giám hộ không được vượt quá 100 ký tự!',
              },
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
              placeholder="VD: Nguyễn Văn Cha, Trần Thị Mẹ..."
            />
          </Form.Item>
        </Col>

        <Col xs={24} sm={12}>
          <Form.Item
            name="guardianRelationship"
            label="Mối quan hệ với bệnh nhân"
            rules={[
              {
                required: isRequired,
                message: 'Vui lòng chọn hoặc nhập mối quan hệ của người giám hộ!',
              },
              {
                max: 50,
                message: 'Mối quan hệ không được vượt quá 50 ký tự!',
              },
            ]}
          >
            <Select
              placeholder="Chọn hoặc nhập mối quan hệ"
              options={GUARDIAN_RELATIONSHIP_PRESETS.map((rel) => ({
                value: rel,
                label: rel,
              }))}
              showSearch
              allowClear
            />
          </Form.Item>
        </Col>

        <Col xs={24} sm={12}>
          <Form.Item
            name="guardianPhone"
            label="Số điện thoại người giám hộ"
            rules={[
              {
                required: isRequired,
                message: 'Vui lòng nhập số điện thoại người giám hộ!',
              },
              {
                pattern: /^0[35789]\d{8}$/,
                message: 'Số điện thoại phải gồm 10 số và bắt đầu bằng đầu số di động VN (03, 05, 07, 08, 09)!',
              },
            ]}
          >
            <Input
              prefix={<PhoneOutlined style={{ color: '#94a3b8' }} />}
              placeholder="VD: 0912345678"
            />
          </Form.Item>
        </Col>

        <Col xs={24} sm={12}>
          <Form.Item
            name="guardianIdentityNumber"
            label="Số CCCD/CMND người giám hộ"
            rules={[
              {
                max: 20,
                message: 'Số CCCD/CMND không được vượt quá 20 ký tự!',
              },
            ]}
          >
            <Input
              prefix={<IdcardOutlined style={{ color: '#94a3b8' }} />}
              placeholder="VD: 079090001234 (Tùy chọn)"
            />
          </Form.Item>
        </Col>
      </Row>
    </div>
  )
}
