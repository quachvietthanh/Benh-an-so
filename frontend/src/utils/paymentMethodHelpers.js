export const PAYMENT_METHODS = {
  CASH: 'CASH',
  CARD: 'CARD',
  BANK_TRANSFER: 'BANK_TRANSFER',
  QR_CODE: 'QR_CODE',
  E_WALLET: 'E_WALLET',
  MULTIPLE: 'MULTIPLE',
}

export const PAYMENT_METHOD_OPTIONS = [
  {
    value: 'CASH',
    label: 'Tiền mặt',
    icon: 'DollarCircleOutlined',
    color: '#16a34a',
    tagColor: 'green',
    description: 'Thanh toán bằng tiền mặt trực tiếp tại quầy thu ngân',
  },
  {
    value: 'BANK_TRANSFER',
    label: 'Chuyển khoản',
    icon: 'BankOutlined',
    color: '#7c3aed',
    tagColor: 'purple',
    description: 'Chuyển khoản qua số tài khoản ngân hàng (yêu cầu mã giao dịch)',
  },
  {
    value: 'CARD',
    label: 'Thẻ (POS)',
    icon: 'CreditCardOutlined',
    color: '#2563eb',
    tagColor: 'blue',
    description: 'Quẹt thẻ ATM/Visa/MasterCard qua máy POS',
  },
  {
    value: 'QR_CODE',
    label: 'Mã QR',
    icon: 'QrcodeOutlined',
    color: '#0891b2',
    tagColor: 'cyan',
    description: 'Quét mã VietQR chuyển khoản nhanh',
  },
  {
    value: 'E_WALLET',
    label: 'Ví điện tử',
    icon: 'WalletOutlined',
    color: '#ea580c',
    tagColor: 'orange',
    description: 'Ví MoMo, ZaloPay, VNPay...',
  },
]

export const getPaymentMethodMeta = (method) => {
  if (method === 'MULTIPLE') {
    return {
      label: 'Nhiều phương thức',
      color: '#4f46e5',
      tagColor: 'geekblue',
    }
  }
  const found = PAYMENT_METHOD_OPTIONS.find((opt) => opt.value === method)
  if (found) {
    return {
      label: found.label,
      color: found.color,
      tagColor: found.tagColor,
    }
  }
  return {
    label: method || 'Không xác định',
    color: '#64748b',
    tagColor: 'default',
  }
}

export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || amount === '') return '0 ₫'
  const numeric = typeof amount === 'number' ? amount : Number(amount)
  if (Number.isNaN(numeric)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(numeric)
}

export const validatePaymentMethods = (methods, expectedTotal) => {
  const expected = typeof expectedTotal === 'number' ? expectedTotal : Number(expectedTotal)
  const safeExpected = Number.isNaN(expected) ? 0 : expected

  const errors = []
  const itemErrors = []

  if (!Array.isArray(methods) || methods.length === 0) {
    return {
      isValid: false,
      difference: safeExpected,
      totalEntered: 0,
      expectedTotal: safeExpected,
      isExact: false,
      isUnder: safeExpected > 0,
      isOver: safeExpected < 0,
      errors: ['Vui lòng nhập ít nhất một phương thức thanh toán.'],
      itemErrors: [],
    }
  }

  let totalEntered = 0

  methods.forEach((item, index) => {
    const rawAmount = item?.amount
    const numericAmount = typeof rawAmount === 'number' ? rawAmount : Number(rawAmount)

    if (rawAmount === undefined || rawAmount === null || rawAmount === '' || Number.isNaN(numericAmount)) {
      itemErrors.push({
        index,
        field: 'amount',
        message: `Dòng ${index + 1}: Vui lòng nhập số tiền hợp lệ.`,
      })
      errors.push(`Dòng ${index + 1}: Vui lòng nhập số tiền hợp lệ.`)
    } else if (numericAmount <= 0) {
      itemErrors.push({
        index,
        field: 'amount',
        message: `Dòng ${index + 1}: Số tiền thanh toán phải lớn hơn 0.`,
      })
      errors.push(`Dòng ${index + 1}: Số tiền thanh toán phải lớn hơn 0.`)
    } else {
      totalEntered += numericAmount
    }

    const method = item?.paymentMethod
    const validValues = ['CASH', 'CARD', 'BANK_TRANSFER', 'QR_CODE', 'E_WALLET']
    if (!method || !validValues.includes(method)) {
      itemErrors.push({
        index,
        field: 'paymentMethod',
        message: `Dòng ${index + 1}: Phương thức thanh toán không hợp lệ.`,
      })
      errors.push(`Dòng ${index + 1}: Phương thức thanh toán không hợp lệ.`)
    }

    if (method === 'BANK_TRANSFER') {
      const ref = item?.referenceNumber ? String(item.referenceNumber).trim() : ''
      if (!ref) {
        itemErrors.push({
          index,
          field: 'referenceNumber',
          message: `Dòng ${index + 1}: Chuyển khoản ngân hàng bắt buộc phải có số tham chiếu/mã giao dịch.`,
        })
        errors.push(`Dòng ${index + 1}: Bắt buộc nhập mã tham chiếu giao dịch cho phương thức Chuyển khoản.`)
      } else if (ref.length > 100) {
        itemErrors.push({
          index,
          field: 'referenceNumber',
          message: `Dòng ${index + 1}: Số tham chiếu không được vượt quá 100 ký tự.`,
        })
        errors.push(`Dòng ${index + 1}: Mã tham chiếu giao dịch vượt quá 100 ký tự (${ref.length} ký tự).`)
      }
    }
  })

  const difference = safeExpected - totalEntered

  if (difference > 0) {
    errors.push(`Tổng tiền đã nhập còn thiếu ${formatCurrency(difference)} so với số tiền cần thu.`)
  } else if (difference < 0) {
    errors.push(`Tổng tiền đã nhập vượt quá ${formatCurrency(Math.abs(difference))} so với số tiền cần thu.`)
  }

  const isExact = difference === 0
  const isValid = errors.length === 0 && isExact

  return {
    isValid,
    difference,
    totalEntered,
    expectedTotal: safeExpected,
    isExact,
    isUnder: difference > 0,
    isOver: difference < 0,
    errors,
    itemErrors,
  }
}

export const calculateEqualSplit = (totalAmount, count) => {
  if (!count || count <= 0) return []
  const total = Math.max(0, Math.round(Number(totalAmount) || 0))
  const base = Math.floor(total / count)
  const remainder = total - base * count

  const result = []
  for (let i = 0; i < count; i += 1) {
    result.push(i === 0 ? base + remainder : base)
  }
  return result
}

export const mapPaymentErrorMessage = (error) => {
  if (!error) return 'Đã xảy ra lỗi không xác định khi thực hiện thanh toán.'

  const res = error?.response
  const status = res?.status
  const data = res?.data
  const code = data?.code || data?.errorCode
  const message = data?.message || error?.message || ''

  if (status === 400) {
    if (code === 'PAYMENT_AMOUNT_MISMATCH' || message.includes('Payment amount must equal')) {
      return `Tổng tiền các phương thức thanh toán không khớp với số tiền cần thu của hóa đơn. ${message}`
    }
    if (message.includes('reference') || message.includes('tham chiếu')) {
      return 'Phương thức chuyển khoản ngân hàng bắt buộc phải nhập mã tham chiếu giao dịch (tối đa 100 ký tự).'
    }
    if (message.includes('amount') || message.includes('greater than zero')) {
      return 'Số tiền thanh toán của mỗi phương thức phải lớn hơn 0.'
    }
    return data?.message || 'Thông tin thanh toán không hợp lệ (400). Vui lòng kiểm tra lại.'
  }

  if (status === 409) {
    if (code === 'PAYMENT_ALREADY_EXISTS' || message.includes('already exists')) {
      return 'Khoản thu cho lượt khám này đã được ghi nhận thanh toán từ trước.'
    }
    if (code === 'PAYMENT_NOT_ALLOWED' || message.includes('cancelled') || message.includes('dispensing')) {
      return 'Không thể ghi nhận thu phí: Lượt khám đã bị hủy hoặc đơn thuốc chưa hoàn tất cấp phát tại quầy Dược.'
    }
    if (code === 'PENDING_DISCOUNT_APPROVAL' || message.includes('discount')) {
      return 'Lượt khám đang có yêu cầu miễn giảm viện phí chờ Quản lý phê duyệt. Vui lòng chờ duyệt trước khi thu phí.'
    }
    return data?.message || 'Lượt khám không đủ điều kiện để ghi nhận thanh toán vào thời điểm này (409).'
  }

  if (status === 403) {
    return 'Bạn không có quyền thực hiện thu phí viện phí (yêu cầu quyền INVOICE_CREATE).'
  }

  if (status === 404) {
    return 'Không tìm thấy thông tin lượt khám hoặc bảng giá dịch vụ tương ứng (404).'
  }

  if (status && status >= 500) {
    return 'Hệ thống máy chủ xử lý hóa đơn đang bận hoặc gặp sự cố. Vui lòng thử lại sau.'
  }

  return message || 'Không thể ghi nhận thanh toán. Vui lòng thử lại.'
}

export const buildCreateInvoicePayload = (params) => {
  if (!params || !params.visitId) {
    throw new Error('visitId là bắt buộc khi tạo Hóa đơn.')
  }
  const payload = {
    visitId: params.visitId,
  }
  if (params.paymentId && String(params.paymentId).trim()) {
    payload.paymentId = String(params.paymentId).trim()
  }
  return payload
}

export const extractInvoicePaymentMeta = (invoiceData) => {
  if (!invoiceData) {
    return { paymentMethod: 'CASH', paymentMethods: [] }
  }
  const method = invoiceData.payment?.paymentMethod || invoiceData.paymentMethod || 'CASH'
  const methods = Array.isArray(invoiceData.payment?.paymentMethods)
    ? invoiceData.payment.paymentMethods
    : Array.isArray(invoiceData.paymentMethods)
    ? invoiceData.paymentMethods
    : []

  return {
    paymentMethod: method,
    paymentMethods: methods,
  }
}

export const classifyInvoiceError = (error) => {
  if (!error) {
    return {
      isClientError: false,
      message: 'Đã xảy ra lỗi không xác định khi tạo hóa đơn.',
    }
  }

  if (!error.response) {
    return {
      isClientError: true,
      message: `Lỗi xử lý ứng dụng phía giao diện (Client Error): ${error.message || 'Lỗi runtime không xác định'}. Vui lòng thử lại.`,
    }
  }

  const status = error.response.status
  const data = error.response.data
  const code = data?.code || data?.errorCode
  const backendMsg = data?.message

  if (status === 409) {
    return {
      isClientError: false,
      status: 409,
      code: code || 'INVOICE_ALREADY_EXISTS',
      message: backendMsg || 'Lượt khám này đã được lập hóa đơn trước đó (409).',
    }
  }

  if (status === 400) {
    return {
      isClientError: false,
      status: 400,
      code: code || 'BAD_REQUEST',
      message: backendMsg || 'Dữ liệu tạo hóa đơn không hợp lệ (400).',
    }
  }

  if (status === 401) {
    return {
      isClientError: false,
      status: 401,
      code: 'UNAUTHORIZED',
      message: 'Hết phiên làm việc. Vui lòng đăng nhập lại (401).',
    }
  }

  if (status === 403) {
    return {
      isClientError: false,
      status: 403,
      code: 'FORBIDDEN',
      message: 'Bạn không có quyền lập hóa đơn (403, yêu cầu INVOICE_CREATE).',
    }
  }

  if (status === 404) {
    return {
      isClientError: false,
      status: 404,
      code: 'NOT_FOUND',
      message: 'Không tìm thấy thông tin lượt khám / thanh toán trên máy chủ (404).',
    }
  }

  return {
    isClientError: false,
    status,
    code,
    message: backendMsg || 'Không thể lập hóa đơn điện tử từ máy chủ (Backend). Vui lòng thử lại.',
  }
}
