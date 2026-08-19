import dayjs from 'dayjs'

export function validateDateRange(fromDate, toDate) {
  if (!fromDate || !toDate) {
    return {
      valid: false,
      error: 'Từ ngày và đến ngày là bắt buộc.',
    }
  }

  const start = dayjs.isDayjs(fromDate) ? fromDate : dayjs(fromDate)
  const end = dayjs.isDayjs(toDate) ? toDate : dayjs(toDate)

  if (!start.isValid() || !end.isValid()) {
    return {
      valid: false,
      error: 'Khoảng thời gian không hợp lệ.',
    }
  }

  if (start.isAfter(end, 'day')) {
    return {
      valid: false,
      error: 'Ngày bắt đầu không được lớn hơn ngày kết thúc.',
    }
  }

  return {
    valid: true,
    error: '',
  }
}

export function mapDoctorVisitResponse(data) {
  if (!data || !Array.isArray(data.items)) {
    return {
      from: data?.from || '',
      to: data?.to || '',
      generatedAt: data?.generatedAt || '',
      items: [],
    }
  }

  const items = data.items.map((item, idx) => ({
    rank: item.rank || idx + 1,
    doctorId: item.doctorId,
    doctorCode: item.doctorCode || 'N/A',
    doctorName: item.doctorName || 'Chưa xác định',
    totalVisits: Number(item.totalVisits || 0),
  })).sort((a, b) => b.totalVisits - a.totalVisits)

  return {
    from: data.from || '',
    to: data.to || '',
    generatedAt: data.generatedAt || '',
    items,
  }
}

export function calculateDoctorVisitStats(items = []) {
  const safeItems = Array.isArray(items) ? items : []
  const totalVisitsAll = safeItems.reduce(
    (sum, item) => sum + Number(item.totalVisits || 0),
    0,
  )
  const doctorCount = safeItems.length
  const avgVisits =
    doctorCount > 0 ? (totalVisitsAll / doctorCount).toFixed(1) : '0.0'

  let topDoctor = null
  if (safeItems.length > 0) {
    topDoctor = safeItems.reduce(
      (max, curr) =>
        Number(curr.totalVisits || 0) > Number(max.totalVisits || 0)
          ? curr
          : max,
      safeItems[0],
    )
  }

  return {
    totalVisitsAll,
    doctorCount,
    avgVisits,
    topDoctor,
  }
}

export function calculateDoctorContributionPercentage(totalVisits, totalVisitsAll) {
  const count = Number(totalVisits || 0)
  const total = Number(totalVisitsAll || 0)
  if (total <= 0) return '0.0%'
  return `${((count / total) * 100).toFixed(1)}%`
}
