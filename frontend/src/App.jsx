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
      <AppRoutes />
    </ConfigProvider>
  )
}

export default App
