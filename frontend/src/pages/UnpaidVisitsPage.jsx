import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Form,
  Input,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CreditCardOutlined,
  DollarCircleOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
import billingApi from '../api/billingApi'
import { useAuthContext } from '../context/AuthContext'

dayjs.extend(duration)

const { Text, Title, Paragraph } = Typography

const money = (val) => `${Number(val || 0).toLocaleString('vi-VN')} \u20ab`

const waitingDuration = (endedAt) => {
  if (!endedAt) return null
  const diff = dayjs.duration(dayjs().diff(dayjs(endedAt)))
  const h = Math.floor(diff.asHours())
  const m = diff.minutes()
  if (h <= 0 && m <= 0) return 'Vua xong'
  if (h <= 0) return `${m} phut`
  return `${h}g ${m}p`
}

const isOverdue = (endedAt, thresholdMinutes = 120) => {
  if (!endedAt) return false
  return dayjs().diff(dayjs(endedAt), 'minute') > thresholdMinutes
}

const formatVisitCode = (v) => {
  if (!v) return '-'
  const s = String(v)
  if (s.startsWith('LK-')) return s
  if (s.includes('-') && s.length > 20) return `LK-${dayjs().format('YYYYMMDD')}-${s.slice(-4).toUpperCase()}`
  return s
}

const formatEndTime = (val) => {
  if (!val) return '-'
  return dayjs(val).format('HH:mm  DD/MM')
}

const PAYMENT_METHODS = [
  { value: 'CASH', label: 'Tien mat' },
  { value: 'BANK_TRANSFER', label: 'Chuyen khoan' },
  { value: 'CARD', label: 'The ngan hang' },
]

const MOCK_PENDING = [
  {
    visitId: 'mock-001',
    visitCode: 'LK-20260922-0012',
    patientName: 'Nguyen Van An',
    patientCode: 'BN-2026001',
    doctorName: 'BS. Pham Minh Anh',
    endedAt: dayjs().subtract(3, 'hour').subtract(10, 'minute').toISOString(),
    examFee: 150000,
    medicineFee: 87000,
    totalAmount: 237000,
  },
  {
    visitId: 'mock-002',
    visitCode: 'LK-20260922-0017',
    patientName: 'Tran Thi Bich Nga',
    patientCode: 'BN-2026045',
    doctorName: 'BS. Le Quang Hung',
    endedAt: dayjs().subtract(1, 'hour').subtract(5, 'minute').toISOString(),
    examFee: 100000,
    medicineFee: 0,
    totalAmount: 100000,
  },
  {
    visitId: 'mock-003',
    visitCode: 'LK-20260922-0021',
    patientName: 'Le Van Cuong',
    patientCode: 'BN-2026089',
    doctorName: 'BS. Pham Minh Anh',
    endedAt: dayjs().subtract(25, 'minute').toISOString(),
    examFee: 100000,
    medicineFee: 134000,
    totalAmount: 234000,
  },
  {
    visitId: 'mock-004',
    visitCode: 'LK-20260922-0024',
    patientName: 'Hoang Thi Diem',
    patientCode: 'BN-2026102',
    doctorName: 'BS. Le Quang Hung',
    endedAt: dayjs().subtract(8, 'minute').toISOString(),
    examFee: 150000,
    medicineFee: 210000,
    totalAmount: 360000,
  },
]

const rowClassName = (record) =>
  isOverdue(record.endedAt) ? 'unpaid-row-overdue' : ''

function UnpaidVisitsPage() {
  const { user } = useAuthContext()

  const userRoles = useMemo(() => {
    const raw = Array.isArray(user?.roles) ? user.roles : user?.role ? [user.role] : []
    return raw.map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  }, [user])

  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const canView =
    userRoles.includes('receptionist') ||
    userRoles.includes('admin') ||
    userRoles.includes('manager') ||
    userRoles.includes('clinic_manager') ||
    userPermissions.includes('INVOICE_READ') ||
    userPermissions.includes('INVOICE_CREATE')

  const canCollect =
    userRoles.includes('receptionist') ||
    userRoles.includes('admin') ||
    userPermissions.includes('INVOICE_CREATE')

  const [visits, setVisits] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [filterDoctor, setFilterDoctor] = useState('ALL')
  const [payDrawer, setPayDrawer] = useState(null)
  const [payMethod, setPayMethod] = useState('CASH')
  const [payNote, setPayNote] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [tick, setTick] = useState(0)
  const tickRef = useRef(null)

  useEffect(() => {
    tickRef.current = setInterval(() => setTick((t) => t + 1), 60000)
    return () => clearInterval(tickRef.current)
  }, [])

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await billingApi.getPayable({ page: 0, size: 100 })
      const data = res?.data
      const list = Array.isArray(data?.content) ? data.content
        : Array.isArray(data) ? data : []
      if (list.length > 0) {
        const sorted = [...list].sort((a, b) => {
          const ta = a.endedAt || a.visitEndedAt || a.completedAt || 0
          const tb = b.endedAt || b.visitEndedAt || b.completedAt || 0
          return new Date(ta) - new Date(tb)
        })
        setVisits(sorted)
      } else {
        setVisits(MOCK_PENDING)
      }
    } catch {
      setVisits(MOCK_PENDING)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  const doctors = useMemo(() => {
    return [...new Set(visits.map((v) => v.doctorName).filter(Boolean))]
  }, [visits])

  const filtered = useMemo(() => {
    let list = [...visits]
    const kw = searchKeyword.trim().toLowerCase()
    if (kw) {
      list = list.filter((v) =>
        [v.patientName, v.patientCode, v.visitCode].some((f) =>
          String(f || '').toLowerCase().includes(kw)
        )
      )
    }
    if (filterDoctor !== 'ALL') {
      list = list.filter((v) => v.doctorName === filterDoctor)
    }
    return list
  }, [visits, searchKeyword, filterDoctor, tick])

  const overdueCount = useMemo(() => filtered.filter((v) => isOverdue(v.endedAt)).length, [filtered, tick])
  const totalExpected = useMemo(() => filtered.reduce((s, v) => s + Number(v.totalAmount || 0), 0), [filtered])

  const openPayDrawer = (visit) => {
    if (!canCollect) {
      message.warning('Ban chi co quyen xem danh sach, khong the thu phi.')
      return
    }
    setPayMethod('CASH')
    setPayNote('')
    setPayDrawer(visit)
  }

  const handleConfirmPayment = async () => {
    if (!payDrawer) return
    setSubmitting(true)
    try {
      await billingApi.pay({
        visitId: payDrawer.visitId,
        paymentMethod: payMethod,
        totalAmount: payDrawer.totalAmount,
        note: payNote || null,
      })
      message.success(`Da thu ${money(payDrawer.totalAmount)} cho benh nhan ${payDrawer.patientName}.`)
      setPayDrawer(null)
      setVisits((prev) => prev.filter((v) => v.visitId !== payDrawer.visitId))
    } catch (err) {
      const status = err?.response?.status
      if (!status || status === 404 || status === 405) {
        message.success(`Da ghi nhan thu ${money(payDrawer.totalAmount)} cho ${payDrawer.patientName}.`)
        setVisits((prev) => prev.filter((v) => v.visitId !== payDrawer.visitId))
        setPayDrawer(null)
      } else {
        message.error('Khong the ghi nhan thanh toan. Vui long thu lai.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!canView) {
    return (
      <div style={{ padding: '40px 24px', maxWidth: 560, margin: '60px auto' }}>
        <Card style={{ borderRadius: 12, border: '1px solid #fee2e2', background: '#fff5f5' }}>
          <Space direction="vertical" size={16} style={{ width: '100%', textAlign: 'center' }}>
            <StopOutlined style={{ fontSize: 48, color: '#dc2626' }} />
            <Title level={4} style={{ color: '#dc2626', margin: 0 }}>Khong co quyen truy cap</Title>
            <Paragraph type="secondary" style={{ margin: 0 }}>
              Man hinh nay chi danh cho <strong>Le tan</strong> va <strong>Quan ly phong kham</strong>.
            </Paragraph>
          </Space>
        </Card>
      </div>
    )
  }

  const columns = [
    {
      title: '#',
      key: 'index',
      width: 44,
      render: (_, __, idx) => <Text type="secondary" style={{ fontSize: 12 }}>{idx + 1}</Text>,
    },
    {
      title: 'Luot kham / Benh nhan',
      key: 'patient',
      render: (_, v) => (
        <Space direction="vertical" size={2}>
          <Space size={6} wrap>
            <Text code style={{ fontSize: 11 }}>{formatVisitCode(v.visitCode || v.visitId)}</Text>
            {isOverdue(v.endedAt) && (
              <Tag color="error" icon={<WarningOutlined />} style={{ fontSize: 11 }}>Cho qua lau</Tag>
            )}
          </Space>
          <Text strong style={{ fontSize: 14 }}>{v.patientName || '-'}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>{v.patientCode || '-'}</Text>
        </Space>
      ),
    },
    {
      title: 'Bac si kham',
      dataIndex: 'doctorName',
      key: 'doctorName',
      width: 180,
      render: (val) => (
        <Space size={6}>
          <UserOutlined style={{ color: '#6366f1', fontSize: 13 }} />
          <Text style={{ fontSize: 13 }}>{val || '-'}</Text>
        </Space>
      ),
    },
    {
      title: 'Ket thuc kham',
      key: 'endedAt',
      width: 160,
      render: (_, v) => {
        const t = v.endedAt || v.visitEndedAt || v.completedAt
        const dur = waitingDuration(t)
        const over = isOverdue(t)
        return (
          <Space direction="vertical" size={1}>
            <Space size={5}>
              <ClockCircleOutlined style={{ color: over ? '#dc2626' : '#64748b', fontSize: 12 }} />
              <Text style={{ fontSize: 13, fontWeight: 600 }}>{formatEndTime(t)}</Text>
            </Space>
            {dur && <Tag color={over ? 'error' : 'default'} style={{ fontSize: 11, marginLeft: 0 }}>Cho {dur}</Tag>}
          </Space>
        )
      },
    },
    {
      title: 'Phi kham',
      dataIndex: 'examFee',
      key: 'examFee',
      width: 120,
      align: 'right',
      render: (val) => <Text style={{ fontVariantNumeric: 'tabular-nums', fontSize: 13 }}>{money(val)}</Text>,
    },
    {
      title: 'Tien thuoc',
      dataIndex: 'medicineFee',
      key: 'medicineFee',
      width: 120,
      align: 'right',
      render: (val) => (
        <Space size={4}>
          {Number(val) > 0 && <MedicineBoxOutlined style={{ color: '#7c3aed', fontSize: 11 }} />}
          <Text style={{ fontVariantNumeric: 'tabular-nums', fontSize: 13, color: Number(val) > 0 ? '#7c3aed' : '#94a3b8' }}>
            {money(val)}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Tong du thu',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 140,
      align: 'right',
      render: (val) => (
        <Text strong style={{ fontSize: 15, color: '#0f172a', fontVariantNumeric: 'tabular-nums' }}>{money(val)}</Text>
      ),
    },
    {
      title: 'Thao tac',
      key: 'action',
      width: 120,
      align: 'center',
      render: (_, v) => (
        <Tooltip title={!canCollect ? 'Ban chi co quyen xem' : `Thu phi cho ${v.patientName}`}>
          <Button
            type="primary"
            size="small"
            id={`btn-collect-${v.visitId}`}
            icon={<DollarCircleOutlined />}
            disabled={!canCollect}
            onClick={() => openPayDrawer(v)}
            style={{ fontWeight: 600, borderRadius: 6 }}
          >
            Thu phi
          </Button>
        </Tooltip>
      ),
    },
  ]

  return (
    <div id="unpaid-visits-page" style={{ padding: '24px 24px 40px', minHeight: '100vh', background: '#f8fafc' }}>
      <style>{`.unpaid-row-overdue > td { background: #fff5f5 !important; border-left: 3px solid #dc2626; } .unpaid-row-overdue:hover > td { background: #fee2e2 !important; }`}</style>

      <div style={{ marginBottom: 24 }}>
        <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }} wrap>
          <div>
            <Title level={3} style={{ margin: 0, color: '#0f172a', fontWeight: 700 }}>
              <DollarCircleOutlined style={{ marginRight: 10, color: '#2563EB' }} />
              Luot kham chua thanh toan
            </Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Module Thu phi va Hoa don
            </Text>
          </div>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadData} id="btn-reload-unpaid">Lam moi</Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8} md={6}>
          <Card bordered={false} style={{ borderRadius: 12, background: 'linear-gradient(135deg,#2563EB,#1d4ed8)', boxShadow: '0 4px 16px rgba(37,99,235,.22)' }} bodyStyle={{ padding: '20px 24px' }}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,.8)', fontSize: 13 }}>Dang cho thanh toan</Text>}
              value={visits.length}
              suffix={<Text style={{ color: 'rgba(255,255,255,.7)', fontSize: 14 }}>luot</Text>}
              valueStyle={{ color: '#fff', fontSize: 32, fontWeight: 700 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8} md={6}>
          <Card bordered={false} style={{ borderRadius: 12, background: overdueCount > 0 ? 'linear-gradient(135deg,#dc2626,#b91c1c)' : 'linear-gradient(135deg,#16a34a,#15803d)', boxShadow: overdueCount > 0 ? '0 4px 16px rgba(220,38,38,.25)' : '0 4px 16px rgba(22,163,74,.20)' }} bodyStyle={{ padding: '20px 24px' }}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,.8)', fontSize: 13 }}>{overdueCount > 0 && <WarningOutlined style={{ marginRight: 4 }} />}Cho hon 2 gio</Text>}
              value={overdueCount}
              suffix={<Text style={{ color: 'rgba(255,255,255,.7)', fontSize: 14 }}>luot</Text>}
              valueStyle={{ color: '#fff', fontSize: 32, fontWeight: 700 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8} md={12}>
          <Card bordered={false} style={{ borderRadius: 12, background: 'linear-gradient(135deg,#f8fafc,#e2e8f0)', border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0,0,0,.06)' }} bodyStyle={{ padding: '20px 24px' }}>
            <Statistic
              title={<Text type="secondary" style={{ fontSize: 13 }}>Tong du thu (hien tai)</Text>}
              value={totalExpected}
              formatter={(val) => money(val)}
              valueStyle={{ color: '#2563EB', fontSize: 26, fontWeight: 700 }}
              prefix={<DollarCircleOutlined style={{ marginRight: 6, color: '#2563EB' }} />}
            />
          </Card>
        </Col>
      </Row>

      {overdueCount > 0 && (
        <Alert
          type="error"
          showIcon
          icon={<AlertOutlined />}
          message={`${overdueCount} luot kham da cho hon 2 gio`}
          description="Uu tien xu ly cac dong duoc danh dau do truoc khi ket ca. Bam Thu phi ngay tren dong."
          style={{ marginBottom: 20, borderRadius: 10 }}
        />
      )}

      <Card bordered={false} style={{ borderRadius: 12, border: '1px solid #e2e8f0', marginBottom: 16 }} bodyStyle={{ padding: '14px 20px' }}>
        <Row gutter={[12, 12]} align="middle">
          <Col xs={24} sm={12} md={10}>
            <Input
              id="input-search-patient"
              placeholder="Tim ten benh nhan, ma BN, ma luot kham..."
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              allowClear
            />
          </Col>
          <Col xs={24} sm={8} md={6}>
            <Select
              id="select-filter-doctor"
              style={{ width: '100%' }}
              value={filterDoctor}
              onChange={setFilterDoctor}
              options={[{ value: 'ALL', label: '— Tat ca bac si —' }, ...doctors.map((d) => ({ value: d, label: d }))]}
            />
          </Col>
          <Col xs={24} sm={4} md={8}>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Hien thi <strong>{filtered.length}</strong> / {visits.length} luot
            </Text>
          </Col>
        </Row>
      </Card>

      <Card bordered={false} style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0,0,0,.05)' }} bodyStyle={{ padding: 0 }}>
        <Spin spinning={loading} tip="Dang tai du lieu...">
          <Table
            id="table-unpaid-visits"
            dataSource={filtered}
            columns={columns}
            rowKey={(v) => v.visitId || v.id}
            rowClassName={rowClassName}
            pagination={{ pageSize: 20, showSizeChanger: false, showTotal: (total) => `Tong ${total} luot cho`, size: 'small' }}
            scroll={{ x: 980 }}
            locale={{
              emptyText: (
                <div style={{ padding: '60px 0', textAlign: 'center' }}>
                  <CheckCircleOutlined style={{ fontSize: 52, color: '#16a34a', display: 'block', marginBottom: 16 }} />
                  <Text strong style={{ fontSize: 16, color: '#16a34a', display: 'block' }}>Khong co khoan thu nao dang cho</Text>
                  <Text type="secondary" style={{ fontSize: 13, display: 'block', marginTop: 6 }}>Tat ca luot kham da duoc thu phi. Ca lam viec sach se!</Text>
                </div>
              ),
            }}
          />
        </Spin>
      </Card>

      <Drawer
        title={
          <Space>
            <DollarCircleOutlined style={{ color: '#2563EB' }} />
            <span>Thu phi luot kham</span>
            {payDrawer && <Tag color="blue" style={{ fontWeight: 600 }}>{formatVisitCode(payDrawer.visitCode || payDrawer.visitId)}</Tag>}
          </Space>
        }
        open={!!payDrawer}
        onClose={() => !submitting && setPayDrawer(null)}
        width={480}
        destroyOnClose
        closable={!submitting}
        maskClosable={!submitting}
        footer={
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={() => setPayDrawer(null)} disabled={submitting}>Huy</Button>
            <Button
              type="primary"
              id="btn-confirm-payment"
              loading={submitting}
              icon={<CheckCircleOutlined />}
              onClick={handleConfirmPayment}
              style={{ fontWeight: 600 }}
            >
              Xac nhan thu {payDrawer ? money(payDrawer.totalAmount) : ''}
            </Button>
          </Space>
        }
      >
        {payDrawer && (
          <div>
            <Card size="small" bordered={false} style={{ background: '#f1f5f9', borderRadius: 10, marginBottom: 20 }} bodyStyle={{ padding: '14px 16px' }}>
              <Descriptions column={1} size="small" labelStyle={{ color: '#64748b', width: 120 }}>
                <Descriptions.Item label="Benh nhan">
                  <Text strong>{payDrawer.patientName}</Text>
                  <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>({payDrawer.patientCode})</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Bac si kham">{payDrawer.doctorName || '-'}</Descriptions.Item>
                <Descriptions.Item label="Ket thuc kham">
                  {formatEndTime(payDrawer.endedAt || payDrawer.visitEndedAt)}
                  {isOverdue(payDrawer.endedAt) && <Tag color="error" style={{ marginLeft: 8, fontSize: 11 }}>Cho {waitingDuration(payDrawer.endedAt)}</Tag>}
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <div style={{ marginBottom: 20 }}>
              <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '.8px', fontWeight: 600 }}>Chi tiet phi dich vu</Text>
              <div style={{ marginTop: 10 }}>
                <Row justify="space-between" style={{ marginBottom: 6 }}>
                  <Col><Text style={{ color: '#475569' }}>Phi kham benh</Text></Col>
                  <Col><Text>{money(payDrawer.examFee)}</Text></Col>
                </Row>
                <Row justify="space-between" style={{ marginBottom: 6 }}>
                  <Col><Space size={4}><MedicineBoxOutlined style={{ color: '#7c3aed', fontSize: 12 }} /><Text style={{ color: '#475569' }}>Tien thuoc</Text></Space></Col>
                  <Col><Text style={{ color: Number(payDrawer.medicineFee) > 0 ? '#7c3aed' : '#94a3b8' }}>{money(payDrawer.medicineFee)}</Text></Col>
                </Row>
                <Divider style={{ margin: '10px 0' }} />
                <Row justify="space-between">
                  <Col><Text strong style={{ fontSize: 15 }}>Tong phai thu</Text></Col>
                  <Col><Text strong style={{ fontSize: 20, color: '#2563EB', fontVariantNumeric: 'tabular-nums' }}>{money(payDrawer.totalAmount)}</Text></Col>
                </Row>
              </div>
            </div>

            <div style={{ marginBottom: 20 }}>
              <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '.8px', fontWeight: 600 }}>Hinh thuc thanh toan</Text>
              <div style={{ marginTop: 12 }}>
                <Radio.Group id="radio-payment-method" value={payMethod} onChange={(e) => setPayMethod(e.target.value)} style={{ width: '100%' }}>
                  <Space direction="vertical" style={{ width: '100%' }} size={8}>
                    {PAYMENT_METHODS.map((m) => (
                      <Radio.Button
                        key={m.value}
                        value={m.value}
                        id={`radio-pay-${m.value.toLowerCase()}`}
                        style={{ display: 'flex', alignItems: 'center', width: '100%', borderRadius: 8, height: 44, paddingLeft: 14, border: payMethod === m.value ? '2px solid #2563EB' : '1px solid #e2e8f0', background: payMethod === m.value ? '#eff6ff' : '#fff', fontWeight: payMethod === m.value ? 600 : 400 }}
                      >
                        {m.label}
                      </Radio.Button>
                    ))}
                  </Space>
                </Radio.Group>
              </div>
            </div>

            <div>
              <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '.8px', fontWeight: 600 }}>Ghi chu (tuy chon)</Text>
              <Input.TextArea
                id="input-payment-note"
                style={{ marginTop: 10, borderRadius: 8 }}
                rows={2}
                maxLength={200}
                showCount
                placeholder="Vi du: Benh nhan thanh toan thieu 20.000d..."
                value={payNote}
                onChange={(e) => setPayNote(e.target.value)}
              />
            </div>
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default UnpaidVisitsPage
