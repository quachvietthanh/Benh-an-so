import React, { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Empty,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  BarcodeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  FieldTimeOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  RollbackOutlined,
  SwapOutlined,
  ThunderboltOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import prescriptionDispenseApi from '../../api/prescriptionDispenseApi.js'
import pharmacyApi from '../../api/pharmacyApi.js'
import {
  buildFefoDispensePayload,
  calculateItemShortage,
  getDaysUntilExpiry,
  getExpiryStatusTag,
  getRemainingQuantity,
  hasShortageAfterDispense,
  mapDispenseError,
  validateBatchChangeReason,
} from '../../utils/partialDispensingHelpers.js'

const { Text, Title } = Typography

function PartialDispenseModal({ open, onClose, prescription, onSuccess }) {
  const [loadingDetails, setLoadingDetails] = useState(false)
  const [loadingSuggestions, setLoadingSuggestions] = useState(false)
  const [prescriptionData, setPrescriptionData] = useState(null)
  const [quantities, setQuantities] = useState({})
  const [suggestions, setSuggestions] = useState([])
  const [selectedBatches, setSelectedBatches] = useState({})
  const [batchChangeReasons, setBatchChangeReasons] = useState({})
  const [validationErrors, setValidationErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [submittingQuick, setSubmittingQuick] = useState(false)
  const [serverShortages, setServerShortages] = useState([])
  const [errorMessage, setErrorMessage] = useState('')
  const [stocks, setStocks] = useState([])
  const [expandedRowKeys, setExpandedRowKeys] = useState([])

  useEffect(() => {
    if (!open || !prescription) {
      setPrescriptionData(null)
      setQuantities({})
      setSuggestions([])
      setSelectedBatches({})
      setBatchChangeReasons({})
      setValidationErrors({})
      setServerShortages([])
      setErrorMessage('')
      setStocks([])
      setExpandedRowKeys([])
      return
    }

    const rxId = prescription.id || prescription.prescriptionId
    if (!rxId) return

    pharmacyApi
      .stocks({ active: true })
      .then((res) => {
        const list = Array.isArray(res?.data) ? res.data : (res?.data?.content || [])
        setStocks(list)
      })
      .catch((err) => {
        console.warn('[PartialDispenseModal] Không tải được dữ liệu tồn kho:', err)
      })

    const rawItems = prescription.items || []
    if (rawItems.length > 0) {
      setPrescriptionData(prescription)
      initQuantities(rawItems)
    } else {
      setLoadingDetails(true)
      pharmacyApi
        .getById(rxId)
        .then((res) => {
          const data = res?.data || prescription
          setPrescriptionData(data)
          initQuantities(data.items || [])
        })
        .catch((err) => {
          console.warn('[PartialDispenseModal] Không tải được chi tiết đơn:', err)
          setPrescriptionData(prescription)
          initQuantities([])
        })
        .finally(() => setLoadingDetails(false))
    }

    // Tải gợi ý phân bổ lô theo nguyên tắc FEFO từ backend
    setLoadingSuggestions(true)
    prescriptionDispenseApi
      .getDispenseSuggestion(rxId)
      .then((res) => {
        const data = res?.data
        const items = data?.items || []
        setSuggestions(items)

        // Mặc định chọn sẵn lô FEFO ưu tiên số 1 cho từng thuốc
        const initialBatches = {}
        const autoExpandKeys = []
        items.forEach((item) => {
          const itemId = item.prescriptionItemId || item.id
          if (itemId && Array.isArray(item.batches) && item.batches.length > 0) {
            initialBatches[itemId] = item.batches[0].batchId
            if (item.batches.length > 1) {
              autoExpandKeys.push(itemId)
            }
          }
        })
        setSelectedBatches(initialBatches)
        if (autoExpandKeys.length > 0) {
          setExpandedRowKeys(autoExpandKeys)
        }
      })
      .catch((err) => {
        console.warn('[PartialDispenseModal] Không tải được gợi ý FEFO:', err)
      })
      .finally(() => setLoadingSuggestions(false))
  }, [open, prescription])

  const initQuantities = (items) => {
    const initial = {}
    items.forEach((item) => {
      const itemId = item.id || item.prescriptionItemId
      if (itemId) {
        initial[itemId] = getRemainingQuantity(item)
      }
    })
    setQuantities(initial)
    setServerShortages([])
    setErrorMessage('')
    setValidationErrors({})
  }

  const items = useMemo(() => {
    return prescriptionData?.items || []
  }, [prescriptionData])

  const suggestionsMap = useMemo(() => {
    const map = {}
    suggestions.forEach((s) => {
      const key = s.prescriptionItemId || s.id
      if (key) {
        map[key] = s
      }
    })
    return map
  }, [suggestions])

  const stockByMedicineId = useMemo(() => {
    const map = new Map()
    stocks.forEach((s) => {
      const id = s?.medicineId || s?.id
      if (id) {
        map.set(String(id), s)
      }
    })
    return map
  }, [stocks])

  const handleQuantityChange = (itemId, val, maxQuantity) => {
    let nextVal = val
    if (nextVal === null || nextVal === undefined) {
      nextVal = 0
    }
    if (nextVal < 0) {
      nextVal = 0
    }
    if (nextVal > maxQuantity) {
      nextVal = maxQuantity
    }

    setQuantities((prev) => ({
      ...prev,
      [itemId]: nextVal,
    }))
    setServerShortages([])
    setErrorMessage('')
  }

  const handleBatchSelect = (itemId, newBatchId) => {
    setSelectedBatches((prev) => ({
      ...prev,
      [itemId]: newBatchId,
    }))

    const suggestion = suggestionsMap[itemId]
    const fefoFirst = suggestion?.batches?.[0]
    if (fefoFirst && String(newBatchId) === String(fefoFirst.batchId)) {
      setBatchChangeReasons((prev) => {
        const next = { ...prev }
        delete next[itemId]
        return next
      })
      setValidationErrors((prev) => {
        const next = { ...prev }
        delete next[itemId]
        return next
      })
    } else {
      // Tự động mở rộng dòng để dược sĩ thấy ô nhập lý do
      if (!expandedRowKeys.includes(itemId)) {
        setExpandedRowKeys((prev) => [...prev, itemId])
      }
    }
  }

  const handleReasonChange = (itemId, text) => {
    setBatchChangeReasons((prev) => ({
      ...prev,
      [itemId]: text,
    }))
    if (text && text.trim().length > 0) {
      setValidationErrors((prev) => {
        const next = { ...prev }
        delete next[itemId]
        return next
      })
    }
  }

  const isItemBatchOverridden = (itemId) => {
    const suggestion = suggestionsMap[itemId]
    const fefoFirst = suggestion?.batches?.[0]
    if (!fefoFirst) return false
    const currentSelected = selectedBatches[itemId] || fefoFirst.batchId
    return String(currentSelected) !== String(fefoFirst.batchId)
  }

  const executeSubmit = async (payloadItems) => {
    const rxId = prescriptionData?.id || prescription?.id
    if (!rxId) {
      message.error('Không tìm thấy mã đơn thuốc.')
      return
    }

    setSubmitting(true)
    setServerShortages([])
    setErrorMessage('')

    try {
      const response = await prescriptionDispenseApi.partialDispense(rxId, payloadItems)
      const data = response?.data

      message.success('Cấp phát đơn thuốc theo lô thành công!')
      if (onSuccess) {
        onSuccess(data || { status: 'PARTIALLY_DISPENSED' })
      }
      onClose()
    } catch (err) {
      const mapped = mapDispenseError(err, rxId, payloadItems)
      const errorCode = mapped.code || ''

      if (errorCode === 'INSUFFICIENT_STOCK' && mapped.status === 409) {
        setServerShortages(mapped.shortages)
        setErrorMessage(mapped.message)
        message.error(mapped.message)
      } else if (errorCode === 'DATA_INTEGRITY_VIOLATION') {
        setErrorMessage(mapped.message)
        message.error(
          'Dữ liệu cấp phát không hợp lệ hoặc bị xung đột ràng buộc hệ thống. Vui lòng tải lại trang và thử lại. Nếu lỗi vẫn tiếp diễn, liên hệ quản trị viên hệ thống.'
        )
      } else if (mapped.status === 500) {
        setErrorMessage(mapped.message)
        message.error(mapped.message)
      } else {
        setErrorMessage(mapped.message)
        message.error(mapped.message)
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleConfirmDispense = () => {
    const { payloadItems, hasAnyItemToDispense, validationErrors: errors, isValid } = buildFefoDispensePayload(
      quantities,
      items,
      selectedBatches,
      batchChangeReasons,
      suggestionsMap
    )

    if (!hasAnyItemToDispense) {
      message.warning('Vui lòng nhập số lượng cấp phát lớn hơn 0 cho ít nhất một loại thuốc.')
      return
    }

    if (!isValid) {
      setValidationErrors(errors)
      message.error('Vui lòng nhập đầy đủ lý do đổi lô cho những thuốc đã thay đổi so với đề xuất FEFO.')
      // Mở rộng tất cả dòng có lỗi để người dùng quan sát
      const errorKeys = Object.keys(errors)
      setExpandedRowKeys((prev) => Array.from(new Set([...prev, ...errorKeys])))
      return
    }

    const willHaveShortage = hasShortageAfterDispense(quantities, items)

    if (willHaveShortage) {
      Modal.confirm({
        title: 'Cảnh báo cấp phát một phần',
        icon: <ExclamationCircleOutlined style={{ color: '#f59e0b', fontSize: 24 }} />,
        content: (
          <div style={{ marginTop: 8, fontSize: 14 }}>
            <p style={{ color: '#334155', lineHeight: 1.6, marginBottom: 8 }}>
              Một số thuốc sẽ không được cấp đủ theo đơn. Bệnh nhân sẽ cần quay lại nhận phần còn thiếu sau.
            </p>
            <p style={{ color: '#d97706', fontWeight: 600, margin: 0 }}>
              Bạn có chắc chắn muốn tiếp tục xuất kho cho đợt cấp này?
            </p>
          </div>
        ),
        okText: 'Xác nhận cấp phát',
        cancelText: 'Kiểm tra lại',
        okButtonProps: {
          type: 'primary',
          style: { backgroundColor: '#d97706', borderColor: '#d97706', height: 40, minWidth: 120, fontWeight: 600 },
        },
        cancelButtonProps: {
          style: { height: 40, minWidth: 100 },
        },
        onOk: () => executeSubmit(payloadItems),
      })
    } else {
      executeSubmit(payloadItems)
    }
  }

  const handleQuickFefoDispense = async () => {
    const rxId = prescriptionData?.id || prescription?.id
    if (!rxId) {
      message.error('Không tìm thấy mã đơn thuốc.')
      return
    }

    setSubmittingQuick(true)
    setServerShortages([])
    setErrorMessage('')

    try {
      const response = await prescriptionDispenseApi.dispenseAll(rxId)
      message.success('Cấp phát tự động toàn bộ theo nguyên tắc FEFO thành công!')
      if (onSuccess) {
        onSuccess(response?.data || { status: 'DISPENSED' })
      }
      onClose()
    } catch (err) {
      const mapped = mapDispenseError(err, rxId, [])
      const responseMsg = String(err?.response?.data?.message || '')
      if (mapped.code === 'INSUFFICIENT_STOCK' && mapped.status === 409) {
        setServerShortages(mapped.shortages)
        setErrorMessage(mapped.message)
        message.error(mapped.message)
      } else if (err?.response?.status === 409 && (responseMsg.includes('Partially dispensed') || responseMsg.includes('partially dispensed'))) {
        const viMsg = 'Đơn thuốc này đã được cấp phát một phần trước đó. Vui lòng sử dụng nút "Xác nhận cấp phát" để hoàn tất số lượng còn thiếu.'
        setErrorMessage(viMsg)
        message.error(viMsg)
      } else {
        setErrorMessage(mapped.message)
        message.error(mapped.message)
      }
    } finally {
      setSubmittingQuick(false)
    }
  }

  const shortageDetailsColumns = [
    { title: 'Tên thuốc', dataIndex: 'medicineName', key: 'medicineName' },
    { title: 'Yêu cầu cấp', dataIndex: 'requiredQuantity', key: 'requiredQuantity', align: 'center' },
    { title: 'Tồn khả dụng', dataIndex: 'availableQuantity', key: 'availableQuantity', align: 'center' },
    {
      title: 'Thiếu hụt',
      dataIndex: 'shortageQuantity',
      key: 'shortageQuantity',
      align: 'center',
      render: (val) => <Tag color="red">Thiếu {val}</Tag>,
    },
  ]

  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 50,
      align: 'center',
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Tên thuốc & Quy cách',
      key: 'medicineName',
      render: (_, item) => {
        const itemId = item.id || item.prescriptionItemId
        const suggestion = suggestionsMap[itemId]
        const batches = suggestion?.batches || []
        const hasMultipleBatches = batches.length > 1
        const isOverridden = isItemBatchOverridden(itemId)

        return (
          <div>
            <Text strong style={{ color: '#1e40af', fontSize: 14 }}>
              {item.medicineName || `Mã ${item.medicineId}`}
            </Text>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
              {[item.activeIngredient, item.strength].filter(Boolean).join(' · ') || item.dosage || '—'}
            </div>
            <Space size={4} style={{ marginTop: 4 }} wrap>
              {hasMultipleBatches && (
                <Tag color="purple" style={{ fontWeight: 600 }}>
                  <ThunderboltOutlined style={{ marginRight: 3 }} />
                  Xuất từ {batches.length} lô
                </Tag>
              )}
              {isOverridden && (
                <Tag color="volcano" style={{ fontWeight: 600 }}>
                  <SwapOutlined style={{ marginRight: 3 }} />
                  Đã đổi lô thủ công
                </Tag>
              )}
              {validationErrors[itemId] && (
                <Tag color="error" style={{ fontWeight: 600 }}>
                  Chưa nhập lý do đổi lô
                </Tag>
              )}
            </Space>
          </div>
        )
      },
    },
    {
      title: 'SL kê',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 85,
      align: 'center',
      render: (qty, item) => (
        <Text strong>
          {qty} {item.unit || ''}
        </Text>
      ),
    },
    {
      title: 'Đã cấp',
      key: 'dispensedQuantity',
      width: 85,
      align: 'center',
      render: (_, item) => {
        const dispensed = item.dispensedQuantity || 0
        return <Tag color={dispensed > 0 ? 'blue' : 'default'}>{dispensed} {item.unit || ''}</Tag>
      },
    },
    {
      title: 'Tồn kho',
      key: 'stockQuantity',
      width: 110,
      align: 'center',
      render: (_, item) => {
        const itemId = item.id || item.prescriptionItemId
        const suggestion = suggestionsMap[itemId]
        let stockQty = 0
        if (suggestion?.batches && suggestion.batches.length > 0) {
          stockQty = suggestion.batches.reduce((sum, b) => sum + (b.availableQuantity || 0), 0)
        } else {
          const medId = item.medicineId || item.medicine?.id || item.id
          const stock = stockByMedicineId.get(String(medId))
          stockQty =
            stock?.eligibleStockQuantity ??
            stock?.stockQuantity ??
            item.stockQuantity ??
            item.availableStock ??
            item.availableQuantity ??
            0
        }
        return (
          <Tag color={stockQty > 0 ? 'green' : 'orange'} style={{ fontWeight: 600 }}>
            {stockQty} {item.unit || ''}
          </Tag>
        )
      },
    },
    {
      title: 'Lô ưu tiên xuất',
      key: 'selectedBatch',
      width: 210,
      render: (_, item) => {
        const itemId = item.id || item.prescriptionItemId
        const suggestion = suggestionsMap[itemId]
        const batches = suggestion?.batches || []
        const fefoFirst = batches[0]
        const currentBatchId = selectedBatches[itemId] || fefoFirst?.batchId
        const isOverridden = isItemBatchOverridden(itemId)

        if (batches.length === 0) {
          return <span style={{ color: '#94a3b8', fontSize: 13 }}>Không có lô còn hạn</span>
        }

        return (
          <div style={{ width: '100%' }}>
            <Select
              value={currentBatchId}
              onChange={(val) => handleBatchSelect(itemId, val)}
              style={{ width: '100%' }}
              size="middle"
              status={validationErrors[itemId] ? 'error' : ''}
              options={batches.map((b, idx) => {
                const tagInfo = getExpiryStatusTag(b.expiryDate)
                return {
                  value: b.batchId,
                  label: (
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>
                        <strong>{b.batchNumber}</strong>
                        {idx === 0 && <Tag color="green" style={{ marginLeft: 6, fontSize: 11 }}>FEFO #1</Tag>}
                      </span>
                      <span style={{ fontSize: 12, color: tagInfo.color === 'red' ? '#ef4444' : tagInfo.color === 'orange' ? '#d97706' : '#64748b' }}>
                        HSD: {dayjs(b.expiryDate).format('DD/MM/YYYY')}
                      </span>
                    </div>
                  ),
                }
              })}
            />
            {isOverridden && (
              <div style={{ fontSize: 12, color: '#d97706', marginTop: 4, fontWeight: 500 }}>
                ⚠️ Khác lô FEFO đề xuất ban đầu
              </div>
            )}
          </div>
        )
      },
    },
    {
      title: 'Số lượng thực cấp',
      key: 'actualDispense',
      width: 185,
      align: 'left',
      render: (_, item) => {
        const itemId = item.id || item.prescriptionItemId
        const remaining = getRemainingQuantity(item)
        const currentVal = quantities[itemId] !== undefined ? quantities[itemId] : remaining
        const shortage = calculateItemShortage(currentVal, remaining)
        const isSkipped = currentVal === 0

        return (
          <div>
            <InputNumber
              min={0}
              max={remaining}
              step={1}
              precision={0}
              disabled={submitting || remaining <= 0}
              value={currentVal}
              onChange={(val) => handleQuantityChange(itemId, val, remaining)}
              style={{ width: '100%' }}
              addonAfter={item.unit || 'đơn vị'}
            />
            {remaining > 0 && shortage > 0 && !isSkipped && (
              <div style={{ color: '#d97706', fontSize: 12, fontWeight: 500, marginTop: 4 }}>
                Còn thiếu {shortage} sau lần cấp này
              </div>
            )}
            {remaining > 0 && isSkipped && (
              <div style={{ color: '#ef4444', fontSize: 12, fontWeight: 500, marginTop: 4 }}>
                Không cấp đợt này (thiếu {remaining})
              </div>
            )}
            {remaining === 0 && (
              <div style={{ color: '#16a34a', fontSize: 12, marginTop: 4 }}>
                Đã cấp đủ theo đơn
              </div>
            )}
          </div>
        )
      },
    },
  ]

  const expandedRowRender = (record) => {
    const itemId = record.id || record.prescriptionItemId
    const suggestion = suggestionsMap[itemId]
    const batches = suggestion?.batches || []
    const fefoFirst = batches[0]
    const currentBatchId = selectedBatches[itemId] || fefoFirst?.batchId
    const isOverridden = isItemBatchOverridden(itemId)

    if (batches.length === 0) {
      return (
        <div style={{ padding: '10px 16px', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: 6 }}>
          Chưa có danh sách lô khả dụng từ hệ thống cho thuốc này.
        </div>
      )
    }

    const batchColumns = [
      {
        title: 'Mã số lô',
        dataIndex: 'batchNumber',
        key: 'batchNumber',
        width: 140,
        render: (text, batch, idx) => (
          <Space>
            <strong style={{ color: '#1e293b' }}>{text}</strong>
            {idx === 0 && (
              <Tag color="green" style={{ fontWeight: 600 }}>
                Ưu tiên FEFO
              </Tag>
            )}
          </Space>
        ),
      },
      {
        title: 'Hạn sử dụng',
        dataIndex: 'expiryDate',
        key: 'expiryDate',
        width: 190,
        render: (date) => {
          const tagInfo = getExpiryStatusTag(date)
          return (
            <Space size={6}>
              <span>{date ? dayjs(date).format('DD/MM/YYYY') : '—'}</span>
              <Tag color={tagInfo.color} style={{ fontSize: 11 }}>
                {tagInfo.label}
              </Tag>
            </Space>
          )
        },
      },
      {
        title: 'Tồn khả dụng',
        dataIndex: 'availableQuantity',
        key: 'availableQuantity',
        width: 110,
        align: 'center',
        render: (val) => <Text strong>{val} {record.unit || ''}</Text>,
      },
      {
        title: 'Gợi ý xuất (FEFO)',
        dataIndex: 'suggestedQuantity',
        key: 'suggestedQuantity',
        width: 130,
        align: 'center',
        render: (val) => (
          <Tag color={val > 0 ? 'cyan' : 'default'} style={{ fontWeight: 600, fontSize: 13 }}>
            {val} {record.unit || ''}
          </Tag>
        ),
      },
      {
        title: 'Lựa chọn lô xuất',
        key: 'action',
        width: 150,
        align: 'center',
        render: (_, batch) => {
          const isSelected = String(currentBatchId) === String(batch.batchId)
          return isSelected ? (
            <Tag color="geekblue" icon={<CheckCircleOutlined />} style={{ padding: '4px 10px', fontSize: 12, fontWeight: 600 }}>
              Đang chọn lô này
            </Tag>
          ) : (
            <Button
              size="middle"
              onClick={() => handleBatchSelect(itemId, batch.batchId)}
              style={{ height: 36, borderRadius: 6 }}
            >
              Chọn lô này
            </Button>
          )
        },
      },
    ]

    return (
      <div style={{ backgroundColor: '#f8fafc', padding: '14px 18px', borderRadius: 8, border: '1px solid #e2e8f0' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <Space>
            <ClockCircleOutlined style={{ color: '#0284c7' }} />
            <Text strong style={{ color: '#0369a1', fontSize: 13.5 }}>
              Chi tiết các lô thuốc còn hạn sử dụng (Sắp xếp theo hạn dùng gần trước - FEFO):
            </Text>
          </Space>
          {batches.length > 1 && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              Hệ thống tự động phân bổ theo thứ tự ưu tiên hạn sử dụng tăng dần.
            </Text>
          )}
        </div>

        <Table
          rowKey="batchId"
          columns={batchColumns}
          dataSource={batches}
          pagination={false}
          size="small"
          bordered
        />

        {isOverridden && (
          <div style={{ marginTop: 14, padding: 12, backgroundColor: '#fffbeb', borderRadius: 6, border: '1px solid #fde68a' }}>
            <Space direction="vertical" style={{ width: '100%' }} size={6}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <WarningOutlined style={{ color: '#d97706', fontSize: 15 }} />
                <strong style={{ color: '#b45309', fontSize: 13.5 }}>
                  Lý do thay đổi lô thuốc so với đề xuất FEFO (Bắt buộc theo quy định):
                </strong>
              </div>
              <Input.TextArea
                rows={2}
                placeholder="Nhập lý do đổi lô (ví dụ: Bao bì lô cũ móp méo, bệnh nhân yêu cầu cùng lô với lần điều trị trước, kiểm định tồn dư...)"
                value={batchChangeReasons[itemId] || ''}
                onChange={(e) => handleReasonChange(itemId, e.target.value)}
                status={validationErrors[itemId] ? 'error' : ''}
                style={{ borderRadius: 6 }}
              />
              {validationErrors[itemId] && (
                <Text type="danger" style={{ fontSize: 12, fontWeight: 500 }}>
                  * {validationErrors[itemId]}
                </Text>
              )}
            </Space>
          </div>
        )}
      </div>
    )
  }

  const rx = prescriptionData || prescription
  const rxCode = rx?.prescriptionCode || rx?.id || '—'
  const isPartiallyDispensed = rx?.status === 'PARTIALLY_DISPENSED'

  return (
    <Modal
      title={
        <Space align="center">
          <MedicineBoxOutlined style={{ color: '#0284c7', fontSize: 22 }} />
          <span style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>
            Cấp phát thuốc theo nguyên tắc hạn dùng gần trước (FEFO)
          </span>
        </Space>
      }
      open={open}
      onCancel={() => {
        if (!submitting && !submittingQuick) onClose()
      }}
      width={1040}
      destroyOnClose
      footer={[
        <Button
          key="back"
          icon={<RollbackOutlined />}
          onClick={onClose}
          disabled={submitting || submittingQuick}
          style={{ height: 42, minWidth: 100 }}
        >
          Hủy bỏ
        </Button>,
        <Tooltip
          key="quick"
          title={
            isPartiallyDispensed
              ? 'Đơn thuốc đã cấp một phần trước đó chỉ có thể hoàn tất qua luồng Cấp phát một phần.'
              : undefined
          }
        >
          <Popconfirm
            title={
              <Text strong style={{ fontSize: 16, color: '#1e3a8a' }}>
                Xác nhận cấp phát nhanh theo FEFO
              </Text>
            }
            description={
              <div style={{ marginTop: 6, maxWidth: 380, fontSize: 13.5, lineHeight: 1.5 }}>
                Hệ thống sẽ tự động xuất kho toàn bộ các thuốc theo đúng thứ tự ưu tiên hạn dùng gần trước mà không cần can thiệp từng lô thủ công.
              </div>
            }
            icon={<ThunderboltOutlined style={{ color: '#16a34a', fontSize: 20 }} />}
            okText="Xác nhận cấp nhanh"
            cancelText="Xem lại"
            okButtonProps={{
              style: { backgroundColor: '#16a34a', borderColor: '#16a34a', height: 38, minWidth: 130, fontWeight: 600 },
            }}
            cancelButtonProps={{
              style: { height: 38, minWidth: 90 },
            }}
            onConfirm={handleQuickFefoDispense}
            disabled={submitting || submittingQuick || loadingDetails || items.length === 0 || isPartiallyDispensed}
          >
            <Button
              icon={<ThunderboltOutlined />}
              loading={submittingQuick}
              disabled={submitting || submittingQuick || loadingDetails || items.length === 0 || isPartiallyDispensed}
              style={{
                height: 42,
                minWidth: 190,
                color: isPartiallyDispensed ? undefined : '#15803d',
                borderColor: isPartiallyDispensed ? undefined : '#86efac',
                backgroundColor: isPartiallyDispensed ? undefined : '#f0fdf4',
                fontWeight: 600,
              }}
            >
              Cấp phát nhanh theo FEFO
            </Button>
          </Popconfirm>
        </Tooltip>,
        <Button
          key="submit"
          type="primary"
          icon={<CheckCircleOutlined />}
          loading={submitting}
          disabled={submitting || submittingQuick || loadingDetails || items.length === 0}
          onClick={handleConfirmDispense}
          style={{
            height: 42,
            minWidth: 180,
            backgroundColor: isPartiallyDispensed ? '#d97706' : '#0284c7',
            borderColor: isPartiallyDispensed ? '#d97706' : '#0284c7',
            fontWeight: 600,
            fontSize: 14.5,
          }}
        >
          {isPartiallyDispensed ? 'Xác nhận cấp tiếp một phần' : 'Xác nhận cấp phát'}
        </Button>,
      ]}
    >
      <div style={{ marginTop: 12 }}>
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Mã đơn thuốc">
            <Tag color="blue" style={{ fontWeight: 700, margin: 0, fontSize: 13 }}>
              <BarcodeOutlined style={{ marginRight: 4 }} />
              {rxCode}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Bệnh nhân">
            <strong>{rx?.patientName || '—'}</strong> ({rx?.patientCode || '—'})
          </Descriptions.Item>
          <Descriptions.Item label="Bác sĩ kê">
            {rx?.doctorName || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái hiện tại">
            {rx?.status === 'PARTIALLY_DISPENSED' ? (
              <Tag color="gold" style={{ fontWeight: 600 }}>Cấp phát một phần</Tag>
            ) : rx?.status === 'PENDING_DISPENSE' ? (
              <Tag color="orange">Chờ cấp phát</Tag>
            ) : (
              <Tag>{rx?.status || '—'}</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Thời gian kê" span={2}>
            {rx?.prescribedAt && dayjs(rx.prescribedAt).isValid()
              ? dayjs(rx.prescribedAt).format('HH:mm DD/MM/YYYY')
              : '—'}
          </Descriptions.Item>
        </Descriptions>

        <Alert
          type="info"
          showIcon
          icon={<FieldTimeOutlined style={{ fontSize: 18, color: '#0284c7' }} />}
          message="Nguyên tắc xuất kho theo hạn dùng gần trước (FEFO):"
          description={
            <div style={{ fontSize: 13.5, lineHeight: 1.6 }}>
              <div>• Hệ thống tự động sắp xếp và chọn sẵn lô thuốc có hạn dùng gần nhất để giảm thiểu hao hụt thuốc do hết hạn.</div>
              <div>• Bấm vào biểu tượng dấu cộng <strong style={{ color: '#0284c7' }}>[+]</strong> ở đầu mỗi dòng để xem chi tiết danh sách các lô phân bổ hoặc chọn đổi lô khác khi có lý do chính đáng.</div>
            </div>
          }
          style={{ marginBottom: 16, backgroundColor: '#f0f9ff', borderColor: '#bae6fd' }}
        />

        {errorMessage && (
          <Alert
            type="error"
            showIcon
            message="Thông báo từ hệ thống"
            description={errorMessage}
            style={{ marginBottom: 16 }}
          />
        )}

        {serverShortages.length > 0 && (
          <Alert
            type="error"
            showIcon
            message="Máy chủ phát hiện kho không đủ số lượng bạn yêu cầu cấp phát:"
            description={
              <Table
                rowKey={(r) => r.prescriptionItemId || r.medicineId}
                columns={shortageDetailsColumns}
                dataSource={serverShortages}
                pagination={false}
                size="small"
                style={{ marginTop: 8 }}
              />
            }
            style={{ marginBottom: 16 }}
          />
        )}

        <Spin spinning={loadingDetails || loadingSuggestions} tip="Đang tải dữ liệu phân bổ lô FEFO...">
          <Table
            rowKey={(r) => r.id || r.prescriptionItemId}
            columns={columns}
            dataSource={items}
            pagination={false}
            size="middle"
            expandable={{
              expandedRowRender,
              expandedRowKeys,
              onExpandedRowsChange: setExpandedRowKeys,
              rowExpandable: (record) => {
                const itemId = record.id || record.prescriptionItemId
                const s = suggestionsMap[itemId]
                return Boolean(s?.batches && s.batches.length > 0)
              },
            }}
            rowClassName={(record) => {
              const itemId = record.id || record.prescriptionItemId
              const remaining = getRemainingQuantity(record)
              const currentVal = quantities[itemId] !== undefined ? quantities[itemId] : remaining
              if (validationErrors[itemId]) return 'ant-table-row-selected'
              return remaining > 0 && currentVal < remaining ? 'partial-dispense-shortage-row' : ''
            }}
            locale={{ emptyText: <Empty description="Đơn thuốc không có danh mục thuốc" /> }}
          />
        </Spin>
      </div>
    </Modal>
  )
}

export default PartialDispenseModal
