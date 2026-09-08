import React, { useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FireOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import patientAllergyApi from '../../api/patientAllergyApi'
import {
  ALLERGY_SEVERITIES,
  ALLERGY_SEVERITY_OPTIONS,
  COMMON_MEDICATION_ALLERGENS,
  getAllergySeverityMeta,
} from '../../utils/allergyConstants'
import { getApiErrorMessage } from '../../utils/apiError'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

export default function PatientAllergyModal({
  open,
  onClose,
  patientId,
  patientName = '',
  visitId = null,
  onAllergiesUpdated,
  onAllergiesChange,
  currentUser = null,
  canWrite = true,
}) {
  const [activeTab, setActiveTab] = useState('list')
  const [allergies, setAllergies] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // Form thêm mới
  const [addForm] = Form.useForm()

  // State chỉnh sửa
  const [editingAllergy, setEditingAllergy] = useState(null)
  const [editForm] = Form.useForm()
  const [editModalOpen, setEditModalOpen] = useState(false)

  // State xóa
  const [deletingAllergy, setDeletingAllergy] = useState(null)
  const [deleteReason, setDeleteReason] = useState('')
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)

  // State lịch sử thay đổi
  const [historyModalOpen, setHistoryModalOpen] = useState(false)
  const [selectedAllergyForHistory, setSelectedAllergyForHistory] = useState(null)
  const [changeLogs, setChangeLogs] = useState([])
  const [loadingLogs, setLoadingLogs] = useState(false)

  const loadAllergies = async () => {
    if (!patientId) return
    setLoading(true)
    try {
      const res = await patientAllergyApi.getAllergies(patientId)
      const list = Array.isArray(res?.data) ? res.data : []
      setAllergies(list)
      if (onAllergiesUpdated) {
        onAllergiesUpdated(list)
      }
      if (onAllergiesChange) {
        onAllergiesChange(list)
      }
    } catch (err) {
      console.warn('Lỗi nạp danh sách dị ứng thuốc:', err)
      const msg = getApiErrorMessage(err, 'Không thể tải danh sách dị ứng thuốc.')
      // Không chặn giao diện
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && patientId) {
      loadAllergies()
    }
  }, [open, patientId])

  // Xử lý thêm mới dị ứng
  const handleAddAllergy = async (values) => {
    if (!patientId) return
    setSubmitting(true)
    try {
      const isUuid = (val) =>
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(val || ''))

      const payload = {
        allergenType: 'MEDICATION',
        allergenName: values.allergenName?.trim(),
        severity: values.severity,
        reaction: values.reaction?.trim() || '',
        notes: values.notes?.trim() || '',
        ...(visitId && isUuid(visitId) ? { visitId } : {}),
      }

      try {
        await patientAllergyApi.addAllergy(patientId, payload)
      } catch (postErr) {
        // Nếu truyền visitId bị lỗi không tìm thấy lượt khám (404), tự động thử lại không kèm visitId
        if (payload.visitId && (postErr.response?.status === 404 || postErr.response?.data?.code === 'VISIT_NOT_FOUND')) {
          const { visitId: _omitted, ...fallbackPayload } = payload
          await patientAllergyApi.addAllergy(patientId, fallbackPayload)
        } else {
          throw postErr
        }
      }

      message.success(`Đã thêm tiền sử dị ứng thuốc "${payload.allergenName}" thành công!`)
      addForm.resetFields()
      await loadAllergies()
      setActiveTab('list')
    } catch (err) {
      const msg = getApiErrorMessage(
        err,
        'Không thể thêm dị ứng thuốc. Vui lòng kiểm tra lại thông tin.'
      )
      message.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  // Mở modal sửa
  const handleOpenEdit = (allergy) => {
    setEditingAllergy(allergy)
    editForm.setFieldsValue({
      allergenName: allergy.allergenName,
      severity: allergy.severity,
      reaction: allergy.reaction || '',
      notes: allergy.notes || '',
      changeReason: '',
    })
    setEditModalOpen(true)
  }

  // Xử lý cập nhật
  const handleUpdateAllergy = async (values) => {
    if (!patientId || !editingAllergy?.id) return
    setSubmitting(true)
    try {
      const payload = {
        allergenName: values.allergenName?.trim(),
        severity: values.severity,
        reaction: values.reaction?.trim() || '',
        notes: values.notes?.trim() || '',
        changeReason: values.changeReason?.trim() || 'Bác sĩ cập nhật thông tin dị ứng',
      }

      await patientAllergyApi.updateAllergy(patientId, editingAllergy.id, payload)
      message.success(`Đã cập nhật thông tin dị ứng "${payload.allergenName}" thành công!`)
      setEditModalOpen(false)
      setEditingAllergy(null)
      editForm.resetFields()
      await loadAllergies()
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Không thể cập nhật dị ứng thuốc.')
      message.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  // Mở modal xóa
  const handleOpenDelete = (allergy) => {
    setDeletingAllergy(allergy)
    setDeleteReason('')
    setDeleteModalOpen(true)
  }

  // Xử lý xóa
  const handleConfirmDelete = async () => {
    if (!patientId || !deletingAllergy?.id) return
    if (!deleteReason?.trim()) {
      message.warning('Vui lòng nhập lý do xóa/vô hiệu hóa dị ứng để lưu vết.')
      return
    }

    setSubmitting(true)
    try {
      await patientAllergyApi.deleteAllergy(patientId, deletingAllergy.id, deleteReason.trim())
      message.success(`Đã vô hiệu hóa mục dị ứng "${deletingAllergy.allergenName}" thành công!`)
      setDeleteModalOpen(false)
      setDeletingAllergy(null)
      setDeleteReason('')
      await loadAllergies()
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Không thể xóa mục dị ứng thuốc.')
      message.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  // Mở xem lịch sử lưu vết
  const handleOpenHistory = async (allergy) => {
    setSelectedAllergyForHistory(allergy)
    setHistoryModalOpen(true)
    setLoadingLogs(true)
    try {
      const res = await patientAllergyApi.getChangeLogs(patientId, allergy.id)
      setChangeLogs(Array.isArray(res?.data) ? res.data : [])
    } catch (err) {
      console.warn('Lỗi nạp lịch sử thay đổi dị ứng:', err)
      const msg = getApiErrorMessage(err, 'Không thể tải lịch sử lưu vết thay đổi.')
      message.warning(msg)
    } finally {
      setLoadingLogs(false)
    }
  }

  const columns = [
    {
      title: 'Hoạt chất / Nhóm thuốc dị ứng',
      dataIndex: 'allergenName',
      key: 'allergenName',
      render: (name, record) => {
        const severityMeta = getAllergySeverityMeta(record.severity)
        const isAnaphylaxis = record.severity === 'ANAPHYLAXIS'
        return (
          <div>
            <Space align="center">
              {isAnaphylaxis ? (
                <FireOutlined style={{ color: '#dc2626', fontSize: 16 }} />
              ) : (
                <MedicineBoxOutlined style={{ color: '#2563eb' }} />
              )}
              <Text strong style={{ fontSize: 14, color: isAnaphylaxis ? '#b91c1c' : '#1e293b' }}>
                {name}
              </Text>
            </Space>
            {record.reaction && (
              <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                Biểu hiện: <i>{record.reaction}</i>
              </div>
            )}
          </div>
        )
      },
    },
    {
      title: 'Mức độ phản ứng',
      dataIndex: 'severity',
      key: 'severity',
      width: 170,
      render: (severity) => {
        const meta = getAllergySeverityMeta(severity)
        return (
          <Tag
            color={meta.color}
            style={{
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: 4,
              fontSize: 12,
            }}
          >
            {meta.label}
          </Tag>
        )
      },
    },
    {
      title: 'Ghi chú lâm sàng',
      dataIndex: 'notes',
      key: 'notes',
      render: (notes) => notes || <Text type="secondary">—</Text>,
    },
    {
      title: 'Thời điểm ghi nhận',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (createdAt) => (
        <span style={{ fontSize: 12, color: '#64748b' }}>
          {createdAt ? dayjs(createdAt).format('DD/MM/YYYY HH:mm') : '—'}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 160,
      align: 'center',
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="Xem lịch sử thay đổi (Audit Log)">
            <Button
              size="small"
              icon={<HistoryOutlined />}
              onClick={() => handleOpenHistory(record)}
            />
          </Tooltip>
          {canWrite && (
            <>
              <Tooltip title="Sửa thông tin dị ứng (Lưu vết lý do)">
                <Button
                  size="small"
                  type="primary"
                  ghost
                  icon={<EditOutlined />}
                  onClick={() => handleOpenEdit(record)}
                />
              </Tooltip>
              <Tooltip title="Vô hiệu hóa / Xóa mục dị ứng">
                <Button
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => handleOpenDelete(record)}
                />
              </Tooltip>
            </>
          )}
        </Space>
      ),
    },
  ]

  const hasAnaphylaxis = allergies.some((a) => a.severity === 'ANAPHYLAXIS')
  const hasSevere = allergies.some((a) => a.severity === 'SEVERE')

  return (
    <>
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                background: hasAnaphylaxis
                  ? 'linear-gradient(135deg, #ef4444 0%, #b91c1c 100%)'
                  : 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontSize: 18,
                boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)',
              }}
            >
              <AlertOutlined />
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
                Quản lý Tiền sử Dị ứng thuốc
              </div>
              <div style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
                Bệnh nhân: <strong>{patientName || 'Hồ sơ bệnh nhân'}</strong> • Mã BN: {patientId?.slice(0, 8)}
              </div>
            </div>
          </div>
        }
        open={open}
        onCancel={onClose}
        width={860}
        style={{ top: 24 }}
        footer={[
          <Button key="close" onClick={onClose}>
            Đóng
          </Button>,
          activeTab === 'list' && canWrite && (
            <Button
              key="addNew"
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setActiveTab('add')}
              style={{ background: '#dc2626', borderColor: '#dc2626', fontWeight: 600 }}
            >
              Khai báo dị ứng mới
            </Button>
          ),
        ]}
      >
        {/* Banner cảnh báo an toàn chuyên môn */}
        {hasAnaphylaxis ? (
          <Alert
            type="error"
            showIcon
            icon={<FireOutlined style={{ fontSize: 20 }} />}
            message={
              <strong style={{ fontSize: 14 }}>
                CẢNH BÁO NGUY HIỂM: Bệnh nhân có tiền sử SỐC PHẢN VỆ với thuốc!
              </strong>
            }
            description="Tuyệt đối kiểm tra kỹ hoạt chất trước khi chỉ định, kê đơn hoặc sử dụng bất kỳ thuốc tiêm/uống nào cho bệnh nhân này."
            style={{ marginBottom: 16, border: '1px solid #f87171', background: '#fef2f2' }}
          />
        ) : hasSevere ? (
          <Alert
            type="warning"
            showIcon
            icon={<WarningOutlined style={{ fontSize: 18 }} />}
            message={<strong>Bệnh nhân có tiền sử dị ứng thuốc mức độ NẶNG</strong>}
            description="Bác sĩ cần rà soát kỹ hoạt chất và các phản ứng chéo cùng nhóm thuốc khi kê đơn."
            style={{ marginBottom: 16, border: '1px solid #fed7aa', background: '#fff7ed' }}
          />
        ) : null}

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'list',
              label: (
                <span>
                  <SafetyCertificateOutlined /> Danh sách dị ứng đã ghi nhận ({allergies.length})
                </span>
              ),
              children: (
                <div>
                  <Table
                    columns={columns}
                    dataSource={allergies}
                    rowKey="id"
                    loading={loading}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                          description="Bệnh nhân chưa có ghi nhận dị ứng thuốc nào"
                        >
                          {canWrite && (
                            <Button
                              type="dashed"
                              icon={<PlusOutlined />}
                              onClick={() => setActiveTab('add')}
                            >
                              Khai báo dị ứng đầu tiên
                            </Button>
                          )}
                        </Empty>
                      ),
                    }}
                  />
                  <div
                    style={{
                      marginTop: 12,
                      fontSize: 12,
                      color: '#64748b',
                      background: '#f8fafc',
                      padding: '8px 12px',
                      borderRadius: 6,
                    }}
                  >
                    ℹ️ Danh sách dị ứng thuốc được lưu cố định vào hồ sơ và tự động cảnh báo ở mọi lượt khám, kê đơn tiếp theo của bệnh nhân.
                  </div>
                </div>
              ),
            },
            canWrite && {
              key: 'add',
              label: (
                <span>
                  <PlusOutlined /> Khai báo hoạt chất dị ứng mới
                </span>
              ),
              children: (
                <div style={{ paddingTop: 8 }}>
                  <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                    Chọn nhanh các nhóm thuốc thường gặp hoặc nhập trực tiếp tên hoạt chất gây dị ứng:
                  </Paragraph>

                  {/* Gợi ý nhóm thuốc nhanh */}
                  <div style={{ marginBottom: 16, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    {COMMON_MEDICATION_ALLERGENS.map((common) => (
                      <Tag
                        key={common}
                        style={{
                          cursor: 'pointer',
                          padding: '3px 10px',
                          borderRadius: 12,
                          background: '#f1f5f9',
                          borderColor: '#cbd5e1',
                          color: '#334155',
                          fontSize: 12,
                        }}
                        onClick={() => addForm.setFieldsValue({ allergenName: common })}
                      >
                        + {common}
                      </Tag>
                    ))}
                  </div>

                  <Form
                    form={addForm}
                    layout="vertical"
                    onFinish={handleAddAllergy}
                    initialValues={{ severity: 'MODERATE' }}
                  >
                    <Row gutter={16}>
                      <Col span={14}>
                        <Form.Item
                          name="allergenName"
                          label="Tên hoạt chất hoặc nhóm thuốc gây dị ứng"
                          rules={[
                            { required: true, message: 'Vui lòng nhập tên hoạt chất hoặc nhóm thuốc' },
                            { max: 255, message: 'Tên không vượt quá 255 ký tự' },
                          ]}
                        >
                          <Input
                            placeholder="Ví dụ: Amoxicillin, Aspirin, Ibuprofen, Paracetamol..."
                            size="large"
                          />
                        </Form.Item>
                      </Col>
                      <Col span={10}>
                        <Form.Item
                          name="severity"
                          label="Mức độ nghiêm trọng của phản ứng"
                          rules={[{ required: true, message: 'Vui lòng chọn mức độ phản ứng' }]}
                        >
                          <Select size="large" options={ALLERGY_SEVERITY_OPTIONS} />
                        </Form.Item>
                      </Col>
                    </Row>

                    <Form.Item
                      name="reaction"
                      label="Biểu hiện phản ứng lâm sàng"
                      tooltip="Mô tả triệu chứng bệnh nhân từng gặp khi dùng thuốc"
                    >
                      <Input placeholder="Ví dụ: Nổi mề đay toàn thân, ngứa rát, phù mi mắt, khó thở nhẹ..." />
                    </Form.Item>

                    <Form.Item
                      name="notes"
                      label="Ghi chú lâm sàng / Tiền sử chi tiết"
                    >
                      <TextArea
                        rows={2}
                        placeholder="Ví dụ: Xảy ra năm 2023 sau khi uống 1 viên Clamoxyl tại nhà, đã cấp cứu tiêm kháng histamin..."
                      />
                    </Form.Item>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                      <Button onClick={() => setActiveTab('list')}>Hủy</Button>
                      <Button
                        type="primary"
                        htmlType="submit"
                        loading={submitting}
                        icon={<CheckCircleOutlined />}
                        style={{ background: '#dc2626', borderColor: '#dc2626', fontWeight: 600 }}
                      >
                        Lưu tiền sử dị ứng thuốc
                      </Button>
                    </div>
                  </Form>
                </div>
              ),
            },
          ].filter(Boolean)}
        />
      </Modal>

      {/* Modal Chỉnh sửa thông tin dị ứng */}
      <Modal
        title={
          <Space>
            <EditOutlined style={{ color: '#2563eb' }} />
            <span>Chỉnh sửa thông tin dị ứng thuốc</span>
          </Space>
        }
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        footer={null}
        width={600}
      >
        <Form form={editForm} layout="vertical" onFinish={handleUpdateAllergy}>
          <Form.Item
            name="allergenName"
            label="Tên hoạt chất / Nhóm thuốc"
            rules={[{ required: true, message: 'Vui lòng nhập tên hoạt chất' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            name="severity"
            label="Mức độ phản ứng"
            rules={[{ required: true, message: 'Vui lòng chọn mức độ phản ứng' }]}
          >
            <Select options={ALLERGY_SEVERITY_OPTIONS} />
          </Form.Item>

          <Form.Item name="reaction" label="Biểu hiện phản ứng">
            <Input />
          </Form.Item>

          <Form.Item name="notes" label="Ghi chú">
            <TextArea rows={2} />
          </Form.Item>

          <Form.Item
            name="changeReason"
            label={
              <span>
                <strong style={{ color: '#dc2626' }}>* Lý do thay đổi (Bắt buộc để lưu vết)</strong>
              </span>
            }
            rules={[{ required: true, message: 'Vui lòng nhập lý do thay đổi để lưu vết audit log' }]}
            tooltip="Quy định bắt buộc: Mọi lần sửa đổi tiền sử dị ứng thuốc đều được hệ thống lưu vết cùng thời điểm và danh tính Bác sĩ."
          >
            <Input placeholder="Ví dụ: Bổ sung biểu hiện lâm sàng sau khi hỏi bệnh sử..." />
          </Form.Item>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
            <Button onClick={() => setEditModalOpen(false)}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Cập nhật & Lưu vết
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Modal Vô hiệu hóa / Xóa dị ứng */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: '#dc2626' }} />
            <span>Xác nhận vô hiệu hóa mục dị ứng thuốc</span>
          </Space>
        }
        open={deleteModalOpen}
        onCancel={() => setDeleteModalOpen(false)}
        onOk={handleConfirmDelete}
        confirmLoading={submitting}
        okText="Xác nhận vô hiệu hóa"
        okButtonProps={{ danger: true }}
        cancelText="Hủy"
      >
        <Paragraph>
          Bạn có chắc chắn muốn vô hiệu hóa mục dị ứng thuốc{' '}
          <strong>"{deletingAllergy?.allergenName}"</strong> không?
        </Paragraph>
        <div style={{ marginBottom: 8 }}>
          <Text strong style={{ color: '#dc2626' }}>
            * Vui lòng nhập lý do vô hiệu hóa (Bắt buộc):
          </Text>
        </div>
        <TextArea
          rows={2}
          value={deleteReason}
          onChange={(e) => setDeleteReason(e.target.value)}
          placeholder="Ví dụ: Bệnh nhân làm test lẩy da kết quả âm tính, không còn dị ứng..."
        />
        <div style={{ marginTop: 8, fontSize: 12, color: '#64748b' }}>
          Lưu ý: Thao tác này sẽ được lưu vết vào nhật ký kiểm toán hệ thống.
        </div>
      </Modal>

      {/* Modal Xem lịch sử lưu vết (Audit Trail Change Logs) */}
      <Modal
        title={
          <Space>
            <HistoryOutlined style={{ color: '#2563eb' }} />
            <span>Lịch sử lưu vết thay đổi dị ứng thuốc</span>
          </Space>
        }
        open={historyModalOpen}
        onCancel={() => setHistoryModalOpen(false)}
        footer={<Button onClick={() => setHistoryModalOpen(false)}>Đóng</Button>}
        width={680}
      >
        <div style={{ marginBottom: 12 }}>
          <Text strong>Hoạt chất:</Text> {selectedAllergyForHistory?.allergenName} •{' '}
          <Text strong>Mã mục:</Text> {selectedAllergyForHistory?.id?.slice(0, 8)}
        </div>

        {loadingLogs ? (
          <div style={{ padding: 24, textAlign: 'center' }}>Đang nạp lịch sử thay đổi...</div>
        ) : changeLogs.length === 0 ? (
          <Empty description="Chưa có nhật ký thay đổi nào cho mục dị ứng này" />
        ) : (
          <Timeline
            style={{ marginTop: 16 }}
            items={changeLogs.map((log) => {
              const isCreate = log.action === 'CREATE'
              const isDelete = log.action === 'DELETE'
              const color = isCreate ? 'green' : isDelete ? 'red' : 'blue'
              const actionText = isCreate
                ? 'Ghi nhận mới'
                : isDelete
                ? 'Vô hiệu hóa / Xóa'
                : 'Cập nhật chỉnh sửa'

              return {
                color,
                children: (
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <Tag color={color}>{actionText}</Tag>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {log.changedAt ? dayjs(log.changedAt).format('DD/MM/YYYY HH:mm:ss') : '—'}
                      </Text>
                      {log.changedBy && (
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          (Người ghi: {String(log.changedBy).slice(0, 8)})
                        </Text>
                      )}
                    </div>
                    {log.changeReason && (
                      <div style={{ marginTop: 4, fontSize: 13 }}>
                        <strong>Lý do:</strong> <i>{log.changeReason}</i>
                      </div>
                    )}
                    {log.beforeData && (
                      <div
                        style={{
                          marginTop: 4,
                          background: '#f8fafc',
                          padding: 6,
                          borderRadius: 4,
                          fontSize: 12,
                          color: '#64748b',
                        }}
                      >
                        <div>
                          <strong>Dữ liệu trước sửa:</strong>{' '}
                          {typeof log.beforeData === 'object'
                            ? `${log.beforeData.allergenName || ''} (${log.beforeData.severity || ''}) - ${log.beforeData.reaction || ''}`
                            : String(log.beforeData)}
                        </div>
                        {log.afterData && (
                          <div style={{ marginTop: 2 }}>
                            <strong>Dữ liệu sau sửa:</strong>{' '}
                            {typeof log.afterData === 'object'
                              ? `${log.afterData.allergenName || ''} (${log.afterData.severity || ''}) - ${log.afterData.reaction || ''}`
                              : String(log.afterData)}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ),
              }
            })}
          />
        )}
      </Modal>
    </>
  )
}
