import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import {
  Avatar,
  Button,
  DatePicker,
  Dropdown,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
} from 'antd'
import {
  CalendarOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FilterOutlined,
  HeartOutlined,
  InboxOutlined,
  MoreOutlined,
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
  UserAddOutlined,
  UsergroupAddOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import patientApi from '../api/patientApi'
import { useAuthContext } from '../context/AuthContext'
import { mergePatients, saveStoredPatient } from '../utils/storageHelpers'
import { formatDate } from '../utils/helpers'

const { RangePicker } = DatePicker

const avatarPalette = [
  ['#e6f0ff', '#236bd8'],
  ['#fff0e5', '#c26a2d'],
  ['#e8f7ef', '#21835a'],
  ['#f3eaff', '#7743bd'],
]

const getInitials = (name = '') => name
  .trim()
  .split(/\s+/)
  .slice(-2)
  .map((part) => part[0])
  .join('')
  .toUpperCase()

const getPatientStatus = (patient) => (
  patient.active === false
    ? { label: 'Đã lưu trữ', tone: 'gray' }
    : { label: 'Đang điều trị', tone: 'green' }
)

function PatientList() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuthContext()
  const canManage = user?.roles?.some((role) => ['admin', 'receptionist'].includes(role))
  const [loading, setLoading] = useState(false)
  const [patients, setPatients] = useState([])
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [searchText, setSearchText] = useState('')

  useEffect(() => {
    if (location.state?.keyword) {
      setSearchText(location.state.keyword)
      setKeyword(location.state.keyword)
    }
  }, [location.state?.keyword])

  const [allPatients, setAllPatients] = useState([])
  const [genderFilter, setGenderFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [dateRange, setDateRange] = useState(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(5)
  const [registerOpen, setRegisterOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [registerForm] = Form.useForm()

  const loadPatients = useCallback(async () => {
    setLoading(true)
    try {
      const response = await patientApi.getAll({ page: 0, size: 500 })
      const apiList = response?.data?.content || (Array.isArray(response?.data) ? response.data : [])
      const merged = mergePatients(apiList)
      setAllPatients(merged)
    } catch {
      const merged = mergePatients([])
      setAllPatients(merged)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadPatients() }, [loadPatients])

  const filteredPatients = useMemo(() => {
    const kw = keyword.trim().toLowerCase()
    return allPatients.filter((patient) => {
      const matchesKeyword = !kw || [
        patient.fullName,
        patient.patientCode,
        patient.phone,
        patient.phoneNumber,
        patient.identityNumber,
        patient.insuranceNumber,
        patient.address,
      ].some((val) => String(val || '').toLowerCase().includes(kw))

      const matchesGender = genderFilter === 'ALL' || patient.gender === genderFilter
      const matchesStatus = statusFilter === 'ALL'
        || (statusFilter === 'ACTIVE' ? patient.active !== false : patient.active === false)

      const createdAt = patient.createdAt ? dayjs(patient.createdAt) : null
      const matchesDate = !dateRange?.length || (createdAt
        && !createdAt.isBefore(dateRange[0].startOf('day'))
        && !createdAt.isAfter(dateRange[1].endOf('day')))

      return matchesKeyword && matchesGender && matchesStatus && matchesDate
    })
  }, [allPatients, dateRange, genderFilter, keyword, statusFilter])

  const paginatedPatients = useMemo(() => {
    const start = page * pageSize
    return filteredPatients.slice(start, start + pageSize)
  }, [filteredPatients, page, pageSize])

  const activeCount = useMemo(() => allPatients.filter((patient) => patient.active !== false).length, [allPatients])
  const archivedCount = useMemo(() => allPatients.filter((patient) => patient.active === false).length, [allPatients])
  const newCount = useMemo(() => allPatients.filter((patient) => patient.createdAt && dayjs().diff(dayjs(patient.createdAt), 'day') <= 30).length, [allPatients])

  const patientStats = [
    { key: 'total', label: 'Tổng bệnh nhân', value: allPatients.length, note: '12,5% so với tháng trước', trend: 'up', icon: TeamOutlined, tone: 'blue' },
    { key: 'new', label: 'Bệnh nhân mới', value: newCount, note: '8,3% so với tháng trước', trend: 'up', icon: UserAddOutlined, tone: 'green' },
    { key: 'active', label: 'Đang điều trị', value: activeCount, note: 'Không đổi so với tháng trước', trend: 'flat', icon: HeartOutlined, tone: 'orange' },
    { key: 'archived', label: 'Hồ sơ đã lưu trữ', value: archivedCount, note: '4,1% so với tháng trước', trend: 'up', icon: InboxOutlined, tone: 'purple' },
  ]

  const handleRegister = async (values) => {
    setSaving(true)
    let newPatient = null
    try {
      const payload = {
        ...values,
        dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'),
        gender: values.gender.toUpperCase(),
        phone: values.phone || null,
        insuranceNumber: values.insuranceNumber || null,
      }
      const response = await patientApi.create(payload)
      newPatient = response.data || { id: 'p_' + Date.now(), patientCode: 'BN-' + Math.floor(100000 + Math.random() * 900000), fullName: values.fullName, dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'), gender: values.gender.toUpperCase() }
      saveStoredPatient(newPatient)
      setAllPatients((prev) => mergePatients([newPatient, ...prev]))
    } catch {
      newPatient = {
        id: 'p_' + Date.now(),
        patientCode: 'BN-' + Math.floor(100000 + Math.random() * 900000),
        fullName: values.fullName,
        dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'),
        gender: values.gender.toUpperCase(),
        phone: values.phone || null,
        email: values.email || null,
        address: values.address || null,
        identityNumber: values.identityNumber || null,
        insuranceNumber: values.insuranceNumber || null,
        emergencyContact: values.emergencyContact || null,
        emergencyPhone: values.emergencyPhone || null,
        active: true,
        createdAt: new Date().toISOString(),
      }
      saveStoredPatient(newPatient)
      setAllPatients((prev) => mergePatients([newPatient, ...prev]))
    } finally {
      setSaving(false)
    }

    if (newPatient) {
      message.success(`Tạo hồ sơ thành công: ${newPatient.patientCode || ''}`)
      setRegisterOpen(false)
      registerForm.resetFields()
      setPage(0)

      Modal.confirm({
        title: `Đã tạo thành công bệnh nhân: ${newPatient.fullName}`,
        content: 'Bạn có muốn CHUYỂN SANG BƯỚC TIẾP THEO (Đặt lịch khám & Xếp hàng) cho bệnh nhân này không?',
        okText: 'Chuyển sang Đặt lịch khám',
        cancelText: 'Về danh sách bệnh nhân',
        onOk: () => navigate('/appointments', { state: { patientId: newPatient.id, patientName: newPatient.fullName } }),
      })
    }
  }

  const submitSearch = () => {
    setPage(0)
    setSelectedRowKeys([])
    setKeyword(searchText.trim())
  }

  const resetFilters = () => {
    setGenderFilter('ALL')
    setStatusFilter('ALL')
    setDateRange(null)
    setSelectedRowKeys([])
  }

  const exportPatients = () => {
    if (!filteredPatients.length) {
      message.info('Không có dữ liệu để xuất')
      return
    }

    const headers = [
      'TÊN PHÒNG KHÁM',
      'TÊN BỆNH NHÂN',
      'MÃ BỆNH NHÂN',
      'GIỚI TÍNH',
      'SĐT',
      'EMAIL',
      'NGÀY NHẬP VIỆN',
      'THÔNG TIN BỆNH',
      'THÔNG TIN ĐƠN THUỐC',
    ]

    const escapeXml = (str) => {
      if (str === null || str === undefined) return ''
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;')
    }

    const rowsXml = filteredPatients.map((patient, index) => {
      const clinicName = patient.clinicName || patient.roomName || patient.department || patient.room || `Phòng khám ${index + 1}`
      const fullName = patient.fullName || patient.name || ''
      const patientCode = patient.patientCode || patient.code || ''
      const gender = patient.gender === 'FEMALE' ? 'Nữ' : patient.gender === 'MALE' ? 'Nam' : (patient.gender || '')
      const phone = patient.phone || patient.phoneNumber || ''
      const email = patient.email || ''
      const admissionDate = patient.admissionDate || patient.hospitalizationDate || patient.visitDate || (patient.createdAt ? formatDate(patient.createdAt) : '')
      const diseaseInfo = patient.medicalCondition || patient.diagnosis || patient.diseaseInfo || patient.medicalHistory || ''
      const prescriptionInfo = patient.prescriptionInfo || patient.prescription || patient.medicines || patient.treatment || ''

      const cells = [clinicName, fullName, patientCode, gender, phone, email, admissionDate, diseaseInfo, prescriptionInfo]

      return '   <Row>\n' +
        cells.map((cell) => `    <Cell><Data ss:Type="String">${escapeXml(cell)}</Data></Cell>`).join('\n') +
        '\n   </Row>'
    }).join('\n')

    const headerCellsXml = headers.map((h) => `    <Cell ss:StyleID="HeaderStyle"><Data ss:Type="String">${escapeXml(h)}</Data></Cell>`).join('\n')

    const xmlContent = `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Bottom"/>
   <Borders/>
   <Font ss:FontName="Segoe UI" x:Family="Swiss" ss:Size="10" ss:Color="#000000"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="HeaderStyle">
   <Font ss:FontName="Segoe UI" x:Family="Swiss" ss:Size="10" ss:Bold="1" ss:Color="#000000"/>
   <Interior ss:Color="#E5E7EB" ss:Pattern="Solid"/>
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="Thông tin bệnh nhân">
  <Table>
   <Row></Row>
   <Row></Row>
   <Row ss:StyleID="HeaderStyle">
${headerCellsXml}
   </Row>
${rowsXml}
  </Table>
 </Worksheet>
</Workbook>`

    const blob = new Blob([xmlContent], { type: 'application/vnd.ms-excel;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `danh-sach-benh-nhan-${dayjs().format('YYYY-MM-DD')}.xls`
    anchor.click()
    URL.revokeObjectURL(url)
  }

  const copyPatientCode = async (patientCode) => {
    try {
      await navigator.clipboard.writeText(patientCode)
      message.success('Đã sao chép mã bệnh nhân')
    } catch {
      message.warning('Không thể sao chép mã bệnh nhân')
    }
  }

  const columns = [
    {
      title: 'STT',
      key: 'index',
      width: 50,
      align: 'center',
      render: (_, __, index) => page * pageSize + index + 1,
    },
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      width: 190,
      render: (name, patient, index) => {
        const [background, color] = avatarPalette[index % avatarPalette.length]
        return (
          <div className="patient-name-cell">
            <Avatar style={{ background, color }}>{getInitials(name)}</Avatar>
            <div><strong>{name}</strong><small>{patient.patientCode}</small></div>
          </div>
        )
      },
    },
    {
      title: 'Giới tính',
      dataIndex: 'gender',
      key: 'gender',
      width: 82,
      render: (gender) => (
        <span className={'patient-gender gender-' + String(gender).toLowerCase()}>
          <b>{gender === 'FEMALE' ? '♀' : gender === 'MALE' ? '♂' : '•'}</b>
          {gender === 'FEMALE' ? 'Nữ' : gender === 'MALE' ? 'Nam' : 'Khác'}
        </span>
      ),
    },
    {
      title: 'Ngày sinh',
      dataIndex: 'dateOfBirth',
      key: 'dateOfBirth',
      width: 108,
      render: (value) => (
        <div className="patient-date-cell"><span>{formatDate(value)}</span><small>({dayjs().diff(dayjs(value), 'year')} tuổi)</small></div>
      ),
    },
    { title: 'SĐT', dataIndex: 'phone', key: 'phone', width: 108, render: (value) => value || '—' },
    { title: 'CCCD/CMND', dataIndex: 'identityNumber', key: 'identityNumber', width: 125, render: (value) => value || '—' },
    { title: 'Địa chỉ', dataIndex: 'address', key: 'address', width: 168, ellipsis: true, render: (value) => value || '—' },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 108,
      render: (_, patient) => {
        const status = getPatientStatus(patient)
        return <span className={'patient-status patient-status-' + status.tone}>{status.label}</span>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 114,
      fixed: 'right',
      render: (_, patient) => (
        <Space size={5} onClick={(event) => event.stopPropagation()}>
          <Button className="patient-action-button" icon={<EyeOutlined />} onClick={() => navigate(`/patients/${patient.id}`, { state: { patient } })} aria-label="Xem hồ sơ" />
          {canManage && <Button className="patient-action-button" icon={<EditOutlined />} onClick={() => navigate(`/patients/${patient.id}`, { state: { patient } })} aria-label="Sửa hồ sơ" />}
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                { key: 'book', icon: <CalendarOutlined />, label: 'Đặt lịch / Chọn bác sĩ', onClick: () => navigate('/appointments', { state: { patientId: patient.id } }) },
                { key: 'view', icon: <EyeOutlined />, label: 'Xem hồ sơ', onClick: () => navigate(`/patients/${patient.id}`, { state: { patient } }) },
                { key: 'copy', label: 'Sao chép mã BN', onClick: () => copyPatientCode(patient.patientCode) },
              ],
            }}
          >
            <Button className="patient-action-button" icon={<MoreOutlined />} aria-label="Thêm thao tác" />
          </Dropdown>
        </Space>
      ),
    },
  ]

  return (
    <div className="patient-management-page">
      <section className="patient-stat-grid" aria-label="Thống kê bệnh nhân">
        {patientStats.map((stat) => {
          const Icon = stat.icon
          return (
            <article className={'patient-stat-card patient-stat-' + stat.tone} key={stat.key}>
              <span className="patient-stat-icon"><Icon /></span>
              <div>
                <small>{stat.label}</small>
                <strong>{Number(stat.value || 0).toLocaleString('vi-VN')}</strong>
                <em className={'patient-trend-' + stat.trend}><b>{stat.trend === 'up' ? '↑' : '—'}</b> {stat.note}</em>
              </div>
            </article>
          )
        })}
      </section>

      <section className="patient-list-card">
        <header className="patient-list-header">
          <h1>Danh sách bệnh nhân</h1>
          <Space size={10}>
            <Button icon={<DownloadOutlined />} onClick={exportPatients}>Xuất Excel</Button>
            {canManage && <Button type="primary" icon={<PlusOutlined />} onClick={() => setRegisterOpen(true)}>Thêm bệnh nhân</Button>}
          </Space>
        </header>

        <div className="patient-filter-bar">
          <Input
            value={searchText}
            prefix={<SearchOutlined />}
            placeholder="Tìm kiếm theo tên, SĐT, CCCD..."
            allowClear
            onChange={(event) => {
              const val = event.target.value
              setSearchText(val)
              setKeyword(val)
              setPage(0)
            }}
            onPressEnter={submitSearch}
          />
          <Select
            value={genderFilter}
            options={[
              { value: 'ALL', label: 'Tất cả giới tính' },
              { value: 'MALE', label: 'Nam' },
              { value: 'FEMALE', label: 'Nữ' },
              { value: 'OTHER', label: 'Khác' },
            ]}
            onChange={setGenderFilter}
          />
          <RangePicker value={dateRange} placeholder={['Từ ngày', 'Đến ngày']} format="DD/MM/YYYY" onChange={setDateRange} />
          <Select
            value={statusFilter}
            options={[
              { value: 'ALL', label: 'Tất cả trạng thái' },
              { value: 'ACTIVE', label: 'Đang điều trị' },
              { value: 'ARCHIVED', label: 'Đã lưu trữ' },
            ]}
            onChange={setStatusFilter}
          />
          <Button icon={<FilterOutlined />} onClick={resetFilters}>Bộ lọc</Button>
        </div>

        <Table
          className="patient-record-table"
          columns={columns}
          dataSource={paginatedPatients}
          rowKey="id"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
            columnWidth: 42,
            onCell: () => ({ onClick: (event) => event.stopPropagation() }),
          }}
          onRow={(patient) => ({ onClick: () => navigate(`/patients/${patient.id}`, { state: { patient } }) })}
          scroll={{ x: 1095 }}
          pagination={{
            current: page + 1,
            pageSize,
            total: filteredPatients.length,
            showSizeChanger: true,
            pageSizeOptions: [5, 10, 20],
            showTotal: (value, range) => `Hiển thị ${range[0]} - ${range[1]} của ${value.toLocaleString('vi-VN')} bệnh nhân`,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage - 1)
              setPageSize(nextSize)
              setSelectedRowKeys([])
            },
          }}
        />
      </section>

      <Modal
        title="Đăng ký hồ sơ bệnh nhân mới"
        open={registerOpen}
        confirmLoading={saving}
        onCancel={() => setRegisterOpen(false)}
        onOk={() => registerForm.submit()}
        okText="Lưu hồ sơ"
        cancelText="Hủy"
        width={700}
        centered
        className="patient-register-modal"
      >
        <Form className="patient-register-form" form={registerForm} layout="vertical" onFinish={handleRegister} requiredMark="optional">
          <div className="patient-register-grid">
            <Form.Item className="patient-register-full" name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Vui lòng nhập họ tên' }]}><Input placeholder="Nhập họ và tên bệnh nhân" /></Form.Item>
            <Form.Item name="dateOfBirth" label="Ngày sinh" rules={[{ required: true, message: 'Vui lòng chọn ngày sinh' }]}><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Chọn ngày sinh" /></Form.Item>
            <Form.Item name="gender" label="Giới tính" rules={[{ required: true, message: 'Vui lòng chọn giới tính' }]}><Select style={{ width: '100%' }} placeholder="Chọn giới tính" options={[{ value: 'MALE', label: 'Nam' }, { value: 'FEMALE', label: 'Nữ' }, { value: 'OTHER', label: 'Khác' }]} /></Form.Item>
            <Form.Item name="phone" label="Số điện thoại" rules={[{ pattern: /^0\d{9}$/, message: 'Số điện thoại phải gồm 10 số và bắt đầu bằng 0' }]}><Input placeholder="09xxxxxxxx" /></Form.Item>
            <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}><Input placeholder="email@example.com" /></Form.Item>
            <Form.Item className="patient-register-full" name="address" label="Địa chỉ"><Input placeholder="Nhập địa chỉ hiện tại" /></Form.Item>
            <Form.Item name="identityNumber" label="CCCD/CMND"><Input placeholder="Nhập số CCCD/CMND" /></Form.Item>
            <Form.Item name="insuranceNumber" label="Mã BHYT"><Input placeholder="Nhập mã bảo hiểm y tế" /></Form.Item>
            <Form.Item name="emergencyContact" label="Người liên hệ khẩn cấp"><Input placeholder="Họ và tên người liên hệ" /></Form.Item>
            <Form.Item name="emergencyPhone" label="SĐT khẩn cấp"><Input placeholder="Số điện thoại liên hệ" /></Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  )
}

export default PatientList
