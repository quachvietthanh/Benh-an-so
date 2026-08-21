import {
  REPORT_TYPES,
  validateExportParams,
  getExportFilename,
  getExportErrorMessage,
} from './reportExportHelpers'

describe('reportExportHelpers', () => {
  describe('REPORT_TYPES', () => {
    it('contains all 3 required report types with correct values and labels', () => {
      expect(REPORT_TYPES).toEqual([
        { value: 'VISIT_REPORT', label: 'Báo cáo lượt khám' },
        { value: 'REVENUE_REPORT', label: 'Báo cáo doanh thu' },
        { value: 'OPERATIONAL_REPORT', label: 'Báo cáo tổng hợp vận hành' },
      ])
    })
  })

  describe('validateExportParams', () => {
    it('returns valid when dates are correct and within 366 days', () => {
      const result = validateExportParams('2026-08-01', '2026-08-20')
      expect(result.isValid).toBe(true)
      expect(result.from).toBe('2026-08-01')
      expect(result.to).toBe('2026-08-20')
      expect(result.message).toBeNull()
    })

    it('returns error when missing from or to', () => {
      expect(validateExportParams(null, '2026-08-20').isValid).toBe(false)
      expect(validateExportParams('2026-08-01', null).isValid).toBe(false)
      expect(validateExportParams('', '').message).toBe('Vui lòng chọn khoảng thời gian.')
    })

    it('returns error when from is after to', () => {
      const result = validateExportParams('2026-08-20', '2026-08-01')
      expect(result.isValid).toBe(false)
      expect(result.message).toBe('Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.')
    })

    it('returns error when range exceeds 366 days', () => {
      const result = validateExportParams('2025-01-01', '2026-01-03') // 368 days
      expect(result.isValid).toBe(false)
      expect(result.message).toBe('Khoảng thời gian xuất báo cáo không được vượt quá 366 ngày.')
    })

    it('returns valid when range is exactly 366 days (leap year or full year)', () => {
      const result = validateExportParams('2024-01-01', '2024-12-31') // 366 days in leap year
      expect(result.isValid).toBe(true)
    })
  })

  describe('getExportFilename', () => {
    it('extracts filename from Content-Disposition header with quotes', () => {
      const header = 'attachment; filename="custom-report-2026.csv"'
      const name = getExportFilename(header, 'VISIT_REPORT', '2026-08-01', '2026-08-20')
      expect(name).toBe('custom-report-2026.csv')
    })

    it('extracts filename from Content-Disposition header with UTF-8 encoding', () => {
      const header = "attachment; filename*=UTF-8''bao-cao-kham-2026.csv"
      const name = getExportFilename(header, 'VISIT_REPORT', '2026-08-01', '2026-08-20')
      expect(name).toBe('bao-cao-kham-2026.csv')
    })

    it('falls back to visit-report filename when no header is present', () => {
      const name = getExportFilename(null, 'VISIT_REPORT', '2026-08-01', '2026-08-20')
      expect(name).toBe('visit-report-2026-08-01-to-2026-08-20.csv')
    })

    it('falls back to revenue-report filename when no header is present', () => {
      const name = getExportFilename(null, 'REVENUE_REPORT', '2026-08-01', '2026-08-20')
      expect(name).toBe('revenue-report-2026-08-01-to-2026-08-20.csv')
    })

    it('falls back to operational-report filename when no header is present', () => {
      const name = getExportFilename(null, 'OPERATIONAL_REPORT', '2026-08-01', '2026-08-20')
      expect(name).toBe('operational-report-2026-08-01-to-2026-08-20.csv')
    })
  })

  describe('getExportErrorMessage', () => {
    it('handles 403 Forbidden error', async () => {
      const error = { response: { status: 403 } }
      const msg = await getExportErrorMessage(error)
      expect(msg).toBe('Bạn không có quyền xuất báo cáo.')
    })

    it('handles 404 / REPORT_DATA_EMPTY error', async () => {
      const error = { response: { status: 404, data: { code: 'REPORT_DATA_EMPTY' } } }
      const msg = await getExportErrorMessage(error)
      expect(msg).toBe('Không có dữ liệu trong khoảng thời gian đã chọn.')
    })

    it('handles 400 Bad Request / validation error', async () => {
      const error = { response: { status: 400 } }
      const msg = await getExportErrorMessage(error)
      expect(msg).toBe('Khoảng thời gian không hợp lệ.')
    })

    it('handles 500 Server Error', async () => {
      const error = { response: { status: 500 } }
      const msg = await getExportErrorMessage(error)
      expect(msg).toBe('Không thể xuất báo cáo. Vui lòng thử lại.')
    })
  })
})
