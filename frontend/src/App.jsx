import React from 'react'
import AppRoutes from './routes/AppRoutes'
import './App.css'
import { ConfigProvider, message } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'

dayjs.locale('vi')

message.config({
  maxCount: 2,
  duration: 3,
})

function App() {
  return (
    <ConfigProvider
      locale={viVN}
      theme={{
        token: {
          colorPrimary: '#2563EB',
          fontFamily: "Inter, 'Segoe UI', Roboto, Arial, sans-serif",
          borderRadius: 12,
          colorBgContainer: '#FFFFFF',
          colorBgLayout: '#F8FAFC',
          colorText: '#111827',
          colorTextSecondary: '#64748B',
          colorBorder: '#E2E8F0',
          colorSuccess: '#16A34A',
          colorWarning: '#D97706',
          colorError: '#DC2626',
          controlHeight: 42,
        },
        components: {
          Card: {
            paddingLG: 20,
            borderRadiusLG: 12,
          },
          Button: {
            controlHeight: 42,
            fontWeight: 600,
          },
          Input: {
            controlHeight: 42,
          },
          Select: {
            controlHeight: 42,
          },
        },
      }}
    >
      <AppRoutes />
    </ConfigProvider>
  )
}

export default App
