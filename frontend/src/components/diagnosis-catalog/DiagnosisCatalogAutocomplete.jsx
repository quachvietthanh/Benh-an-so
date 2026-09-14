import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Select, Spin, Tag } from 'antd'
import diagnosisCatalogApi from '../../api/diagnosisCatalogApi.js'
import { fixMojibake } from '../../utils/serviceCatalogValidation.js'
import {
  mergeSuggestions,
  formatDiagnosisDisplayLabel,
  buildSuggestionOptions,
  buildSearchOptions,
  getNotFoundStatus,
} from './diagnosisCatalogHelper.js'

export {
  mergeSuggestions,
  formatDiagnosisDisplayLabel,
  buildSuggestionOptions,
  buildSearchOptions,
  getNotFoundStatus,
}

/**
 * Render label for a diagnosis option in dropdown
 */
export function renderDiagnosisOptionLabel(item) {
  if (!item) return null
  const displayName = fixMojibake(item.name || '')
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        width: '100%',
        gap: 8,
        padding: '2px 0',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          flex: 1,
        }}
      >
        <Tag
          color="blue"
          style={{
            fontWeight: 700,
            margin: 0,
            fontSize: 12,
            padding: '1px 6px',
            borderRadius: 4,
            flexShrink: 0,
          }}
        >
          {item.code}
        </Tag>
        <span
          style={{
            fontWeight: 500,
            color: '#1F2937',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
          title={displayName}
        >
          {displayName}
        </span>
        {item.abbreviation && (
          <Tag
            style={{
              margin: 0,
              fontSize: 11,
              color: '#4B5563',
              background: '#F3F4F6',
              border: '1px solid #E5E7EB',
              flexShrink: 0,
            }}
          >
            {item.abbreviation}
          </Tag>
        )}
      </div>
      {item.diseaseGroup && (
        <span
          style={{
            fontSize: 11,
            color: '#6B7280',
            flexShrink: 0,
            maxWidth: 160,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
          title={item.diseaseGroup}
        >
          {item.diseaseGroup}
        </span>
      )}
    </div>
  )
}

/**
 * Determine notFoundContent JSX based on state
 */
export function renderNotFoundContent(loading, errorMessage, searchKeyword) {
  const status = getNotFoundStatus(loading, errorMessage, searchKeyword)
  if (status.type === 'loading') {
    return (
      <div style={{ padding: '8px 12px', textAlign: 'center', color: '#6B7280' }}>
        <Spin size="small" />
        <span style={{ marginLeft: 8, fontSize: 13 }}>{status.message}</span>
      </div>
    )
  }
  if (status.type === 'error') {
    return (
      <div style={{ padding: '8px 12px', color: '#DC2626', fontSize: 13 }}>
        <span style={{ marginRight: 6 }}>⚠️</span>
        <span>{status.message}</span>
      </div>
    )
  }
  if (status.type === 'empty') {
    return (
      <div style={{ padding: '8px 12px', color: '#6B7280', fontSize: 13 }}>
        {status.message}
      </div>
    )
  }
  return (
    <div style={{ padding: '8px 12px', color: '#9CA3AF', fontSize: 13 }}>
      {status.message}
    </div>
  )
}

/**
 * DiagnosisCatalogAutocomplete
 * Component tra cứu mã bệnh ICD-10 theo từ khóa tiếng Việt kết nối REST API backend.
 */
function DiagnosisCatalogAutocomplete({
  value = null,
  onChange,
  onSelect,
  diseaseGroup,
  fallbackSuggestions = [],
  placeholder = '🔍 Tra cứu mã bệnh theo mã ICD (J00, I10...) hoặc tên bệnh (cảm cúm, đau đầu...)',
  disabled = false,
  allowClear = true,
  style = { width: '100%' },
  className,
  id,
  'aria-label': ariaLabel,
}) {
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [suggestionItems, setSuggestionItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  const searchTimerRef = useRef(null)
  const hasLoadedSuggestionsRef = useRef(false)
  const isMountedRef = useRef(true)

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current)
      }
    }
  }, [])

  // Load suggestions when dropdown opens or on initial focus if input is empty
  const fetchSuggestions = useCallback(async () => {
    if (hasLoadedSuggestionsRef.current) return
    try {
      setLoading(true)
      const res = await diagnosisCatalogApi.getSuggestions()
      if (!isMountedRef.current) return
      const rawData = res?.data || res || {}
      const merged = mergeSuggestions(rawData)
      if (merged.length > 0) {
        setSuggestionItems(merged)
      } else if (Array.isArray(fallbackSuggestions) && fallbackSuggestions.length > 0) {
        setSuggestionItems(fallbackSuggestions.slice(0, 15))
      }
      hasLoadedSuggestionsRef.current = true
    } catch (err) {
      if (!isMountedRef.current) return
      console.warn('Lỗi khi tải gợi ý chẩn đoán:', err?.response?.status, err?.message)
      if (Array.isArray(fallbackSuggestions) && fallbackSuggestions.length > 0) {
        setSuggestionItems(fallbackSuggestions.slice(0, 15))
      }
      hasLoadedSuggestionsRef.current = true
    } finally {
      if (isMountedRef.current) {
        setLoading(false)
      }
    }
  }, [fallbackSuggestions])

  // Debounced search handler (300ms)
  const handleSearch = useCallback(
    (text) => {
      const keyword = typeof text === 'string' ? text : ''
      setSearchKeyword(keyword)
      setErrorMessage(null)

      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current)
      }

      if (!keyword.trim()) {
        setSearchResults([])
        setLoading(false)
        return
      }

      setLoading(true)
      searchTimerRef.current = setTimeout(async () => {
        try {
          const params = { search: keyword }
          if (diseaseGroup) {
            params.diseaseGroup = diseaseGroup
          }
          const res = await diagnosisCatalogApi.search(params)
          if (!isMountedRef.current) return
          const data = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
          setSearchResults(data)
        } catch (err) {
          if (!isMountedRef.current) return
          console.warn('Lỗi tìm kiếm danh mục chẩn đoán:', err)
          // Gentle inline error notification, no popup/modal
          setErrorMessage('Không thể tải kết quả tìm kiếm. Vui lòng thử lại.')
          setSearchResults([])
        } finally {
          if (isMountedRef.current) {
            setLoading(false)
          }
        }
      }, 300)
    },
    [diseaseGroup],
  )

  const handleDropdownVisibleChange = useCallback(
    (open) => {
      if (open) {
        if (!searchKeyword.trim()) {
          fetchSuggestions()
        }
      } else {
        if (searchKeyword) {
          setSearchKeyword('')
          setSearchResults([])
        }
        setErrorMessage(null)
      }
    },
    [fetchSuggestions, searchKeyword],
  )

  const handleFocus = useCallback(() => {
    if (!searchKeyword.trim()) {
      fetchSuggestions()
    }
  }, [fetchSuggestions, searchKeyword])

  const options = useMemo(() => {
    if (searchKeyword.trim().length > 0) {
      return buildSearchOptions(searchResults, renderDiagnosisOptionLabel)
    }
    const grouped = buildSuggestionOptions(suggestionItems, renderDiagnosisOptionLabel)
    if (grouped.length > 0) {
      return [
        {
          label: (
            <span style={{ fontWeight: 600, color: '#1E3A8A', fontSize: 12 }}>
              Chẩn đoán gợi ý
            </span>
          ),
          options: grouped[0].options,
        },
      ]
    }
    return []
  }, [searchKeyword, searchResults, suggestionItems])

  const notFoundContent = useMemo(
    () => renderNotFoundContent(loading, errorMessage, searchKeyword),
    [loading, errorMessage, searchKeyword],
  )

  const handleChange = useCallback(
    (selectedCode, option) => {
      const selectedItem =
        option?.data ||
        searchResults.find((i) => i.code === selectedCode) ||
        suggestionItems.find((i) => i.code === selectedCode) ||
        null

      if (onChange) {
        onChange(selectedCode, selectedItem)
      }
      if (onSelect && selectedItem) {
        onSelect(selectedItem)
      }

      setSearchKeyword('')
      setSearchResults([])
    },
    [onChange, onSelect, searchResults, suggestionItems],
  )

  return (
    <Select
      showSearch
      allowClear={allowClear}
      disabled={disabled}
      placeholder={placeholder}
      value={value}
      options={options}
      filterOption={false}
      defaultActiveFirstOption={false}
      optionLabelProp="displayLabel"
      notFoundContent={notFoundContent}
      onSearch={handleSearch}
      onFocus={handleFocus}
      onDropdownVisibleChange={handleDropdownVisibleChange}
      onChange={handleChange}
      style={style}
      className={className}
      id={id}
      aria-label={ariaLabel || 'Tìm mã bệnh ICD-10 theo từ khóa tiếng Việt'}
    />
  )
}

export default DiagnosisCatalogAutocomplete
