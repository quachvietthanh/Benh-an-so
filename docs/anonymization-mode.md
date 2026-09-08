# Anonymization Mode (NCL-15-CN-003 — CV-01 & CV-02)

> Scope: this document covers **CV-01** (field identification) and **CV-02**
> (anonymization mechanism) only. The administrator UI toggle (**CV-03**) and
> testing (**CV-04**) are intentionally out of scope.

## 1. Purpose

During demonstrations, patient-identifying information must not be exposed on
screen or in exported files. This feature masks identity **only at the
presentation/export boundary**; the underlying patient records are never
modified.

## 2. Fields identified (CV-01)

### Mandatory anonymization (required by the Excel spec)

| Field | Where exposed | Strategy |
| ----- | ------------- | -------- |
| Patient full name | `Patient`, `PatientResult`/`PatientResponse`, `VisitEncounterResult`/`VisitEncounterResponse`, `MedicalRecordDetailResult`/`MedicalRecordDetailResponse`, `PrescriptionResult`/`PrescriptionResponse`, `QueueItemResult`/`QueueItemResponse`, `PayableEncounterResult`/`PayableEncounterResponse`, `PrescriptionPrintDocument` (PDF), `MedicalRecordCopyDocument` (PDF) | `BỆNH NHÂN #<patientCode>` |
| Patient phone | `Patient`, `PatientResult`/`PatientResponse`, `VisitEncounterResult.PatientInfo`, `MedicalRecordDetailResult.PatientInfo` | `09******78` (first 2 + last 2 digits) |
| Patient address | `Patient`, `PatientResult`/`PatientResponse` | `[ĐỊA CHỈ ĐÃ ẨN DANH]` |

### Candidate identifying fields (NOT anonymized — out of current scope)

| Field | Note |
| ----- | ---- |
| `identityNumber` | Directly identifying, but not required by the current acceptance criteria. |
| `insuranceNumber` | Directly identifying, not required. |
| `email` | Identifying, not required. |
| `dateOfBirth` | Quasi-identifier; kept because it is clinically relevant and not required. |
| `emergencyContact` / `emergencyPhone` | Third-party contact, not required. |
| `patientCode` | Internal business reference (non-identifying); retained and reused as the stable mask key. |

## 3. Anonymization strategy (CV-02)

Deterministic, readable masking:

- **Full name** → `BỆNH NHÂN #<patientCode>` so the same patient stays
  recognizable during a demo.
- **Phone** → keeps only the first two and last two digits, e.g.
  `0912345678` → `09******78`.
- **Address** → fixed placeholder `[ĐỊA CHỈ ĐÃ ẨN DANH]`.

## 4. Where the mode state lives

`PatientAnonymizationService` (application layer) holds a thread-safe
in-memory `AtomicBoolean`:

- `enable()`, `disable()`, `isEnabled()` — the internal contract for the future
  administrator toggle (CV-03).
- Default is **OFF**; optionally boot ON via `app.anonymization.enabled`
  (env `ANONYMIZATION_ENABLED`).
- No database schema change, no persistence.

Masking rules themselves live in the pure domain policy
`domain.patient.PatientAnonymizer` (framework-free, no Spring).

## 5. Where masking is applied

### REST responses (presentation boundary)

Masking is applied in the REST mappers (single shared mechanism, no controller
logic):

- `PatientRestMapper` — full name, phone, address.
- `VisitRestMapper` — full name, phone.
- `MedicalRecordDetailRestMapper` — full name, phone.
- `PrescriptionRestMapper` — full name.
- `QueueRestMapper` — full name.
- `BillingRestMapper` — full name.

### Exports (TC-02)

- `ExportPrescriptionService` — masks patient name in the prescription PDF.
- `IssueMedicalRecordCopyService` — masks patient name in the medical record
  copy PDF.

The operational report CSV (`ExportOperationalReportService`) does not contain
patient identity and is therefore unchanged.

## 6. Database safety

Original patient data remains unchanged. Masking happens only after data is
read and before it is returned/exported; nothing is written back.

## 7. Future toggle (CV-03)

The later UI toggle must call `PatientAnonymizationService.enable()` /
`disable()` behind an administrator permission. No mode-switch endpoint or UI
is implemented here.
