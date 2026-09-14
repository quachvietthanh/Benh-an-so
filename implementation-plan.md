# Implementation Plan: Gọi lại và tạm hoãn bệnh nhân vắng trong hàng đợi (NCL-03-CN-009)

Kế hoạch kỹ thuật triển khai backend hoàn chỉnh cho User Story `NCL-03-CN-009`: **Gọi lại và tạm hoãn bệnh nhân vắng trong hàng đợi** thuộc Epic `NCL-03` (Lịch hẹn và hàng đợi khám), bao gồm kế hoạch fix các finding P2 từ báo cáo review (P2-1 Historical status fallback, P2-2 Semantic actions), đảm bảo đáp ứng đầy đủ Acceptance Criteria (TC-01, TC-02, TC-03, TC-04), bảo toàn kiến trúc Hexagonal Architecture, không gây lỗi hồi quy và tuân thủ các quy tắc nghiệp vụ.

---

## 1. TÓM TẮT QUYẾT ĐỊNH VÀ GIẢI PHÁP KỸ THUẬT

### 1.1. Mục tiêu và Phạm vi
* **Mục tiêu**: Xây dựng hoàn chỉnh luồng nghiệp vụ backend khi bệnh nhân vắng mặt tại thời điểm gọi khám: cho phép tạm hoãn (defer/skip), tự động chuyển lượt sang người kế tiếp, ghi nhận số lần gọi (`callCount`), và cho phép Lễ tân đưa bệnh nhân trở lại hàng đợi (`WAITING`) khi có mặt; lưu vết lịch sử phục vụ kiểm toán với timeline chính xác.
* **Phạm vi**: Chỉ backend (Domain, Ports, Services, Persistence, REST API, Audit, Tests). Không thay đổi frontend.

### 1.2. Các quyết định kỹ thuật cốt lõi
1. **Khắc phục Bug huỷ dữ liệu của Skip hiện tại**:
   - Khi tạm hoãn, `QueueItem` chuyển sang `SKIPPED`, `Visit` được hoàn trả về trạng thái `WAITING` (thông qua phương thức `revertToWaiting()`), và `Appointment` hoàn trả về trạng thái `CHECKED_IN` (thông qua phương thức `revertToCheckedIn()`). Tuyệt đối **KHÔNG huỷ (`cancel`)** Visit và Appointment khi tạm hoãn.
2. **Số lần gọi (`callCount`)**:
   - Thêm cột `call_count INT NOT NULL DEFAULT 0` vào bảng `queue_items` thông qua migration `V54__add_queue_item_call_count_and_defer_support.sql`.
   - `callCount` tăng lên 1 mỗi khi phương thức `call()` được gọi (tại `CallNextQueueItemService`).
3. **Cơ chế Tự chuyển người kế tiếp (TC-01)**:
   - Trong `SkipQueueItemService`: Sau khi chuyển ca hiện tại sang tạm hoãn, use case tự động tìm ca kế tiếp đang `WAITING` trong queue để gọi (`callNext`) nếu còn bệnh nhân chờ; nếu queue rỗng thì chỉ hoàn tất tạm hoãn ca hiện tại.
4. **Cơ chế Đưa lại vào hàng đợi (Re-queue - TC-02)**:
   - Thêm API `POST /queue-items/{itemId}/re-queue`.
   - Domain `QueueItem.reQueue(Instant)`: Cho phép chuyển đổi hợp lệ từ `SKIPPED -> WAITING`.
   - Sắp xếp vị trí: Giữ nguyên `queueNumber` gốc (vì các số nhỏ hơn đã khám xong nên tự nhiên ca này sẽ nằm ở đầu danh sách `WAITING` được ưu tiên gọi sớm), đồng thời tôn trọng mức độ ưu tiên của hàng đợi (`priorityLevel`: EMERGENCY > APPOINTMENT > REGULAR).
5. **Chặn trạng thái sai (TC-03)**:
   - Chặn và ném `QueueItemInvalidStatusException` nếu ca đã `COMPLETED` hoặc `CANCELLED` khi gọi tạm hoãn.
6. **Lịch sử hàng đợi & Khắc phục 2 Finding P2 (TC-04)**:
   - **Fix P2-1 (History historical status fallback)**:
     - `QueueCheckInCoordinator` lưu tường minh `status="WAITING"` và `callCount=0` trong audit detail của sự kiện `CHECK_IN`.
     - `GetQueueHistoryService` tuyệt đối không fallback về `item.getStatus().name()` (trạng thái hiện tại của entity). Toàn bộ historical status phải lấy từ audit payload snapshot.
   - **Fix P2-2 (Queue history semantic actions)**:
     - Định nghĩa chuẩn 6 semantic actions: `CHECK_IN`, `CALL`, `DEFERRED`, `RE_QUEUED`, `COMPLETED`, `CANCELLED`.
     - `QueueAuditService` persist explicit key `"action"` trong mọi detail JSON của hàng đợi.
     - `GetQueueHistoryService` đọc trực tiếp từ `node.get("action")`, không suy luận ngược từ generic `ActionType.name()`.
   - **Đồng bộ Migration & Quyền hạn (F-4, F-6)**:
     - Migration chính thức là `V54__add_queue_item_call_count_and_defer_support.sql`.
     - Quyền `USER_READ` cho `RECEPTIONIST` trong `V54` là hợp lệ để Lễ tân lấy danh sách bác sĩ phục vụ đặt hẹn và tiếp nhận bệnh nhân.

---

## 2. KẾ HOẠCH TRIỂN KHAI THEO TỪNG GIAI ĐOẠN

### Giai đoạn 1: Database Migration V54 & Domain Foundation
* **File tác động**:
  * `backend/src/main/resources/db/migration/V54__add_queue_item_call_count_and_defer_support.sql`
  * `backend/src/main/java/com/benhsoan/domain/queue/QueueItem.java`
  * `backend/src/main/java/com/benhsoan/domain/visit/Visit.java`
  * `backend/src/main/java/com/benhsoan/domain/appointment/Appointment.java`
  * `backend/src/main/java/com/benhsoan/domain/queue/enums/QueueSemanticAction.java` [NEW]
* **Quy tắc**: `callCount++`, `reQueue()`, enum `QueueSemanticAction`.

### Giai đoạn 2: Chuẩn hóa Ghi Audit Log Hàng Đợi (Fix P2-1 & P2-2)
* **File tác động**:
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/QueueAuditService.java`
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/QueueCheckInCoordinator.java`
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/CallNextQueueItemService.java`
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/CompleteQueueItemService.java`
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/SkipQueueItemService.java`
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/UpdateQueueItemStatusService.java`
* **Quy tắc**:
  - `recordCheckIn`: lưu `action="CHECK_IN"`, `status="WAITING"`, `callCount=0`.
  - `recordCall`: lưu `action="CALL"`, `status="IN_PROGRESS"`, `callCount=item.getCallCount()`.
  - `recordCompleted`: lưu `action="COMPLETED"`, `status="COMPLETED"`, `callCount=item.getCallCount()`.
  - `recordCancelled`: lưu `action="CANCELLED"`, `status="CANCELLED"`, `callCount=item.getCallCount()`.
  - `recordSkipped`: lưu `action="DEFERRED"`.
  - `recordReQueued`: lưu `action="RE_QUEUED"`.

### Giai đoạn 3: Chuẩn hóa Đọc Lịch Sử Hàng Đợi (Fix P2-1 & P2-2)
* **File tác động**:
  * `backend/src/main/java/com/benhsoan/application/ucservice/queue/GetQueueHistoryService.java`
* **Quy tắc**:
  - Đọc `action` từ payload `node.get("action")`.
  - Đọc `status` từ payload `node.get("status")`.
  - Tuyệt đối không fallback về `item.getStatus()` hoặc `ActionType.name()`.

### Giai đoạn 4: Kiểm thử và Xác minh
* **File tác động**:
  * `backend/src/test/java/com/benhsoan/application/ucservice/queue/QueueAuditServiceTest.java`
  * `backend/src/test/java/com/benhsoan/application/ucservice/queue/GetQueueHistoryServiceTest.java`
  * `backend/src/test/java/com/benhsoan/adapter/inbound/rest/controller/QueueControllerTest.java`
  * `backend/src/test/java/com/benhsoan/adapter/inbound/rest/controller/FullClinicalEncounterWorkflowE2EIntegrationTest.java`
* **Quy tắc**:
  - Verify toàn bộ chuỗi lifecycle và lịch sử không bị đè trạng thái.
