import React, { useCallback, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Form, Input, InputNumber, List, message, Modal, Select, Space, Table, Tag } from 'antd'
import { DeleteOutlined, PlusOutlined, WarningOutlined, EditOutlined, MedicineBoxOutlined } from '@ant-design/icons'
import medicalRecordApi from '../api/medicalRecordApi'
import pharmacyApi from '../api/pharmacyApi'
import { drugInteractions } from '../mock-data/mockData'
import { mergeMedicalRecords, mergeMedicines, mergePrescriptions, saveStoredPrescription } from '../utils/storageHelpers'
import dayjs from 'dayjs'

const emptyItem = () => ({ medicineId: undefined, quantity: 1, dosage: '' })
const parseJson = (value, fallback = []) => {
  try { return typeof value === 'string' ? JSON.parse(value) : (value || fallback) }
  catch { return fallback }
}

function PrescriptionPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [medicines, setMedicines] = useState([])
  const [records, setRecords] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [recordId, setRecordId] = useState()
  const [items, setItems] = useState([emptyItem()])
  const [warnings, setWarnings] = useState([])
  const [overrideReason, setOverrideReason] = useState('')
  const [editing, setEditing] = useState(null)
  const [changeReason, setChangeReason] = useState('')
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    try {
      const [medicineRes, recordRes, prescriptionRes] = await Promise.allSettled([
        pharmacyApi.medicines(),
        medicalRecordApi.getAll(),
        pharmacyApi.prescriptions(),
      ])
      const apiMeds = medicineRes.status === 'fulfilled' ? (medicineRes.value.data || []) : []
      const apiRecords = recordRes.status === 'fulfilled' ? (recordRes.value.data || []) : []
      const apiPrescs = prescriptionRes.status === 'fulfilled' ? (prescriptionRes.value.data || []) : []

      setMedicines(mergeMedicines(apiMeds))
      setRecords(mergeMedicalRecords(apiRecords))
      setPrescriptions(mergePrescriptions(apiPrescs))
    } catch {
      setMedicines(mergeMedicines([]))
      setRecords(mergeMedicalRecords([]))
      setPrescriptions(mergePrescriptions([]))
    }
  }, [])

  useEffect(() => { load() }, [load])

  const checkInteractions = useCallback(async (nextItems) => {
    const ids = [...new Set(nextItems.map((item) => item.medicineId).filter(Boolean))]
    if (ids.length < 2) {
      setWarnings([])
      return
    }

    try {
      const response = await pharmacyApi.interactions(ids)
      const resData = response.data || []
      if (resData.length) {
        setWarnings(resData)
      } else {
        const foundWarnings = drugInteractions.filter((interaction) => (
          interaction.drugs.every((drugId) => ids.includes(drugId))
        ))
        setWarnings(foundWarnings)
      }
    } catch {
      const foundWarnings = drugInteractions.filter((interaction) => (
        interaction.drugs.every((drugId) => ids.includes(drugId))
      ))
      setWarnings(foundWarnings)
    }
  }, [])

  const updateItem = (index, field, value) => {
    const next = items.map((item, i) => (i === index ? { ...item, [field]: value } : item))
    setItems(next)
    if (field === 'medicineId') checkInteractions(next)
  }

  const validate = () => {
    if (!recordId && !editing) return 'Vui lòng chọn bệnh án đã có chẩn đoán'
    if (!items.length || items.some((item) => !item.medicineId || !item.quantity || !item.dosage?.trim())) {
      return 'Mỗi thuốc phải chọn tên thuốc, số lượng và liều dùng'
    }
    if (new Set(items.map((item) => item.medicineId)).size !== items.length) {
      return 'Không được chọn trùng thuốc trong cùng một đơn'
    }
    if (warnings.length && !overrideReason.trim()) {
      return 'Phát hiện tương tác thuốc! Vui lòng nhập lý do chuyên môn để xác nhận tiếp tục'
    }
    if (editing && !changeReason.trim()) {
      return 'Vui lòng nhập lý do điều chỉnh đơn thuốc'
    }
    return null
  }

  const save = async () => {
    const error = validate()
    if (error) {
      message.error(error)
      return
    }

    setSaving(true)
    const selectedRecord = records.find((r) => r.id === recordId)

    try {
      if (editing) {
        await pharmacyApi.updatePrescription(editing.id, { items, changeReason, overrideReason })
        saveStoredPrescription({ ...editing, items: JSON.stringify(items), overrideReason, changeReason, updatedAt: new Date().toISOString() })
        message.success('Đã cập nhật đơn thuốc và lưu vết thay đổi')
      } else {
        const res = await pharmacyApi.createPrescription({ medicalRecordId: recordId, items, overrideReason })
        if (res?.data) saveStoredPrescription(res.data)
        message.success('Đơn thuốc đã tạo thành công ở trạng thái chờ cấp phát')
      }
      setEditing(null)
      setRecordId(undefined)
      setItems([emptyItem()])
      setWarnings([])
      setOverrideReason('')
      setChangeReason('')
      await load()

      Modal.confirm({
        title: 'Đã kê đơn thuốc thành công!',
        content: 'Bạn có muốn CHUYỂN SANG BƯỚC TIẾP THEO (Thu phí & Hóa đơn) cho bệnh nhân này không?',
        okText: 'Chuyển sang Thu phí',
        cancelText: 'Ở lại màn hình Kê đơn',
        onOk: () => navigate('/billing', { state: { patientId: selectedRecord?.patientId } }),
      })
    } catch {
      // Fallback saving for frontend persistence
      if (editing) {
        const updated = { ...editing, items: JSON.stringify(items), overrideReason, changeReason, updatedAt: new Date().toISOString() }
        saveStoredPrescription(updated)
        setPrescriptions(mergePrescriptions([]))
        message.success('Đã cập nhật đơn thuốc và lưu vết thay đổi')
      } else {
        const newPrescription = {
          id: `presc-${Date.now()}`,
          prescriptionCode: `DT-${dayjs().format('YYYYMMDDHHmmss')}`,
          medicalRecordId: recordId,
          patientName: selectedRecord?.patientName || 'Bệnh nhân',
          status: 'PENDING_DISPENSING',
          items: JSON.stringify(items),
          overrideReason,
          createdAt: new Date().toISOString(),
        }
        saveStoredPrescription(newPrescription)
        setPrescriptions(mergePrescriptions([]))
        message.success(`Đơn thuốc ${newPrescription.prescriptionCode} đã tạo ở trạng thái chờ cấp phát`)

        Modal.confirm({
          title: 'Đã kê đơn thuốc thành công!',
          content: 'Bạn có muốn CHUYỂN SANG BƯỚC TIẾP THEO (Thu phí & Hóa đơn) cho bệnh nhân này không?',
          okText: 'Chuyển sang Thu phí',
          cancelText: 'Ở lại màn hình Kê đơn',
          onOk: () => navigate('/billing', { state: { patientId: selectedRecord?.patientId } }),
        })
      }
      setEditing(null)
      setRecordId(undefined)
      setItems([emptyItem()])
      setWarnings([])
      setOverrideReason('')
      setChangeReason('')
    } finally {
      setSaving(false)
    }
  }

  const beginEdit = (prescription) => {
    const nextItems = parseJson(prescription.items)
    setEditing(prescription)
    setItems(nextItems.length ? nextItems : [emptyItem()])
    setOverrideReason(prescription.overrideReason || '')
    setChangeReason('')
    checkInteractions(nextItems)
  }

  const cancelEdit = () => {
    setEditing(null)
    setRecordId(undefined)
    setItems([emptyItem()])
    setWarnings([])
    setOverrideReason('')
    setChangeReason('')
  }

  return (
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>
          <MedicineBoxOutlined /> Kê đơn thuốc và cảnh báo tương tác
        </h2>
        <Space>
          {editing && <Button onClick={cancelEdit}>Hủy bỏ điều chỉnh</Button>}
          <Button type="primary" loading={saving} onClick={save}>
            {editing ? 'Lưu điều chỉnh đơn thuốc' : 'Tạo đơn thuốc'}
          </Button>
        </Space>
      </div>

      <Card title={editing ? `Điều chỉnh đơn thuốc ${editing.prescriptionCode}` : 'Kê đơn thuốc mới'} style={{ marginBottom: 20 }}>
        {!editing && (
          <Form.Item label="Bệnh án đã có chẩn đoán" required>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Chọn bệnh án cần kê đơn thuốc"
              value={recordId}
              onChange={setRecordId}
              options={records.map((r) => ({
                value: r.id,
                label: `${r.recordCode} — ${r.patientName} (Chẩn đoán: ${r.diagnosis || 'Chưa rõ'})`,
              }))}
            />
          </Form.Item>
        )}

        <div style={{ marginBottom: 12 }}>
          <strong>Danh sách thuốc trong đơn:</strong>
        </div>

        {items.map((item, index) => (
          <Space key={index} style={{ display: 'flex', marginBottom: 10 }} align="start">
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Chọn thuốc từ danh mục"
              style={{ width: 320 }}
              value={item.medicineId}
              onChange={(value) => updateItem(index, 'medicineId', value)}
              options={medicines.map((m) => ({
                value: m.id,
                label: `${m.name} (Tồn kho: ${m.stock} ${m.unit || 'đơn vị'})`,
              }))}
            />
            <InputNumber
              min={1}
              value={item.quantity}
              onChange={(value) => updateItem(index, 'quantity', value)}
              addonBefore="SL"
              style={{ width: 120 }}
            />
            <Input
              placeholder="Liều dùng & cách dùng (VD: 2 viên/ngày chia 2 lần sau ăn)"
              style={{ width: 340 }}
              value={item.dosage}
              onChange={(e) => updateItem(index, 'dosage', e.target.value)}
            />
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={items.length === 1}
              onClick={() => {
                const next = items.filter((_, i) => i !== index)
                setItems(next)
                checkInteractions(next)
              }}
            />
          </Space>
        ))}

        <Button icon={<PlusOutlined />} onClick={() => setItems((current) => [...current, emptyItem()])}>
          Thêm thuốc vào đơn
        </Button>

        {!!warnings.length && (
          <Alert
            style={{ marginTop: 16 }}
            type="error"
            showIcon
            icon={<WarningOutlined />}
            message="CẢNH BÁO TƯƠNG TÁC THUỐC"
            description={
              <List
                size="small"
                dataSource={warnings}
                renderItem={(w) => (
                  <List.Item>
                    <Tag color="red">{w.severity || 'Nghiêm trọng'}</Tag>
                    <span>{w.description}</span>
                  </List.Item>
                )}
              />
            }
          />
        )}

        {!!warnings.length && (
          <Form.Item label="Lý do chuyên môn khi vẫn tiếp tục kê đơn (bắt buộc)" required style={{ marginTop: 12 }}>
            <Input.TextArea
              rows={2}
              placeholder="Nhập lý do bác sĩ quyết định cho dùng kết hợp..."
              value={overrideReason}
              onChange={(e) => setOverrideReason(e.target.value)}
            />
          </Form.Item>
        )}

        {editing && (
          <Form.Item label="Lý do điều chỉnh đơn thuốc (lưu vết thay đổi)" required style={{ marginTop: 12 }}>
            <Input.TextArea
              rows={2}
              placeholder="Nhập lý do điều chỉnh đơn thuốc trước khi cấp phát..."
              value={changeReason}
              onChange={(e) => setChangeReason(e.target.value)}
            />
          </Form.Item>
        )}
      </Card>

      <Card title="Danh sách đơn thuốc đã lập">
        <Table
          rowKey="id"
          dataSource={prescriptions}
          columns={[
            { title: 'Mã đơn thuốc', dataIndex: 'prescriptionCode', render: (val) => <strong>{val}</strong> },
            { title: 'Bệnh nhân', dataIndex: 'patientName', render: (val) => val || '—' },
            {
              title: 'Danh sách thuốc',
              dataIndex: 'items',
              render: (value) => parseJson(value).map((item) => (
                medicines.find((m) => m.id === item.medicineId)?.name || item.medicineId
              )).filter(Boolean).join(', ') || '—',
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              render: (value) => (
                <Tag color={value === 'PENDING_DISPENSING' ? 'orange' : 'green'}>
                  {value === 'PENDING_DISPENSING' ? 'Chờ cấp phát' : 'Đã cấp phát'}
                </Tag>
              ),
            },
            {
              title: 'Thao tác',
              key: 'actions',
              render: (_, row) => (
                <Button
                  icon={<EditOutlined />}
                  disabled={row.status !== 'PENDING_DISPENSING'}
                  onClick={() => beginEdit(row)}
                >
                  Điều chỉnh
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </div>
  )
}

export default PrescriptionPage

