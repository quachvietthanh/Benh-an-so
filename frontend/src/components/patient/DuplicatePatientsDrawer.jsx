import React, { useEffect, useState, useMemo } from 'react'
import {
  Drawer,
  Button,
  Table,
  Tag,
  Space,
  Typography,
  Alert,
  Empty,
  Spin,
  Card,
  Input,
  Tooltip,
} from 'antd'
import {
  AuditOutlined,
  ReloadOutlined,
  MergeCellsOutlined,
  UsergroupDeleteOutlined,
  SearchOutlined,
  CheckCircleOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons'
import patientApi from '../../api/patientApi'
import { formatDate } from '../../utils/helpers'

const { Title, Text, Paragraph } = Typography

export default function DuplicatePatientsDrawer({
  open,
  onClose,
  onSelectMerge,
  canMerge = true,
}) {
  const [loading, setLoading] = useState(false)
  const [groups, setGroups] = useState([])
  const [searchFilter, setSearchFilter] = useState('')

  const fetchDuplicates = async () => {
    setLoading(true)
    try {
      const res = await patientApi.getDuplicates()
      const list = Array.isArray(res?.data) ? res.data : []
      setGroups(list)
    } catch (err) {
      setGroups([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open) {
      fetchDuplicates()
      setSearchFilter('')
    }
  }, [open])

  const filteredGroups = useMemo(() => {
    const kw = searchFilter.trim().toLowerCase()
    if (!kw) return groups

    return groups.filter((g) => {
      const nameMatch = String(g.fullName || '').toLowerCase().includes(kw)
      const phoneMatch = String(g.phone || '').includes(kw)
      const candidateMatch = (g.candidates || []).some(
        (c) =>
          String(c.patientCode || '').toLowerCase().includes(kw) ||
          String(c.fullName || '').toLowerCase().includes(kw)
      )
      return nameMatch || phoneMatch || candidateMatch
    })
  }, [groups, searchFilter])

  const totalDuplicateRecords = useMemo(() => {
    return groups.reduce((acc, g) => acc + (g.candidates?.length || 0), 0)
  }, [groups])

  return (
    <Drawer
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <UsergroupDeleteOutlined style={{ color: '#2563eb', fontSize: 20 }} />
          <span>Rà soát hồ sơ bệnh nhân nghi trùng lặp</span>
        </div>
      }
      open={open}
      onClose={onClose}
      width={720}
      extra={
        <Button
          icon={<ReloadOutlined />}
          onClick={fetchDuplicates}
          loading={loading}
          size="small"
        >
          Làm mới
        </Button>
      }
    >
      <Alert
        type="info"
        showIcon
        message="Hệ thống tự động phát hiện hồ sơ nghi trùng lặp"
        description="Dựa trên thuật toán đối soát tự động theo Họ tên, Ngày sinh và Số điện thoại. Vui lòng kiểm tra và hợp nhất các hồ sơ trùng lặp để đảm bảo tính liên tục của lịch sử bệnh án."
        style={{ marginBottom: 16 }}
      />

      <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
        <Input
          prefix={<SearchOutlined style={{ color: '#9ca3af' }} />}
          placeholder="Tìm theo họ tên, SĐT hoặc mã BN..."
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
          allowClear
          style={{ maxWidth: 360 }}
        />
        <Text type="secondary" style={{ fontSize: 13 }}>
          Tìm thấy <strong>{filteredGroups.length}</strong> nhóm ({totalDuplicateRecords} hồ sơ)
        </Text>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" />
          <div style={{ marginTop: 12, color: '#6b7280' }}>Đang quét hồ sơ trùng lặp trong hệ thống...</div>
        </div>
      ) : filteredGroups.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            searchFilter
              ? 'Không tìm thấy nhóm hồ sơ trùng khớp với từ khóa tìm kiếm.'
              : 'Tuyệt vời! Không phát hiện hồ sơ bệnh nhân nào bị trùng lặp.'
          }
        />
      ) : (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          {filteredGroups.map((group, index) => {
            const candidates = group.candidates || []
            return (
              <Card
                key={index}
                size="small"
                style={{
                  border: '1px solid #fed7aa',
                  borderRadius: 8,
                  background: '#fffaf5',
                }}
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Space>
                      <Tag color="orange" style={{ fontWeight: 600 }}>
                        Nhóm #{index + 1}
                      </Tag>
                      <Text strong style={{ fontSize: 14 }}>
                        {group.fullName || '---'}
                      </Text>
                      <Text type="secondary">({formatDate(group.dateOfBirth)})</Text>
                      {group.phone && <Tag color="blue">{group.phone}</Tag>}
                    </Space>
                    <Tag color="volcano">{candidates.length} hồ sơ trùng</Tag>
                  </div>
                }
                extra={
                  candidates.length >= 2 && canMerge ? (
                    <Button
                      type="primary"
                      size="small"
                      icon={<AuditOutlined />}
                      onClick={() => {
                        // Default candidate 0 as target, candidate 1 as source
                        onSelectMerge(candidates[0], candidates[1])
                      }}
                      style={{ background: '#ea580c', borderColor: '#ea580c' }}
                    >
                      Gộp nhóm này
                    </Button>
                  ) : null
                }
              >
                <Table
                  dataSource={candidates}
                  rowKey={(r) => r.id || r.patientId || r.patientCode}
                  pagination={false}
                  size="small"
                  columns={[
                    {
                      title: 'Mã BN',
                      dataIndex: 'patientCode',
                      key: 'patientCode',
                      render: (text) => <Text strong copyable>{text}</Text>,
                      width: 110,
                    },
                    {
                      title: 'Họ và tên',
                      dataIndex: 'fullName',
                      key: 'fullName',
                    },
                    {
                      title: 'Giới tính',
                      dataIndex: 'gender',
                      key: 'gender',
                      width: 80,
                      render: (val) => (val === 'MALE' ? 'Nam' : val === 'FEMALE' ? 'Nữ' : 'Khác'),
                    },
                    {
                      title: 'Trạng thái',
                      key: 'status',
                      width: 120,
                      render: (_, r) => {
                        if (r.isMerged || r.status === 'MERGED') {
                          return <Tag color="magenta">Đã gộp</Tag>
                        }
                        if (r.active === false) {
                          return <Tag color="default">Đã lưu trữ</Tag>
                        }
                        return <Tag color="green">Hoạt động</Tag>
                      },
                    },
                    {
                      title: 'Thao tác',
                      key: 'action',
                      width: 140,
                      render: (_, r, candIndex) => {
                        if (r.isMerged) return <Text type="secondary">Đã gộp</Text>
                        if (!canMerge) return null

                        const other = candidates.find((c, i) => i !== candIndex && !c.isMerged)
                        if (!other) return null

                        return (
                          <Tooltip title={`Chọn làm hồ sơ chính giữ lại, gộp ${other.patientCode} vào đây`}>
                            <Button
                              type="link"
                              size="small"
                              style={{ padding: 0 }}
                              onClick={() => onSelectMerge(r, other)}
                            >
                              Giữ làm chính <ArrowRightOutlined />
                            </Button>
                          </Tooltip>
                        )
                      },
                    },
                  ]}
                />
              </Card>
            )
          })}
        </Space>
      )}
    </Drawer>
  )
}
