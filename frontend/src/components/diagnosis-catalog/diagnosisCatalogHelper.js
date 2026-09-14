import { fixMojibake } from '../../utils/serviceCatalogValidation.js'

/**
 * Merge recent and popular suggestions into a single list, deduplicating by code.
 */
export function mergeSuggestions(suggestionData) {
  if (!suggestionData || typeof suggestionData !== 'object') return []
  const recent = Array.isArray(suggestionData.recent) ? suggestionData.recent : []
  const popular = Array.isArray(suggestionData.popular) ? suggestionData.popular : []

  const merged = []
  const seenCodes = new Set()

  for (const item of [...recent, ...popular]) {
    if (item && item.code && !seenCodes.has(item.code)) {
      seenCodes.add(item.code)
      merged.push(item)
    }
  }
  return merged
}

/**
 * Format clean display label for selected input value: e.g. "[J00] Cảm lạnh thông thường"
 */
export function formatDiagnosisDisplayLabel(item) {
  if (!item || !item.code) return ''
  const name = fixMojibake(item.name || '')
  return name ? `[${item.code}] ${name}` : `[${item.code}]`
}

/**
 * Build grouped suggestions option structure for AntD Select
 */
export function buildSuggestionOptions(suggestionItems, renderLabel) {
  if (!Array.isArray(suggestionItems) || suggestionItems.length === 0) return []
  return [
    {
      label: 'Chẩn đoán gợi ý',
      options: suggestionItems.map((item) => ({
        key: item.code,
        value: item.code,
        data: item,
        displayLabel: formatDiagnosisDisplayLabel(item),
        label: renderLabel ? renderLabel(item) : formatDiagnosisDisplayLabel(item),
      })),
    },
  ]
}

/**
 * Build flat search options structure for AntD Select
 */
export function buildSearchOptions(searchResults, renderLabel) {
  if (!Array.isArray(searchResults) || searchResults.length === 0) return []
  return searchResults.map((item) => ({
    key: item.code,
    value: item.code,
    data: item,
    displayLabel: formatDiagnosisDisplayLabel(item),
    label: renderLabel ? renderLabel(item) : formatDiagnosisDisplayLabel(item),
  }))
}

/**
 * Determine notFound message or status based on search state
 */
export function getNotFoundStatus(loading, errorMessage, searchKeyword) {
  if (loading) {
    return {
      type: 'loading',
      message: 'Đang tìm trong danh mục mã bệnh...',
    }
  }
  if (errorMessage) {
    return {
      type: 'error',
      message: errorMessage,
    }
  }
  const trimmed = (searchKeyword || '').trim()
  if (trimmed.length > 0) {
    return {
      type: 'empty',
      message: `Không tìm thấy mã bệnh phù hợp với '${trimmed}'`,
    }
  }
  return {
    type: 'idle',
    message: 'Nhập mã ICD-10 hoặc tên bệnh tiếng Việt để tìm kiếm',
  }
}
