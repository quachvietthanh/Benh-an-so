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
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
  notification,
} from 'antd'
import {
  AlertOutlined,
  ArrowLeftOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
  FieldTimeOutlined,
  HistoryOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  ShopOutlined,
  StopOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useLocation, useNavigate } from 'react-router-dom'
import pharmacyApi from '../api/pharmacyApi'
import { useAuthContext } from '../context/AuthContext'
import { getApiErrorMessage as getErrorMessage } from '../utils/apiError'

const { Text, Title, Paragraph } = Typography

const EMPTY_RECEIPT_ITEM = {
  medicineId: undefined,
  batchNumber: '',
  expiryDate: null,
  quantity: 1,
  importPrice: 0,
}

const toCollection = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const normalizeBatch = (batch) => ({
  ...batch,
  batchId: batch?.batchId || batch?.id,
  id: batch?.batchId || batch?.id,
})

const isDiscardableBatch = (batch) => (
  Boolean(batch?.expiryDate)
  && dayjs(batch.expiryDate).isBefore(dayjs(), 'day')
  && String(batch?.status || '').toUpperCase() !== 'EXPIRED'
  && Number(batch?.quantity || 0) > 0
)

const inventoryOperationErrorMessage = (error, fallback) => {
  const code = error?.response?.data?.code || error?.apiError?.code
  const messages = {
    VALIDATION_FAILED: 'Dữ liệu điều chỉnh chưa hợp lệ. Vui lòng kiểm tra số lượng thực tế và lý do.',
    BATCH_NOT_EXPIRED: 'Chỉ được hủy lô thuốc đã quá hạn sử dụng.',
    BATCH_ALREADY_DISCARDED: 'Lô thuốc đã được hủy hoặc không còn số lượng tồn.',
    ACCESS_DENIED: 'Bạn không có quyền điều chỉnh hoặc hủy lô thuốc.',
    BATCH_NOT_FOUND: 'Không tìm thấy lô thuốc. Dữ liệu có thể đã thay đổi.',
    BATCH_STATE_CONFLICT: 'Trạng thái lô thuốc đã thay đổi, không thể điều chỉnh.',
  }
  return messages[code] || getErrorMessage(error, fallback)
}

function InventoryReceiptPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuthContext()

  const roles = useMemo(() => {
    const values = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return values
      .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
      .filter(Boolean)
  }, [user])
  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])
  const canManageReceipts = userPermissions.includes('PHARMACY_CREATE') || roles.includes('pharmacist') || roles.includes('admin')
  // NCL-06-CN-010: PHARMACY_UPDATE — controller + service double-check (PHARMACIST/ADMIN fallback)
  const canAdjustDiscard = userPermissions.includes('PHARMACY_UPDATE') || roles.includes('pharmacist') || roles.includes('admin')

  const [form] = Form.useForm()
  const [adjustForm] = Form.useForm()
  const [discardForm] = Form.useForm()

  const [activeTab, setActiveTab] = useState(location.state?.tab || 'create')

  const [medicines, setMedicines] = useState([])
  const [, setStocks] = useState([])
  const [batches, setBatches] = useState([])
  const [expiryAlerts, setExpiryAlerts] = useState([])
  const [recentReceipts, setRecentReceipts] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [loadError, setLoadError] = useState('')

  const [batchKeyword, setBatchKeyword] = useState('')
  const [batchStatusFilter, setBatchStatusFilter] = useState('ALL')
  const [batchEligibleFilter, setBatchEligibleFilter] = useState('ALL')
  const [batchExpiryFilter, setBatchExpiryFilter] = useState('ALL')

  // NCL-06-CN-010 CV-04 — adjust / discard UI state
  const [adjustModalOpen, setAdjustModalOpen] = useState(false)
  const [discardModalOpen, setDiscardModalOpen] = useState(false)
  const [selectedBatch, setSelectedBatch] = useState(null)
  const [submittingAdjust, setSubmittingAdjust] = useState(false)
  const [submittingDiscard, setSubmittingDiscard] = useState(false)

  const formItems = Form.useWatch('items', form) || []
  const totalReceiptAmount = useMemo(() => {
    return formItems.reduce((sum, item) => {
      const q = Number(item?.quantity || 0)
      const p = Number(item?.importPrice || 0)
      return sum + (q > 0 ? q : 0) * (p > 0 ? p : 0)
    }, 0)
  }, [formItems])

  const openAdjustModal = (batch) => {
    if (!canAdjustDiscard) {
      message.error('Bạn không có quyền điều chỉnh tồn kho.')
      return
    }
    setSelectedBatch(batch)
    adjustForm.setFieldsValue({ actualQuantity: Number(batch?.quantity || 0), reason: '' })
    setAdjustModalOpen(true)
  }

  const openDiscardModal = (batch) => {
    if (!canAdjustDiscard) {
      message.error('Bạn không có quyền hủy lô thuốc.')
      return
    }
    setSelectedBatch(batch)
    discardForm.resetFields()
    setDiscardModalOpen(true)
  }

  const closeOperationModals = () => {
    if (!submittingAdjust && !submittingDiscard) {
      setAdjustModalOpen(false)
      setDiscardModalOpen(false)
      setSelectedBatch(null)
      adjustForm.resetFields()
      discardForm.resetFields()
    }
  }

  const handleAdjustSubmit = async (values) => {
    if (!selectedBatch || !canAdjustDiscard) return
    const actualQuantity = Number(values.actualQuantity)
    const currentQuantity = Number(selectedBatch.quantity || 0)
    const reason = String(values.reason || '').trim()

    if (!Number.isInteger(actualQuantity) || actualQuantity < 0) {
      adjustForm.setFields([{ name: 'actualQuantity', errors: ['Số lượng thực tế phải là số nguyên không âm.'] }])
      return
    }
    if (!reason) {
      adjustForm.setFields([{ name: 'reason', errors: ['Vui lòng nhập lý do điều chỉnh theo QTN-32.'] }])
      return
    }
    if (actualQuantity === currentQuantity) {
      adjustForm.setFields([{ name: 'actualQuantity', errors: ['Số thực tế phải khác tồn hiện tại (chênh lệch khác 0).'] }])
      return
    }

    setSubmittingAdjust(true)
    try {
      const response = await pharmacyApi.adjustBatchStock(selectedBatch.batchId || selectedBatch.id, {
        actualQuantity,
        reason,
      })
      const result = response?.data || {}
      const change = Number(result.quantityChange ?? actualQuantity - currentQuantity)
      const changeLabel = change > 0 ? `tăng ${change}` : `giảm ${Math.abs(change)}`
      message.success(`Đã điều chỉnh tồn kho: ${changeLabel} ${selectedBatch.unit || ''}. Người thực hiện và lý do đã được ghi nhận.`)
      setAdjustModalOpen(false)
      setSelectedBatch(null)
      adjustForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(inventoryOperationErrorMessage(error, 'Không thể điều chỉnh tồn kho.'))
    } finally {
      setSubmittingAdjust(false)
    }
  }

  const handleDiscardSubmit = async (values) => {
    if (!selectedBatch || !canAdjustDiscard) return
    const reason = String(values.reason || '').trim()
    if (!reason) {
      discardForm.setFields([{ name: 'reason', errors: ['Vui lòng nhập lý do hủy lô theo QTN-32.'] }])
      return
    }
    if (!isDiscardableBatch(selectedBatch)) {
      message.error('Chỉ được hủy lô đã quá hạn và còn số lượng tồn.')
      return
    }

    setSubmittingDiscard(true)
    try {
      const response = await pharmacyApi.discardExpiredBatch(selectedBatch.batchId || selectedBatch.id, { reason })
      const result = response?.data || {}
      message.success(`Đã hủy ${Number(result.discardedQuantity ?? selectedBatch.quantity ?? 0).toLocaleString('vi-VN')} ${selectedBatch.unit || ''} của lô ${selectedBatch.batchNumber}. Lô đã được loại khỏi cấp phát.`)
      setDiscardModalOpen(false)
      setSelectedBatch(null)
      discardForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(inventoryOperationErrorMessage(error, 'Không thể hủy lô thuốc hết hạn.'))
    } finally {
      setSubmittingDiscard(false)
    }
  }

  const loadData = useCallback(async () => {
    if (!canManageReceipts) return
    setLoading(true)
    setLoadError('')
    try {
      const [medicineResponse, stockResponse, batchResponse, expiryResponse] = await Promise.allSettled([
        pharmacyApi.medicines({ active: true }),
        pharmacyApi.stocks(),
        pharmacyApi.batches(),
        pharmacyApi.expiryAlerts(),
      ])

      if (medicineResponse.status === 'fulfilled') {
        setMedicines(toCollection(medicineResponse.value?.data).filter((item) => item?.active !== false))
      }
      if (stockResponse.status === 'fulfilled') {
        setStocks(toCollection(stockResponse.value?.data))
      }
      if (batchResponse.status === 'fulfilled') {
        setBatches(toCollection(batchResponse.value?.data).map(normalizeBatch))
      }
      if (expiryResponse.status === 'fulfilled') {
        setExpiryAlerts(toCollection(expiryResponse.value?.data))
      }
    } catch (error) {
      setLoadError(getErrorMessage(error, 'Không thể tải dữ liệu kho từ máy chủ.'))
    } finally {
      setLoading(false)
    }
  }, [canManageReceipts])

  useEffect(() => {
    loadData()
  }, [loadData])

  useEffect(() => {
    if (location.state?.prefillItem && medicines.length > 0) {
      const { medicineId, medicineName } = location.state.prefillItem
      form.setFieldsValue({
        items: [
          {
            medicineId,
            batchNumber: '',
            expiryDate: null,
            quantity: 1,
            importPrice: 0,
          },
        ],
        note: `Phiếu nhập bổ sung cho ${medicineName || 'thuốc dưới ngưỡng tồn'}`,
      })
      setActiveTab('create')
    }
  }, [location.state, medicines, form])

  const medicineOptions = useMemo(
    () =>
      medicines.map((medicine) => ({
        value: medicine.id,
        label: `${medicine.medicineCode || '—'} · ${medicine.medicineName} (${[medicine.activeIngredient, medicine.strength].filter(Boolean).join(' - ') || medicine.unit})`,
        unit: medicine.unit,
        medicineCode: medicine.medicineCode,
        medicineName: medicine.medicineName,
      })),
    [medicines],
  )

  const medicineMap = useMemo(
    () => new Map(medicines.map((m) => [String(m.id), m])),
    [medicines],
  )

  const stats = useMemo(() => {
    const totalBatches = batches.length
    const eligibleBatches = batches.filter((b) => b.eligibleForDispense !== false && b.status !== 'EXPIRED' && b.quantity > 0).length
    const nearExpiryBatches = batches.filter((b) => {
      if (!b.expiryDate || b.status === 'EXPIRED') return false
      const days = dayjs(b.expiryDate).diff(dayjs(), 'day')
      return days >= 0 && days <= 90 && b.quantity > 0
    }).length
    const expiredOrDepleted = batches.filter((b) => b.status === 'EXPIRED' || b.quantity === 0).length

    return {
      totalBatches,
      eligibleBatches,
      nearExpiryBatches,
      expiredOrDepleted,
      expiryAlertCount: expiryAlerts.length,
    }
  }, [batches, expiryAlerts])

  const handleSubmit = async (values) => {
    if (!canManageReceipts) {
      message.error('Bạn không có quyền tạo phiếu nhập kho.')
      return
    }

    const rawItems = Array.isArray(values.items) ? values.items : []
    if (rawItems.length === 0) {
      message.error('Phiếu nhập phải có ít nhất một dòng thuốc.')
      return
    }

    const today = dayjs().startOf('day')

    for (let i = 0; i < rawItems.length; i++) {
      const item = rawItems[i]

      if (!item.medicineId) {
        message.error(`Dòng ${i + 1}: Vui lòng chọn thuốc.`)
        return
      }

      if (!item.batchNumber || !String(item.batchNumber).trim()) {
        message.error(`Dòng ${i + 1}: Vui lòng nhập số lô.`)
        return
      }

      if (!item.expiryDate) {
        message.error(`Dòng ${i + 1}: Vui lòng chọn hạn dùng.`)
        return
      }

      const expDay = dayjs(item.expiryDate).startOf('day')
      if (!expDay.isAfter(today)) {
        message.error(`Dòng ${i + 1}: Hạn sử dụng phải là ngày trong tương lai.`)
        return
      }

      const qty = Number(item.quantity)
      if (item.quantity == null || isNaN(qty) || qty <= 0 || !Number.isInteger(qty)) {
        message.error(`Dòng ${i + 1}: Số lượng nhập phải là số nguyên lớn hơn 0.`)
        return
      }

      const price = Number(item.importPrice)
      if (item.importPrice == null || isNaN(price) || price < 0) {
        message.error(`Dòng ${i + 1}: Đơn giá nhập không được âm.`)
        return
      }
    }

    const payload = {
      note: String(values.note || '').trim() || null,
      items: rawItems.map((item) => ({
        medicineId: item.medicineId,
        batchNumber: String(item.batchNumber).trim(),
        expiryDate: item.expiryDate.format('YYYY-MM-DD'),
        quantity: Number(item.quantity),
        importPrice: Number(item.importPrice || 0),
      })),
    }

    setSubmitting(true)
    try {
      const response = await pharmacyApi.receiveBatch(payload)
      const receiptData = response?.data

      message.success(
        `Đã tạo phiếu nhập kho thành công${receiptData?.id ? ` (Mã phiếu: ${String(receiptData.id).slice(-8)})` : ''}.`
      )

      if (receiptData) {
        setRecentReceipts((prev) => [receiptData, ...prev])
      }

      const warnings = Array.isArray(receiptData?.warnings) ? receiptData.warnings : []
      if (warnings.length > 0) {
        notification.warning({
          message: 'Lưu ý gộp lô thuốc đã tồn tại',
          description: warnings.map((w, idx) => (
            <div key={idx} style={{ marginBottom: 4 }}>
              Số lô <strong>{w.batchNumber}</strong> đã có sẵn trong kho và đã được gộp số lượng tồn trên Backend.
            </div>
          )),
          duration: 6,
        })
      }

      form.resetFields()
      form.setFieldsValue({ items: [{ ...EMPTY_RECEIPT_ITEM }] })

      await loadData()
      setActiveTab('batches')
    } catch (error) {
      const status = error?.response?.status
      let errorMsg = getErrorMessage(error, 'Không thể tạo phiếu nhập kho.')

      if (status === 400) {
        errorMsg = `Dữ liệu không hợp lệ (400): ${errorMsg}`
      } else if (status === 401) {
        errorMsg = `Phiên làm việc hết hạn (401). Vui lòng đăng nhập lại.`
      } else if (status === 403) {
        errorMsg = `Bạn không có quyền thực hiện thao tác nhập kho (403).`
      } else if (status === 404) {
        errorMsg = `Thuốc hoặc lô không tồn tại trên hệ thống (404).`
      } else if (status === 409) {
        errorMsg = `Xung đột dữ liệu lô (409): ${errorMsg}`
      } else if (status === 500) {
        errorMsg = `Lỗi hệ thống máy chủ (500). Không thể tạo phiếu nhập kho.`
      }

      message.error(errorMsg)
    } finally {
      setSubmitting(false)
    }
  }

  const filteredBatches = useMemo(() => {
    let list = Array.isArray(batches) ? batches : []

    const kw = batchKeyword.trim().toLowerCase()
    if (kw) {
      list = list.filter((b) =>
        [b.batchNumber, b.medicineName, b.medicineCode].some((f) =>
          String(f || '').toLowerCase().includes(kw)
        )
      )
    }

    if (batchStatusFilter !== 'ALL') {
      list = list.filter((b) => String(b.status).toUpperCase() === batchStatusFilter)
    }

    if (batchEligibleFilter === 'ELIGIBLE') {
      list = list.filter((b) => b.eligibleForDispense !== false && b.status !== 'EXPIRED' && b.quantity > 0)
    } else if (batchEligibleFilter === 'INELIGIBLE') {
      list = list.filter((b) => b.eligibleForDispense === false || b.status === 'EXPIRED' || b.quantity === 0)
    }

    if (batchExpiryFilter === 'EXPIRED') {
      list = list.filter((b) => b.expiryDate && dayjs(b.expiryDate).isBefore(dayjs(), 'day'))
    } else if (batchExpiryFilter === 'NEAR_30') {
      list = list.filter((b) => {
        if (!b.expiryDate) return false
        const d = dayjs(b.expiryDate).diff(dayjs(), 'day')
        return d >= 0 && d <= 30
      })
    } else if (batchExpiryFilter === 'NEAR_90') {
      list = list.filter((b) => {
        if (!b.expiryDate) return false
        const d = dayjs(b.expiryDate).diff(dayjs(), 'day')
        return d >= 0 && d <= 90
      })
    } else if (batchExpiryFilter === 'SAFE') {
      list = list.filter((b) => {
        if (!b.expiryDate) return false
        return dayjs(b.expiryDate).diff(dayjs(), 'day') > 90
      })
    }

    return list
  }, [batches, batchKeyword, batchStatusFilter, batchEligibleFilter, batchExpiryFilter])

  const batchColumns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'medicineCode',
      key: 'medicineCode',
      width: 120,
      render: (value) => <Text code>{value || '—'}</Text>,
    },
    {
      title: 'Tên thuốc & Thông tin',
      key: 'medicineName',
      render: (_, batch) => {
        const med = medicineMap.get(String(batch.medicineId))
        return (
          <Space direction="vertical" size={0}>
            <strong>{batch.medicineName || med?.medicineName || '—'}</strong>
            {med && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {[med.activeIngredient, med.strength].filter(Boolean).join(' · ')}
              </Text>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Số lô',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 140,
      render: (value) => (
        <Space>
          <Tag color="geekblue" style={{ fontWeight: 600 }}>{value}</Tag>
          <Tooltip title="Sao chép số lô">
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => {
                navigator.clipboard?.writeText(value)
                message.success(`Đã sao chép số lô: ${value}`)
              }}
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Hạn sử dụng',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 180,
      render: (value) => {
        if (!value) return '—'
        const exp = dayjs(value)
        const days = exp.diff(dayjs(), 'day')
        const isExpired = days < 0
        const isNear30 = days >= 0 && days <= 30
        const isNear90 = days > 30 && days <= 90

        return (
          <Space direction="vertical" size={1}>
            <span style={{ fontWeight: 600 }}>{exp.format('DD/MM/YYYY')}</span>
            {isExpired ? (
              <Tag color="error">Đã hết hạn ({Math.abs(days)} ngày trước)</Tag>
            ) : isNear30 ? (
              <Tag color="volcano">Sắp hết hạn (Còn {days} ngày)</Tag>
            ) : isNear90 ? (
              <Tag color="warning">Cận hạn (Còn {days} ngày)</Tag>
            ) : (
              <Tag color="success">Còn {days} ngày</Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Tồn kho lô',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 120,
      align: 'right',
      render: (value, batch) => {
        const med = medicineMap.get(String(batch.medicineId))
        const q = Number(value || 0)
        return (
          <span style={{ fontWeight: 700, color: q === 0 ? '#94a3b8' : '#1e293b', fontSize: 14 }}>
            {q.toLocaleString('vi-VN')} {med?.unit || ''}
          </span>
        )
      },
    },
    {
      title: 'Trạng thái cấp phát (FEFO)',
      key: 'eligible',
      width: 180,
      render: (_, batch) => {
        const isExpired = batch.expiryDate && dayjs(batch.expiryDate).isBefore(dayjs(), 'day')
        const isDepleted = Number(batch.quantity || 0) === 0
        const isEligible = batch.eligibleForDispense !== false && !isExpired && !isDepleted

        if (isExpired) {
          return <Tag color="red">Hết hạn - Không cấp</Tag>
        }
        if (isDepleted) {
          return <Tag color="default">Đã hết tồn</Tag>
        }
        if (isEligible) {
          return <Tag color="green" icon={<CheckCircleOutlined />}>Được cấp phát (FEFO)</Tag>
        }
        return <Tag color="orange">Không đủ điều kiện</Tag>
      },
    },
    {
      title: 'Thao tác (NCL-06-CN-010)',
      key: 'inventoryOps',
      width: 210,
      render: (_, batch) => {
        const expired = String(batch?.status || '').toUpperCase() === 'EXPIRED'
        const canAdjust = !expired
        const adjustDisabled = !canAdjustDiscard || !canAdjust
        const discardDisabled = !isDiscardableBatch(batch) || !canAdjustDiscard
        return (
          <Space size={6} wrap>
            <Tooltip title={adjustDisabled ? (canAdjust ? 'Bạn không có quyền PHARMACY_UPDATE' : 'Lô đã hủy, không thể điều chỉnh') : 'Điều chỉnh tồn kho sau kiểm kê (QTN-32)'}>
              <Button size="small" icon={<EditOutlined />} disabled={adjustDisabled} onClick={() => openAdjustModal(batch)}>
                Điều chỉnh
              </Button>
            </Tooltip>
            <Tooltip title={!isDiscardableBatch(batch) ? 'Chỉ hủy được lô đã quá hạn và còn tồn' : (!canAdjustDiscard ? 'Yêu cầu quyền PHARMACY_UPDATE' : 'Hủy lô hết hạn (QTN-14, QTN-32)')}>
              <Button size="small" danger icon={<StopOutlined />} disabled={discardDisabled} onClick={() => openDiscardModal(batch)}>
                Hủy lô
              </Button>
            </Tooltip>
          </Space>
        )
      },
    },
  ]

  const alertColumns = [
    {
      title: 'Mã & Tên thuốc',
      key: 'medicine',
      render: (_, item) => (
        <Space direction="vertical" size={1}>
          <strong>{item.medicineName || '—'}</strong>
          <Text code style={{ fontSize: 12 }}>{item.medicineCode || item.medicineId}</Text>
        </Space>
      ),
    },
    {
      title: 'Số lô',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 140,
      render: (val) => <Tag color="blue">{val}</Tag>,
    },
    {
      title: 'Hạn sử dụng',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 150,
      render: (val) => val ? dayjs(val).format('DD/MM/YYYY') : '—',
    },
    {
      title: 'Thời hạn còn lại',
      dataIndex: 'daysToExpiry',
      key: 'daysToExpiry',
      width: 170,
      render: (days, item) => {
        const d = Number(days ?? 0)
        if (item.alertStatus === 'EXPIRED' || d < 0) {
          return <Tag color="red">🔴 Đã quá hạn {Math.abs(d)} ngày</Tag>
        }
        if (d === 0) {
          return <Tag color="volcano">⚠️ Hết hạn hôm nay</Tag>
        }
        return <Tag color="warning">🟡 Còn {d} ngày</Tag>
      },
    },
    {
      title: 'Số lượng còn',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 130,
      align: 'right',
      render: (val) => (
        <span style={{ fontWeight: 700, color: Number(val || 0) > 0 ? '#dc2626' : '#94a3b8' }}>
          {Number(val || 0).toLocaleString('vi-VN')}
        </span>
      ),
    },
    {
      title: 'Trạng thái cảnh báo',
      dataIndex: 'alertStatus',
      key: 'alertStatus',
      width: 280,
      render: (alertStatus) => {
        if (alertStatus === 'EXPIRED') {
          return <Tag color="red" icon={<AlertOutlined />}>🔴 Lô thuốc đã hết hạn – Không được cấp phát</Tag>
        }
        if (alertStatus === 'NEAR_EXPIRY') {
          return <Tag color="orange" icon={<WarningOutlined />}>🟡 Lô thuốc sắp hết hạn</Tag>
        }
        return <Tag>{alertStatus || '—'}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 260,
      align: 'center',
      render: (_, record) => (
        <Space size={6} wrap>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => {
              form.setFieldsValue({
                items: [
                  {
                    medicineId: record.medicineId,
                    batchNumber: '',
                    expiryDate: null,
                    quantity: Math.max(Number(record.quantity) || 10, 10),
                    importPrice: 0,
                  },
                ],
                note: `Phiếu nhập thay thế lô ${record.batchNumber} sắp/đã hết hạn`,
              })
              setActiveTab('create')
            }}
          >
            Nhập lô mới
          </Button>
          <Tooltip title={!isDiscardableBatch(record) ? 'Chỉ hủy được lô đã quá hạn và còn tồn' : (!canAdjustDiscard ? 'Yêu cầu quyền PHARMACY_UPDATE' : 'Hủy lô hết hạn')}>
            <Button
              danger
              size="small"
              icon={<StopOutlined />}
              disabled={!canAdjustDiscard || !isDiscardableBatch(record)}
              onClick={() => openDiscardModal(record)}
            >
              Hủy lô
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ]

  const receiptHistoryColumns = [
    {
      title: 'Mã phiếu',
      dataIndex: 'id',
      key: 'id',
      width: 160,
      render: (val) => <Text code strong color="blue">{String(val || '—').slice(-8).toUpperCase()}</Text>,
    },
    {
      title: 'Thời điểm nhập',
      dataIndex: 'receivedAt',
      key: 'receivedAt',
      width: 180,
      render: (val, row) => dayjs(val || row.createdAt).format('HH:mm DD/MM/YYYY'),
    },
    {
      title: 'Người thực hiện nhập',
      dataIndex: 'receivedBy',
      key: 'receivedBy',
      width: 220,
      render: (val) => (
        <Space>
          <UserOutlined style={{ color: '#2563eb' }} />
          <Text strong>{user?.fullName || user?.username || val || 'Dược sĩ'}</Text>
        </Space>
      ),
    },
    {
      title: 'Số dòng thuốc',
      dataIndex: 'items',
      key: 'itemCount',
      width: 120,
      align: 'center',
      render: (items) => <Badge count={items?.length || 0} showZero color="#2563eb" />,
    },
    {
      title: 'Ghi chú phiếu',
      dataIndex: 'note',
      key: 'note',
      render: (val) => val ? <Text type="secondary">{val}</Text> : '—',
    },
  ]

  if (!canManageReceipts) {
    return (
      <Card style={{ marginTop: 24 }}>
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          message="Bạn không có quyền nhập kho"
          description="Chức năng nhập kho theo lô và quản lý hạn dùng chỉ dành cho tài khoản có vai trò Dược sĩ (Pharmacist) hoặc Quản trị viên."
          action={
            <Button type="primary" onClick={() => navigate('/pharmacy')}>
              Về màn hình Cấp phát thuốc
            </Button>
          }
        />
      </Card>
    )
  }

  return (
    <div style={{ paddingBottom: 40 }}>
      <div
        className="page-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 16,
        }}
      >
        <div>
          <Title level={2} style={{ margin: 0 }}>
            <InboxOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Nhập kho theo lô & Quản lý Hạn dùng
          </Title>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/pharmacy')}>
            Về Cấp phát thuốc
          </Button>
          <Button icon={<ShopOutlined />} onClick={() => navigate('/medicines')}>
            Danh mục & Ngưỡng tồn
          </Button>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
            Làm mới
          </Button>
        </Space>
      </div>

      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu kho từ máy chủ"
          description={loadError}
          action={<Button size="small" onClick={loadData}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col xs={12} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8, height: '100%' }} styles={{ body: { padding: '12px', height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' } }}>
            <Statistic
              title={<div style={{ minHeight: 38, display: 'flex', alignItems: 'center', fontSize: 13, lineHeight: '18px', color: '#64748b' }}>Tổng số lô trong kho</div>}
              value={stats.totalBatches}
              valueStyle={{ fontWeight: 700 }}
              prefix={<InboxOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8, height: '100%' }} styles={{ body: { padding: '12px', height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' } }}>
            <Statistic
              title={<div style={{ minHeight: 38, display: 'flex', alignItems: 'center', fontSize: 13, lineHeight: '18px', color: '#64748b' }}>Lô đủ điều kiện cấp phát (FEFO)</div>}
              value={stats.eligibleBatches}
              valueStyle={{ color: '#52c41a', fontWeight: 700 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card
            size="small"
            style={{
              borderRadius: 8,
              borderLeft: stats.nearExpiryBatches > 0 ? '4px solid #faad14' : undefined,
              cursor: 'pointer',
              height: '100%',
            }}
            styles={{ body: { padding: '12px', height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' } }}
            onClick={() => setActiveTab('alerts')}
          >
            <Statistic
              title={<div style={{ minHeight: 38, display: 'flex', alignItems: 'center', fontSize: 13, lineHeight: '18px', color: '#64748b' }}>Lô sắp hết hạn (≤ 90 ngày)</div>}
              value={stats.nearExpiryBatches}
              valueStyle={stats.nearExpiryBatches > 0 ? { color: '#faad14', fontWeight: 700 } : { fontWeight: 700 }}
              prefix={<FieldTimeOutlined />}
              suffix={<Text type="secondary" style={{ fontSize: 12, marginLeft: 4 }}>Xem →</Text>}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card size="small" style={{ borderRadius: 8, height: '100%' }} styles={{ body: { padding: '12px', height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' } }}>
            <Statistic
              title={<div style={{ minHeight: 38, display: 'flex', alignItems: 'center', fontSize: 13, lineHeight: '18px', color: '#64748b' }}>Lô hết hạn / Hết tồn</div>}
              value={stats.expiredOrDepleted}
              valueStyle={stats.expiredOrDepleted > 0 ? { color: '#ff4d4f', fontWeight: 700 } : { fontWeight: 700 }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card styles={{ body: { padding: '16px 20px' } }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'create',
              label: (
                <Space>
                  <PlusOutlined />
                  <span>Tạo phiếu nhập kho theo lô</span>
                </Space>
              ),
              children: (
                <div>
                  <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                      note: '',
                      items: [
                        {
                          ...EMPTY_RECEIPT_ITEM,
                        },
                      ],
                    }}
                    onFinish={handleSubmit}
                  >
                    <Row gutter={[16, 16]}>
                      <Col xs={24} md={15} lg={16}>
                        <Form.Item
                          name="note"
                          label="Ghi chú phiếu nhập kho"
                          style={{ marginBottom: 4 }}
                        >
                          <Input.TextArea
                            rows={2}
                            maxLength={1000}
                            showCount
                            placeholder="Ví dụ: Nhập theo hợp đồng dược phẩm quý III từ Công ty Dược TW"
                          />
                        </Form.Item>
                        <div style={{ marginTop: 6, fontSize: 12, color: '#64748b' }}>
                          Nhập thông tin nguồn cung cấp, hóa đơn chứng từ hoặc biên bản kiểm nhận...
                        </div>
                      </Col>
                      <Col xs={24} md={9} lg={8}>
                        <Card
                          size="small"
                          style={{
                            background: '#f8fafc',
                            borderColor: '#e2e8f0',
                            borderRadius: 8,
                            height: '100%',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                          }}
                          styles={{ body: { padding: '12px 16px' } }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                            <div>
                              <Text type="secondary" style={{ fontSize: 13, fontWeight: 600 }}>Tổng giá trị phiếu nhập:</Text>
                              <div>
                                <Text type="secondary" style={{ fontSize: 12 }}>
                                  {formItems.length} dòng thuốc
                                </Text>
                              </div>
                            </div>
                            <div style={{ fontSize: 21, fontWeight: 700, color: '#1677ff' }}>
                              {totalReceiptAmount.toLocaleString('vi-VN')} ₫
                            </div>
                          </div>
                        </Card>
                      </Col>
                    </Row>

                    <Divider style={{ margin: '16px 0' }} />

                    <Title level={5} style={{ marginBottom: 12 }}>
                      Chi tiết thuốc, số lô và hạn dùng:
                    </Title>

                    <Form.List
                      name="items"
                      rules={[
                        {
                          validator: async (_, items) => {
                            if (!items || items.length === 0) {
                              throw new Error('Phiếu nhập phải có ít nhất một dòng thuốc.')
                            }
                          },
                        },
                      ]}
                    >
                      {(fields, { add, remove }, { errors }) => (
                        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                          {fields.map((field, index) => {
                            const currentItem = formItems[index]
                            const selectedMed = medicineMap.get(String(currentItem?.medicineId))

                            return (
                              <Card
                                key={field.key}
                                size="small"
                                style={{
                                  background: '#ffffff',
                                  borderColor: '#cbd5e1',
                                  borderRadius: 8,
                                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                                }}
                                title={
                                  <Space>
                                    <Tag color="blue">Dòng {index + 1}</Tag>
                                    {selectedMed && (
                                      <Text strong>{selectedMed.medicineName} ({selectedMed.medicineCode})</Text>
                                    )}
                                  </Space>
                                }
                                extra={
                                  fields.length > 1 && (
                                    <Button
                                      danger
                                      type="text"
                                      icon={<DeleteOutlined />}
                                      onClick={() => remove(field.name)}
                                    >
                                      Xóa dòng này
                                    </Button>
                                  )
                                }
                              >
                                <Row gutter={[16, 8]}>
                                  <Col xs={24} md={10}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'medicineId']}
                                      label="Chọn loại thuốc cần nhập *"
                                      rules={[{ required: true, message: 'Vui lòng chọn thuốc.' }]}
                                    >
                                      <Select
                                        showSearch
                                        optionFilterProp="label"
                                        options={medicineOptions}
                                        placeholder="Tìm theo tên thuốc, mã thuốc, hoạt chất..."
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={7}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'batchNumber']}
                                      label="Số lô sản xuất *"
                                      rules={[
                                        { required: true, whitespace: true, message: 'Vui lòng nhập số lô.' },
                                        { max: 100, message: 'Số lô không vượt quá 100 ký tự.' },
                                      ]}
                                    >
                                      <Input
                                        placeholder="Nhập số lô thực tế..."
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={7}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'expiryDate']}
                                      label="Hạn sử dụng (Phải trong tương lai) *"
                                      rules={[{ required: true, message: 'Vui lòng chọn hạn dùng.' }]}
                                    >
                                      <DatePicker
                                        style={{ width: '100%' }}
                                        format="DD/MM/YYYY"
                                        placeholder="Chọn hạn dùng"
                                        disabledDate={(current) => current && !current.isAfter(dayjs(), 'day')}
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={12}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'quantity']}
                                      label={
                                        <Space>
                                          <span>Số lượng nhập *</span>
                                          {selectedMed?.unit && (
                                            <Text type="secondary">(Đơn vị: {selectedMed.unit})</Text>
                                          )}
                                        </Space>
                                      }
                                      rules={[{ required: true, message: 'Vui lòng nhập số lượng.' }]}
                                    >
                                      <InputNumber
                                        min={1}
                                        max={1000000}
                                        precision={0}
                                        style={{ width: '100%' }}
                                        placeholder="Nhập số lượng lớn hơn 0..."
                                        addonAfter={selectedMed?.unit || 'Đơn vị'}
                                      />
                                    </Form.Item>
                                  </Col>

                                  <Col xs={24} md={12}>
                                    <Form.Item
                                      {...field}
                                      name={[field.name, 'importPrice']}
                                      label="Đơn giá nhập (VNĐ) *"
                                      rules={[{ required: true, message: 'Vui lòng nhập đơn giá.' }]}
                                    >
                                      <InputNumber
                                        min={0}
                                        precision={2}
                                        style={{ width: '100%' }}
                                        placeholder="Nhập đơn giá"
                                        addonAfter="₫"
                                        formatter={(val) => `${val}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                                        parser={(val) => val.replace(/\$\s?|(,*)/g, '')}
                                      />
                                    </Form.Item>
                                  </Col>
                                </Row>
                              </Card>
                            )
                          })}

                          <Form.ErrorList errors={errors} />

                          <Button
                            type="dashed"
                            block
                            icon={<PlusOutlined />}
                            onClick={() =>
                              add({
                                ...EMPTY_RECEIPT_ITEM,
                              })
                            }
                            style={{ height: 44, borderRadius: 8 }}
                          >
                            + Thêm dòng thuốc khác vào phiếu nhập
                          </Button>
                        </Space>
                      )}
                    </Form.List>

                    <div style={{ marginTop: 24, display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
                      <Button
                        onClick={() => {
                          form.resetFields()
                          form.setFieldsValue({ items: [{ ...EMPTY_RECEIPT_ITEM }] })
                        }}
                      >
                        Làm lại
                      </Button>
                      <Button
                        type="primary"
                        htmlType="submit"
                        icon={<SaveOutlined />}
                        loading={submitting}
                        disabled={loading || medicines.length === 0}
                      >
                        Xác nhận nhập kho theo lô
                      </Button>
                    </div>
                  </Form>
                </div>
              ),
            },
            {
              key: 'batches',
              label: (
                <Space>
                  <InboxOutlined />
                  <span>Danh sách & Theo dõi Lô thuốc ({batches.length})</span>
                </Space>
              ),
              children: (
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: 12,
                      marginBottom: 16,
                    }}
                  >
                    <Space wrap size="middle">
                      <Input
                        placeholder="Tìm theo số lô, mã, tên thuốc..."
                        allowClear
                        prefix={<SearchOutlined />}
                        style={{ width: 280 }}
                        value={batchKeyword}
                        onChange={(e) => setBatchKeyword(e.target.value)}
                      />

                      <Select
                        defaultValue="ALL"
                        value={batchStatusFilter}
                        style={{ width: 170 }}
                        onChange={setBatchStatusFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả trạng thái lô' },
                          { value: 'ACTIVE', label: '🟢 Đang hoạt động' },
                          { value: 'DEPLETED', label: '⚪ Đã hết tồn' },
                          { value: 'EXPIRED', label: '🔴 Đã hết hạn' },
                        ]}
                      />

                      <Select
                        defaultValue="ALL"
                        value={batchEligibleFilter}
                        style={{ width: 200 }}
                        onChange={setBatchEligibleFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả điều kiện FEFO' },
                          { value: 'ELIGIBLE', label: '🟢 Được cấp phát FEFO' },
                          { value: 'INELIGIBLE', label: '🔴 Không đủ điều kiện' },
                        ]}
                      />

                      <Select
                        defaultValue="ALL"
                        value={batchExpiryFilter}
                        style={{ width: 200 }}
                        onChange={setBatchExpiryFilter}
                        options={[
                          { value: 'ALL', label: 'Tất cả hạn dùng' },
                          { value: 'NEAR_30', label: '⚠️ Sắp hết hạn (≤ 30 ngày)' },
                          { value: 'NEAR_90', label: '🟡 Cận hạn (≤ 90 ngày)' },
                          { value: 'SAFE', label: '🟢 Còn hạn dài (> 90 ngày)' },
                          { value: 'EXPIRED', label: '🔴 Đã hết hạn' },
                        ]}
                      />
                    </Space>

                    <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
                      Làm mới dữ liệu từ Backend
                    </Button>
                  </div>

                  <Table
                    rowKey={(batch) => batch.batchId || `${batch.medicineId}-${batch.batchNumber}`}
                    columns={batchColumns}
                    dataSource={filteredBatches}
                    loading={loading}
                    pagination={{
                      pageSize: 10,
                      showSizeChanger: true,
                      pageSizeOptions: ['10', '20', '50', '100'],
                      showTotal: (total) => `Tổng số ${total} lô thuốc trong kho`,
                    }}
                    scroll={{ x: 950 }}
                    locale={{ emptyText: <Empty description="Không tìm thấy lô thuốc nào phù hợp" /> }}
                  />
                </div>
              ),
            },
            {
              key: 'history',
              label: (
                <Space>
                  <HistoryOutlined />
                  <span>Phiếu nhập vừa tạo ({recentReceipts.length})</span>
                </Space>
              ),
              children: (
                <div>
                  <Table
                    rowKey="id"
                    columns={receiptHistoryColumns}
                    dataSource={recentReceipts}
                    pagination={{ pageSize: 5 }}
                    bordered
                    scroll={{ x: 750 }}
                    locale={{ emptyText: <Empty description="Chưa có phiếu nhập nào trong phiên làm việc hiện tại" /> }}
                  />
                </div>
              ),
            },
            {
              key: 'alerts',
              label: (
                <Space>
                  <WarningOutlined style={{ color: stats.expiryAlertCount > 0 ? '#faad14' : undefined }} />
                  <span>Cảnh báo Hạn sử dụng</span>
                  {stats.expiryAlertCount > 0 && (
                    <Badge count={stats.expiryAlertCount} overflowCount={99} style={{ backgroundColor: '#faad14' }} />
                  )}
                </Space>
              ),
              children: (
                <div>
                  <Alert
                    type="info"
                    showIcon
                    icon={<CalendarOutlined />}
                    message="Quy tắc kiểm soát hạn sử dụng thuốc"
                    description="Thuốc hết hạn hoặc cận hạn sẽ tự động bị loại khỏi danh sách đủ điều kiện cấp phát theo nguyên tắc FEFO (First Expired, First Out) để đảm bảo an toàn tuyệt đối cho bệnh nhân."
                    style={{ marginBottom: 16 }}
                  />

                  <Table
                    rowKey={(item) => item.batchId || `${item.medicineId}-${item.batchNumber}`}
                    columns={alertColumns}
                    dataSource={expiryAlerts}
                    loading={loading}
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                    scroll={{ x: 900 }}
                    locale={{
                      emptyText: (
                        <Empty
                          description="Hiện không có lô thuốc nào cần cảnh báo hạn dùng."
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                        />
                      ),
                    }}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={(
          <Space>
            <EditOutlined style={{ color: '#1677ff' }} />
            <span>Điều chỉnh tồn kho sau kiểm kê</span>
          </Space>
        )}
        open={adjustModalOpen}
        onCancel={closeOperationModals}
        onOk={() => adjustForm.submit()}
        okText="Xác nhận điều chỉnh"
        cancelText="Hủy"
        confirmLoading={submittingAdjust}
        destroyOnClose
        maskClosable={!submittingAdjust}
      >
        {selectedBatch && (
          <>
            <Descriptions bordered size="small" column={1} style={{ marginBottom: 20 }}>
              <Descriptions.Item label="Thuốc">
                <strong>{selectedBatch.medicineName || medicineMap.get(String(selectedBatch.medicineId))?.medicineName || '—'}</strong>
                {selectedBatch.medicineCode && <Text type="secondary"> ({selectedBatch.medicineCode})</Text>}
              </Descriptions.Item>
              <Descriptions.Item label="Số lô">{selectedBatch.batchNumber || '—'}</Descriptions.Item>
              <Descriptions.Item label="Hạn sử dụng">
                {selectedBatch.expiryDate ? dayjs(selectedBatch.expiryDate).format('DD/MM/YYYY') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Tồn hiện tại">
                <strong>{Number(selectedBatch.quantity || 0).toLocaleString('vi-VN')}</strong>{' '}
                {selectedBatch.unit || medicineMap.get(String(selectedBatch.medicineId))?.unit || ''}
              </Descriptions.Item>
            </Descriptions>
            <Form form={adjustForm} layout="vertical" onFinish={handleAdjustSubmit} preserve={false}>
              <Form.Item
                name="actualQuantity"
                label="Số lượng thực tế sau kiểm kê *"
                rules={[
                  { required: true, message: 'Vui lòng nhập số lượng thực tế.' },
                  { type: 'number', min: 0, message: 'Số lượng thực tế không được âm.' },
                ]}
              >
                <InputNumber
                  min={0}
                  precision={0}
                  style={{ width: '100%' }}
                  placeholder="Nhập số lượng thực tế"
                  addonAfter={selectedBatch.unit || medicineMap.get(String(selectedBatch.medicineId))?.unit || 'Đơn vị'}
                />
              </Form.Item>
              <Form.Item
                name="reason"
                label="Lý do điều chỉnh *"
                rules={[
                  { required: true, whitespace: true, message: 'Vui lòng nhập lý do điều chỉnh theo QTN-32.' },
                  { max: 500, message: 'Lý do không được vượt quá 500 ký tự.' },
                ]}
              >
                <Input.TextArea
                  rows={4}
                  maxLength={500}
                  showCount
                  placeholder="Ví dụ: Kiểm kê cuối tháng phát hiện thuốc bị vỡ hoặc thất thoát..."
                />
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>

      <Modal
        title={(
          <Space>
            <StopOutlined style={{ color: '#ff4d4f' }} />
            <span>Hủy lô thuốc hết hạn</span>
          </Space>
        )}
        open={discardModalOpen}
        onCancel={closeOperationModals}
        onOk={() => discardForm.submit()}
        okText="Xác nhận hủy lô"
        cancelText="Đóng"
        okButtonProps={{ danger: true }}
        confirmLoading={submittingDiscard}
        destroyOnClose
        maskClosable={!submittingDiscard}
      >
        {selectedBatch && (
          <>
            <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Thuốc">
                <strong>{selectedBatch.medicineName || medicineMap.get(String(selectedBatch.medicineId))?.medicineName || '—'}</strong>
                {selectedBatch.medicineCode && <Text type="secondary"> ({selectedBatch.medicineCode})</Text>}
              </Descriptions.Item>
              <Descriptions.Item label="Số lô">{selectedBatch.batchNumber || '—'}</Descriptions.Item>
              <Descriptions.Item label="Ngày hết hạn">
                <Tag color="red">{selectedBatch.expiryDate ? dayjs(selectedBatch.expiryDate).format('DD/MM/YYYY') : '—'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Số lượng sẽ hủy">
                <strong style={{ color: '#cf1322' }}>{Number(selectedBatch.quantity || 0).toLocaleString('vi-VN')}</strong>{' '}
                {selectedBatch.unit || medicineMap.get(String(selectedBatch.medicineId))?.unit || ''}
              </Descriptions.Item>
            </Descriptions>
            <Alert
              type="warning"
              showIcon
              message="Lô sẽ chuyển sang trạng thái EXPIRED"
              description="Số lượng tồn sẽ về 0 và lô bị loại khỏi danh sách cấp phát FEFO. Thao tác này cần lý do theo QTN-32."
              style={{ marginBottom: 16 }}
            />
            <Form form={discardForm} layout="vertical" onFinish={handleDiscardSubmit} preserve={false}>
              <Form.Item
                name="reason"
                label="Lý do hủy lô *"
                rules={[
                  { required: true, whitespace: true, message: 'Vui lòng nhập lý do hủy lô theo QTN-32.' },
                  { max: 500, message: 'Lý do không được vượt quá 500 ký tự.' },
                ]}
              >
                <Input.TextArea
                  rows={4}
                  maxLength={500}
                  showCount
                  placeholder="Ví dụ: Hủy theo biên bản tiêu hủy lô thuốc hết hạn..."
                />
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </div>
  )
}

export default InventoryReceiptPage
