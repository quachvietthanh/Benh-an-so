# Contract áp mẫu bệnh án khi khám

> User story: **NCL-13-CN-004 — Dùng mẫu bệnh án khi khám**  
> Phạm vi: backend contract được chốt trước khi triển khai API/use case.  
> Phụ thuộc: NCL-13-CN-003, migration `V29__create_medical_record_template_schema.sql`.

## 1. Các quyết định đã chốt

### 1.1 Thời điểm áp mẫu

Mẫu được áp qua endpoint riêng **sau khi bệnh án đã tồn tại**, không gộp vào
`POST /medical-records`.

Lý do: bác sĩ cần xem các mẫu hợp lệ của đúng lượt khám trước khi chọn; việc
tạo bệnh án hiện tại vẫn tương thích với client cũ. API áp mẫu sẽ ghi nhận
ngay `appliedTemplateVersionId`, người áp và thời điểm áp.

Contract dự kiến:

```http
PUT /medical-records/{medicalRecordId}/template
Content-Type: application/json

{
  "templateId": "uuid"
}
```

Server nhận `templateId`, kiểm tra mẫu active và lưu **current immutable
version** của mẫu tại thời điểm áp. Client không được tự gửi version number
hay danh sách section.

### 1.2 Đổi mẫu khi đang ghi

Đổi mẫu chỉ được cho phép khi bệnh án còn editable (`DRAFT` hoặc `OPEN`) **và
tất cả tám trường lâm sàng chuẩn đều rỗng/null**:

- `chiefComplaint`, `symptoms`, `medicalHistory`, `physicalExamination`;
- `clinicalProgress`, `treatmentPlan`, `doctorInstructions`, `conclusion`.

Khi có bất kỳ nội dung nào, backend trả `409 Conflict`; không xóa, ghi đè,
hay giữ ngầm nội dung cũ. Bác sĩ phải tiếp tục với mẫu đã chọn hoặc tạo một
bệnh án/lượt khám hợp lệ khác theo workflow nghiệp vụ.

Bệnh án `SIGNED`, `LOCKED` hoặc `ARCHIVED` luôn bị chặn, phù hợp
`NCL-13-CN-004-TC-03` và quy tắc khóa sau ký.

### 1.3 Fallback mẫu chung

Mẫu fallback là **mẫu active default của specialty `GENERAL`**, không tạo
một loại thực thể "mẫu chung" thứ hai.

Thứ tự xác định mẫu hiệu lực khi mở bệnh án:

1. Bác sĩ được hiển thị các mẫu active của specialty thuộc lượt khám.
2. Nếu specialty đó không có mẫu active, server chọn default active của
   `GENERAL`, đánh dấu `fallback=true` và trả thông báo cho client.
3. Nếu `GENERAL` không có đúng một default active, thao tác không được suy
   đoán; trả `409 Conflict` với lỗi cấu hình và ghi audit thất bại.

Môi trường triển khai phải có ít nhất một template active/default cho
`GENERAL`. Việc tạo template này là dữ liệu cấu hình/seed vận hành, không
phải fallback do frontend tự dựng.

### 1.4 Thời điểm kiểm tra section bắt buộc

Section có `required=true` được kiểm tra **khi ký bệnh án**, không chặn lưu
nháp hoặc cập nhật trong lúc khám.

Nếu bệnh án đã áp template, mọi fieldCode required trong immutable version
được tham chiếu phải có giá trị không blank trước khi ký. Các điều kiện ký
hiện có (ít nhất `chiefComplaint`, `conclusion`, chẩn đoán chính hợp lệ) vẫn
được giữ. Template không được áp thì giữ nguyên các điều kiện ký hiện hành.

### 1.5 Nguồn specialty của lượt khám

`Visit.specialtyId` là snapshot nghiệp vụ duy nhất dùng để chọn mẫu và không
được suy ra lại từ doctor sau khi visit được tạo.

- Khám trực tiếp: lễ tân gửi `specialtyId`; nếu không gửi, backend gán
  `GENERAL` như hành vi tương thích hiện tại.
- Khám theo lịch hẹn: appointment phải có `specialtyId`; khi check-in,
  backend copy giá trị đó sang visit.
- Lịch hẹn cũ chưa có specialty được backfill/có hành vi tương thích là
  `GENERAL`; lịch hẹn mới được tạo sau khi triển khai field này phải chọn một
  specialty active.

Không dùng specialty của tài khoản bác sĩ làm fallback, vì bác sĩ có thể khám
nhiều chuyên khoa và dữ liệu lịch sử phải tái hiện được.

## 2. Authorization, validation và audit

Chỉ `DOCTOR` là bác sĩ phụ trách lượt khám mới được áp mẫu. `ADMIN` không có
quyền áp mẫu chỉ nhờ có quyền quản trị template. Endpoint cần đồng thời yêu
cầu permission cập nhật bệnh án và kiểm tra ngữ cảnh doctor/visit ở service.

Trước khi ghi, backend phải kiểm tra trong một transaction:

1. Bệnh án tồn tại và được khóa pessimistic.
2. Lượt khám tồn tại, active và có specialty snapshot.
3. Người gọi là doctor phụ trách lượt khám.
4. Bệnh án thỏa điều kiện trạng thái và nội dung tại mục 1.2.
5. Template được chọn active, thuộc specialty của visit; riêng fallback phải
   là default active của `GENERAL` theo mục 1.3.

Áp hoặc đổi mẫu thành công phải lưu cùng transaction với access audit của
bệnh án. Audit tối thiểu gồm record ID, template ID, template version ID,
specialty, actor, thời điểm và cờ fallback. Truy cập bị từ chối theo permission
hoặc ngữ cảnh cũng phải được audit theo cơ chế hiện có.

## 3. Response tối thiểu cần có ở giai đoạn API

Response của thao tác áp mẫu và các API đọc bệnh án phải trả được thông tin
đủ để frontend dựng đúng các mục mà không suy đoán từ template mới nhất:

```json
{
  "appliedTemplate": {
    "templateId": "uuid",
    "templateVersionId": "uuid",
    "specialtyId": "uuid",
    "name": "Internal medicine initial examination",
    "versionNo": 2,
    "fallback": false,
    "appliedBy": "uuid",
    "appliedAt": "2026-08-26T08:00:00Z",
    "sections": [
      {
        "fieldCode": "CHIEF_COMPLAINT",
        "label": "Lý do khám",
        "required": true,
        "displayOrder": 1
      }
    ]
  }
}
```

`sections` phải lấy từ version đã áp, không lấy current version của template.

## 4. Hệ quả triển khai đã được xác định

- Không cần migration mới cho việc áp mẫu tối thiểu: V29 đã có specialty của
  visit và ba cột lưu template version/actor/thời điểm trên `medical_records`.
- Cần migration tiếp theo cho `appointments.specialty_id`, cùng entity,
  request/response, command, mapper và flow check-in lịch hẹn.
- Cần một endpoint query dành cho doctor; endpoint `/system/**` hiện chỉ dành
  cho ADMIN quản trị template và không được tái sử dụng.
- Nếu cần lịch sử từng lần đổi mẫu ngoài access audit, đó là scope mới và cần
  bảng event riêng; contract hiện tại không yêu cầu bảng này.

## 4.1 API implemented in phase 2

### Get available/effective template

```http
GET /medical-records/{medicalRecordId}/template-options
```

Equivalent lookup by visit:

```http
GET /medical-records/visits/{visitId}/template-options
```

For the responsible `DOCTOR` only. It returns the visit specialty, all selectable active
templates, and the effective default. `fallback=true` means no active template exists for
the visit specialty and the response was resolved from `GENERAL`.

### Apply a template

```http
PUT /medical-records/{medicalRecordId}/template
Content-Type: application/json

{ "templateId": "uuid" }
```

The request has no template-version input: the server resolves and persists the current
immutable version. The returned medical-record response includes `appliedTemplate` with
template/version/specialty/sections/appliedBy/appliedAt/fallback.

### Errors

| Status | Typical condition |
| --- | --- |
| 400 | Missing or malformed `templateId`. |
| 403 | Caller is not a doctor or is not responsible for the visit. |
| 404 | Medical record, visit, template, or specialty does not exist. |
| 409 | Record is locked, content already exists when changing template, inactive/mismatched template, or invalid default configuration. |

## 5. Acceptance mapping

| Acceptance criterion | Contract outcome |
| --- | --- |
| TC-01 | Doctor chọn template active đúng specialty; server lưu current immutable version và trả ordered sections. |
| TC-02 | Không có template active cho specialty visit thì dùng default active của `GENERAL`, `fallback=true`, có thông báo. |
| TC-03 | Bệnh án signed/locked/archived bị từ chối đổi; bệnh án editable có nội dung cũng bị từ chối để tránh mất dữ liệu. |
| TC-04 | `appliedTemplateVersionId`, actor/time và audit cùng transaction lưu lại mẫu/version đã dùng. |
