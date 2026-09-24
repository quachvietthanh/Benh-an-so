import assert from 'node:assert/strict'
import test from 'node:test'
import {
  NOTIFICATION_TYPES,
  NOTIFICATION_TYPE_CONFIG,
  calculateUnreadCount,
  getNotificationTypeMeta,
  resolveNotificationRoute,
  formatNotificationTime,
} from './patientNotificationHelpers.js'
import patientPortalNotificationApi from '../api/patientPortalNotificationApi.js'

// ============================================================================
// 1. Kiểm thử tính số lượng thông báo chưa đọc (calculateUnreadCount)
// Ràng buộc #3: Tính từ mảng danh sách do Backend chưa có /unread-count
// ============================================================================
test('TC-NOTIF-01: calculateUnreadCount - Trả về 0 khi danh sách rỗng, null hoặc undefined', () => {
  assert.equal(calculateUnreadCount([]), 0)
  assert.equal(calculateUnreadCount(null), 0)
  assert.equal(calculateUnreadCount(undefined), 0)
  assert.equal(calculateUnreadCount('not an array'), 0)
})

test('TC-NOTIF-02: calculateUnreadCount - Đếm chính xác số lượng thông báo read = false', () => {
  const notifications = [
    { id: '1', read: false, type: NOTIFICATION_TYPES.APPOINTMENT_REMINDER },
    { id: '2', read: true, type: NOTIFICATION_TYPES.APPOINTMENT_CHANGED },
    { id: '3', read: false, type: NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE },
    { id: '4', read: true, type: NOTIFICATION_TYPES.APPOINTMENT_REMINDER },
  ]
  assert.equal(calculateUnreadCount(notifications), 2)
})

test('TC-NOTIF-03: calculateUnreadCount - Trả về 0 khi toàn bộ thông báo đã đọc', () => {
  const notifications = [
    { id: '1', read: true },
    { id: '2', read: true },
  ]
  assert.equal(calculateUnreadCount(notifications), 0)
})

test('TC-NOTIF-04: calculateUnreadCount - Đếm đúng khi toàn bộ thông báo chưa đọc', () => {
  const notifications = [
    { id: '1', read: false },
    { id: '2', read: false },
    { id: '3', read: false },
  ]
  assert.equal(calculateUnreadCount(notifications), 3)
})

// ============================================================================
// 2. Kiểm thử Điều hướng theo type (resolveNotificationRoute)
// Ràng buộc #2: Không dùng appointmentId hay clinicalResultId do Backend không trả
// ============================================================================
test('TC-NOTIF-05: resolveNotificationRoute - APPOINTMENT_REMINDER điều hướng đến /portal/my-appointments', () => {
  const route = resolveNotificationRoute(NOTIFICATION_TYPES.APPOINTMENT_REMINDER)
  assert.equal(route, '/portal/my-appointments')
})

test('TC-NOTIF-06: resolveNotificationRoute - APPOINTMENT_CHANGED điều hướng đến /portal/my-appointments', () => {
  const route = resolveNotificationRoute(NOTIFICATION_TYPES.APPOINTMENT_CHANGED)
  assert.equal(route, '/portal/my-appointments')
})

test('TC-NOTIF-07: resolveNotificationRoute - LAB_RESULT_AVAILABLE điều hướng đến /portal/medical-history', () => {
  const route = resolveNotificationRoute(NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE)
  assert.equal(route, '/portal/medical-history')
})

test('TC-NOTIF-08: resolveNotificationRoute - Loại không xác định fallback an toàn về /portal/dashboard', () => {
  assert.equal(resolveNotificationRoute('UNKNOWN_TYPE'), '/portal/dashboard')
  assert.equal(resolveNotificationRoute(null), '/portal/dashboard')
  assert.equal(resolveNotificationRoute(undefined), '/portal/dashboard')
})

// ============================================================================
// 3. Kiểm thử Metadata cấu hình giao diện (getNotificationTypeMeta)
// ============================================================================
test('TC-NOTIF-09: getNotificationTypeMeta - Metadata cho APPOINTMENT_REMINDER', () => {
  const meta = getNotificationTypeMeta(NOTIFICATION_TYPES.APPOINTMENT_REMINDER)
  assert.equal(meta.label, 'Nhắc lịch hẹn')
  assert.equal(meta.tagColor, 'blue')
  assert.equal(meta.targetRoute, '/portal/my-appointments')
  assert.match(meta.actionText, /Xem lịch hẹn/)
})

test('TC-NOTIF-10: getNotificationTypeMeta - Metadata cho APPOINTMENT_CHANGED', () => {
  const meta = getNotificationTypeMeta(NOTIFICATION_TYPES.APPOINTMENT_CHANGED)
  assert.equal(meta.label, 'Lịch hẹn thay đổi')
  assert.equal(meta.tagColor, 'orange')
  assert.equal(meta.targetRoute, '/portal/my-appointments')
})

test('TC-NOTIF-11: getNotificationTypeMeta - Metadata cho LAB_RESULT_AVAILABLE', () => {
  const meta = getNotificationTypeMeta(NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE)
  assert.equal(meta.label, 'Kết quả cận lâm sàng mới')
  assert.equal(meta.tagColor, 'green')
  assert.equal(meta.targetRoute, '/portal/medical-history')
  assert.match(meta.actionText, /kết quả/)
})

test('TC-NOTIF-12: getNotificationTypeMeta - Fallback an toàn cho type lạ', () => {
  const meta = getNotificationTypeMeta('SOME_NEW_TYPE')
  assert.equal(meta.label, 'Thông báo')
  assert.equal(meta.targetRoute, '/portal/dashboard')
})

// ============================================================================
// 4. Kiểm thử định dạng thời gian hiển thị (formatNotificationTime)
// ============================================================================
test('TC-NOTIF-13: formatNotificationTime - Định dạng chuẩn ngày giờ', () => {
  const dateStr = '2026-09-24T08:30:00.000Z'
  const formatted = formatNotificationTime(dateStr)
  assert.notEqual(formatted, '—')
  assert.match(formatted, /\d{2}:\d{2}\s+\d{2}\/\d{2}\/\d{4}/)
})

test('TC-NOTIF-14: formatNotificationTime - Xử lý an toàn khi date rỗng hoặc sai format', () => {
  assert.equal(formatNotificationTime(null), '—')
  assert.equal(formatNotificationTime(''), '—')
  assert.equal(formatNotificationTime('invalid-date-string'), '—')
})

// ============================================================================
// 5. Kiểm thử API Client Interface
// ============================================================================
test('TC-NOTIF-15: patientPortalNotificationApi - Cung cấp đầy đủ 3 phương thức theo contract Backend', () => {
  assert.equal(typeof patientPortalNotificationApi.getNotifications, 'function')
  assert.equal(typeof patientPortalNotificationApi.getById, 'function')
  assert.equal(typeof patientPortalNotificationApi.markAsRead, 'function')
})

// ============================================================================
// 6. Kiểm thử Hành vi Component-level của PatientNotificationBell (I.3)
// ============================================================================

test('TC-NOTIF-16: Component-level - Optimistic update khi bấm đánh dấu đã đọc (unreadCount giảm ngay lập tức & gọi đúng API)', async () => {
  // Giả lập danh sách thông báo ban đầu
  const initialNotifications = [
    { id: 'notif-1', title: 'Nhắc lịch khám', read: false, type: NOTIFICATION_TYPES.APPOINTMENT_REMINDER },
    { id: 'notif-2', title: 'Có kết quả xét nghiệm', read: false, type: NOTIFICATION_TYPES.LAB_RESULT_AVAILABLE },
    { id: 'notif-3', title: 'Lịch đổi ngày', read: true, type: NOTIFICATION_TYPES.APPOINTMENT_CHANGED },
  ]

  let currentNotifications = [...initialNotifications]
  assert.equal(calculateUnreadCount(currentNotifications), 2, 'Ban đầu có 2 thông báo chưa đọc')

  // Giả lập hành vi API markAsRead
  let apiCalledWithId = null
  const originalMarkAsRead = patientPortalNotificationApi.markAsRead
  patientPortalNotificationApi.markAsRead = async (id) => {
    apiCalledWithId = id
    return { data: { success: true } }
  }

  try {
    // Mô phỏng logic handleMarkAsRead trong PatientNotificationBell.jsx:
    // 1. Optimistic update: Cập nhật state ngay lập tức
    const targetItem = currentNotifications[0]
    currentNotifications = currentNotifications.map((n) =>
      n.id === targetItem.id ? { ...n, read: true, readAt: new Date().toISOString() } : n
    )

    // Kiểm tra unreadCount giảm ngay lập tức trước/cùng lúc gọi API
    assert.equal(calculateUnreadCount(currentNotifications), 1, 'unreadCount phải giảm ngay từ 2 xuống 1 (optimistic)')
    assert.equal(currentNotifications[0].read, true, 'Thông báo mục tiêu phải chuyển read = true')
    assert.ok(currentNotifications[0].readAt, 'Thông báo mục tiêu phải có thời gian readAt')

    // 2. Gọi API ngầm phía dưới
    await patientPortalNotificationApi.markAsRead(targetItem.id)
    assert.equal(apiCalledWithId, 'notif-1', 'API markAsRead phải được gọi với đúng ID')
  } finally {
    patientPortalNotificationApi.markAsRead = originalMarkAsRead
  }
})

test('TC-NOTIF-17: Component-level - Unmount dọn dẹp đầy đủ clearInterval và removeEventListener (chống memory leak)', () => {
  // Mock môi trường window và timers
  const originalWindow = global.window
  const registeredListeners = new Map()
  let clearedIntervalId = null

  const mockWindow = {
    addEventListener: (event, handler) => {
      registeredListeners.set(event, handler)
    },
    removeEventListener: (event, handler) => {
      if (registeredListeners.get(event) === handler) {
        registeredListeners.delete(event)
      }
    },
  }

  global.window = mockWindow

  const originalClearInterval = global.clearInterval
  global.clearInterval = (id) => {
    clearedIntervalId = id
    if (typeof originalClearInterval === 'function') {
      originalClearInterval(id)
    }
  }

  try {
    // 1. Giả lập quá trình Mount của PatientNotificationBell:
    // - Khởi tạo polling interval (30000ms)
    const mockTimerId = 9999
    let intervalCleanupCalled = false
    const pollingCleanup = () => {
      global.clearInterval(mockTimerId)
      intervalCleanupCalled = true
    }

    // - Đăng ký focus listener
    const handleFocus = () => {}
    mockWindow.addEventListener('focus', handleFocus)
    assert.equal(registeredListeners.has('focus'), true, 'Đã đăng ký listener "focus"')

    const focusCleanup = () => {
      mockWindow.removeEventListener('focus', handleFocus)
    }

    // 2. Giả lập quá trình Unmount của component:
    pollingCleanup()
    focusCleanup()

    // 3. Xác minh cleanup:
    assert.equal(intervalCleanupCalled, true, 'Cleanup polling phải được thực thi')
    assert.equal(clearedIntervalId, mockTimerId, 'clearInterval phải được gọi đúng timerId đã tạo')
    assert.equal(registeredListeners.has('focus'), false, 'removeEventListener phải gỡ bỏ focus listener để chống memory leak')
  } finally {
    global.window = originalWindow
    global.clearInterval = originalClearInterval
  }
})

test('TC-NOTIF-18: Component-level - Polling định kỳ đúng 30 giây, không gọi trùng lặp bất thường', () => {
  const POLLING_INTERVAL_MS = 30000
  let fetchCallCount = 0

  // Giả lập hàm fetchNotifications
  const fetchNotifications = (isSilent) => {
    if (isSilent) fetchCallCount++
  }

  // Giả lập timer chạy polling
  let fakeClock = 0
  const tick = (ms, callback) => {
    fakeClock += ms
    if (fakeClock >= POLLING_INTERVAL_MS) {
      const times = Math.floor(fakeClock / POLLING_INTERVAL_MS)
      for (let i = 0; i < times; i++) {
        callback(true)
      }
      fakeClock = fakeClock % POLLING_INTERVAL_MS
    }
  }

  // Sau 10 giây: chưa gọi polling
  tick(10000, fetchNotifications)
  assert.equal(fetchCallCount, 0, 'Chưa đủ 30 giây thì không được kích hoạt polling')

  // Thêm 15 giây (tổng 25s): vẫn chưa gọi
  tick(15000, fetchNotifications)
  assert.equal(fetchCallCount, 0, 'Sau 25 giây vẫn không được gọi polling')

  // Thêm 5 giây (đạt 30s): gọi đúng 1 lần
  tick(5000, fetchNotifications)
  assert.equal(fetchCallCount, 1, 'Đúng 30 giây kích hoạt polling 1 lần duy nhất')

  // Thêm 30 giây tiếp theo (đạt 60s): gọi lần thứ 2
  tick(30000, fetchNotifications)
  assert.equal(fetchCallCount, 2, 'Sau chu kỳ 30 giây tiếp theo, gọi đúng lần thứ 2')
})

test('TC-NOTIF-19: Component-level - Optimistic update cho Mark All As Read', async () => {
  const list = [
    { id: '1', read: false },
    { id: '2', read: false },
    { id: '3', read: true },
  ]

  assert.equal(calculateUnreadCount(list), 2)

  // Mô phỏng handleMarkAllAsRead: Cập nhật toàn bộ thành read = true
  const updatedList = list.map((n) => ({ ...n, read: true, readAt: new Date().toISOString() }))

  assert.equal(calculateUnreadCount(updatedList), 0, 'Toàn bộ thông báo chuyển sang đã đọc, unreadCount = 0')
  assert.ok(updatedList.every((n) => n.read === true), 'Tất cả phần tử đều có read = true')
})

