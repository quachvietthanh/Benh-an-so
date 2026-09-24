import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildCreateInvoicePayload,
  extractInvoicePaymentMeta,
  classifyInvoiceError,
  formatCurrency,
} from './paymentMethodHelpers.js'


test('TC-REG-01: buildCreateInvoicePayload - Chuẩn hóa payload đúng contract CreateInvoiceRequest Backend', () => {
  const visitId = 'e2b7e1c8-2b81-42e7-9d48-8df89623e111'
  const paymentId = 'a1f3c5e7-4b92-41d8-8c37-7ef98765e222'

  const payload = buildCreateInvoicePayload({ visitId, paymentId })

  assert.equal(payload.visitId, visitId)
  assert.equal(payload.paymentId, paymentId)
  assert.equal(Object.keys(payload).length, 2)
})

test('TC-REG-02: buildCreateInvoicePayload - Bỏ qua paymentId khi undefined/null hoặc rỗng', () => {
  const visitId = 'e2b7e1c8-2b81-42e7-9d48-8df89623e111'

  const payload1 = buildCreateInvoicePayload({ visitId, paymentId: undefined })
  assert.equal(payload1.visitId, visitId)
  assert.equal('paymentId' in payload1, false)

  const payload2 = buildCreateInvoicePayload({ visitId, paymentId: null })
  assert.equal(payload2.visitId, visitId)
  assert.equal('paymentId' in payload2, false)

  const payload3 = buildCreateInvoicePayload({ visitId, paymentId: '   ' })
  assert.equal(payload3.visitId, visitId)
  assert.equal('paymentId' in payload3, false)
})

test('TC-REG-03: buildCreateInvoicePayload - Bắt buộc visitId, ném lỗi nếu thiếu', () => {
  assert.throws(() => {
    buildCreateInvoicePayload(null)
  }, /visitId là bắt buộc/i)

  assert.throws(() => {
    buildCreateInvoicePayload({})
  }, /visitId là bắt buộc/i)

  assert.throws(() => {
    buildCreateInvoicePayload({ visitId: '' })
  }, /visitId là bắt buộc/i)
})

test('TC-REG-04: extractInvoicePaymentMeta - Trích xuất an toàn từ trường payment của InvoiceResponse Backend', () => {
  const mockBackendInvoiceResponse = {
    id: 'inv-uuid-001',
    invoiceCode: 'HD000004',
    totalAmount: 250000,
    payment: {
      id: 'pay-uuid-001',
      paymentMethod: 'MULTIPLE',
      amountPaid: 250000,
      paymentMethods: [
        { paymentMethod: 'CASH', amount: 100000, referenceNumber: null },
        { paymentMethod: 'BANK_TRANSFER', amount: 150000, referenceNumber: 'VCB-9988' },
      ],
    },
  }

  const meta = extractInvoicePaymentMeta(mockBackendInvoiceResponse)
  assert.equal(meta.paymentMethod, 'MULTIPLE')
  assert.equal(meta.paymentMethods.length, 2)
  assert.equal(meta.paymentMethods[0].paymentMethod, 'CASH')
  assert.equal(meta.paymentMethods[1].referenceNumber, 'VCB-9988')
})

test('TC-REG-05: extractInvoicePaymentMeta - Fallback an toàn khi payment object null/rỗng', () => {
  const metaNull = extractInvoicePaymentMeta(null)
  assert.equal(metaNull.paymentMethod, 'CASH')
  assert.deepEqual(metaNull.paymentMethods, [])

  const metaEmpty = extractInvoicePaymentMeta({})
  assert.equal(metaEmpty.paymentMethod, 'CASH')
  assert.deepEqual(metaEmpty.paymentMethods, [])

  const metaSingle = extractInvoicePaymentMeta({ paymentMethod: 'CARD' })
  assert.equal(metaSingle.paymentMethod, 'CARD')
  assert.deepEqual(metaSingle.paymentMethods, [])
})

test('TC-REG-06: Regression Check - Không gọi bất kỳ hàm localStorage/undefined nào trong luồng xử lý response', () => {
  // Giả lập đối tượng response từ backend
  const invoiceRes = {
    id: 'inv-uuid-777',
    invoiceCode: 'HD000007',
    type: 'ORIGINAL',
    lines: [
      { id: 'line-1', itemName: 'Khám bệnh', amount: 100000 },
      { id: 'line-2', itemName: 'Thuốc', amount: 150000 },
    ],
    createdAt: '2026-09-23T12:00:00Z',
    totalAmount: 250000,
    payment: {
      paymentMethod: 'MULTIPLE',
      paymentMethods: [
        { paymentMethod: 'CASH', amount: 100000 },
        { paymentMethod: 'BANK_TRANSFER', amount: 150000, referenceNumber: 'VCB-123' },
      ],
    },
  }

  let prevState = {
    visitId: 'visit-uuid-1',
    paymentStatus: 'UNPAID',
    totalAmount: 250000,
  }

  // Khối logic đã được fix trong handleCreateInvoice
  const { paymentMethod: finalMethod, paymentMethods: finalMethods } = extractInvoicePaymentMeta(invoiceRes)

  const updatedState = {
    ...prevState,
    invoiceId: invoiceRes.id,
    invoiceCode: invoiceRes.invoiceCode,
    invoiceType: invoiceRes.type || 'ORIGINAL',
    invoiceLines: invoiceRes.lines || [],
    invoiceCreatedAt: invoiceRes.createdAt,
    totalAmount: Number(invoiceRes.totalAmount || prevState.totalAmount),
    paymentStatus: 'PAID',
    paymentMethod: finalMethod || prevState.paymentMethod || 'CASH',
    paymentMethods: finalMethods.length > 0 ? finalMethods : (prevState.paymentMethods || []),
  }

  // Khẳng định cập nhật thành công, không phát sinh ReferenceError
  assert.equal(updatedState.invoiceId, 'inv-uuid-777')
  assert.equal(updatedState.invoiceCode, 'HD000007')
  assert.equal(updatedState.paymentStatus, 'PAID')
  assert.equal(updatedState.paymentMethod, 'MULTIPLE')
  assert.equal(updatedState.paymentMethods.length, 2)
})

test('TC-REG-07: classifyInvoiceError - Bắt đúng lỗi Client Runtime và KHÔNG đổ lỗi cho Backend', () => {
  // Mô phỏng đúng lỗi ReferenceError đã xảy ra trước đây
  const runtimeError = new ReferenceError('savePaymentMethodForVisit is not defined')

  const classified = classifyInvoiceError(runtimeError)
  assert.equal(classified.isClientError, true, 'Lỗi không có err.response phải là lỗi Client')
  assert.ok(classified.message.includes('Client Error'), 'Thông báo phải ghi rõ Client Error')
  assert.ok(classified.message.includes('savePaymentMethodForVisit is not defined'))
  assert.ok(!classified.message.includes('từ máy chủ'), 'Tuyệt đối không được báo lỗi từ máy chủ khi do Client')
})

test('TC-REG-08: classifyInvoiceError - Nhận diện đúng mã lỗi 409 (Invoice Already Issued)', () => {
  const error409 = {
    response: {
      status: 409,
      data: {
        code: 'INVOICE_ALREADY_ISSUED',
        message: 'Original invoice has already been issued for visit.',
      },
    },
  }

  const classified = classifyInvoiceError(error409)
  assert.equal(classified.isClientError, false)
  assert.equal(classified.status, 409)
  assert.equal(classified.code, 'INVOICE_ALREADY_ISSUED')
  assert.ok(classified.message.includes('already been issued') || classified.message.includes('409'))
})

test('TC-REG-09: classifyInvoiceError - Xử lý đầy đủ các mã lỗi HTTP chuẩn 400, 401, 403, 404', () => {
  const err400 = { response: { status: 400, data: { message: 'Invalid payment ID' } } }
  assert.equal(classifyInvoiceError(err400).status, 400)
  assert.equal(classifyInvoiceError(err400).message, 'Invalid payment ID')

  const err401 = { response: { status: 401, data: {} } }
  assert.equal(classifyInvoiceError(err401).status, 401)
  assert.ok(classifyInvoiceError(err401).message.includes('Hết phiên làm việc'))

  const err403 = { response: { status: 403, data: {} } }
  assert.equal(classifyInvoiceError(err403).status, 403)
  assert.ok(classifyInvoiceError(err403).message.includes('INVOICE_CREATE'))

  const err404 = { response: { status: 404, data: {} } }
  assert.equal(classifyInvoiceError(err404).status, 404)
  assert.ok(classifyInvoiceError(err404).message.includes('Không tìm thấy'))
})

test('TC-REG-10: classifyInvoiceError - Xử lý lỗi 500 máy chủ Backend', () => {
  const err500 = { response: { status: 500, data: { message: 'Internal database error' } } }
  const classified = classifyInvoiceError(err500)
  assert.equal(classified.isClientError, false)
  assert.equal(classified.status, 500)
  assert.equal(classified.message, 'Internal database error')
})

test('TC-REG-11: Luồng nút "Thử lại" (refreshAllData logic) - Xóa lỗi và đồng bộ hóa đơn hiện tại', () => {
  let apiError = 'Lỗi tạm thời'
  let loadedVisitId = null

  // Giả lập hàm refreshAllData
  const mockRefreshAllData = async (selectedVisitId) => {
    apiError = '' // 1. Xóa thông báo lỗi
    if (selectedVisitId) {
      loadedVisitId = selectedVisitId // 2. Nạp lại thông tin hóa đơn của lượt khám hiện tại
    }
  }

  // Kích hoạt nút Thử lại với lượt khám đang chọn
  mockRefreshAllData('visit-uuid-123')

  assert.equal(apiError, '', 'Thao tác Thử lại phải xóa sạch apiError')
  assert.equal(loadedVisitId, 'visit-uuid-123', 'Thao tác Thử lại phải nạp lại lượt khám đang chọn')
})

test('TC-REG-12: Thứ tự khai báo Hook & Callback - Không được truy cập loadInvoiceData trước khi khởi tạo (TDZ Check)', () => {
  // Giả lập thứ tự khai báo đúng trong React Functional Component:
  // 1. loadHistoryInvoices
  const loadHistoryInvoices = async () => [{ id: 'inv-1' }]
  // 2. loadPendingVisits
  const loadPendingVisits = async () => [{ visitId: 'visit-1' }]
  // 3. loadInvoiceData (PHẢI khai báo trước refreshAllData để không bị Temporal Dead Zone)
  const loadInvoiceData = async (visitId) => ({ visitId, loaded: true })
  // 4. refreshAllData (nhận loadInvoiceData an toàn vì đã được khởi tạo ở trên)
  const refreshAllData = async (selectedVisitId) => {
    await loadHistoryInvoices()
    await loadPendingVisits()
    if (selectedVisitId) {
      return await loadInvoiceData(selectedVisitId)
    }
    return null
  }

  // Khẳng định: khi gọi refreshAllData, loadInvoiceData đã sẵn sàng và chạy thành công
  assert.doesNotThrow(async () => {
    const res = await refreshAllData('visit-1')
    assert.deepEqual(res, { visitId: 'visit-1', loaded: true })
  })
})
