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
import { mergeMedicines, mergeBatches } from '../utils/storageHelpers'
import MedicineCatalogPage from './MedicineCatalogPage'

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

      const rawMeds = medicineRes.status === 'fulfilled' ? (medicineRes.value.data?.content || medicineRes.value.data || []) : []
      const apiMeds = Array.isArray(rawMeds) ? rawMeds : []
      const apiBatches = batchRes.status === 'fulfilled' ? (Array.isArray(batchRes.value.data) ? batchRes.value.data : []) : []
      const apiPrescs = prescriptionRes.status === 'fulfilled' ? (prescriptionRes.value.data || []) : []

      const finalMeds = mergeMedicines(apiMeds)
      const finalBatches = mergeBatches(apiBatches)

      setMedicines(finalMeds)
      setBatches(finalBatches)
      setPrescriptions(apiPrescs)
    } catch {
      setMedicines(mergeMedicines([]))
      setBatches(mergeBatches([]))
      setPrescriptions([])
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
    const trimmedActive = String(values.activeIngredient ?? '').trim() || 'Chưa cập nhật'
    const trimmedStrength = String(values.strength ?? '').trim() || '500 mg'
    const dosageFormVal = values.dosageForm || 'TABLET'
    const defaultRouteVal = values.defaultRoute || 'ORAL'

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
          medicineName: trimmedName,
          activeIngredient: trimmedActive,
          strength: trimmedStrength,
          dosageForm: dosageFormVal,
          unit: trimmedUnit,
          defaultRoute: defaultRouteVal,
        }
        await pharmacyApi.updateMedicine(editingMedicine.id, payload)

        if (typeof values.active === 'boolean' && values.active !== (editingMedicine.active !== false)) {
          await pharmacyApi.updateMedicineStatus(editingMedicine.id, values.active)
        }

        message.success(`Đã cập nhật thông tin thuốc ${trimmedName}`)
      } else {
        const newMed = {
          medicineCode: `MED-${Date.now().toString().slice(-6)}`,
          medicineName: trimmedName,
          activeIngredient: trimmedActive,
          strength: trimmedStrength,
          dosageForm: dosageFormVal,
          unit: trimmedUnit,
          defaultRoute: defaultRouteVal,
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

  const handleToggleMedicineStatus = async (medRecord, targetActiveState = false) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền quản lý danh mục thuốc.')
      return
    }

    try {
      if (medRecord.id) {
        await pharmacyApi.updateMedicineStatus(medRecord.id, targetActiveState)
      }
      message.success(`Đã ${targetActiveState ? 'kích hoạt lại' : 'ngừng sử dụng'} thuốc ${medRecord.medicineName || medRecord.name}`)
      setDeletingMedicine(null)
      await loadData()
    } catch (error) {
      message.error(error.response?.data?.message || error.message || 'Không thể cập nhật trạng thái thuốc')
    }
  }

  const handleReceiveBatch = async (values) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền tạo phiếu nhập kho.')
      return
    }

    try {
      const selectedMed = medicines.find((m) => m.id === values.medicineId)
      const formattedDate = values.expiryDate.format('YYYY-MM-DD')

      const receiptPayload = {
        note: values.note || `Nhập lô thuốc ${values.lotNumber || ''}`,
        items: [
          {
            medicineId: values.medicineId,
            batchNumber: values.lotNumber,
            expiryDate: formattedDate,
            quantity: Number(values.quantity),
            importPrice: Number(values.unitCost || 0),
          },
        ],
      }

      await pharmacyApi.receiveBatch(receiptPayload)

      message.success(`Đã nhập kho theo lô ${values.lotNumber} cho thuốc ${selectedMed?.name || selectedMed?.medicineName || ''}`)
      setBatchOpen(false)
      batchForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.response?.data?.message || error.message || 'Không thể nhập kho theo lô')
    }
  }

  const handleDispense = async (prescription) => {
    if (!canManageMedicineCatalog) {
      message.error('Bạn không có quyền cấp phát thuốc.')
      return
    }

    setDispensingId(prescription.id)
    try {
      await pharmacyApi.dispense(prescription.id)
      message.success(`Đã cấp phát thành công đơn thuốc ${prescription.prescriptionCode}. Tồn kho đã được trừ!`)
      await loadData()
    } catch (error) {
      message.error(error.response?.data?.message || error.message || 'Không thể cấp phát đơn thuốc trên Backend')
    } finally {
      setDispensingId(null)
    }
  }

  const openAddMedicine = () => {
    if (!canManageMedicineCatalog) {
      message.error('Chỉ Dược sĩ (PHARMACIST) mới có quyền thêm thuốc mới. Admin có quyền xem và giám sát.')
      return
    }
    setEditingMedicine(null)
    medicineForm.resetFields()
    medicineForm.setFieldsValue({
      strength: '500 mg',
      dosageForm: 'TABLET',
      defaultRoute: 'ORAL',
      unit: 'vien',
    })
    setMedicineOpen(true)
  }

  const openBatchModal = () => {
    if (!canManageMedicineCatalog) {
      message.error('Chỉ Dược sĩ (PHARMACIST) mới có quyền tạo phiếu nhập kho. Admin có quyền xem và giám sát.')
      return
    }
    batchForm.resetFields()
    setBatchOpen(true)
  }

  const openEditMedicine = (record) => {
    if (!canManageMedicineCatalog) {
      message.error('Chỉ Dược sĩ (PHARMACIST) mới có quyền sửa danh mục thuốc. Admin có quyền xem và giám sát.')
      return
    }
    setEditingMedicine(record)
    medicineForm.setFieldsValue({
      name: record.medicineName || record.name,
      activeIngredient: record.activeIngredient || '',
      strength: record.strength || '500 mg',
      dosageForm: record.dosageForm || 'TABLET',
      defaultRoute: record.defaultRoute || 'ORAL',
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

  const lowStockMedicines = medicines.filter((m) => {
    const medStock = m.stockQuantity ?? m.stock
    const medMinStock = m.minStockQuantity ?? m.minStock
    if (medStock === undefined || medMinStock === undefined) return false
    return m.active !== false && Number(medStock) <= Number(medMinStock)
  })

  const expiringBatches = batches.filter((b) => {
    const diff = dayjs(b.expiryDate).diff(dayjs(), 'day')
    return diff <= 30
  })

  const medicineColumns = [
    {
      title: 'Tên thuốc',
      dataIndex: 'medicineName',
      key: 'medicineName',
      render: (val, record) => <strong>{val || record.name || '—'}</strong>,
    },
    {
      title: 'Hoạt chất',
      dataIndex: 'activeIngredient',
      key: 'activeIngredient',
      render: (v) => v || '—',
    },
    { title: 'Hàm lượng', dataIndex: 'strength', key: 'strength', render: (v) => v || '—' },
    { title: 'Đơn vị tính', dataIndex: 'unit', key: 'unit', render: (v) => v || '—' },
    {
      title: 'Số lượng tồn',
      dataIndex: 'stockQuantity',
      key: 'stockQuantity',
      render: (val, record) => {
        const stockVal = val ?? record.stock ?? 100
        const isLow = Number(stockVal) <= Number(record.minStock || 10)
        return (
          <Tag color={isLow ? 'red' : 'green'}>
            {stockVal} {record.unit || 'đơn vị'} {isLow && '⚠️ Tồn thấp'}
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
          {record.active !== false ? (
            <Button
              danger
              size="small"
              disabled={!canManageMedicineCatalog}
              onClick={() => setDeletingMedicine(record)}
            >
              Ngừng dùng
            </Button>
          ) : (
            <Button
              type="primary"
              ghost
              size="small"
              disabled={!canManageMedicineCatalog}
              onClick={() => handleToggleMedicineStatus(record, true)}
            >
              Kích hoạt lại
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
        const price = val || record.importPrice || record.price || 1500
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
      render: (val) => {
        const parsed = parseItems(val)
        return (
          parsed.map((item) => {
            const med = medicines.find((m) => String(m.id) === String(item.medicineId))
            const medName = item.medicineName || (med ? (med.medicineName || med.name) : item.medicineId)
            return `${medName} (x${item.quantity} ${item.dosage ? `— ${item.dosage}` : ''})`
          }).join(' | ') || '—'
        )
      },
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (val) => {
        const isPending = val === 'PENDING_DISPENSE' || val === 'PENDING_DISPENSING'
        return (
          <Tag color={isPending ? 'orange' : 'green'}>
            {isPending ? 'Chờ cấp phát' : 'Đã cấp phát'}
          </Tag>
        )
      },
    },
    {
      title: 'Thao tác cấp phát',
      key: 'actions',
      render: (_, record) => {
        const isPending = record.status === 'PENDING_DISPENSE' || record.status === 'PENDING_DISPENSING'
        return (
          <Button
            type="primary"
            disabled={!isPending || !canManageMedicineCatalog}
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
          <Button
            icon={<PlusOutlined />}
            disabled={!canManageMedicineCatalog}
            onClick={openAddMedicine}
          >
            Thêm thuốc mới
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            disabled={!canManageMedicineCatalog}
            onClick={openBatchModal}
          >
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
          description={`Các thuốc sau có số lượng tồn dưới hoặc bằng ngưỡng tối thiểu: ${lowStockMedicines.map((m) => `${m.medicineName || m.name} (Tồn: ${m.stockQuantity ?? m.stock}/${m.minStockQuantity ?? m.minStock})`).join(', ')}`}
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
            label: <span><MedicineBoxOutlined /> Quản lý danh mục thuốc</span>,
            children: <MedicineCatalogPage />,
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

          <Form.Item name="strength" label="Hàm lượng" rules={[{ required: true, message: 'Nhập hàm lượng' }]}>
            <Input placeholder="VD: 500 mg, 10mg/5ml..." />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item name="dosageForm" label="Dạng bào chế" rules={[{ required: true, message: 'Chọn dạng bào chế' }]}>
              <Select
                style={{ width: 190 }}
                options={[
                  { value: 'TABLET', label: 'Viên nén' },
                  { value: 'CAPSULE', label: 'Viên nang' },
                  { value: 'SYRUP', label: 'Siro' },
                  { value: 'SUSPENSION', label: 'Hỗn dịch' },
                  { value: 'SOLUTION', label: 'Dung dịch' },
                  { value: 'INJECTION', label: 'Tiêm' },
                  { value: 'CREAM', label: 'Kem bôi' },
                  { value: 'OINTMENT', label: 'Thuốc mỡ' },
                  { value: 'INHALER', label: 'Xịt/Hít' },
                  { value: 'POWDER', label: 'Thuốc bột' },
                  { value: 'OTHER', label: 'Khác' },
                ]}
              />
            </Form.Item>

            <Form.Item name="defaultRoute" label="Đường dùng" rules={[{ required: true, message: 'Chọn đường dùng' }]}>
              <Select
                style={{ width: 190 }}
                options={[
                  { value: 'ORAL', label: 'Uống' },
                  { value: 'TOPICAL', label: 'Bôi ngoài' },
                  { value: 'INHALATION', label: 'Hít/Xịt' },
                  { value: 'INTRAVENOUS', label: 'Tiêm IV' },
                  { value: 'INTRAMUSCULAR', label: 'Tiêm IM' },
                  { value: 'SUBCUTANEOUS', label: 'Tiêm SC' },
                  { value: 'OTHER', label: 'Khác' },
                ]}
              />
            </Form.Item>
          </Space>

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
        onOk={() => deletingMedicine && handleToggleMedicineStatus(deletingMedicine, false)}
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
              options={medicines.filter((m) => m.active !== false).map((m) => ({ value: m.id, label: `${m.medicineName || m.name} (Tồn hiện tại: ${m.stockQuantity ?? m.stock ?? 0})` }))}
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
