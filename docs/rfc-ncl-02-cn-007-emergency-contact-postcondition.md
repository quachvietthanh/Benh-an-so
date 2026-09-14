# RFC-007: Đồng bộ Quy tắc Cohesive Triplet và Postcondition Người liên hệ khẩn cấp (NCL-02-CN-007)

**Mã RFC**: RFC-NCL-02-CN-007-01  
**Tác giả**: Tech Lead Bệnh Án Số  
**Trạng thái**: Đã phê duyệt kỹ thuật (Technical Approved) & Đã đồng bộ Workbook  
**Đối tượng áp dụng**: Product Owner (PO), Business Analyst (BA), Backend Team, QA Team  
**User Story liên quan**: `NCL-02-CN-007` (Epic `NCL-02` - Quản lý hồ sơ bệnh nhân)

---

## 1. Bối cảnh & Vấn đề phát hiện (Context & Problem)

Trong tài liệu đặc tả `project-workbook.xlsx`, sheet `Product Backlog (User Stories)`, Row 85:
- **Tên User Story**: `Người liên hệ khẩn cấp của bệnh nhân` (`NCL-02-CN-007`)
- **Độ ưu tiên**: `Nên có` (Should Have)
- **Cột Postcondition (Điều kiện sau hoàn thành)** hiện ghi:  
  *"Hồ sơ có ít nhất một người liên hệ khẩn cấp và mọi thay đổi đều lưu vết."*

Tuy nhiên, trong quá trình phân tích nghiệp vụ và triển khai backend:
1. **Khi tạo mới hồ sơ bệnh nhân (`NCL-02-CN-001`)**: Thông tin người liên hệ khẩn cấp là **TÙY CHỌN (Optional)**. Nhiều bệnh nhân khi đi khám một mình, người vô gia cư hoặc người trưởng thành độc lập không có nhu cầu khai báo người liên hệ khẩn cấp.
2. **Khi cập nhật hồ sơ bệnh nhân (`UpdatePatientProfile`)**:
   - Nếu bệnh nhân đã có người liên hệ khẩn cấp, nhưng vì lý do cá nhân (ly hôn, người thân qua đời, hoặc bệnh nhân yêu cầu rút lại thông tin cá nhân của bên thứ ba theo Luật Khám bệnh, chữa bệnh & Nghị định 13/2023/NĐ-CP), bệnh nhân có quyền yêu cầu xóa người liên hệ.
   - Nếu hệ thống cứng nhắc bắt buộc "hồ sơ phải luôn có ít nhất một người liên hệ khẩn cấp và cấm xóa", bệnh nhân sẽ bị tước bỏ quyền xóa thông tin, và lễ tân sẽ không thể cập nhật các thông tin khác (ví dụ đổi địa chỉ/email) nếu bệnh nhân muốn xóa người liên hệ cũ.

---

## 2. Quyết định kỹ thuật của Tech Lead (Tech Lead Decision)

Áp dụng chính thức **Quy tắc bộ ba toàn vẹn (Cohesive Triplet Rule)**:
1. **Tính tùy chọn (Optionality)**:
   - Thông tin người liên hệ khẩn cấp là tùy chọn khi tạo mới và khi cập nhật.
   - Cho phép xóa trắng người liên hệ khẩn cấp khi và chỉ khi người dùng gửi `null` hoặc chuỗi rỗng `""` cho **cả 3 trường** (`emergencyContact`, `emergencyRelationship`, `emergencyPhone`).
2. **Tính toàn vẹn (Integrity)**:
   - Nếu người dùng nhập **bất kỳ một trường nào** trong bộ ba, hệ thống **bắt buộc phải có đầy đủ cả 3 trường** hợp lệ:
     - `emergencyContact`: Không được để trống, tối đa 100 ký tự.
     - `emergencyRelationship`: Không được để trống, tối đa 50 ký tự (`@Size(max = 50)`).
     - `emergencyPhone`: Không được để trống, đúng định dạng di động Việt Nam (`^(?:(0|\+84)(3|5|7|8|9)[0-9]{8})$`).
   - Cấm hành vi cập nhật hoặc xóa nửa vời (ví dụ: chỉ nhập tên mà không nhập SĐT, hoặc xóa SĐT nhưng để lại tên).
3. **Lưu vết kiểm toán đầy đủ (Audit Trail)**:
   - Thỏa mãn vế *"mọi thay đổi đều lưu vết"*: Bất kể người liên hệ khẩn cấp được thêm mới, sửa đổi hay xóa trắng về `null`, hệ thống đều ghi nhận chi tiết vào `patient_change_logs` kèm `actorId`, `updatedAt`, `oldValue` và `newValue`.

---

## 3. Điều chỉnh trong Tài liệu Đặc tả (Workbook Synchronization)

Cập nhật lại câu chữ trong file `project-workbook.xlsx`:

| Vị trí | Nội dung cũ (Gây hiểu nhầm) | Nội dung mới (Chuẩn hóa) |
| :--- | :--- | :--- |
| **Sheet `Product Backlog`, Row 85, Col 11 (Postcondition)** | `Hồ sơ có ít nhất một người liên hệ khẩn cấp và mọi thay đổi đều lưu vết.` | `Nếu ghi nhận người liên hệ khẩn cấp thì phải có đầy đủ bộ ba thông tin và mọi thay đổi đều lưu vết.` |
| **Sheet `Tasks (Công việc)`, Row 384, Col 5 (Task Description)** | `Hồ sơ có ít nhất một người liên hệ khẩn cấp và mọi thay đổi đều lưu vết.` | `Nếu ghi nhận người liên hệ khẩn cấp thì phải có đầy đủ bộ ba thông tin và mọi thay đổi đều lưu vết.` |

---

## 4. Bằng chứng kiểm thử tự động (Automated Test Coverage)

Các test case sau đã được bổ sung vào `UpdatePatientServiceTest.java` để bảo đảm hành vi:
1. `allowsClearingEmergencyContactWhenAllThreeFieldsAreNullAndLogsChange`: Kiểm chứng khi gửi cả 3 trường null, thông tin được xóa sạch về null và `patient_change_logs` ghi nhận đầy đủ thay đổi.
2. `rejectsPartialClearingWhenOnlyEmergencyContactProvided`: Kiểm chứng khi chỉ nhập/giữ tên mà bỏ trống quan hệ/SĐT thì bị ném `ValidationException` (HTTP 400).
