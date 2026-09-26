import React, { useEffect, useState } from 'react'
import { Alert, Badge, Button, Modal, Space, Tag, Tooltip, Typography } from 'antd'
import {
  MedicineBoxOutlined,
  PlusOutlined,
  UnorderedListOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import patientChronicDiseaseApi from '../../api/patientChronicDiseaseApi.js'
import { useAuthContext } from '../../context/AuthContext.jsx'
import {
  hasChronicDiseaseReadPermission,
  hasChronicDiseaseWritePermission,
} from '../../utils/chronicDiseaseHelpers.js'
import ChronicDiseaseList from './ChronicDiseaseList.jsx'
import AddChronicDiseaseModal from './AddChronicDiseaseModal.jsx'

const { Text } = Typography

export default function PatientChronicDiseaseBanner({
  patientId,
  patientName = '',
  visitId = null,
  compact = false,
  currentUser: customUser = null,
  doctorName = null,
  onUpdated = null,
  style = {},
}) {
  const { user: authUser } = useAuthContext()
  const user = customUser || authUser

  const canWrite = hasChronicDiseaseWritePermission(user)
  const canRead = hasChronicDiseaseReadPermission(user)

  const [diseases, setDiseases] = useState([])
  const [loading, setLoading] = useState(false)
  const [listModalOpen, setListModalOpen] = useState(false)
  const [addModalOpen, setAddModalOpen] = useState(false)

  const fetchDiseases = async () => {
    if (!patientId || !canRead) return
    setLoading(true)
    try {
      const res = await patientChronicDiseaseApi.list(patientId)
      const list = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
      const activeList = list.filter((item) => item.active !== false)
      setDiseases(activeList)
      if (onUpdated) {
        onUpdated(activeList)
      }
    } catch (err) {
      console.warn('Lỗi nạp tiền sử bệnh mạn tính cho banner:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (patientId && canRead) {
      fetchDiseases()
    }
  }, [patientId, canRead])

  if (!canRead) {
    return null
  }

  const hasDiseases = diseases.length > 0

  if (compact) {
    return (
      <div style={{ ...style }}>
        {hasDiseases ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: '#f0f9ff',
              border: '1.5px solid #bae6fd',
              padding: '6px 12px',
              borderRadius: 6,
              flexWrap: 'wrap',
              gap: 8,
            }}
          >
            <Space size={6} wrap style={{ flex: 1 }}>
              <MedicineBoxOutlined style={{ color: '#0284c7', fontSize: 16 }} />
              <Text strong style={{ color: '#0369a1', fontSize: 12 }}>
                Bệnh mạn tính ({diseases.length}):
              </Text>
              {diseases.map((d) => (
                <Tooltip
                  key={d.id}
                  title={`${d.diagnosisCode} - ${d.diagnosisName || ''}${d.yearDetected ? ` (Phát hiện: ${d.yearDetected})` : ''}${d.notes ? ` - ${d.notes}` : ''}`}
                >
                  <Tag
                    color="blue"
                    style={{
                      margin: 0,
                      fontWeight: 600,
                      cursor: 'pointer',
                      borderRadius: 4,
                      fontSize: 12,
                    }}
                    onClick={() => setListModalOpen(true)}
                  >
                    {d.diagnosisCode} - {d.diagnosisName || 'Bệnh nền'}
                    {d.yearDetected && (
                      <span style={{ fontWeight: 400, opacity: 0.85 }}> ({d.yearDetected})</span>
                    )}
                  </Tag>
                </Tooltip>
              ))}
            </Space>
            <Space size={6}>
              <Button
                size="small"
                type="link"
                icon={<UnorderedListOutlined />}
                onClick={() => setListModalOpen(true)}
                style={{ fontSize: 12, padding: 0, color: '#0284c7', fontWeight: 600 }}
              >
                Chi tiết
              </Button>
              {canWrite && (
                <Button
                  size="small"
                  type="primary"
                  ghost
                  icon={<PlusOutlined />}
                  onClick={() => setAddModalOpen(true)}
                  style={{ fontSize: 11, height: 24, padding: '0 8px' }}
                >
                  Thêm
                </Button>
              )}
            </Space>
          </div>
        ) : (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: 13,
              color: '#475569',
              background: '#f8fafc',
              padding: '6px 12px',
              borderRadius: 6,
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <span style={{ display: 'inline-block', width: 120, fontWeight: 700, color: '#1e293b' }}>
                Bệnh mạn tính:
              </span>
              <Text type="secondary" style={{ fontStyle: 'italic' }}>Chưa ghi nhận</Text>
            </div>
            {canWrite && (
              <Button
                size="middle"
                type="primary"
                ghost
                icon={<PlusOutlined />}
                onClick={() => setAddModalOpen(true)}
                style={{
                  fontSize: 13,
                  fontWeight: 600,
                  height: 32,
                  padding: '0 14px',
                  borderRadius: 6,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 4,
                }}
              >
                Ghi nhận
              </Button>
            )}
          </div>
        )}

        {/* Modal Danh sách chi tiết */}
        <Modal
          open={listModalOpen}
          onCancel={() => setListModalOpen(false)}
          footer={<Button onClick={() => setListModalOpen(false)}>Đóng</Button>}
          width={860}
          title={null}
          destroyOnClose
        >
          {listModalOpen && (
            <div style={{ paddingTop: 12 }}>
              <ChronicDiseaseList
                patientId={patientId}
                patientName={patientName}
                visitId={visitId}
                currentUser={user}
                doctorName={doctorName}
                onListUpdated={(list) => {
                  setDiseases(list)
                  if (onUpdated) onUpdated(list)
                }}
                bordered={false}
              />
            </div>
          )}
        </Modal>

        {/* Modal Thêm bệnh mạn tính nhanh */}
        {addModalOpen && (
          <AddChronicDiseaseModal
            open={addModalOpen}
            onClose={() => setAddModalOpen(false)}
            patientId={patientId}
            patientName={patientName}
            visitId={visitId}
            onSuccess={(newRecord) => {
              fetchDiseases()
            }}
          />
        )}
      </div>
    )
  }

  // Non-compact: Only display warning banner when patient has chronic diseases recorded
  if (!hasDiseases) {
    return null
  }

  return (
    <div style={{ marginBottom: 16, ...style }}>
      <Alert
        type="info"
        showIcon
        icon={<MedicineBoxOutlined style={{ fontSize: 18, color: '#0284c7' }} />}
        message={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <strong style={{ fontSize: 13.5, color: '#0369a1' }}>
              Tiền sử bệnh mạn tính ({diseases.length} bệnh nền đã ghi nhận):
            </strong>
            <Space size={8}>
              <Button
                size="small"
                type="link"
                icon={<UnorderedListOutlined />}
                onClick={() => setListModalOpen(true)}
                style={{ fontWeight: 600, color: '#0284c7' }}
              >
                Xem danh sách đầy đủ
              </Button>
              {canWrite && (
                <Button
                  size="small"
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setAddModalOpen(true)}
                  style={{ fontSize: 12 }}
                >
                  Ghi nhận thêm
                </Button>
              )}
            </Space>
          </div>
        }
        description={
          <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {diseases.map((d) => (
              <Tag
                key={d.id}
                color="blue"
                style={{
                  padding: '2px 8px',
                  fontSize: 12.5,
                  borderRadius: 4,
                  fontWeight: 600,
                }}
              >
                {d.diagnosisCode} - {d.diagnosisName || 'Bệnh mạn tính'}
                {d.yearDetected && ` (${d.yearDetected})`}
                {d.notes && (
                  <span style={{ fontWeight: 400, color: '#475569', marginLeft: 4 }}>
                    • {d.notes}
                  </span>
                )}
              </Tag>
            ))}
          </div>
        }
        style={{
          border: '1.5px solid #bae6fd',
          background: '#f0f9ff',
          borderRadius: 8,
        }}
      />

      {/* Modal Danh sách chi tiết */}
      <Modal
        open={listModalOpen}
        onCancel={() => setListModalOpen(false)}
        footer={<Button onClick={() => setListModalOpen(false)}>Đóng</Button>}
        width={860}
        title={null}
        destroyOnClose
      >
        {listModalOpen && (
          <div style={{ paddingTop: 12 }}>
            <ChronicDiseaseList
              patientId={patientId}
              patientName={patientName}
              visitId={visitId}
              currentUser={user}
              doctorName={doctorName}
              onListUpdated={(list) => {
                setDiseases(list)
                if (onUpdated) onUpdated(list)
              }}
              bordered={false}
            />
          </div>
        )}
      </Modal>

      {/* Modal Thêm bệnh mạn tính nhanh */}
      {addModalOpen && (
        <AddChronicDiseaseModal
          open={addModalOpen}
          onClose={() => setAddModalOpen(false)}
          patientId={patientId}
          patientName={patientName}
          visitId={visitId}
          onSuccess={(newRecord) => {
            fetchDiseases()
          }}
        />
      )}
    </div>
  )
}
