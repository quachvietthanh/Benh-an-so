import test from 'node:test'
import assert from 'node:assert/strict'

import {
  PROCUREMENT_STATUS,
  calculateSuggestedQuantity,
  hasEnoughConsumptionHistory,
  getProcurementStatusMeta,
  canApproveOrRejectPlan,
  validateProcurementPlanForm,
  validateRejectionReason,
  formatDate,
  formatDateTime,
  getSuggestionFormulaText,
} from './medicationProcurementHelpers.js'

test('TC-MP-01: calculateSuggestedQuantity - Tính số lượng gợi ý mua theo công thức chuẩn', () => {
  // Gợi ý = max(0, Lượng cấp phát kỳ trước + Tồn tối thiểu - Tồn khả dụng)
  // Tồn hiện tại = 10, Tồn tối thiểu = 50, Tiêu thụ kỳ trước = 100 -> Gợi ý = 50 + 100 - 10 = 140
  const suggested1 = calculateSuggestedQuantity(10, 50, 100)
  assert.equal(suggested1, 140)

  // Trường hợp tồn kho lớn hơn nhu cầu (không cần mua thêm): Gợi ý = max(0, 50 + 20 - 150) = 0
  const suggested2 = calculateSuggestedQuantity(150, 50, 20)
  assert.equal(suggested2, 0)

  // Trường hợp hết hàng (tồn = 0): Gợi ý = 20 + 30 - 0 = 50
  const suggested3 = calculateSuggestedQuantity(0, 20, 30)
  assert.equal(suggested3, 50)
})

test('TC-MP-02: calculateSuggestedQuantity - Thuốc mới chưa có dữ liệu lịch sử tiêu thụ phải trả về null', () => {
  // Thuốc mới nhập danh mục, chưa có lịch sử tiêu thụ -> null (dược sĩ phải tự nhập)
  assert.equal(calculateSuggestedQuantity(5, 20, null), null)
  assert.equal(calculateSuggestedQuantity(5, 20, undefined), null)
  assert.equal(calculateSuggestedQuantity(5, 20, 'INVALID'), null)
  assert.equal(calculateSuggestedQuantity(5, 20, -1), null)
})

test('TC-MP-03: hasEnoughConsumptionHistory - Nhận diện chính xác thuốc có hay chưa có lịch sử tiêu thụ', () => {
  assert.equal(hasEnoughConsumptionHistory({ previousPeriodConsumption: 45 }), true)
  assert.equal(hasEnoughConsumptionHistory({ previousPeriodConsumption: 0 }), true)
  assert.equal(hasEnoughConsumptionHistory({ previousPeriodConsumption: null }), false)
  assert.equal(hasEnoughConsumptionHistory({ previousPeriodConsumption: undefined }), false)
  assert.equal(hasEnoughConsumptionHistory({ previousPeriodConsumption: 20, hasConsumptionData: false }), false)
  assert.equal(hasEnoughConsumptionHistory(null), false)
})

test('TC-MP-04: getProcurementStatusMeta - Ánh xạ đầy đủ nhãn và quyền cho tất cả các trạng thái', () => {
  // PENDING_APPROVAL: Chờ duyệt, cho phép duyệt và chỉnh sửa (bản phía dược sĩ)
  const pending = getProcurementStatusMeta(PROCUREMENT_STATUS.PENDING_APPROVAL)
  assert.equal(pending.label, 'Chờ duyệt')
  assert.equal(pending.canApprove, true)

  // APPROVED: Đã duyệt, khóa sửa và khóa duyệt lại
  const approved = getProcurementStatusMeta(PROCUREMENT_STATUS.APPROVED)
  assert.equal(approved.label, 'Đã duyệt')
  assert.equal(approved.canApprove, false)
  assert.equal(approved.canEdit, false)

  // REJECTED: Bị từ chối
  const rejected = getProcurementStatusMeta(PROCUREMENT_STATUS.REJECTED)
  assert.equal(rejected.label, 'Bị từ chối')
  assert.equal(rejected.canApprove, false)

  // DRAFT: Bản nháp
  const draft = getProcurementStatusMeta(PROCUREMENT_STATUS.DRAFT)
  assert.equal(draft.label, 'Bản nháp')
  assert.equal(draft.canEdit, true)
  assert.equal(draft.canApprove, false)

  // UNKNOWN fallback
  const unknown = getProcurementStatusMeta('UNKNOWN_STATUS')
  assert.equal(unknown.label, 'UNKNOWN_STATUS')
})

test('TC-MP-05: canApproveOrRejectPlan - Áp dụng nghiêm ngặt quy tắc SoD (Separation of Duties)', () => {
  const managerId = 'mgr-001'
  const pharmacistId = 'pharm-002'

  // Trường hợp vi phạm SoD: Dược sĩ lập phiếu tự duyệt phiếu của chính mình
  const ownPlan = {
    procurementPlanId: 'plan-101',
    status: PROCUREMENT_STATUS.PENDING_APPROVAL,
    createdBy: pharmacistId,
  }
  const selfApproveCheck = canApproveOrRejectPlan(ownPlan, pharmacistId)
  assert.equal(selfApproveCheck.allowed, false)
  assert.equal(selfApproveCheck.isSoDViolation, true)
  assert.match(selfApproveCheck.reason, /SoD/i)

  // Trường hợp hợp lệ: Quản lý phòng khám duyệt phiếu do Dược sĩ lập
  const validCheck = canApproveOrRejectPlan(ownPlan, managerId)
  assert.equal(validCheck.allowed, true)
  assert.equal(validCheck.reason, '')

  // Trường hợp phiếu không ở trạng thái PENDING_APPROVAL (ví dụ đã APPROVED)
  const approvedPlan = {
    procurementPlanId: 'plan-102',
    status: PROCUREMENT_STATUS.APPROVED,
    createdBy: pharmacistId,
  }
  const approvedCheck = canApproveOrRejectPlan(approvedPlan, managerId)
  assert.equal(approvedCheck.allowed, false)
  assert.match(approvedCheck.reason, /Chờ duyệt/i)
})

test('TC-MP-06: validateProcurementPlanForm - Kiểm tra tính toàn vẹn của danh sách thuốc dự trù', () => {
  // Không có thuốc nào trong phiếu
  const emptyCheck = validateProcurementPlanForm([], 'Ghi chú')
  assert.equal(emptyCheck.valid, false)
  assert.match(emptyCheck.errors[0], /ít nhất một loại thuốc/i)

  // Trùng lặp thuốc trong danh sách
  const duplicateItems = [
    { medicineId: 'med-1', medicineName: 'Paracetamol 500mg', proposedQuantity: 100 },
    { medicineId: 'med-1', medicineName: 'Paracetamol 500mg', proposedQuantity: 50 },
  ]
  const dupCheck = validateProcurementPlanForm(duplicateItems)
  assert.equal(dupCheck.valid, false)
  assert.ok(dupCheck.errors.some(e => e.includes('trùng lặp')))

  // Số lượng không hợp lệ (<= 0 hoặc thập phân hoặc rỗng)
  const invalidQtyItems = [
    { medicineId: 'med-1', medicineName: 'Paracetamol 500mg', proposedQuantity: 0 },
    { medicineId: 'med-2', medicineName: 'Amoxicillin 500mg', proposedQuantity: -5 },
    { medicineId: 'med-3', medicineName: 'Ibuprofen 400mg', proposedQuantity: 12.5 },
  ]
  const qtyCheck = validateProcurementPlanForm(invalidQtyItems)
  assert.equal(qtyCheck.valid, false)
  assert.ok(qtyCheck.errors.some(e => e.includes('số nguyên > 0')))

  // Dữ liệu hợp lệ hoàn chỉnh
  const validItems = [
    { medicineId: 'med-1', medicineName: 'Paracetamol 500mg', proposedQuantity: 100 },
    { medicineId: 'med-2', medicineName: 'Amoxicillin 500mg', proposedQuantity: 50 },
  ]
  const validResult = validateProcurementPlanForm(validItems, 'Dự trù tháng 10')
  assert.equal(validResult.valid, true)
  assert.equal(validResult.errors.length, 0)
})

test('TC-MP-07: validateRejectionReason - Bắt buộc lý do từ chối tối thiểu 5 ký tự', () => {
  // Rỗng hoặc chỉ có khoảng trắng
  assert.equal(validateRejectionReason('').valid, false)
  assert.equal(validateRejectionReason('   ').valid, false)
  assert.equal(validateRejectionReason(null).valid, false)

  // Dưới 5 ký tự (ví dụ: 'Lỗi' có 3 ký tự)
  const tooShort = validateRejectionReason('Lỗi')
  assert.equal(tooShort.valid, false)
  assert.match(tooShort.message, /ít nhất 5 ký tự/i)

  // Hợp lệ
  const valid = validateRejectionReason('Số lượng quá lớn so với ngân sách tháng')
  assert.equal(valid.valid, true)
  assert.equal(valid.message, '')
})

test('TC-MP-08: getSuggestionFormulaText - Sinh thông điệp công thức trực quan cho tooltip', () => {
  const textWithData = getSuggestionFormulaText(10, 50, 100, 140)
  assert.match(textWithData, /Tồn tối thiểu \(50\)/)
  assert.match(textWithData, /Tiêu thụ kỳ trước \(100\)/)
  assert.match(textWithData, /Tồn hiện tại \(10\)/)
  assert.match(textWithData, /= 140/)

  const textNoData = getSuggestionFormulaText(10, 50, null, null)
  assert.match(textNoData, /Chưa đủ dữ liệu tiêu thụ/i)
})
