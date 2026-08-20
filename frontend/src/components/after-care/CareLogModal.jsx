import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, DatePicker, Form, Input, Modal, Select, Typography } from 'antd'
import followUpReminderApi from '../../api/followUpReminderApi'
import {
  CONTACT_CHANNELS,
  CONTACT_OUTCOMES,
  PATIENT_CONDITIONS,
  formatVietnamDate,
  formatVietnamDateTime,
  getAftercareErrorMessage,
  getReminderTypeMeta,
  normalizePage,
  shortId,
  vietnamNowForPicker,
} from '../../utils/aftercareHelpers'

const { Text } = Typography

const responseData = (response) => response?.data ?? response

const getPatientId = (patient) => patient?.id || patient?.patientId || patient?.patient?.id

const getPatientName = (patient) => (
  patient?.fullName || patient?.patientName || patient?.patient?.fullName || 'Bệnh nhân'
)

const getPatientCode = (patient) => (
  patient?.patientCode || patient?.code || patient?.patient?.patientCode
)

const getPatientPhone = (patient) => patient?.phone || patient?.patient?.phone

const getVisitId = (visit) => visit?.visitId || visit?.id || visit?.visit?.id

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
  || visit?.queueDate
  || visit?.visit?.completedAt
  || visit?.visit?.visitAt
)

const getDoctorName = (visit) => (
  visit?.doctorName
  || visit?.doctor?.fullName
  || visit?.visit?.doctorName
  || 'Chưa có thông tin bác sĩ'
)

const getReminderLabel = (reminder) => {
  const parts = [
    `ID ${shortId(reminder?.id)}`,
    formatVietnamDate(reminder?.followUpDate),
    getReminderTypeMeta(reminder?.reminderType).label,
  ]
  return parts.join(' · ')
}

const loadAllPatientReminders = async (patientId) => {
  const firstResponse = await followUpReminderApi.search({ patientId, page: 0, size: 100 })
  const firstPage = normalizePage(responseData(firstResponse), 0, 100)
  const remainingPageNumbers = Array.from(
    { length: Math.max(0, firstPage.totalPages - 1) },
    (_, index) => index + 1,
  )
  const remainingResponses = await Promise.all(remainingPageNumbers.map((page) => (
    followUpReminderApi.search({ patientId, page, size: 100 })
  )))
  return [
    ...firstPage.content,
    ...remainingResponses.flatMap((response, index) => (
      normalizePage(responseData(response), index + 1, 100).content
    )),
  ]
}

function CareLogModal({
  open,
  onCancel,
  onSubmit,
  submitting = false,
  todayPatients = [],
  todayVisits = [],
  loadingTodayVisits = false,
  todayVisitsError = null,
  sourceReminder = null,
  canReadReminders = false,
}) {
  const [form] = Form.useForm()
  const selectedPatientId = Form.useWatch('patientId', form)
  const selectedVisitId = Form.useWatch('visitId', form)
  const patientCondition = Form.useWatch('patientCondition', form)
  const [reminders, setReminders] = useState([])
  const [remindersLoading, setRemindersLoading] = useState(false)
  const [remindersError, setRemindersError] = useState(null)
  const contextRequestRef = useRef(0)
  const lockedToReminder = Boolean(sourceReminder)

  const loadPatientReminders = useCallback(async (patientId) => {
    if (!patientId || !canReadReminders) {
      setReminders([])
      setRemindersLoading(false)
      setRemindersError(null)
      return
    }

    const requestId = ++contextRequestRef.current
    setReminders([])
    setRemindersError(null)
    setRemindersLoading(true)

    try {
      const loadedReminders = await loadAllPatientReminders(patientId)
      if (contextRequestRef.current !== requestId) return
      setReminders(loadedReminders)
    } catch (error) {
      if (contextRequestRef.current !== requestId) return
      setRemindersError(getAftercareErrorMessage(error, 'Không thể tải lịch nhắc của bệnh nhân.'))
    } finally {
      if (contextRequestRef.current === requestId) setRemindersLoading(false)
    }
  }, [canReadReminders])

  useEffect(() => {
    if (!open) {
      contextRequestRef.current += 1
      return
    }

    form.resetFields()
    form.setFieldsValue({
      patientId: sourceReminder?.patientId,
      visitId: sourceReminder?.visitId || undefined,
      reminderId: sourceReminder?.id || undefined,
      contactChannel: 'PHONE',
      contactedAt: vietnamNowForPicker(),
      contactOutcome: 'REACHED',
      patientCondition: 'STABLE',
      careNotes: undefined,
    })

    if (sourceReminder?.patientId) {
      loadPatientReminders(sourceReminder.patientId)
    } else {
      setReminders([])
    }
  }, [form, loadPatientReminders, open, sourceReminder])

  const patientOptions = useMemo(() => {
    const options = (Array.isArray(todayPatients) ? todayPatients : [])
      .filter((patient) => getPatientId(patient))
      .map((patient) => {
        const pId = getPatientId(patient)
        const pName = getPatientName(patient)
        const pCode = getPatientCode(patient)
        const phone = getPatientPhone(patient)
        const details = [pCode, phone].filter(Boolean).join(' · ')

        return {
          value: pId,
          label: `${pName}${details ? ` (${details})` : ''}`,
          searchText: [pName, pCode, phone].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN'),
        }
      })

    if (sourceReminder?.patientId && !options.some((o) => String(o.value) === String(sourceReminder.patientId))) {
      options.push({
        value: sourceReminder.patientId,
        label: `Bệnh nhân · ID ${shortId(sourceReminder.patientId)}`,
      })
    }

    return options
  }, [todayPatients, sourceReminder])

  const patientVisits = useMemo(() => {
    if (!selectedPatientId) return []
    return (Array.isArray(todayVisits) ? todayVisits : [])
      .filter((visit) => (
        String(getVisitPatientId(visit)) === String(selectedPatientId)
      ))
  }, [selectedPatientId, todayVisits])

  const visitOptions = useMemo(() => {
    const options = patientVisits.map((visit) => {
      const vCode = getVisitCode(visit)
      const vTime = formatVietnamDateTime(getVisitDateTime(visit))
      const docName = getDoctorName(visit)

      return {
        value: getVisitId(visit),
        searchText: [vCode, vTime, docName].join(' ').toLocaleLowerCase('vi-VN'),
        label: `${vCode} | ${vTime} | ${docName}`,
      }
    })

    if (
      sourceReminder?.visitId
      && String(getVisitPatientId(sourceReminder)) === String(selectedPatientId)
      && !options.some((o) => String(o.value) === String(sourceReminder.visitId))
    ) {
      options.push({
        value: sourceReminder.visitId,
        label: `Lượt khám · ID ${shortId(sourceReminder.visitId)}`,
      })
    }

    return options
  }, [patientVisits, selectedPatientId, sourceReminder])

  const reminderOptions = useMemo(() => reminders
    .filter((reminder) => reminder?.id)
    .map((reminder) => ({ value: reminder.id, label: getReminderLabel(reminder) })), [reminders])

  const handlePatientChange = (patientId) => {
    form.setFieldsValue({ patientId, visitId: undefined, reminderId: undefined })
    if (patientId) {
      loadPatientReminders(patientId)
    } else {
      setReminders([])
    }
  }

  const handleVisitChange = (visitId) => {
    const reminderId = form.getFieldValue('reminderId')
    const selectedReminder = reminders.find((reminder) => reminder.id === reminderId)
    if (selectedReminder?.visitId && selectedReminder.visitId !== visitId) {
      form.setFieldValue('reminderId', undefined)
    }
  }

  const handleReminderChange = (reminderId) => {
    const selectedReminder = reminders.find((reminder) => reminder.id === reminderId)
    if (selectedReminder?.visitId) {
      form.setFieldValue('visitId', selectedReminder.visitId)
    }
  }

  const handleCancel = () => {
    if (submitting) return
    contextRequestRef.current += 1
    onCancel?.()
  }

  const showClinicalWarning = ['COMPLICATIONS', 'NEEDS_REVISIT'].includes(patientCondition)

  return (
    <Modal
      title="GHI NHẬN CHĂM SÓC SAU KHÁM"
      open={open}
      width={760}
      onCancel={handleCancel}
      onOk={() => form.submit()}
      okText="Lưu ghi nhận"
      cancelText="Hủy"
      confirmLoading={submitting}
      okButtonProps={{ loading: submitting }}
      cancelButtonProps={{ disabled: submitting }}
      closable={!submitting}
      keyboard={!submitting}
      maskClosable={!submitting}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark
        onFinish={onSubmit}
      >
        <Form.Item
          name="patientId"
          label="Bệnh nhân"
          rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân.' }]}
        >
          <Select
            showSearch
            allowClear={!lockedToReminder}
            loading={loadingTodayVisits}
            disabled={lockedToReminder || submitting}
            placeholder="Chọn bệnh nhân"
            optionFilterProp="searchText"
            options={patientOptions}
            onChange={handlePatientChange}
            notFoundContent={loadingTodayVisits ? 'Đang tải bệnh nhân...' : 'Không có bệnh nhân hoàn tất khám trong hôm nay.'}
          />
        </Form.Item>

        <Form.Item
          name="visitId"
          label="Lượt khám liên quan"
          extra="Không bắt buộc nếu ghi nhận không gắn với một lượt khám cụ thể."
        >
          <Select
            showSearch
            allowClear={!lockedToReminder}
            loading={loadingTodayVisits}
            disabled={lockedToReminder || submitting}
            placeholder="Chọn lượt khám (không bắt buộc)"
            optionFilterProp="searchText"
            options={visitOptions}
            onChange={handleVisitChange}
            notFoundContent={
              loadingTodayVisits
                ? 'Đang tải lượt khám...'
                : (selectedPatientId
                  ? 'Bệnh nhân không có lượt khám hoàn tất hôm nay.'
                  : 'Vui lòng chọn bệnh nhân trước.')
            }
          />
        </Form.Item>
        {todayVisitsError && (
          <Alert
            type="warning"
            showIcon
            message="Không tải được lượt khám hôm nay"
            description={getAftercareErrorMessage(todayVisitsError, 'Không thể tải dữ liệu lượt khám từ Backend.')}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form.Item
          name="reminderId"
          label="Lịch nhắc liên quan"
          extra={canReadReminders || lockedToReminder
            ? 'Không bắt buộc nếu ghi nhận không gắn với một lịch nhắc cụ thể.'
            : 'Bạn không có quyền đọc danh sách lịch nhắc; có thể tiếp tục ghi nhận mà không chọn lịch nhắc.'}
        >
          <Select
            showSearch
            allowClear={!lockedToReminder}
            loading={remindersLoading}
            disabled={lockedToReminder || !canReadReminders || submitting}
            placeholder="Chọn lịch nhắc (không bắt buộc)"
            optionFilterProp="label"
            options={reminderOptions}
            onChange={handleReminderChange}
            notFoundContent={remindersLoading ? 'Đang tải lịch nhắc...' : 'Bệnh nhân chưa có lịch nhắc'}
          />
        </Form.Item>

        <Form.Item
          name="contactChannel"
          label="Kênh liên hệ"
          rules={[{ required: true, message: 'Vui lòng chọn kênh liên hệ.' }]}
        >
          <Select
            disabled={submitting}
            placeholder="Chọn kênh liên hệ"
            options={CONTACT_CHANNELS}
          />
        </Form.Item>

        <Form.Item
          name="contactedAt"
          label="Thời gian liên hệ"
          rules={[{ required: true, message: 'Vui lòng chọn thời gian liên hệ.' }]}
        >
          <DatePicker
            showTime={{ format: 'HH:mm' }}
            format="DD/MM/YYYY HH:mm"
            placeholder="Chọn thời gian liên hệ"
            disabled={submitting}
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item
          name="contactOutcome"
          label="Kết quả liên hệ"
          rules={[{ required: true, message: 'Vui lòng chọn kết quả liên hệ.' }]}
        >
          <Select
            disabled={submitting}
            placeholder="Chọn kết quả liên hệ"
            options={CONTACT_OUTCOMES.map(({ value, label }) => ({ value, label }))}
          />
        </Form.Item>

        <Form.Item
          name="patientCondition"
          label="Tình trạng sức khỏe"
          rules={[{ required: true, message: 'Vui lòng chọn tình trạng sức khỏe.' }]}
        >
          <Select
            disabled={submitting}
            placeholder="Chọn tình trạng sức khỏe"
            options={PATIENT_CONDITIONS.map(({ value, label }) => ({ value, label }))}
          />
        </Form.Item>

        {showClinicalWarning && (
          <Alert
            type="warning"
            showIcon
            message="Cảnh báo lâm sàng"
            description="Bệnh nhân có biến chứng hoặc cần tái khám. Vui lòng ghi chú chi tiết và tạo Lịch nhắc tái khám nếu cần."
            style={{ marginBottom: 16 }}
          />
        )}

        <Form.Item
          name="careNotes"
          label="Ghi chú chăm sóc"
        >
          <Input.TextArea
            rows={4}
            maxLength={1000}
            showCount
            disabled={submitting}
            placeholder="Nhập ghi chú chi tiết về tình trạng bệnh nhân, tư vấn đã đưa ra, phản hồi..."
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default CareLogModal
