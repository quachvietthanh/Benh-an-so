import React from 'react'
import { Spin } from 'antd'
import { MedicineBoxOutlined } from '@ant-design/icons'

/**
 * Enhanced Loading Component
 * Supports multiple modes: Fullpage, Container, Overlay, Table Skeleton, Card Skeleton
 */
function Loading({
  tip = 'Đang tải dữ liệu...',
  subtip,
  fullPage = false,
  type = 'spinner',
  size = 'large',
  minHeight = 220,
  rows = 5,
  cols = 4,
  cardCount = 4,
  spinning = true,
  children,
}) {
  // If used as a wrapper with children
  if (children) {
    return (
      <div style={{ position: 'relative', width: '100%' }}>
        {children}
        {spinning && (
          <div className="overlay-loading-wrapper" aria-busy="true">
            <Spin size={size} />
            {tip && <div className="container-loading-tip">{tip}</div>}
          </div>
        )}
      </div>
    )
  }

  // Full page modal / overlay mode
  if (fullPage || type === 'fullscreen') {
    return (
      <div className="fullpage-loading-wrapper" role="status" aria-live="polite">
        <div className="fullpage-loading-card">
          <div className="loading-brand-icon">
            <MedicineBoxOutlined />
          </div>
          <Spin size="large" />
          <div className="fullpage-loading-tip">{tip}</div>
          {subtip ? (
            <div className="fullpage-loading-subtip">{subtip}</div>
          ) : (
            <div className="fullpage-loading-subtip">Hệ thống đang xử lý và tải thông tin...</div>
          )}
        </div>
      </div>
    )
  }

  // Skeleton Table Mode
  if (type === 'table') {
    return (
      <div className="skeleton-table-wrapper" aria-busy="true" aria-label={tip}>
        <div className="skeleton-table-header">
          {Array.from({ length: cols }).map((_, idx) => (
            <div
              key={`th-${idx}`}
              className="skeleton-shimmer"
              style={{
                height: 28,
                flex: idx === 0 ? 1 : idx === cols - 1 ? 1 : 2,
                borderRadius: 4,
              }}
            />
          ))}
        </div>
        {Array.from({ length: rows }).map((_, rIdx) => (
          <div key={`tr-${rIdx}`} className="skeleton-table-row">
            {Array.from({ length: cols }).map((_, cIdx) => (
              <div
                key={`td-${rIdx}-${cIdx}`}
                className="skeleton-shimmer"
                style={{
                  height: 20,
                  flex: cIdx === 0 ? 1 : cIdx === cols - 1 ? 1 : 2,
                  opacity: 0.85 - (rIdx * 0.08),
                  borderRadius: 4,
                }}
              />
            ))}
          </div>
        ))}
      </div>
    )
  }

  // Skeleton Cards Grid Mode
  if (type === 'cards') {
    return (
      <div className="skeleton-card-grid" aria-busy="true" aria-label={tip}>
        {Array.from({ length: cardCount }).map((_, idx) => (
          <div key={`card-${idx}`} className="skeleton-card-item">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div className="skeleton-shimmer" style={{ width: 80, height: 16 }} />
              <div className="skeleton-shimmer" style={{ width: 32, height: 32, borderRadius: 8 }} />
            </div>
            <div className="skeleton-shimmer" style={{ width: 120, height: 28, margin: '6px 0' }} />
            <div className="skeleton-shimmer" style={{ width: 160, height: 14 }} />
          </div>
        ))}
      </div>
    )
  }

  // Overlay Mode
  if (type === 'overlay') {
    return (
      <div className="overlay-loading-wrapper" aria-busy="true">
        <Spin size={size} />
        {tip && <div className="container-loading-tip">{tip}</div>}
      </div>
    )
  }

  // Default Inline / Container Spinner Mode
  return (
    <div
      className="container-loading-wrapper"
      style={{ minHeight: typeof minHeight === 'number' ? `${minHeight}px` : minHeight }}
      role="status"
      aria-live="polite"
    >
      <Spin size={size} />
      {tip && <div className="container-loading-tip">{tip}</div>}
      {subtip && <div className="fullpage-loading-subtip" style={{ marginTop: 4 }}>{subtip}</div>}
    </div>
  )
}

export default Loading
