const DEFAULT_MESSAGE = 'Thao tác không thành công. Vui lòng thử lại.'

export const normalizeApiError = (error, fallbackMessage = DEFAULT_MESSAGE) => {
  const response = error?.response
  const body = response?.data
  const responseMessage = typeof body === 'string' ? body : body?.message
  const details = body && typeof body.details === 'object' && !Array.isArray(body.details)
    ? body.details
    : {}
  const fields = details.fields && typeof details.fields === 'object' ? details.fields : {}

  return {
    status: body?.status || response?.status || 0,
    code: body?.code || 'NETWORK_ERROR',
    message: responseMessage || error?.message || fallbackMessage,
    details,
    fields,
    firstFieldError: Object.values(fields)[0] || null,
  }
}

export const getApiErrorMessage = (error, fallbackMessage) => {
  const normalized = normalizeApiError(error, fallbackMessage)
  return normalized.firstFieldError || normalized.message
}
