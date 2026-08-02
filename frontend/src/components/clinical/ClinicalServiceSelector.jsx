import React, { useEffect, useMemo, useState } from 'react'
import {
  Card,
  Input,
  Tabs,
  Checkbox,
  Table,
  Button,
  Tag,
  Space,
  Empty,
  Typography,
  Row,
  Col,
  Tooltip,
} from 'antd'
import {
  SearchOutlined,
  PlusOutlined,
  DeleteOutlined,
  CheckOutlined,
  ExperimentOutlined,
  FileImageOutlined,
  HeartOutlined,
} from '@ant-design/icons'
import { demoClinicalCatalog } from '../../mock-data/mockData'
import clinicalOrderApi from '../../api/clinicalOrderApi'

const { Text, Title } = Typography

export const ClinicalServiceSelector = ({ selectedServices = [], onChange }) => {
  const [activeTab, setActiveTab] = useState('ALL')
  const [searchText, setSearchText] = useState('')
  const [catalog, setCatalog] = useState(demoClinicalCatalog)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let isMounted = true
    const fetchServices = async () => {
      setLoading(true)
      try {
        const response = await clinicalOrderApi.searchServices({ size: 100 })
        const content = response.data?.content || response.data
        if (Array.isArray(content) && content.length > 0 && isMounted) {
          const mapped = content.map((item) => {
            const rawType = item.serviceType || 'LAB_TEST'
            let category = 'LABORATORY'
            let categoryName = 'Xét nghiệm'
            if (rawType === 'IMAGING') {
              category = 'IMAGING'
              categoryName = 'Chẩn đoán hình ảnh'
            } else if (rawType === 'OTHER') {
              category = 'FUNCTIONAL'
              categoryName = 'Thăm dò chức năng'
            }
            return {
              id: item.id || `cls-${item.serviceCode}`,
              code: item.serviceCode || 'DV-01',
              name: item.serviceName || item.name,
              category,
              categoryName,
              price: item.price || 120000,
              room: item.room || (category === 'LABORATORY' ? 'Phòng Xét nghiệm' : category === 'IMAGING' ? 'Phòng CĐHA' : 'Phòng TDCN'),
              description: item.description || '',
            }
          })
          setCatalog(mapped)
        }
      } catch {
        // Fallback to demoClinicalCatalog
      } finally {
        if (isMounted) setLoading(false)
      }
    }
    fetchServices()
    return () => { isMounted = false }
  }, [])

  const categories = useMemo(() => [
    { key: 'ALL', label: 'Tất cả dịch vụ', icon: <ExperimentOutlined /> },
    { key: 'LABORATORY', label: 'Xét nghiệm', icon: <ExperimentOutlined /> },
    { key: 'IMAGING', label: 'Chẩn đoán hình ảnh', icon: <FileImageOutlined /> },
    { key: 'FUNCTIONAL', label: 'Thăm dò chức năng', icon: <HeartOutlined /> },
  ], [])

  const filteredCatalog = useMemo(() => {
    const kw = searchText.trim().toLowerCase()
    return catalog.filter((item) => {
      const matchesCategory = activeTab === 'ALL' || item.category === activeTab
      const matchesKw = !kw || [item.code, item.name, item.categoryName, item.room]
        .some((val) => String(val || '').toLowerCase().includes(kw))
      return matchesCategory && matchesKw
    })
  }, [catalog, activeTab, searchText])

  const selectedMap = useMemo(() => {
    const map = new Map()
    const safeSelected = Array.isArray(selectedServices) ? selectedServices : []
    safeSelected.forEach((item) => {
      if (item) map.set(item.serviceId || item.id, item)
    })
    return map
  }, [selectedServices])

  const handleToggleService = (service) => {
    const safeSelected = Array.isArray(selectedServices) ? selectedServices : []
    const serviceKey = service.id
    if (selectedMap.has(serviceKey)) {
      const updated = safeSelected.filter((s) => (s?.serviceId || s?.id) !== serviceKey)
      onChange(updated)
    } else {
      const newItem = {
        serviceId: service.id,
        serviceCode: service.code,
        serviceName: service.name,
        categoryName: service.categoryName,
        price: service.price,
        quantity: 1,
        note: '',
        status: 'PENDING',
      }
      onChange([...safeSelected, newItem])
    }
  }

  const handleUpdateItemNote = (serviceKey, note) => {
    const safeSelected = Array.isArray(selectedServices) ? selectedServices : []
    const updated = safeSelected.map((item) => {
      if ((item?.serviceId || item?.id) === serviceKey) {
        return { ...item, note }
      }
      return item
    })
    onChange(updated)
  }

  const handleRemoveService = (serviceKey) => {
    const safeSelected = Array.isArray(selectedServices) ? selectedServices : []
    const updated = safeSelected.filter((s) => (s?.serviceId || s?.id) !== serviceKey)
    onChange(updated)
  }

  const totalAmount = useMemo(() => {
    const safeSelected = Array.isArray(selectedServices) ? selectedServices : []
    return safeSelected.reduce((sum, item) => sum + (Number(item?.price || 0) * Number(item?.quantity || 1)), 0)
  }, [selectedServices])

  return (
    <div style={{ background: '#fff', borderRadius: 8 }}>
      <Row gutter={[16, 16]}>
        {/* Left Column: Service Catalog Browser */}
        <Col xs={24} lg={14}>
          <Card
            title={
              <Space>
                <ExperimentOutlined style={{ color: '#1890ff' }} />
                <span>Danh mục dịch vụ cận lâm sàng</span>
              </Space>
            }
            size="small"
            style={{ borderRadius: 8 }}
          >
            <Input
              prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="Tìm theo tên dịch vụ, mã XN, CDHA..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
              style={{ marginBottom: 12 }}
            />

            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              size="small"
              items={categories.map((cat) => ({
                key: cat.key,
                label: (
                  <Space>
                    {cat.icon}
                    <span>{cat.label}</span>
                  </Space>
                ),
              }))}
            />

            <div style={{ maxHeight: 360, overflowY: 'auto', paddingRight: 4 }}>
              {filteredCatalog.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không tìm thấy dịch vụ phù hợp" />
              ) : (
                filteredCatalog.map((item) => {
                  const isSelected = selectedMap.has(item.id)
                  return (
                    <div
                      key={item.id}
                      onClick={() => handleToggleService(item)}
                      style={{
                        padding: '10px 12px',
                        marginBottom: 8,
                        borderRadius: 6,
                        border: isSelected ? '1px solid #91d5ff' : '1px solid #f0f0f0',
                        background: isSelected ? '#e6f7ff' : '#fafafa',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        transition: 'all 0.2s',
                      }}
                    >
                      <div style={{ flex: 1, paddingRight: 12 }}>
                        <Space size={6}>
                          <Tag color="blue" style={{ fontWeight: 600 }}>{item.code}</Tag>
                          <Text strong style={{ fontSize: 14 }}>{item.name}</Text>
                        </Space>
                        <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>
                          {item.room} • {item.description}
                        </div>
                      </div>

                      <div style={{ textAlign: 'right' }}>
                        <div style={{ color: '#cf1322', fontWeight: 600, fontSize: 14 }}>
                          {item.price.toLocaleString('vi-VN')} đ
                        </div>
                        <Button
                          type={isSelected ? 'primary' : 'default'}
                          size="small"
                          icon={isSelected ? <CheckOutlined /> : <PlusOutlined />}
                          style={{ marginTop: 4 }}
                        >
                          {isSelected ? 'Đã chọn' : 'Chọn'}
                        </Button>
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </Card>
        </Col>

        {/* Right Column: Selected Services & Notes */}
        <Col xs={24} lg={10}>
          <Card
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Space>
                  <CheckOutlined style={{ color: '#52c41a' }} />
                  <span>Dịch vụ chỉ định ({selectedServices.length})</span>
                </Space>
                {selectedServices.length > 0 && (
                  <Button type="link" danger size="small" onClick={() => onChange([])}>
                    Xóa tất cả
                  </Button>
                )}
              </div>
            }
            size="small"
            style={{ borderRadius: 8, height: '100%', display: 'flex', flexDirection: 'column' }}
          >
            <div style={{ flex: 1, maxHeight: 380, overflowY: 'auto' }}>
              {selectedServices.length === 0 ? (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Chưa chọn dịch vụ cận lâm sàng nào. Nhấn vào danh sách bên trái để chọn."
                />
              ) : (
                selectedServices.map((item, index) => {
                  const key = item.serviceId || item.id
                  return (
                    <div
                      key={key}
                      style={{
                        padding: 10,
                        marginBottom: 10,
                        borderRadius: 6,
                        border: '1px solid #d9d9d9',
                        background: '#fff',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <Text strong style={{ color: '#1890ff' }}>#{index + 1}. [{item.serviceCode || 'CLS'}]</Text>{' '}
                          <Text strong>{item.serviceName}</Text>
                        </div>
                        <Tooltip title="Xóa dịch vụ này">
                          <Button
                            type="text"
                            danger
                            size="small"
                            icon={<DeleteOutlined />}
                            onClick={() => handleRemoveService(key)}
                          />
                        </Tooltip>
                      </div>

                      <Input
                        placeholder="Ghi chú vị trí chụp, yêu cầu đặc biệt..."
                        size="small"
                        value={item.note || ''}
                        onChange={(e) => handleUpdateItemNote(key, e.target.value)}
                        style={{ marginTop: 6, fontSize: 12 }}
                      />

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
                        <Tag color="purple" style={{ margin: 0 }}>{item.categoryName || 'Chỉ định'}</Tag>
                        <Text strong style={{ color: '#cf1322' }}>
                          {Number(item.price || 0).toLocaleString('vi-VN')} đ
                        </Text>
                      </div>
                    </div>
                  )
                })
              )}
            </div>

            {/* Total Footer Summary */}
            <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 12, marginTop: 8 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Text style={{ fontSize: 14 }}>Tổng chi phí tạm tính:</Text>
                <Title level={4} style={{ color: '#cf1322', margin: 0 }}>
                  {totalAmount.toLocaleString('vi-VN')} đ
                </Title>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default ClinicalServiceSelector
