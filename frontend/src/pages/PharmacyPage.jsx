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
import {
  dispensePrescriptionHelper,
  mergeBatches,
  mergeMedicines,
  mergePrescriptions,
  saveStoredBatch,
  saveStoredMedicine,
} from '../utils/storageHelpers'

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
  const userRoleList = Array.isArray(currentUser?.roles)
    ? currentUser.roles
    : currentUser?.role
    ? [currentUser.role]
    : []

  const normalizedRoles = userRoleList.map((r) =>
    String(r).toLowerCase().replace(/^role_/, ''),
  )

  const canManageMedicineCatalog = normalizedRoles.includes('pharmacist')

  const [medicines, setMedicines] = useState([])
  const [batches, setBatches] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(false)
  const [medicineOpen, setMedicineOpen] = useState(false)
  const [batchOpen, setBatchOpen] = useState(false)
  const [editingMedicine, setEditingMedicine] = useState(null)
  const [deletingMedicine, setDeletingMedicine] = useState(null)
  const [dispensingId, setDispensingId] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const [medicineForm] = Form.useForm()
  const [batchForm] = Form.useForm()

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [medicineRes, batchRes, prescriptionRes] = await Promise.allSettled([
        pharmacyApi.medicines(),
        pharmacyApi.batches(),
        pharmacyApi.prescriptions(),
      ])

      const apiMeds = medicineRes.status === 'fulfilled' ? (medicineRes.value.data || []) : []
      const apiBatches = batchRes.status === 'fulfilled' ? (batchRes.value.data || []) : []
      const apiPrescs = prescriptionRes.status === 'fulfilled' ? (prescriptionRes.value.data || []) : []

      setMedicines(mergeMedicines(apiMeds))
      setBatches(mergeBatches(apiBatches))
      setPrescriptions(mergePrescriptions(apiPrescs))
    } catch {
      setMedicines(mergeMedicines([]))
      setBatches(mergeBatches([]))
      setPrescriptions(mergePrescriptions([]))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  const handleSaveMedicine = async (values) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

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
          name: trimmedName,
          unit: trimmedUnit,
          activeIngredient: trimmedActive,
        }
        try {
          await pharmacyApi.updateMedicine(editingMedicine.id, payload)
        } catch {
        }
        saveStoredMedicine(payload)
        message.success(`Đã cập nhật thông tin thuốc ${trimmedName}`)
      } else {
        const newMed = {
          id: `16000000-0000-0000-0000-${Date.now().toString().slice(-12)}`,
          ...values,
          name: trimmedName,
          unit: trimmedUnit,
          activeIngredient: trimmedActive,
          stock: values.stock || 0,
          active: true,
        }
        try {
          await pharmacyApi.createMedicine(newMed)
        } catch {
        }
        saveStoredMedicine(newMed)
        message.success(`Đã thêm thuốc mới ${trimmedName} vào danh mục`)
      }
      setMedicineOpen(false)
      setEditingMedicine(null)
      medicineForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể lưu thông tin thuốc')
    }
  }

  const handleDeactivateMedicine = async (medRecord) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

    try {
      const updated = { ...medRecord, active: false }
      try {
        await pharmacyApi.updateMedicine(medRecord.id, updated)
      } catch {
      }
      saveStoredMedicine(updated)
      message.success(`Đã ngừng sử dụng thuốc ${medRecord.name || medRecord.medicineName}`)
      setDeletingMedicine(null)
      await loadData()
    } catch {
      message.error('Không thể cập nhật trạng thái thuốc')
    }
  }

  const handleReceiveBatch = async (values) => {
    try {
      const selectedMed = medicines.find((m) => m.id === values.medicineId)
      const formattedDate = values.expiryDate.format('YYYY-MM-DD')
      const batchPayload = {
        id: `batch-${Date.now()}`,
        medicineId: values.medicineId,
        medicineName: selectedMed ? selectedMed.name : 'Thuốc',
        lotNumber: values.lotNumber,
        expiryDate: formattedDate,
        quantity: values.quantity,
        unitCost: values.unitCost || 0,
      }

      try {
        await pharmacyApi.receiveBatch({
          ...values,
          expiryDate: formattedDate,
        })
      } catch {
      }

      saveStoredBatch(batchPayload)
      message.success(`Đã nhập kho theo lô ${values.lotNumber} cho thuốc ${selectedMed?.name || ''}`)
      setBatchOpen(false)
      batchForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể nhập kho theo lô')
    }
  }

  const handleDispense = async (prescription) => {
    setDispensingId(prescription.id)
    try {
      try {
        await pharmacyApi.dispense(prescription.id)
      } catch {
      }
      dispensePrescriptionHelper(prescription.id)
      message.success(`Đã cấp phát thành công đơn thuốc ${prescription.prescriptionCode}. Tồn kho đã được trừ!`)
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể cấp phát đơn thuốc')
    } finally {
      setDispensingId(null)
    }
  }

  const openAddMedicine = () => {
    if (!canManageMedicineCatalog) {
      message.error('Chỉ Dược sĩ (PHARMACIST) mới có quyền thêm thuốc vào danh mục.')
      return
    }
    setEditingMedicine(null)
    medicineForm.resetFields()
    setMedicineOpen(true)
  }

  const openEditMedicine = (record) => {
    if (!canManageMedicineCatalog) {
      message.error('Chỉ Dược sĩ (PHARMACIST) mới có quyền sửa danh mục thuốc.')
      return
    }
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

  const expiringBatches = batches.filter((b) => {
    const diff = dayjs(b.expiryDate).diff(dayjs(), 'day')
    return diff <= 30
  })

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
      title: 'Số lượng tồn',
      dataIndex: 'stock',
      key: 'stock',
      render: (val, record) => {
        const isLow = Number(val || 0) <= Number(record.minStock || 0)
        return (
          <Tag color={isLow ? 'red' : 'green'}>
            {val || 0} {record.unit || 'đơn vị'} {isLow && '⚠️ Tồn thấp'}
          </Tag>
        )
      },
    },
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
            disabled={!canManageMedicineCatalog}
            onClick={() => openEditMedicine(record)}
          >
            Sửa
          </Button>
          {record.active !== false && (
            <Button
              danger
              size="small"
              disabled={!canManageMedicineCatalog}
              onClick={() => setDeletingMedicine(record)}
            >
              Ngừng dùng
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const batchColumns = [
    { title: 'Tên thuốc', dataIndex: 'medicineName', key: 'medicineName', render: (v) => <strong>{v}</strong> },
    {
      title: 'Số lô nhập',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      render: (v, record) => <Tag color="blue">{v || record.lotNumber || 'LO-DEFAULT'}</Tag>,
    },
    { title: 'Số lượng nhập', dataIndex: 'quantity', key: 'quantity', render: (v) => `${Number(v || 0).toLocaleString('vi-VN')}` },
    {
      title: 'Hạn sử dụng',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      render: (dateStr) => {
        const isExpired = dayjs(dateStr).isBefore(dayjs(), 'day')
        const daysLeft = dayjs(dateStr).diff(dayjs(), 'day')
        const isNear = daysLeft <= 30 && !isExpired

        return (
          <Tag color={isExpired ? 'red' : isNear ? 'orange' : 'green'}>
            {dayjs(dateStr).format('DD/MM/YYYY')}
            {isExpired ? ' (ĐÃ HẾT HẠN - CHẶN CẤP PHÁT)' : isNear ? ` (Còn ${daysLeft} ngày)` : ''}
          </Tag>
        )
      },
    },
    {
      title: 'Đơn giá nhập',
      dataIndex: 'unitCost',
      key: 'unitCost',
      render: (val, record) => {
        const price = val || record.price || 1500
        return `${Number(price).toLocaleString('vi-VN')} ₫`
      },
    },
    {
      title: 'Nhà cung cấp',
      dataIndex: 'supplier',
      key: 'supplier',
      render: (v) => v || 'Công ty Dược phẩm',
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
          return `${med ? med.name : item.medicineId} (x${item.quantity} ${item.dosage ? `— ${item.dosage}` : ''})`
        }).join(' | ') || '—'
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (val) => (
        <Tag color={val === 'PENDING_DISPENSING' ? 'orange' : 'green'}>
          {val === 'PENDING_DISPENSING' ? 'Chờ cấp phát' : 'Đã cấp phát'}
        </Tag>
      ),
    },
    {
      title: 'Thao tác cấp phát',
      key: 'actions',
      render: (_, record) => {
        const isPending = record.status === 'PENDING_DISPENSING'
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
          <MedicineBoxOutlined /> Quản lý kho thuốc và cấp phát
        </h2>
        <Space>
          <Button icon={<PlusOutlined />} onClick={openAddMedicine}>
            Thêm thuốc mới
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setBatchOpen(true)}>
            Nhập kho theo lô &amp; hạn dùng
          </Button>
        </Space>
      </div>

      {lowStockMedicines.length > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          message="CẢNH BÁO TỒN KHO THẤP"
          description={`Các thuốc sau có số lượng tồn dưới hoặc bằng ngưỡng tối thiểu: ${lowStockMedicines.map((m) => `${m.name} (Tồn: ${m.stock}/${m.minStock})`).join(', ')}`}
          style={{ marginBottom: 16 }}
        />
      )}

      {expiringBatches.length > 0 && (
        <Alert
          type="error"
          showIcon
          icon={<AlertOutlined />}
          message="CẢNH BÁO THUỐC HẾT HẠN / GẦN HẾT HẠN"
          description={`Có ${expiringBatches.length} lô thuốc sắp hết hạn (trong vòng 30 ngày) hoặc đã hết hạn. Các lô đã hết hạn sẽ tự động bị chặn cấp phát.`}
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs
        defaultActiveKey="dispense"
        items={[
          {
            key: 'dispense',
            label: <span><CheckCircleOutlined /> Cấp phát thuốc theo đơn ({prescriptions.filter((p) => p.status === 'PENDING_DISPENSING').length} đơn chờ)</span>,
            children: (
              <Card title="Danh sách đơn thuốc chờ cấp phát">
                <Table rowKey="id" columns={prescriptionColumns} dataSource={prescriptions} loading={loading} />
              </Card>
            ),
          },
          {
            key: 'med',
            label: <span><MedicineBoxOutlined /> Danh mục &amp; tồn kho thuốc ({medicines.length})</span>,
            children: (
              <Card
                title="Danh mục thuốc dùng chung cho kê đơn và kho"
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
          {
            key: 'batch',
            label: <span><SafetyCertificateOutlined /> Nhập kho theo lô &amp; Hạn dùng ({batches.length} lô)</span>,
            children: (
              <Card title="Quản lý lô nhập và hạn sử dụng">
                <Table rowKey="id" columns={batchColumns} dataSource={batches} loading={loading} />
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

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              name="unit"
              label="Đơn vị tính"
              rules={[{ required: true, message: 'Vui lòng nhập đơn vị tính.' }]}
            >
              <Input placeholder="viên / chai / tuyp / goi..." style={{ width: 200 }} />
            </Form.Item>

            <Form.Item name="minStock" label="Tồn tối thiểu (Cảnh báo)">
              <InputNumber min={0} style={{ width: 200 }} placeholder="Số lượng tối thiểu" />
            </Form.Item>
          </Space>

          {!editingMedicine && (
            <Form.Item name="stock" label="Số lượng tồn kho ban đầu">
              <InputNumber min={0} placeholder="0" style={{ width: '100%' }} />
            </Form.Item>
          )}

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
          * Thuốc sẽ chuyển sang trạng thái <em>Ngừng dùng</em>, không xuất hiện khi kê đơn mới nhưng vẫn được lưu vết trong lịch sử và báo cáo tồn kho cũ.
        </p>
      </Modal>

      <Modal
        title="Nhập kho theo lô & Hạn sử dụng"
        open={batchOpen}
        onCancel={() => setBatchOpen(false)}
        onOk={() => batchForm.submit()}
        okText="Xác nhận nhập kho"
        cancelText="Hủy"
      >
        <Form form={batchForm} layout="vertical" onFinish={handleReceiveBatch}>
          <Form.Item name="medicineId" label="Chọn thuốc" rules={[{ required: true, message: 'Vui lòng chọn thuốc' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Chọn thuốc trong danh mục"
              options={medicines.filter((m) => m.active !== false).map((m) => ({ value: m.id, label: `${m.name || m.medicineName} (Tồn hiện tại: ${m.stock})` }))}
            />
          </Form.Item>

          <Form.Item name="lotNumber" label="Số lô nhập" rules={[{ required: true, message: 'Nhập số lô' }]}>
            <Input placeholder="VD: LOT-2026-001" />
          </Form.Item>

          <Form.Item name="expiryDate" label="Hạn sử dụng" rules={[{ required: true, message: 'Chọn hạn sử dụng' }]}>
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Chọn ngày hết hạn" />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item name="quantity" label="Số lượng nhập" rules={[{ required: true, message: 'Nhập số lượng' }]}>
              <InputNumber min={1} style={{ width: 220 }} placeholder="Số lượng" />
            </Form.Item>

            <Form.Item name="unitCost" label="Đơn giá nhập (VNĐ)">
              <InputNumber min={0} style={{ width: 220 }} placeholder="Giá nhập" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  )
}

export default PharmacyPage
