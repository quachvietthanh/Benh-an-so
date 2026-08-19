import React, { useCallback, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import {
  Card,
  Row,
  Col,
  Statistic,
  Button,
  Typography,
  Space,
  message,
} from 'antd'
import {
  FileDoneOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  ExperimentOutlined,
} from '@ant-design/icons'

import ResultSearch from '../components/results/ResultSearch'
import ResultFilter from '../components/results/ResultFilter'
import ResultTable from '../components/results/ResultTable'
import ResultModal from '../components/results/ResultModal'

import clinicalResultApi from '../api/clinicalResultApi'
import clinicalServiceApi from '../api/clinicalServiceApi'
import patientApi from '../api/patientApi'
import queueApi from '../api/queueApi'
import { useAuthContext } from '../context/AuthContext'

const { Title, Text } = Typography

const PAGE_SIZE = 100
const TERMINAL_ORDER_ITEM_STATUSES = new Set(['COMPLETED', 'CANCELLED'])

const responseItems = (response) => {
  const data = response?.data
  if (Array.isArray(data)) return data
  return Array.isArray(data?.content) ? data.content : []
}

const apiErrorMessage = (error) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  error?.message ||
  'Lỗi không xác định từ máy chủ.'

const calculateAge = (dateOfBirth) => {
  if (!dateOfBirth || !dayjs(dateOfBirth).isValid()) return null
  return String(dayjs().diff(dayjs(dateOfBirth), 'year'))
}

const uiStatusForResult = (result, orderItem) => {
  if (orderItem?.status === 'CANCELLED') return 'CANCELLED'
  if (!result) return 'PENDING'
  if (result.status === 'FINAL') return 'CONFIRMED'
  if (['DRAFT', 'CORRECTED'].includes(result.status)) return 'RESULTED'
  return 'IN_PROGRESS'
}

const resultValues = (result) => {
  if (!result) return ''
  if (result.resultType === 'NUMBER' && result.numericValue !== null && result.numericValue !== undefined) {
    return String(result.numericValue)
  }
  return result.textValue || ''
}

const normalizeAttachments = (attachments = []) =>
  attachments.map((attachment) => ({
    ...attachment,
    name: attachment.fileName || attachment.name,
    size: attachment.fileSize ?? attachment.size,
    type: attachment.contentType || attachment.type,
  }))

const mergeResultIntoOrder = (order, result) => ({
  ...order,
  id: result.id,
  clinicalResultId: result.id,
  resultType: result.resultType || order.resultType,
  numericValue: result.numericValue ?? null,
  textValue: result.textValue ?? null,
  resultValues: resultValues(result),
  conclusion: result.conclusion || '',
  abnormalFlag: result.abnormalFlag || 'UNKNOWN',
  backendStatus: result.status,
  status: uiStatusForResult(result, order.items?.[0]),
  attachments: normalizeAttachments(result.attachments),
})

const normalizeQueueItem = (item = {}) => ({
  ...item,
  id: item.id || item.queueItemId,
  status: item.status || item.queueItemStatus,
  roomNumber: item.roomNumber || item.roomName,
})

const buildResultRequest = (order) => {
  const rawValue = String(order.resultValues || '').trim()
  const resultType = order.resultType

  if (!resultType) {
    throw new Error(`Không xác định được kiểu kết quả cho dịch vụ ${order.items?.[0]?.serviceCode || ''}.`)
  }

  let numericValue = order.numericValue ?? null
  let textValue = rawValue || null

  if (resultType === 'NUMBER') {
    const normalizedNumber = rawValue.replace(',', '.')
    if (!/^-?\d+(\.\d+)?$/.test(normalizedNumber)) {
      throw new Error('Kết quả của dịch vụ này phải là một giá trị số hợp lệ.')
    }
    numericValue = Number(normalizedNumber)
    textValue = null
  }

  return {
    numericValue,
    textValue,
    abnormalFlag: order.abnormalFlag || 'UNKNOWN',
    conclusion: String(order.conclusion || '').trim() || null,
  }
}

const requireClinicalResult = (response) => {
  const result = response?.data
  if (!result?.id) {
    throw new Error('Máy chủ không trả về clinical result hợp lệ.')
  }
  return result
}

const requireClinicalAttachment = (response) => {
  const attachment = response?.data
  if (!attachment?.id) {
    throw new Error('Máy chủ không trả về tệp đính kèm hợp lệ.')
  }
  return attachment
}

export function ResultPage() {
  const { user } = useAuthContext()
  const isAdmin = (user?.roles || [])
    .map((role) => String(role || '').toLowerCase().replace(/^role_/, ''))
    .includes('admin')
  const [loading, setLoading] = useState(false)
  const [orders, setOrders] = useState([])

  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [categoryFilter, setCategoryFilter] = useState('ALL')

  const [modalVisible, setModalVisible] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const queueResponse = await (isAdmin ? queueApi.getQueues : queueApi.getMyQueue)({
        date: dayjs().format('YYYY-MM-DD'),
      })
      const waitingQueues = responseItems(queueResponse)
        .map(normalizeQueueItem)
        .filter((item) => item.status === 'WAITING_FOR_RESULT')

      if (waitingQueues.some((item) => !item.id || !item.visitId || !item.patientId)) {
        throw new Error('Queue WAITING_FOR_RESULT thiếu queueItemId, visitId hoặc patientId.')
      }

      const queueByVisit = new Map()
      waitingQueues.forEach((item) => {
        if (!queueByVisit.has(String(item.visitId))) {
          queueByVisit.set(String(item.visitId), item)
        }
      })
      const visitQueues = [...queueByVisit.values()]

      if (!visitQueues.length) {
        setOrders([])
        return
      }

      const patientIds = [...new Set(visitQueues.map((item) => String(item.patientId)))]
      const [catalogResponse, patientEntries, visitEntries] = await Promise.all([
        clinicalServiceApi.getCatalog({ page: 0, size: PAGE_SIZE }),
        Promise.all(
          patientIds.map(async (patientId) => {
            const response = await patientApi.getById(patientId)
            return [patientId, response?.data]
          })
        ),
        Promise.all(
          visitQueues.map(async (queueItem) => {
            const [clinicalOrdersResponse, clinicalResultsResponse] = await Promise.all([
              clinicalResultApi.getOrdersByVisit(queueItem.visitId, { page: 0, size: PAGE_SIZE }),
              clinicalResultApi.getByVisit(queueItem.visitId, { page: 0, size: PAGE_SIZE }),
            ])
            return {
              queueItem,
              clinicalOrders: responseItems(clinicalOrdersResponse),
              clinicalResults: responseItems(clinicalResultsResponse),
            }
          })
        ),
      ])

      const servicesByCode = new Map(
        responseItems(catalogResponse).map((service) => [String(service.serviceCode), service])
      )
      const patientsById = new Map(patientEntries)

      const nextOrders = visitEntries.flatMap(({ queueItem, clinicalOrders, clinicalResults }) => {
        const patient = patientsById.get(String(queueItem.patientId))
        if (!patient?.id) {
          throw new Error(`Không tải được bệnh nhân ${queueItem.patientId} của visit ${queueItem.visitId}.`)
        }

        const resultsByOrderItem = new Map(
          clinicalResults.map((result) => [String(result.clinicalOrderItemId), result])
        )

        return clinicalOrders.flatMap((clinicalOrder) =>
          (clinicalOrder.items || []).map((item) => {
            const service = servicesByCode.get(String(item.serviceCode))
            const result = resultsByOrderItem.get(String(item.id))
            const resolvedResultType = result?.resultType || service?.resultDataType
            if (!resolvedResultType) {
              throw new Error(`Không tìm thấy kiểu kết quả của dịch vụ ${item.serviceCode}.`)
            }

            const normalizedItem = {
              ...item,
              category: service?.serviceType || 'OTHER',
              resultDataType: resolvedResultType,
              unit: result?.unit || service?.unit || null,
              referenceRange: result?.referenceRange || service?.referenceRange || null,
            }
            const order = {
              id: result?.id || item.id,
              clinicalResultId: result?.id || null,
              clinicalOrderId: clinicalOrder.id,
              clinicalOrderItemId: item.id,
              visitId: queueItem.visitId,
              queueItemId: queueItem.id,
              queueStatus: queueItem.status,
              patientId: patient.id,
              patientCode: patient.patientCode,
              patientName: patient.fullName || queueItem.patientName,
              gender: patient.gender,
              dateOfBirth: patient.dateOfBirth
                ? dayjs(patient.dateOfBirth).format('DD/MM/YYYY')
                : null,
              age: calculateAge(patient.dateOfBirth),
              doctorId: queueItem.doctorId,
              doctorName: queueItem.doctorName,
              department: queueItem.roomNumber
                ? `Phòng ${queueItem.roomNumber}`
                : 'Cận lâm sàng',
              orderCode: clinicalOrder.orderCode,
              diagnosis: clinicalOrder.clinicalReason,
              createdAt: clinicalOrder.orderedAt || queueItem.checkedInAt,
              items: [normalizedItem],
              resultType: resolvedResultType,
              numericValue: result?.numericValue ?? null,
              textValue: result?.textValue ?? null,
              resultValues: resultValues(result),
              conclusion: result?.conclusion || '',
              abnormalFlag: result?.abnormalFlag || 'UNKNOWN',
              backendStatus: result?.status || null,
              status: uiStatusForResult(result, item),
              attachments: normalizeAttachments(result?.attachments),
            }

            return order
          })
        )
      })

      setOrders(nextOrders)
    } catch (err) {
      console.error('Error loading clinical results from API:', err)
      setOrders([])
      message.error(`Không thể tải kết quả cận lâm sàng: ${apiErrorMessage(err)}`)
    } finally {
      setLoading(false)
    }
  }, [isAdmin])

  useEffect(() => {
    loadData()
  }, [loadData])

  const filteredOrders = useMemo(() => {
    const kw = searchText.trim().toLowerCase()
    return orders.filter((order) => {
      if (order.status === 'CANCELLED') return false

      const matchesKw =
        !kw ||
        [
          order.orderCode,
          order.patientName,
          order.patientCode,
          order.diagnosis,
          order.doctorName,
          ...((order.items || []).flatMap((item) => [item.serviceCode, item.serviceName])),
        ].some((val) => String(val || '').toLowerCase().includes(kw))

      const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter

      const matchesCategory =
        categoryFilter === 'ALL' ||
        order.items?.some((item) => {
          const category = String(item.category || '').toUpperCase()
          if (categoryFilter === 'LABORATORY') return category === 'LAB_TEST'
          if (categoryFilter === 'IMAGING') return category === 'IMAGING'
          if (categoryFilter === 'FUNCTIONAL') return category === 'OTHER'
          return true
        })

      return matchesKw && matchesStatus && matchesCategory
    })
  }, [orders, searchText, statusFilter, categoryFilter])

  const stats = useMemo(() => {
    const total = orders.filter((o) => o.status !== 'CANCELLED').length
    const pending = orders.filter((o) => o.status === 'PENDING').length
    const inProgress = orders.filter((o) => o.status === 'IN_PROGRESS').length
    const resulted = orders.filter((o) => o.status === 'RESULTED').length
    const confirmed = orders.filter((o) => o.status === 'CONFIRMED').length
    return { total, pending, inProgress, resulted, confirmed }
  }, [orders])

  const handleResetFilter = () => {
    setSearchText('')
    setStatusFilter('ALL')
    setCategoryFilter('ALL')
  }

  const handleOpenModal = (order) => {
    setSelectedOrder(order)
    setModalVisible(true)
  }

  const handleSaveResultSuccess = async (updatedOrder) => {
    if (!['RESULTED', 'CONFIRMED'].includes(updatedOrder.status)) {
      throw new Error(`Trạng thái kết quả không được hỗ trợ: ${updatedOrder.status}.`)
    }
    if (!updatedOrder.visitId || !updatedOrder.queueItemId) {
      throw new Error('Kết quả thiếu visitId hoặc queueItemId của lượt khám.')
    }

    const request = buildResultRequest(updatedOrder)
    const newAttachments = (updatedOrder.attachments || []).filter((attachment) => attachment?.file)
    const shouldFinalize = updatedOrder.status === 'CONFIRMED'
    let persistedResult

    const reflectServerResult = (result) => {
      const serverOrder = mergeResultIntoOrder(updatedOrder, result)
      setOrders((currentOrders) =>
        currentOrders.map((order) =>
          String(order.clinicalOrderItemId) === String(updatedOrder.clinicalOrderItemId)
            ? serverOrder
            : order
        )
      )
      setSelectedOrder(serverOrder)
      return serverOrder
    }

    if (updatedOrder.clinicalResultId) {
      const currentResult = requireClinicalResult(
        await clinicalResultApi.getById(updatedOrder.clinicalResultId)
      )

      if (currentResult.status === 'FINAL') {
        if (!shouldFinalize) {
          throw new Error('Kết quả đã được xác nhận và không thể chỉnh sửa.')
        }
        if (newAttachments.length) {
          throw new Error('Không thể tải thêm tệp vào kết quả đã được xác nhận.')
        }
        persistedResult = currentResult
      } else {
        persistedResult = requireClinicalResult(
          await clinicalResultApi.update(currentResult.id, {
            ...request,
            changeReason:
              String(updatedOrder.notes || '').trim() ||
              'Cập nhật kết quả từ màn hình nhập kết quả cận lâm sàng.',
          })
        )
      }
    } else {
      if (!updatedOrder.clinicalOrderItemId) {
        throw new Error('Kết quả thiếu clinicalOrderItemId để tạo mới.')
      }
      persistedResult = requireClinicalResult(
        await clinicalResultApi.enter(updatedOrder.clinicalOrderItemId, request)
      )
    }

    reflectServerResult(persistedResult)

    if (persistedResult.status !== 'FINAL' && newAttachments.length) {
      const uploadedAttachments = await Promise.all(
        newAttachments.map((attachment) =>
          clinicalResultApi
            .uploadAttachment(persistedResult.id, attachment.file)
            .then(requireClinicalAttachment)
        )
      )
      persistedResult = {
        ...persistedResult,
        attachments: [...(persistedResult.attachments || []), ...uploadedAttachments],
      }
      reflectServerResult(persistedResult)
    }

    if (shouldFinalize && persistedResult.status !== 'FINAL') {
      persistedResult = requireClinicalResult(
        await clinicalResultApi.finalize(persistedResult.id)
      )
      reflectServerResult(persistedResult)
    }

    if (shouldFinalize) {
      const clinicalOrdersResponse = await clinicalResultApi.getOrdersByVisit(
        updatedOrder.visitId,
        { page: 0, size: PAGE_SIZE }
      )
      const visitOrderItems = responseItems(clinicalOrdersResponse)
        .flatMap((clinicalOrder) => clinicalOrder.items || [])

      if (!visitOrderItems.length) {
        throw new Error('Máy chủ không trả về chỉ định cận lâm sàng của lượt khám.')
      }

      const allItemsFinished = visitOrderItems.every((item) =>
        TERMINAL_ORDER_ITEM_STATUSES.has(item.status)
      )

      if (allItemsFinished) {
        const queueResponse = await queueApi.getById(updatedOrder.queueItemId)
        let queueItem = normalizeQueueItem(queueResponse?.data)
        if (!queueItem.id) {
          throw new Error('Máy chủ không trả về queue item hợp lệ.')
        }

        if (queueItem.status === 'WAITING_FOR_RESULT') {
          queueItem = normalizeQueueItem(
            (await queueApi.updateStatus(queueItem.id, 'IN_PROGRESS'))?.data
          )
        }

        if (!queueItem.id || queueItem.status !== 'IN_PROGRESS') {
          throw new Error(
            `Không thể đưa lượt khám về IN_PROGRESS (trạng thái hiện tại: ${queueItem.status || 'không xác định'}).`
          )
        }

        setOrders((currentOrders) =>
          currentOrders.filter(
            (order) => String(order.visitId) !== String(updatedOrder.visitId)
          )
        )
      }
    }

    return persistedResult
  }

  return (
    <div style={{ maxWidth: 1600, margin: '0 auto', padding: '4px 0 24px' }}>
      <div
        style={{
          display: 'flex',
          justify: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0, color: '#0f172a', fontWeight: 700 }}>
            <FileDoneOutlined style={{ color: '#2563eb', marginRight: 8 }} />
            Nhập kết quả cận lâm sàng
          </Title>
          <Text style={{ color: '#64748b', fontSize: 14 }}>
            Nhập và xác nhận kết quả theo lượt khám đang chờ cận lâm sàng.
          </Text>
        </div>

        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
            style={{ borderRadius: 8, borderColor: '#cbd5e1' }}
          >
            Tải lại
          </Button>
        </Space>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={8} md={4} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #2563eb',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Tổng phiếu CĐLS</span>}
              value={stats.total}
              prefix={<ExperimentOutlined style={{ color: '#2563eb' }} />}
              valueStyle={{ fontWeight: 700, color: '#1e293b' }}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #096dd9',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Chờ thực hiện</span>}
              value={stats.pending}
              valueStyle={{ color: '#096dd9', fontWeight: 700 }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={4} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #d46b08',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Đang thực hiện</span>}
              value={stats.inProgress}
              valueStyle={{ color: '#d46b08', fontWeight: 700 }}
              prefix={<SyncOutlined spin />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={6} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #722ed1',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Đã có kết quả</span>}
              value={stats.resulted}
              valueStyle={{ color: '#722ed1', fontWeight: 700 }}
              prefix={<FileDoneOutlined />}
            />
          </Card>
        </Col>

        <Col xs={12} sm={8} md={6} lg={4.8}>
          <Card
            size="small"
            style={{
              borderRadius: '12px',
              borderLeft: '4px solid #16a34a',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Statistic
              title={<span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Đã xác nhận & Khóa</span>}
              value={stats.confirmed}
              valueStyle={{ color: '#16a34a', fontWeight: 700 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card
        size="small"
        style={{
          borderRadius: '12px',
          marginBottom: 16,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
        }}
      >
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} md={12} lg={10}>
            <ResultSearch value={searchText} onChange={setSearchText} />
          </Col>

          <Col xs={24} md={12} lg={14} style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <ResultFilter
              status={statusFilter}
              onStatusChange={setStatusFilter}
              category={categoryFilter}
              onCategoryChange={setCategoryFilter}
              onReset={handleResetFilter}
            />
          </Col>
        </Row>
      </Card>

      <ResultTable
        dataSource={filteredOrders}
        loading={loading}
        onOpenModal={handleOpenModal}
      />

      <ResultModal
        visible={modalVisible}
        order={selectedOrder}
        onClose={() => setModalVisible(false)}
        onSaveSuccess={handleSaveResultSuccess}
      />
    </div>
  )
}

export default ResultPage
