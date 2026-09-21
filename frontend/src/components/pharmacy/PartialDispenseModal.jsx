import React, { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Empty,
  InputNumber,
  Modal,
  Space,
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
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  RollbackOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import prescriptionDispenseApi from '../../api/prescriptionDispenseApi.js'
import pharmacyApi from '../../api/pharmacyApi.js'
import {
  buildPartialDispensePayload,
  calculateItemShortage,
  getRemainingQuantity,
  hasShortageAfterDispense,
  mapDispenseError,
} from '../../utils/partialDispensingHelpers.js'

const { Text, Title } = Typography

function PartialDispenseModal({ open, onClose, prescription, onSuccess }) {
  const [loadingDetails, setLoadingDetails] = useState(false)
  const [prescriptionData, setPrescriptionData] = useState(null)
  const [quantities, setQuantities] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [serverShortages, setServerShortages] = useState([])
  const [errorMessage, setErrorMessage] = useState('')
  const [stocks, setStocks] = useState([])

  useEffect(() => {
    if (!open || !prescription) {
      setPrescriptionData(null)
      setQuantities({})
      setServerShortages([])
      setErrorMessage('')
      setStocks([])
      return
    }

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
    } else if (prescription.id) {
      setLoadingDetails(true)
      pharmacyApi
        .getById(prescription.id)
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
  }

  const items = useMemo(() => {
    return prescriptionData?.items || []
  }, [prescriptionData])

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

      message.success('Cấp phát một phần đơn thuốc thành công!')
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
    const { payloadItems, hasAnyItemToDispense } = buildPartialDispensePayload(quantities, items)

    if (!hasAnyItemToDispense) {
      message.warning('Vui lòng nhập số lượng cấp phát lớn hơn 0 cho ít nhất một loại thuốc.')
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
          style: { backgroundColor: '#d97706', borderColor: '#d97706' },
        },
        onOk: () => executeSubmit(payloadItems),
      })
    } else {
      executeSubmit(payloadItems)
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
      render: (_, item) => (
        <div>
          <Text strong style={{ color: '#1e40af', fontSize: 14 }}>
            {item.medicineName || `Mã ${item.medicineId}`}
          </Text>
          <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
            {[item.activeIngredient, item.strength].filter(Boolean).join(' · ') || item.dosage || '—'}
          </div>
        </div>
      ),
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
      width: 90,
      align: 'center',
      render: (_, item) => {
        const dispensed = item.dispensedQuantity || 0
        return <Tag color={dispensed > 0 ? 'blue' : 'default'}>{dispensed} {item.unit || ''}</Tag>
      },
    },
    {
      title: 'Số lượng tồn trong kho',
      key: 'stockQuantity',
      width: 125,
      align: 'center',
      render: (_, item) => {
        const medId = item.medicineId || item.medicine?.id || item.id
        const stock = stockByMedicineId.get(String(medId))
        const stockQty =
          stock?.eligibleStockQuantity ??
          stock?.stockQuantity ??
          item.stockQuantity ??
          item.availableStock ??
          item.availableQuantity ??
          0
        return (
          <Tag color={stockQty > 0 ? 'green' : 'orange'} style={{ fontWeight: 600 }}>
            {stockQty} {item.unit || ''}
          </Tag>
        )
      },
    },
    {
      title: 'Số lượng thực cấp',
      key: 'actualDispense',
      width: 190,
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

  const rx = prescriptionData || prescription
  const rxCode = rx?.prescriptionCode || rx?.id || '—'

  return (
    <Modal
      title={
        <Space align="center">
          <MedicineBoxOutlined style={{ color: '#d97706', fontSize: 20 }} />
          <span style={{ fontSize: 17, fontWeight: 700 }}>Cấp phát một phần khi tồn kho không đủ</span>
        </Space>
      }
      open={open}
      onCancel={() => {
        if (!submitting) onClose()
      }}
      width={920}
      destroyOnClose
      footer={[
        <Button key="back" icon={<RollbackOutlined />} onClick={onClose} disabled={submitting}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<CheckCircleOutlined />}
          loading={submitting}
          disabled={submitting || loadingDetails || items.length === 0}
          onClick={handleConfirmDispense}
          style={{
            backgroundColor: '#d97706',
            borderColor: '#d97706',
            fontWeight: 600,
          }}
        >
          Xác nhận cấp một phần
        </Button>,
      ]}
    >
      <div style={{ marginTop: 12 }}>
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Mã đơn thuốc">
            <Tag color="blue" style={{ fontWeight: 700, margin: 0 }}>
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
          message="Hướng dẫn cấp phát một phần:"
          description="Hệ thống tự động điền số lượng còn cần cấp. Hãy điều chỉnh giảm số lượng thực cấp cho những loại thuốc bị thiếu hàng trong kho. Những thuốc để số lượng = 0 sẽ được tự động bỏ qua đợt này để cấp sau."
          style={{ marginBottom: 16 }}
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

        <Table
          rowKey={(r) => r.id || r.prescriptionItemId}
          columns={columns}
          dataSource={items}
          loading={loadingDetails}
          pagination={false}
          size="middle"
          rowClassName={(record) => {
            const itemId = record.id || record.prescriptionItemId
            const remaining = getRemainingQuantity(record)
            const currentVal = quantities[itemId] !== undefined ? quantities[itemId] : remaining
            return remaining > 0 && currentVal < remaining ? 'partial-dispense-shortage-row' : ''
          }}
          locale={{ emptyText: <Empty description="Đơn thuốc không có danh mục thuốc" /> }}
        />
      </div>
    </Modal>
  )
}

export default PartialDispenseModal
