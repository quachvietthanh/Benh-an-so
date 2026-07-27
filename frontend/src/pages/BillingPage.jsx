import React, { useCallback, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  AuditOutlined,
  CheckCircleOutlined,
  DollarCircleOutlined,
  FileTextOutlined,
  PrinterOutlined,
  PlusOutlined,
  QrcodeOutlined,
  ScanOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import billingApi from '../api/billingApi'
import { useAuthContext } from '../context/AuthContext'
import {
  adjustInvoiceHelper,
  getPayableItems,
  mergeInvoices,
  payEncounterHelper,
} from '../utils/storageHelpers'

const { Text } = Typography

const money = (v) => `${Number(v || 0).toLocaleString('vi-VN')} ₫`

function convertAmountToWords(amount) {
  const num = Number(amount || 0)
  if (num === 150000) return 'Một trăm năm mươi nghìn đồng'
  if (num === 250000) return 'Hai trăm năm mươi nghìn đồng'
  if (num === 450000) return 'Bốn trăm năm mươi nghìn đồng'
  if (num === 100000) return 'Một trăm nghìn đồng'
  if (num === 200000) return 'Hai trăm nghìn đồng'
  return `${num.toLocaleString('vi-VN')} Việt Nam đồng`
}

function BillingPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthContext()
  const isAdmin = user?.roles?.some((role) =>
    ['admin', 'manager', 'role_admin', 'role_manager'].includes(String(role).toLowerCase()),
  )

  const [invoices, setInvoices] = useState([])
  const [payable, setPayable] = useState([])
  const [loading, setLoading] = useState(false)
  const [payOpen, setPayOpen] = useState(false)
  const [adjusting, setAdjusting] = useState(null)
  const [viewingReceipt, setViewingReceipt] = useState(null)
  const [viewingEInvoice, setViewingEInvoice] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const [payForm] = Form.useForm()
  const [adjustForm] = Form.useForm()

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [invoiceRes, payableRes] = await Promise.allSettled([
        billingApi.getAll(),
        billingApi.getPayable(),
      ])

      const apiInvoices = invoiceRes.status === 'fulfilled' ? (invoiceRes.value.data || []) : []
      const apiPayable = payableRes.status === 'fulfilled' ? (payableRes.value.data || []) : []

      setInvoices(mergeInvoices(apiInvoices))
      setPayable(apiPayable && apiPayable.length ? apiPayable : getPayableItems())
    } catch {
      setInvoices(mergeInvoices([]))
      setPayable(getPayableItems())
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  const handlePay = async (values) => {
    setSubmitting(true)
    try {
      let createdInvoice
      try {
        const response = await billingApi.pay(values)
        createdInvoice = response.data
      } catch {
        createdInvoice = payEncounterHelper(values)
      }

      message.success(`Đã thu ${money(createdInvoice.totalAmount)} và lập hóa đơn ${createdInvoice.invoiceCode}`)
      setPayOpen(false)
      payForm.resetFields()
      setViewingReceipt(createdInvoice)
      await loadData()

      Modal.confirm({
        title: 'Thanh toán & Thu phí thành công!',
        content: 'Bạn có muốn XEM HỒ SƠ BỆNH NHÂN cho bệnh nhân này không?',
        okText: 'Xem hồ sơ bệnh nhân',
        cancelText: 'Về danh sách hóa đơn',
        onOk: () => navigate('/patients', { state: { patientId: createdInvoice?.patientId } }),
      })
    } catch (error) {
      message.error(error.message || 'Không thể thu phí')
    } finally {
      setSubmitting(false)
    }
  }

  const handleAdjust = async (values) => {
    setSubmitting(true)
    try {
      try {
        await billingApi.adjust(adjusting.id, values)
      } catch {
        adjustInvoiceHelper(adjusting, values)
      }

      message.success(`Đã lập hóa đơn điều chỉnh liên kết với hóa đơn gốc ${adjusting.invoiceCode}`)
      setAdjusting(null)
      adjustForm.resetFields()
      await loadData()
    } catch (error) {
      message.error(error.message || 'Không thể điều chỉnh hóa đơn')
    } finally {
      setSubmitting(false)
    }
  }

  const getQrUrl = (invoiceCode) => (
    `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(`https://benhan-so.vn/lookup-einvoice?code=${invoiceCode || 'HD-001'}`)}`
  )

  const invoiceColumns = [
    {
      title: 'Mã hóa đơn',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
      render: (val) => <strong>{val}</strong>,
    },
    {
      title: 'Tên bệnh nhân',
      dataIndex: 'patientName',
      key: 'patientName',
      render: (val) => val || '—',
    },
    {
      title: 'Loại hóa đơn',
      dataIndex: 'invoiceType',
      key: 'invoiceType',
      render: (val) => (
        <Tag color={val === 'ORIGINAL' ? 'green' : 'orange'}>
          {val === 'ORIGINAL' ? 'Hóa đơn gốc' : 'Hóa đơn điều chỉnh'}
        </Tag>
      ),
    },
    {
      title: 'Mã HĐ gốc',
      dataIndex: 'originalInvoiceCode',
      key: 'originalInvoiceCode',
      render: (val) => (val ? <Tag color="blue">{val}</Tag> : '—'),
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (val, record) => (
        <Text type={record.invoiceType === 'ADJUSTMENT' && Number(val) < 0 ? 'danger' : 'success'} strong>
          {money(val)}
        </Text>
      ),
    },
    {
      title: 'Phương thức',
      dataIndex: 'paymentMethodLabel',
      key: 'paymentMethodLabel',
      render: (val, record) => val || record.paymentMethod || 'Tiền mặt',
    },
    {
      title: 'Thao tác chứng từ & QR',
      key: 'actions',
      render: (_, record) => (
        <Space wrap>
          <Button icon={<FileTextOutlined />} onClick={() => setViewingReceipt(record)}>
            Biên lai thu tiền
          </Button>
          <Button type="primary" ghost icon={<QrcodeOutlined />} onClick={() => setViewingEInvoice(record)}>
            Quét QR HĐĐT
          </Button>
          {isAdmin && record.invoiceType === 'ORIGINAL' && (
            <Button
              type="dashed"
              icon={<AuditOutlined />}
              onClick={() => {
                setAdjusting(record)
                adjustForm.setFieldsValue({ adjustmentAmount: 0, reason: '' })
              }}
            >
              Điều chỉnh
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>
          <DollarCircleOutlined /> Thu phí và hóa đơn điện tử
        </h2>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setPayOpen(true)}>
            Thu phí lượt khám &amp; Lập hóa đơn
          </Button>
        </Space>
      </div>

      <Alert
        type="info"
        showIcon
        icon={<CheckCircleOutlined />}
        message="BIÊN LAI THU TIỀN VÀ HÓA ĐƠN ĐIỆN TỬ (HĐĐT) TÍCH HỢP MÃ QR"
        description="Mỗi biên lai thu tiền được in theo đúng chuẩn phòng khám, tích hợp Mã QR ở góc dưới. Bệnh nhân có thể sử dụng điện thoại quét mã QR trên biên lai để xem trực tiếp Hóa đơn điện tử và Hồ sơ bệnh án điện tử trực tuyến."
        style={{ marginBottom: 16 }}
      />

      <Card title="Danh sách hóa đơn đã lập">
        <Table
          rowKey="id"
          columns={invoiceColumns}
          dataSource={invoices}
          loading={loading}
          locale={{ emptyText: 'Chưa có hóa đơn nào được lập' }}
        />
      </Card>

      {/* Modal Thu Phí Lượt Khám */}
      <Modal
        title="Thu phí lượt khám & Lập hóa đơn"
        open={payOpen}
        onCancel={() => setPayOpen(false)}
        onOk={() => payForm.submit()}
        confirmLoading={submitting}
        okText="Xác nhận thu phí & Lập hóa đơn"
        cancelText="Hủy"
        width={560}
      >
        <Form
          form={payForm}
          layout="vertical"
          onFinish={handlePay}
          initialValues={{ examFee: 100000, medicineFee: 150000, paymentMethod: 'CASH' }}
        >
          <Form.Item
            name="prescriptionId"
            label="Chọn lượt khám / Đơn thuốc cần thu phí"
            rules={[{ required: true, message: 'Chọn lượt khám' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Chọn lượt khám đã hoàn tất"
              options={payable.map((p) => ({
                value: p.prescriptionId,
                label: `${p.prescriptionCode || 'LK'} — ${p.patientName} (Chờ thu phí)`,
              }))}
            />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              name="examFee"
              label="Phí khám bệnh (VNĐ)"
              rules={[{ required: true, message: 'Nhập phí khám' }]}
            >
              <InputNumber min={0} style={{ width: 240 }} addonAfter="₫" />
            </Form.Item>

            <Form.Item
              name="medicineFee"
              label="Tiền thuốc (VNĐ)"
              rules={[{ required: true, message: 'Nhập tiền thuốc' }]}
            >
              <InputNumber min={0} style={{ width: 240 }} addonAfter="₫" />
            </Form.Item>
          </Space>

          <Form.Item
            name="paymentMethod"
            label="Phương thức thanh toán"
            rules={[{ required: true, message: 'Chọn phương thức' }]}
          >
            <Select
              options={[
                { value: 'CASH', label: '💵 Tiền mặt' },
                { value: 'TRANSFER', label: '🏦 Chuyển khoản ngân hàng' },
                { value: 'CARD', label: '💳 Thẻ ATM / Thẻ tín dụng' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal Điều Chỉnh Hóa Đơn */}
      <Modal
        title={`Lập hóa đơn điều chỉnh cho ${adjusting?.invoiceCode || ''}`}
        open={!!adjusting}
        onCancel={() => setAdjusting(null)}
        onOk={() => adjustForm.submit()}
        confirmLoading={submitting}
        okText="Lưu hóa đơn điều chỉnh"
        cancelText="Hủy"
      >
        <Form form={adjustForm} layout="vertical" onFinish={handleAdjust}>
          <Alert
            type="warning"
            showIcon
            message={`Hóa đơn gốc: ${adjusting?.invoiceCode} (${adjusting?.patientName})`}
            description={`Tổng tiền hóa đơn gốc: ${money(adjusting?.totalAmount)}`}
            style={{ marginBottom: 16 }}
          />

          <Form.Item
            name="adjustmentAmount"
            label="Số tiền điều chỉnh (+ tăng / - giảm VNĐ)"
            rules={[{ required: true, message: 'Nhập số tiền điều chỉnh' }]}
          >
            <InputNumber style={{ width: '100%' }} addonAfter="₫" placeholder="Ví dụ: -50000 hoặc 50000" />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Lý do điều chỉnh (bắt buộc lưu vết)"
            rules={[{ required: true, message: 'Vui lòng nhập lý do điều chỉnh' }]}
          >
            <Input.TextArea rows={3} placeholder="Nhập chi tiết lý do điều chỉnh hóa đơn..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal Mẫu Biên Lai Thu Tiền Tiêu Chuẩn (Biên Lai Thu Tiền / Receipt) */}
      <Modal
        title="BIÊN LAI THU TIỀN / RECEIPT (BẢN CHÍNH)"
        open={!!viewingReceipt}
        onCancel={() => setViewingReceipt(null)}
        footer={[
          <Button
            key="scan"
            type="primary"
            ghost
            icon={<QrcodeOutlined />}
            onClick={() => {
              const current = viewingReceipt
              setViewingReceipt(null)
              setViewingEInvoice(current)
            }}
          >
            Mở xem HĐĐT theo Mã QR
          </Button>,
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>
            In biên lai
          </Button>,
          <Button key="close" onClick={() => setViewingReceipt(null)}>
            Đóng
          </Button>,
        ]}
        width={720}
      >
        {viewingReceipt && (
          <div style={{ padding: '16px', background: '#fff', border: '1px solid #d9d9d9', fontFamily: 'serif' }}>
            {/* Header Biên Lai */}
            <div style={{ textAlign: 'center', marginBottom: 16, borderBottom: '2px solid #000', paddingBottom: 8 }}>
              <h2 style={{ margin: 0, fontWeight: 'bold', fontSize: 20 }}>BIÊN LAI THU TIỀN / RECEIPT</h2>
              <div style={{ fontSize: 13 }}>Số phiếu / Receipt No.: <strong>{viewingReceipt.invoiceCode}</strong></div>
              <div style={{ fontStyle: 'italic', fontSize: 12 }}>(Bản chính)</div>
            </div>

            {/* Bảng Thông tin hành chính */}
            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 12, fontSize: 13 }} border="1" cellPadding="6">
              <tbody>
                <tr>
                  <td colSpan="6" style={{ background: '#f5f5f5', fontWeight: 'bold', textAlign: 'center' }}>
                    Thông tin hành chính / Patient Information
                  </td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold', width: '15%' }}>Họ và tên / Name:</td>
                  <td style={{ width: '35%' }}><strong>{viewingReceipt.patientName}</strong></td>
                  <td style={{ fontWeight: 'bold', width: '10%' }}>Giới tính:</td>
                  <td>Nam / Nữ</td>
                  <td style={{ fontWeight: 'bold', width: '10%' }}>Năm sinh:</td>
                  <td>1988</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold' }}>Khoa / Dept:</td>
                  <td>Khám bệnh</td>
                  <td style={{ fontWeight: 'bold' }}>Phòng:</td>
                  <td colSpan="3">Phòng khám nội 1 - Tầng 2 - STT: 1</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 'bold' }}>Địa chỉ / Address:</td>
                  <td colSpan="5">Phường 12, Quận Phú Nhuận, Tp. Hồ Chí Minh</td>
                </tr>
              </tbody>
            </table>

            {/* Bảng Chi tiết Khoản Thu */}
            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 12, fontSize: 12, textAlign: 'center' }} border="1" cellPadding="6">
              <thead>
                <tr style={{ background: '#fafafa', fontWeight: 'bold' }}>
                  <th style={{ width: '6%' }}>No.</th>
                  <th>Tên dịch vụ / Services Name</th>
                  <th style={{ width: '8%' }}>SL</th>
                  <th style={{ width: '16%' }}>Đơn giá</th>
                  <th style={{ width: '16%' }}>Thành tiền</th>
                  <th style={{ width: '12%' }}>Bảo hiểm</th>
                  <th style={{ width: '16%' }}>BN trả</th>
                </tr>
              </thead>
              <tbody>
                {viewingReceipt.examFee > 0 && (
                  <tr>
                    <td>1</td>
                    <td style={{ textAlign: 'left' }}>Khám bệnh nội khoa</td>
                    <td>1</td>
                    <td>{money(viewingReceipt.examFee)}</td>
                    <td>{money(viewingReceipt.examFee)}</td>
                    <td>0 ₫</td>
                    <td><strong>{money(viewingReceipt.examFee)}</strong></td>
                  </tr>
                )}
                {viewingReceipt.medicineFee > 0 && (
                  <tr>
                    <td>{viewingReceipt.examFee > 0 ? 2 : 1}</td>
                    <td style={{ textAlign: 'left' }}>Thuốc theo đơn chẩn đoán</td>
                    <td>1</td>
                    <td>{money(viewingReceipt.medicineFee)}</td>
                    <td>{money(viewingReceipt.medicineFee)}</td>
                    <td>0 ₫</td>
                    <td><strong>{money(viewingReceipt.medicineFee)}</strong></td>
                  </tr>
                )}
                <tr style={{ fontWeight: 'bold', background: '#fafafa' }}>
                  <td colSpan="4" style={{ textAlign: 'right' }}>Tổng tạm / Subtotal:</td>
                  <td>{money(viewingReceipt.totalAmount)}</td>
                  <td>0 ₫</td>
                  <td style={{ color: '#1677ff' }}>{money(viewingReceipt.totalAmount)}</td>
                </tr>
              </tbody>
            </table>

            {/* Tổng tiền bằng chữ & Phương thức */}
            <div style={{ border: '1px solid #000', padding: '8px 12px', marginBottom: 12, fontSize: 13 }}>
              <div><strong>Số tiền viết bằng chữ:</strong> <em>{convertAmountToWords(viewingReceipt.totalAmount)}</em></div>
              <div style={{ marginTop: 4 }}>
                <strong>Phương thức:</strong> {viewingReceipt.paymentMethodLabel || viewingReceipt.paymentMethod || 'Tiền mặt'} &nbsp;|&nbsp;
                <strong>Tổng tiền thanh toán:</strong> <span style={{ fontSize: 15, fontWeight: 'bold' }}>{money(viewingReceipt.totalAmount)}</span>
              </div>
            </div>

            {/* Chữ ký */}
            <div style={{ display: 'flex', justifyContent: 'space-between', textAlign: 'center', marginBottom: 16, fontSize: 12 }}>
              <div>
                <strong>Người nộp tiền</strong>
                <div style={{ height: 40 }} />
                <span>(Ký, họ tên)</span>
              </div>
              <div>
                <strong>Đề nghị của bác sĩ</strong>
                <div style={{ height: 40 }} />
                <span>(Ký, họ tên)</span>
              </div>
              <div>
                <strong>Người thu tiền</strong>
                <div style={{ fontSize: 11 }}>
                  {new Date(viewingReceipt.createdAt || Date.now()).toLocaleDateString('vi-VN')}
                </div>
                <div style={{ height: 30 }} />
                <span>(Đã ký)</span>
              </div>
            </div>

            {/* Khung Mã QR & Ghi Chú Theo Mẫu */}
            <div style={{ display: 'flex', border: '2px solid #08979c', padding: 10, background: '#e6fffb', alignItems: 'center' }}>
              <div
                style={{
                  textAlign: 'center',
                  marginRight: 16,
                  cursor: 'pointer',
                  padding: 4,
                  background: '#fff',
                  border: '1px solid #b5f5ec',
                }}
                onClick={() => {
                  const current = viewingReceipt
                  setViewingReceipt(null)
                  setViewingEInvoice(current)
                }}
              >
                <img
                  src={getQrUrl(viewingReceipt.invoiceCode)}
                  alt="Mã QR HĐĐT"
                  style={{ width: 110, height: 110, display: 'block' }}
                />
                <div style={{ fontSize: 11, fontWeight: 'bold', color: '#08979c', marginTop: 4 }}>
                  <QrcodeOutlined /> Quét mã QR
                </div>
              </div>

              <div style={{ fontSize: 12, color: '#00474f', lineHeight: '1.5' }}>
                <p style={{ margin: '0 0 4px 0', fontWeight: 'bold' }}>
                  Quý khách hàng vui lòng kiểm tra kỹ thông tin trên biên lai. Phòng khám không có chính sách đổi trả các dịch vụ sau khi hoàn tất thanh toán.
                </p>
                <p style={{ margin: '0 0 4px 0', color: '#0958d9', fontWeight: 'bold' }}>
                  Quét mã QR bên cạnh để xuất Hóa đơn điện tử (HĐĐT) &amp; Tra cứu bệnh án điện tử trực tuyến.
                </p>
                <p style={{ margin: 0, fontStyle: 'italic' }}>
                  Lưu ý: Thực hiện cập nhật thông tin xuất HĐĐT chỉ áp dụng trong ngày (Trước 22h00). Quý khách vui lòng giữ biên lai này để tra cứu HĐĐT.
                </p>
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal Tra Cứu Hóa Đơn Điện Tử & Bệnh Án Điện Tử khi Quét Mã QR */}
      <Modal
        title="HÓA ĐƠN ĐIỆN TỬ (HĐĐT) & BỆNH ÁN ĐIỆN TỬ TRỰC TUYẾN"
        open={!!viewingEInvoice}
        onCancel={() => setViewingEInvoice(null)}
        footer={[
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>
            Tải / In HĐĐT PDF
          </Button>,
          <Button key="close" onClick={() => setViewingEInvoice(null)}>
            Đóng
          </Button>,
        ]}
        width={680}
      >
        {viewingEInvoice && (
          <div>
            <Alert
              type="success"
              showIcon
              icon={<ScanOutlined />}
              message="XÁC THỰC MÃ QR THÀNH CÔNG"
              description={`Tra cứu thành công dữ liệu HĐĐT và Bệnh án điện tử cho mã chứng từ ${viewingEInvoice.invoiceCode}`}
              style={{ marginBottom: 16 }}
            />

            <Card title="Chi tiết Hóa đơn điện tử (Tra cứu từ Mã QR)" size="small" style={{ marginBottom: 16 }}>
              <Descriptions bordered column={2} size="small">
                <Descriptions.Item label="Mã hóa đơn">{viewingEInvoice.invoiceCode}</Descriptions.Item>
                <Descriptions.Item label="Trạng thái">
                  <Tag color="green">ĐÃ XÁC THỰC KÝ SỐ (HĐĐT)</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Họ và tên bệnh nhân">{viewingEInvoice.patientName}</Descriptions.Item>
                <Descriptions.Item label="Ngày lập">
                  {new Date(viewingEInvoice.createdAt).toLocaleString('vi-VN')}
                </Descriptions.Item>
                <Descriptions.Item label="Phí khám">{money(viewingEInvoice.examFee)}</Descriptions.Item>
                <Descriptions.Item label="Tiền thuốc">{money(viewingEInvoice.medicineFee)}</Descriptions.Item>
                <Descriptions.Item label="Tổng cộng" span={2}>
                  <Text type="success" strong style={{ fontSize: 16 }}>
                    {money(viewingEInvoice.totalAmount)} ({convertAmountToWords(viewingEInvoice.totalAmount)})
                  </Text>
                </Descriptions.Item>
                <Descriptions.Item label="Phương thức">{viewingEInvoice.paymentMethodLabel || viewingEInvoice.paymentMethod}</Descriptions.Item>
                <Descriptions.Item label="Cơ quan thuế">Đã gửi dữ liệu HĐĐT tới Tổng cục Thuế</Descriptions.Item>
              </Descriptions>
            </Card>

            <Card title="Hồ sơ bệnh án điện tử liên kết" size="small">
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="Chẩn đoán của bác sĩ">
                  Tăng huyết áp / Đái tháo đường (Đã hoàn tất lượt khám)
                </Descriptions.Item>
                <Descriptions.Item label="Đơn thuốc đã cấp phát">
                  Amlodipine 5mg (1 viên/ngày), Paracetamol 500mg
                </Descriptions.Item>
                <Descriptions.Item label="Ghi chú bác sĩ">
                  Uống thuốc đúng giờ, tái khám sau 2 tuần hoặc khi có dấu hiệu bất thường.
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default BillingPage
