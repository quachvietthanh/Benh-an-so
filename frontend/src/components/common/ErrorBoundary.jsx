import React from 'react'

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo)
  }

  handleReset = () => {
    localStorage.clear()
    window.location.href = '/login'
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          backgroundColor: '#f8fafc',
          fontFamily: "Inter, 'Segoe UI', Roboto, sans-serif",
          textAlign: 'center',
        }}>
          <div style={{
            maxWidth: '500px',
            backgroundColor: '#ffffff',
            padding: '32px',
            borderRadius: '16px',
            boxShadow: '0 10px 25px rgba(0, 0, 0, 0.08)',
            border: '1px solid #e2e8f0',
          }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
            <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#1e293b', marginBottom: '12px' }}>
              Đã xảy ra lỗi giao diện
            </h2>
            <p style={{ fontSize: '14px', color: '#64748b', marginBottom: '24px', lineHeight: '1.6' }}>
              Hệ thống phát hiện lỗi khi tải giao diện (hoặc dữ liệu phiên đăng nhập cũ không hợp lệ). Vui lòng thử tải lại trang hoặc xoá phiên đăng nhập để tiếp tục.
            </p>
            {this.state.error && (
              <pre style={{
                textAlign: 'left',
                backgroundColor: '#f1f5f9',
                padding: '12px',
                borderRadius: '8px',
                fontSize: '12px',
                color: '#dc2626',
                overflowX: 'auto',
                marginBottom: '24px',
                maxHeight: '120px',
              }}>
                {this.state.error.toString()}
              </pre>
            )}
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                type="button"
                onClick={this.handleReload}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '8px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Tải lại trang
              </button>
              <button
                type="button"
                onClick={this.handleReset}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#ef4444',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '8px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Xóa cache & Đăng nhập lại
              </button>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
