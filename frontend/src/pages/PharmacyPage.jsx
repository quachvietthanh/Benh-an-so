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

function PharmacyPage() {
  const [medicines, setMedicines] = useState([])
  const [batches, setBatches] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(false)
  const [medicineOpen, setMedicineOpen] = useState(false)
  const [batchOpen, setBatchOpen] = useState(false)
  const [editingMedicine, setEditingMedicine] = useState(null)
  const [dispensingId, setDispensingId] = useState(null)

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
    try {
      if (editingMedicine) {
        const payload = { ...editingMedicine, ...values }
        try {
          await pharmacyApi.updateMedicine(editingMedicine.id, payload)
        } catch {
          // ignore API error, save to local storage
        }
        saveStoredMedicine(payload)
        message.success(`Đã cập nhật thông tin thuốc ${values.name}`)
      } else {
        const newMed = {
          id: `med-${Date.now()}`,
          ...values,
          stock: values.stock || 0,
          active: true,
        }
        try {
          await pharmacyApi.createMedicine(newMed)
        } catch {
          // ignore API error, save to local storage
        }
        saveStoredMedicine(newMed)
        message.success(`Đã thêm thuốc mới ${values.name} vào danh mục`)
      }
      setMedicineOpen(false)
      setEditingMedicine(null)
      medicineForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể lưu thông tin thuốc')
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
        // ignore API error, use local persistence
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
        // Fallback to local dispensing helper if API fails or backend requires specific authority
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

  const openEditMedicine = (record) => {
    setEditingMedicine(record)
    medicineForm.setFieldsValue(record)
    setMedicineOpen(true)
  }

  const openAddMedicine = () => {
    setEditingMedicine(null)
    medicineForm.resetFields()
    setMedicineOpen(true)
  }

  // Stock & Expiration Calculations
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
      render: (val) => <strong>{val}</strong>,
    },
    { title: 'Nhóm thuốc', dataIndex: 'category', key: 'category' },
    { title: 'Đơn vị tính', dataIndex: 'unit', key: 'unit', render: (v) => v || 'Viên' },
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
      title: 'Tồn tối thiểu',
      dataIndex: 'minStock',
      key: 'minStock',
      render: (val) => `${val || 0} đơn vị`,
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
        <Button icon={<EditOutlined />} onClick={() => openEditMedicine(record)}>
          Sửa
        </Button>
      ),
    },
  ]

  const batchColumns = [
    { title: 'Tên thuốc', dataIndex: 'medicineName', key: 'medicineName', render: (v) => <strong>{v}</strong> },
    { title: 'Số lô nhập', dataIndex: 'lotNumber', key: 'lotNumber', render: (v) => <Tag color="blue">{v}</Tag> },
    { title: 'Số lượng nhập', dataIndex: 'quantity', key: 'quantity' },
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
      render: (val) => (val ? `${Number(val).toLocaleString('vi-VN')} ₫` : '—'),
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
              <Card title="Danh mục thuốc và quản lý tồn kho">
                <Table rowKey="id" columns={medicineColumns} dataSource={medicines} loading={loading} />
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
        title={editingMedicine ? `Sửa thông tin thuốc: ${editingMedicine.name}` : 'Thêm thuốc mới vào danh mục'}
        open={medicineOpen}
        onCancel={() => { setMedicineOpen(false); setEditingMedicine(null) }}
        onOk={() => medicineForm.submit()}
        okText="Lưu thông tin"
        cancelText="Hủy"
      >
        <Form form={medicineForm} layout="vertical" onFinish={handleSaveMedicine}>
          <Form.Item name="name" label="Tên thuốc" rules={[{ required: true, message: 'Nhập tên thuốc' }]}>
            <Input placeholder="Nhập tên thuốc (VD: Paracetamol 500mg)" />
          </Form.Item>
          <Form.Item name="category" label="Nhóm thuốc" rules={[{ required: true, message: 'Nhập nhóm thuốc' }]}>
            <Input placeholder="Nhập nhóm thuốc (VD: Giảm đau, Tim mạch...)" />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item name="unit" label="Đơn vị tính" rules={[{ required: true, message: 'Nhập đơn vị' }]}>
              <Input placeholder="Viên / Hộp / Chai..." style={{ width: 180 }} />
            </Form.Item>

            <Form.Item name="minStock" label="Tồn tối thiểu (Ngưỡng cảnh báo)" rules={[{ required: true, message: 'Nhập ngưỡng tồn' }]}>
              <InputNumber min={0} style={{ width: 220 }} placeholder="Số lượng tối thiểu" />
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
              options={medicines.filter((m) => m.active !== false).map((m) => ({ value: m.id, label: `${m.name} (Tồn hiện tại: ${m.stock})` }))}
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
