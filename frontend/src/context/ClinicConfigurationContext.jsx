import React, { createContext, useState, useContext, useEffect, useCallback } from 'react'
import systemApi from '../api/systemApi'

const ClinicConfigurationContext = createContext(null)

export const ClinicConfigurationProvider = ({ children }) => {
  const [clinicConfig, setClinicConfig] = useState(null)
  const [loadingConfig, setLoadingConfig] = useState(false)

  const refreshClinicConfig = useCallback(async () => {
    setLoadingConfig(true)
    try {
      const res = await systemApi.clinic()
      if (res?.data) {
        setClinicConfig(res.data)
      }
    } catch (err) {
      console.warn('[ClinicConfigurationContext] Lỗi tải cấu hình phòng khám từ Backend:', err)
    } finally {
      setLoadingConfig(false)
    }
  }, [])

  useEffect(() => {
    refreshClinicConfig()
  }, [refreshClinicConfig])

  return (
    <ClinicConfigurationContext.Provider
      value={{
        clinicConfig,
        loadingConfig,
        refreshClinicConfig,
      }}
    >
      {children}
    </ClinicConfigurationContext.Provider>
  )
}

export const useClinicConfig = () => {
  const context = useContext(ClinicConfigurationContext)
  if (!context) {
    return {
      clinicConfig: null,
      loadingConfig: false,
      refreshClinicConfig: () => {},
    }
  }
  return context
}

export default ClinicConfigurationContext
