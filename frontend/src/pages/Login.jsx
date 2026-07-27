import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useAuthContext } from '../context/AuthContext'
import './login.css'

function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login, isAuthenticated } = useAuthContext()

  // Redirect if already logged in
  if (isAuthenticated) {
    navigate('/', { replace: true })
    return null
  }

  const handleSubmit = async (values) => {
    setLoading(true)
    try {
      const result = await login(values)
      if (result.success) {
        message.success('Đăng nhập thành công!')
        navigate('/', { replace: true })
      } else {
        message.error(result.message || 'Đăng nhập thất bại')
      }
    } catch (error) {
      message.error('Đã xảy ra lỗi hệ thống')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bsa2-page">
      <div className="bsa2-card">
        <svg className="bsa2-ecg" viewBox="0 0 360 40">
          <path
            className="bsa2-pulse"
            d="M0 20 L60 20 L74 20 L82 4 L92 36 L100 20 L120 20 L360 20"
            fill="none"
            stroke="#2FA8A0"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        <div className="bsa2-title">Bệnh Án Số</div>
        <p className="bsa2-sub">Đăng nhập hệ thống khám chữa bệnh</p>


        <Form
          className="bsa2-form"
          name="login"
          onFinish={handleSubmit}
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Tên đăng nhập"
              bordered={false}
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Mật khẩu"
              bordered={false}
            /> 
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={{ height: 44, fontSize: 16 }}
            >
              Đăng nhập
            </Button>
          </Form.Item>
        </Form>

        <p className="bsa2-foot">
          <Link className="bsa2-lookup-link" to="/public-lookup">
            Tra cứu lịch hẹn dành cho bệnh nhân
          </Link>
          <span>Phiên bản nội bộ · Bệnh Án Số</span>
        </p>
      </div>
    </div>
  )
}

export default Login
