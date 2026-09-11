import React, { useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  DatePicker,
  Divider,
  Modal,
  Space,
  Typography,
  message,
} from 'antd'
import {
  CalendarOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import reportApi from '../../api/reportApi.js'
import { useAuthContext } from '../../context/AuthContext.jsx'
import {
  downloadCsvBlob,
  getExportErrorMessage,
  getExportFilename,
  validateExportParams,
} from '../../utils/reportExportHelpers.js'

const { RangePicker } = DatePicker
const { Text, Paragraph } = Typography

/**
 * Modal for Administrators to review and export medical record access log reports (NCL-15-CN-004 / V43).
 * Aggregates access counts per user account over a specified period.
 */
function ExportAccessLogReportModal({ open, onClose, onSuccess }) {
  const { user } = useAuthContext()
  const userRoles = useMemo(() => {
    return (user?.roles || [user?.role || '']).map((r) => String(r || '').toLowerCase().replace(/^role_/, ''))
  }, [user])
  const userPermissions = useMemo(() => {
    return (user?.permissions || []).map((p) => String(p || '').toUpperCase().replace(/^PERMISSION_/, ''))
  }, [user])

  const isAdmin = userRoles.includes('admin')
  const canExport = isAdmin || userPermissions.includes('ACCESS_LOG_REPORT_EXPORT')

  const [dateRange, setDateRange] = useState([
    dayjs().subtract(29, 'day'),
    dayjs(),
  ])
  const [exporting, setExporting] = useState(false)
  const isExportingRef = useRef(false)

  const rangePresets = [
    { label: 'Hôm nay', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
    { label: '7 ngày qua', value: [dayjs().subtract(6, 'day'), dayjs()] },
    { label: '30 ngày qua', value: [dayjs().subtract(29, 'day'), dayjs()] },
    { label: 'Tháng này', value: [dayjs().startOf('month'), dayjs().endOf('month')] },
    { label: 'Tháng trước', value: [dayjs().subtract(1, 'month').startOf('month'), dayjs().subtract(1, 'month').endOf('month')] },
    { label: '90 ngày qua', value: [dayjs().subtract(89, 'day'), dayjs()] },
  ]

  const handleExport = async () => {
    if (isExportingRef.current || exporting) return

    if (!canExport) {
      message.error('Bạn không có quyền xuất báo cáo (Yêu cầu quyền ACCESS_LOG_REPORT_EXPORT của Quản trị viên).')
      return
    }

    if (!dateRange || !dateRange[0] || !dateRange[1]) {
      message.error('Vui lòng chọn khoảng thời gian cần xuất báo cáo.')
      return
    }

    const from = dateRange[0].format('YYYY-MM-DD')
    const to = dateRange[1].format('YYYY-MM-DD')

    const validation = validateExportParams(from, to)
    if (!validation.isValid) {
      message.error(validation.message)
      return
    }

    isExportingRef.current = true
    setExporting(true)
    try {
      const response = await reportApi.exportAccessLog({ from, to })
      const disposition = response.headers?.['content-disposition']
      const filename = getExportFilename(disposition, 'ACCESS_LOG_REPORT', from, to)

      downloadCsvBlob(response.data, filename)
      message.success(`Đã xuất và tải về báo cáo: ${filename}`)
      if (onSuccess) {
        onSuccess({ from, to, filename })
      }
      onClose()
    } catch (err) {
      console.error('Lỗi xuất báo cáo nhật ký truy cập:', err)
      const errorMsg = await getExportErrorMessage(err)
      message.error(errorMsg)
    } finally {
      isExportingRef.current = false
      setExporting(false)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={(
        <Space align="center">
          <SafetyCertificateOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span style={{ fontSize: 16, fontWeight: 600 }}>Xuất báo cáo nhật ký truy cập hồ sơ bệnh án</span>
        </Space>
      )}
      width={620}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={exporting}>
          Hủy bỏ
        </Button>,
        <Button
          key="export"
          type="primary"
          icon={<DownloadOutlined />}
          loading={exporting}
          disabled={!canExport}
          onClick={handleExport}
          style={{ backgroundColor: canExport ? '#1d4ed8' : undefined }}
        >
          Xuất tệp CSV
        </Button>,
      ]}
      destroyOnClose
    >
      <div style={{ marginTop: 12 }}>
        {!canExport && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16, borderRadius: 8, background: '#fffbeb', borderColor: '#fde68a' }}
            message={<strong>Giới hạn phân quyền: Yêu cầu quyền Quản trị viên</strong>}
            description="Tài khoản hiện tại không có quyền ACCESS_LOG_REPORT_EXPORT. Tính năng xuất tệp báo cáo nhật ký truy cập hồ sơ bệnh án chỉ dành riêng cho Quản trị viên (ADMIN) hoặc tài khoản được cấp quyền giám sát an toàn y tế."
          />
        )}
        <Alert
          type="info"
          showIcon
          icon={<SafetyCertificateOutlined style={{ fontSize: 18, color: '#2563eb' }} />}
          style={{ marginBottom: 20, borderRadius: 8, background: '#eff6ff', borderColor: '#bfdbfe' }}
          message={<strong>Mục đích giám sát an toàn thông tin y tế</strong>}
          description={(
            <Paragraph style={{ margin: 0, fontSize: 13, color: '#1e3a8a' }}>
              Báo cáo tổng hợp số lượt truy cập bệnh án theo từng tài khoản nhân sự (Bác sĩ, Tiếp nhận, Quản trị viên)
              trong kỳ được chọn. Dữ liệu này dùng để lưu hồ sơ giám sát định kỳ và trình cơ quan thanh tra khi được yêu cầu.
            </Paragraph>
          )}
        />

        <div style={{ marginBottom: 16 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, color: '#1e293b' }}>
            <CalendarOutlined style={{ marginRight: 6, color: '#2563eb' }} />
            Chọn khoảng thời gian kỳ giám sát:
          </label>
          <RangePicker
            value={dateRange}
            onChange={(dates) => setDateRange(dates)}
            presets={rangePresets}
            format="DD/MM/YYYY"
            style={{ width: '100%', height: 42, borderRadius: 8 }}
            allowClear={false}
          />
          <Text type="secondary" style={{ display: 'block', marginTop: 6, fontSize: 12 }}>
            * Khoảng thời gian xuất báo cáo tối đa 366 ngày (1 năm). Định dạng tệp xuất ra là CSV với chuẩn mã hóa UTF-8 BOM hiển thị chuẩn trên Microsoft Excel.
          </Text>
        </div>

        <Divider style={{ margin: '16px 0' }} />

        <div style={{ background: '#f8fafc', padding: 12, borderRadius: 8, border: '1px dashed #cbd5e1' }}>
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <Space align="center">
              <FileExcelOutlined style={{ color: '#16a34a', fontSize: 18 }} />
              <strong>Cấu trúc dữ liệu báo cáo:</strong>
            </Space>
            <div style={{ fontSize: 12, color: '#475569', marginLeft: 24 }}>
              • <strong>Account</strong>: Tên tài khoản đăng nhập (Username)<br />
              • <strong>Full Name</strong>: Họ và tên cán bộ y tế<br />
              • <strong>Access Count</strong>: Tổng số lượt truy cập hồ sơ bệnh án trong kỳ
            </div>
            <div style={{ fontSize: 11, color: '#64748b', fontStyle: 'italic', marginTop: 4, marginLeft: 24 }}>
              Lần xuất báo cáo sẽ được hệ thống ghi nhận tự động vào Nhật ký giám sát (Audit Log) theo quy định.
            </div>
          </Space>
        </div>
      </div>
    </Modal>
  )
}

export default ExportAccessLogReportModal
