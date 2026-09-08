import React, { useEffect, useState } from 'react'
import { Alert, Badge, Button, Space, Tag, Tooltip, Typography } from 'antd'
import {
  AlertOutlined,
  ExclamationCircleOutlined,
  FireOutlined,
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
              border: hasAnaphylaxis ? '1px solid #f87171' : '1px solid #fed7aa',
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
                style={{ fontSize: 11, padding: 0 }}
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
        <Alert
          type={hasAnaphylaxis || hasSevere ? 'error' : 'warning'}
          showIcon
          icon={
            hasAnaphylaxis ? (
              <FireOutlined style={{ fontSize: 22, color: '#dc2626' }} />
            ) : hasSevere ? (
              <AlertOutlined style={{ fontSize: 20, color: '#ef4444' }} />
            ) : (
              <WarningOutlined style={{ fontSize: 20, color: '#f59e0b' }} />
            )
          }
          style={{
            borderRadius: 8,
            border: hasAnaphylaxis
              ? '2px solid #ef4444'
              : hasSevere
              ? '1.5px solid #f87171'
              : '1.5px solid #f59e0b',
            background: hasAnaphylaxis
              ? 'linear-gradient(135deg, #fff1f2 0%, #fee2e2 100%)'
              : 'linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%)',
            boxShadow: hasAnaphylaxis
              ? '0 4px 12px rgba(239, 68, 68, 0.15)'
              : '0 2px 8px rgba(245, 158, 11, 0.1)',
          }}
          message={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
              <Space size={8} align="center">
                <Text
                  strong
                  style={{
                    fontSize: 14,
                    color: hasAnaphylaxis ? '#991b1b' : '#b45309',
                    letterSpacing: '0.3px',
                  }}
                >
                  {hasAnaphylaxis
                    ? '⚠️ CẢNH BÁO NGUY HIỂM: TIỀN SỬ SỐC PHẢN VỆ VỚI THUỐC'
                    : hasSevere
                    ? '⚠️ CẢNH BÁO: TIỀN SỬ DỊ ỨNG THUỐC MỨC ĐỘ NẶNG'
                    : 'THÔNG TIN TIỀN SỬ DỊ ỨNG THUỐC CỦA BỆNH NHÂN'}
                </Text>
                <Tag color={hasAnaphylaxis ? '#b91c1c' : '#ea580c'} style={{ fontWeight: 700, borderRadius: 10 }}>
                  {activeAllergies.length} hoạt chất / nhóm thuốc
                </Tag>
              </Space>
              <Space size={6}>
                {canWrite && (
                  <Button
                    size="small"
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => setModalOpen(true)}
                    style={{
                      background: hasAnaphylaxis ? '#dc2626' : '#d97706',
                      borderColor: hasAnaphylaxis ? '#dc2626' : '#d97706',
                      fontWeight: 600,
                      borderRadius: 4,
                    }}
                  >
                    Khai báo thêm
                  </Button>
                )}
                <Button
                  size="small"
                  icon={<SafetyCertificateOutlined />}
                  onClick={() => setModalOpen(true)}
                  style={{ fontWeight: 600, borderRadius: 4 }}
                >
                  Quản lý & Lưu vết
                </Button>
              </Space>
            </div>
          }
          description={
            <div style={{ marginTop: 8 }}>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
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
                          fontSize: 13,
                          fontWeight: 700,
                          padding: '4px 10px',
                          borderRadius: 6,
                          border: isCritical ? '1.5px solid #b91c1c' : undefined,
                          cursor: 'pointer',
                        }}
                        onClick={() => setModalOpen(true)}
                      >
                        {isCritical && <FireOutlined style={{ marginRight: 4 }} />}
                        {allergy.allergenName} ({meta.label})
                      </Tag>
                    </Tooltip>
                  )
                })}
              </div>
              <div style={{ marginTop: 6, fontSize: 12, color: hasAnaphylaxis ? '#7f1d1d' : '#78350f' }}>
                {hasAnaphylaxis
                  ? 'Bác sĩ tuyệt đối không chỉ định hoặc kê đơn các thuốc chứa hoạt chất trên để phòng ngừa phản vệ đe dọa tính mạng.'
                  : 'Vui lòng đối soát kỹ thành phần và tá dược trước khi kê đơn bất kỳ thuốc nào cho bệnh nhân.'}
              </div>
            </div>
          }
        />
      ) : (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: 8,
            padding: '8px 16px',
          }}
        >
          <Space align="center" size={8}>
            <SafetyCertificateOutlined style={{ color: '#16a34a', fontSize: 18 }} />
            <Text strong style={{ color: '#334155', fontSize: 13 }}>
              Tiền sử dị ứng thuốc:
            </Text>
            <Tag color="default" style={{ margin: 0 }}>
              Chưa ghi nhận dị ứng thuốc
            </Tag>
          </Space>
          {canWrite && (
            <Button
              size="small"
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
