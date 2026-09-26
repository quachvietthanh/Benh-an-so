import React, { useEffect, useMemo, useState } from 'react'
import { Button, Input, Modal, Space, Table, Tag, Typography, message } from 'antd'
import { PlusOutlined, SearchOutlined, ShopOutlined } from '@ant-design/icons'
import medicineApi from '../../api/medicineApi.js'

const { Text } = Typography

export default function AddMedicineToProcurementModal({
  open,
  onClose,
  onAddMedicines,
  existingMedicineIds = [],
}) {
  const [medicines, setMedicines] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [selectedRows, setSelectedRows] = useState([])

  useEffect(() => {
    if (!open) {
      setSelectedRowKeys([])
      setSelectedRows([])
      setSearchKeyword('')
      return
    }

    let isMounted = true
    const fetchMedicines = async () => {
      setLoading(true)
      try {
        const res = await medicineApi.search({ active: true, size: 200 })
        const list = Array.isArray(res.data)
          ? res.data
          : Array.isArray(res.data?.content)
          ? res.data.content
          : []
        if (isMounted) {
          setMedicines(list)
        }
      } catch (err) {
        if (isMounted) {
          message.error('Không thể tải danh mục thuốc. Vui lòng thử lại sau.')
        }
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    fetchMedicines()

    return () => {
      isMounted = false
    }
  }, [open])

  // Lọc theo từ khóa tìm kiếm và loại bỏ các thuốc đã có trong phiếu
  const filteredMedicines = useMemo(() => {
    const existingSet = new Set(existingMedicineIds.map(String))
    let list = medicines.filter((m) => !existingSet.has(String(m.id || m.medicineId)))

    if (searchKeyword.trim()) {
      const kw = searchKeyword.trim().toLowerCase()
      list = list.filter((m) => {
        const name = (m.name || m.medicineName || '').toLowerCase()
        const code = (m.code || m.medicineCode || '').toLowerCase()
        return name.includes(kw) || code.includes(kw)
      })
    }
    return list
  }, [medicines, existingMedicineIds, searchKeyword])

  const handleConfirmAdd = () => {
    if (selectedRows.length === 0) {
      message.warning('Vui lòng chọn ít nhất một loại thuốc để thêm vào phiếu.')
      return
    }

    // Chuyển đổi thành định dạng item cho phiếu dự trù
    const formattedItems = selectedRows.map((m) => {
      const mId = m.id || m.medicineId
      const currentStock = Number(m.currentStock ?? m.stock ?? 0)
      const minStock = Number(m.minStockThreshold ?? m.minStock ?? 0)
      const prevConsumption = m.previousPeriodConsumption != null ? Number(m.previousPeriodConsumption) : null

      return {
        medicineId: mId,
        medicineCode: m.code || m.medicineCode || 'THUOC',
        medicineName: m.name || m.medicineName,
        unit: m.unit || 'Viên',
        currentStock,
        eligibleStock: currentStock,
        minStockThreshold: minStock,
        previousPeriodConsumption: prevConsumption,
        suggestedQuantity: null, // Thuốc thêm thủ công: để dược sĩ tự quyết định số lượng
        proposedQuantity: 10, // Mặc định số khởi tạo hợp lý
        note: 'Thêm chủ động vào phiếu',
        isManualAdded: true,
      }
    })

    onAddMedicines(formattedItems)
    message.success(`Đã thêm ${formattedItems.length} loại thuốc vào phiếu dự trù.`)
    onClose()
  }

  const columns = [
    {
      title: 'Mã thuốc',
      dataIndex: 'code',
      key: 'code',
      width: 120,
      render: (code, record) => (
        <span style={{ fontFamily: 'monospace', fontWeight: 600, color: '#2563eb' }}>
          {code || record.medicineCode || '—'}
        </span>
      ),
    },
    {
      title: 'Tên thuốc & Quy cách',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <div>
          <strong style={{ color: '#0f172a' }}>{name || record.medicineName}</strong>
          {record.activeIngredient && (
            <div style={{ fontSize: 12, color: '#64748b' }}>
              Hoạt chất: {record.activeIngredient}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Đơn vị',
      dataIndex: 'unit',
      key: 'unit',
      width: 90,
      render: (unit) => <Tag color="blue">{unit || 'Viên'}</Tag>,
    },
    {
      title: 'Tồn hiện tại',
      key: 'stock',
      width: 110,
      align: 'right',
      render: (_, record) => {
        const stock = record.currentStock ?? record.stock ?? 0
        return <span>{stock}</span>
      },
    },
    {
      title: 'Tồn tối thiểu',
      key: 'minStock',
      width: 110,
      align: 'right',
      render: (_, record) => {
        const minStock = record.minStockThreshold ?? record.minStock ?? 0
        return <span style={{ color: '#64748b' }}>{minStock}</span>
      },
    },
  ]

  const rowSelection = {
    selectedRowKeys,
    onChange: (keys, rows) => {
      setSelectedRowKeys(keys)
      setSelectedRows(rows)
    },
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      width={760}
      centered
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: '#eff6ff',
              color: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 18,
            }}
          >
            <ShopOutlined />
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
              Thêm thuốc khác vào phiếu dự trù
            </div>
            <div style={{ fontSize: 12.5, color: '#64748b' }}>
              Chủ động chọn thêm các thuốc chưa chạm ngưỡng tồn tối thiểu để dự phòng
            </div>
          </div>
        </div>
      }
      footer={[
        <Button key="cancel" onClick={onClose} style={{ borderRadius: 8 }}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleConfirmAdd}
          disabled={selectedRowKeys.length === 0}
          style={{ background: '#2563eb', borderColor: '#2563eb', borderRadius: 8, fontWeight: 600 }}
        >
          Thêm vào phiếu ({selectedRowKeys.length})
        </Button>,
      ]}
    >
      <div style={{ margin: '14px 0' }}>
        <Input
          prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
          placeholder="Tìm thuốc theo tên hoặc mã thuốc..."
          allowClear
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          style={{ borderRadius: 8, marginBottom: 14 }}
        />

        <Table
          rowKey={(r) => String(r.id || r.medicineId)}
          rowSelection={rowSelection}
          columns={columns}
          dataSource={filteredMedicines}
          loading={loading}
          size="small"
          pagination={{ pageSize: 8, showTotal: (t) => `Tổng ${t} thuốc khả dụng` }}
          locale={{ emptyText: 'Không tìm thấy thuốc khả dụng để thêm' }}
        />
      </div>
    </Modal>
  )
}
