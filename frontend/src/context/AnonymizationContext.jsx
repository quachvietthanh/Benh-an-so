import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { message } from 'antd'
import systemConfigApi from '../api/systemConfigApi'
import { invalidatePatientDataCache } from '../utils/storageHelpers'
import { useAuthContext } from './AuthContext'

const AnonymizationContext = createContext(null)

export const AnonymizationProvider = ({ children }) => {
  const { isAuthenticated, user } = useAuthContext()
  const [anonymizationEnabled, setAnonymizationEnabled] = useState(false)
  const [updatedAt, setUpdatedAt] = useState(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [hasInitialized, setHasInitialized] = useState(false)

  const fetchAnonymizationStatus = useCallback(async () => {
    // Chỉ gọi khi người dùng đã đăng nhập vào hệ thống
    if (!localStorage.getItem('token')) {
      setAnonymizationEnabled(false)
      setHasInitialized(true)
      return
    }

    setLoading(true)
    try {
      const res = await systemConfigApi.getAnonymizationStatus()
      if (res?.data) {
        setAnonymizationEnabled(Boolean(res.data.enabled))
        setUpdatedAt(res.data.updatedAt || null)
      }
    } catch (err) {
      // 403 nghĩa là người dùng không có quyền SYSTEM_CONFIG_READ (ví dụ lễ tân hoặc bệnh nhân)
      // ta giữ giá trị an toàn mặc định là false mà không ném lỗi làm crash trang
      if (err?.response?.status !== 403 && err?.response?.status !== 401) {
        console.warn('[AnonymizationContext] Không thể lấy trạng thái ẩn danh:', err)
      }
    } finally {
      setLoading(false)
      setHasInitialized(true)
    }
  }, [])

  useEffect(() => {
    if (isAuthenticated) {
      fetchAnonymizationStatus()
    } else {
      setAnonymizationEnabled(false)
      setUpdatedAt(null)
      setHasInitialized(true)
    }
  }, [isAuthenticated, fetchAnonymizationStatus])

  const toggleAnonymization = async (targetEnabled) => {
    setSaving(true)
    try {
      const res = await systemConfigApi.updateAnonymizationStatus(targetEnabled)
      const confirmedState = Boolean(res?.data?.enabled)
      const confirmedTime = res?.data?.updatedAt || new Date().toISOString()

      // Cập nhật trạng thái theo đúng response Backend trả về
      setAnonymizationEnabled(confirmedState)
      setUpdatedAt(confirmedTime)

      // Xóa toàn bộ cache dữ liệu bệnh nhân ngay sau khi đổi trạng thái
      invalidatePatientDataCache(confirmedState)

      if (confirmedState) {
        message.success('Đã bật chế độ ẩn danh dữ liệu bệnh nhân (chế độ trình diễn).')
      } else {
        message.success('Đã tắt chế độ ẩn danh dữ liệu bệnh nhân.')
      }

      return { success: true, enabled: confirmedState, updatedAt: confirmedTime }
    } catch (err) {
      console.error('[AnonymizationContext] Lỗi cập nhật chế độ ẩn danh:', err)
      const errorMsg = err?.response?.data?.message || 'Không thể cập nhật chế độ ẩn danh. Vui lòng thử lại.'
      message.error(errorMsg)
      throw err
    } finally {
      setSaving(false)
    }
  }

  return (
    <AnonymizationContext.Provider
      value={{
        anonymizationEnabled,
        updatedAt,
        loading,
        saving,
        hasInitialized,
        fetchAnonymizationStatus,
        toggleAnonymization,
      }}
    >
      {children}
    </AnonymizationContext.Provider>
  )
}

export const useAnonymization = () => {
  const context = useContext(AnonymizationContext)
  if (!context) {
    return {
      anonymizationEnabled: false,
      updatedAt: null,
      loading: false,
      saving: false,
      hasInitialized: true,
      fetchAnonymizationStatus: () => Promise.resolve(),
      toggleAnonymization: () => Promise.reject(new Error('AnonymizationProvider not mounted')),
    }
  }
  return context
}

export default AnonymizationContext
