import React, { useState } from 'react'
import {
  Alert,
  Button,
  DatePicker,
  Divider,
  Form,
  Modal,
  Radio,
  Space,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../../api/reportApi'
import {
  downloadCsvBlob,
  getExportErrorMessage,
  getExportFilename,
  validateExportParams,
} from '../../utils/reportExportHelpers'

const { Text } = Typography
const { RangePicker } = DatePicker

const DATE_PRESETS = [
  { label: 'Hôm nay', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
  { label: '7 ngày qua', value: [dayjs().subtract(6, 'day').startOf('day'), dayjs().endOf('day')] },
  { label: '30 ngày qua', value: [dayjs().subtract(29, 'day').startOf('day'), dayjs().endOf('day')] },
  { label: 'Tháng này', value: [dayjs().startOf('month'), dayjs().endOf('month')] },
]

export default function ExportAccessLogReportModal({
  open = false,
  onClose,
  onSuccess,
}) {
  const [range, setRange] = useState([
    dayjs().subtract(29, 'day').startOf('day'),
    dayjs().endOf('day'),
  ])
  const [exporting, setExporting] = useState(false)

  const handleRangePresetChange = (e) => {
    const val = e.target.value
    if (val === 'today') {
      setRange([dayjs().startOf('day'), dayjs().endOf('day')])
    } else if (val === '7d') {
      setRange([dayjs().subtract(6, 'day').startOf('day'), dayjs().endOf('day')])
    } else if (val === '30d') {
      setRange([dayjs().subtract(29, 'day').startOf('day'), dayjs().endOf('day')])
    } else if (val === 'month') {
      setRange([dayjs().startOf('month'), dayjs().endOf('month')])
    }
  }

  const handleExport = async () => {
    if (exporting) return

    if (!range || !range[0] || !range[1]) {
      message.error('Vui lòng chọn khoảng thời gian cần xuất báo cáo.')
      return
    }

    const fromStr = range[0].format('YYYY-MM-DD')
    const toStr = range[1].format('YYYY-MM-DD')

    const validation = validateExportParams({ from: fromStr, to: toStr })
    if (!validation.isValid) {
      message.error(validation.message || validation.errorMessage)
      return
    }

    setExporting(true)
    try {
      const response = await reportApi.exportAccessLog({
        from: fromStr,
        to: toStr,
      })

      const disposition = response.headers?.['content-disposition']
      const filename = getExportFilename(disposition, 'ACCESS_LOG_REPORT', fromStr, toStr)
      downloadCsvBlob(response.data, filename)

      message.success(`Đã xuất báo cáo ${filename} thành công!`)
      if (onSuccess) {
        onSuccess()
      }
      if (onClose) {
        onClose()
      }
    } catch (err) {
      console.error('Lỗi xuất báo cáo nhật ký truy cập:', err)
      const errorMsg = await getExportErrorMessage(err)
      message.error(errorMsg)
    } finally {
      setExporting(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={exporting ? undefined : onClose}
      title={
        <Space align="center">
          <FileExcelOutlined style={{ color: '#16a34a', fontSize: 20 }} />
          <span style={{ fontWeight: 600 }}>Xuất Báo Cáo Nhật Ký Truy Cập Bệnh Án (CSV)</span>
        </Space>
      }
      footer={[
        <Button key="cancel" onClick={onClose} disabled={exporting}>
          Hủy bỏ
        </Button>,
        <Button
          key="export"
          type="primary"
          icon={<DownloadOutlined />}
          loading={exporting}
          onClick={handleExport}
          style={{ background: '#16a34a', borderColor: '#16a34a' }}
        >
          {exporting ? 'Đang xuất tệp...' : 'Tải xuống tệp CSV'}
        </Button>,
      ]}
      destroyOnClose
      width={560}
    >
      <div style={{ marginTop: 16 }}>
        <Alert
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
          message="Phạm vi dữ liệu báo cáo"
          description="Báo cáo được trích xuất trực tiếp từ máy chủ theo định dạng CSV chuẩn UTF-8 (BOM). Dữ liệu bao gồm các thông tin kiểm toán: thời gian, tài khoản, vai trò, loại hành động, mã hồ sơ bệnh án và địa chỉ IP."
          style={{ marginBottom: 20 }}
        />

        <Form layout="vertical">
          <Form.Item label={<Text strong>Khoảng thời gian nhanh</Text>}>
            <Radio.Group onChange={handleRangePresetChange} defaultValue="30d">
              <Radio.Button value="today">Hôm nay</Radio.Button>
              <Radio.Button value="7d">7 ngày qua</Radio.Button>
              <Radio.Button value="30d">30 ngày qua</Radio.Button>
              <Radio.Button value="month">Tháng này</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            label={
              <Space>
                <CalendarOutlined style={{ color: '#2563eb' }} />
                <Text strong>Tùy chọn khoảng thời gian giám sát (Tối đa 366 ngày)</Text>
              </Space>
            }
            required
          >
            <RangePicker
              value={range}
              onChange={(dates) => setRange(dates)}
              format="DD/MM/YYYY"
              presets={DATE_PRESETS}
              disabledDate={(current) => current && current > dayjs().endOf('day')}
              style={{ width: '100%' }}
              allowClear={false}
            />
          </Form.Item>
        </Form>

        <Divider style={{ margin: '16px 0 12px 0' }} />

        <div style={{ fontSize: 13, color: '#64748b' }}>
          <p style={{ margin: 0 }}>
            * Chú ý: Cần quyền <strong>ACCESS_LOG_REPORT_EXPORT</strong> (Quản trị viên) để thực hiện thao tác này.
          </p>
        </div>
      </div>
    </Modal>
  )
}
