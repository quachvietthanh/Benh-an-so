import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getRemainingQuantity,
  validateDispenseQuantity,
  calculateItemShortage,
  buildPartialDispensePayload,
  hasShortageAfterDispense,
  mapDispenseError,
  canUserDispense,
  canUserViewDispenseHistory,
  calculateBillingItemAmount,
} from './partialDispensingHelpers.js'

test('getRemainingQuantity correctly returns remaining quantity or defaults to (quantity - dispensedQuantity)', () => {
  assert.equal(getRemainingQuantity(null), 0)
  assert.equal(getRemainingQuantity({ remainingQuantity: 5 }), 5)
  assert.equal(getRemainingQuantity({ quantity: 10, dispensedQuantity: 4 }), 6)
  assert.equal(getRemainingQuantity({ quantity: 10, dispensedQuantity: 12 }), 0)
  assert.equal(getRemainingQuantity({ remainingQuantity: -2 }), 0)
})

test('validateDispenseQuantity validates input bounds: 0 <= quantity <= remainingQuantity', () => {
  // Valid bounds
  assert.deepEqual(validateDispenseQuantity(0, 10), { isValid: true, error: null })
  assert.deepEqual(validateDispenseQuantity(5, 10), { isValid: true, error: null })
  assert.deepEqual(validateDispenseQuantity(10, 10), { isValid: true, error: null })
  assert.deepEqual(validateDispenseQuantity('7', 10), { isValid: true, error: null })

  // Invalid: exceeds remaining
  const exceedRes = validateDispenseQuantity(11, 10)
  assert.equal(exceedRes.isValid, false)
  assert.match(exceedRes.error, /không được vượt quá số lượng còn lại/)

  // Invalid: negative quantity
  const negativeRes = validateDispenseQuantity(-1, 10)
  assert.equal(negativeRes.isValid, false)
  assert.match(negativeRes.error, /không được âm/)

  // Invalid: empty or non-numeric
  assert.equal(validateDispenseQuantity('', 10).isValid, false)
  assert.equal(validateDispenseQuantity(null, 10).isValid, false)
  assert.equal(validateDispenseQuantity('abc', 10).isValid, false)
})

test('calculateItemShortage accurately computes remaining shortage when input < remaining', () => {
  assert.equal(calculateItemShortage(4, 10), 6)
  assert.equal(calculateItemShortage(10, 10), 0)
  assert.equal(calculateItemShortage(0, 5), 5)
  assert.equal(calculateItemShortage(12, 10), 0)
})

test('buildPartialDispensePayload STRIPS OUT items with quantity <= 0 (Backend @Min(1) contract)', () => {
  const items = [
    { id: 'item-1', prescriptionItemId: 'item-1', remainingQuantity: 10 },
    { id: 'item-2', prescriptionItemId: 'item-2', remainingQuantity: 5 },
    { id: 'item-3', prescriptionItemId: 'item-3', remainingQuantity: 8 },
  ]

  // Case 1: item-2 has input quantity = 0, item-1 has 6, item-3 left default (8)
  const quantities = {
    'item-1': 6,
    'item-2': 0, // This MUST be filtered out completely
  }

  const { payloadItems, hasAnyItemToDispense } = buildPartialDispensePayload(quantities, items)

  assert.equal(hasAnyItemToDispense, true)
  assert.equal(payloadItems.length, 2)
  // item-2 is completely removed
  assert.equal(payloadItems.some((pi) => pi.prescriptionItemId === 'item-2'), false)
  // item-1 has 6
  const p1 = payloadItems.find((pi) => pi.prescriptionItemId === 'item-1')
  assert.deepEqual(p1, { prescriptionItemId: 'item-1', quantity: 6 })
  // item-3 took default remaining = 8
  const p3 = payloadItems.find((pi) => pi.prescriptionItemId === 'item-3')
  assert.deepEqual(p3, { prescriptionItemId: 'item-3', quantity: 8 })

  // Every single item in payloadItems MUST have quantity >= 1
  payloadItems.forEach((pi) => {
    assert.ok(pi.quantity >= 1, `Payload item ${pi.prescriptionItemId} must have quantity >= 1, got ${pi.quantity}`)
  })
})

test('buildPartialDispensePayload handles all items set to 0', () => {
  const items = [
    { id: 'item-1', remainingQuantity: 5 },
    { id: 'item-2', remainingQuantity: 3 },
  ]
  const quantities = {
    'item-1': 0,
    'item-2': 0,
  }
  const { payloadItems, hasAnyItemToDispense } = buildPartialDispensePayload(quantities, items)
  assert.equal(payloadItems.length, 0)
  assert.equal(hasAnyItemToDispense, false)
})

test('hasShortageAfterDispense detects whether any item will remain unfulfilled', () => {
  const items = [
    { id: 'item-1', remainingQuantity: 10 },
    { id: 'item-2', remainingQuantity: 5 },
  ]

  // Case: item-1 dispensed 10, item-2 dispensed 3 -> shortage exists!
  assert.equal(hasShortageAfterDispense({ 'item-1': 10, 'item-2': 3 }, items), true)

  // Case: both dispensed in full -> no shortage
  assert.equal(hasShortageAfterDispense({ 'item-1': 10, 'item-2': 5 }, items), false)

  // Case: item-1 dispensed 0 -> shortage exists
  assert.equal(hasShortageAfterDispense({ 'item-1': 0, 'item-2': 5 }, items), true)
})

test('mapDispenseError accurately maps 409 INSUFFICIENT_STOCK with details.shortages', () => {
  const mockError = {
    response: {
      status: 409,
      data: {
        code: 'INSUFFICIENT_STOCK',
        message: 'Insufficient inventory',
        details: {
          shortages: [
            {
              prescriptionItemId: 'item-uuid-1',
              medicineName: 'Paracetamol 500mg',
              requiredQuantity: 10,
              availableQuantity: 4,
              shortageQuantity: 6,
            },
          ],
        },
      },
    },
  }

  const res = mapDispenseError(mockError, 'rx-123', [])
  assert.equal(res.status, 409)
  assert.equal(res.code, 'INSUFFICIENT_STOCK')
  assert.match(res.message, /Không đủ số lượng thuốc tồn kho khả dụng/)
  assert.equal(res.shortages.length, 1)
  assert.equal(res.shortages[0].shortageQuantity, 6)
})

test('mapDispenseError accurately maps 409 closed / cancelled prescription', () => {
  const mockError = {
    response: {
      status: 409,
      data: {
        code: 'PRESCRIPTION_ALREADY_DISPENSED',
        message: 'Prescription is already dispensed',
      },
    },
  }

  const res = mapDispenseError(mockError, 'rx-123', [])
  assert.equal(res.status, 409)
  assert.match(res.message, /Đơn thuốc đã được cấp phát đầy đủ hoặc đã bị hủy/)
})

test('mapDispenseError accurately maps 400 invalid quantity', () => {
  const mockError = {
    response: {
      status: 400,
      data: {
        code: 'INVALID_QUANTITY',
        message: 'Dispense quantity must be greater than zero',
      },
    },
  }

  const res = mapDispenseError(mockError, 'rx-123', [])
  assert.equal(res.status, 400)
  assert.match(res.message, /Dispense quantity must be greater than zero/)
})

test('mapDispenseError accurately maps 403 forbidden', () => {
  const mockError = {
    response: {
      status: 403,
      data: {
        code: 'FORBIDDEN',
      },
    },
  }

  const res = mapDispenseError(mockError, 'rx-123', [])
  assert.equal(res.status, 403)
  assert.match(res.message, /Bạn không có quyền thực hiện cấp phát thuốc/)
})

test('mapDispenseError accurately maps 500 error with defensive user message and console.error logging', () => {
  const loggedErrors = []
  const originalConsoleError = console.error
  console.error = (...args) => {
    loggedErrors.push(args)
  }

  try {
    const mockError = {
      response: {
        status: 500,
        data: {
          code: 'INTERNAL_SERVER_ERROR',
          message: 'Internal server error.',
        },
      },
    }

    const payload = [{ prescriptionItemId: 'p-1', quantity: 5 }]
    const res = mapDispenseError(mockError, 'rx-999', payload)

    assert.equal(res.status, 500)
    // User message MUST NOT be raw "Internal server error."
    assert.notEqual(res.message, 'Internal server error.')
    assert.match(res.message, /Hệ thống đang gặp sự cố khi xử lý cấp phát một phần/)
    assert.match(res.message, /liên hệ quản trị viên hệ thống/)

    // Console logging MUST capture [PartialDispense 500 Error] and details for backend team
    assert.ok(loggedErrors.length > 0)
    const [tag, payloadLog] = loggedErrors[0]
    assert.equal(tag, '[PartialDispense 500 Error]')
    assert.equal(payloadLog.prescriptionId, 'rx-999')
    assert.deepEqual(payloadLog.payload, payload)
  } finally {
    console.error = originalConsoleError
  }
})

test('canUserDispense role-based access control enforces PHARMACIST/ADMIN only, blocks DOCTOR', () => {
  // Pharmacist can dispense
  assert.equal(canUserDispense(['PHARMACIST'], []), true)
  assert.equal(canUserDispense(['ROLE_PHARMACIST'], []), true)

  // Admin can dispense
  assert.equal(canUserDispense(['ADMIN'], []), true)
  assert.equal(canUserDispense(['ROLE_ADMIN'], []), true)

  // Permission-based dispenser
  assert.equal(canUserDispense([], ['PRESCRIPTION_UPDATE_STATUS']), true)

  // Doctor CANNOT dispense directly
  assert.equal(canUserDispense(['DOCTOR'], []), false)
  assert.equal(canUserDispense(['ROLE_DOCTOR'], []), false)

  // Receptionist CANNOT dispense
  assert.equal(canUserDispense(['RECEPTIONIST'], []), false)
})

test('canUserViewDispenseHistory allows DOCTOR, PHARMACIST, ADMIN, MANAGER to view history', () => {
  assert.equal(canUserViewDispenseHistory(['DOCTOR'], []), true)
  assert.equal(canUserViewDispenseHistory(['PHARMACIST'], []), true)
  assert.equal(canUserViewDispenseHistory(['ADMIN'], []), true)
  assert.equal(canUserViewDispenseHistory(['MANAGER'], []), true)
  assert.equal(canUserViewDispenseHistory([], ['PRESCRIPTION_DISPENSE_HISTORY_READ']), true)
  assert.equal(canUserViewDispenseHistory(['PATIENT'], []), false)
})

test('calculateBillingItemAmount uses dispensedQuantity for PARTIALLY_DISPENSED prescriptions', () => {
  const item = {
    medicineName: 'Amoxicillin 500mg',
    quantity: 20, // Kê 20
    dispensedQuantity: 12, // Thực cấp 12
    unitPrice: 5000,
  }

  // Under PARTIALLY_DISPENSED status: Must calculate by dispensedQuantity (12 * 5000 = 60,000)
  const partialRes = calculateBillingItemAmount(item, 'PARTIALLY_DISPENSED')
  assert.equal(partialRes.effectiveQty, 12)
  assert.equal(partialRes.amount, 60000)

  // Under normal PENDING / CREATED status: Calculates by prescribed quantity (20 * 5000 = 100,000)
  const pendingRes = calculateBillingItemAmount(item, 'PENDING_DISPENSE')
  assert.equal(pendingRes.effectiveQty, 20)
  assert.equal(pendingRes.amount, 100000)

  // If dispensedQuantity is 0 under PARTIALLY_DISPENSED (item not dispensed in this round)
  const zeroItem = {
    quantity: 10,
    dispensedQuantity: 0,
    unitPrice: 15000,
  }
  const zeroRes = calculateBillingItemAmount(zeroItem, 'PARTIALLY_DISPENSED')
  assert.equal(zeroRes.effectiveQty, 0)
  assert.equal(zeroRes.amount, 0)
})
