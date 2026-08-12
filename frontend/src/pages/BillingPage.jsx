import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Col, Descriptions, Divider, Empty, Form, Input, List, message, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography } from 'antd'
import { CheckCircleOutlined, ClockCircleOutlined, CreditCardOutlined, DollarCircleOutlined, FileTextOutlined, LockOutlined, PrinterOutlined, ReloadOutlined, RightOutlined, SearchOutlined, WarningOutlined } from '@ant-design/icons'
import billingApi from '../api/billingApi'
import pharmacyApi from '../api/pharmacyApi'
import queueApi from '../api/queueApi'
import { useAuthContext } from '../context/AuthContext'

const { Text, Title } = Typography

const money = (val) => `${Number(val || 0).toLocaleString('vi-VN')} ₫`

const formatDateTime = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  return isNaN(date.getTime()) ? '—' : date.toLocaleString('vi-VN')
}

const PAYMENT_METHODS = [
  { value: 'CASH', label: '💵 Tiền mặt' },
  { value: 'BANK_TRANSFER', label: '🏦 Chuyển khoản' },
  { value: 'CREDIT_CARD', label: '💳 Thẻ ngân hàng' },
  { value: 'OTHER', label: '🌐 Phương thức khác' },
]

function BillingPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()

  // 1. Phân quyền Lễ tân (RECEPTIONIST) & Admin
  const userRoles = useMemo(() => {
    const raw = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return raw.map((r) => String(r || '').toLowerCase().replace(/^role_/, '')).filter(Boolean)
  }, [user])

  const canCollectPayment = userRoles.includes('receptionist') || userRoles.includes('admin')

  // State lượt khám & thanh toán
  const [visits, setVisits] = useState([])
  const [selectedVisitId, setSelectedVisitId] = useState(null)
  const [selectedVisitData, setSelectedVisitData] = useState(null)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [loadingVisits, setLoadingVisits] = useState(false)
  const [loadingData, setLoadingData] = useState(false)
  const [submittingPayment, setSubmittingPayment] = useState(false)
  const [submittingInvoice, setSubmittingInvoice] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [apiError, setApiError] = useState('')
  const [viewingInvoiceModal, setViewingInvoiceModal] = useState(null)

  // 2. Tải danh sách Lượt khám (Phân biệt rõ UUID visitId và String visitCode)
  const loadVisits = useCallback(async () => {
    setLoadingVisits(true)
    setApiError('')
    try {
      const today = new Date().toISOString().split('T')[0]
      const [payableRes, queueRes] = await Promise.allSettled([
        billingApi.getPayable({ page: 0, size: 50 }),
        queueApi.getQueues({ date: today }),
      ])

      let payableList = []
      if (payableRes.status === 'fulfilled') {
        const data = payableRes.value?.data
        payableList = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
      }

      let queueList = []
      if (queueRes.status === 'fulfilled') {
        const data = queueRes.value?.data
        queueList = Array.isArray(data) ? data : []
      }

      const mergedMap = new Map()
      queueList.forEach((q) => {
        const id = q.visitId || q.id
        if (id) {
          mergedMap.set(String(id), {
            ...q,
            visitId: id, // UUID thực sự
            visitCode: q.visitCode || q.queueCode || '—',
          })
        }
      })

      payableList.forEach((p) => {
        const id = p.visitId || p.id
        if (id) {
          const existing = mergedMap.get(String(id)) || {}
          mergedMap.set(String(id), {
            ...existing,
            ...p,
            visitId: id, // UUID thực sự
            visitCode: p.visitCode || existing.visitCode || '—',
            status: 'COMPLETED',
          })
        }
      })

      const finalVisits = Array.from(mergedMap.values())
      setVisits(finalVisits)

      if (location.state?.visitId) {
        setSelectedVisitId(location.state.visitId)
      }
    } catch (err) {
      console.error('[BillingPage] Lỗi loadVisits:', err)
      setApiError('Không thể tải danh sách lượt khám. Vui lòng thử lại.')
    } finally {
      setLoadingVisits(false)
    }
  }, [location.state])

  // 3. Tải chi tiết khoản thu & đơn thuốc theo đúng contract Backend (Status-based prescription query)
  const loadInvoiceData = useCallback(async (vUUID) => {
    if (!vUUID) {
      setSelectedVisitData(null)
      return
    }
    setLoadingData(true)
    setApiError('')

    const matchedVisit = visits.find((v) => String(v.visitId) === String(vUUID) || String(v.id) === String(vUUID))
    const visitUUID = matchedVisit?.visitId || vUUID
    const visitCode = matchedVisit?.visitCode || matchedVisit?.queueCode || '—'
    const visitStatus = matchedVisit?.status || 'IN_PROGRESS'
    const isVisitCompleted = visitStatus === 'COMPLETED' || visitStatus === 'WAITING_FOR_PAYMENT'

    try {
      const [invoiceRes, pendingRxRes, dispensedRxRes] = await Promise.allSettled([
        billingApi.getByVisit(visitUUID),
        pharmacyApi.prescriptions({ status: 'PENDING_DISPENSE', size: 100 }),
        pharmacyApi.prescriptions({ status: 'DISPENSED', size: 100 }),
      ])

      let invoiceData = null
      if (invoiceRes.status === 'fulfilled') {
        const rawData = invoiceRes.value?.data
        const list = Array.isArray(rawData?.content) ? rawData.content : Array.isArray(rawData) ? rawData : [rawData]
        invoiceData = list.find((i) => i && i.id) || null
      }

      // Tổng hợp đơn thuốc của lượt khám này theo UUID visitId hoặc visitCode
      let allPrescriptions = []
      if (pendingRxRes.status === 'fulfilled') {
        const raw = pendingRxRes.value?.data
        const list = Array.isArray(raw?.content) ? raw.content : Array.isArray(raw) ? raw : []
        allPrescriptions.push(...list)
      }
      if (dispensedRxRes.status === 'fulfilled') {
        const raw = dispensedRxRes.value?.data
        const list = Array.isArray(raw?.content) ? raw.content : Array.isArray(raw) ? raw : []
        allPrescriptions.push(...list)
      }

      const matchingRx = allPrescriptions.filter((p) =>
        String(p.visitId) === String(visitUUID) || String(p.visitCode) === String(visitCode),
      )

      let prescriptionItems = []
      let prescriptionStatus = null
      if (matchingRx.length > 0) {
        prescriptionStatus = matchingRx[0].status
        prescriptionItems = matchingRx.flatMap((p) => (Array.isArray(p.items) ? p.items : []))
      }

      const isDispensingCompleted = !prescriptionStatus || prescriptionStatus === 'DISPENSED'

      const examFee = Number(invoiceData?.examFee || 100000)
      const medicineFee = Number(
        invoiceData?.medicineFee ||
        prescriptionItems.reduce((s, i) => s + Number(i.quantity || 0) * Number(i.unitPrice || i.price || 0), 0),
      )
      const totalAmount = Number(invoiceData?.totalAmount || examFee + medicineFee)

      setSelectedVisitData({
        visitId: visitUUID, // UUID chuẩn gửi Backend
        paymentId: invoiceData?.paymentId || invoiceData?.id || null,
        visitCode,
        patientName: matchedVisit?.patientName || invoiceData?.patientName || 'Bệnh nhân',
        patientCode: matchedVisit?.patientCode || invoiceData?.patientCode || '—',
        doctorName: matchedVisit?.doctorName || invoiceData?.doctorName || 'Bác sĩ khám',
        visitStatus,
        prescriptionStatus,
        hasPrescription: matchingRx.length > 0,
        isVisitCompleted,
        isDispensingCompleted,
        isEligibleToPay: isVisitCompleted && isDispensingCompleted,
        examFee,
        medicineFee,
        totalAmount,
        paymentStatus: invoiceData?.status === 'PAID' || invoiceData?.status === 'COMPLETED' ? 'PAID' : 'UNPAID',
        invoiceCode: invoiceData?.invoiceCode || invoiceData?.code || null,
        paidAt: invoiceData?.paidAt || invoiceData?.createdAt || null,
        paymentMethodLabel: invoiceData?.paymentMethod || 'Tiền mặt',
        collectedBy: invoiceData?.collectedByName || user?.fullName || 'Lễ tân',
        prescriptionItems,
      })
    } catch (err) {
      console.error('[BillingPage] Lỗi loadInvoiceData:', err)
      setApiError('Không thể tải thông tin thanh toán. Vui lòng thử lại.')
    } finally {
      setLoadingData(false)
    }
  }, [user, visits])

  useEffect(() => { loadVisits() }, [loadVisits])

  useEffect(() => {
    if (selectedVisitId) loadInvoiceData(selectedVisitId)
  }, [selectedVisitId, loadInvoiceData])

  const filteredVisits = useMemo(() => {
    const kw = searchKeyword.trim().toLowerCase()
    if (!kw) return visits
    return visits.filter((v) =>
      [v.patientName, v.patientCode, v.visitCode, v.queueCode]
        .some((val) => String(val || '').toLowerCase().includes(kw)),
    )
  }, [visits, searchKeyword])

  // 4. Ghi nhận thanh toán (Gửi đúng RecordPaymentRequest DTO với UUID visitId)
  const handleConfirmPayment = async () => {
    if (!selectedVisitData || !selectedVisitData.visitId) return
    if (!canCollectPayment) {
      message.error('Bạn không có quyền thực hiện thu phí.')
      return
    }
    if (!selectedVisitData.isEligibleToPay) {
      message.warning('Lượt khám chưa đủ điều kiện thanh toán.')
      return
    }

    setSubmittingPayment(true)
    setApiError('')
    try {
      const payload = {
        visitId: selectedVisitData.visitId, // Bắt buộc UUID
        examFee: selectedVisitData.examFee,
        medicineFee: selectedVisitData.medicineFee,
        amountPaid: selectedVisitData.totalAmount,
        paymentMethod,
      }

      const res = await billingApi.pay(payload)
      const paymentRes = res?.data

      message.success(`Đã thu thành công ${money(selectedVisitData.totalAmount)} cho lượt khám ${selectedVisitData.visitCode}!`)
      await loadInvoiceData(selectedVisitData.visitId)
      await loadVisits()
    } catch (err) {
      console.error('[BillingPage] Lỗi payment:', err?.config?.url, err?.response?.status, err?.response?.data)
      const status = err?.response?.status
      const msg = err?.response?.data?.message

      if (status === 404) {
        setApiError('Không tìm thấy dữ liệu thanh toán hoặc lượt khám không tồn tại trên hệ thống (404).')
      } else if (status === 409) {
        setApiError('Khoản thu đã được xử lý hoặc trạng thái lượt khám đã thay đổi (409).')
        message.warning('Khoản thu này đã được thanh toán.')
        await loadInvoiceData(selectedVisitData.visitId)
      } else if (status === 403) {
        setApiError('Bạn không có quyền thực hiện thu phí.')
      } else {
        setApiError(msg || 'Không thể xử lý thanh toán. Vui lòng thử lại.')
      }
    } finally {
      setSubmittingPayment(false)
    }
  }

  // 5. Lập hóa đơn sau khi thanh toán PAID (Gửi CreateInvoiceRequest DTO)
  const handleCreateInvoice = async () => {
    if (!selectedVisitData) return
    if (selectedVisitData.paymentStatus !== 'PAID') {
      message.warning('Cần hoàn tất thu phí trước khi lập hóa đơn.')
      return
    }

    setSubmittingInvoice(true)
    setApiError('')
    try {
      const payload = {
        visitId: selectedVisitData.visitId,
        paymentId: selectedVisitData.paymentId,
      }

      const res = await billingApi.createInvoice(payload)
      const invoiceData = res?.data
      message.success(`Lập hóa đơn thành công! Mã HĐ: ${invoiceData?.invoiceCode || 'HD-NEW'}`)
      await loadInvoiceData(selectedVisitData.visitId)
    } catch (err) {
      console.error('[BillingPage] Lỗi createInvoice:', err?.config?.url, err?.response?.status, err?.response?.data)
      const status = err?.response?.status
      if (status === 409) {
        setApiError('Lượt khám đã có hóa đơn.')
        message.warning('Lượt khám đã có hóa đơn.')
      } else {
        setApiError('Không thể tạo hóa đơn điện tử. Vui lòng thử lại.')
      }
    } finally {
      setSubmittingInvoice(false)
    }
  }

  const feeColumns = [
    { title: 'Khoản thu / Dịch vụ', key: 'name', render: (_, r) => <strong>{r.name}</strong> },
    { title: 'Số lượng', dataIndex: 'quantity', key: 'quantity', width: 90, align: 'center' },
    { title: 'Đơn giá Backend', dataIndex: 'price', key: 'price', width: 150, align: 'right', render: (v) => money(v) },
    { title: 'Thành tiền', dataIndex: 'amount', key: 'amount', width: 160, align: 'right', render: (v) => <Text strong style={{ color: '#1677ff' }}>{money(v)}</Text> },
  ]

  const feeDataSource = selectedVisitData ? [
    { key: 'exam', name: 'Phí khám bệnh', quantity: 1, price: selectedVisitData.examFee, amount: selectedVisitData.examFee },
    { key: 'med', name: selectedVisitData.hasPrescription ? `Tiền thuốc kê đơn (${selectedVisitData.prescriptionItems.length} loại)` : 'Tiền thuốc (Không có đơn thuốc)', quantity: selectedVisitData.prescriptionItems.length || 0, price: selectedVisitData.medicineFee, amount: selectedVisitData.medicineFee },
  ] : []

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>
            <DollarCircleOutlined /> Thu phí &amp; Hóa đơn
          </Title>
          <Text type="secondary">NCL-07-CN-001 (Thu phí) &amp; NCL-07-CN-002 (Lập hóa đơn).</Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={loadingVisits} onClick={loadVisits}>
          Làm mới danh sách
        </Button>
      </div>

      {!canCollectPayment && (
        <Alert
          type="error"
          showIcon
          icon={<LockOutlined />}
          message="Bạn không có quyền thực hiện thu phí."
          description="Chức năng dành riêng cho tài khoản Lễ tân (RECEPTIONIST) hoặc Quản trị viên (ADMIN)."
          style={{ marginBottom: 16 }}
        />
      )}

      {apiError && (
        <Alert
          type="error"
          showIcon
          message="Thông báo từ hệ thống"
          description={apiError}
          action={<Button size="small" onClick={loadVisits}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]} align="stretch">
        {/* Cột trái: Danh sách Lượt khám */}
        <Col xs={24} xl={9}>
          <Card title={`Danh sách lượt khám (${filteredVisits.length})`} styles={{ body: { padding: 12 } }} style={{ height: '100%' }}>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="Tìm tên BN, mã BN, mã lượt khám..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              style={{ marginBottom: 12 }}
            />
            <List
              loading={loadingVisits}
              dataSource={filteredVisits}
              locale={{ emptyText: <Empty description="Không có lượt khám nào" /> }}
              style={{ maxHeight: 620, overflowY: 'auto' }}
              renderItem={(item) => {
                const vId = item.visitId || item.id
                const selected = String(vId) === String(selectedVisitId)
                const isCompleted = item.status === 'COMPLETED' || item.status === 'WAITING_FOR_PAYMENT'
                return (
                  <List.Item
                    key={vId}
                    onClick={() => setSelectedVisitId(vId)}
                    style={{
                      cursor: 'pointer',
                      border: selected ? '1px solid #1677ff' : '1px solid #f0f0f0',
                      background: selected ? '#e6f4ff' : '#fff',
                      borderRadius: 8,
                      marginBottom: 8,
                      padding: 12,
                    }}
                    extra={<RightOutlined style={{ color: selected ? '#1677ff' : '#bfbfbf' }} />}
                  >
                    <List.Item.Meta
                      title={(
                        <Space wrap>
                          <Text strong>{item.visitCode || item.queueCode || '—'}</Text>
                          {isCompleted ? (
                            <Tag color="green">Đã khám xong</Tag>
                          ) : (
                            <Tag color="orange" icon={<WarningOutlined />}>Đang khám ({item.status})</Tag>
                          )}
                        </Space>
                      )}
                      description={(
                        <Space direction="vertical" size={1}>
                          <Text strong style={{ color: '#0f172a' }}>{item.patientName} ({item.patientCode || '—'})</Text>
                          <Text type="secondary">Bác sĩ: {item.doctorName || '—'}</Text>
                        </Space>
                      )}
                    />
                  </List.Item>
                )
              }}
            />
          </Card>
        </Col>

        {/* Cột phải: Chi tiết Khoản Thu & Thanh Toán */}
        <Col xs={24} xl={15}>
          <Card title={selectedVisitData ? `Tổng hợp khoản thu: ${selectedVisitData.visitCode}` : 'Chi tiết khoản thu & Hóa đơn'} style={{ height: '100%' }}>
            {!selectedVisitData ? (
              <Empty description="Vui lòng chọn lượt khám ở danh sách bên trái" />
            ) : (
              <Space direction="vertical" size="large" style={{ width: '100%' }}>
                {/* 1. Thông tin lượt khám Read-Only */}
                <div>
                  <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>1. Thông tin lượt khám (Read-only)</Title>
                  <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
                    <Descriptions.Item label="Mã lượt khám"><Text strong>{selectedVisitData.visitCode}</Text></Descriptions.Item>
                    <Descriptions.Item label="Bệnh nhân"><Text strong style={{ color: '#1677ff' }}>{selectedVisitData.patientName}</Text> ({selectedVisitData.patientCode})</Descriptions.Item>
                    <Descriptions.Item label="Bác sĩ khám">{selectedVisitData.doctorName}</Descriptions.Item>
                    <Descriptions.Item label="Trạng thái lượt khám">
                      {selectedVisitData.isVisitCompleted ? (
                        <Tag color="green">Đã khám xong ({selectedVisitData.visitStatus})</Tag>
                      ) : (
                        <Tag color="orange" icon={<WarningOutlined />}>Đang khám ({selectedVisitData.visitStatus})</Tag>
                      )}
                    </Descriptions.Item>
                    <Descriptions.Item label="Trạng thái đơn thuốc">
                      {selectedVisitData.prescriptionStatus === 'DISPENSED' ? (
                        <Tag color="green">Đã xuất kho cấp thuốc (DISPENSED)</Tag>
                      ) : selectedVisitData.prescriptionStatus === 'PENDING_DISPENSE' ? (
                        <Tag color="orange" icon={<ClockCircleOutlined />}>Chờ Dược sĩ cấp phát (PENDING_DISPENSE)</Tag>
                      ) : (
                        <Tag color="default">Không có đơn thuốc</Tag>
                      )}
                    </Descriptions.Item>
                    <Descriptions.Item label="Trạng thái thanh toán">
                      {selectedVisitData.paymentStatus === 'PAID' ? (
                        <Tag color="green" icon={<CheckCircleOutlined />}>ĐÃ THANH TOÁN</Tag>
                      ) : (
                        <Tag color="orange">CHƯA THANH TOÁN (PENDING)</Tag>
                      )}
                    </Descriptions.Item>
                  </Descriptions>
                </div>

                {!selectedVisitData.isVisitCompleted && (
                  <Alert type="warning" showIcon icon={<WarningOutlined />} message="Lượt khám chưa đủ điều kiện thanh toán." description="Lượt khám đang ở trạng thái IN_PROGRESS. Chỉ lượt khám COMPLETED mới được ghi nhận thanh toán." />
                )}

                {selectedVisitData.isVisitCompleted && !selectedVisitData.isDispensingCompleted && (
                  <Alert type="warning" showIcon icon={<ClockCircleOutlined />} message="Thuốc chưa được cấp phát." description="Đơn thuốc đang ở trạng thái PENDING_DISPENSE. Dược sĩ cần xuất kho cấp thuốc trước khi Lễ tân thu phí." />
                )}

                <Divider style={{ margin: '4px 0' }} />

                <div>
                  <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>2. Chi tiết các khoản phải thu (Nguồn từ Backend)</Title>
                  <Table rowKey="key" columns={feeColumns} dataSource={feeDataSource} pagination={false} size="small" loading={loadingData} />
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
                    <Card size="small" style={{ backgroundColor: '#f8fafc', minWidth: 300, borderColor: '#cbd5e1' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text strong style={{ fontSize: 15 }}>TỔNG PHẢI THU:</Text>
                        <Text strong style={{ fontSize: 19, color: '#dc2626' }}>{money(selectedVisitData.totalAmount)}</Text>
                      </div>
                    </Card>
                  </div>
                </div>

                <Divider style={{ margin: '4px 0' }} />

                <div>
                  <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>3. Ghi nhận thanh toán (NCL-07-CN-001)</Title>
                  {selectedVisitData.paymentStatus === 'PAID' ? (
                    <Alert type="success" showIcon icon={<CheckCircleOutlined />} message="Khoản thu này đã được thanh toán thành công." description={`Mã HĐ/Thanh toán: ${selectedVisitData.invoiceCode || 'HD-PAID'} | Người thu: ${selectedVisitData.collectedBy} | Thời gian: ${formatDateTime(selectedVisitData.paidAt)}`} />
                  ) : (
                    <Card style={{ backgroundColor: '#f0f7ff', borderColor: '#bae6fd' }}>
                      <Row gutter={[16, 16]} align="middle">
                        <Col xs={24} md={14}>
                          <Form layout="vertical">
                            <Form.Item label={<strong>Phương thức thanh toán *</strong>} style={{ marginBottom: 0 }}>
                              <Select value={paymentMethod} onChange={setPaymentMethod} options={PAYMENT_METHODS} size="large" disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment} />
                            </Form.Item>
                          </Form>
                        </Col>
                        <Col xs={24} md={10} style={{ display: 'flex', justifyContent: 'flex-end' }}>
                          <Popconfirm
                            title={<Text strong style={{ color: '#1e3a8a' }}>Xác nhận ghi nhận thu phí</Text>}
                            description={`Xác nhận thu ${money(selectedVisitData.totalAmount)} cho ${selectedVisitData.visitCode}?`}
                            okText="Xác nhận thu tiền"
                            cancelText="Hủy"
                            onConfirm={handleConfirmPayment}
                            disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment || selectedVisitData.paymentStatus === 'PAID'}
                          >
                            <Button type="primary" size="large" icon={<CreditCardOutlined />} loading={submittingPayment} disabled={!canCollectPayment || !selectedVisitData.isEligibleToPay || submittingPayment || selectedVisitData.paymentStatus === 'PAID'} style={{ height: 42, padding: '0 20px', fontWeight: 600 }}>
                              Xác nhận thanh toán
                            </Button>
                          </Popconfirm>
                        </Col>
                      </Row>
                    </Card>
                  )}
                </div>

                <Divider style={{ margin: '4px 0' }} />

                <div>
                  <Title level={5} style={{ marginBottom: 8, color: '#1e3a8a' }}>4. Giao diện Hóa đơn điện tử (NCL-07-CN-002)</Title>
                  {selectedVisitData.paymentStatus !== 'PAID' ? (
                    <Alert type="warning" showIcon message="Cần hoàn tất thu phí trước khi lập hóa đơn." />
                  ) : (
                    <Space wrap>
                      {selectedVisitData.invoiceCode ? (
                        <Button type="primary" icon={<FileTextOutlined />} onClick={() => setViewingInvoiceModal(selectedVisitData)}>
                          Xem &amp; In hóa đơn điện tử ({selectedVisitData.invoiceCode})
                        </Button>
                      ) : (
                        <Button type="primary" icon={<FileTextOutlined />} loading={submittingInvoice} onClick={handleCreateInvoice}>
                          Lập hóa đơn điện tử
                        </Button>
                      )}
                    </Space>
                  )}
                </div>
              </Space>
            )}
          </Card>
        </Col>
      </Row>

      {/* Modal Xem & In Hóa đơn */}
      <Modal
        title={`HÓA ĐƠN ĐIỆN TỬ (${viewingInvoiceModal?.invoiceCode || 'CHÍNH THỨC'})`}
        open={!!viewingInvoiceModal}
        onCancel={() => setViewingInvoiceModal(null)}
        width={680}
        footer={[
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>
            In hóa đơn
          </Button>,
          <Button key="close" onClick={() => setViewingInvoiceModal(null)}>
            Đóng
          </Button>,
        ]}
      >
        {viewingInvoiceModal && (
          <div style={{ padding: 12, background: '#fff', border: '1px solid #e2e8f0', borderRadius: 8 }}>
            <div style={{ textAlign: 'center', marginBottom: 12, borderBottom: '2px solid #0f172a', paddingBottom: 6 }}>
              <Title level={4} style={{ margin: 0 }}>HÓA ĐƠN GIÁ TRỊ GIA TĂNG (HĐĐT)</Title>
              <Text type="secondary">Mã HĐ: <strong>{viewingInvoiceModal.invoiceCode || 'HD-001'}</strong> | Ngày lập: {formatDateTime(viewingInvoiceModal.paidAt)}</Text>
            </div>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 12 }}>
              <Descriptions.Item label="Mã lượt khám">{viewingInvoiceModal.visitCode}</Descriptions.Item>
              <Descriptions.Item label="Bệnh nhân"><strong>{viewingInvoiceModal.patientName}</strong> ({viewingInvoiceModal.patientCode})</Descriptions.Item>
              <Descriptions.Item label="Người lập HĐ">{viewingInvoiceModal.collectedBy}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái HĐ"><Tag color="green">ĐÃ LẬP HÓA ĐƠN</Tag></Descriptions.Item>
            </Descriptions>
            <Table rowKey="key" columns={feeColumns} dataSource={feeDataSource} pagination={false} size="small" />
            <div style={{ textAlign: 'right', marginTop: 12 }}>
              <Text strong style={{ fontSize: 16 }}>TỔNG TIỀN HÓA ĐƠN: </Text>
              <Text strong style={{ fontSize: 18, color: '#dc2626' }}>{money(viewingInvoiceModal.totalAmount)}</Text>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default BillingPage
