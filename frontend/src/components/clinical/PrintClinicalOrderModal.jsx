import React from 'react'
import { Modal, Button, Typography, Divider, Row, Col } from 'antd'
import { PrinterOutlined } from '@ant-design/icons'
import { formatGender } from '../../utils/helpers'

const { Title, Text, Paragraph } = Typography

export const PrintClinicalOrderModal = ({ visible, order, onClose }) => {
  if (!order) return null

  const handlePrint = () => {
    window.print()
  }

  return (
    <Modal
      open={visible}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>Đóng</Button>,
        <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={handlePrint}>In phiếu</Button>,
      ]}
      width={750}
      title="Xem trước bản in phiếu chỉ định"
    >
      <div id="printable-clinical-order" style={{ padding: 24, background: '#fff', color: '#000', fontFamily: 'Times New Roman, serif' }}>
        {/* Header */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={14}>
            <div style={{ fontWeight: 'bold', fontSize: 14 }}>BỆNH VIỆN / PHÒNG KHÁM ĐA KHOA BỆNH ÁN SỐ</div>
            <div style={{ fontSize: 12 }}>Địa chỉ: 123 Đường Y Tế, Phường 1, TP. Hồ Chí Minh</div>
            <div style={{ fontSize: 12 }}>Điện thoại: (028) 3800 9999</div>
          </Col>
          <Col span={10} style={{ textAlign: 'right' }}>
            <div style={{ fontWeight: 'bold', fontSize: 14 }}>MÃ PHIẾU: {order.orderCode}</div>
            <div style={{ fontSize: 12 }}>Độ ưu tiên: {order.priority === 'URGENT' ? 'KHẨN CẤP' : 'THƯỜNG'}</div>
            <div style={{ fontSize: 12 }}>Ngày chỉ định: {new Date(order.createdAt).toLocaleDateString('vi-VN')}</div>
          </Col>
        </Row>

        <Divider style={{ margin: '10px 0' }} />

        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <Title level={3} style={{ margin: 0, textTransform: 'uppercase', fontFamily: 'inherit' }}>
            PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG
          </Title>
        </div>

        {/* Patient Demographics */}
        <div style={{ fontSize: 14, lineHeight: '1.8' }}>
          <Row>
            <Col span={16}>Họ và tên bệnh nhân: <b>{order.patientName?.toUpperCase()}</b></Col>
            <Col span={4}>Tuổi: <b>{order.age}</b></Col>
            <Col span={4}>Giới tính: <b>{formatGender(order.gender)}</b></Col>
          </Row>
          <Row>
            <Col span={12}>Mã số bệnh nhân: <b>{order.patientCode}</b></Col>
            <Col span={12}>Số điện thoại: {order.phone || '-'}</Col>
          </Row>
          <Row>
            <Col span={24}>Khoa chỉ định: {order.department}</Col>
          </Row>
          <Row>
            <Col span={24}>Chẩn đoán lâm sàng: <b>{order.diagnosis}</b></Col>
          </Row>
        </div>

        {/* Services Table */}
        <table style={{ width: '100%', marginTop: 16, marginBottom: 16, borderCollapse: 'collapse', border: '1px solid #000' }}>
          <thead>
            <tr style={{ background: '#f0f0f0', textAlign: 'center' }}>
              <th style={{ border: '1px solid #000', padding: 8, width: 40 }}>STT</th>
              <th style={{ border: '1px solid #000', padding: 8, width: 100 }}>Mã Dịch vụ</th>
              <th style={{ border: '1px solid #000', padding: 8 }}>Tên dịch vụ chỉ định</th>
              <th style={{ border: '1px solid #000', padding: 8, width: 120 }}>Ghi chú / Vị trí</th>
              <th style={{ border: '1px solid #000', padding: 8, width: 100 }}>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {order.items?.map((item, idx) => (
              <tr key={idx}>
                <td style={{ border: '1px solid #000', padding: 8, textAlign: 'center' }}>{idx + 1}</td>
                <td style={{ border: '1px solid #000', padding: 8, textAlign: 'center' }}>{item.serviceCode}</td>
                <td style={{ border: '1px solid #000', padding: 8 }}>{item.serviceName}</td>
                <td style={{ border: '1px solid #000', padding: 8, fontSize: 12 }}>{item.note || '-'}</td>
                <td style={{ border: '1px solid #000', padding: 8, textAlign: 'right' }}>
                  {item.price == null ? 'Chưa cập nhật' : `${Number(item.price).toLocaleString('vi-VN')} đ`}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={4} style={{ border: '1px solid #000', padding: 8, textAlign: 'right', fontWeight: 'bold' }}>
                Tổng cộng chi phí:
              </td>
              <td style={{ border: '1px solid #000', padding: 8, textAlign: 'right', fontWeight: 'bold' }}>
                {order.totalAmount == null ? 'Chưa cập nhật' : `${Number(order.totalAmount).toLocaleString('vi-VN')} đ`}
              </td>
            </tr>
          </tfoot>
        </table>

        {/* Signatures */}
        <Row gutter={16} style={{ marginTop: 30, textAlign: 'center' }}>
          <Col span={12}>
            <div style={{ fontWeight: 'bold' }}>BỆNH NHÂN / NGƯỜI NHÀ</div>
            <div style={{ fontSize: 11, fontStyle: 'italic' }}>(Ký và ghi rõ họ tên)</div>
          </Col>
          <Col span={12}>
            <div style={{ fontSize: 12, fontStyle: 'italic' }}>
              Ngày {new Date(order.createdAt).getDate()} tháng {new Date(order.createdAt).getMonth() + 1} năm {new Date(order.createdAt).getFullYear()}
            </div>
            <div style={{ fontWeight: 'bold' }}>BÁC SĨ CHỈ ĐỊNH</div>
            <div style={{ fontSize: 11, fontStyle: 'italic' }}>(Ký và ghi rõ họ tên)</div>
            <div style={{ marginTop: 50, fontWeight: 'bold' }}>{order.doctorName}</div>
          </Col>
        </Row>
      </div>
    </Modal>
  )
}

export default PrintClinicalOrderModal
