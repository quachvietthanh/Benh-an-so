import React from 'react'
import { Modal, Button, Table, Typography, Tag, Divider, Space } from 'antd'
import { PrinterOutlined, DownloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { formatCurrency } from '../../utils/clinicalCatalogData'

const { Title, Text, Paragraph } = Typography

function ClinicalOrderPrintModal({ open, onClose, patient, recordCode, diagnosis, primaryIcd, secondaryIcds = [], orders = [], doctorName, vitalSigns }) {
  if (!open) return null

  const handlePrint = () => {
    window.print()
  }

  const columns = [
    { title: 'STT', key: 'stt', width: 50, render: (_, __, index) => index + 1 },
    {
      title: 'Dịch vụ chỉ định',
      key: 'name',
      render: (r) => (
        <div>
          <Text strong>{r.name || r.code}</Text>
          {r.preparation && <div style={{ fontSize: 12, color: '#666' }}>HD: {r.preparation}</div>}
        </div>
      ),
    },
    { title: 'Nơi thực hiện', dataIndex: 'department', width: 140, render: (val) => val || 'Phòng Chức năng' },
    {
      title: 'Độ ưu tiên',
      dataIndex: 'isUrgent',
      width: 100,
      render: (val) => (val ? <Tag color="red">CẤP CỨU</Tag> : <Tag color="blue">Thường</Tag>),
    },
    { title: 'Ghi chú chỉ định', dataIndex: 'note', width: 150, render: (val) => val || '---' },
    { title: 'Đơn giá', dataIndex: 'price', width: 110, align: 'right', render: (val) => formatCurrency(val) },
  ]

  const totalFee = orders.reduce((sum, item) => sum + (Number(item.price) || 0), 0)

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={780}
      title={
        <Space>
          <PrinterOutlined /> <span>Xem trước & In Phiếu Chỉ Định Cận Lâm Sàng</span>
        </Space>
      }
      footer={[
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
        <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={handlePrint}>
          In Phiếu chỉ định
        </Button>,
      ]}
    >
      <div id="printable-order-sheet" style={{ padding: '16px', background: '#fff', color: '#111827' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '2px solid #2563EB', pb: 12, marginBottom: 16 }}>
          <div>
            <Title level={4} style={{ margin: 0, color: '#1E3A8A' }}>
              BỆNH VIỆN ĐA KHOA BỆNH ÁN SỐ
            </Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Địa chỉ: 123 Đường Y Học, Quận 1, TP. Hồ Chí Minh - Hotline: 1900 8888
            </Text>
          </div>
          <div style={{ textAlign: 'right' }}>
            <Text strong style={{ fontSize: 13, color: '#2563EB' }}>
              MÃ BA: {recordCode || 'BA-20260730'}
            </Text>
            <div style={{ fontSize: 12, color: '#666' }}>Ngày lập: {dayjs().format('HH:mm DD/MM/YYYY')}</div>
          </div>
        </div>

        <div style={{ textAlign: 'center', marginBottom: 16 }}>
          <Title level={3} style={{ margin: 0, color: '#1E293B', textTransform: 'uppercase' }}>
            PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG
          </Title>
        </div>

        {/* Patient Details */}
        <div style={{ background: '#F8FAFC', padding: 12, borderRadius: 8, marginBottom: 16, border: '1px solid #E2E8F0' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px 16px', fontSize: 13 }}>
            <div>
              <Text type="secondary">Họ tên bệnh nhân: </Text>
              <Text strong>{patient?.fullName || '---'}</Text>
            </div>
            <div>
              <Text type="secondary">Mã BN: </Text>
              <Text strong>{patient?.patientCode || '---'}</Text>
            </div>
            <div>
              <Text type="secondary">Giới tính / Tuổi: </Text>
              <Text strong>
                {patient?.gender === 'MALE' ? 'Nam' : patient?.gender === 'FEMALE' ? 'Nữ' : 'Khác'} -{' '}
                {patient?.dateOfBirth ? `${dayjs().diff(dayjs(patient.dateOfBirth), 'year')} tuổi` : '---'}
              </Text>
            </div>
            <div>
              <Text type="secondary">Số BHYT: </Text>
              <Text strong>{patient?.healthInsuranceCode || 'Không có'}</Text>
            </div>
            <div>
              <Text type="secondary">Điện thoại: </Text>
              <Text>{patient?.phoneNumber || '---'}</Text>
            </div>
            <div>
              <Text type="secondary">Địa chỉ: </Text>
              <Text>{patient?.address || '---'}</Text>
            </div>
          </div>

          {vitalSigns && (vitalSigns.bp || vitalSigns.pulse || vitalSigns.temp) && (
            <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px dashed #CBD5E1', fontSize: 12, color: '#334155' }}>
              <Text strong>Sinh hiệu: </Text>
              {vitalSigns.bp && <span>Huyết áp: <b>{vitalSigns.bp}</b> mmHg | </span>}
              {vitalSigns.pulse && <span>Mạch: <b>{vitalSigns.pulse}</b> l/p | </span>}
              {vitalSigns.temp && <span>Nhiệt độ: <b>{vitalSigns.temp}</b> °C | </span>}
              {vitalSigns.spO2 && <span>SpO2: <b>{vitalSigns.spO2}</b> %</span>}
            </div>
          )}
        </div>

        {/* Diagnosis */}
        <div style={{ marginBottom: 16, fontSize: 13 }}>
          <div style={{ marginBottom: 4 }}>
            <Text type="secondary">Chẩn đoán chính: </Text>
            <Text strong color="#1E40AF">
              {primaryIcd ? `[${primaryIcd.code}] ${primaryIcd.name}` : diagnosis || 'Chưa cập nhật'}
            </Text>
          </div>
          {secondaryIcds && secondaryIcds.length > 0 && (
            <div>
              <Text type="secondary">Chẩn đoán kèm theo: </Text>
              <Text style={{ color: '#475569' }}>
                {secondaryIcds.map((item) => `[${item.code}] ${item.name}`).join('; ')}
              </Text>
            </div>
          )}
        </div>

        {/* Orders Table */}
        <Table
          size="small"
          columns={columns}
          dataSource={orders}
          pagination={false}
          rowKey={(r, index) => r.code || index}
          bordered
          style={{ marginBottom: 16 }}
        />

        {/* Summary & Footer Signatures */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginTop: 16 }}>
          <div>
            <Text strong style={{ fontSize: 14 }}>
              Tổng tiền chỉ định: <span style={{ color: '#DC2626' }}>{formatCurrency(totalFee)}</span>
            </Text>
            <Paragraph style={{ fontSize: 12, color: '#64748B', marginTop: 4 }}>
              * Bệnh nhân mang phiếu chỉ định này đến các phòng cận lâm sàng ghi trong danh mục để thực hiện.
            </Paragraph>
          </div>

          <div style={{ textAlign: 'center', minWidth: 200 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Ngày {dayjs().format('DD')} tháng {dayjs().format('MM')} năm {dayjs().format('YYYY')}
            </Text>
            <div style={{ fontWeight: 600, marginTop: 4, marginBottom: 40 }}>BÁC SĨ CHỈ ĐỊNH</div>
            <Text strong style={{ color: '#1E3A8A' }}>
              {doctorName || 'BS. Phạm Hồng Anh'}
            </Text>
          </div>
        </div>
      </div>
    </Modal>
  )
}

export default ClinicalOrderPrintModal
