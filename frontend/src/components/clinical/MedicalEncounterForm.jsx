import React, { useMemo, useState } from 'react'
import {
  Avatar,
  Button,
  Card,
  Col,
  Descriptions,
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
} from 'antd'
import {
  CheckCircleOutlined,
  CheckOutlined,
  DeleteOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  HeartOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  PrinterOutlined,
  SearchOutlined,
  TableOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { getDiseaseGroupName, icd10Categories } from '../../utils/icd10Data'
import { fixMojibake } from '../../utils/serviceCatalogValidation'
import { clinicalCategories, formatCurrency } from '../../utils/clinicalCatalogData'
import MedicalRecordSignatureStamp from './MedicalRecordSignatureStamp'

const { Title, Text } = Typography

const categoryMeta = {
  RESPIRATORY: { label: 'Hô hấp', color: 'cyan' },
  CIRCULATORY: { label: 'Tim mạch', color: 'red' },
  DIGESTIVE: { label: 'Tiêu hóa', color: 'orange' },
  ENDOCRINE: { label: 'Nội tiết', color: 'gold' },
  MUSCULOSKELETAL: { label: 'Cơ xương khớp', color: 'geekblue' },
  NERVOUS: { label: 'Thần kinh', color: 'purple' },
  INFECTIOUS: { label: 'Nhiễm trùng', color: 'magenta' },
  GENITOURINARY: { label: 'Tiết niệu', color: 'blue' },
  SYMPTOMS: { label: 'Triệu chứng', color: 'volcano' },
}

function MedicalEncounterForm({
  form,
  isDoctor,
  isSigned = false,
  medicalRecord = null,
  encounterContext,
  selectedPatientObj,
  vitalSigns,
  setVitalSigns,
  bmiValue,
  diagnosisType,
  setDiagnosisType,
  primaryIcd,
  clearPrimaryDiagnosis,
  selectPrimaryDiagnosis,
  secondaryIcds,
  setSecondaryIcds,
  addSecondaryDiagnosis,
  diagnosisOptions,
  diagnosisSearching,
  onDiagnosisSearch,
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
  serviceCatalogError,
  onOpenSignModal,
}) {
  const [icdTableSearch, setIcdTableSearch] = useState('')
  const [icdTableCategory, setIcdTableCategory] = useState('ALL')
  const [showIcdTable, setShowIcdTable] = useState(true)

  const availableIcdList = useMemo(() => {
    let list = diagnosisOptions || []
    const q = icdTableSearch.trim().toLowerCase()
    if (q) {
      list = list.filter(
        (item) =>
          item.code.toLowerCase().includes(q) ||
          (item.name && item.name.toLowerCase().includes(q)),
      )
    }
    if (icdTableCategory !== 'ALL') {
      list = list.filter((item) => item.category === icdTableCategory)
    }
    return list
  }, [diagnosisOptions, icdTableSearch, icdTableCategory])

  const hasCompletePricing = selectedOrders.every((order) => order.price != null)

  return (
    <Form form={form} layout="vertical" disabled={!isDoctor || isSigned}>
      {isSigned && (
        <div style={{ marginBottom: 16 }}>
          <MedicalRecordSignatureStamp
            signatureData={medicalRecord?.signatureData}
            signedAt={medicalRecord?.signedAt}
            signedBy={medicalRecord?.signedBy}
            doctorName={encounterContext?.doctor?.fullName || 'Bác sĩ phụ trách'}
            status={medicalRecord?.status || 'SIGNED'}
          />
        </div>
      )}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card
            title={<span style={{ color: '#1E3A8A' }}><UserOutlined /> Thông tin lượt khám</span>}
            style={{ marginBottom: 16 }}
            bordered
          >
            <Form.Item name="patientId" hidden>
              <Input />
            </Form.Item>

            <Descriptions size="small" column={1} bordered style={{ marginBottom: 12 }}>
              <Descriptions.Item label="Mã lượt khám">
                <Text strong>{encounterContext?.visit?.visitCode || encounterContext?.visit?.id}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Queue / STT">
                {encounterContext?.queueItem
                  ? `${encounterContext.queueItem.id} / ${encounterContext.queueItem.queueNumber}`
                  : 'Không có'}
              </Descriptions.Item>
              <Descriptions.Item label="Phòng">
                {encounterContext?.room?.roomNumber || 'Chưa phân phòng'}
              </Descriptions.Item>
              <Descriptions.Item label="Bác sĩ">
                {encounterContext?.doctor?.fullName || 'Chưa phân công'}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color="processing">{encounterContext?.queueItem?.status || encounterContext?.visit?.status}</Tag>
              </Descriptions.Item>
            </Descriptions>

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

        <Col xs={24} lg={16}>
          <Card
            title={<span style={{ color: '#1E3A8A' }}><MedicineBoxOutlined /> Khám lâm sàng</span>}
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

          <Card
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: '#2563EB' }}><FileSearchOutlined /> Phân loại và mã bệnh chẩn đoán (ICD-10)</span>
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
            <Form.Item label="Loại chẩn đoán y khoa">
              <Select
                value={diagnosisType}
                onChange={setDiagnosisType}
                options={[
                  { value: 'PRELIMINARY', label: 'Chẩn đoán Sơ bộ (Lâm sàng)' },
                  { value: 'DEFINITIVE', label: 'Chẩn đoán xác định (có cận lâm sàng)' },
                  { value: 'DIFFERENTIAL', label: 'Chẩn đoán Phân biệt' },
                ]}
              />
            </Form.Item>

            <Form.Item label="Chẩn đoán chính (Mã ICD-10)" required>
              {primaryIcd ? (
                <div style={{ background: '#F0FDF4', padding: '12px 16px', borderRadius: 8, border: '1px solid #BBF7D0', marginBottom: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                    <Space size={8} wrap align="center">
                      <Tag color="blue" style={{ fontSize: 13.5, fontWeight: 700, padding: '3px 8px', borderRadius: 6, margin: 0 }}>
                        {primaryIcd.code}
                      </Tag>
                      <Text strong style={{ fontSize: 14, color: '#0F172A' }}>
                        {fixMojibake(primaryIcd.name)}
                      </Text>
                      <Tag color={categoryMeta[primaryIcd.category]?.color || 'cyan'} style={{ fontSize: 11.5, fontWeight: 600, margin: 0 }}>
                        Nhóm: {primaryIcd.diseaseGroup || getDiseaseGroupName(primaryIcd.code, primaryIcd.diseaseGroup)}
                      </Tag>
                    </Space>
                    <Button type="text" danger icon={<DeleteOutlined />} onClick={clearPrimaryDiagnosis} style={{ fontWeight: 600 }}>
                      Đổi mã
                    </Button>
                  </div>
                </div>
              ) : (
                <div>
                  <Select
                    showSearch
                    allowClear
                    placeholder="🔍 Tra cứu mã bệnh theo mã ICD (J00, I10...) hoặc tên bệnh (cảm cúm, đau đầu...)"
                    value={null}
                    loading={diagnosisSearching}
                    style={{ width: '100%' }}
                    filterOption={(input, option) => {
                      const q = (input || '').toLowerCase().trim()
                      const code = (option?.data?.code || '').toLowerCase()
                      const name = (option?.data?.name || '').toLowerCase()
                      const group = (option?.data?.diseaseGroup || option?.data?.category || '').toLowerCase()
                      return code.includes(q) || name.includes(q) || group.includes(q)
                    }}
                    onSearch={onDiagnosisSearch}
                    onChange={(code) => {
                      if (!code) return
                      const item = diagnosisOptions.find((d) => d.code === code)
                      if (item) selectPrimaryDiagnosis(item)
                    }}
                    notFoundContent={diagnosisSearching ? 'Đang tìm trong danh mục mã bệnh...' : 'Không tìm thấy mã bệnh phù hợp'}
                    options={diagnosisOptions.map((item) => {
                      const groupName = item.diseaseGroup || getDiseaseGroupName(item.code, item.diseaseGroup)
                      const groupColor = categoryMeta[item.category]?.color || 'blue'
                      return {
                        value: item.code,
                        data: item,
                        label: (
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', gap: 8 }}>
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              <Tag color="blue" style={{ fontWeight: 700, marginInlineEnd: 6 }}>{item.code}</Tag>
                              <span>{fixMojibake(item.name)}</span>
                            </span>
                            <Tag color={groupColor} style={{ fontSize: 11, margin: 0, flexShrink: 0 }}>
                              {groupName}
                            </Tag>
                          </div>
                        ),
                      }
                    })}
                  />

                  <div style={{ marginTop: 10 }}>
                    <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
                      Gợi ý chẩn đoán thường gặp / Dùng gần đây:
                    </Text>
                    <Space wrap size={[6, 6]}>
                      {diagnosisOptions.slice(0, 10).map((icd) => (
                        <Tag
                          key={icd.code}
                          color="cyan"
                          style={{ cursor: 'pointer', padding: '3px 8px', fontSize: 12, borderRadius: 6 }}
                          onClick={() => selectPrimaryDiagnosis(icd)}
                        >
                          <b>{icd.code}</b> - {fixMojibake(icd.name)}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                </div>
              )}
            </Form.Item>

            <Form.Item label="Chẩn đoán kèm theo / Bệnh phụ (Mã ICD-10 phụ)">
              <div>
                {secondaryIcds.length > 0 && (
                  <div style={{ marginBottom: 10 }}>
                    <Space wrap size={[6, 8]}>
                      {secondaryIcds.map((item) => (
                        <Tag
                          key={item.code}
                          color="purple"
                          closable
                          onClose={() => setSecondaryIcds((prev) => prev.filter((i) => i.code !== item.code))}
                          style={{ fontSize: 13, padding: '4px 10px', borderRadius: 6 }}
                        >
                          <b>{item.code}</b>: {fixMojibake(item.name)}
                          <span style={{ color: '#7e22ce', marginLeft: 6, fontSize: 11 }}>
                            ({item.diseaseGroup || getDiseaseGroupName(item.code, item.diseaseGroup)})
                          </span>
                        </Tag>
                      ))}
                    </Space>
                  </div>
                )}

                <Select
                  showSearch
                  allowClear
                  placeholder={primaryIcd ? '🔍 Tìm mã hoặc tên bệnh kèm theo...' : 'Vui lòng chọn chẩn đoán chính trước'}
                  value={null}
                  disabled={!primaryIcd}
                  loading={diagnosisSearching}
                  style={{ width: '100%' }}
                  filterOption={(input, option) => {
                    const q = (input || '').toLowerCase().trim()
                    const code = (option?.data?.code || '').toLowerCase()
                    const name = (option?.data?.name || '').toLowerCase()
                    const group = (option?.data?.diseaseGroup || option?.data?.category || '').toLowerCase()
                    return code.includes(q) || name.includes(q) || group.includes(q)
                  }}
                  onSearch={onDiagnosisSearch}
                  onDropdownVisibleChange={(open) => open && onDiagnosisSearch('')}
                  onChange={(code) => {
                    if (!code) return
                    const item = diagnosisOptions.find((diagnosis) => diagnosis.code === code)
                    if (item) addSecondaryDiagnosis(item)
                  }}
                  notFoundContent={diagnosisSearching ? 'Đang tìm trong danh mục...' : 'Không tìm thấy chẩn đoán phù hợp'}
                  options={diagnosisOptions.map((item) => {
                    const groupName = item.diseaseGroup || getDiseaseGroupName(item.code, item.diseaseGroup)
                    const groupColor = categoryMeta[item.category]?.color || 'purple'
                    return {
                      value: item.code,
                      data: item,
                      label: (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', gap: 8 }}>
                          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            <Tag color="purple" style={{ fontWeight: 700, marginInlineEnd: 6 }}>{item.code}</Tag>
                            <span>{fixMojibake(item.name)}</span>
                          </span>
                          <Tag color={groupColor} style={{ fontSize: 11, margin: 0, flexShrink: 0 }}>
                            {groupName}
                          </Tag>
                        </div>
                      ),
                    }
                  })}
                />
              </div>
            </Form.Item>

            <Divider style={{ margin: '16px 0 12px' }} />

            <div style={{ background: '#F8FAFC', padding: 14, borderRadius: 10, border: '1px solid #E2E8F0', marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <TableOutlined style={{ color: '#2563EB', fontSize: 16 }} />
                  <Text strong style={{ fontSize: 14, color: '#1E293B' }}>
                    Bảng chọn mã bệnh chẩn đoán (ICD-10)
                  </Text>
                  <Tag color="blue" style={{ margin: 0 }}>
                    {availableIcdList.length} mã bệnh
                  </Tag>
                </div>
                <Button
                  size="small"
                  type="text"
                  onClick={() => setShowIcdTable((prev) => !prev)}
                  style={{ color: '#2563EB', fontWeight: 600 }}
                >
                  {showIcdTable ? 'Thu gọn bảng' : 'Mở rộng bảng chọn'}
                </Button>
              </div>

              {showIcdTable && (
                <>
                  <Row gutter={[8, 8]} style={{ marginBottom: 10 }}>
                    <Col xs={24} sm={14}>
                      <Input
                        allowClear
                        placeholder="Tìm nhanh theo mã (J00, I10, K21...) hoặc tên bệnh..."
                        prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
                        value={icdTableSearch}
                        onChange={(e) => setIcdTableSearch(e.target.value)}
                      />
                    </Col>
                    <Col xs={24} sm={10}>
                      <Select
                        style={{ width: '100%' }}
                        value={icdTableCategory}
                        onChange={setIcdTableCategory}
                        options={icd10Categories.map((item) => ({
                          value: item.key,
                          label: item.label,
                        }))}
                      />
                    </Col>
                  </Row>

                  <Table
                    size="small"
                    rowKey={(record) => record.code}
                    dataSource={availableIcdList}
                    pagination={{
                      pageSize: 5,
                      size: 'small',
                      showSizeChanger: true,
                      pageSizeOptions: ['5', '10', '20'],
                      showTotal: (total, range) => `${range[0]}-${range[1]} / ${total} mã bệnh`,
                    }}
                    columns={[
                      {
                        title: 'Mã ICD',
                        dataIndex: 'code',
                        key: 'code',
                        width: 90,
                        render: (code) => (
                          <Tag color="blue" style={{ fontWeight: 700, fontSize: 12.5, margin: 0 }}>
                            {code}
                          </Tag>
                        ),
                      },
                      {
                        title: 'Tên bệnh / Chẩn đoán y khoa',
                        dataIndex: 'name',
                        key: 'name',
                        render: (name) => (
                          <Text strong style={{ fontSize: 13, color: '#1E293B' }}>
                            {fixMojibake(name)}
                          </Text>
                        ),
                      },
                      {
                        title: 'Nhóm bệnh / Chuyên khoa',
                        dataIndex: 'diseaseGroup',
                        key: 'diseaseGroup',
                        width: 170,
                        render: (_, record) => {
                          const groupName = record.diseaseGroup || getDiseaseGroupName(record.code, record.diseaseGroup)
                          const meta = categoryMeta[record.category]
                          return <Tag color={meta?.color || 'default'} style={{ margin: 0, fontSize: 12 }}>{groupName}</Tag>
                        },
                      },
                      {
                        title: 'Thao tác chọn',
                        key: 'actions',
                        width: 230,
                        align: 'center',
                        render: (_, record) => {
                          const isPrimary = primaryIcd?.code === record.code
                          const isSecondary = secondaryIcds.some((s) => s.code === record.code)

                          return (
                            <Space size={6} wrap>
                              {isPrimary ? (
                                <Tag color="success" style={{ fontWeight: 600, margin: 0, padding: '2px 8px' }}>
                                  <CheckCircleOutlined /> Đang là CĐ chính
                                </Tag>
                              ) : (
                                <Button
                                  size="small"
                                  type="primary"
                                  icon={<CheckCircleOutlined />}
                                  onClick={() => selectPrimaryDiagnosis(record)}
                                >
                                  Chọn CĐ chính
                                </Button>
                              )}

                              {isSecondary ? (
                                <Tag color="purple" style={{ fontWeight: 600, margin: 0, padding: '2px 8px' }}>
                                  <CheckOutlined /> Đã thêm CĐ phụ
                                </Tag>
                              ) : isPrimary ? null : (
                                <Button
                                  size="small"
                                  icon={<PlusOutlined />}
                                  disabled={!primaryIcd}
                                  onClick={() => addSecondaryDiagnosis(record)}
                                >
                                  + CĐ phụ
                                </Button>
                              )}
                            </Space>
                          )
                        },
                      },
                    ]}
                  />
                </>
              )}
            </div>

            <Form.Item name="treatmentPlan" label="Hướng điều trị & Lời dặn của bác sĩ">
              <Input.TextArea
                rows={2}
                placeholder="Chỉ định nhập viện, kê đơn thuốc về nhà, hạn chế ăn mặn, tái khám sau 7 ngày..."
              />
            </Form.Item>
          </Card>
        </Col>
      </Row>

      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ color: '#16A34A' }}><FileTextOutlined /> Nhập Chỉ Định Cận Lâm Sàng</span>
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
        {serviceCatalogError && (
          <div style={{ color: '#b42318', marginBottom: 12 }}>
            {serviceCatalogError}
          </div>
        )}
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={10} style={{ display: 'flex' }}>
            <Card
              size="small"
              title="Danh mục dịch vụ cận lâm sàng"
              style={{ width: '100%', height: '100%', borderRadius: 12, borderColor: '#DCE7E1' }}
              bodyStyle={{ background: '#F8FAFC', borderRadius: '0 0 12px 12px' }}
            >

              <Input
                prefix={<SearchOutlined />}
                placeholder="Tìm xét nghiệm, X-quang, siêu âm..."
                value={orderSearchQuery}
                onChange={(e) => setOrderSearchQuery(e.target.value)}
                style={{ marginBottom: 10 }}
                allowClear
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
                              <Text type={item.price == null ? 'secondary' : 'danger'} strong>
                                {item.price == null ? 'Chưa cập nhật giá' : formatCurrency(item.price)}
                              </Text> | {item.department}
                            </div>
                          }
                        />
                      </List.Item>
                    )
                  }}
                />
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={14} style={{ display: 'flex' }}>
            <Card
              size="small"
              title={`Dịch vụ đã chọn (${selectedOrders.length})`}
              style={{ width: '100%', height: '100%', borderRadius: 12, borderColor: '#DCE7E1' }}
            >
              {selectedOrders.length === 0 ? (
                <div style={{ border: '2px dashed #CBD5E1', padding: 40, textAlign: 'center', borderRadius: 10, color: '#64748B' }}>
                  <FileTextOutlined style={{ fontSize: 32, marginBottom: 8, color: '#94A3B8' }} />
                  <div>Chưa có dịch vụ cận lâm sàng nào được chỉ định.</div>
                  <div style={{ fontSize: 12 }}>Chọn dịch vụ từ danh mục bên trái để thêm vào phiếu.</div>
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
                        title: 'Ưu tiên',
                        key: 'urgent',
                        width: 90,
                        render: (r) => (
                          <Button
                            size="small"
                            type={r.isUrgent ? 'primary' : 'default'}
                            danger={r.isUrgent}
                            onClick={() => handleToggleUrgent(r.code)}
                          >
                            {r.isUrgent ? 'Khẩn' : 'Thường'}
                          </Button>
                        ),
                      },
                      {
                        title: 'Ghi chú kỹ thuật',
                        key: 'note',
                        width: 170,
                        render: (r) => (
                          <Input
                            size="small"
                            placeholder="Nhịn ăn, tư thế chụp..."
                            value={r.note}
                            onChange={(e) => handleUpdateOrderNote(r.code, e.target.value)}
                          />
                        ),
                      },
                      {
                        title: 'Chi phí',
                        dataIndex: 'price',
                        width: 100,
                        align: 'right',
                        render: (val) => (val == null ? 'Chưa cập nhật' : formatCurrency(val)),
                      },
                      {
                        title: '',
                        key: 'action',
                        width: 44,
                        render: (r) => (
                          <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            aria-label={`Xóa dịch vụ ${r.name}`}
                            onClick={() => handleRemoveOrder(r.code)}
                          />
                        ),
                      },
                    ]}
                  />

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginTop: 12, flexWrap: 'wrap' }}>
                    <Button icon={<PrinterOutlined />} onClick={() => setPrintModalOpen(true)}>
                      Xem trước và in phiếu
                    </Button>
                    <Text strong style={{ fontSize: 15 }}>
                      {hasCompletePricing
                        ? <>Tổng tạm tính: <span style={{ color: '#DC2626' }}>{formatCurrency(totalOrderFee)}</span></>
                        : 'Chưa có đủ dữ liệu bảng giá'}
                    </Text>
                  </div>
                </div>
              )}
            </Card>
          </Col>
        </Row>
      </Card>

    </Form>
  )
}

export default MedicalEncounterForm
