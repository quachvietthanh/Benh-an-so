import React, { useMemo, useState } from 'react'
import { Card, Col, Empty, Input, Row, Skeleton, Tag, Typography } from 'antd'
import {
  AppstoreOutlined,
  CheckCircleFilled,
  SearchOutlined,
} from '@ant-design/icons'
import { getSpecialtyTheme } from '../../constants/specialtyThemeConstants'

const { Text, Title } = Typography

function SpecialtySelector({
  specialties = [],
  selectedSpecialty = null,
  onSelectSpecialty,
  loading = false,
}) {
  const [searchQuery, setSearchQuery] = useState('')

  const filteredSpecialties = useMemo(() => {
    if (!searchQuery.trim()) return specialties
    const q = searchQuery.trim().toLowerCase()
    return specialties.filter(
      (s) =>
        (s.name && s.name.toLowerCase().includes(q)) ||
        (s.code && s.code.toLowerCase().includes(q)) ||
        (s.description && s.description.toLowerCase().includes(q))
    )
  }, [specialties, searchQuery])

  return (
    <div className="specialty-selector-container">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={5} style={{ margin: 0, color: '#1e3a8a' }}>
            <AppstoreOutlined style={{ marginRight: 8 }} />
            Bước 1: Chọn chuyên khoa khám
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Vui lòng chọn chuyên khoa phù hợp với nhu cầu thăm khám của bạn
          </Text>
        </div>
        <Input
          placeholder="Tìm kiếm chuyên khoa..."
          prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ width: 240, borderRadius: 8 }}
          allowClear
        />
      </div>

      {loading ? (
        <Row gutter={[16, 16]}>
          {[1, 2, 3, 4, 5, 6].map((k) => (
            <Col xs={24} sm={12} md={8} key={k}>
              <Card style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}>
                <Skeleton active avatar paragraph={{ rows: 1 }} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : filteredSpecialties.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: '32px 0', borderRadius: 10 }}>
          <Empty
            description="Không tìm thấy chuyên khoa nào phù hợp."
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </Card>
      ) : (
        <Row gutter={[16, 16]} style={{ display: 'flex', flexWrap: 'wrap' }}>
          {filteredSpecialties.map((specialty, idx) => {
            const isSelected = selectedSpecialty?.id === specialty.id
            const theme = getSpecialtyTheme(specialty.code, idx)
            const IconComp = theme.IconComponent

            return (
              <Col xs={24} sm={12} md={8} key={specialty.id || idx} style={{ display: 'flex' }}>
                <Card
                  hoverable
                  onClick={() => onSelectSpecialty(specialty)}
                  style={{
                    width: '100%',
                    borderRadius: 14,
                    border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                    background: isSelected ? '#eff6ff' : '#ffffff',
                    transition: 'all 0.2s ease',
                    position: 'relative',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    boxShadow: isSelected
                      ? '0 8px 20px rgba(37, 99, 235, 0.12)'
                      : '0 2px 6px rgba(0, 0, 0, 0.02)',
                  }}
                  bodyStyle={{
                    padding: '18px 20px',
                    display: 'flex',
                    flexDirection: 'column',
                    flex: 1,
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 10 }}>
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          borderRadius: 10,
                          background: theme.bg,
                          color: theme.text,
                          border: `1px solid ${theme.border}`,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: 19,
                          flexShrink: 0,
                        }}
                      >
                        {IconComp ? <IconComp /> : null}
                      </div>

                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 6 }}>
                          <Text
                            strong
                            style={{
                              fontSize: 15,
                              color: isSelected ? '#1e40af' : '#1e293b',
                              lineHeight: 1.35,
                            }}
                          >
                            {specialty.name}
                          </Text>
                          {isSelected && (
                            <CheckCircleFilled style={{ color: '#2563eb', fontSize: 18, flexShrink: 0, marginTop: 2 }} />
                          )}
                        </div>

                        <div style={{ marginTop: 5 }}>
                          <span
                            style={{
                              fontSize: 11,
                              fontWeight: 600,
                              padding: '2px 8px',
                              borderRadius: 4,
                              background: theme.bg,
                              color: theme.text,
                              border: `1px solid ${theme.border}`,
                              letterSpacing: '0.2px',
                              display: 'inline-block',
                            }}
                          >
                            {specialty.code}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div style={{ minHeight: 38, marginTop: 6 }}>
                      <Text
                        type="secondary"
                        style={{
                          fontSize: 12.5,
                          lineHeight: 1.45,
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                          overflow: 'hidden',
                          color: '#64748b',
                        }}
                      >
                        {specialty.description || 'Khám và tư vấn chuyên khoa'}
                      </Text>
                    </div>
                  </div>

                  <div
                    style={{
                      marginTop: 12,
                      paddingTop: 10,
                      borderTop: isSelected ? '1px solid #bfdbfe' : '1px solid #f1f5f9',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      fontSize: 12,
                      fontWeight: 500,
                      color: isSelected ? '#2563eb' : '#94a3b8',
                    }}
                  >
                    <span>{isSelected ? 'Đã chọn chuyên khoa này' : 'Bấm để chọn khoa'}</span>
                    <span style={{ fontSize: 13 }}>➔</span>
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      )}
    </div>
  )
}

export default SpecialtySelector
