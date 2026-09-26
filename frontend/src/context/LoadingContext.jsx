import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import loadingManager from '../utils/loadingManager'

const LoadingContext = createContext({
  isLoading: false,
  activeRequests: 0,
  tip: 'Đang tải dữ liệu...',
  showLoading: () => {},
  hideLoading: () => {},
  setCustomTip: () => {},
})

export function LoadingProvider({ children }) {
  const [loadingState, setLoadingState] = useState({
    isLoading: loadingManager.isLoading,
    activeRequests: loadingManager.count,
  })
  const [tip, setTip] = useState('Đang tải dữ liệu...')

  useEffect(() => {
    const unsubscribe = loadingManager.subscribe((state) => {
      setLoadingState(state)
    })
    return () => unsubscribe()
  }, [])

  const contextValue = useMemo(() => ({
    isLoading: loadingState.isLoading,
    activeRequests: loadingState.activeRequests,
    tip,
    showLoading: (customTip) => {
      if (customTip) setTip(customTip)
      loadingManager.start()
    },
    hideLoading: () => {
      loadingManager.stop()
    },
    setCustomTip: (newTip) => {
      setTip(newTip)
    },
  }), [loadingState, tip])

  return (
    <LoadingContext.Provider value={contextValue}>
      {children}
    </LoadingContext.Provider>
  )
}

export function useLoading() {
  const context = useContext(LoadingContext)
  if (!context) {
    throw new Error('useLoading must be used within a LoadingProvider')
  }
  return context
}

export default LoadingContext
