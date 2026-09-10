import React, { useEffect, useState } from 'react'
import { Alert, Badge, Button, Space, Tag, Tooltip, Typography } from 'antd'
import {
  AlertOutlined,
  ExclamationCircleOutlined,
  FireOutlined,
  HistoryOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import patientAllergyApi from '../../api/patientAllergyApi'
import { getAllergySeverityMeta } from '../../utils/allergyConstants'
import PatientAllergyModal from './PatientAllergyModal'

const { Text } = Typography

export default function PatientAllergyBanner({
  patientId,
  patientName = '',
  visitId = null,
  allergies: externalAllergies = null,
  onAllergiesUpdated = null,
  onAllergiesChange = null,
  currentUser = null,
  canWrite = true,
  compact = false,
  onOpenLogs = null,
  style = {},
}) {
  const [allergies, setAllergies] = useState(externalAllergies || [])
  const [modalOpen, setModalOpen] = useState(false)
  const [loading, setLoading] = useState(false)

  const fetchAllergies = async () => {
    if (!patientId) return
    setLoading(true)
    try {
      const res = await patientAllergyApi.getAllergies(patientId)
      const list = Array.isArray(res?.data) ? res.data : []
      setAllergies(list)
      if (onAllergiesUpdated) {
        onAllergiesUpdated(list)
      }
      if (onAllergiesChange) {
        onAllergiesChange(list)
      }
    } catch (err) {
      console.warn('Không thể nạp danh sách dị ứng thuốc:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (externalAllergies !== null) {
      setAllergies(externalAllergies)
    } else if (patientId) {
      fetchAllergies()
    }
  }, [patientId, externalAllergies])

  const activeAllergies = allergies.filter((a) => a.active !== false)
  const hasAllergies = activeAllergies.length > 0
  const hasAnaphylaxis = activeAllergies.some((a) => a.severity === 'ANAPHYLAXIS')
  const hasSevere = activeAllergies.some((a) => a.severity === 'SEVERE')

  const handleUpdateCallback = (newList) => {
    setAllergies(newList)
    if (onAllergiesUpdated) {
      onAllergiesUpdated(newList)
    }
    if (onAllergiesChange) {
      onAllergiesChange(newList)
    }
  }

  if (compact) {
    return (
      <div style={{ ...style }}>
        {hasAllergies ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: hasAnaphylaxis ? '#fef2f2' : '#fff7ed',
              border: hasAnaphylaxis ? '1.5px solid #f87171' : '1.5px solid #fed7aa',
              padding: '6px 12px',
              borderRadius: 6,
            }}
          >
            <Space size={6} wrap style={{ flex: 1 }}>
              {hasAnaphylaxis ? (
                <FireOutlined style={{ color: '#dc2626', fontSize: 16 }} />
              ) : (
                <WarningOutlined style={{ color: '#ea580c', fontSize: 16 }} />
              )}
              <Text strong style={{ color: hasAnaphylaxis ? '#b91c1c' : '#c2410c', fontSize: 12 }}>
                Dị ứng ({activeAllergies.length}):
              </Text>
              {activeAllergies.map((allergy) => {
                const meta = getAllergySeverityMeta(allergy.severity)
                return (
                  <Tooltip
                    key={allergy.id || allergy.allergenName}
                    title={
                      <div>
                        <div><strong>{allergy.allergenName}</strong> ({meta.label})</div>
                        {allergy.reaction && <div>Biểu hiện: {allergy.reaction}</div>}
                        {allergy.notes && <div>Ghi chú: {allergy.notes}</div>}
                      </div>
                    }
                  >
                    <Tag
                      color={meta.color}
                      style={{ fontWeight: 700, margin: '2px 0', fontSize: 11 }}
                    >
                      {allergy.allergenName}
                    </Tag>
                  </Tooltip>
                )
              })}
            </Space>
            <Button
              size="small"
              type="link"
              onClick={() => setModalOpen(true)}
              style={{ fontSize: 12, padding: '0 4px', fontWeight: 600 }}
            >
              Chi tiết
            </Button>
          </div>
        ) : (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: 12,
              color: '#64748b',
              background: '#f8fafc',
              padding: '4px 8px',
              borderRadius: 4,
            }}
          >
            <span>
              <b>Dị ứng thuốc:</b> <Text type="secondary">Chưa ghi nhận</Text>
            </span>
            {canWrite && (
              <Button
                size="small"
                type="link"
                icon={<PlusOutlined />}
                onClick={() => setModalOpen(true)}
                style={{ fontSize: 12, padding: 0 }}
              >
                Ghi nhận
              </Button>
            )}
          </div>
        )}

        <PatientAllergyModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          patientId={patientId}
          patientName={patientName}
          visitId={visitId}
          currentUser={currentUser}
          canWrite={canWrite}
          onAllergiesUpdated={handleUpdateCallback}
        />
      </div>
    )
  }

  return (
    <div style={{ marginBottom: 16, ...style }}>
      {hasAllergies ? (
        <div
          style={{
            borderRadius: 10,
            border: hasAnaphylaxis
              ? '2.5px solid #dc2626'
              : hasSevere
              ? '2px solid #ef4444'
              : '2px solid #ea580c',
            background: hasAnaphylaxis
              ? 'linear-gradient(135deg, #fff1f2 0%, #fee2e2 100%)'
              : 'linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%)',
            boxShadow: hasAnaphylaxis
              ? '0 4px 14px rgba(220, 38, 38, 0.16)'
              : '0 3px 10px rgba(234, 88, 12, 0.12)',
            padding: '16px 20px',
          }}
        >
          {/* Header Row */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: 12,
              marginBottom: 12,
            }}
          >
            <Space size={10} align="center">
              {hasAnaphylaxis ? (
                <FireOutlined style={{ fontSize: 26, color: '#dc2626' }} />
              ) : (
                <AlertOutlined style={{ fontSize: 24, color: '#ea580c' }} />
              )}
              <div>
                <div
                  style={{
                    fontSize: 16,
                    fontWeight: 800,
                    color: hasAnaphylaxis ? '#991b1b' : '#9a3412',
                    letterSpacing: '0.3px',
                    textTransform: 'uppercase',
                  }}
                >
                  {hasAnaphylaxis
                    ? '⚠️ CẢNH BÁO NGUY HIỂM: BỆNH NHÂN CÓ TIỀN SỬ SỐC PHẢN VỆ'
                    : hasSevere
                    ? '⚠️ CẢNH BÁO LÂM SÀNG: TIỀN SỬ DỊ ỨNG THUỐC MỨC ĐỘ NẶNG'
                    : 'CẢNH BÁO LÂM SÀNG: BỆNH NHÂN CÓ TIỀN SỬ DỊ ỨNG THUỐC'}
                </div>
              </div>
              <Tag
                color={hasAnaphylaxis ? '#b91c1c' : '#c2410c'}
                style={{
                  fontWeight: 700,
                  borderRadius: 12,
                  fontSize: 12,
                  padding: '2px 10px',
                  margin: 0,
                }}
              >
                {activeAllergies.length} hoạt chất / nhóm dị ứng
              </Tag>
            </Space>

            <Space size={8} wrap>
              {canWrite && (
                <Button
                  size="middle"
                  type="primary"
                  danger
                  icon={<PlusOutlined />}
                  onClick={() => setModalOpen(true)}
                  style={{
                    fontWeight: 700,
                    borderRadius: 6,
                    boxShadow: '0 2px 6px rgba(220, 38, 38, 0.25)',
                  }}
                >
                  Khai báo thêm
                </Button>
              )}
              {onOpenLogs && (
                <Button
                  size="middle"
                  icon={<HistoryOutlined />}
                  onClick={onOpenLogs}
                  style={{
                    fontWeight: 600,
                    borderRadius: 6,
                    color: '#991b1b',
                    borderColor: '#fca5a5',
                    background: '#ffffff',
                  }}
                >
                  Nhật ký vượt cảnh báo
                </Button>
              )}
              <Button
                size="middle"
                icon={<SafetyCertificateOutlined />}
                onClick={() => setModalOpen(true)}
                style={{ fontWeight: 600, borderRadius: 6 }}
              >
                Quản lý hồ sơ dị ứng
              </Button>
            </Space>
          </div>

          {/* Tags List */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center', marginBottom: 10 }}>
            {activeAllergies.map((allergy) => {
              const meta = getAllergySeverityMeta(allergy.severity)
              const isCritical = allergy.severity === 'ANAPHYLAXIS'
              return (
                <Tooltip
                  key={allergy.id || allergy.allergenName}
                  title={
                    <div style={{ padding: 4 }}>
                      <div style={{ fontWeight: 700, fontSize: 13 }}>{allergy.allergenName}</div>
                      <div style={{ color: '#fde047' }}>Mức độ: {meta.label}</div>
                      {allergy.reaction && <div>Biểu hiện: {allergy.reaction}</div>}
                      {allergy.notes && <div>Ghi chú: {allergy.notes}</div>}
                    </div>
                  }
                >
                  <Tag
                    color={meta.color}
                    style={{
                      fontSize: 14,
                      fontWeight: 700,
                      padding: '6px 14px',
                      borderRadius: 8,
                      border: isCritical ? '2px solid #b91c1c' : undefined,
                      cursor: 'pointer',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 6,
                    }}
                    onClick={() => setModalOpen(true)}
                  >
                    {isCritical && <FireOutlined style={{ color: '#ffffff' }} />}
                    <span>{allergy.allergenName}</span>
                    <span style={{ fontSize: 12, opacity: 0.9 }}>({meta.label})</span>
                  </Tag>
                </Tooltip>
              )
            })}
          </div>

          {/* Clinical note */}
          <div
            style={{
              fontSize: 13,
              fontWeight: 500,
              color: hasAnaphylaxis ? '#7f1d1d' : '#7c2d12',
              lineHeight: '1.5',
            }}
          >
            {hasAnaphylaxis
              ? 'Bác sĩ tuyệt đối không chỉ định hoặc kê đơn các thuốc chứa hoạt chất trên để phòng ngừa phản vệ đe dọa tính mạng.'
              : 'Vui lòng đối soát kỹ hoạt chất và nhóm tương tự trước khi chỉ định hoặc kê đơn thuốc cho bệnh nhân.'}
          </div>
        </div>
      ) : (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: '#f0fdf4',
            border: '1.5px solid #86efac',
            borderRadius: 8,
            padding: '12px 18px',
          }}
        >
          <Space align="center" size={10}>
            <SafetyCertificateOutlined style={{ color: '#16a34a', fontSize: 22 }} />
            <Text strong style={{ color: '#166534', fontSize: 14 }}>
              Tiền sử dị ứng thuốc:
            </Text>
            <Tag color="success" style={{ margin: 0, fontWeight: 600, fontSize: 13, padding: '2px 8px' }}>
              Chưa ghi nhận tiền sử dị ứng thuốc
            </Tag>
          </Space>
          {canWrite && (
            <Button
              size="middle"
              type="dashed"
              icon={<PlusOutlined />}
              onClick={() => setModalOpen(true)}
              style={{ fontWeight: 600 }}
            >
              Khai báo dị ứng thuốc
            </Button>
          )}
        </div>
      )}

      <PatientAllergyModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        patientId={patientId}
        patientName={patientName}
        visitId={visitId}
        currentUser={currentUser}
        canWrite={canWrite}
        onAllergiesUpdated={handleUpdateCallback}
      />
    </div>
  )
}
