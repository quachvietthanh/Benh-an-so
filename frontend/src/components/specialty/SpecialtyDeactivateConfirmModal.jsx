import React, { useEffect, useState } from 'react'
import { Alert, Button, Modal, Spin, Typography, message } from 'antd'
import { ExclamationCircleOutlined, StopOutlined, WarningOutlined } from '@ant-design/icons'
import specialtyApi from '../../api/specialtyApi.js'
import { getApiErrorMessage } from '../../utils/apiError.js'

const { Text, Paragraph } = Typography

export default function SpecialtyDeactivateConfirmModal({ open, specialty, onCancel, onSuccess }) {
  const [loading, setLoading] = useState(false)
  const [fetchingDetail, setFetchingDetail] = useState(false)
  const [specialtyDetail, setSpecialtyDetail] = useState(null)
  const [errorMessage, setErrorMessage] = useState(null)

  const isDefaultSpecialty = specialty?.code === 'GENERAL' || specialty?.id === 'f0000000-0000-0000-0000-000000000001'

  useEffect(() => {
    if (!open || !specialty) {
      setSpecialtyDetail(null)
      setErrorMessage(null)
      return
    }

    const fetchDetail = async () => {
      setFetchingDetail(true)
      try {
        const res = await specialtyApi.getById(specialty.id)
        setSpecialtyDetail(res.data)
      } catch {
        // Fallback to passed specialty object
        setSpecialtyDetail(specialty)
      } finally {
        setFetchingDetail(false)
      }
    }

    fetchDetail()
  }, [open, specialty])

  const handleDeactivate = async () => {
    if (isDefaultSpecialty) {
      message.error('Không thể ngừng dùng chuyên khoa mặc định hệ thống (GENERAL).')
      return
    }

    setLoading(true)
    setErrorMessage(null)

    try {
      // Send confirm=true to satisfy TC-02 when in use
      await specialtyApi.deactivate(specialty.id, true)
      message.success(`Đã ngừng dùng chuyên khoa "${specialty.name}" thành công.`)
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Không thể ngừng dùng chuyên khoa.')
      setErrorMessage(msg)
    } finally {
      setLoading(false)
    }
  }

  const assignedDoctorsCount = specialtyDetail?.doctors?.length || 0
  const activeTemplateCount = specialtyDetail?.activeTemplateCount || 0
  const assignedRoomsCount = specialtyDetail?.rooms?.length || 0
  const isInUse = assignedDoctorsCount > 0 || activeTemplateCount > 0

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#dc2626' }}>
          <ExclamationCircleOutlined style={{ fontSize: 20 }} />
          <span>Xác nhận ngừng dùng chuyên khoa</span>
        </div>
      }
      open={open}
      onCancel={onCancel}
      footer={[
        <Button
          key="cancel"
          size="large"
          style={{ minWidth: 100, height: 42, borderRadius: 8, fontSize: 15 }}
          onClick={onCancel}
        >
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          danger
          size="large"
          style={{ minWidth: 160, height: 42, borderRadius: 8, fontSize: 15, fontWeight: 600 }}
          disabled={isDefaultSpecialty || fetchingDetail}
          loading={loading}
          onClick={handleDeactivate}
          icon={<StopOutlined style={{ fontSize: 16 }} />}
        >
          Xác nhận ngừng dùng
        </Button>,
      ]}
      destroyOnClose
    >
      {fetchingDetail ? (
        <div style={{ textAlign: 'center', padding: '32px 0' }}>
          <Spin tip="Đang kiểm tra dữ liệu phụ thuộc..." />
        </div>
      ) : (
        <div>
          {isDefaultSpecialty && (
            <Alert
              type="error"
              showIcon
              message="Chuyên khoa mặc định hệ thống"
              description="Không thể ngừng dùng chuyên khoa mặc định hệ thống (GENERAL). Chuyên khoa này bắt buộc phải luôn hoạt động để duy trì các nghiệp vụ cơ bản."
              style={{ marginBottom: 16 }}
            />
          )}

          {errorMessage && (
            <Alert
              type="error"
              showIcon
              message="Lỗi thao tác"
              description={errorMessage}
              style={{ marginBottom: 16 }}
            />
          )}

          <Paragraph>
            Bạn đang chuẩn bị ngừng dùng chuyên khoa: <Text strong>{specialty?.name}</Text> (Mã:{' '}
            <Text code>{specialty?.code}</Text>).
          </Paragraph>

          {isInUse ? (
            <Alert
              type="warning"
              showIcon
              icon={<WarningOutlined style={{ fontSize: 22 }} />}
              message="Cảnh báo: Chuyên khoa đang được liên kết sử dụng"
              description={
                <div style={{ marginTop: 8 }}>
                  <p style={{ marginBottom: 6 }}>
                    Hệ thống ghi nhận chuyên khoa này hiện đang có:
                  </p>
                  <ul style={{ paddingLeft: 20, margin: '4px 0 10px 0' }}>
                    {assignedDoctorsCount > 0 && (
                      <li>
                        <Text strong>{assignedDoctorsCount}</Text> bác sĩ đang được phân công phụ trách.
                      </li>
                    )}
                    {assignedRoomsCount > 0 && (
                      <li>
                        <Text strong>{assignedRoomsCount}</Text> phòng khám bệnh liên kết.
                      </li>
                    )}
                    {activeTemplateCount > 0 && (
                      <li>
                        <Text strong>{activeTemplateCount}</Text> mẫu bệnh án chuyên khoa đang hoạt động.
                      </li>
                    )}
                  </ul>
                  <p style={{ margin: 0, color: '#b45309', fontWeight: 500 }}>
                    Lưu ý: Khi ngừng dùng, chuyên khoa sẽ không thể chọn khi tạo lịch hẹn mới, mẫu bệnh án mới hoặc phân công tiếp đón. Các dữ liệu lịch sử trước đây vẫn được lưu trữ nguyên vẹn.
                  </p>
                </div>
              }
              style={{ marginBottom: 16 }}
            />
          ) : (
            <Paragraph type="secondary">
              Chuyên khoa này hiện không có bác sĩ hoặc mẫu bệnh án nào đang hoạt động. Bạn có thể ngừng dùng an toàn bất kỳ lúc nào.
            </Paragraph>
          )}

          <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 0 }}>
            Hành động này có thể được hoàn tác bằng cách bấm &quot;Kích hoạt lại&quot; trong danh sách.
          </Paragraph>
        </div>
      )}
    </Modal>
  )
}
