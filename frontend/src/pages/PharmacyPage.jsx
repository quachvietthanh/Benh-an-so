import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  List,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ControlOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FilterOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { demoMedicines } from '../mock-data/mockData'
import {
  dispensePrescriptionHelper,
  mergeBatches,
  mergeMedicines,
  mergePrescriptions,
  saveStoredBatch,
  saveStoredMedicine,
} from '../utils/storageHelpers'

const { Title, Text, Paragraph } = Typography

// Helper format active ingredient, strength, unit, route
const getMedicineDetails = (med) => {
  if (!med) return { name: '', activeIngredient: '—', strength: '—', unit: 'Viên', route: 'Uống' }

  const name = med.name || ''
  let activeIngredient = med.activeIngredient || med.hoatChat
  let strength = med.strength || med.hamLuong
  let unit = med.unit || med.donVi || 'Viên'
  let route = med.route || med.duongDung || 'Uống'

  if (!activeIngredient || !strength) {
    if (name.includes('Amlodipine')) {
      activeIngredient = 'Amlodipine'
      strength = '5mg'
    } else if (name.includes('Metformin')) {
      activeIngredient = 'Metformin HCl'
      strength = '500mg'
    } else if (name.includes('Paracetamol')) {
      activeIngredient = 'Paracetamol'
      strength = '500mg'
    } else if (name.includes('Ibuprofen')) {
      activeIngredient = 'Ibuprofen'
      strength = '400mg'
    } else if (name.includes('Aspirin')) {
      activeIngredient = 'Aspirin'
      strength = '81mg'
    } else {
      const parts = name.split(' ')
      activeIngredient = parts[0] || name
      strength = parts[1] || '—'
    }
  }

  return { name, activeIngredient, strength, unit, route }
}

const parseItemsList = (value) => {
  try {
    return typeof value === 'string' ? JSON.parse(value) : (value || [])
  } catch {
    return []
  }
}

function PharmacyPage() {
  const { user } = useAuthContext()

  // User details
  const pharmacistName = user?.fullName && user.fullName !== 'admin' ? user.fullName : 'Dược sĩ Lê Thị Hạnh'

  // Data States
  const [medicines, setMedicines] = useState([])
  const [batches, setBatches] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(false)

  // Filters & Searches
  const [medSearchTerm, setMedSearchTerm] = useState('')
  const [medCategoryFilter, setMedCategoryFilter] = useState('ALL')
  const [medStatusFilter, setMedStatusFilter] = useState('ALL')
  const [medAlertFilter, setMedAlertFilter] = useState('ALL')

  const [prescStatusFilter, setPrescStatusFilter] = useState('ALL')
  const [prescSearchTerm, setPrescSearchTerm] = useState('')

  // Modals & Forms
  const [medicineOpen, setMedicineOpen] = useState(false)
  const [editingMedicine, setEditingMedicine] = useState(null)

  const [batchOpen, setBatchOpen] = useState(false)
  const [minStockModalOpen, setMinStockModalOpen] = useState(false)
  const [selectedMinStockMed, setSelectedMinStockMed] = useState(null)
  const [newMinStockValue, setNewMinStockValue] = useState(20)

  const [viewPrescription, setViewPrescription] = useState(null)
  const [dispensingId, setDispensingId] = useState(null)

  const [medicineForm] = Form.useForm()
  const [batchForm] = Form.useForm()

  // Load Data
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

      const combinedMeds = mergeMedicines(apiMeds.length ? apiMeds : demoMedicines)
      const combinedBatches = mergeBatches(apiBatches)
      const combinedPrescs = mergePrescriptions(apiPrescs)

      setMedicines(combinedMeds)
      setBatches(combinedBatches)
      setPrescriptions(combinedPrescs)
    } catch {
      setMedicines(mergeMedicines(demoMedicines))
      setBatches(mergeBatches([]))
      setPrescriptions(mergePrescriptions([]))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // CN-008: Dashboard Statistics
  const stats = useMemo(() => {
    const totalMedicines = medicines.length

    // Sắp hết (Tồn <= Ngưỡng hoặc = 0)
    const lowStockMeds = medicines.filter(
      (m) => m.active !== false && Number(m.stock || 0) <= Number(m.minStock || 20),
    )

    // Sắp hết hạn (<= 30 ngày) hoặc đã hết hạn
    const expiringOrExpiredBatches = batches.filter((b) => {
      if (!b.expiryDate) return false
      const diffDays = dayjs(b.expiryDate).diff(dayjs(), 'day')
      return diffDays <= 30
    })

    // Đơn chờ cấp phát
    const pendingPrescriptions = prescriptions.filter(
      (p) => p.status === 'PENDING_DISPENSING' || p.status === 'PENDING',
    )

    return {
      totalMedicines,
      lowStockCount: lowStockMeds.length,
      expiringCount: expiringOrExpiredBatches.length,
      pendingCount: pendingPrescriptions.length,
      lowStockMeds,
      expiringOrExpiredBatches,
    }
  }, [medicines, batches, prescriptions])

  // Categories list
  const categories = useMemo(() => {
    const setCat = new Set(medicines.map((m) => m.category).filter(Boolean))
    return ['ALL', ...Array.from(setCat)]
  }, [medicines])

  // CN-001: Filtered Medicines
  const filteredMedicines = useMemo(() => {
    return medicines.filter((med) => {
      const details = getMedicineDetails(med)
      const term = medSearchTerm.trim().toLowerCase()

      const matchSearch = !term
        || (med.code && med.code.toLowerCase().includes(term))
        || med.name.toLowerCase().includes(term)
        || (details.activeIngredient && details.activeIngredient.toLowerCase().includes(term))

      const matchCategory = medCategoryFilter === 'ALL' || med.category === medCategoryFilter

      const matchStatus = medStatusFilter === 'ALL'
        || (medStatusFilter === 'ACTIVE' && med.active !== false)
        || (medStatusFilter === 'INACTIVE' && med.active === false)

      let matchAlert = true
      const isLow = Number(med.stock || 0) <= Number(med.minStock || 20)
      const isOut = Number(med.stock || 0) === 0
      if (medAlertFilter === 'LOW') matchAlert = isLow && !isOut
      if (medAlertFilter === 'OUT') matchAlert = isOut

      return matchSearch && matchCategory && matchStatus && matchAlert
    })
  }, [medicines, medSearchTerm, medCategoryFilter, medStatusFilter, medAlertFilter])

  // CN-003: Filtered Prescriptions
  const filteredPrescriptions = useMemo(() => {
    return prescriptions.filter((p) => {
      const term = prescSearchTerm.trim().toLowerCase()
      const matchSearch = !term
        || p.prescriptionCode.toLowerCase().includes(term)
        || (p.patientName && p.patientName.toLowerCase().includes(term))

      const matchStatus = prescStatusFilter === 'ALL'
        || (prescStatusFilter === 'PENDING' && (p.status === 'PENDING_DISPENSING' || p.status === 'PENDING'))
        || (prescStatusFilter === 'DISPENSED' && (p.status === 'DISPENSED' || p.status === 'COMPLETED'))

      return matchSearch && matchStatus
    })
  }, [prescriptions, prescSearchTerm, prescStatusFilter])

  // CN-001: Save/Edit Medicine
  const handleSaveMedicine = async (values) => {
    try {
      const details = getMedicineDetails({ name: values.name })

      if (editingMedicine) {
        const payload = {
          ...editingMedicine,
          ...values,
          code: values.code || editingMedicine.code || `MT-${Math.floor(100 + Math.random() * 900)}`,
          activeIngredient: values.activeIngredient || details.activeIngredient,
          strength: values.strength || details.strength,
          unit: values.unit || details.unit,
          route: values.route || details.route,
          price: values.price || editingMedicine.price || 2000,
          updatedAt: new Date().toISOString(),
        }

        try {
          await pharmacyApi.updateMedicine(editingMedicine.id, payload)
        } catch {
          // ignore API error
        }

        saveStoredMedicine(payload)
        message.success(`Đã cập nhật thông tin thuốc ${values.name}`)
      } else {
        const newMed = {
          id: `med-${Date.now()}`,
          code: values.code || `MT-${Math.floor(100 + Math.random() * 900)}`,
          name: values.name,
          activeIngredient: values.activeIngredient || details.activeIngredient,
          strength: values.strength || details.strength,
          category: values.category || 'Thuốc chung',
          unit: values.unit || details.unit,
          route: values.route || details.route,
          stock: values.stock || 0,
          minStock: values.minStock || 20,
          price: values.price || 2000,
          active: true,
          createdAt: new Date().toISOString(),
        }

        try {
          await pharmacyApi.createMedicine(newMed)
        } catch {
          // ignore API error
        }

        saveStoredMedicine(newMed)
        message.success(`Đã thêm thuốc mới ${values.name} vào danh mục thành công`)
      }

      setMedicineOpen(false)
      setEditingMedicine(null)
      medicineForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể lưu thông tin thuốc')
    }
  }

  // CN-001: Ngừng sử dụng / Kích hoạt lại
  const handleToggleMedicineActive = (med) => {
    const updated = { ...med, active: !med.active, updatedAt: new Date().toISOString() }
    saveStoredMedicine(updated)
    message.success(updated.active ? `Đã kích hoạt lại thuốc ${med.name}` : `Đã chuyển thuốc ${med.name} sang trạng thái Ngừng sử dụng`)
    loadData()
  }

  // CN-007: Cập nhật ngưỡng tồn tối thiểu
  const handleSaveMinStockThreshold = () => {
    if (!selectedMinStockMed) return
    if (newMinStockValue < 0) {
      message.error('Ngưỡng tồn tối thiểu không được âm!')
      return
    }

    const updated = { ...selectedMinStockMed, minStock: Number(newMinStockValue), updatedAt: new Date().toISOString() }
    saveStoredMedicine(updated)
    message.success(`Đã cập nhật ngưỡng tồn tối thiểu cho ${selectedMinStockMed.name} là ${newMinStockValue} đơn vị`)
    setMinStockModalOpen(false)
    setSelectedMinStockMed(null)
    loadData()
  }

  // CN-002 & CN-009: Nhập kho theo lô (Receipt Batch with Validation)
  const handleReceiveBatch = async (values) => {
    // CN-009 VALIDATE
    if (Number(values.quantity) <= 0) {
      message.error('Số lượng nhập phải lớn hơn 0!')
      return
    }
    if (!values.lotNumber || !values.lotNumber.trim()) {
      message.error('Vui lòng nhập số lô!')
      return
    }
    if (!values.expiryDate) {
      message.error('Vui lòng chọn hạn sử dụng!')
      return
    }

    if (values.manufactureDate && values.expiryDate.isBefore(values.manufactureDate)) {
      message.error('Cảnh báo Validation: Hạn sử dụng phải lớn hơn Ngày sản xuất!')
      return
    }

    try {
      const selectedMed = medicines.find((m) => m.id === values.medicineId)
      const expiryStr = values.expiryDate.format('YYYY-MM-DD')
      const mfgStr = values.manufactureDate ? values.manufactureDate.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')

      const batchPayload = {
        id: `batch-${Date.now()}`,
        medicineId: values.medicineId,
        medicineName: selectedMed ? selectedMed.name : 'Thuốc',
        supplier: values.supplier || 'Công ty Dược phẩm Hậu Giang',
        lotNumber: values.lotNumber.trim(),
        manufactureDate: mfgStr,
        expiryDate: expiryStr,
        quantity: Number(values.quantity),
        unitCost: Number(values.unitCost || 0),
        notes: values.notes ? values.notes.trim() : '',
        createdAt: new Date().toISOString(),
      }

      try {
        await pharmacyApi.receiveBatch({
          ...values,
          expiryDate: expiryStr,
          manufactureDate: mfgStr,
        })
      } catch {
        // ignore API error
      }

      saveStoredBatch(batchPayload)
      message.success(`Nhập kho thành công lô ${batchPayload.lotNumber} (${values.quantity} ${selectedMed?.unit || 'đơn vị'}) cho thuốc ${selectedMed?.name || ''}`)
      setBatchOpen(false)
      batchForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể nhập kho theo lô')
    }
  }

  // CN-003, CN-005 & CN-009: Cấp phát thuốc theo đơn
  const handleDispense = async (prescription) => {
    setDispensingId(prescription.id)
    try {
      const itemsList = parseItemsList(prescription.items)

      // CN-009 & CN-005 Check stock & Check expired batches before dispensing
      for (const item of itemsList) {
        const med = medicines.find((m) => m.id === item.medicineId)
        if (med) {
          if (Number(med.stock || 0) < Number(item.quantity || 0)) {
            message.error(`Không đủ tồn kho! Thuốc ${med.name} chỉ còn ${med.stock} ${med.unit || 'đơn vị'}, không đủ cấp ${item.quantity} ${med.unit || 'đơn vị'}.`)
            setDispensingId(null)
            return
          }

          // Check expired batch constraint (CN-005: Không cho cấp phát thuốc hết hạn!)
          const medBatches = batches.filter((b) => b.medicineId === med.id)
          const expiredBatch = medBatches.find((b) => b.expiryDate && dayjs(b.expiryDate).isBefore(dayjs(), 'day'))
          if (expiredBatch) {
            message.error(`CẢNH BÁO TỪ CHỐI CẤP PHÁT: Lô thuốc ${med.name} (Số lô: ${expiredBatch.lotNumber}) ĐÃ HẾT HẠN (${dayjs(expiredBatch.expiryDate).format('DD/MM/YYYY')}). Hệ thống tự động CHẶN CẤP PHÁT!`)
            setDispensingId(null)
            return
          }
        }
      }

      try {
        await pharmacyApi.dispense(prescription.id)
      } catch {
        // Fallback local persistence
      }

      const updatedPresc = dispensePrescriptionHelper(prescription.id)
      updatedPresc.dispensedBy = pharmacistName
      updatedPresc.warehouse = 'Kho Nhà Thuốc Trung Tâm'
      saveStoredMedicine(updatedPresc)

      message.success(`Đã cấp phát thành công đơn thuốc ${prescription.prescriptionCode}! Tồn kho đã được tự động trừ.`)
      await loadData()
    } catch (error) {
      message.error(error.message || 'Lỗi cấp phát đơn thuốc')
    } finally {
      setDispensingId(null)
    }
  }

  const openEditMedicine = (record) => {
    setEditingMedicine(record)
    medicineForm.setFieldsValue({
      ...record,
      code: record.code || `MT-${Math.floor(100 + Math.random() * 900)}`,
      activeIngredient: record.activeIngredient || getMedicineDetails(record).activeIngredient,
      strength: record.strength || getMedicineDetails(record).strength,
      unit: record.unit || getMedicineDetails(record).unit,
      route: record.route || getMedicineDetails(record).route,
    })
    setMedicineOpen(true)
  }

  const openAddMedicine = () => {
    setEditingMedicine(null)
    medicineForm.resetFields()
    medicineForm.setFieldsValue({
      code: `MT-${Math.floor(100 + Math.random() * 900)}`,
      unit: 'Viên',
      route: 'Uống',
      minStock: 20,
      stock: 50,
      price: 2000,
      category: 'Giảm đau, Kháng viêm',
    })
    setMedicineOpen(true)
  }

  // CN-001: Columns Danh mục thuốc
  const medicineColumns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'code',
      key: 'code',
      width: 100,
      render: (val, row) => <strong>{val || `MT-${row.id.substring(0, 4)}`}</strong>,
    },
    {
      title: 'Tên thuốc',
      dataIndex: 'name',
      key: 'name',
      render: (val) => <strong style={{ color: '#1e40af' }}>{val}</strong>,
    },
    {
      title: 'Hoạt chất',
      dataIndex: 'activeIngredient',
      key: 'activeIngredient',
      render: (val, row) => <Tag color="cyan">{val || getMedicineDetails(row).activeIngredient}</Tag>,
    },
    {
      title: 'Hàm lượng',
      dataIndex: 'strength',
      key: 'strength',
      render: (val, row) => val || getMedicineDetails(row).strength,
    },
    {
      title: 'Đơn vị',
      dataIndex: 'unit',
      key: 'unit',
      render: (val, row) => val || getMedicineDetails(row).unit,
    },
    {
      title: 'Tồn kho',
      dataIndex: 'stock',
      key: 'stock',
      render: (val, row) => {
        const stockNum = Number(val || 0)
        const minNum = Number(row.minStock || 20)
        const isOut = stockNum === 0
        const isLow = stockNum <= minNum && !isOut

        return (
          <Space direction="vertical" size={2}>
            <span style={{ fontWeight: 700, fontSize: 14 }}>{stockNum} {row.unit || 'viên'}</span>
            {isOut && <Tag color="red">⚠️ CN-004: Hết hàng</Tag>}
            {isLow && <Tag color="orange">⚠️ CN-004: Sắp hết</Tag>}
            {!isOut && !isLow && <Tag color="green">An toàn</Tag>}
          </Space>
        )
      },
    },
    {
      title: 'Ngưỡng tối thiểu',
      dataIndex: 'minStock',
      key: 'minStock',
      render: (val, row) => (
        <Space>
          <span>{val || 20}</span>
          <Tooltip title="Thiết lập ngưỡng tồn tối thiểu">
            <Button
              type="text"
              size="small"
              icon={<ControlOutlined style={{ color: '#2563eb' }} />}
              onClick={() => {
                setSelectedMinStockMed(row)
                setNewMinStockValue(row.minStock || 20)
                setMinStockModalOpen(true)
              }}
            />
          </Tooltip>
        </Space>
      ),
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
      width: 140,
      render: (_, row) => (
        <Space size="small">
          <Tooltip title="Sửa thông tin thuốc">
            <Button size="small" icon={<EditOutlined />} onClick={() => openEditMedicine(row)}>
              Sửa
            </Button>
          </Tooltip>
          <Popconfirm
            title={row.active !== false ? 'Ngừng sử dụng thuốc này?' : 'Kích hoạt lại thuốc này?'}
            onConfirm={() => handleToggleMedicineActive(row)}
          >
            <Button size="small" danger={row.active !== false}>
              {row.active !== false ? 'Ngừng dùng' : 'Bật lại'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // CN-006: Columns Quản lý theo lô
  const batchColumns = [
    {
      title: 'Số lô',
      dataIndex: 'lotNumber',
      key: 'lotNumber',
      render: (val) => <Tag color="blue" style={{ fontWeight: 700 }}>{val}</Tag>,
    },
    {
      title: 'Tên thuốc & Hoạt chất',
      dataIndex: 'medicineName',
      key: 'medicineName',
      render: (val, row) => {
        const med = medicines.find((m) => m.id === row.medicineId)
        const details = getMedicineDetails(med)
        return (
          <div>
            <strong>{val}</strong>
            <div style={{ fontSize: 11, color: '#64748b' }}>Hoạt chất: {details.activeIngredient}</div>
          </div>
        )
      },
    },
    {
      title: 'Nhà cung cấp',
      dataIndex: 'supplier',
      key: 'supplier',
      render: (val) => val || 'Công ty Dược Hậu Giang',
    },
    {
      title: 'Ngày nhập',
      dataIndex: 'createdAt',
      key: 'receiveDate',
      render: (val) => (val ? dayjs(val).format('DD/MM/YYYY') : 'Hôm nay'),
    },
    {
      title: 'Ngày sản xuất',
      dataIndex: 'manufactureDate',
      key: 'mfg',
      render: (val) => (val ? dayjs(val).format('DD/MM/YYYY') : '—'),
    },
    {
      title: 'Hạn sử dụng (HSD)',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      render: (dateStr) => {
        if (!dateStr) return '—'
        const isExpired = dayjs(dateStr).isBefore(dayjs(), 'day')
        const diffDays = dayjs(dateStr).diff(dayjs(), 'day')
        const isNear = diffDays >= 0 && diffDays <= 30

        return (
          <Space direction="vertical" size={2}>
            <span>{dayjs(dateStr).format('DD/MM/YYYY')}</span>
            {isExpired && <Tag color="red">🚨 CN-005: HẾT HẠN (CHẶN CẤP PHÁT)</Tag>}
            {isNear && <Tag color="orange">⚠️ CN-005: Sắp hết hạn (Còn {diffDays} ngày)</Tag>}
            {!isExpired && !isNear && <Tag color="green">Hợp lệ</Tag>}
          </Space>
        )
      },
    },
    {
      title: 'SL còn trong lô',
      dataIndex: 'quantity',
      key: 'quantity',
      render: (val) => <strong>{val || 0} đơn vị</strong>,
    },
    {
      title: 'Đơn giá nhập',
      dataIndex: 'unitCost',
      key: 'unitCost',
      render: (val) => (val ? `${Number(val).toLocaleString('vi-VN')} ₫` : '—'),
    },
  ]

  // CN-003: Columns Cấp phát thuốc
  const prescriptionColumns = [
    {
      title: 'Mã đơn',
      dataIndex: 'prescriptionCode',
      key: 'code',
      render: (val) => <strong style={{ color: '#2563eb' }}>{val}</strong>,
    },
    {
      title: 'Bệnh nhân',
      dataIndex: 'patientName',
      key: 'patient',
      render: (val, row) => (
        <div>
          <strong>{val || 'Bệnh nhân'}</strong>
          <div style={{ fontSize: 11, color: '#64748b' }}>Mã BN: {row.patientCode || 'BN-001'}</div>
        </div>
      ),
    },
    {
      title: 'Bác sĩ kê',
      dataIndex: 'doctorName',
      key: 'doctor',
      render: (val) => val || 'BS. Phạm Hồng Anh',
    },
    {
      title: 'Danh sách thuốc',
      dataIndex: 'items',
      key: 'itemsList',
      render: (val) => {
        const list = parseItemsList(val)
        if (!list.length) return '—'
        return (
          <div>
            {list.map((item, idx) => {
              const med = medicines.find((m) => m.id === item.medicineId)
              return (
                <Tag key={idx} color="blue" style={{ marginBottom: 2 }}>
                  {med?.name || item.medicineId} (SL: {item.quantity})
                </Tag>
              )
            })}
          </div>
        )
      },
    },
    {
      title: 'Số lượng tổng',
      dataIndex: 'items',
      key: 'totalQty',
      render: (val) => {
        const list = parseItemsList(val)
        const total = list.reduce((acc, i) => acc + Number(i.quantity || 0), 0)
        return <strong>{total} món</strong>
      },
    },
    {
      title: 'Kho xuất',
      dataIndex: 'warehouse',
      key: 'warehouse',
      render: (val) => val || 'Kho Nhà Thuốc Trung Tâm',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (val) => {
        if (val === 'PENDING_DISPENSING' || val === 'PENDING') {
          return <Tag color="orange" style={{ fontSize: 12, padding: '2px 8px' }}>Chờ cấp phát</Tag>
        }
        return <Tag color="green" style={{ fontSize: 12, padding: '2px 8px' }}>Đã cấp phát</Tag>
      },
    },
    {
      title: 'Ngày kê',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val) => (val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—'),
    },
    {
      title: 'Ngày cấp',
      dataIndex: 'dispensedAt',
      key: 'dispensedAt',
      render: (val, row) => {
        if (row.status !== 'DISPENSED' && row.status !== 'COMPLETED') return '—'
        return val ? dayjs(val).format('HH:mm DD/MM/YYYY') : 'Hôm nay'
      },
    },
    {
      title: 'Người cấp',
      dataIndex: 'dispensedBy',
      key: 'dispensedBy',
      render: (val, row) => {
        if (row.status !== 'DISPENSED' && row.status !== 'COMPLETED') return '—'
        return val || pharmacistName
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 140,
      render: (_, row) => {
        const isPending = row.status === 'PENDING_DISPENSING' || row.status === 'PENDING'

        return (
          <Space>
            <Tooltip title="Xem chi tiết đơn thuốc">
              <Button size="small" icon={<EyeOutlined />} onClick={() => setViewPrescription(row)} />
            </Tooltip>

            {isPending && (
              <Popconfirm title="Xác nhận cấp phát đơn thuốc này?" onConfirm={() => handleDispense(row)}>
                <Button
                  type="primary"
                  size="small"
                  loading={dispensingId === row.id}
                  icon={<CheckCircleOutlined />}
                  style={{ backgroundColor: '#16a34a' }}
                >
                  Cấp phát
                </Button>
              </Popconfirm>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div className="pharmacy-page" style={{ paddingBottom: 40 }}>
      {/* PAGE HEADER */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: 10 }}>
            <MedicineBoxOutlined style={{ color: '#2563eb' }} /> Quản Lý Kho Thuốc & Cấp Phát
          </Title>
          <Text type="secondary">Quản lý danh mục thuốc, nhập kho theo lô/hạn dùng, cảnh báo tồn kho và cấp phát đơn thuốc</Text>
        </div>

        <Space wrap>
          <Button icon={<PlusOutlined />} onClick={openAddMedicine}>
            Thêm thuốc mới
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setBatchOpen(true)} style={{ backgroundColor: '#2563eb' }}>
            Nhập kho theo lô & HSD
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadData}>
            Tải lại
          </Button>
        </Space>
      </div>

      {/* CN-008: DASHBOARD STATISTIC CARDS */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #2563eb' }}>
            <Statistic
              title={<Text strong style={{ color: '#64748b' }}>📦 Tổng danh mục thuốc</Text>}
              value={stats.totalMedicines}
              suffix="sản phẩm"
              valueStyle={{ color: '#1e3a8a', fontWeight: 700 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #d97706' }}>
            <Statistic
              title={<Text strong style={{ color: '#d97706' }}>⚠️ CN-004: Thuốc sắp hết / Hết</Text>}
              value={stats.lowStockCount}
              suffix="thuốc"
              valueStyle={{ color: '#b45309', fontWeight: 700 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #dc2626' }}>
            <Statistic
              title={<Text strong style={{ color: '#dc2626' }}>🚨 CN-005: Sắp hết hạn / Hết hạn</Text>}
              value={stats.expiringCount}
              suffix="lô"
              valueStyle={{ color: '#991b1b', fontWeight: 700 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card borderRadius={12} style={{ borderLeft: '4px solid #16a34a' }}>
            <Statistic
              title={<Text strong style={{ color: '#16a34a' }}>📋 Đơn chờ cấp phát</Text>}
              value={stats.pendingCount}
              suffix="đơn"
              valueStyle={{ color: '#15803d', fontWeight: 700 }}
            />
          </Card>
        </Col>
      </Row>

      {/* CẢNH BÁO BANNERS */}
      {stats.lowStockCount > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          style={{ marginBottom: 14 }}
          message={`CẢNH BÁO TỒN KHO THẤP (${stats.lowStockCount} THUỐC)`}
          description={`Có ${stats.lowStockCount} thuốc có số lượng tồn bằng hoặc dưới ngưỡng tối thiểu: ${stats.lowStockMeds.map((m) => `${m.name} (Tồn: ${m.stock}/${m.minStock})`).join(', ')}`}
        />
      )}

      {stats.expiringCount > 0 && (
        <Alert
          type="error"
          showIcon
          icon={<AlertOutlined />}
          style={{ marginBottom: 16 }}
          message={`CẢNH BÁO HẠN SỬ DỤNG LÔ THUỐC (${stats.expiringCount} LÔ)`}
          description={`Có ${stats.expiringCount} lô thuốc sắp hết hạn (trong vòng 30 ngày) hoặc đã HẾT HẠN. Hệ thống tự động CHẶN CẤP PHÁT với các lô đã hết hạn.`}
        />
      )}

      {/* RESPONSIVE TAB CONTROL (Laptop 3 tabs / Mobile 1 cột) */}
      <Tabs
        defaultActiveKey="med"
        items={[
          // TAB 1: DANH MỤC & TỒN KHO THUỐC (CN-001, CN-004, CN-007)
          {
            key: 'med',
            label: <span><MedicineBoxOutlined /> 1. Danh mục & Tồn kho thuốc ({medicines.length})</span>,
            children: (
              <Card
                title={(
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
                    <span style={{ fontWeight: 600 }}>Quản lý danh mục & Thiết lập ngưỡng tồn kho</span>
                    <Button type="dashed" icon={<ControlOutlined />} onClick={() => { setSelectedMinStockMed(medicines[0]); setMinStockModalOpen(true) }}>
                      Thiết lập tồn tối thiểu
                    </Button>
                  </div>
                )}
              >
                {/* THANH LỌC & TÌM KIẾM */}
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={8} md={8}>
                    <Input
                      placeholder="Tìm theo Mã thuốc, Tên thuốc, Hoạt chất..."
                      prefix={<SearchOutlined />}
                      value={medSearchTerm}
                      onChange={(e) => setMedSearchTerm(e.target.value)}
                      allowClear
                    />
                  </Col>

                  <Col xs={12} sm={5} md={5}>
                    <Select
                      style={{ width: '100%' }}
                      value={medCategoryFilter}
                      onChange={setMedCategoryFilter}
                      options={categories.map((c) => ({ value: c, label: c === 'ALL' ? 'Tất cả Nhóm thuốc' : c }))}
                    />
                  </Col>

                  <Col xs={12} sm={5} md={5}>
                    <Select
                      style={{ width: '100%' }}
                      value={medStatusFilter}
                      onChange={setMedStatusFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Trạng thái' },
                        { value: 'ACTIVE', label: 'Đang dùng' },
                        { value: 'INACTIVE', label: 'Ngừng dùng' },
                      ]}
                    />
                  </Col>

                  <Col xs={24} sm={6} md={6}>
                    <Select
                      style={{ width: '100%' }}
                      value={medAlertFilter}
                      onChange={setMedAlertFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả Ngưỡng tồn' },
                        { value: 'LOW', label: '⚠️ Sắp hết (Tồn <= Ngưỡng)' },
                        { value: 'OUT', label: '🔴 Hết hàng (Tồn = 0)' },
                      ]}
                    />
                  </Col>
                </Row>

                <Table
                  rowKey="id"
                  loading={loading}
                  columns={medicineColumns}
                  dataSource={filteredMedicines}
                  pagination={{ pageSize: 10, showSizeChanger: true }}
                />
              </Card>
            ),
          },

          // TAB 2: CẤP PHÁT THUỐC THEO ĐƠN (CN-003, CN-005)
          {
            key: 'dispense',
            label: <span><CheckCircleOutlined /> 2. Cấp phát thuốc theo đơn ({stats.pendingCount} đơn chờ)</span>,
            children: (
              <Card title="Danh sách đơn thuốc chờ & lịch sử cấp phát">
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={12} md={8}>
                    <Input
                      placeholder="Tìm theo Mã đơn thuốc hoặc Tên bệnh nhân..."
                      prefix={<SearchOutlined />}
                      value={prescSearchTerm}
                      onChange={(e) => setPrescSearchTerm(e.target.value)}
                      allowClear
                    />
                  </Col>

                  <Col xs={24} sm={12} md={6}>
                    <Select
                      style={{ width: '100%' }}
                      value={prescStatusFilter}
                      onChange={setPrescStatusFilter}
                      options={[
                        { value: 'ALL', label: 'Tất cả trạng thái' },
                        { value: 'PENDING', label: '🟠 Chờ cấp phát' },
                        { value: 'DISPENSED', label: '🟢 Đã cấp phát' },
                      ]}
                    />
                  </Col>
                </Row>

                <Table
                  rowKey="id"
                  loading={loading}
                  columns={prescriptionColumns}
                  dataSource={filteredPrescriptions}
                  pagination={{ pageSize: 10 }}
                />
              </Card>
            ),
          },

          // TAB 3: NHẬP KHO THEO LÔ & HẠN DÙNG (CN-002, CN-005, CN-006)
          {
            key: 'batch',
            label: <span><SafetyCertificateOutlined /> 3. Quản lý Lô & Hạn sử dụng ({batches.length} lô)</span>,
            children: (
              <Card
                title={(
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>Danh sách lô thuốc đã nhập kho</span>
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setBatchOpen(true)}>
                      Nhập lô mới
                    </Button>
                  </div>
                )}
              >
                <Table
                  rowKey="id"
                  loading={loading}
                  columns={batchColumns}
                  dataSource={batches}
                  pagination={{ pageSize: 10 }}
                />
              </Card>
            ),
          },
        ]}
      />

      {/* MODAL 1: THÊM / SỬA THUỐC (CN-001) */}
      <Modal
        title={editingMedicine ? `📝 Sửa thông tin thuốc: ${editingMedicine.name}` : '💊 Thêm thuốc mới vào danh mục'}
        open={medicineOpen}
        onCancel={() => { setMedicineOpen(false); setEditingMedicine(null) }}
        onOk={() => medicineForm.submit()}
        okText="Lưu thông tin"
        cancelText="Hủy"
        width={600}
      >
        <Form form={medicineForm} layout="vertical" onFinish={handleSaveMedicine}>
          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="code" label="Mã thuốc (*)" rules={[{ required: true, message: 'Nhập mã thuốc' }]}>
                <Input placeholder="VD: MT-001" />
              </Form.Item>
            </Col>
            <Col span={16}>
              <Form.Item name="name" label="Tên thuốc (*)" rules={[{ required: true, message: 'Nhập tên thuốc' }]}>
                <Input placeholder="VD: Paracetamol 500mg" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="activeIngredient" label="Hoạt chất (*)" rules={[{ required: true, message: 'Nhập hoạt chất' }]}>
                <Input placeholder="VD: Paracetamol" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="strength" label="Hàm lượng (*)" rules={[{ required: true, message: 'Nhập hàm lượng' }]}>
                <Input placeholder="VD: 500mg / 5ml" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="category" label="Nhóm thuốc" rules={[{ required: true, message: 'Nhập nhóm thuốc' }]}>
                <Input placeholder="VD: Giảm đau" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="unit" label="Đơn vị tính (*)" rules={[{ required: true, message: 'Nhập đơn vị' }]}>
                <Input placeholder="Viên / Chai / Hộp" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="route" label="Đường dùng">
                <Input placeholder="Uống / Tiêm / Bôi" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="minStock" label="Ngưỡng tồn tối thiểu (CN-007)" rules={[{ required: true, message: 'Nhập ngưỡng tồn' }]}>
                <InputNumber min={0} style={{ width: '100%' }} placeholder="20" />
              </Form.Item>
            </Col>

            {!editingMedicine ? (
              <Col span={12}>
                <Form.Item name="stock" label="Tồn kho ban đầu">
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="50" />
                </Form.Item>
              </Col>
            ) : (
              <Col span={12}>
                <Form.Item name="active" label="Trạng thái">
                  <Select
                    options={[
                      { value: true, label: 'Đang dùng' },
                      { value: false, label: 'Ngừng dùng' },
                    ]}
                  />
                </Form.Item>
              </Col>
            )}
          </Row>
        </Form>
      </Modal>

      {/* MODAL 2: NHẬP KHO THEO LÔ (CN-002, CN-009 VALIDATE) */}
      <Modal
        title="📥 Nhập kho theo lô & Hạn sử dụng (CN-002)"
        open={batchOpen}
        onCancel={() => setBatchOpen(false)}
        onOk={() => batchForm.submit()}
        okText="Xác nhận nhập kho"
        cancelText="Hủy"
        width={600}
      >
        <Form form={batchForm} layout="vertical" onFinish={handleReceiveBatch}>
          <Form.Item name="medicineId" label="Chọn thuốc nhập kho (*)" rules={[{ required: true, message: 'Chọn thuốc' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="-- Tìm & chọn thuốc trong danh mục --"
              options={medicines.filter((m) => m.active !== false).map((m) => ({
                value: m.id,
                label: `${m.name} (Tồn hiện tại: ${m.stock} ${m.unit || 'viên'})`,
              }))}
            />
          </Form.Item>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="supplier" label="Nhà cung cấp">
                <Input placeholder="VD: Công ty Dược Hậu Giang" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="lotNumber" label="Số lô nhập (*)" rules={[{ required: true, message: 'Nhập số lô' }]}>
                <Input placeholder="VD: LOT-2026-001" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="manufactureDate" label="Ngày sản xuất (NSX)">
                <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Ngày sản xuất" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="expiryDate" label="Hạn sử dụng (HSD) (*)" rules={[{ required: true, message: 'Chọn hạn dùng' }]}>
                <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Hạn sử dụng" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="quantity" label="Số lượng nhập (*)" rules={[{ required: true, message: 'Nhập số lượng' }]}>
                <InputNumber min={1} style={{ width: '100%' }} placeholder="Số lượng > 0" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unitCost" label="Đơn giá nhập (VNĐ)">
                <InputNumber min={0} style={{ width: '100%' }} placeholder="Đơn giá" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="notes" label="Ghi chú nhập kho">
            <Input.TextArea rows={2} placeholder="Ghi chú thêm về đợt nhập kho..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL 3: THIẾT LẬP TỒN TỐI THIỂU NHANH (CN-007) */}
      <Modal
        title="⚙️ Thiết lập ngưỡng tồn tối thiểu (CN-007)"
        open={minStockModalOpen}
        onCancel={() => setMinStockModalOpen(false)}
        onOk={handleSaveMinStockThreshold}
        okText="Lưu ngưỡng tồn"
        cancelText="Hủy"
      >
        <Form layout="vertical">
          <Form.Item label="Chọn thuốc cần chỉnh ngưỡng:">
            <Select
              showSearch
              optionFilterProp="label"
              value={selectedMinStockMed?.id}
              onChange={(val) => {
                const found = medicines.find((m) => m.id === val)
                setSelectedMinStockMed(found)
                setNewMinStockValue(found?.minStock || 20)
              }}
              options={medicines.map((m) => ({ value: m.id, label: `${m.name} (Tồn hiện tại: ${m.stock}, Ngưỡng cũ: ${m.minStock || 20})` }))}
            />
          </Form.Item>

          <Form.Item label="Ngưỡng tồn kho tối thiểu mới (Cảnh báo Sắp hết):">
            <InputNumber
              min={0}
              style={{ width: '100%' }}
              value={newMinStockValue}
              onChange={(v) => setNewMinStockValue(v || 0)}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL XEM CHI TIẾT ĐƠN THUỐC CẤP PHÁT */}
      <Modal
        title={`Chi tiết đơn thuốc #${viewPrescription?.prescriptionCode}`}
        open={!!viewPrescription}
        onCancel={() => setViewPrescription(null)}
        footer={[
          <Button key="close" type="primary" onClick={() => setViewPrescription(null)}>Đóng</Button>,
        ]}
      >
        {viewPrescription && (
          <div>
            <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Mã đơn thuốc">{viewPrescription.prescriptionCode}</Descriptions.Item>
              <Descriptions.Item label="Bệnh nhân">{viewPrescription.patientName}</Descriptions.Item>
              <Descriptions.Item label="Bác sĩ chỉ định">{viewPrescription.doctorName}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={viewPrescription.status === 'PENDING_DISPENSING' ? 'orange' : 'green'}>
                  {viewPrescription.status === 'PENDING_DISPENSING' ? 'Chờ cấp phát' : 'Đã cấp phát'}
                </Tag>
              </Descriptions.Item>
            </Descriptions>

            <Text strong>Thuốc chỉ định:</Text>
            <Table
              size="small"
              pagination={false}
              dataSource={parseItemsList(viewPrescription.items)}
              columns={[
                { title: 'STT', render: (_, __, idx) => idx + 1, width: 50 },
                { title: 'Thuốc', dataIndex: 'medicineId', render: (id) => medicines.find((m) => m.id === id)?.name || id },
                { title: 'Số lượng', dataIndex: 'quantity' },
                { title: 'Cách dùng', dataIndex: 'usageInstruction' },
              ]}
            />
          </div>
        )}
      </Modal>
    </div>
  )
}

export default PharmacyPage
