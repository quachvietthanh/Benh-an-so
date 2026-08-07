import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'

const parseItems = (value) => {
  try {
    return typeof value === 'string' ? JSON.parse(value) : (value || [])
  } catch {
    return []
  }
}

const normalizeText = (value) =>
  String(value ?? '')
    .trim()
    .toLocaleLowerCase('vi-VN')
    .replace(/\s+/g, ' ')

function PharmacyPage() {
  const { user: currentUser } = useAuthContext()

  const [medicines, setMedicines] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(false)
  const [medicineOpen, setMedicineOpen] = useState(false)
  const [editingMedicine, setEditingMedicine] = useState(null)
  const [deletingMedicine, setDeletingMedicine] = useState(null)
  const [dispensingId, setDispensingId] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const [medicineForm] = Form.useForm()

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [medicineRes, prescriptionRes] = await Promise.allSettled([
        pharmacyApi.medicines(),
        pharmacyApi.prescriptions(),
      ])

      const rawMeds = medicineRes.status === 'fulfilled' ? (medicineRes.value.data?.content || medicineRes.value.data || []) : []
      const apiMeds = Array.isArray(rawMeds) ? rawMeds : []
      const apiPrescs = prescriptionRes.status === 'fulfilled' ? (prescriptionRes.value.data || []) : []

      setMedicines(apiMeds)
      setPrescriptions(apiPrescs)
    } catch {
      setMedicines([])
      setPrescriptions([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  const handleSaveMedicine = async (values) => {
    const trimmedName = String(values.name ?? '').trim()
    const trimmedUnit = String(values.unit ?? '').trim()
    const trimmedActive = String(values.activeIngredient ?? '').trim()

    if (!trimmedName) {
      message.error('Vui lòng nhập tên thuốc.')
      return
    }
    if (!trimmedUnit) {
      message.error('Vui lòng nhập đơn vị tính.')
      return
    }

    const targetKey = normalizeText(trimmedName) + '_' + normalizeText(trimmedActive)
    const isDuplicate = medicines.some((m) => {
      if (editingMedicine && m.id === editingMedicine.id) return false
      const existingKey = normalizeText(m.name || m.medicineName) + '_' + normalizeText(m.activeIngredient)
      return existingKey === targetKey
    })

    if (isDuplicate) {
      message.warning('Thuốc đã tồn tại trong danh mục.')
      return
    }

    try {
      if (editingMedicine) {
        const payload = {
          ...editingMedicine,
          ...values,
          medicineName: trimmedName,
          name: trimmedName,
          unit: trimmedUnit,
          activeIngredient: trimmedActive,
        }
        await pharmacyApi.updateMedicine(editingMedicine.id, payload)
        message.success(`Đã cập nhật thông tin thuốc ${trimmedName}`)
      } else {
        const newMed = {
          medicineCode: `MED-${Date.now().toString().slice(-6)}`,
          medicineName: trimmedName,
          name: trimmedName,
          unit: trimmedUnit,
          activeIngredient: trimmedActive,
          strength: values.strength || '',
          dosageForm: values.dosageForm || 'TABLET',
          active: true,
        }
        await pharmacyApi.createMedicine(newMed)
        message.success(`Đã thêm thuốc mới ${trimmedName} vào danh mục`)
      }
      setMedicineOpen(false)
      setEditingMedicine(null)
      medicineForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.response?.data?.message || error.message || 'Không thể lưu thông tin thuốc trên Backend')
    }
  }

  const handleDeactivateMedicine = async (medRecord) => {
    try {
      if (medRecord.id) {
        await pharmacyApi.updateMedicineStatus(medRecord.id, false)
      }
      message.success(`Đã ngừng sử dụng thuốc ${medRecord.name || medRecord.medicineName}`)
      setDeletingMedicine(null)
      await loadData()
    } catch (error) {
      message.error(error.response?.data?.message || error.message || 'Không thể cập nhật trạng thái thuốc')
    }
  }

  const handleDispense = async (prescription) => {
    setDispensingId(prescription.id)
    try {
      await pharmacyApi.dispense(prescription.id)
      message.success(`Đã cấp phát thành công đơn thuốc ${prescription.prescriptionCode}.`)
      await loadData()
    } catch (error) {
      message.error(error.response?.data?.message || error.message || 'Không thể cấp phát đơn thuốc trên Backend')
    } finally {
      setDispensingId(null)
    }
  }

  const openAddMedicine = () => {
    setEditingMedicine(null)
    medicineForm.resetFields()
    setMedicineOpen(true)
  }

  const openEditMedicine = (record) => {
    setEditingMedicine(record)
    medicineForm.setFieldsValue({
      name: record.name || record.medicineName,
      activeIngredient: record.activeIngredient || '',
      category: record.category || '',
      unit: record.unit || 'vien',
      minStock: record.minStock || 0,
      active: record.active !== false,
    })
    setMedicineOpen(true)
  }

  const filteredMedicines = medicines.filter((m) => {
    const medName = String(m.name || m.medicineName || '').toLowerCase()
    const activeIng = String(m.activeIngredient || '').toLowerCase()
    const kw = searchKeyword.trim().toLowerCase()

    const matchesKw = !kw || medName.includes(kw) || activeIng.includes(kw)
    const matchesStatus =
      statusFilter === 'ALL'
        ? true
        : statusFilter === 'ACTIVE'
        ? m.active !== false
        : m.active === false

    return matchesKw && matchesStatus
  })

  const lowStockMedicines = medicines.filter(
    (m) => m.active !== false && Number(m.stock || 0) <= Number(m.minStock || 0),
  )

  const medicineColumns = [
    {
      title: 'Tên thuốc',
      dataIndex: 'name',
      key: 'name',
      render: (val, record) => <strong>{val || record.medicineName || '—'}</strong>,
    },
    {
      title: 'Hoạt chất',
      dataIndex: 'activeIngredient',
      key: 'activeIngredient',
      render: (v) => v || '—',
    },
    { title: 'Nhóm thuốc', dataIndex: 'category', key: 'category', render: (v) => v || '—' },
    { title: 'Đơn vị tính', dataIndex: 'unit', key: 'unit', render: (v) => v || '—' },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      render: (val) => (
        <Tag color={val !== false ? 'green' : 'default'}>
          {val !== false ? 'Đang dùng' : 'Ngừng dùng'}
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            icon={<EditOutlined />}
            size="small"
            onClick={() => openEditMedicine(record)}
          >
            Sửa
          </Button>
          {record.active !== false && (
            <Button
              danger
              size="small"
              onClick={() => setDeletingMedicine(record)}
            >
              Ngừng dùng
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const prescriptionColumns = [
    { title: 'Mã đơn thuốc', dataIndex: 'prescriptionCode', key: 'prescriptionCode', render: (v) => <strong>{v}</strong> },
    { title: 'Bệnh nhân', dataIndex: 'patientName', key: 'patientName', render: (v) => v || '—' },
    {
      title: 'Danh sách thuốc trong đơn',
      dataIndex: 'items',
      key: 'items',
      render: (val) => (
        parseItems(val).map((item) => {
          const med = medicines.find((m) => m.id === item.medicineId)
          return `${med ? (med.name || med.medicineName) : item.medicineId} (x${item.quantity} ${item.dosage ? `— ${item.dosage}` : ''})`
        }).join(' | ') || '—'
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (val) => (
        <Tag color={val === 'DISPENSED' ? 'green' : 'orange'}>
          {val === 'DISPENSED' ? 'Đã cấp phát' : 'Chờ cấp phát'}
        </Tag>
      ),
    },
    {
      title: 'Thao tác cấp phát',
      key: 'actions',
      render: (_, record) => {
        const isPending = record.status !== 'DISPENSED'
        return (
          <Button
            type="primary"
            disabled={!isPending}
            loading={dispensingId === record.id}
            icon={<CheckCircleOutlined />}
            onClick={() => handleDispense(record)}
          >
            {isPending ? 'Cấp phát thuốc' : 'Đã cấp phát'}
          </Button>
        )
      },
    },
  ]

  return (
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>
          <MedicineBoxOutlined /> Quản lý danh mục thuốc và cấp phát
        </h2>
        <Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openAddMedicine}
          >
            Thêm thuốc mới
          </Button>
        </Space>
      </div>

      {lowStockMedicines.length > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          message="CẢNH BÁO TỒN KHO THẤP"
          description={`Các thuốc sau có số lượng tồn dưới hoặc bằng ngưỡng tối thiểu: ${lowStockMedicines.map((m) => `${m.name || m.medicineName} (Tồn: ${m.stock || 0}/${m.minStock || 0})`).join(', ')}`}
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs
        defaultActiveKey="dispense"
        items={[
          {
            key: 'dispense',
            label: <span><CheckCircleOutlined /> Cấp phát thuốc theo đơn ({prescriptions.filter((p) => p.status !== 'DISPENSED').length} đơn chờ)</span>,
            children: (
              <Card title="Danh sách đơn thuốc chờ cấp phát">
                <Table rowKey="id" columns={prescriptionColumns} dataSource={prescriptions} loading={loading} />
              </Card>
            ),
          },
          {
            key: 'med',
            label: <span><MedicineBoxOutlined /> Danh mục thuốc ({medicines.length})</span>,
            children: (
              <Card
                title="Danh mục thuốc dùng chung cho kê đơn và cấp phát"
                extra={
                  <Space>
                    <Input.Search
                      placeholder="Tìm theo tên thuốc hoặc hoạt chất..."
                      allowClear
                      style={{ width: 280 }}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                    />
                    <Select
                      defaultValue="ALL"
                      style={{ width: 140 }}
                      onChange={(val) => setStatusFilter(val)}
                      options={[
                        { value: 'ALL', label: 'Tất cả trạng thái' },
                        { value: 'ACTIVE', label: 'Đang dùng' },
                        { value: 'INACTIVE', label: 'Ngừng dùng' },
                      ]}
                    />
                  </Space>
                }
              >
                <Table rowKey="id" columns={medicineColumns} dataSource={filteredMedicines} loading={loading} />
              </Card>
            ),
          },
        ]}
      />

      <Modal
        title={editingMedicine ? `Sửa thông tin thuốc: ${editingMedicine.name || editingMedicine.medicineName}` : 'Thêm thuốc mới vào danh mục'}
        open={medicineOpen}
        onCancel={() => { setMedicineOpen(false); setEditingMedicine(null) }}
        onOk={() => medicineForm.submit()}
        okText="Lưu thông tin"
        cancelText="Hủy"
      >
        <Form form={medicineForm} layout="vertical" onFinish={handleSaveMedicine}>
          <Form.Item
            name="name"
            label="Tên thuốc"
            rules={[{ required: true, message: 'Vui lòng nhập tên thuốc.' }]}
          >
            <Input placeholder="Nhập tên thuốc (VD: Paracetamol 500mg)" />
          </Form.Item>

          <Form.Item name="activeIngredient" label="Hoạt chất">
            <Input placeholder="Nhập hoạt chất chính (VD: Paracetamol, Ibuprofen...)" />
          </Form.Item>

          <Form.Item name="category" label="Nhóm thuốc / Phân loại">
            <Input placeholder="Nhập nhóm thuốc (VD: Hạ sốt, Kháng sinh, Tim mạch...)" />
          </Form.Item>

          <Form.Item
            name="unit"
            label="Đơn vị tính"
            rules={[{ required: true, message: 'Vui lòng nhập đơn vị tính.' }]}
          >
            <Input placeholder="viên / chai / tuyp / goi..." style={{ width: '100%' }} />
          </Form.Item>

          {editingMedicine && (
            <Form.Item name="active" label="Trạng thái sử dụng">
              <Select options={[{ value: true, label: 'Đang dùng' }, { value: false, label: 'Ngừng dùng (Vô hiệu hóa)' }]} />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title="Xác nhận ngừng sử dụng thuốc"
        open={!!deletingMedicine}
        onCancel={() => setDeletingMedicine(null)}
        onOk={() => deletingMedicine && handleDeactivateMedicine(deletingMedicine)}
        okText="Xác nhận ngừng dùng"
        okButtonProps={{ danger: true }}
        cancelText="Hủy"
      >
        <p>
          Bạn có chắc chắn muốn <strong>ngừng sử dụng</strong> thuốc{' '}
          <strong>{deletingMedicine?.name || deletingMedicine?.medicineName}</strong> không?
        </p>
        <p style={{ color: '#8c8c8c', fontSize: 13 }}>
          * Thuốc sẽ chuyển sang trạng thái <em>Ngừng dùng</em>, không xuất hiện khi kê đơn mới.
        </p>
      </Modal>
    </div>
  )
}

export default PharmacyPage
