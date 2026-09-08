export const PASSWORD_POLICY = {
  MIN_LENGTH: 8,
  MAX_LENGTH: 50,
}

/**
 * Validate password strength matching backend PasswordPolicyValidator (QTN-28).
 * Returns array of Vietnamese violation messages.
 */
export const validatePasswordStrength = (password) => {
  const violations = []

  if (!password || !password.trim()) {
    violations.push('Mật khẩu không được để trống.')
    return violations
  }

  if (password.length < PASSWORD_POLICY.MIN_LENGTH || password.length > PASSWORD_POLICY.MAX_LENGTH) {
    violations.push(`Mật khẩu phải có độ dài từ ${PASSWORD_POLICY.MIN_LENGTH} đến ${PASSWORD_POLICY.MAX_LENGTH} ký tự.`)
  }

  let hasUpper = false
  let hasLower = false
  let hasDigit = false

  for (let i = 0; i < password.length; i++) {
    const char = password[i]
    if (char >= 'A' && char <= 'Z') {
      hasUpper = true
    } else if (char >= 'a' && char <= 'z') {
      hasLower = true
    } else if (char >= '0' && char <= '9') {
      hasDigit = true
    }
  }

  if (!hasUpper) {
    violations.push('Mật khẩu phải chứa ít nhất một chữ cái in hoa (A-Z).')
  }
  if (!hasLower) {
    violations.push('Mật khẩu phải chứa ít nhất một chữ cái viết thường (a-z).')
  }
  if (!hasDigit) {
    violations.push('Mật khẩu phải chứa ít nhất một chữ số (0-9).')
  }

  return violations
}

/**
 * Comprehensive client-side validation for change password form.
 */
export const validateChangePasswordForm = ({ oldPassword, newPassword, confirmPassword }) => {
  const errors = {
    oldPassword: null,
    newPassword: null,
    confirmPassword: null,
    violations: [],
  }

  if (!oldPassword || !oldPassword.trim()) {
    errors.oldPassword = 'Vui lòng nhập mật khẩu hiện tại.'
  }

  if (!newPassword || !newPassword.trim()) {
    errors.newPassword = 'Vui lòng nhập mật khẩu mới.'
  } else {
    const violations = validatePasswordStrength(newPassword)
    if (violations.length > 0) {
      errors.violations = violations
      errors.newPassword = 'Mật khẩu mới chưa đạt độ mạnh yêu cầu.'
    } else if (oldPassword && newPassword === oldPassword) {
      errors.newPassword = 'Mật khẩu mới không được trùng với mật khẩu hiện tại.'
    }
  }

  if (!confirmPassword || !confirmPassword.trim()) {
    errors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới.'
  } else if (newPassword && confirmPassword !== newPassword) {
    errors.confirmPassword = 'Xác nhận mật khẩu mới không khớp.'
  }

  const isValid = !errors.oldPassword && !errors.newPassword && !errors.confirmPassword && errors.violations.length === 0

  return { isValid, errors }
}
