import dayjs from 'dayjs'

export const formatMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`

// Helper calculation of financial metrics
export const calculateFinancialMetrics = (invoices = [], timeline = [], dayCount = 1) => {
  let grossRevenue = 0
  let adjustmentTotal = 0
  let originalCount = 0
  let adjustmentCount = 0

  invoices.forEach((inv) => {
    const amt = Number(inv.totalAmount || 0)
    if (inv.invoiceType === 'ADJUSTMENT' || amt < 0) {
      adjustmentTotal += amt // usually negative
      adjustmentCount += 1
    } else {
      grossRevenue += amt
      originalCount += 1
    }
  })

  // Net revenue = Gross + negative adjustments (or Gross - abs(adjustments))
  const netRevenue = grossRevenue + adjustmentTotal
  const effectiveDays = Math.max(1, dayCount)
  const averageDaily = netRevenue / effectiveDays
  const adjustmentRate = grossRevenue > 0
    ? (Math.abs(adjustmentTotal) / grossRevenue) * 100
    : 0

  return {
    grossRevenue,
    adjustmentTotal,
    netRevenue,
    originalCount,
    adjustmentCount,
    totalInvoices: invoices.length,
    averageDaily,
    adjustmentRate,
  }
}

export const aggregateDailyTimeline = (invoices = [], startDate, endDate) => {
  const start = dayjs(startDate)
  const end = dayjs(endDate)
  const diffDays = Math.max(0, end.diff(start, 'day'))
  const totalDays = Math.min(diffDays + 1, 366)

  const dailyMap = new Map()

  for (let i = 0; i < totalDays; i++) {
    const current = start.add(i, 'day')
    const dateKey = current.format('YYYY-MM-DD')
    dailyMap.set(dateKey, {
      date: dateKey,
      displayDate: current.format('DD/MM/YYYY'),
      shortDate: current.format('DD/MM'),
      grossRevenue: 0,
      adjustments: 0,
      netRevenue: 0,
      originalCount: 0,
      adjustmentCount: 0,
      totalCount: 0,
    })
  }

  invoices.forEach((inv) => {
    if (!inv.createdAt) return
    const dateKey = dayjs(inv.createdAt).format('YYYY-MM-DD')
    if (dailyMap.has(dateKey)) {
      const item = dailyMap.get(dateKey)
      const amt = Number(inv.totalAmount || 0)
      if (inv.invoiceType === 'ADJUSTMENT' || amt < 0) {
        item.adjustments += amt
        item.adjustmentCount += 1
      } else {
        item.grossRevenue += amt
        item.originalCount += 1
      }
      item.netRevenue = item.grossRevenue + item.adjustments
      item.totalCount += 1
    }
  })

  return Array.from(dailyMap.values())
}

export const aggregatePaymentMethods = (invoices = []) => {
  const methodMap = {
    CASH: { label: 'Tiền mặt', color: '#10b981', total: 0, count: 0 },
    BANK_TRANSFER: { label: 'Chuyển khoản ngân hàng', color: '#3b82f6', total: 0, count: 0 },
    CREDIT_CARD: { label: 'Thẻ tín dụng / Ghi nợ', color: '#8b5cf6', total: 0, count: 0 },
    E_WALLET: { label: 'Ví điện tử / QR Code', color: '#f59e0b', total: 0, count: 0 },
    OTHER: { label: 'Phương thức khác', color: '#64748b', total: 0, count: 0 },
  }

  let totalCollected = 0

  invoices.forEach((inv) => {
    const method = String(inv.paymentMethod || '').toUpperCase()
    const amt = Number(inv.totalAmount || 0)
    if (amt > 0) {
      totalCollected += amt
      if (method.includes('CASH') || method.includes('TIEN_MAT')) {
        methodMap.CASH.total += amt
        methodMap.CASH.count += 1
      } else if (method.includes('TRANSFER') || method.includes('CHUYEN_KHOAN')) {
        methodMap.BANK_TRANSFER.total += amt
        methodMap.BANK_TRANSFER.count += 1
      } else if (method.includes('CARD') || method.includes('THE')) {
        methodMap.CREDIT_CARD.total += amt
        methodMap.CREDIT_CARD.count += 1
      } else if (method.includes('WALLET') || method.includes('QR') || method.includes('VNPAY') || method.includes('MOMO')) {
        methodMap.E_WALLET.total += amt
        methodMap.E_WALLET.count += 1
      } else {
        methodMap.CASH.total += amt
        methodMap.CASH.count += 1
      }
    }
  })

  return Object.entries(methodMap)
    .map(([key, item]) => ({
      key,
      ...item,
      percentage: totalCollected > 0 ? (item.total / totalCollected) * 100 : 0,
    }))
    .filter((m) => m.total > 0 || m.key === 'CASH' || m.key === 'BANK_TRANSFER')
}

export const getPresetDateRange = (presetKey) => {
  const today = dayjs()
  switch (presetKey) {
    case 'today':
      return [today.startOf('day'), today.endOf('day')]
    case '7days':
      return [today.subtract(6, 'day').startOf('day'), today.endOf('day')]
    case '30days':
      return [today.subtract(29, 'day').startOf('day'), today.endOf('day')]
    case 'thisMonth':
      return [today.startOf('month'), today.endOf('month')]
    case 'thisQuarter':
      return [today.startOf('quarter'), today.endOf('quarter')]
    case 'thisYear':
      return [today.startOf('year'), today.endOf('year')]
    default:
      return [today.subtract(6, 'day'), today]
  }
}
