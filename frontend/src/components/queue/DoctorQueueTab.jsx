import React from 'react'
import { Card, Row, Col, Select, Typography, List, Avatar, Button, Popconfirm } from 'antd'
import {
  MedicineBoxOutlined,
  StepForwardOutlined,
  ReloadOutlined,
} from '@ant-design/icons'

const { Text } = Typography

export const DoctorQueueTab = ({
  doctorQueueGroups = {},
  permissions = {},
  queueDoctorFilter,
  setQueueDoctorFilter,
  doctors = [],
  getPatientInfo = () => ({ name: 'Bệnh nhân', code: 'BN-001' }),
  getAvatarStyle = () => ({}),
  getInitials = () => 'BN',
  navigate,
  handleCallNext = () => {},
  handleRecallQueueItem = () => {},
  handleUpdateItemStatus = () => {},
  handleCompleteItem = () => {},
  skipForm,
  setSkipModalItem = () => {},
}) => {
  const inProgress = doctorQueueGroups?.inProgress || []
  const waiting = doctorQueueGroups?.waiting || []
  const waitingForResult = doctorQueueGroups?.waitingForResult || []
  const skipped = doctorQueueGroups?.skipped || []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {!permissions.isDoctorOnly && (
        <Card style={{ borderRadius: 12, backgroundColor: '#f8fafc', borderColor: '#e2e8f0' }} bodyStyle={{ padding: 12 }}>
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} sm={12} md={8}>
              <Text strong style={{ marginRight: 8 }}>Bác sĩ phụ trách (Hôm nay):</Text>
              <Select
                style={{ width: '220px' }}
                value={queueDoctorFilter || 'ALL'}
                onChange={setQueueDoctorFilter}
                options={[
                  { value: 'ALL', label: 'Tất cả Bác sĩ (Hôm nay)' },
                  ...doctors.map((d) => ({ value: d.id, label: d.fullName || d.username })),
                ]}
              />
            </Col>
            <Col xs={24} sm={12} md={16}>
              <Text type="secondary">Quyền Admin / Lễ tân: Bạn có thể xem toàn bộ bệnh nhân hoặc chọn từng bác sĩ để xem danh sách phụ trách riêng.</Text>
            </Col>
          </Row>
        </Card>
      )}

      {/* 🔴 BỆNH NHÂN ĐANG KHÁM */}
      <Card
        title={<Text strong style={{ color: '#16a34a' }}>🔴 BỆNH NHÂN ĐANG KHÁM ({inProgress.length})</Text>}
        style={{ borderRadius: 12, borderColor: '#bbf7d0' }}
      >
        {inProgress.length === 0 ? (
          <Text type="secondary">Chưa có bệnh nhân nào đang trong phòng khám.</Text>
        ) : (
          <List
            dataSource={inProgress}
            renderItem={(item) => {
              const pInfo = getPatientInfo(item.patientId, item.patientName)
              return (
                <List.Item
                  actions={[
                    (permissions.isAdmin || permissions.isDoctor || permissions.isNurse) && (
                      <Button
                        key="exam"
                        type="primary"
                        icon={<MedicineBoxOutlined />}
                        onClick={() => navigate('/medical-records', { state: { patientId: item.patientId, visitId: item.visitId, queueItemId: item.id } })}
                      >
                        Ghi bệnh án / Khám
                      </Button>
                    ),
                    permissions.canUpdateStatus && (
                      <Button
                        key="cdls"
                        style={{ borderColor: '#9333ea', color: '#9333ea' }}
                        onClick={() => handleUpdateItemStatus(item.id, 'WAITING_FOR_RESULT')}
                      >
                        Chờ CĐLS
                      </Button>
                    ),
                    permissions.canComplete && (
                      <Popconfirm
                        key="complete"
                        title="Xác nhận hoàn tất lượt khám?"
                        description="Đảm bảo bệnh án đã được Bác sĩ ký/khóa trước khi hoàn tất."
                        onConfirm={() => handleCompleteItem(item.id)}
                      >
                        <Button type="primary" style={{ backgroundColor: '#16a34a' }}>
                          Hoàn tất
                        </Button>
                      </Popconfirm>
                    ),
                    permissions.canSkip && (
                      <Button
                        key="skip"
                        danger
                        onClick={() => {
                          if (skipForm?.setFieldsValue) skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                          setSkipModalItem(item)
                        }}
                      >
                        Bỏ qua
                      </Button>
                    ),
                  ].filter(Boolean)}
                >
                  <List.Item.Meta
                    avatar={<Avatar size="large" style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>}
                    title={<Text strong>{pInfo.name} - <Text type="secondary">STT: {item.queueNumber}</Text></Text>}
                    description={`Mã lượt: ${item.visitCode || item.visitId || 'N/A'} | Nguồn: ${item.sourceType === 'WALK_IN' ? 'Walk-in' : 'Hẹn trước'}`}
                  />
                </List.Item>
              )
            }}
          />
        )}
      </Card>

      {/* 🟡 BỆNH NHÂN ĐANG CHỜ */}
      <Card
        title={<Text strong style={{ color: '#2563eb' }}>🟡 BỆNH NHÂN ĐANG CHỜ ({waiting.length})</Text>}
        style={{ borderRadius: 12 }}
      >
        <List
          dataSource={waiting}
          pagination={{ pageSize: 5 }}
          renderItem={(item) => {
            const pInfo = getPatientInfo(item.patientId, item.patientName)
            return (
              <List.Item
                actions={[
                  permissions.canCallNext && (
                    <Button
                      key="call-next"
                      type="primary"
                      icon={<StepForwardOutlined />}
                      onClick={() => handleCallNext(item.medicalQueueId || item.queueId)}
                    >
                      Gọi vào khám
                    </Button>
                  ),
                  permissions.canSkip && (
                    <Button
                      key="skip"
                      danger
                      onClick={() => {
                        if (skipForm?.setFieldsValue) skipForm.setFieldsValue({ reason: 'Vắng mặt khi gọi' })
                        setSkipModalItem(item)
                      }}
                    >
                      Bỏ qua
                    </Button>
                  ),
                ].filter(Boolean)}
              >
                <List.Item.Meta
                  avatar={<Avatar style={getAvatarStyle(pInfo.name)}>{getInitials(pInfo.name)}</Avatar>}
                  title={<Text strong>{pInfo.name} - STT: {item.queueNumber}</Text>}
                  description={`Đến lúc: ${item.checkedInAt ? new Date(item.checkedInAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : 'N/A'}`}
                />
              </List.Item>
            )
          }}
        />
      </Card>

      {/* 🟣 BỆNH NHÂN CHỜ CẤP CỨU / CĐLS & ⚪ VẮNG MẶT */}
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card
            title={<Text strong style={{ color: '#9333ea' }}>🟣 CHỜ KẾT QUẢ CĐLS ({waitingForResult.length})</Text>}
            style={{ borderRadius: 12 }}
          >
            <List
              dataSource={waitingForResult}
              pagination={{ pageSize: 5 }}
              renderItem={(item) => {
                const pInfo = getPatientInfo(item.patientId, item.patientName)
                return (
                  <List.Item
                    actions={[
                      permissions.canUpdateStatus && (
                        <Button
                          key="resume"
                          type="primary"
                          icon={<ReloadOutlined />}
                          onClick={() => handleRecallQueueItem(item)}
                        >
                          Gọi lại phòng khám
                        </Button>
                      ),
                    ]}
                  >
                    <List.Item.Meta
                      title={<Text strong>{pInfo.name}</Text>}
                      description={`STT: ${item.queueNumber} - Đã chuyển xét nghiệm / X-quang`}
                    />
                  </List.Item>
                )
              }}
            />
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card
            title={<Text strong style={{ color: '#64748b' }}>⚪ DANH SÁCH BỎ QUA / VẮNG MẶT ({skipped.length})</Text>}
            style={{ borderRadius: 12 }}
          >
            <List
              dataSource={skipped}
              pagination={{ pageSize: 5 }}
              renderItem={(item) => {
                const pInfo = getPatientInfo(item.patientId, item.patientName)
                return (
                  <List.Item
                    actions={[
                      permissions.canUpdateStatus && (
                        <Button
                          key="recall"
                          type="dashed"
                          icon={<ReloadOutlined />}
                          onClick={() => handleRecallQueueItem(item)}
                        >
                          Gọi khám lại
                        </Button>
                      ),
                    ]}
                  >
                    <List.Item.Meta
                      title={<Text strong>{pInfo.name}</Text>}
                      description={`Lý do: ${item.skipReason || 'Vắng mặt khi gọi'}`}
                    />
                  </List.Item>
                )
              }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DoctorQueueTab
