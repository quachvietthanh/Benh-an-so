import React, { useCallback, useEffect, useRef, useState } from 'react'
import {
  CalendarOutlined,
  HistoryOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Empty,
  Modal,
  Space,
  Spin,
  Tag,
  Timeline,
  Tooltip,
  Typography,
} from 'antd'
import postCareLogApi from '../../api/postCareLogApi'
import {
  formatVietnamDateTime,
  getAftercareErrorMessage,
  getContactChannelMeta,
  getContactOutcomeMeta,
  getPatientConditionMeta,
  isUuid,
  shortId,
} from '../../utils/aftercareHelpers'

const { Paragraph, Text } = Typography

function PatientCareHistoryModal({
  open,
  onCancel,
  patientId,
  patient,
  patientsById = {},
  performerNames = {},
}) {
  const [careLogs, setCareLogs] = useState([])
  const [loadedPatientId, setLoadedPatientId] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const requestSequence = useRef(0)

  const loadHistory = useCallback(async () => {
    if (!open) return

    const requestId = ++requestSequence.current
    setLoading(true)
    setError(null)

    if (!isUuid(patientId)) {
      if (requestId === requestSequence.current) {
        setError(getAftercareErrorMessage(
          new Error('ID bệnh nhân không hợp lệ.'),
          'Không thể tải lịch sử chăm sóc bệnh nhân.',
        ))
        setLoading(false)
      }
      return
    }

    try {
      const response = await postCareLogApi.getForPatient(patientId)
      const responseData = response?.data
      const records = Array.isArray(responseData)
        ? responseData
        : (Array.isArray(responseData?.content) ? responseData.content : null)

      if (!records) {
        throw new Error('Dữ liệu lịch sử chăm sóc không hợp lệ.')
      }

      if (requestId === requestSequence.current) {
        setCareLogs(records)
        setLoadedPatientId(patientId)
      }
    } catch (requestError) {
      if (requestId === requestSequence.current) {
        setError(getAftercareErrorMessage(
          requestError,
          'Không thể tải lịch sử chăm sóc bệnh nhân.',
        ))
      }
    } finally {
      if (requestId === requestSequence.current) {
        setLoading(false)
      }
    }
  }, [open, patientId])

  useEffect(() => {
    if (!open) {
      requestSequence.current += 1
      setLoading(false)
      return undefined
    }

    loadHistory()

    return () => {
      requestSequence.current += 1
    }
  }, [loadHistory, open])

  const handleCancel = () => {
    requestSequence.current += 1
    setLoading(false)
    onCancel?.()
  }

  const indexedPatient = patientsById instanceof Map
    ? patientsById.get(patientId)
    : patientsById?.[patientId]
  const patientMatchesId = !patient?.id || String(patient.id) === String(patientId)
  const currentPatient = patientMatchesId ? (patient || indexedPatient) : indexedPatient
  const patientName = currentPatient?.fullName || currentPatient?.name
  const patientLabel = patientName || (patientId ? `ID ${shortId(patientId)}` : 'Bệnh nhân')
  const patientCode = currentPatient?.patientCode
  const visibleLogs = loadedPatientId === patientId ? careLogs : []

  const getPerformerName = (performedBy) => {
    if (!performedBy) return '—'
    const knownName = performerNames instanceof Map
      ? performerNames.get(performedBy)
      : performerNames?.[performedBy]

    if (knownName) return knownName
    return (
      <Tooltip title={performedBy}>
        <Text>ID {shortId(performedBy)}</Text>
      </Tooltip>
    )
  }

  const renderContent = () => {
    if (loading) {
      return (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '48px 0' }}>
          <Spin tip="Đang tải lịch sử chăm sóc..." />
        </div>
      )
    }

    if (error) {
      return (
        <Alert
          showIcon
          type="error"
          message="Không thể tải lịch sử chăm sóc"
          description={error}
          action={(
            <Button
              size="small"
              icon={<ReloadOutlined />}
              onClick={loadHistory}
            >
              Thử lại
            </Button>
          )}
        />
      )
    }

    if (visibleLogs.length === 0) {
      return (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Chưa có ghi nhận chăm sóc sau khám."
        />
      )
    }

    return (
      <Timeline
        items={visibleLogs.map((log, index) => {
          const channel = getContactChannelMeta(log.contactChannel)
          const condition = getPatientConditionMeta(log.patientCondition)
          const outcome = getContactOutcomeMeta(log.contactOutcome)

          return {
            key: log.id || `${log.contactedAt || 'care-log'}-${index}`,
            color: condition.color === 'error'
              ? 'red'
              : (condition.color === 'warning' ? 'orange' : 'blue'),
            children: (
              <Card size="small" style={{ marginBottom: 12 }}>
                <Space direction="vertical" size={10} style={{ width: '100%' }}>
                  <Space wrap size={[8, 8]}>
                    <Text strong>
                      <CalendarOutlined /> {formatVietnamDateTime(log.contactedAt)}
                    </Text>
                    <Tag color="blue">{channel.label}</Tag>
                    <Tag color={condition.color}>{condition.label}</Tag>
                    <Tag color={outcome.color}>{outcome.label}</Tag>
                  </Space>

                  <div>
                    <Text type="secondary">Nội dung chăm sóc</Text>
                    <Paragraph style={{ margin: '2px 0 0', whiteSpace: 'pre-wrap' }}>
                      {log.careNotes || '—'}
                    </Paragraph>
                  </div>

                  <Space size={6}>
                    <UserOutlined />
                    <Text type="secondary">Người thực hiện:</Text>
                    {getPerformerName(log.performedBy)}
                  </Space>
                </Space>
              </Card>
            ),
          }
        })}
      />
    )
  }

  return (
    <Modal
      open={open}
      onCancel={handleCancel}
      title={(
        <Space>
          <HistoryOutlined style={{ color: '#1677ff' }} />
          <span>Lịch sử chăm sóc bệnh nhân</span>
        </Space>
      )}
      footer={<Button onClick={handleCancel}>Đóng</Button>}
      width={760}
      styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div>
          <Text strong>{patientLabel}</Text>
          {patientCode && <Text type="secondary"> ({patientCode})</Text>}
        </div>
        {renderContent()}
      </Space>
    </Modal>
  )
}

export default PatientCareHistoryModal
