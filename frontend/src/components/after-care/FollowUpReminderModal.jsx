import React, { useEffect, useMemo, useRef } from 'react'
import { Alert, DatePicker, Form, Input, Modal, Select, Spin, Typography } from 'antd'
import {
  REMINDER_TYPES,
  formatVietnamDateTime,
  getAftercareErrorMessage,
  getVietnamDateKey,
  isUuid,
  isTodayVisitSelectionValid,
} from '../../utils/aftercareHelpers'

const { Text } = Typography

const EMPTY_TODAY_MESSAGE = 'Chưa có bệnh nhân hoàn tất khám trong hôm nay.'

const getPatientId = (patient) => patient?.id || patient?.patientId || patient?.patient?.id

const getPatientName = (patient) => (
  patient?.fullName || patient?.patientName || patient?.patient?.fullName || 'Bệnh nhân'
)

const getPatientCode = (patient) => (
  patient?.patientCode || patient?.code || patient?.patient?.patientCode
)

const getPatientPhone = (patient) => patient?.phone || patient?.patient?.phone

const getVisitId = (visit) => visit?.visitId || visit?.visit?.id

const getVisitPatientId = (visit) => (
  visit?.patientId || visit?.patient?.id || visit?.visit?.patientId
)

const getVisitCode = (visit) => (
  visit?.visitCode || visit?.visit?.visitCode || 'Chưa có mã lượt khám'
)

const getVisitDateTime = (visit) => (
  visit?.completedAt
  || visit?.visitAt
  || visit?.checkedInAt
  || visit?.createdAt
  || visit?.visit?.completedAt
  || visit?.visit?.visitAt
)

const getDoctorName = (visit) => (
  visit?.doctorName
  || visit?.doctor?.fullName
  || visit?.visit?.doctorName
  || 'Chưa có thông tin bác sĩ'
)

function FollowUpReminderModal({
  open,
  onCancel,
  onSubmit,
  submitting,
  todayPatients,
  todayVisits,
  loadingTodayVisits,
  todayVisitsError,
  presetPatientId,
}) {
  const [form] = Form.useForm()
  const selectedPatientId = Form.useWatch('patientId', form)
  const selectedVisitId = Form.useWatch('visitId', form)
  const finishLockRef = useRef(false)
  const wasSubmittingRef = useRef(false)
  const wasOpenRef = useRef(false)
  const presetAppliedRef = useRef(false)

  const patientOptions = useMemo(() => (
    (Array.isArray(todayPatients) ? todayPatients : [])
      .filter((patient) => getPatientId(patient))
      .map((patient) => {
        const patientId = getPatientId(patient)
        const patientName = getPatientName(patient)
        const patientCode = getPatientCode(patient)
        const phone = getPatientPhone(patient)
        const details = [patientCode, phone].filter(Boolean).join(' · ')

        return {
          value: patientId,
          label: `${patientName}${details ? ` (${details})` : ''}`,
          searchText: [patientName, patientCode, phone]
            .filter(Boolean)
            .join(' ')
            .toLocaleLowerCase('vi-VN'),
        }
      })
  ), [todayPatients])

  const patientVisits = useMemo(() => (
    (Array.isArray(todayVisits) ? todayVisits : [])
      .filter((visit) => (
        String(getVisitPatientId(visit)) === String(selectedPatientId)
        && isUuid(getVisitId(visit))
      ))
  ), [selectedPatientId, todayVisits])

  const visitOptions = useMemo(() => patientVisits.map((visit) => {
    const visitCode = getVisitCode(visit)
    const visitDateTime = formatVietnamDateTime(getVisitDateTime(visit))
    const doctorName = getDoctorName(visit)

    return {
      value: getVisitId(visit),
      searchText: [visitCode, visitDateTime, doctorName]
        .join(' ')
        .toLocaleLowerCase('vi-VN'),
      label: (
        <div style={{ padding: '3px 0', lineHeight: 1.35 }}>
          <Text strong>{visitCode}</Text>
          <Text type="secondary"> | {visitDateTime} | {doctorName}</Text>
        </div>
      ),
    }
  }), [patientVisits])

  useEffect(() => {
    if (wasSubmittingRef.current && !submitting) finishLockRef.current = false
    if (!open) finishLockRef.current = false
    wasSubmittingRef.current = submitting
  }, [open, submitting])

  useEffect(() => {
    if (open && !wasOpenRef.current) {
      form.resetFields()
      presetAppliedRef.current = false
    }
    if (!open) presetAppliedRef.current = false
    wasOpenRef.current = open
  }, [form, open])

  useEffect(() => {
    if (
      !open
      || loadingTodayVisits
      || todayVisitsError
      || presetAppliedRef.current
    ) return

    presetAppliedRef.current = true
    const presetIsAvailable = patientOptions.some(
      (option) => String(option.value) === String(presetPatientId),
    )
    if (presetIsAvailable) form.setFieldValue('patientId', presetPatientId)
  }, [form, loadingTodayVisits, open, patientOptions, presetPatientId, todayVisitsError])

  useEffect(() => {
    if (
      !open
      || loadingTodayVisits
      || !selectedPatientId
    ) return

    if (todayVisitsError) {
      form.setFieldsValue({ patientId: undefined, visitId: undefined })
      return
    }

    const patientStillAvailable = patientOptions.some(
      (option) => String(option.value) === String(selectedPatientId),
    )
    if (!patientStillAvailable) {
      form.setFieldsValue({ patientId: undefined, visitId: undefined })
    }
  }, [
    form,
    loadingTodayVisits,
    open,
    patientOptions,
    selectedPatientId,
    todayVisitsError,
  ])

  useEffect(() => {
    if (
      !open
      || loadingTodayVisits
      || todayVisitsError
      || !selectedVisitId
    ) return

    if (!isTodayVisitSelectionValid(todayVisits, selectedPatientId, selectedVisitId)) {
      form.setFieldValue('visitId', undefined)
    }
  }, [
    form,
    loadingTodayVisits,
    open,
    selectedPatientId,
    selectedVisitId,
    todayVisits,
    todayVisitsError,
  ])

  const handlePatientChange = (patientId) => {
    form.setFieldsValue({ patientId, visitId: undefined })
  }

  const handleFinish = (values) => {
    if (submitting || finishLockRef.current) return
    finishLockRef.current = true
    onSubmit?.(values)
  }

  const handleCancel = () => {
    if (!submitting) onCancel?.()
  }

  const selectedVisit = patientVisits.find(
    (visit) => String(getVisitId(visit)) === String(selectedVisitId),
  )
  const minimumFollowUpDate = getVietnamDateKey(getVisitDateTime(selectedVisit))
  const sourceUnavailable = Boolean(loadingTodayVisits || todayVisitsError)
  const noTodayPatients = !loadingTodayVisits && !todayVisitsError && patientOptions.length === 0
  const todayVisitsErrorDescription = todayVisitsError?.response?.status === 403
    ? 'Bạn không có quyền đọc lượt khám.'
    : getAftercareErrorMessage(
      todayVisitsError,
      'Không thể tải dữ liệu lượt khám hôm nay. Vui lòng thử lại.',
    )

  return (
    <Modal
      title="TẠO LỊCH NHẮC TÁI KHÁM"
      open={open}
      width={680}
      onCancel={handleCancel}
      onOk={() => {
        if (!submitting && !finishLockRef.current) form.submit()
      }}
      okText="Tạo lịch nhắc"
      cancelText="Hủy"
      confirmLoading={submitting}
      okButtonProps={{
        disabled: submitting || sourceUnavailable || noTodayPatients,
      }}
      cancelButtonProps={{ disabled: submitting }}
      closable={!submitting}
      keyboard={!submitting}
      maskClosable={!submitting}
      destroyOnClose
    >
      {loadingTodayVisits && (
        <Alert
          type="info"
          showIcon
          message="Đang tải các lượt khám hoàn tất trong hôm nay..."
          style={{ marginBottom: 16 }}
        />
      )}

      {todayVisitsError && (
        <Alert
          type="error"
          showIcon
          message="Không thể tải các lượt khám hoàn tất trong hôm nay"
          description={todayVisitsErrorDescription}
          style={{ marginBottom: 16 }}
        />
      )}

      {noTodayPatients && (
        <Alert
          type="info"
          showIcon
          message={EMPTY_TODAY_MESSAGE}
          style={{ marginBottom: 16 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        initialValues={{ reminderType: 'REVISIT' }}
        onFinish={handleFinish}
      >
        <Form.Item
          name="patientId"
          label="Bệnh nhân"
          rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân.' }]}
        >
          <Select
            showSearch
            allowClear
            loading={loadingTodayVisits}
            disabled={sourceUnavailable || noTodayPatients || submitting}
            placeholder="Chọn bệnh nhân hoàn tất khám hôm nay"
            filterOption={(input, option) => (
              option?.searchText?.includes(input.trim().toLocaleLowerCase('vi-VN'))
            )}
            options={patientOptions}
            notFoundContent={loadingTodayVisits
              ? <Spin size="small" />
              : (todayVisitsError ? 'Không thể tải dữ liệu bệnh nhân hôm nay' : EMPTY_TODAY_MESSAGE)}
            onChange={handlePatientChange}
          />
        </Form.Item>

        <Form.Item
          name="visitId"
          label="Lượt khám"
          rules={[
            { required: true, message: 'Vui lòng chọn lượt khám.' },
            {
              validator: (_, value) => {
                if (!value || isTodayVisitSelectionValid(todayVisits, selectedPatientId, value)) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('Lượt khám không còn trong danh sách hoàn tất hôm nay.'))
              },
            },
          ]}
        >
          <Select
            showSearch
            loading={loadingTodayVisits}
            disabled={!selectedPatientId || sourceUnavailable || submitting}
            placeholder="Chọn lượt khám hoàn tất hôm nay"
            filterOption={(input, option) => (
              option?.searchText?.includes(input.trim().toLocaleLowerCase('vi-VN'))
            )}
            options={visitOptions}
            listHeight={320}
            notFoundContent={loadingTodayVisits ? <Spin size="small" /> : 'Không có lượt khám hoàn tất hôm nay'}
          />
        </Form.Item>

        {!sourceUnavailable && selectedPatientId && visitOptions.length === 0 && (
          <Alert
            type="info"
            showIcon
            message="Bệnh nhân không có lượt khám hoàn tất trong hôm nay."
            style={{ marginBottom: 16 }}
          />
        )}

        {!sourceUnavailable && selectedVisit && (
          <Alert
            type={selectedVisit.doctorInstructions ? 'success' : 'info'}
            showIcon
            message={selectedVisit.doctorInstructions ? 'Chỉ định / dặn dò từ bác sĩ' : 'Thông tin lượt khám'}
            description={selectedVisit.doctorInstructions || 'Backend sẽ tự động xác thực chỉ định dặn dò của bác sĩ khi tạo lịch nhắc.'}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form.Item
          name="followUpDate"
          label="Ngày tái khám"
          dependencies={['visitId']}
          rules={[
            { required: true, message: 'Vui lòng chọn ngày tái khám.' },
            {
              validator: (_, value) => {
                if (!value) return Promise.resolve()
                const todayKey = getVietnamDateKey(new Date())
                const dateKey = value.format('YYYY-MM-DD')
                if (dateKey < todayKey) {
                  return Promise.reject(new Error('Ngày tái khám không được trong quá khứ.'))
                }
                if (minimumFollowUpDate && dateKey < minimumFollowUpDate) {
                  return Promise.reject(new Error('Ngày tái khám không được trước ngày của lượt khám.'))
                }
                return Promise.resolve()
              },
            },
          ]}
        >
          <DatePicker
            style={{ width: '100%' }}
            format="DD/MM/YYYY"
            placeholder="Chọn ngày tái khám"
            disabled={submitting}
            disabledDate={(current) => {
              if (!current) return false
              const todayKey = getVietnamDateKey(new Date())
              const dateKey = current.format('YYYY-MM-DD')
              const minKey = minimumFollowUpDate || todayKey
              return dateKey < minKey
            }}
          />
        </Form.Item>

        <Form.Item
          name="remindAt"
          label="Thời điểm gửi nhắc"
          rules={[{ required: true, message: 'Vui lòng chọn thời điểm gửi nhắc.' }]}
        >
          <DatePicker
            showTime={{ format: 'HH:mm' }}
            style={{ width: '100%' }}
            format="DD/MM/YYYY HH:mm"
            placeholder="Chọn ngày và giờ gửi nhắc"
            disabled={submitting}
          />
        </Form.Item>

        <Form.Item
          name="reminderType"
          label="Loại nhắc"
          rules={[{ required: true, message: 'Vui lòng chọn loại nhắc.' }]}
        >
          <Select
            options={REMINDER_TYPES}
            placeholder="Chọn loại nhắc"
            disabled={submitting}
          />
        </Form.Item>

        <Form.Item
          name="notes"
          label="Ghi chú lịch nhắc"
          rules={[{ max: 500, message: 'Ghi chú lịch nhắc không được vượt quá 500 ký tự.' }]}
        >
          <Input.TextArea
            rows={3}
            maxLength={500}
            showCount
            placeholder="Nhập ghi chú dành riêng cho lịch nhắc (nếu có)"
            disabled={submitting}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default FollowUpReminderModal
