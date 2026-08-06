import React, { useCallback, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Descriptions, Dropdown, Form, Input, InputNumber, List, message, Modal, Select, Space, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, PlusOutlined, WarningOutlined, EditOutlined, MedicineBoxOutlined, EyeOutlined, CheckCircleOutlined, ExclamationCircleOutlined, MoreOutlined } from '@ant-design/icons'
import pharmacyApi from '../api/pharmacyApi'
import { drugInteractions } from '../mock-data/mockData'
import { mergeMedicalRecords, mergeMedicines, mergePrescriptions, saveStoredPrescription } from '../utils/storageHelpers'
import dayjs from 'dayjs'

const { Text } = Typography

const emptyItem = () => ({ medicineId: undefined, quantity: 1, dosage: '' })
const parseJson = (value, fallback = []) => {
  try { return typeof value === 'string' ? JSON.parse(value) : (value || fallback) }
  catch { return fallback }
}

const renderSeverityTag = (severity) => {
  const s = String(severity || '').toUpperCase()
  if (s === 'CONTRAINDICATED' || s.includes('CHỐNG CHỈ ĐỊNH')) {
    return <Tag color="#722ed1" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>⛔ CHỐNG CHỈ ĐỊNH</Tag>
  }
  if (s === 'SEVERE' || s.includes('NGHIÊM TRỌNG') || s.includes('CAO')) {
    return <Tag color="#f5222d" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>🚨 NGHIÊM TRỌNG</Tag>
  }
  if (s === 'MODERATE' || s.includes('VỪA') || s.includes('TRUNG BÌNH')) {
    return <Tag color="#fa8c16" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>⚠️ TRUNG BÌNH</Tag>
  }
  return <Tag color="#1890ff" style={{ fontWeight: 700, padding: '2px 10px', fontSize: 12 }}>ℹ️ MỨC ĐỘ NHẸ</Tag>
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
  const [viewingWarningModal, setViewingWarningModal] = useState(null)

  const load = useCallback(async () => {
    try {
      const [medicineRes, prescriptionRes] = await Promise.allSettled([
        pharmacyApi.medicines(),
        pharmacyApi.prescriptions(),
      ])
      const apiMeds = medicineRes.status === 'fulfilled' ? (medicineRes.value.data || []) : []
      const apiPrescs = prescriptionRes.status === 'fulfilled' ? (prescriptionRes.value.data || []) : []

      setMedicines(mergeMedicines(apiMeds))
      setRecords(mergeMedicalRecords([]))
      setPrescriptions(mergePrescriptions(apiPrescs))
    } catch {
      setMedicines(mergeMedicines([]))
      setRecords(mergeMedicalRecords([]))
      setPrescriptions(mergePrescriptions([]))
    }
  }, [])

  useEffect(() => { load() }, [load])

  const getDrugName = useCallback((drugId) => {
    if (!drugId) return ''
    const found = medicines.find((m) => String(m.id) === String(drugId))
    if (found) return `${found.name || found.medicineName} (${found.activeIngredient || found.category || ''})`
    return drugId
  }, [medicines])

  const checkInteractions = useCallback(async (nextItems) => {
    const ids = [...new Set(nextItems.map((item) => item.medicineId).filter(Boolean))]
    if (ids.length < 2) {
      setWarnings([])
      return
    }

    try {
      const response = await pharmacyApi.checkInteractions(ids)
      const resData = response.data || []
      if (resData.length) {
        setWarnings(resData)
      } else {
        const foundWarnings = drugInteractions
          .filter((interaction) => interaction.drugs.every((drugId) => ids.includes(drugId)))
          .map((w, index) => ({
            ruleId: w.ruleId || `rule-local-${index}`,
            drugIdA: w.drugs[0],
            drugIdB: w.drugs[1],
            severity: w.severity?.includes('Cao') ? 'SEVERE' : 'MODERATE',
            description: w.description,
            clinicalRecommendation: w.recommendation || 'Theo dõi sát diễn biến lâm sàng của bệnh nhân và điều chỉnh liều nếu cần.',
          }))
        setWarnings(foundWarnings)
      }
    } catch {
      const foundWarnings = drugInteractions
        .filter((interaction) => interaction.drugs.every((drugId) => ids.includes(drugId)))
        .map((w, index) => ({
          ruleId: w.ruleId || `rule-local-${index}`,
          drugIdA: w.drugs[0],
          drugIdB: w.drugs[1],
          severity: w.severity?.includes('Cao') ? 'SEVERE' : 'MODERATE',
          description: w.description,
          clinicalRecommendation: w.recommendation || 'Theo dõi sát diễn biến lâm sàng của bệnh nhân và điều chỉnh liều nếu cần.',
        }))
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
      return 'Phát hiện tương tác thuốc! Vui lòng nhập lý do chuyên môn để xác nhận vượt cảnh báo'
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

    const interactionOverrides = warnings.map((w) => ({
      ruleId: w.ruleId || 'rule-override',
      overrideReason: overrideReason.trim(),
    }))

    const payload = {
      medicalRecordId: recordId,
      items: items.map((i) => ({
        medicineId: i.medicineId,
        quantity: i.quantity,
        dosage: i.dosage,
        frequency: i.dosage,
        route: 'ORAL',
        durationDays: 7,
      })),
      overrideReason: overrideReason.trim(),
      changeReason: changeReason.trim(),
      interactionOverrides,
    }

    try {
      if (editing) {
        await pharmacyApi.updatePrescription(editing.id, payload)
        saveStoredPrescription({ ...editing, items: JSON.stringify(items), warnings, overrideReason, changeReason, updatedAt: new Date().toISOString() })
        message.success('Đã cập nhật đơn thuốc và lưu vết thay đổi')
      } else {
        const res = await pharmacyApi.createPrescription(payload)
        if (res?.data) saveStoredPrescription({ ...res.data, warnings, overrideReason })
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
    } catch (err) {
      // Catch HTTP 409 Conflict from backend (PrescriptionInteractionConfirmationRequiredException)
      if (err.response?.status === 409 && err.response?.data?.warnings?.length) {
        const beWarnings = err.response.data.warnings
        setWarnings(beWarnings)
        message.warning('Phát hiện tương tác thuốc cần xác nhận! Vui lòng kiểm tra và nhập lý do chuyên môn.')
        setSaving(false)
        return
      }

      // Fallback saving for offline/frontend persistence
      if (editing) {
        const updated = { ...editing, items: JSON.stringify(items), warnings, overrideReason, changeReason, updatedAt: new Date().toISOString() }
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
          warnings,
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
          <MedicineBoxOutlined /> Kê đơn thuốc
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
              style={{ width: 340 }}
              value={item.medicineId}
              onChange={(value) => updateItem(index, 'medicineId', value)}
              options={medicines.map((m) => ({
                value: m.id,
                label: `${m.name || m.medicineName} (${m.activeIngredient || m.category || ''}) — Tồn: ${m.stock ?? 100} ${m.unit || 'viên'}`,
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

        {/* Khối Hiển thị Cảnh báo Tương tác Thuốc Trực quan */}
        {!!warnings.length && (
          <Card
            type="inner"
            title={
              <Space>
                <ExclamationCircleOutlined style={{ color: '#cf1322', fontSize: 18 }} />
                <span style={{ color: '#cf1322', fontWeight: 700 }}>
                  CẢNH BÁO TƯƠNG TÁC THUỐC ({warnings.length} cặp tương tác có nguy cơ)
                </span>
              </Space>
            }
            style={{
              marginTop: 18,
              borderColor: '#ffa39e',
              backgroundColor: '#fff2f0',
              borderRadius: 8,
            }}
          >
            <List
              itemLayout="vertical"
              dataSource={warnings}
              renderItem={(w, idx) => {
                const drugA = getDrugName(w.drugIdA || w.firstMedicineId)
                const drugB = getDrugName(w.drugIdB || w.secondMedicineId)
                return (
                  <List.Item
                    key={idx}
                    style={{
                      backgroundColor: '#ffffff',
                      padding: 14,
                      borderRadius: 8,
                      marginBottom: 10,
                      border: '1px solid #ffccc7',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <div style={{ fontSize: 14, fontWeight: 700, color: '#1f1f1f' }}>
                        <MedicineBoxOutlined style={{ color: '#1890ff', marginRight: 6 }} />
                        {drugA || 'Thuốc A'}
                        <span style={{ margin: '0 8px', color: '#ff4d4f', fontWeight: 800 }}>⚡</span>
                        {drugB || 'Thuốc B'}
                      </div>
                      {renderSeverityTag(w.severity)}
                    </div>

                    <div style={{ fontSize: 13, color: '#434343', marginBottom: 6 }}>
                      <strong>Mô tả cơ chế / Tác hại: </strong> {w.description || w.warningMessage || 'Phát hiện tương tác dược lý giữa hai loại thuốc này.'}
                    </div>

                    {(w.clinicalRecommendation || w.action) && (
                      <div
                        style={{
                          backgroundColor: '#e6f7ff',
                          borderLeft: '4px solid #1890ff',
                          padding: '6px 12px',
                          borderRadius: 4,
                          fontSize: 13,
                          color: '#0050b3',
                        }}
                      >
                        <strong>💡 Khuyến cáo lâm sàng: </strong>
                        {w.clinicalRecommendation || w.action}
                      </div>
                    )}
                  </List.Item>
                )
              }}
            />

            <Form.Item
              label={<span style={{ fontWeight: 600, color: '#cf1322' }}>Lý do chuyên môn vượt cảnh báo (Bắt buộc nhập khi tiếp tục kê đơn)</span>}
              required
              style={{ marginTop: 14, marginBottom: 0 }}
            >
              <Input.TextArea
                rows={2}
                placeholder="Nhập lý do bác sĩ quyết định cho dùng kết hợp (VD: Đã cân nhắc lợi ích vượt nguy cơ, theo dõi sát các chỉ số sinh hóa/INR...)"
                value={overrideReason}
                onChange={(e) => setOverrideReason(e.target.value)}
              />
            </Form.Item>
          </Card>
        )}

        {editing && (
          <Form.Item label="Lý do điều chỉnh đơn thuốc (lưu vết thay đổi)" required style={{ marginTop: 16 }}>
            <Input.TextArea
              rows={2}
              placeholder="Nhập lý do điều chỉnh đơn thuốc trước khi cấp phát..."
              value={changeReason}
              onChange={(e) => setChangeReason(e.target.value)}
            />
          </Form.Item>
        )}
      </Card>

      {/* Danh sách đơn thuốc đã lập */}
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
              title: 'Cảnh báo tương tác',
              key: 'warnings',
              render: (_, row) => {
                const rowWarns = row.warnings || parseJson(row.warnings)
                if (rowWarns && rowWarns.length) {
                  return (
                    <Tag
                      color="volcano"
                      icon={<WarningOutlined />}
                      style={{ cursor: 'pointer' }}
                      onClick={() => setViewingWarningModal(row)}
                    >
                      {rowWarns.length} Tương tác (Đã xác nhận)
                    </Tag>
                  )
                }
                return <Tag color="default">Không có</Tag>
              },
            },
            {
              title: 'Ngày lập',
              dataIndex: 'createdAt',
              render: (val) => val ? dayjs(val).format('HH:mm DD/MM/YYYY') : '—',
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              render: (value) => (
                <Tag color={value === 'PENDING_DISPENSING' || value === 'PENDING_DISPENSE' ? 'orange' : 'green'}>
                  {value === 'PENDING_DISPENSING' || value === 'PENDING_DISPENSE' ? 'Chờ cấp phát' : 'Đã cấp phát'}
                </Tag>
              ),
            },
            {
              title: 'Thao tác',
              key: 'actions',
              width: 130,
              render: (_, row) => {
                const isPending = row.status === 'PENDING_DISPENSING' || row.status === 'PENDING_DISPENSE'
                const hasWarnings = row.warnings && row.warnings.length > 0
                const menuItems = [
                  {
                    key: 'edit',
                    label: 'Điều chỉnh đơn thuốc',
                    icon: <EditOutlined />,
                    disabled: !isPending,
                    onClick: () => beginEdit(row),
                  },
                  ...(hasWarnings
                    ? [
                        {
                          key: 'warnings',
                          label: 'Xem chi tiết cảnh báo',
                          icon: <EyeOutlined />,
                          onClick: () => setViewingWarningModal(row),
                        },
                      ]
                    : []),
                ]
                return (
                  <Dropdown menu={{ items: menuItems }} trigger={['click']}>
                    <Button size="small" icon={<MoreOutlined />}>
                      Thao tác
                    </Button>
                  </Dropdown>
                )
              },
            },
          ]}
        />
      </Card>

      {/* Modal Xem chi tiết cảnh báo tương tác đã lưu */}
      <Modal
        title={
          <Space>
            <WarningOutlined style={{ color: '#fa8c16' }} />
            <span>Chi tiết cảnh báo tương tác thuốc - Đơn {viewingWarningModal?.prescriptionCode}</span>
          </Space>
        }
        open={!!viewingWarningModal}
        onCancel={() => setViewingWarningModal(null)}
        footer={[
          <Button key="close" onClick={() => setViewingWarningModal(null)}>
            Đóng
          </Button>,
        ]}
        width={650}
      >
        {viewingWarningModal && (
          <div>
            <Descriptions size="small" column={1} bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Lý do chuyên môn bác sĩ đã ghi nhận">
                <Text type="danger" strong>{viewingWarningModal.overrideReason || 'Bác sĩ đã xác nhận vượt cảnh báo khi kê đơn.'}</Text>
              </Descriptions.Item>
            </Descriptions>

            <List
              itemLayout="vertical"
              dataSource={viewingWarningModal.warnings || []}
              renderItem={(w, idx) => (
                <List.Item key={idx} style={{ backgroundColor: '#fafafa', padding: 12, borderRadius: 6, marginBottom: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <strong>{getDrugName(w.drugIdA || w.firstMedicineId)} ⚡ {getDrugName(w.drugIdB || w.secondMedicineId)}</strong>
                    {renderSeverityTag(w.severity)}
                  </div>
                  <div style={{ fontSize: 12, color: '#595959' }}>{w.description || w.warningMessage}</div>
                  {(w.clinicalRecommendation || w.action) && (
                    <div style={{ fontSize: 12, color: '#096dd9', marginTop: 4 }}>
                      <em>Khuyến cáo: {w.clinicalRecommendation || w.action}</em>
                    </div>
                  )}
                </List.Item>
              )}
            />
          </div>
        )}
      </Modal>
    </div>
  )
}

export default PrescriptionPage
