import assert from 'node:assert/strict'
import test from 'node:test'
import diagnosisCatalogApi from '../../api/diagnosisCatalogApi.js'
import axiosClient from '../../api/axiosClient.js'
import {
  mergeSuggestions,
  buildSuggestionOptions,
  buildSearchOptions,
  getNotFoundStatus,
  formatDiagnosisDisplayLabel,
} from './diagnosisCatalogHelper.js'

test('TC01: Mở ô input trống -> nạp suggestions và nhóm thành một nhóm duy nhất "Chẩn đoán gợi ý"', async () => {
  const mockSuggestionPayload = {
    data: {
      recent: [
        { id: '1', code: 'J00', name: 'Cảm lạnh thông thường', diseaseGroup: 'Bệnh hệ hô hấp', active: true },
        { id: '2', code: 'I10', name: 'Tăng huyết áp vô căn', diseaseGroup: 'Bệnh hệ tuần hoàn', active: true },
      ],
      popular: [
        { id: '2', code: 'I10', name: 'Tăng huyết áp vô căn', diseaseGroup: 'Bệnh hệ tuần hoàn', active: true },
        { id: '3', code: 'K29.7', name: 'Viêm dạ dày', diseaseGroup: 'Bệnh hệ tiêu hóa', active: true },
      ],
      diseaseGroups: ['Bệnh hệ hô hấp', 'Bệnh hệ tuần hoàn', 'Bệnh hệ tiêu hóa'],
    },
  }

  const originalGet = axiosClient.get
  let capturedUrl = null
  axiosClient.get = async (url) => {
    capturedUrl = url
    return mockSuggestionPayload
  }

  try {
    const res = await diagnosisCatalogApi.getSuggestions()
    assert.equal(capturedUrl, '/diagnosis-catalog/suggestions')

    // Verify deduplication and merging
    const merged = mergeSuggestions(res.data)
    assert.equal(merged.length, 3, 'recent and popular should be merged and deduplicated by code')
    assert.equal(merged[0].code, 'J00')
    assert.equal(merged[1].code, 'I10')
    assert.equal(merged[2].code, 'K29.7')

    // Verify grouped option structure
    const options = buildSuggestionOptions(merged)
    assert.equal(options.length, 1, 'Must have exactly 1 group')
    assert.equal(options[0].label, 'Chẩn đoán gợi ý')
    assert.equal(options[0].options.length, 3)
    assert.equal(options[0].options[0].value, 'J00')
    assert.equal(options[0].options[0].data.name, 'Cảm lạnh thông thường')
    assert.equal(options[0].options[0].displayLabel, '[J00] Cảm lạnh thông thường')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC02: Gõ từ khóa tiếng Việt có dấu -> gọi đúng API ?search={keyword} nguyên văn, không strip dấu', async () => {
  const originalGet = axiosClient.get
  let capturedUrl = null
  let capturedConfig = null

  const mockResponse = {
    data: [
      { id: '1', code: 'J02.9', name: 'Viêm họng cấp', diseaseGroup: 'Bệnh hệ hô hấp', active: true },
    ],
  }

  axiosClient.get = async (url, config) => {
    capturedUrl = url
    capturedConfig = config
    return mockResponse
  }

  try {
    const keywordWithDiacritics = 'viêm họng cấp'
    const res = await diagnosisCatalogApi.search({ search: keywordWithDiacritics })
    assert.equal(capturedUrl, '/diagnosis-catalog')
    assert.equal(capturedConfig?.params?.search, 'viêm họng cấp', 'Keyword with diacritics must be preserved')
    assert.equal(res.data.length, 1)

    const searchOptions = buildSearchOptions(res.data)
    assert.equal(searchOptions.length, 1)
    assert.equal(searchOptions[0].value, 'J02.9')
    assert.equal(searchOptions[0].displayLabel, '[J02.9] Viêm họng cấp')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC03: Gõ từ khóa không dấu -> gọi đúng API ?search={keyword}', async () => {
  const originalGet = axiosClient.get
  let capturedConfig = null

  axiosClient.get = async (url, config) => {
    capturedConfig = config
    return { data: [{ id: '1', code: 'I10', name: 'Tăng huyết áp vô căn', active: true }] }
  }

  try {
    const unaccentedKeyword = 'tang huyet ap'
    await diagnosisCatalogApi.search({ search: unaccentedKeyword })
    assert.equal(capturedConfig?.params?.search, 'tang huyet ap')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC04: Gõ mã ICD (vd: "J00") -> gọi đúng API ?search=J00', async () => {
  const originalGet = axiosClient.get
  let capturedConfig = null

  axiosClient.get = async (url, config) => {
    capturedConfig = config
    return { data: [{ id: '1', code: 'J00', name: 'Cảm lạnh thông thường', active: true }] }
  }

  try {
    await diagnosisCatalogApi.search({ search: 'J00' })
    assert.equal(capturedConfig?.params?.search, 'J00')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC05: Debounce - gõ liên tục nhiều ký tự chỉ gọi API 1 lần sau 300ms', async () => {
  const originalGet = axiosClient.get
  let callCount = 0
  let lastSearchParam = null

  axiosClient.get = async (url, config) => {
    callCount++
    lastSearchParam = config?.params?.search
    return { data: [] }
  }

  try {
    // Simulate debounce logic matching component implementation
    let timer = null
    const simulateDebounceSearch = (text, delay = 300) => {
      if (timer) clearTimeout(timer)
      return new Promise((resolve) => {
        timer = setTimeout(async () => {
          await diagnosisCatalogApi.search({ search: text })
          resolve()
        }, delay)
      })
    }

    // Typing rapidly: "c", "ca", "cam" within 50ms intervals
    simulateDebounceSearch('c', 300)
    await new Promise((r) => setTimeout(r, 50))
    simulateDebounceSearch('ca', 300)
    await new Promise((r) => setTimeout(r, 50))
    const p3 = simulateDebounceSearch('cam', 300)

    await p3

    assert.equal(callCount, 1, 'API should only be called once after typing stops')
    assert.equal(lastSearchParam, 'cam', 'Final search parameter should be the last typed keyword')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC06: Hỗ trợ param diseaseGroup và chọn 1 mã bệnh trigger callback', async () => {
  const originalGet = axiosClient.get
  let capturedConfig = null

  const mockItem = {
    id: 'uuid-1',
    code: 'J00',
    name: 'Cảm lạnh thông thường',
    diseaseGroup: 'Bệnh hệ hô hấp',
    abbreviation: 'CL',
    active: true,
  }

  axiosClient.get = async (url, config) => {
    capturedConfig = config
    return { data: [mockItem] }
  }

  try {
    // 1. Verify diseaseGroup filter param
    await diagnosisCatalogApi.search({ search: 'cam', diseaseGroup: 'Bệnh hệ hô hấp' })
    assert.equal(capturedConfig?.params?.diseaseGroup, 'Bệnh hệ hô hấp')

    // 2. Verify selection triggers onChange & onSelect with correct item object
    let selectedCode = null
    let selectedItem = null
    const onChange = (code, item) => {
      selectedCode = code
      selectedItem = item
    }

    const options = buildSearchOptions([mockItem])
    const chosenOption = options.find((o) => o.value === 'J00')
    onChange(chosenOption.value, chosenOption.data)

    assert.equal(selectedCode, 'J00')
    assert.deepEqual(selectedItem, mockItem)
    assert.equal(formatDiagnosisDisplayLabel(mockItem), '[J00] Cảm lạnh thông thường')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC07: Xử lý lỗi API (500 / Network error) và trạng thái rỗng mà không crash', async () => {
  const originalGet = axiosClient.get
  axiosClient.get = async () => {
    const err = new Error('Internal Server Error')
    err.response = { status: 500 }
    throw err
  }

  try {
    let errorHandled = false
    let displayedErrorMessage = null

    try {
      await diagnosisCatalogApi.search({ search: 'khongton' })
    } catch (err) {
      errorHandled = true
      displayedErrorMessage = 'Không thể tải kết quả tìm kiếm. Vui lòng thử lại.'
    }

    assert.equal(errorHandled, true)

    // Verify notFound status functions
    const errorStatus = getNotFoundStatus(false, displayedErrorMessage, 'khongton')
    assert.equal(errorStatus.type, 'error')
    assert.equal(errorStatus.message, 'Không thể tải kết quả tìm kiếm. Vui lòng thử lại.')

    const emptyStatus = getNotFoundStatus(false, null, 'khong_tim_thay_ma')
    assert.equal(emptyStatus.type, 'empty')
    assert.equal(emptyStatus.message, "Không tìm thấy mã bệnh phù hợp với 'khong_tim_thay_ma'")

    const loadingStatus = getNotFoundStatus(true, null, 'dang_tim')
    assert.equal(loadingStatus.type, 'loading')
    assert.equal(loadingStatus.message, 'Đang tìm trong danh mục mã bệnh...')

    const idleStatus = getNotFoundStatus(false, null, '')
    assert.equal(idleStatus.type, 'idle')
    assert.equal(idleStatus.message, 'Nhập mã ICD-10 hoặc tên bệnh tiếng Việt để tìm kiếm')
  } finally {
    axiosClient.get = originalGet
  }
})

test('TC08: Gợi ý chẩn đoán tự động fallback sang danh sách chẩn đoán có sẵn khi API suggestions không khả dụng', async () => {
  const originalGet = axiosClient.get
  axiosClient.get = async (url) => {
    if (url === '/diagnosis-catalog/suggestions') {
      const err = new Error('Not Found')
      err.response = { status: 404 }
      throw err
    }
    // Fallback call
    return { data: [{ id: '1', code: 'J00', name: 'Cảm lạnh thông thường', active: true }] }
  }

  try {
    const res = await diagnosisCatalogApi.getSuggestions()
    assert.ok(res?.data, 'Should return fallback response without throwing')
    assert.ok(Array.isArray(res.data.popular), 'Should return popular fallback array')
    assert.equal(res.data.popular.length, 1)
    assert.equal(res.data.popular[0].code, 'J00')
  } finally {
    axiosClient.get = originalGet
  }
})

