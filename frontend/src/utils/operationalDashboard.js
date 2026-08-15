const toNonNegativeNumber = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? Math.max(0, parsed) : 0
}

const toCount = (value) => Math.trunc(toNonNegativeNumber(value))

export const EMPTY_OPERATIONAL_DASHBOARD = Object.freeze({
  visitSummary: Object.freeze({
    total: 0,
    waiting: 0,
    inProgress: 0,
    completed: 0,
    cancelled: 0,
  }),
  revenueSummary: Object.freeze({
    totalRevenueToday: 0,
  }),
  inventoryAlertSummary: Object.freeze({
    lowStockCount: 0,
    expiryAlertCount: 0,
  }),
  asOf: null,
})

export const normalizeOperationalDashboard = (payload) => {
  const data = payload && typeof payload === 'object' ? payload : {}
  const visits = data.visitSummary && typeof data.visitSummary === 'object'
    ? data.visitSummary
    : {}
  const revenue = data.revenueSummary && typeof data.revenueSummary === 'object'
    ? data.revenueSummary
    : {}
  const inventory = data.inventoryAlertSummary && typeof data.inventoryAlertSummary === 'object'
    ? data.inventoryAlertSummary
    : {}

  return {
    visitSummary: {
      total: toCount(visits.total),
      waiting: toCount(visits.waiting),
      inProgress: toCount(visits.inProgress),
      completed: toCount(visits.completed),
      cancelled: toCount(visits.cancelled),
    },
    revenueSummary: {
      totalRevenueToday: toNonNegativeNumber(revenue.totalRevenueToday),
    },
    inventoryAlertSummary: {
      lowStockCount: toCount(inventory.lowStockCount),
      expiryAlertCount: toCount(inventory.expiryAlertCount),
    },
    asOf: typeof data.asOf === 'string' && data.asOf.trim() ? data.asOf : null,
  }
}

export const buildOperationalSnapshotFromReports = ({
  summary = null,
  timeline = null,
  records = [],
  invoices = [],
  medicines = [],
  batches = [],
  todayStr = '',
} = {}) => {
  const safeRecords = Array.isArray(records) ? records : []
  const safeInvoices = Array.isArray(invoices) ? invoices : []
  const safeMeds = Array.isArray(medicines) ? medicines : []
  const safeBatches = Array.isArray(batches) ? batches : []

  const todayRecords = safeRecords.filter((r) => {
    if (!r?.createdAt) return false
    return String(r.createdAt).slice(0, 10) === todayStr
  })

  const waiting = todayRecords.filter((r) => r.status === 'WAITING' || r.status === 'WAITING_FOR_EXAMINATION').length
  const inProgress = todayRecords.filter((r) => r.status === 'IN_PROGRESS' || r.status === 'EXAMINING' || r.status === 'WAITING_FOR_RESULT').length
  let completed = todayRecords.filter((r) => r.status === 'COMPLETED').length
  const cancelled = todayRecords.filter((r) => r.status === 'CANCELLED').length

  const visitCount = summary?.visitCount != null
    ? toCount(summary.visitCount)
    : (timeline && Array.isArray(timeline.items) && timeline.items[0]?.visitCount != null
      ? toCount(timeline.items[0].visitCount)
      : completed)

  if (visitCount > 0) {
    completed = Math.max(completed, visitCount)
  }
  const total = Math.max(waiting + inProgress + completed + cancelled, visitCount)

  const totalRevenue = summary?.revenue != null
    ? toNonNegativeNumber(summary.revenue)
    : (timeline && Array.isArray(timeline.items) && timeline.items[0]?.revenue != null
      ? toNonNegativeNumber(timeline.items[0].revenue)
      : safeInvoices
          .filter((inv) => inv?.createdAt && String(inv.createdAt).slice(0, 10) === todayStr)
          .reduce((sum, inv) => sum + Number(inv.totalAmount || 0), 0))

  const lowStockCount = safeMeds.filter((m) => {
    const stock = Number(m.stockQuantity ?? m.stock ?? 0)
    const minStock = Number(m.minStockQuantity ?? m.minStock ?? m.minStockThreshold ?? 10)
    return m.active !== false && stock <= minStock
  }).length

  const expiryAlertCount = safeBatches.filter((b) => {
    if (!b?.expiryDate) return false
    const exp = new Date(b.expiryDate).getTime()
    const now = todayStr ? new Date(todayStr).getTime() : Date.now()
    const daysRemaining = (exp - now) / (1000 * 60 * 60 * 24)
    return daysRemaining <= 30
  }).length

  return {
    visitSummary: {
      total,
      waiting,
      inProgress,
      completed,
      cancelled,
    },
    revenueSummary: {
      totalRevenueToday: totalRevenue,
    },
    inventoryAlertSummary: {
      lowStockCount,
      expiryAlertCount,
    },
    asOf: new Date().toISOString(),
  }
}

export const getVisitPercentage = (value, total) => {
  const safeTotal = toCount(total)
  if (!safeTotal) return 0
  return Math.min(100, Math.round((toCount(value) / safeTotal) * 100))
}

export const getActiveQueueCount = (visitSummary) => (
  toCount(visitSummary?.waiting) + toCount(visitSummary?.inProgress)
)
