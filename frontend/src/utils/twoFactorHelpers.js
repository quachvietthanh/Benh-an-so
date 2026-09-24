/**
 * Tiện ích hỗ trợ xác thực hai lớp (2FA) - NCL-01-CN-006
 */

/**
 * Kiểm tra định dạng mã xác thực: đúng 6 chữ số
 * @param {string} code
 * @returns {boolean}
 */
export const isOtpValid = (code) => {
  if (!code || typeof code !== 'string') return false
  return /^\d{6}$/.test(code.trim())
}

/**
 * Che mờ tên đăng nhập để bảo mật thông tin trên giao diện
 * @param {string} username
 * @returns {string}
 */
export const maskUsername = (username) => {
  if (!username || typeof username !== 'string') return ''
  const trimmed = username.trim()
  if (trimmed.length <= 2) return trimmed
  const first = trimmed.charAt(0)
  const last = trimmed.charAt(trimmed.length - 1)
  const stars = '*'.repeat(Math.max(3, trimmed.length - 2))
  return `${first}${stars}${last}`
}

/**
 * Tính số giây còn lại từ thời điểm ISO-8601 đến hiện tại
 * @param {string|Date} expiresAt
 * @param {number} [nowTimestamp]
 * @returns {number}
 */
export const calculateRemainingSeconds = (expiresAt, nowTimestamp = Date.now()) => {
  if (!expiresAt) return 0
  const expTime = new Date(expiresAt).getTime()
  if (isNaN(expTime)) return 0
  const diffMs = expTime - nowTimestamp
  return Math.max(0, Math.floor(diffMs / 1000))
}

/**
 * Định dạng số giây thành chuỗi mm:ss
 * @param {number} totalSeconds
 * @returns {string}
 */
export const formatTimeCountdown = (totalSeconds) => {
  const safeSec = Math.max(0, Math.floor(Number(totalSeconds) || 0))
  const minutes = Math.floor(safeSec / 60)
  const seconds = safeSec % 60
  const mStr = String(minutes).padStart(2, '0')
  const sStr = String(seconds).padStart(2, '0')
  return `${mStr}:${sStr}`
}

/**
 * Ánh xạ mã lỗi từ Backend thành thông báo Tiếng Việt thân thiện, rõ ràng
 * @param {any} error
 * @returns {{ code: string, message: string, isExpired: boolean, isLocked: boolean }}
 */
export const mapTwoFactorError = (error) => {
  const status = error?.response?.status || error?.status
  const errorData = error?.response?.data
  const rawCode = errorData?.code || error?.code || ''

  if (status === 429 || rawCode === 'VERIFICATION_CODE_COOLDOWN') {
    return {
      code: 'VERIFICATION_CODE_COOLDOWN',
      message: 'Bạn vừa yêu cầu gửi lại mã, vui lòng đợi ít phút.',
      isExpired: false,
      isLocked: false,
    }
  }

  if (rawCode === 'CHALLENGE_EXPIRED' || rawCode === 'VERIFICATION_CODE_EXPIRED') {
    return {
      code: 'CHALLENGE_EXPIRED',
      message: 'Mã xác thực đã hết hạn. Vui lòng đăng nhập lại.',
      isExpired: true,
      isLocked: false,
    }
  }

  if (rawCode === 'MAX_ATTEMPTS_EXCEEDED') {
    return {
      code: 'MAX_ATTEMPTS_EXCEEDED',
      message: 'Bạn đã nhập sai quá 5 lần. Vui lòng đăng nhập lại.',
      isExpired: false,
      isLocked: true,
    }
  }

  if (rawCode === 'CHALLENGE_CONSUMED' || rawCode === 'TWO_FACTOR_CHALLENGE_INVALID') {
    return {
      code: 'CHALLENGE_CONSUMED',
      message: 'Mã xác thực này đã được sử dụng hoặc không hợp lệ. Vui lòng đăng nhập lại.',
      isExpired: false,
      isLocked: true,
    }
  }

  if (rawCode === 'ACCOUNT_DISABLED') {
    return {
      code: 'ACCOUNT_DISABLED',
      message: 'Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.',
      isExpired: false,
      isLocked: true,
    }
  }

  if (rawCode === 'INVALID_CODE' || rawCode === 'INVALID_VERIFICATION_CODE') {
    const remainingAttempts = errorData?.details?.remainingAttempts ?? errorData?.remainingAttempts
    if (remainingAttempts !== undefined && remainingAttempts !== null) {
      return {
        code: 'INVALID_CODE',
        message: `Mã xác thực không đúng. Còn lại ${remainingAttempts} lần thử.`,
        isExpired: false,
        isLocked: false,
      }
    }
    return {
      code: 'INVALID_CODE',
      message: 'Mã xác thực không đúng. Vui lòng kiểm tra lại.',
      isExpired: false,
      isLocked: false,
    }
  }

  const fallbackMsg = errorData?.message || error?.message || 'Xác thực không thành công. Vui lòng thử lại.'
  return {
    code: rawCode || 'UNKNOWN_ERROR',
    message: fallbackMsg,
    isExpired: false,
    isLocked: false,
  }
}
