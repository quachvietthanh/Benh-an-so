import test from 'node:test'
import assert from 'node:assert/strict'
import {
  PRESET_PRIORITY_REASONS,
  validatePrioritizeForm,
  canPrioritize,
  getPriorityTag,
  mapPrioritizeErrorMessage,
  sortQueueItemsByPriority,
} from './queuePriorityHelpers.js'

test('1. validatePrioritizeForm - Chặn mức độ ưu tiên không hợp lệ hoặc NORMAL', () => {
  // Chặn NORMAL (Backend và Frontend chỉ cho phép EMERGENCY hoặc PRIORITY)
  const normalResult = validatePrioritizeForm('NORMAL', 'Lý do hợp lệ')
  assert.equal(normalResult.valid, false)
  assert.match(normalResult.error, /Mức độ ưu tiên bắt buộc phải là/i)

  // Chặn rỗng / undefined
  const emptyPriority = validatePrioritizeForm('', 'Lý do hợp lệ')
  assert.equal(emptyPriority.valid, false)

  const invalidPriority = validatePrioritizeForm('UNKNOWN', 'Lý do hợp lệ')
  assert.equal(invalidPriority.valid, false)
})

test('2. validatePrioritizeForm - Chặn lý do rỗng, chỉ có khoảng trắng hoặc vượt 500 ký tự', () => {
  // Lý do rỗng
  const emptyReason = validatePrioritizeForm('EMERGENCY', '')
  assert.equal(emptyReason.valid, false)
  assert.match(emptyReason.error, /Vui lòng nhập hoặc chọn lý do/i)

  // Lý do chỉ có dấu cách
  const whitespaceReason = validatePrioritizeForm('PRIORITY', '     ')
  assert.equal(whitespaceReason.valid, false)
  assert.match(whitespaceReason.error, /Vui lòng nhập hoặc chọn lý do/i)

  // Lý do không phải chuỗi
  const nonStringReason = validatePrioritizeForm('EMERGENCY', null)
  assert.equal(nonStringReason.valid, false)

  // Lý do vượt quá 500 ký tự
  const longReason = 'A'.repeat(501)
  const longResult = validatePrioritizeForm('EMERGENCY', longReason)
  assert.equal(longResult.valid, false)
  assert.match(longResult.error, /không được vượt quá 500 ký tự/i)

  // Lý do hợp lệ (chính xác 500 ký tự hoặc ngắn hơn)
  const valid500 = 'B'.repeat(500)
  const valid500Result = validatePrioritizeForm('EMERGENCY', valid500)
  assert.equal(valid500Result.valid, true)
  assert.equal(valid500Result.error, null)
  assert.equal(valid500Result.trimmedReason.length, 500)

  // Trường hợp chuẩn
  const validNormal = validatePrioritizeForm('PRIORITY', '  Người cao tuổi 80 tuổi, khó thở nhẹ  ')
  assert.equal(validNormal.valid, true)
  assert.equal(validNormal.error, null)
  assert.equal(validNormal.trimmedReason, 'Người cao tuổi 80 tuổi, khó thở nhẹ')
})

test('3. canPrioritize - Chỉ cho phép khi trạng thái là WAITING', () => {
  // Trạng thái hợp lệ
  assert.equal(canPrioritize('WAITING'), true)
  assert.equal(canPrioritize('waiting'), true)

  // Các trạng thái khác bị từ chối
  assert.equal(canPrioritize('IN_PROGRESS'), false)
  assert.equal(canPrioritize('WAITING_FOR_RESULT'), false)
  assert.equal(canPrioritize('COMPLETED'), false)
  assert.equal(canPrioritize('CANCELLED'), false)
  assert.equal(canPrioritize('SKIPPED'), false)
  assert.equal(canPrioritize('EARLY_ENDED'), false)
  assert.equal(canPrioritize(''), false)
  assert.equal(canPrioritize(null), false)
  assert.equal(canPrioritize(undefined), false)
})

test('4. getPriorityTag - Ánh xạ đúng 3 mức độ ưu tiên', () => {
  // Mức CẤP CỨU (EMERGENCY)
  const emergencyTag = getPriorityTag('EMERGENCY')
  assert.ok(emergencyTag)
  assert.equal(emergencyTag.label, 'CẤP CỨU')
  assert.equal(emergencyTag.color, 'red')
  assert.equal(emergencyTag.isEmergency, true)
  assert.equal(emergencyTag.rank, 1)

  // Case insensitive
  const emergencyLower = getPriorityTag('emergency')
  assert.ok(emergencyLower)
  assert.equal(emergencyLower.label, 'CẤP CỨU')

  // Mức ĐỐI TƯỢNG ƯU TIÊN (PRIORITY)
  const priorityTag = getPriorityTag('PRIORITY')
  assert.ok(priorityTag)
  assert.equal(priorityTag.label, 'ƯU TIÊN')
  assert.equal(priorityTag.color, 'orange')
  assert.equal(priorityTag.isEmergency, false)
  assert.equal(priorityTag.rank, 2)

  // Mức THƯỜNG (NORMAL) -> Không trả về Tag đặc biệt (null)
  assert.equal(getPriorityTag('NORMAL'), null)
  assert.equal(getPriorityTag('normal'), null)
  assert.equal(getPriorityTag(null), null)
  assert.equal(getPriorityTag(''), null)
})

test('5. mapPrioritizeErrorMessage - Ánh xạ đầy đủ các mã lỗi chuẩn', () => {
  // Lỗi 409 Conflict: Không phải trạng thái WAITING
  const err409 = { response: { status: 409 } }
  assert.match(
    mapPrioritizeErrorMessage(err409),
    /Chỉ có thể đánh dấu ưu tiên khi bệnh nhân đang ở trạng thái chờ khám/i,
  )

  // Lỗi 403 Forbidden: Không đủ quyền
  const err403 = { response: { status: 403 } }
  assert.match(
    mapPrioritizeErrorMessage(err403),
    /yêu cầu quyền Lễ tân hoặc Quản trị viên/i,
  )

  // Lỗi 404 Not Found: Thông báo thân thiện cho người dùng cuối, không lộ chi tiết kỹ thuật
  const err404 = { response: { status: 404 } }
  assert.equal(
    mapPrioritizeErrorMessage(err404),
    'Không thể kết nối đến tính năng này. Vui lòng thử lại sau ít phút hoặc liên hệ bộ phận kỹ thuật.',
  )

  const err404ResourceMsg = { response: { status: 404, data: { message: 'Resource not found.' } } }
  assert.equal(
    mapPrioritizeErrorMessage(err404ResourceMsg),
    'Không thể kết nối đến tính năng này. Vui lòng thử lại sau ít phút hoặc liên hệ bộ phận kỹ thuật.',
  )

  // Lỗi 400 Bad Request
  const err400WithMsg = { response: { status: 400, data: { message: 'Lý do không hợp lệ' } } }
  assert.equal(mapPrioritizeErrorMessage(err400WithMsg), 'Lý do không hợp lệ')

  const err400Default = { response: { status: 400 } }
  assert.match(
    mapPrioritizeErrorMessage(err400Default),
    /Thông tin đánh dấu ưu tiên không hợp lệ/i,
  )

  // Lỗi chung khác
  const errGeneric = { response: { status: 500, data: { message: 'Lỗi máy chủ' } } }
  assert.equal(mapPrioritizeErrorMessage(errGeneric), 'Lỗi máy chủ')

  const errNetwork = new Error('Network error')
  assert.match(mapPrioritizeErrorMessage(errNetwork), /Không thể đánh dấu ưu tiên/i)
})

test('6. PRESET_PRIORITY_REASONS - Cung cấp đầy đủ danh mục gợi ý lý do mẫu', () => {
  assert.ok(Array.isArray(PRESET_PRIORITY_REASONS.EMERGENCY))
  assert.ok(PRESET_PRIORITY_REASONS.EMERGENCY.length >= 5)
  assert.ok(PRESET_PRIORITY_REASONS.EMERGENCY.includes('Sốt cao co giật'))
  assert.ok(PRESET_PRIORITY_REASONS.EMERGENCY.includes('Đau ngực dữ dội/khó thở'))

  assert.ok(Array.isArray(PRESET_PRIORITY_REASONS.PRIORITY))
  assert.ok(PRESET_PRIORITY_REASONS.PRIORITY.length >= 4)
  assert.ok(PRESET_PRIORITY_REASONS.PRIORITY.includes('Người cao tuổi (>75 tuổi)'))
  assert.ok(PRESET_PRIORITY_REASONS.PRIORITY.includes('Phụ nữ có thai'))
})

test('7. sortQueueItemsByPriority - Sắp xếp bảo toàn thứ tự EMERGENCY -> PRIORITY -> NORMAL', () => {
  const items = [
    { id: '1', queueNumber: 1, priority: 'NORMAL' },
    { id: '2', queueNumber: 2, priority: 'PRIORITY', prioritizedAt: '2026-09-22T08:10:00Z' },
    { id: '3', queueNumber: 3, priority: 'EMERGENCY', prioritizedAt: '2026-09-22T08:15:00Z' },
    { id: '4', queueNumber: 4, priority: 'NORMAL' },
    { id: '5', queueNumber: 5, priority: 'EMERGENCY', prioritizedAt: '2026-09-22T08:05:00Z' },
  ]

  const sorted = sortQueueItemsByPriority(items)

  // ID 5 là EMERGENCY được ưu tiên trước ID 3 (vì prioritizedAt 08:05 < 08:15)
  assert.equal(sorted[0].id, '5')
  assert.equal(sorted[1].id, '3')
  // Sau đó đến PRIORITY
  assert.equal(sorted[2].id, '2')
  // Sau đó đến NORMAL xếp theo queueNumber
  assert.equal(sorted[3].id, '1')
  assert.equal(sorted[4].id, '4')
})
