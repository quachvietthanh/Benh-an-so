import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  StopOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import medicineApi from '../api/medicineApi'
import { useAuthContext } from '../context/AuthContext'

const { Title, Text, Paragraph } = Typography

// Helper chuẩn hóa văn bản để so sánh trùng lắp
const normalizeText = (value) =>
  String(value ?? '')
    .trim()
    .toLocaleLowerCase('vi-VN')
    .replace(/\s+/g, ' ')

// Nhãn hiển thị tiếng Việt cho DosageForm
const DOSAGE_FORM_LABELS = {
  TABLET: 'Viên nén',
  CAPSULE: 'Viên nang',
  SYRUP: 'Siro',
  SUSPENSION: 'Hỗn dịch',
  SOLUTION: 'Dung dịch',
  INJECTION: 'Dạng tiêm',
  INFUSION: 'Dạng truyền',
  CREAM: 'Kem bôi',
  OINTMENT: 'Thuốc mỡ',
  GEL: 'Gel',
  DROPS: 'Thuốc nhỏ',
  INHALER: 'Dạng hít/xịt',
  POWDER: 'Thuốc bột',
  SUPPOSITORY: 'Thuốc đặt',
  OTHER: 'Khác',
}

// Nhãn hiển thị tiếng Việt cho AdministrationRoute
const ROUTE_LABELS = {
  ORAL: 'Uống',
  SUBLINGUAL: 'Ngậm dưới lưỡi',
  BUCCAL: 'Ngậm má',
  INTRAVENOUS: 'Tiêm tĩnh mạch (IV)',
  INTRAMUSCULAR: 'Tiêm bắp (IM)',
  SUBCUTANEOUS: 'Tiêm dưới da (SC)',
  TOPICAL: 'Bôi ngoài da',
  OPHTHALMIC: 'Nhỏ mắt',
  OTIC: 'Nhỏ tai',
  NASAL: 'Nhỏ/Xịt mũi',
  INHALATION: 'Dạng hít qua hô hấp',
  RECTAL: 'Đặt trực tràng',
  VAGINAL: 'Đặt âm đạo',
  TRANSDERMAL: 'Dán qua da',
  OTHER: 'Khác',
}

function MedicineCatalogPage() {
  const { user: currentUser } = useAuthContext()

  // Phân quyền theo chuẩn Acceptance Criteria:
  // CHỈ PHARMACIST mới có quyền mở và quản lý danh mục thuốc
  const userRoles = Array.isArray(currentUser?.roles)
    ? currentUser.roles
    : currentUser?.role
    ? [currentUser.role]
    : []

  const normalizedRole = String(
    userRoles.find(
      (r) => String(r).toUpperCase().replace(/^ROLE_/, '') === 'PHARMACIST'
    ) ||
      currentUser?.role ||
      ''
  )
    .toUpperCase()
    .replace(/^ROLE_/, '')

  const canManageMedicineCatalog = normalizedRole === 'PHARMACIST'

  // State màn hình
  const [medicines, setMedicines] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  // State bộ lọc và phân trang
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)

  // State Modal
  const [modalOpen, setModalOpen] = useState(false)
  const [editingMedicine, setEditingMedicine] = useState(null)
  const [deactivatingMedicine, setDeactivatingMedicine] = useState(null)

  const [form] = Form.useForm()

  // Hàm tải danh sách thuốc từ Backend API
  const loadMedicines = useCallback(async () => {
    if (!canManageMedicineCatalog) return

    setLoading(true)
    setErrorMessage(null)
    try {
      const activeParam =
        statusFilter === 'ALL'
          ? undefined
          : statusFilter === 'ACTIVE'
          ? true
          : false

      const res = await medicineApi.search({
        keyword: searchKeyword.trim() || undefined,
        active: activeParam,
        page: page - 1,
        size: pageSize,
      })

      const responseData = res?.data
      const content = responseData?.content
        ? responseData.content
        : Array.isArray(responseData)
        ? responseData
        : []

      setMedicines(Array.isArray(content) ? content : [])
      setTotalElements(responseData?.totalElements ?? content.length)
    } catch (err) {
      const status = err.response?.status
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh mục thuốc từ máy chủ.'

      if (status === 403) {
        setErrorMessage('Bạn không có quyền quản lý danh mục thuốc.')
      } else if (status === 401) {
        setErrorMessage('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
      } else {
        setErrorMessage(`Lỗi máy chủ (${status || 500}): ${msg}`)
      }
      setMedicines([])
    } finally {
      setLoading(false)
    }
  }, [canManageMedicineCatalog, searchKeyword, statusFilter, page, pageSize])

  useEffect(() => {
    loadMedicines()
  }, [loadMedicines])

  // Xử lý mở Modal Thêm thuốc
  const openAddModal = () => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }
    setEditingMedicine(null)
    form.resetFields()
    form.setFieldsValue({
      medicineCode: `MED-${Date.now().toString().slice(-6)}`,
      dosageForm: 'TABLET',
      defaultRoute: 'ORAL',
      unit: 'Viên',
    })
    setModalOpen(true)
  }

  // Xử lý mở Modal Sửa thuốc
  const openEditModal = (record) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }
    setEditingMedicine(record)
    form.setFieldsValue({
      medicineCode: record.medicineCode || '',
      medicineName: record.medicineName || '',
      activeIngredient: record.activeIngredient || '',
      strength: record.strength || '',
      dosageForm: record.dosageForm || 'TABLET',
      unit: record.unit || '',
      defaultRoute: record.defaultRoute || 'ORAL',
    })
    setModalOpen(true)
  }

  // Xử lý lưu form Thêm / Sửa thuốc
  const handleSaveMedicine = async (values) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

    const trimmedCode = String(values.medicineCode ?? '').trim()
    const trimmedName = String(values.medicineName ?? '').trim()
    const trimmedActive = String(values.activeIngredient ?? '').trim()
    const trimmedStrength = String(values.strength ?? '').trim()
    const trimmedUnit = String(values.unit ?? '').trim()
    const dosageFormVal = values.dosageForm
    const defaultRouteVal = values.defaultRoute

    // Validations bắt buộc
    if (!trimmedName) {
      message.error('Vui lòng nhập tên thuốc.')
      return
    }
    if (!trimmedUnit) {
      message.error('Vui lòng nhập đơn vị tính.')
      return
    }
    if (!trimmedActive) {
      message.error('Vui lòng nhập hoạt chất.')
      return
    }
    if (!trimmedStrength) {
      message.error('Vui lòng nhập hàm lượng.')
      return
    }
    if (!editingMedicine && !trimmedCode) {
      message.error('Vui lòng nhập mã thuốc.')
      return
    }

    // Kiểm tra trùng lắp ở Frontend (Tên thuốc + Hoạt chất)
    const targetKey =
      normalizeText(trimmedName) + '_' + normalizeText(trimmedActive)

    const isDuplicate = medicines.some((m) => {
      if (editingMedicine && String(m.id) === String(editingMedicine.id)) {
        return false
      }
      const existingKey =
        normalizeText(m.medicineName) + '_' + normalizeText(m.activeIngredient)
      return existingKey === targetKey
    })

    if (isDuplicate) {
      message.warning('Thuốc đã tồn tại trong danh mục.')
      return
    }

    setSubmitting(true)
    try {
      if (editingMedicine) {
        // Payload sửa theo UpdateMedicineRequest DTO
        const updatePayload = {
          medicineName: trimmedName,
          activeIngredient: trimmedActive,
          strength: trimmedStrength,
          dosageForm: dosageFormVal,
          unit: trimmedUnit,
          defaultRoute: defaultRouteVal,
        }
        await medicineApi.update(editingMedicine.id, updatePayload)
        message.success(`Đã cập nhật thuốc ${trimmedName}`)
      } else {
        // Payload thêm theo CreateMedicineRequest DTO
        const createPayload = {
          medicineCode: trimmedCode,
          medicineName: trimmedName,
          activeIngredient: trimmedActive,
          strength: trimmedStrength,
          dosageForm: dosageFormVal,
          unit: trimmedUnit,
          defaultRoute: defaultRouteVal,
        }
        await medicineApi.create(createPayload)
        message.success(`Đã thêm thuốc mới ${trimmedName} vào danh mục thành công`)
      }

      setModalOpen(false)
      setEditingMedicine(null)
      form.resetFields()
      await loadMedicines()
    } catch (err) {
      const status = err.response?.status
      const msg = err.response?.data?.message || err.message

      if (status === 409) {
        message.error(msg || 'Thuốc đã tồn tại trong danh mục.')
      } else if (status === 400) {
        message.error(msg || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.')
      } else {
        message.error(msg || 'Không thể lưu dữ liệu thuốc lên máy chủ.')
      }
      // Không đóng modal và không reset form khi lỗi để người dùng sửa lại
    } finally {
      setSubmitting(false)
    }
  }

  // Xử lý Ngừng sử dụng / Kích hoạt lại thuốc (PATCH status)
  const handleToggleStatus = async (record, targetActiveState) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

    try {
      await medicineApi.updateStatus(record.id, targetActiveState)
      message.success(
        `Đã ${targetActiveState ? 'kích hoạt lại' : 'ngừng sử dụng'} thuốc ${
          record.medicineName
        }`
      )
      setDeactivatingMedicine(null)
      await loadMedicines()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể thay đổi trạng thái thuốc.'
      message.error(msg)
    }
  }

  // Nếu người dùng không phải PHARMACIST (Dược sĩ): Chặn truy cập theo TC-04
  if (!canManageMedicineCatalog) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          showIcon
          icon={<StopOutlined />}
          message="Từ chối truy cập"
          description="Bạn không có quyền quản lý danh mục thuốc. Chức năng này chỉ dành riêng cho Dược sĩ (PHARMACIST)."
          style={{ maxWidth: 600, margin: '40px auto' }}
        />
      </div>
    )
  }

  const safeMedicines = Array.isArray(medicines) ? medicines : []

  const columns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'medicineCode',
      key: 'medicineCode',
      width: 130,
      render: (v) => <Text code>{v || '—'}</Text>,
    },
    {
      title: 'Tên thuốc',
      dataIndex: 'medicineName',
      key: 'medicineName',
      render: (v) => <strong>{v || '—'}</strong>,
    },
    {
      title: 'Hoạt chất',
      dataIndex: 'activeIngredient',
      key: 'activeIngredient',
      render: (v) => v || '—',
    },
    {
      title: 'Hàm lượng',
      dataIndex: 'strength',
      key: 'strength',
      width: 120,
      render: (v) => v || '—',
    },
    {
      title: 'Dạng bào chế',
      dataIndex: 'dosageForm',
      key: 'dosageForm',
      width: 130,
      render: (v) => (v ? DOSAGE_FORM_LABELS[v] || v : '—'),
    },
    {
      title: 'Đường dùng',
      dataIndex: 'defaultRoute',
      key: 'defaultRoute',
      width: 150,
      render: (v) => (v ? ROUTE_LABELS[v] || v : '—'),
    },
    {
      title: 'Đơn vị tính',
      dataIndex: 'unit',
      key: 'unit',
      width: 110,
      render: (v) => v || '—',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 130,
      render: (val) => (
        <Tag color={val !== false ? 'green' : 'default'}>
          {val !== false ? 'Đang dùng' : 'Ngừng dùng'}
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 180,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <Button
            icon={<EditOutlined />}
            size="small"
            onClick={() => openEditModal(record)}
          >
            Sửa
          </Button>

          {record.active !== false ? (
            <Button
              danger
              size="small"
              icon={<StopOutlined />}
              onClick={() => setDeactivatingMedicine(record)}
            >
              Ngừng dùng
            </Button>
          ) : (
            <Button
              type="primary"
              ghost
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => handleToggleStatus(record, true)}
            >
              Kích hoạt lại
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      {/* Tiêu đề & Mô tả */}
      <div style={{ marginBottom: 20 }}>
        <Title level={3} style={{ margin: 0 }}>
          <MedicineBoxOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          Quản lý danh mục thuốc
        </Title>
        <Paragraph type="secondary" style={{ marginTop: 4, marginBottom: 0 }}>
          Danh mục thuốc dùng chung cho kê đơn và quản lý kho.
        </Paragraph>
      </div>

      {/* Hiển thị lỗi API nếu có */}
      {errorMessage && (
        <Alert
          type="error"
          showIcon
          message={errorMessage}
          action={
            <Button
              size="small"
              type="primary"
              danger
              icon={<ReloadOutlined />}
              onClick={loadMedicines}
            >
              Thử lại
            </Button>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Card Danh sách thuốc */}
      <Card
        title={
          <Space wrap>
            <Input.Search
              placeholder="Tìm theo tên thuốc hoặc hoạt chất..."
              allowClear
              style={{ width: 320 }}
              onSearch={(val) => {
                setSearchKeyword(val)
                setPage(1)
              }}
              onChange={(e) => {
                if (!e.target.value) {
                  setSearchKeyword('')
                  setPage(1)
                }
              }}
            />
            <Select
              defaultValue="ALL"
              style={{ width: 160 }}
              onChange={(val) => {
                setStatusFilter(val)
                setPage(1)
              }}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Đang dùng' },
                { value: 'INACTIVE', label: 'Ngừng dùng' },
              ]}
            />
          </Space>
        }
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openAddModal}
          >
            Thêm thuốc mới
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={safeMedicines}
          loading={loading}
          pagination={false}
          locale={{ emptyText: 'Không tìm thấy thuốc nào trong danh mục' }}
        />

        <div
          style={{
            marginTop: 16,
            display: 'flex',
            justifyContent: 'flex-end',
          }}
        >
          <Pagination
            current={page}
            pageSize={pageSize}
            total={totalElements}
            showSizeChanger
            pageSizeOptions={['10', '20', '50', '100']}
            onChange={(p, ps) => {
              setPage(p)
              setPageSize(ps)
            }}
          />
        </div>
      </Card>

      {/* Modal Thêm / Sửa thuốc */}
      <Modal
        title={
          editingMedicine
            ? `Sửa thông tin thuốc: ${editingMedicine.medicineName}`
            : 'Thêm thuốc mới vào danh mục'
        }
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false)
          setEditingMedicine(null)
        }}
        onOk={() => form.submit()}
        confirmLoading={submitting}
        okText={editingMedicine ? 'Cập nhật' : 'Thêm mới'}
        cancelText="Hủy"
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveMedicine}
          initialValues={{
            dosageForm: 'TABLET',
            defaultRoute: 'ORAL',
            unit: 'Viên',
          }}
        >
          <Form.Item
            name="medicineCode"
            label="Mã thuốc"
            rules={[{ required: true, message: 'Vui lòng nhập mã thuốc.' }]}
          >
            <Input
              placeholder="Nhập mã thuốc (VD: MED-001)"
              disabled={!!editingMedicine}
            />
          </Form.Item>

          <Form.Item
            name="medicineName"
            label="Tên thuốc"
            rules={[{ required: true, message: 'Vui lòng nhập tên thuốc.' }]}
          >
            <Input placeholder="Nhập tên thuốc (VD: Paracetamol 500mg)" />
          </Form.Item>

          <Form.Item
            name="activeIngredient"
            label="Hoạt chất"
            rules={[{ required: true, message: 'Vui lòng nhập hoạt chất.' }]}
          >
            <Input placeholder="Nhập hoạt chất (VD: Paracetamol)" />
          </Form.Item>

          <Form.Item
            name="strength"
            label="Hàm lượng"
            rules={[{ required: true, message: 'Vui lòng nhập hàm lượng.' }]}
          >
            <Input placeholder="Nhập hàm lượng (VD: 500 mg, 10mg/5ml...)" />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              name="dosageForm"
              label="Dạng bào chế"
              rules={[{ required: true, message: 'Vui lòng chọn dạng bào chế.' }]}
              style={{ flex: 1 }}
            >
              <Select
                options={Object.entries(DOSAGE_FORM_LABELS).map(
                  ([key, val]) => ({
                    value: key,
                    label: val,
                  })
                )}
              />
            </Form.Item>

            <Form.Item
              name="defaultRoute"
              label="Đường dùng"
              rules={[{ required: true, message: 'Vui lòng chọn đường dùng.' }]}
              style={{ flex: 1 }}
            >
              <Select
                options={Object.entries(ROUTE_LABELS).map(([key, val]) => ({
                  value: key,
                  label: val,
                }))}
              />
            </Form.Item>
          </Space>

          <Form.Item
            name="unit"
            label="Đơn vị tính"
            rules={[{ required: true, message: 'Vui lòng nhập đơn vị tính.' }]}
          >
            <Input placeholder="Nhập đơn vị tính (Viên / Chai / Tuýp / Gói...)" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal Xác nhận Ngừng sử dụng thuốc */}
      <Modal
        title="Xác nhận ngừng sử dụng thuốc"
        open={!!deactivatingMedicine}
        onCancel={() => setDeactivatingMedicine(null)}
        onOk={() =>
          deactivatingMedicine && handleToggleStatus(deactivatingMedicine, false)
        }
        okText="Xác nhận ngừng dùng"
        okButtonProps={{ danger: true }}
        cancelText="Hủy"
      >
        <p>
          Bạn có chắc chắn muốn <strong>ngừng sử dụng</strong> thuốc{' '}
          <strong>{deactivatingMedicine?.medicineName}</strong> (Hoạt chất:{' '}
          <em>{deactivatingMedicine?.activeIngredient}</em>) không?
        </p>
        <p style={{ color: '#8c8c8c', fontSize: 13 }}>
          * Thuốc sẽ chuyển sang trạng thái <em>Ngừng dùng</em> và không thể chọn
          khi kê đơn mới, nhưng dữ liệu vẫn lưu vết trong lịch sử hệ thống.
        </p>
      </Modal>
    </div>
  )
}

export default MedicineCatalogPage
