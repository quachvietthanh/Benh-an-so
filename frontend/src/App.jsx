import React from 'react'
import AppRoutes from './routes/AppRoutes'
import './App.css'
import { ConfigProvider, message } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import ForceChangePasswordModal from './components/auth/ForceChangePasswordModal'

dayjs.locale('vi')

message.config({
  top: 20,
  maxCount: 2,
  duration: 3,
})

// Hook message.error để bắt chính xác stack trace khi có toast "Resource not found." hoặc 404
const originalMessageError = message.error
message.error = function (...args) {
  const content = args[0]
  const contentStr = typeof content === 'string' ? content : (content?.content || '')
  if (
    typeof contentStr === 'string' &&
    (contentStr.includes('Resource not found') ||
      contentStr.includes('404') ||
      contentStr.includes('RESOURCE_NOT_FOUND'))
  ) {
    console.error(
      '%c[STACK TRACE - message.error("Resource not found.")]',
      'background: #ef4444; color: white; padding: 4px 8px; font-size: 13px; font-weight: bold; border-radius: 4px;',
      {
        messageArgs: args,
        location: window.location.href,
        timestamp: new Date().toISOString(),
      }
    )
    console.trace('[STACK TRACE GỌI RA TOAST "Resource not found."]')
  }
  return originalMessageError.apply(this, args)
}

function App() {
  return (
    <ConfigProvider
        locale={viVN}
        theme={{
          token: {
            colorPrimary: '#2563EB',
            fontFamily: "Inter, 'Segoe UI', Roboto, Arial, sans-serif",
            borderRadius: 8,
            colorBgContainer: '#FFFFFF',
            colorBgLayout: '#F8FAFC',
            colorText: '#111827',
            colorTextSecondary: '#64748B',
            colorBorder: '#E2E8F0',
            colorSuccess: '#16A34A',
            colorWarning: '#D97706',
            colorError: '#DC2626',
            controlHeight: 38,
            controlHeightSM: 30,
            controlHeightLG: 44,
          },
          components: {
            Card: {
              paddingLG: 20,
              borderRadiusLG: 10,
            },
            Button: {
              controlHeight: 38,
              controlHeightSM: 30,
              controlHeightLG: 44,
              fontWeight: 600,
              borderRadius: 8,
              borderRadiusSM: 6,
              borderRadiusLG: 10,
              paddingInline: 16,
              paddingInlineSM: 10,
              paddingInlineLG: 20,
            },
            Input: {
              controlHeight: 38,
              controlHeightSM: 30,
              controlHeightLG: 44,
              borderRadius: 8,
            },
            Select: {
              controlHeight: 38,
              controlHeightSM: 30,
              controlHeightLG: 44,
              borderRadius: 8,
            },
            DatePicker: {
              controlHeight: 38,
              controlHeightSM: 30,
              controlHeightLG: 44,
              borderRadius: 8,
            },
            Modal: {
              borderRadiusLG: 12,
            },
          },
        }}
      >
        <ForceChangePasswordModal />
        <AppRoutes />
      </ConfigProvider>
  )
}

export default App
