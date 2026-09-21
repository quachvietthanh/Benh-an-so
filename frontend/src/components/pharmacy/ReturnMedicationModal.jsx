import React, { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Input,
  InputNumber,
  Modal,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  ExclamationCircleOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  RollbackOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import medicationReturnApi from '../../api/medicationReturnApi.js'
import prescriptionDispenseApi from '../../api/prescriptionDispenseApi.js'
import {
  MAX_REASON_LENGTH,
  calculateProjectedStatus,
  isDispensedToday,
  mapReturnErrorMessage,
  validateReturnForm,
} from '../../utils/medicationReturnHelpers.js'

const { Text, Title, Paragraph } = Typography
const { TextArea } = Input

const REASON_PRESETS = [
  'Bệnh nhân đổi phác đồ điều trị',
  'Cấp phát nhầm số lượng',
  'Bệnh nhân không sử dụng hết',
  'Thuốc bị lỗi / hỏng bao bì',
]

/**
 * ReturnMedicationModal
 * Modal cho phép Dược sĩ nhận lại thuốc đã cấp phát và hủy/chuyển trạng thái đơn thuốc.
 * Tích hợp chuẩn contract Backend: POST /prescriptions/{id}/return
 */
function ReturnMedicationModal({ open, onClose, prescription, onSuccess }) {
  const [loadingHistory, setLoadingHistory] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [historyItems, setHistoryItems] = useState([])
  const [returnQuantities, setReturnQuantities] = useState({})
  const [reason, setReason] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const rxId = prescription?.id
  const rxCode = prescription?.prescriptionCode || prescription?.id || '—'

  // Load danh sách các dòng đã cấp phát từ endpoint GET /prescriptions/{id}/dispense-history
  const fetchDispenseHistory = async () => {
    if (!rxId) return
    setLoadingHistory(true)
    setErrorMessage('')
    try {
      const res = await prescriptionDispenseApi.getHistory(rxId)
      const data = res?.data
      const list = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []

      // Sắp xếp theo ngày cấp phát giảm dần
      list.sort((a, b) => String(b.dispensedAt || '').localeCompare(String(a.dispensedAt || '')))
      setHistoryItems(list)

      // Khởi tạo số lượng nhận lại mặc định = 0 (Dược sĩ CHỦ ĐỘNG nhập, không tự điền full)
      const initialQuantities = {}
      list.forEach((item) => {
        initialQuantities[item.id] = 0
      })
      setReturnQuantities(initialQuantities)
    } catch (err) {
      const msg = mapReturnErrorMessage(err)
      setErrorMessage(msg || 'Không thể tải lịch sử cấp phát của đơn thuốc này.')
    } finally {
      setLoadingHistory(false)
    }
  }

  useEffect(() => {
    if (open && rxId) {
      setReason('')
      setErrorMessage('')
      setReturnQuantities({})
      fetchDispenseHistory()
    } else {
      setHistoryItems([])
      setReturnQuantities({})
      setReason('')
      setErrorMessage('')
    }
  }, [open, rxId])

  // Xử lý thay đổi số lượng nhận lại của từng dòng
  const handleQuantityChange = (dispenseItemId, val, maxLimit) => {
    let nextVal = val
    if (nextVal === null || nextVal === undefined || isNaN(nextVal)) {
      nextVal = 0
    }
    if (nextVal < 0) {
      nextVal = 0
    }
    if (maxLimit != null && nextVal > maxLimit) {
      nextVal = maxLimit
    }

    setReturnQuantities((prev) => ({
      ...prev,
      [dispenseItemId]: nextVal,
    }))
    setErrorMessage('')
  }

  // Chọn nhanh preset lý do
  const handleSelectPresetReason = (preset) => {
    setReason((prev) => {
      if (!prev || !prev.trim()) return preset
      if (prev.includes(preset)) return prev
      return `${prev}; ${preset}`
    })
    setErrorMessage('')
  }

  // Tính tổng số lượng thuốc nhận lại
  const totalReturningCount = useMemo(() => {
    return Object.values(returnQuantities).reduce((sum, q) => sum + (Number(q) || 0), 0)
  }, [returnQuantities])

  // Dự báo trạng thái đơn sau khi nhận lại
  const projectedStatusInfo = useMemo(() => {
    return calculateProjectedStatus(historyItems, returnQuantities)
  }, [historyItems, returnQuantities])

  // Kiểm tra có dòng nào trong ngày không
  const hasAnyTodayItems = useMemo(() => {
    return historyItems.some((it) => isDispensedToday(it?.dispensedAt))
  }, [historyItems])

  // Chuẩn bị danh sách payload để gửi API
  const preparePayloadItems = () => {
    return historyItems
      .filter((item) => {
        const qty = returnQuantities[item.id] || 0
        return qty > 0
      })
      .map((item) => {
        const qty = returnQuantities[item.id] || 0
        const maxReturnable =
          item.remainingReturnableQuantity != null
            ? item.remainingReturnableQuantity
            : item.dispensedQuantity
        return {
          dispenseItemId: item.id,
          quantity: qty,
          maxReturnable,
          medicineName: item.medicineName,
        }
      })
  }

  // Thực thi submit lên máy chủ
  const handleExecuteReturn = async (validItems) => {
    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const response = await medicationReturnApi.returnMedication(
        rxId,
        reason.trim(),
        validItems.map((it) => ({
          dispenseItemId: it.dispenseItemId,
          quantity: it.quantity,
        }))
      )

      const result = response?.data
      const statusText =
        result?.status === 'CANCELLED'
          ? 'Đã hủy'
          : result?.status === 'PARTIALLY_DISPENSED'
          ? 'Cấp phát một phần'
          : result?.status || 'Hoàn tất'

      message.success(`Đã trả lại thuốc thành công! Đơn thuốc hiện ở trạng thái: [${statusText}].`)

      if (onSuccess) {
        onSuccess(result)
      }
      onClose()
    } catch (err) {
      const friendlyMsg = mapReturnErrorMessage(err)
      setErrorMessage(friendlyMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  // Bấm nút "Xác nhận trả thuốc"
  const handleSubmit = () => {
    setErrorMessage('')
    const payloadItems = preparePayloadItems()

    const validation = validateReturnForm(reason, payloadItems)
    if (!validation.isValid) {
      setErrorMessage(validation.error)
      return
    }

    // Modal.confirm cảnh báo trước khi thực hiện
    const isFullCancellation = projectedStatusInfo.isFullCancellation

    Modal.confirm({
      title: isFullCancellation ? (
        <span style={{ color: '#dc2626', fontWeight: 700 }}>
          CẢNH BÁO: HỦY TOÀN BỘ ĐƠN THUỐC
        </span>
      ) : (
        'Xác nhận nhận lại thuốc và hoàn tồn kho'
      ),
      icon: isFullCancellation ? (
        <WarningOutlined style={{ color: '#dc2626' }} />
      ) : (
        <ExclamationCircleOutlined />
      ),
      content: (
        <div>
          {isFullCancellation ? (
            <Paragraph style={{ color: '#b91c1c', marginBottom: 8 }}>
              <strong>Hành động này sẽ HỦY TOÀN BỘ đơn thuốc</strong> vì bạn đã chọn nhận lại hết toàn bộ
              số lượng thuốc đã cấp phát. Đơn thuốc sẽ chuyển sang trạng thái <strong>ĐÃ HỦY</strong> và
              không thể hoàn tác.
            </Paragraph>
          ) : (
            <Paragraph style={{ marginBottom: 8 }}>
              Hệ thống sẽ hoàn số lượng thuốc nhận lại vào đúng <strong>lô thuốc đã xuất ban đầu</strong>{' '}
              và điều chỉnh giảm số lượng đã cấp phát của đơn.
            </Paragraph>
          )}
          <Descriptions size="small" column={1} bordered style={{ marginTop: 8 }}>
            <Descriptions.Item label="Tổng số lượng nhận lại">
              <strong>{totalReturningCount}</strong> đơn vị
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái sau khi trả">
              <Tag color={projectedStatusInfo.color} style={{ fontWeight: 600 }}>
                {projectedStatusInfo.label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Lý do hoàn trả">
              <em>{reason.trim()}</em>
            </Descriptions.Item>
          </Descriptions>
        </div>
      ),
      okText: isFullCancellation ? 'Xác nhận hủy đơn & hoàn kho' : 'Xác nhận hoàn kho',
      okType: isFullCancellation ? 'danger' : 'primary',
      cancelText: 'Xem lại',
      maskClosable: false,
      onOk: () => handleExecuteReturn(validation.validItems),
    })
  }

  // Định nghĩa các cột của bảng dòng cấp phát
  const columns = [
    {
      title: 'STT',
      key: 'stt',
      width: 50,
      align: 'center',
      render: (_, __, idx) => idx + 1,
    },
    {
      title: 'Tên thuốc & Quy cách',
      dataIndex: 'medicineName',
      key: 'medicineName',
      render: (text) => (
        <Space direction="vertical" size={2}>
          <Text strong>{text || '—'}</Text>
        </Space>
      ),
    },
    {
      title: 'Lô đã xuất',
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 130,
      render: (text) =>
        text ? (
          <Tag color="cyan" style={{ fontFamily: 'monospace' }}>
            {text}
          </Tag>
        ) : (
          <Text type="secondary">—</Text>
        ),
    },
    {
      title: 'Thời điểm cấp phát',
      dataIndex: 'dispensedAt',
      key: 'dispensedAt',
      width: 180,
      render: (dispensedAt) => {
        const isToday = isDispensedToday(dispensedAt)
        const formatted =
          dispensedAt && dayjs(dispensedAt).isValid()
            ? dayjs(dispensedAt).format('HH:mm DD/MM/YYYY')
            : '—'

        return (
          <Space direction="vertical" size={2}>
            <span>{formatted}</span>
            {isToday ? (
              <Tag color="green" style={{ fontSize: 11, margin: 0 }}>
                Trong ngày hôm nay
              </Tag>
            ) : (
              <Tooltip title="Chỉ được phép trả lại thuốc cho phiếu cấp phát lập trong ngày hôm nay.">
                <Tag color="default" style={{ fontSize: 11, margin: 0, color: '#6b7280' }}>
                  Không phải hôm nay
                </Tag>
              </Tooltip>
            )}
          </Space>
        )
      },
    },
    {
      title: 'Đã cấp',
      dataIndex: 'dispensedQuantity',
      key: 'dispensedQuantity',
      width: 80,
      align: 'center',
      render: (qty) => <Tag color="blue">{qty ?? 0}</Tag>,
    },
    {
      title: (
        <Tooltip title="Số lượng thuốc đã trả lại trước đó trong các lần hoàn thuốc trước (nếu có).">
          <span>
            Đã trả trước <InfoCircleOutlined style={{ fontSize: 12, color: '#6b7280' }} />
          </span>
        </Tooltip>
      ),
      key: 'returnedQuantity',
      width: 100,
      align: 'center',
      render: (_, record) => {
        // Nếu Backend có trả returnedQuantity thì hiển thị, nếu chưa có thì hiển thị '—'
        if (record.returnedQuantity !== undefined && record.returnedQuantity !== null) {
          return <Tag color="default">{record.returnedQuantity}</Tag>
        }
        return (
          <Tooltip title="Backend hiện tại chưa cung cấp trường returnedQuantity riêng biệt cho từng đợt cấp phát.">
            <Text type="secondary">—</Text>
          </Tooltip>
        )
      },
    },
    {
      title: (
        <Tooltip title="Số lượng tối đa còn có thể nhận lại của dòng cấp phát này.">
          <span>
            Có thể trả <InfoCircleOutlined style={{ fontSize: 12, color: '#6b7280' }} />
          </span>
        </Tooltip>
      ),
      key: 'remainingReturnableQuantity',
      width: 100,
      align: 'center',
      render: (_, record) => {
        const isToday = isDispensedToday(record.dispensedAt)
        if (!isToday) {
          return <Text type="secondary">0</Text>
        }
        const maxReturnable =
          record.remainingReturnableQuantity != null
            ? record.remainingReturnableQuantity
            : record.dispensedQuantity
        return (
          <Tag color={maxReturnable > 0 ? 'purple' : 'default'} style={{ fontWeight: 600 }}>
            {maxReturnable}
          </Tag>
        )
      },
    },
    {
      title: (
        <span style={{ color: '#dc2626', fontWeight: 600 }}>
          Số lượng nhận lại <span style={{ color: '#dc2626' }}>*</span>
        </span>
      ),
      key: 'actionQuantity',
      width: 150,
      align: 'center',
      render: (_, record) => {
        const isToday = isDispensedToday(record.dispensedAt)
        const maxReturnable =
          record.remainingReturnableQuantity != null
            ? record.remainingReturnableQuantity
            : record.dispensedQuantity

        if (!isToday) {
          return (
            <Tooltip title="Phiếu này không lập trong ngày hôm nay, không thể trả.">
              <InputNumber disabled size="small" value={0} style={{ width: '100%' }} />
            </Tooltip>
          )
        }

        const currentVal = returnQuantities[record.id] || 0

        return (
          <InputNumber
            size="middle"
            min={0}
            max={maxReturnable}
            value={currentVal}
            disabled={isSubmitting || maxReturnable <= 0}
            onChange={(val) => handleQuantityChange(record.id, val, maxReturnable)}
            style={{ width: '100%', borderColor: currentVal > 0 ? '#dc2626' : undefined }}
            placeholder="0"
          />
        )
      },
    },
  ]

  return (
    <Modal
      open={open}
      onCancel={() => {
        if (!isSubmitting) onClose()
      }}
      maskClosable={false}
      closable={!isSubmitting}
      width={960}
      title={
        <Space size={8} align="center">
          <RollbackOutlined style={{ color: '#dc2626', fontSize: 18 }} />
          <span style={{ fontSize: 16, fontWeight: 700 }}>
            Trả lại thuốc và hủy phiếu cấp phát
          </span>
          <Tag color="red" style={{ fontSize: 12 }}>
            QTN-32 / QTN-06 / QTN-14
          </Tag>
        </Space>
      }
      footer={[
        <Button key="cancel" disabled={isSubmitting} onClick={onClose}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          icon={<RollbackOutlined />}
          loading={isSubmitting}
          disabled={isSubmitting || totalReturningCount <= 0 || !reason.trim()}
          onClick={handleSubmit}
        >
          {projectedStatusInfo.isFullCancellation
            ? 'Xác nhận hủy đơn & hoàn kho'
            : 'Xác nhận trả thuốc'}
        </Button>,
      ]}
    >
      <div style={{ marginTop: 8 }}>
        {/* Thông tin đơn thuốc */}
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 14 }}>
          <Descriptions.Item label="Mã đơn thuốc">
            <Tag color="blue" style={{ fontWeight: 700, margin: 0 }}>
              {rxCode}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Bệnh nhân">
            <strong>{prescription?.patientName || '—'}</strong> ({prescription?.patientCode || '—'})
          </Descriptions.Item>
          <Descriptions.Item label="Bác sĩ kê">
            {prescription?.doctorName || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái hiện tại">
            {prescription?.status === 'PARTIALLY_DISPENSED' ? (
              <Tag color="gold" style={{ fontWeight: 600 }}>Cấp phát một phần</Tag>
            ) : prescription?.status === 'DISPENSED' ? (
              <Tag color="green" style={{ fontWeight: 600 }}>Đã cấp phát</Tag>
            ) : (
              <Tag>{prescription?.status || '—'}</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Thời gian kê" span={2}>
            {prescription?.prescribedAt && dayjs(prescription.prescribedAt).isValid()
              ? dayjs(prescription.prescribedAt).format('HH:mm DD/MM/YYYY')
              : '—'}
          </Descriptions.Item>
        </Descriptions>

        {/* Hướng dẫn nghiệp vụ & điều kiện */}
        <Alert
          type="warning"
          showIcon
          icon={<InfoCircleOutlined />}
          message="Quy tắc nghiệp vụ trả lại thuốc (Dược sĩ):"
          description={
            <ul style={{ margin: '4px 0 0 0', paddingLeft: 18, fontSize: 13 }}>
              <li>
                <strong>Điều kiện tiên quyết:</strong> Chỉ có thể hoàn trả cho các phiếu cấp phát được
                lập <strong>trong ngày hôm nay</strong>.
              </li>
              <li>
                <strong>Viện phí:</strong> Lượt khám <em>chưa thu phí</em> hoặc <em>đã được hoàn tiền</em> mới
                được phép trả thuốc. Nếu đã thu phí mà chưa hoàn tiền, máy chủ sẽ từ chối.
              </li>
              <li>
                <strong>Tồn kho:</strong> Số lượng nhận lại sẽ được cộng trả về đúng{' '}
                <strong>lô thuốc đã xuất ban đầu</strong>.
              </li>
              <li>
                <strong>Chủ động nhập:</strong> Mặc định số lượng nhận lại là 0. Dược sĩ chủ động kiểm
                đếm thực tế và nhập đúng số lượng cần nhận lại.
              </li>
            </ul>
          }
          style={{ marginBottom: 14 }}
        />

        {/* Cảnh báo nếu không có dòng nào lập hôm nay */}
        {!loadingHistory && historyItems.length > 0 && !hasAnyTodayItems && (
          <Alert
            type="error"
            showIcon
            message="Không đủ điều kiện trả thuốc"
            description="Tất cả các phiếu cấp phát của đơn này đều được lập trước ngày hôm nay. Quy định nghiệp vụ không cho phép trả lại thuốc khác ngày."
            style={{ marginBottom: 14 }}
          />
        )}

        {/* Thông báo lỗi nếu có */}
        {errorMessage && (
          <Alert
            type="error"
            showIcon
            message="Không thể thực hiện trả thuốc"
            description={errorMessage}
            style={{ marginBottom: 14 }}
          />
        )}

        {/* Bảng chọn thuốc nhận lại */}
        <Card
          size="small"
          title={
            <Space>
              <MedicineBoxOutlined />
              <span>Danh sách các lần cấp phát thuốc của đơn</span>
            </Space>
          }
          extra={
            <Button
              size="small"
              icon={<ReloadOutlined />}
              loading={loadingHistory}
              onClick={fetchDispenseHistory}
            >
              Tải lại
            </Button>
          }
          style={{ marginBottom: 14 }}
        >
          <Table
            rowKey="id"
            columns={columns}
            dataSource={historyItems}
            loading={loadingHistory}
            pagination={false}
            size="small"
            locale={{
              emptyText: (
                <Empty
                  description={
                    loadingHistory
                      ? 'Đang tải lịch sử cấp phát...'
                      : 'Đơn thuốc chưa có lịch sử cấp phát nào để trả lại.'
                  }
                />
              ),
            }}
          />
        </Card>

        {/* Ô nhập lý do trả thuốc */}
        <Card size="small" style={{ marginBottom: 14 }}>
          <Space direction="vertical" style={{ width: '100%' }} size={8}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 6 }}>
              <Text strong>
                Lý do trả thuốc <span style={{ color: '#dc2626' }}>*</span> (Bắt buộc, tối đa {MAX_REASON_LENGTH} ký tự):
              </Text>
              <Space wrap size={4}>
                <Text type="secondary" style={{ fontSize: 12 }}>Chọn nhanh:</Text>
                {REASON_PRESETS.map((preset) => (
                  <Button
                    key={preset}
                    size="small"
                    type="dashed"
                    style={{ fontSize: 11 }}
                    onClick={() => handleSelectPresetReason(preset)}
                  >
                    {preset}
                  </Button>
                ))}
              </Space>
            </div>
            <TextArea
              rows={3}
              placeholder="Nhập lý do chi tiết nhận lại thuốc (ví dụ: Bệnh nhân đổi thuốc do dị ứng, nhập nhầm số lượng cấp phát...)"
              value={reason}
              maxLength={MAX_REASON_LENGTH}
              showCount
              disabled={isSubmitting}
              onChange={(e) => {
                setReason(e.target.value)
                setErrorMessage('')
              }}
            />
          </Space>
        </Card>

        {/* Khung tổng hợp & Dự báo trạng thái */}
        <Card
          size="small"
          style={{
            backgroundColor: projectedStatusInfo.isFullCancellation ? '#fef2f2' : '#f8fafc',
            borderColor: projectedStatusInfo.isFullCancellation ? '#fca5a5' : '#e2e8f0',
          }}
        >
          <Row gutter={[16, 8]} align="middle">
            <Col xs={24} sm={12}>
              <Space direction="vertical" size={2}>
                <Text type="secondary">Tổng số lượng nhận lại:</Text>
                <Title level={4} style={{ margin: 0, color: totalReturningCount > 0 ? '#dc2626' : '#64748b' }}>
                  {totalReturningCount} <span style={{ fontSize: 14, fontWeight: 400 }}>đơn vị</span>
                </Title>
              </Space>
            </Col>
            <Col xs={24} sm={12}>
              <Space direction="vertical" size={2}>
                <Text type="secondary">Dự kiến trạng thái đơn sau khi trả:</Text>
                <Space>
                  <Tag
                    color={projectedStatusInfo.color}
                    style={{ fontSize: 13, fontWeight: 700, padding: '2px 8px' }}
                  >
                    {projectedStatusInfo.label}
                  </Tag>
                </Space>
                <Text style={{ fontSize: 12, color: projectedStatusInfo.isFullCancellation ? '#b91c1c' : '#64748b' }}>
                  {projectedStatusInfo.description}
                </Text>
              </Space>
            </Col>
          </Row>
        </Card>
      </div>
    </Modal>
  )
}

export default ReturnMedicationModal
