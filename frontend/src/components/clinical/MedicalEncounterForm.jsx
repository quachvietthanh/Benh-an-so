import React from 'react'
import {
  Avatar,
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  List,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
} from 'antd'
import {
  CheckCircleOutlined,
  DeleteOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  HeartOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  PrinterOutlined,
  SearchOutlined,
  UploadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { getPopularIcd10 } from '../../utils/icd10Data'
import { clinicalCategories, formatCurrency } from '../../utils/clinicalCatalogData'

const { Title, Text } = Typography

function MedicalEncounterForm({
  form,
  isDoctor,
  patients,
  selectedPatientId,
  setSelectedPatientId,
  selectedPatientObj,
  vitalSigns,
  setVitalSigns,
  bmiValue,
  diagnosisType,
  setDiagnosisType,
  primaryIcd,
  setPrimaryIcd,
  secondaryIcds,
  setSecondaryIcds,
  commonIcd10List,
  setDiagnosisModalOpen,
  selectedOrders,
  orderCategory,
  setOrderCategory,
  orderSearchQuery,
  setOrderSearchQuery,
  filteredCatalog,
  handleAddOrder,
  handleRemoveOrder,
  handleToggleUrgent,
  handleUpdateOrderNote,
  totalOrderFee,
  setPrintModalOpen,
  results,
  setResults,
  files,
  setFiles,
  beforeUpload,
}) {
  return (
    <Form form={form} layout="vertical" disabled={!isDoctor}>
      <Row gutter={[16, 16]}>
        {/* Left Column: Patient Selection & Vital Signs */}
        <Col xs={24} lg={8}>
          <Card
            title={<span style={{ color: '#1E3A8A' }}><UserOutlined /> Thông tin Bệnh nhân</span>}
            style={{ marginBottom: 16 }}
            bordered
          >
            <Form.Item
              name="patientId"
              label="Chọn bệnh nhân khám"
              rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}
            >
              <Select
                showSearch
                placeholder="Gõ tên hoặc mã bệnh nhân (BN-...)"
                optionFilterProp="label"
                onChange={(val) => setSelectedPatientId(val)}
                options={patients.map((p) => ({
                  value: p.id,
                  label: `${p.fullName} - ${p.patientCode} (${p.phoneNumber || 'Không SĐT'}${p.queueNumber ? ` - STT: ${p.queueNumber}` : ''}${p.checkInTimeStr ? ` — Đến lúc: ${p.checkInTimeStr}` : ''})`,
                }))}
              />
            </Form.Item>

            {selectedPatientObj && (
              <div style={{ background: '#F8FAFC', padding: 12, borderRadius: 8, border: '1px solid #E2E8F0', marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                  <Avatar size={44} style={{ backgroundColor: '#2563EB', fontWeight: 600 }}>
                    {selectedPatientObj.fullName?.substring(0, 2).toUpperCase()}
                  </Avatar>
                  <div>
                    <Text strong style={{ fontSize: 15, display: 'block' }}>
                      {selectedPatientObj.fullName}
                    </Text>
                    <Tag color="blue">{selectedPatientObj.patientCode}</Tag>
                    <Tag color="purple">
                      {selectedPatientObj.gender === 'MALE' ? 'Nam' : selectedPatientObj.gender === 'FEMALE' ? 'Nữ' : 'Khác'}
                    </Tag>
                  </div>
                </div>
                <Divider style={{ margin: '8px 0' }} />
                <div style={{ fontSize: 12, lineHeight: '1.8' }}>
                  <div><b>Ngày sinh:</b> {selectedPatientObj.dateOfBirth}</div>
                  <div><b>Thẻ BHYT:</b> {selectedPatientObj.healthInsuranceCode || 'Không có'}</div>
                  <div><b>Tiền sử bệnh:</b> <Text type="danger">{selectedPatientObj.medicalHistory || 'Chưa ghi nhận'}</Text></div>
                  <div><b>Dị ứng thuốc:</b> <Text type="warning">{selectedPatientObj.allergies || 'Không dị ứng'}</Text></div>
                </div>
              </div>
            )}
          </Card>

          {/* Vital Signs Card */}
          <Card
            title={<span style={{ color: '#047857' }}><HeartOutlined /> Chỉ Số Sinh Hiệu Bệnh Nhân</span>}
            bordered
          >
            <Row gutter={8}>
              <Col span={12}>
                <Form.Item label="Huyết áp (mmHg)">
                  <Input
                    placeholder="120/80"
                    value={vitalSigns.bp}
                    onChange={(e) => setVitalSigns((v) => ({ ...v, bp: e.target.value }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="Nhịp mạch (lần/phút)">
                  <Input
                    placeholder="75"
                    value={vitalSigns.pulse}
                    onChange={(e) => setVitalSigns((v) => ({ ...v, pulse: e.target.value }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="Nhiệt độ cơ thể (°C)">
                  <Input
                    placeholder="37.0"
                    value={vitalSigns.temp}
                    onChange={(e) => setVitalSigns((v) => ({ ...v, temp: e.target.value }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="Oxy trong máu (SpO2 %)">
                  <Input
                    placeholder="98"
                    value={vitalSigns.spO2}
                    onChange={(e) => setVitalSigns((v) => ({ ...v, spO2: e.target.value }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="Cân nặng (kg)">
                  <Input
                    placeholder="60"
                    value={vitalSigns.weight}
                    onChange={(e) => setVitalSigns((v) => ({ ...v, weight: e.target.value }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="Chiều cao (cm)">
                  <Input
                    placeholder="165"
                    value={vitalSigns.height}
                    onChange={(e) => setVitalSigns((v) => ({ ...v, height: e.target.value }))}
                  />
                </Form.Item>
              </Col>
            </Row>

            {bmiValue && (
              <div style={{ background: '#ECFDF5', padding: '8px 12px', borderRadius: 6, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Text strong style={{ color: '#065F46' }}>Chỉ số thể trạng (BMI):</Text>
                <Tag color={bmiValue >= 25 ? 'volcano' : bmiValue < 18.5 ? 'orange' : 'green'} style={{ fontWeight: 700, fontSize: 13 }}>
                  {bmiValue} kg/m² ({bmiValue >= 25 ? 'Thừa cân' : bmiValue < 18.5 ? 'Thiếu cân' : 'Bình thường'})
                </Tag>
              </div>
            )}
          </Card>
        </Col>

        {/* Right Column: Symptoms & ICD Diagnosis */}
        <Col xs={24} lg={16}>
          <Card
            title={<span style={{ color: '#1E3A8A' }}><MedicineBoxOutlined /> Khám Lâm Sàng & Khám Bệnh</span>}
            style={{ marginBottom: 16 }}
            bordered
          >
            <Form.Item
              name="symptoms"
              label="Lý do khám / Triệu chứng cơ năng"
              rules={[{ required: true, message: 'Nhập triệu chứng lâm sàng' }]}
            >
              <Input.TextArea
                rows={2}
                placeholder="Ví dụ: Đau đầu kéo dài, sốt nhẹ 38 độ, ho hắt hơi, đau vùng thượng vị..."
              />
            </Form.Item>

            <Form.Item name="examinationNote" label="Khám lâm sàng & Tiền sử bệnh lý">
              <Input.TextArea
                rows={3}
                placeholder="Ghi chép khám tim phổi, bụng, thần kinh, các dấu hiệu thực thể..."
              />
            </Form.Item>
          </Card>

          {/* Diagnosis Module */}
          <Card
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: '#2563EB' }}><FileSearchOutlined /> Phân Loại & Mã Bệnh Chẩn Đoán (ICD-10)</span>
                <Button
                  type="dashed"
                  icon={<SearchOutlined />}
                  size="small"
                  onClick={() => setDiagnosisModalOpen(true)}
                >
                  Tra cứu mã ICD-10
                </Button>
              </div>
            }
            style={{ marginBottom: 16 }}
            bordered
          >
            {/* Diagnostic Type */}
            <Form.Item label="Loại chẩn đoán y khoa">
              <Select
                value={diagnosisType}
                onChange={setDiagnosisType}
                options={[
                  { value: 'PRELIMINARY', label: 'Chẩn đoán Sơ bộ (Lâm sàng)' },
                  { value: 'DEFINITIVE', label: 'Chẩn đoán Xác định (Có Cận lâm sàng)' },
                  { value: 'DIFFERENTIAL', label: 'Chẩn đoán Phân biệt' },
                ]}
              />
            </Form.Item>

            {/* Primary Diagnosis */}
            <Form.Item label="Chẩn đoán chính (Mã ICD-10)" required>
              {primaryIcd ? (
                <div style={{ background: '#EFF6FF', padding: 12, borderRadius: 8, border: '1px solid #BFDBFE', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Tag color="blue" style={{ fontSize: 14, fontWeight: 700, padding: '4px 8px' }}>
                      {primaryIcd.code}
                    </Tag>
                    <Text strong style={{ fontSize: 14, color: '#1E40AF', marginLeft: 8 }}>
                      {primaryIcd.name}
                    </Text>
                  </div>
                  <Button type="text" danger icon={<DeleteOutlined />} onClick={() => setPrimaryIcd(null)}>
                    Đổi mã
                  </Button>
                </div>
              ) : (
                <div>
                  <Form.Item name="diagnosisText" noStyle>
                    <Input.TextArea
                      rows={2}
                      placeholder="Gõ tên bệnh hoặc click chọn từ mã ICD-10 gợi ý bên dưới..."
                    />
                  </Form.Item>

                  {/* Popular ICD quick picker */}
                  <div style={{ marginTop: 10 }}>
                    <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
                      Gợi ý mã ICD-10 phổ biến:
                    </Text>
                    <Space wrap>
                      {getPopularIcd10().slice(0, 8).map((icd) => (
                        <Tag
                          key={icd.code}
                          color="cyan"
                          style={{ cursor: 'pointer', padding: '4px 8px', fontSize: 12 }}
                          onClick={() => {
                            setPrimaryIcd(icd)
                            form.setFieldsValue({ diagnosisText: `[${icd.code}] ${icd.name}` })
                          }}
                        >
                          <b>{icd.code}</b> - {icd.name}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                </div>
              )}
            </Form.Item>

            {/* Secondary / Accompanying Diagnoses */}
            <Form.Item label="Chẩn đoán kèm theo / Bệnh phụ (Mã ICD-10 phụ)">
              <div>
                {secondaryIcds.length > 0 && (
                  <div style={{ marginBottom: 10 }}>
                    <Space wrap>
                      {secondaryIcds.map((item) => (
                        <Tag
                          key={item.code}
                          color="purple"
                          closable
                          onClose={() => setSecondaryIcds((prev) => prev.filter((i) => i.code !== item.code))}
                          style={{ fontSize: 13, padding: '4px 10px' }}
                        >
                          <b>{item.code}</b>: {item.name}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                )}

                <Select
                  showSearch
                  placeholder="Chọn thêm bệnh kèm theo (Mã ICD-10)..."
                  value={null}
                  onChange={(code) => {
                    const item = commonIcd10List.find((i) => i.code === code)
                    if (item && !secondaryIcds.some((i) => i.code === item.code)) {
                      setSecondaryIcds((prev) => [...prev, item])
                    }
                  }}
                  options={commonIcd10List.map((item) => ({
                    value: item.code,
                    label: `[${item.code}] ${item.name}`,
                  }))}
                />
              </div>
            </Form.Item>

            <Form.Item name="treatmentPlan" label="Hướng điều trị & Lời dặn của bác sĩ">
              <Input.TextArea
                rows={2}
                placeholder="Chỉ định nhập viện, kê đơn thuốc về nhà, hạn chế ăn mặn, tái khám sau 7 ngày..."
              />
            </Form.Item>
          </Card>
        </Col>
      </Row>

      {/* Section 2: Clinical Orders Section */}
      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ color: '#16A34A' }}><FileTextOutlined /> Lập Trình & Nhập Chỉ Định Cận Lâm Sàng</span>
            {selectedOrders.length > 0 && (
              <Tag color="green" style={{ fontSize: 13, padding: '4px 10px' }}>
                Tổng chi phí chỉ định: <b>{formatCurrency(totalOrderFee)}</b>
              </Tag>
            )}
          </div>
        }
        bordered
        style={{ marginTop: 16 }}
      >
        <Row gutter={[16, 16]}>
          {/* Left catalog selector */}
          <Col xs={24} lg={10}>
            <div style={{ background: '#F8FAFC', padding: 12, borderRadius: 8, border: '1px solid #E2E8F0' }}>
              <Title level={5} style={{ margin: '0 0 10px 0', fontSize: 14 }}>
                Danh mục Dịch vụ Cận lâm sàng
              </Title>

              <Input
                prefix={<SearchOutlined />}
                placeholder="Tìm dịch vụ (Xét nghiệm, X-quang, SA...)"
                value={orderSearchQuery}
                onChange={(e) => setOrderSearchQuery(e.target.value)}
                style={{ marginBottom: 10 }}
              />

              <Select
                value={orderCategory}
                onChange={setOrderCategory}
                style={{ width: '100%', marginBottom: 12 }}
                options={clinicalCategories.map((c) => ({ value: c.key, label: c.label }))}
              />

              <div style={{ maxHeight: 320, overflowY: 'auto' }}>
                <List
                  size="small"
                  dataSource={filteredCatalog}
                  renderItem={(item) => {
                    const isAdded = selectedOrders.some((o) => o.code === item.code)
                    return (
                      <List.Item
                        actions={[
                          <Button
                            size="small"
                            type={isAdded ? 'default' : 'primary'}
                            disabled={isAdded}
                            icon={isAdded ? <CheckCircleOutlined /> : <PlusOutlined />}
                            onClick={() => handleAddOrder(item)}
                          >
                            {isAdded ? 'Đã chọn' : 'Thêm'}
                          </Button>,
                        ]}
                      >
                        <List.Item.Meta
                          title={
                            <Text strong style={{ fontSize: 13 }}>
                              {item.name}
                            </Text>
                          }
                          description={
                            <div style={{ fontSize: 11 }}>
                              <Tag color="cyan">{item.code}</Tag>
                              <Text type="danger" strong>{formatCurrency(item.price)}</Text> | {item.department}
                            </div>
                          }
                        />
                      </List.Item>
                    )
                  }}
                />
              </div>
            </div>
          </Col>

          {/* Right selected orders list */}
          <Col xs={24} lg={14}>
            <Title level={5} style={{ margin: '0 0 10px 0', fontSize: 14 }}>
              Danh sách Chỉ định Cận lâm sàng đã chọn ({selectedOrders.length})
            </Title>

            {selectedOrders.length === 0 ? (
              <div style={{ border: '2px dashed #CBD5E1', padding: 40, textAlign: 'center', borderRadius: 8, color: '#64748B' }}>
                <FileTextOutlined style={{ fontSize: 32, marginBottom: 8, color: '#94A3B8' }} />
                <div>Chưa có dịch vụ cận lâm sàng nào được chỉ định.</div>
                <div style={{ fontSize: 12 }}>Chọn từ danh mục bên trái để thêm xét nghiệm/CĐHA cho bệnh nhân.</div>
              </div>
            ) : (
              <div>
                <Table
                  size="small"
                  rowKey="code"
                  pagination={false}
                  dataSource={selectedOrders}
                  columns={[
                    {
                      title: 'Dịch vụ',
                      key: 'name',
                      render: (r) => (
                        <div>
                          <Text strong>{r.name}</Text>
                          <div style={{ fontSize: 11, color: '#64748B' }}>Mã: {r.code} - {r.department}</div>
                        </div>
                      ),
                    },
                    {
                      title: 'Cấp cứu',
                      key: 'urgent',
                      width: 90,
                      render: (r) => (
                        <Button
                          size="small"
                          type={r.isUrgent ? 'primary' : 'default'}
                          danger={r.isUrgent}
                          onClick={() => handleToggleUrgent(r.code)}
                        >
                          {r.isUrgent ? 'CẤP CỨU' : 'Thường'}
                        </Button>
                      ),
                    },
                    {
                      title: 'Ghi chú KTV',
                      key: 'note',
                      width: 160,
                      render: (r) => (
                        <Input
                          size="small"
                          placeholder="Nhịn ăn, chụp nghiêng..."
                          value={r.note}
                          onChange={(e) => handleUpdateOrderNote(r.code, e.target.value)}
                        />
                      ),
                    },
                    {
                      title: 'Giá tiền',
                      dataIndex: 'price',
                      width: 100,
                      align: 'right',
                      render: (val) => formatCurrency(val),
                    },
                    {
                      title: '',
                      key: 'action',
                      width: 40,
                      render: (r) => (
                        <Button
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => handleRemoveOrder(r.code)}
                        />
                      ),
                    },
                  ]}
                />

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
                  <Button icon={<PrinterOutlined />} onClick={() => setPrintModalOpen(true)}>
                    Xem trước & In Phiếu chỉ định
                  </Button>
                  <Text strong style={{ fontSize: 15 }}>
                    Tổng cộng: <span style={{ color: '#DC2626' }}>{formatCurrency(totalOrderFee)}</span>
                  </Text>
                </div>
              </div>
            )}
          </Col>
        </Row>
      </Card>

      {/* Section 3: Results & File Attachments */}
      <Card
        title={<span style={{ color: '#7C3AED' }}><UploadOutlined /> Kết Quả Cận Lâm Sàng & Tệp Đính Kèm</span>}
        bordered
        style={{ marginTop: 16 }}
      >
        {selectedOrders.length > 0 && (
          <Form.Item label="Nhập nhanh kết quả các xét nghiệm / chỉ định:">
            <Space direction="vertical" style={{ width: '100%' }}>
              {selectedOrders.map((order) => (
                <Input
                  key={order.code}
                  addonBefore={<Tag color="purple">{order.name}</Tag>}
                  value={results[order.code] || ''}
                  placeholder="Ghi nhận kết quả (Ví dụ: Bình thường, 5.2 mmol/L, có ổ viêm...)"
                  onChange={(e) => setResults((prev) => ({ ...prev, [order.code]: e.target.value }))}
                />
              ))}
            </Space>
          </Form.Item>
        )}

        <Form.Item label="Tệp đính kèm kết quả (PDF / JPG / PNG, tối đa 10 MB)">
          <Upload
            beforeUpload={beforeUpload}
            fileList={files}
            onRemove={(file) => setFiles((current) => current.filter((item) => item.uid !== file.uid))}
          >
            <Button icon={<UploadOutlined />}>Tải tệp đính kèm lên</Button>
          </Upload>
        </Form.Item>
      </Card>
    </Form>
  )
}

export default MedicalEncounterForm
