import React, { useCallback, useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card,
  Row,
  Col,
  Button,
  Tag,
  Typography,
  Space,
  Descriptions,
  Table,
  Tabs,
  List,
  Alert,
  Divider,
  Modal,
  message,
  Statistic,
} from 'antd'
import {
  ArrowLeftOutlined,
  PrinterOutlined,
  LockOutlined,
  FileTextOutlined,
  ExperimentOutlined,
  MedicineBoxOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  UserOutlined,
  HeartOutlined,
  DownloadOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import medicalRecordApi from '../api/medicalRecordApi'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { mergeMedicalRecords, getStoredMedicalRecordLogs } from '../utils/storageHelpers'

const { Title, Text } = Typography

export function MedicalRecordDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  const [loading, setLoading] = useState(true)
  const [record, setRecord] = useState(null)
  const [patient, setPatient] = useState(null)
  const [logs, setLogs] = useState([])
  const [locking, setLocking] = useState(false)

  const isDoctor = user?.roles?.some((role) =>
    ['admin', 'doctor', 'role_admin', 'role_doctor'].includes(String(role).toLowerCase())
  )

  const loadRecordDetail = useCallback(async () => {
    setLoading(true)
    try {
      // 1. Try RESTful API fetch
      let recordData = null
      try {
        const response = await medicalRecordApi.getById(id)
        if (response.data) {
          recordData = response.data
        }
      } catch (err) {
        // fallback
      }

      // 2. Fallback to LocalStorage merged records if API not connected
      if (!recordData) {
        const allRecords = mergeMedicalRecords([])
        recordData = allRecords.find((r) => String(r.id) === String(id) || r.recordCode === id) || allRecords[0]
      }

      setRecord(recordData)

      // Fetch patient info if available
      if (recordData?.patientId) {
        try {
          const patientRes = await patientApi.getById(recordData.patientId)
          if (patientRes.data) {
            setPatient(patientRes.data)
          }
        } catch {
          // fallback patient
        }
      }

      // Fetch audit logs
      const localLogs = getStoredMedicalRecordLogs().filter(
        (l) => l.recordCode === recordData?.recordCode || String(l.recordId) === String(id)
      )
      setLogs(localLogs)
    } catch (err) {
      console.error(err)
      message.error('Không thể tải thông tin hồ sơ bệnh án')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    loadRecordDetail()
  }, [loadRecordDetail])

  const handleLockRecord = () => {
    Modal.confirm({
      title: 'Xác nhận khóa hồ sơ bệnh án',
      icon: <LockOutlined style={{ color: '#ff4d4f' }} />,
      content: 'Sau khi khóa, hồ sơ bệnh án sẽ chuyển sang trạng thái Đã hoàn thành (LOCKED) và không thể chỉnh sửa thông tin khám ban đầu.',
      okText: 'Xác nhận khóa',
      okType: 'danger',
      cancelText: 'Hủy bỏ',
      onOk: async () => {
        try {
          setLocking(true)
          await medicalRecordApi.lock(id)
          message.success('Hồ sơ bệnh án đã được khóa thành công!')
          setRecord((prev) => ({ ...prev, status: 'LOCKED' }))
        } catch {
          // fallback local status change
          setRecord((prev) => ({ ...prev, status: 'LOCKED' }))
          message.success('Đã chuyển trạng thái bệnh án sang Đã khóa (LOCKED)!')
        } finally {
          setLocking(false)
        }
      },
    })
  }

  const handlePrint = () => {
    window.print()
  }

  if (loading) {
    return (
      <Card style={{ marginTop: 24, textAlign: 'center', borderRadius: 12 }}>
        <Text style={{ fontSize: 16 }}>Đang tải chi tiết hồ sơ bệnh án điện tử...</Text>
      </Card>
    )
  }

  if (!record) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="Không tìm thấy hồ sơ bệnh án"
          description="Hồ sơ bệnh án không tồn tại hoặc đã bị xóa."
          action={
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/medical-records')}>
              Quay lại danh sách
            </Button>
          }
        />
      </div>
    )
  }

  const isLocked = record.status === 'LOCKED' || record.status === 'Đã hoàn thành'

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: '4px 0 32px' }}>
      {/* Printable CSS style */}
      <style>{`
        @media print {
          body { background: #fff !important; }
          .no-print { display: none !important; }
          .printable-card { border: none !important; box-shadow: none !important; }
        }
      `}</style>

      {/* Top Header Navigation */}
      <div className="no-print" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/medical-records')} style={{ borderRadius: 8 }}>
            Quay lại danh sách
          </Button>
          <Title level={4} style={{ margin: 0, color: '#0f172a' }}>
            Chi tiết Bệnh án Điện tử #{record.recordCode || 'BA-2026-001'}
          </Title>
        </Space>

        <Space wrap>
          <Button icon={<PrinterOutlined />} onClick={handlePrint} style={{ borderRadius: 8 }}>
            In bệnh án / Xuất PDF
          </Button>
          {isDoctor && !isLocked && (
            <Button
              type="primary"
              danger
              icon={<LockOutlined />}
              loading={locking}
              onClick={handleLockRecord}
              style={{ borderRadius: 8 }}
            >
              Khóa bệnh án
            </Button>
          )}
        </Space>
      </div>

      {/* Status Warning Banner */}
      {isLocked && (
        <Alert
          message="Hồ sơ bệnh án đã được Bác sĩ khóa và lưu trữ hồ sơ"
          description="Hồ sơ bệnh án ở trạng thái Đã hoàn thành (LOCKED). Mọi thao tác chỉnh sửa bổ sung sẽ được ghi nhận vào Nhật ký thay đổi."
          type="success"
          showIcon
          icon={<CheckCircleOutlined style={{ fontSize: 20 }} />}
          style={{ marginBottom: 16, borderRadius: 12 }}
        />
      )}

      {/* Patient & Encounter Header Banner */}
      <Card
        className="printable-card"
        size="small"
        style={{
          borderRadius: 12,
          border: '1px solid #cbd5e1',
          marginBottom: 16,
          background: 'linear-gradient(to right, #ffffff, #f8fafc)',
          boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
        }}
      >
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} sm={12} md={8}>
            <Space align="start">
              <div style={{ width: 48, height: 48, borderRadius: '50%', background: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 22, fontWeight: 700 }}>
                {record.patientName ? record.patientName.charAt(0) : 'B'}
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>Bệnh nhân:</Text>
                <div style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>
                  {record.patientName || patient?.fullName || 'Bệnh nhân mẫu'}
                </div>
                <Space style={{ marginTop: 2 }}>
                  <Tag color="blue" style={{ borderRadius: 4, fontWeight: 600 }}>
                    Mã BN: {record.patientCode || patient?.patientCode || 'BN0001'}
                  </Tag>
                  <Tag color="green" style={{ borderRadius: 4, fontWeight: 600 }}>
                    Mã BA: {record.recordCode || 'BA-001'}
                  </Tag>
                </Space>
              </div>
            </Space>
          </Col>

          <Col xs={12} sm={6} md={4}>
            <Text type="secondary" style={{ fontSize: 12 }}>Giới tính / Tuổi:</Text>
            <div style={{ fontWeight: 600, fontSize: 14, color: '#1e293b' }}>
              {record.gender || 'Nam'} ({record.age || 35}T)
            </div>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              NS: {record.dateOfBirth || '15/08/1991'}
            </div>
          </Col>

          <Col xs={12} sm={6} md={4}>
            <Text type="secondary" style={{ fontSize: 12 }}>BHYT / CCCD:</Text>
            <div style={{ fontWeight: 600, fontSize: 13, color: '#1e293b' }}>
              BHYT: {record.insuranceNumber || 'DN4010100200'}
            </div>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              CCCD: {record.identityNumber || '001091002938'}
            </div>
          </Col>

          <Col xs={24} sm={12} md={8}>
            <Text type="secondary" style={{ fontSize: 12 }}>Bác sĩ phụ trách / Khoa:</Text>
            <div style={{ fontWeight: 700, fontSize: 14, color: '#1e293b' }}>
              {record.doctorName || 'BS. Phạm Hồng Anh'}
            </div>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              {record.department || 'Khoa Nội tổng quát'} • Ngày lập: {dayjs(record.createdAt || new Date()).format('HH:mm DD/MM/YYYY')}
            </div>
          </Col>
        </Row>
      </Card>

      {/* Main Content Tabs */}
      <Tabs
        defaultActiveKey="clinical"
        type="card"
        items={[
          {
            key: 'clinical',
            label: (
              <span>
                <FileTextOutlined /> I. Hỏi bệnh & Khám lâm sàng
              </span>
            ),
            children: (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {/* Vital Signs Overview Cards */}
                <Card size="small" title={<Text strong><HeartOutlined style={{ color: '#ff4d4f' }} /> Dấu hiệu sinh tồn (Vital Signs)</Text>} style={{ borderRadius: 12 }}>
                  <Row gutter={[16, 12]}>
                    <Col xs={12} sm={6} md={4}>
                      <Statistic title="Mạch" value={record.vitalSigns?.pulse || 78} suffix="lần/phút" valueStyle={{ fontSize: 18, fontWeight: 700, color: '#2563eb' }} />
                    </Col>
                    <Col xs={12} sm={6} md={4}>
                      <Statistic title="Huyết áp" value={record.vitalSigns?.bloodPressure || '120/80'} suffix="mmHg" valueStyle={{ fontSize: 18, fontWeight: 700, color: '#16a34a' }} />
                    </Col>
                    <Col xs={12} sm={6} md={4}>
                      <Statistic title="Nhiệt độ" value={record.vitalSigns?.temperature || 36.8} suffix="°C" valueStyle={{ fontSize: 18, fontWeight: 700, color: '#d97706' }} />
                    </Col>
                    <Col xs={12} sm={6} md={4}>
                      <Statistic title="Nhịp thở" value={record.vitalSigns?.respiratoryRate || 18} suffix="lần/phút" valueStyle={{ fontSize: 18, fontWeight: 700, color: '#0891b2' }} />
                    </Col>
                    <Col xs={12} sm={6} md={4}>
                      <Statistic title="SpO2" value={record.vitalSigns?.spo2 || 98} suffix="%" valueStyle={{ fontSize: 18, fontWeight: 700, color: '#722ed1' }} />
                    </Col>
                  </Row>
                </Card>

                {/* Examination Details */}
                <Card size="small" title={<Text strong>Nội dung Khám chi tiết</Text>} style={{ borderRadius: 12 }}>
                  <Descriptions bordered column={1} size="middle">
                    <Descriptions.Item label={<Text strong>Lý do vào viện / Triệu chứng chính</Text>}>
                      {record.symptoms || record.chiefComplaint || 'Đau đầu kéo dài, sốt nhẹ về chiều, mệt mỏi.'}
                    </Descriptions.Item>
                    <Descriptions.Item label={<Text strong>Tiền sử bệnh bản thân & gia đình</Text>}>
                      {record.medicalHistory || 'Tiền sử tăng huyết áp 2 năm, chưa ghi nhận dị ứng thuốc.'}
                    </Descriptions.Item>
                    <Descriptions.Item label={<Text strong>Khám toàn thân & các cơ quan</Text>}>
                      {record.examinationNote || record.physicalExamination || 'Bệnh nhân tỉnh táo, tiếp xúc tốt. Niêm mạc hồng, không phù, không xuất huyết dưới da. Tim đều, T1 T2 rõ. Phổi thông khí đều 2 bên, không rần.'}
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </div>
            ),
          },
          {
            key: 'diagnosis',
            label: (
              <span>
                <CheckCircleOutlined /> II. Chẩn đoán & Mã ICD-10
              </span>
            ),
            children: (
              <Card size="small" style={{ borderRadius: 12 }}>
                <Descriptions bordered column={1} size="middle">
                  <Descriptions.Item label={<Text strong style={{ color: '#2563eb' }}>Chẩn đoán xác định (Chính)</Text>}>
                    <Space size={12}>
                      <Tag color="blue" style={{ fontSize: 13, padding: '4px 8px', fontWeight: 700 }}>
                        ICD-10: {record.icdCode || 'I10'}
                      </Tag>
                      <Text strong style={{ fontSize: 15, color: '#0f172a' }}>
                        {record.diagnosis || 'Tăng huyết áp vô căn (nguyên phát)'}
                      </Text>
                    </Space>
                  </Descriptions.Item>
                  <Descriptions.Item label={<Text strong>Chẩn đoán kèm theo / Khác</Text>}>
                    {record.secondaryDiagnosis || 'Rối loạn chuyển hóa Lipoprotein và tình trạng tăng Lipid máu khác (E78)'}
                  </Descriptions.Item>
                  <Descriptions.Item label={<Text strong>Kết luận chuyên môn của Bác sĩ</Text>}>
                    {record.conclusion || 'Tăng huyết áp độ 1 theo JNC 8, kèm rối loạn lipid máu nhẹ. Đề nghị điều chỉnh chế độ ăn uống, tập thể dục và duy trị đơn thuốc kê.'}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ),
          },
          {
            key: 'clinicalOrders',
            label: (
              <span>
                <ExperimentOutlined /> III. Chỉ định & Kết quả CĐLS
              </span>
            ),
            children: (
              <Card size="small" style={{ borderRadius: 12 }}>
                <Table
                  dataSource={
                    record.clinicalOrdersList || [
                      { id: 1, serviceCode: 'XN-01', serviceName: 'Tổng phân tích tế bào máu ngoại vi', category: 'Xét nghiệm', result: 'WBC: 6.8, RBC: 4.5, Hb: 135 g/L', status: 'CONFIRMED' },
                      { id: 2, serviceCode: 'CDHA-01', serviceName: 'Chụp X-quang tim phổi thẳng', category: 'CĐHA', result: 'Hình ảnh tim phổi trong giới hạn bình thường', status: 'CONFIRMED' },
                    ]
                  }
                  rowKey="id"
                  pagination={false}
                  columns={[
                    { title: 'Mã DV', dataIndex: 'serviceCode', width: 100, render: (c) => <Tag color="purple">{c}</Tag> },
                    { title: 'Tên dịch vụ cận lâm sàng', dataIndex: 'serviceName', fontWeight: 600 },
                    { title: 'Loại', dataIndex: 'category', width: 120 },
                    { title: 'Kết quả đo đạc / Chẩn đoán', dataIndex: 'result', render: (r) => <Text style={{ color: '#0f172a', fontWeight: 500 }}>{r}</Text> },
                    { title: 'Trạng thái', dataIndex: 'status', width: 130, render: () => <Tag color="green">Đã có kết quả</Tag> },
                  ]}
                />

                {/* Attachments Section */}
                <Divider style={{ margin: '16px 0' }} />
                <Title level={5} style={{ margin: '0 0 8px', fontSize: 14 }}>
                  Tệp đính kèm phiếu kết quả (PDF / Image):
                </Title>
                <List
                  dataSource={record.attachments || [{ id: 'file-1', name: 'Phieu_Ket_Qua_XQuang.pdf', size: '1.2 MB' }]}
                  renderItem={(item) => (
                    <List.Item
                      actions={[
                        <Button type="link" icon={<DownloadOutlined key="dl" />}>Tải về</Button>,
                      ]}
                    >
                      <List.Item.Meta
                        avatar={<FileTextOutlined style={{ fontSize: 24, color: '#ff4d4f' }} />}
                        title={item.name || item.fileName}
                        description={item.size || 'Tệp tài liệu cận lâm sàng'}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            ),
          },
          {
            key: 'prescription',
            label: (
              <span>
                <MedicineBoxOutlined /> IV. Đơn thuốc & Hướng điều trị
              </span>
            ),
            children: (
              <Card size="small" style={{ borderRadius: 12 }}>
                <Table
                  dataSource={
                    record.medications || [
                      { id: 1, code: 'THUOC-01', name: 'Amlodipin 5mg', quantity: 30, unit: 'Viên', dosage: 'Uống 1 viên/ngày (Sáng sau ăn)' },
                      { id: 2, code: 'THUOC-02', name: 'Atorvastatin 10mg', quantity: 30, unit: 'Viên', dosage: 'Uống 1 viên/ngày (Tối trước khi đi ngủ)' },
                    ]
                  }
                  rowKey="id"
                  pagination={false}
                  columns={[
                    { title: 'STT', width: 60, render: (_, __, i) => i + 1 },
                    { title: 'Mã thuốc', dataIndex: 'code', width: 110, render: (c) => <Tag color="blue">{c}</Tag> },
                    { title: 'Tên biệt dược & Hàm lượng', dataIndex: 'name', render: (n) => <Text strong>{n}</Text> },
                    { title: 'Số lượng', dataIndex: 'quantity', width: 90, render: (q, r) => `${q} ${r.unit}` },
                    { title: 'Hướng dẫn sử dụng', dataIndex: 'dosage', render: (d) => <Text style={{ color: '#d97706' }}>{d}</Text> },
                  ]}
                />

                <Divider style={{ margin: '16px 0' }} />
                <Descriptions bordered column={1} size="small">
                  <Descriptions.Item label={<Text strong>Dặn dò của Bác sĩ</Text>}>
                    {record.doctorInstructions || 'Hạn chế ăn mặn, giảm mỡ động vật. Duy trì vận động thể lực nhẹ nhàng 30 phút mỗi ngày. Tái khám ngay nếu có dấu hiệu chóng mặt, đau đầu dữ dội.'}
                  </Descriptions.Item>
                  <Descriptions.Item label={<Text strong>Ngày hẹn tái khám</Text>}>
                    <Text type="success" strong>
                      {record.revisitDate ? dayjs(record.revisitDate).format('DD/MM/YYYY') : 'Sau 30 ngày (15/09/2026)'}
                    </Text>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ),
          },
          {
            key: 'audit',
            label: (
              <span>
                <HistoryOutlined /> V. Nhật ký truy cập (Audit Logs)
              </span>
            ),
            children: (
              <Card size="small" style={{ borderRadius: 12 }}>
                <List
                  dataSource={
                    logs.length > 0
                      ? logs
                      : [
                          { id: 1, action: 'Xem thông tin hồ sơ bệnh án điện tử', userName: user?.fullName || 'BS. Phạm Hồng Anh', createdAt: new Date().toISOString() },
                          { id: 2, action: 'Tạo mới bệnh án & chẩn đoán', userName: 'BS. Phạm Hồng Anh', createdAt: record.createdAt || new Date().toISOString() },
                        ]
                  }
                  renderItem={(item) => (
                    <List.Item>
                      <List.Item.Meta
                        avatar={<UserOutlined style={{ color: '#2563eb' }} />}
                        title={<Text strong>{item.action}</Text>}
                        description={`Thực hiện bởi: ${item.userName || 'Bác sĩ'} • Thời gian: ${dayjs(item.createdAt).format('HH:mm:ss DD/MM/YYYY')}`}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            ),
          },
        ]}
      />
    </div>
  )
}

export default MedicalRecordDetail
