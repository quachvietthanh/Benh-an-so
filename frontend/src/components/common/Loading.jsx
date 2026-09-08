import React from 'react'

/**
 * Lightweight Loading Component (No-op)
 * Renders children or null without any blocking delays or overlays.
 */
function Loading({ children }) {
  return children || null
}

export default Loading
