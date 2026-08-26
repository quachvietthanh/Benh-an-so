import React, { useEffect, useRef, useState } from 'react'
import loadingManager from '../../utils/loadingManager'

function GlobalLoadingBar() {
  const [visible, setVisible] = useState(false)
  const [progress, setProgress] = useState(0)
  const [finishing, setFinishing] = useState(false)
  const intervalRef = useRef(null)
  const finishTimeoutRef = useRef(null)

  useEffect(() => {
    const handleLoadingChange = ({ isLoading }) => {
      if (isLoading) {
        if (finishTimeoutRef.current) {
          clearTimeout(finishTimeoutRef.current)
          finishTimeoutRef.current = null
        }
        setFinishing(false)
        setVisible(true)
        setProgress((prev) => (prev > 0 && prev < 90 ? prev : 15))

        if (intervalRef.current) clearInterval(intervalRef.current)
        intervalRef.current = setInterval(() => {
          setProgress((prev) => {
            if (prev >= 85) return prev + 0.5 <= 90 ? prev + 0.5 : 90
            if (prev >= 60) return prev + 2
            if (prev >= 30) return prev + 5
            return prev + 10
          })
        }, 120)
      } else {
        if (intervalRef.current) {
          clearInterval(intervalRef.current)
          intervalRef.current = null
        }
        setProgress(100)
        setFinishing(true)

        finishTimeoutRef.current = setTimeout(() => {
          setVisible(false)
          setProgress(0)
          setFinishing(false)
        }, 400)
      }
    }

    const unsubscribe = loadingManager.subscribe(handleLoadingChange)
    return () => {
      unsubscribe()
      if (intervalRef.current) clearInterval(intervalRef.current)
      if (finishTimeoutRef.current) clearTimeout(finishTimeoutRef.current)
    }
  }, [])

  if (!visible && progress === 0) return null

  return (
    <div className="global-loading-bar-container" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
      <div
        className={`global-loading-bar ${finishing ? 'finishing' : ''}`}
        style={{ width: `${progress}%` }}
      />
    </div>
  )
}

export default GlobalLoadingBar
