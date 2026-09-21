# Báo cáo hiệu quả lịch hẹn (NCL-08-CN-008)

Backend API contract for the appointment effectiveness report.

## Endpoint

```
GET /reports/appointment-effectiveness?from={yyyy-MM-dd}&to={yyyy-MM-dd}[&doctorId={uuid}][&bookingChannel={ONLINE_PORTAL|RECEPTION_COUNTER}]
```

- Method: `GET`
- Authorization: `@RequirePermission("REPORT_VIEW")`
  - `REPORT_VIEW` is seeded to the `MANAGER` role only (V2 seed). Therefore:
    - `MANAGER` → 200
    - `ADMIN` → 403 (ADMIN is intentionally not granted `REPORT_VIEW`)
    - `DOCTOR`, `PHARMACIST`, `RECEPTIONIST` → 403
    - unauthenticated → 401
  - Defense-in-depth: `GetAppointmentEffectivenessReportService` also rejects any
    caller without the `MANAGER` role (service-level `ensureAuthorized`), mirroring
    the sibling MANAGER-only report services.
- Period semantics: half-open interval `[from 00:00, to+1d 00:00)` in clinic timezone
  `Asia/Ho_Chi_Minh`, keyed on `appointments.start_time` (the scheduled occurrence time, **not**
  `created_at`). A rescheduled appointment is counted once, at its current `start_time`.

## Query parameters

| Param | Required | Meaning |
|---|---|---|
| `from` | yes | `yyyy-MM-dd`, inclusive start date |
| `to` | yes | `yyyy-MM-dd`, inclusive end date |
| `doctorId` | no | UUID; restrict the population to one doctor |
| `bookingChannel` | no | `ONLINE_PORTAL` (portal bookings) or `RECEPTION_COUNTER` (at-counter/legacy bookings, stored as `NULL` `booking_channel`). Any other value → 400. |

Validation: `from`/`to` required (`yyyy-MM-dd`), `from <= to`, inclusive range ≤ 366 days.
Violations return `400` (`VALIDATION_ERROR` / `MISSING_PARAMETER`).

## Response (200 OK)

```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "generatedAt": "2026-08-31T08:00:00Z",
  "total": 100,
  "items": [
    { "bookingChannel": "RECEPTION_COUNTER", "status": "COMPLETED", "count": 40, "percentage": 40.00 },
    { "bookingChannel": "ONLINE_PORTAL", "status": "COMPLETED", "count": 10, "percentage": 10.00 },
    { "bookingChannel": "RECEPTION_COUNTER", "status": "CANCELLED", "count": 30, "percentage": 30.00 },
    { "bookingChannel": "ONLINE_PORTAL", "status": "NO_SHOW", "count": 20, "percentage": 20.00 }
  ]
}
```

- `total`: total appointments in the (filtered) period.
- `items`: one entry per (`bookingChannel`, `AppointmentStatus`) pair that has `count > 0`.
  Ordered by `bookingChannel` (lexicographic: `ONLINE_PORTAL` before `RECEPTION_COUNTER`)
  then by `AppointmentStatus` enum declaration order
  (`SCHEDULED, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW`).
- `bookingChannel`: the normalized channel label (see below). `ONLINE_PORTAL` for portal
  bookings; `RECEPTION_COUNTER` for at-counter/legacy bookings.
- `percentage`: `count × 100 / total`, `BigDecimal` scale 2, `RoundingMode.HALF_UP`.
  Percentages across all items are consistent with `total` (sum ≈ 100.00 modulo rounding).

## Booking channel semantics (NCL-08-CN-008-TC-02)

A single request (without the `bookingChannel` filter) returns the data split by booking
channel: one group for at-counter appointments and one group for patient-portal
appointments. The two groups are returned in the same `items` list, distinguished by the
`bookingChannel` field.

| `bookingChannel` value | Stored `booking_channel` column value | Meaning |
|---|---|---|
| `ONLINE_PORTAL` | `ONLINE_PORTAL` | Booked through the patient portal |
| `RECEPTION_COUNTER` | `NULL` | Booked at the reception counter / legacy in-person (the column is `NULL`) |

`RECEPTION_COUNTER` is an API/reporting label only; it is **not** physically stored in the
`booking_channel` column. `NULL` is the authoritative representation of at-counter bookings
(the `ONLINE_PORTAL` literal is written by `PatientBookAppointmentService`; counter bookings
do not set the column). No other database value is introduced.

## Status values (authoritative)

`SCHEDULED`, `CONFIRMED`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`
(enum `com.benhsoan.domain.appointment.enums.AppointmentStatus`). The report does **not**
map technical statuses to high-level business labels (for example there is **no**
`arrived = COMPLETED` aggregation): the raw `AppointmentStatus` value is returned as-is.

## Empty period (NCL-08-CN-008-TC-03)

When there are no appointments in the period (or for the applied filters):

```json
{ "from": "2026-08-01", "to": "2026-08-31", "generatedAt": "...", "total": 0, "items": [] }
```

The frontend should render the "no data" state when `total == 0` (or `items` is empty).

## Filters

- `doctorId` narrows the report population to one doctor. Percentages are always relative
  to the filtered population's `total`.
- `bookingChannel` optionally narrows the population to a single channel
  (`ONLINE_PORTAL` or `RECEPTION_COUNTER`). When omitted, the response contains **both**
  channels (the TC-02 breakdown). When provided, only that channel's rows are returned.

## Error responses

| Case | HTTP | code | message |
|---|---|---|---|
| Missing `from`/`to` | 400 | `MISSING_PARAMETER` | `from is required.` / `to is required.` |
| Invalid date format | 400 | `VALIDATION_ERROR` | `from must be in yyyy-MM-dd format.` |
| `from > to` | 400 | `VALIDATION_ERROR` | `from must be before or equal to to.` |
| Range > 366 days | 400 | `VALIDATION_ERROR` | `Date range must not exceed 366 days.` |
| Invalid `bookingChannel` | 400 | `VALIDATION_ERROR` | `bookingChannel must be one of: ONLINE_PORTAL, RECEPTION_COUNTER.` |
| Unauthenticated | 401 | `AUTHENTICATION_FAILED` | — |
| No `REPORT_VIEW` | 403 | `ACCESS_DENIED` | — |

## Example request

```
GET /reports/appointment-effectiveness?from=2026-08-01&to=2026-08-31
GET /reports/appointment-effectiveness?from=2026-08-01&to=2026-08-31&bookingChannel=ONLINE_PORTAL
GET /reports/appointment-effectiveness?from=2026-08-01&to=2026-08-31&doctorId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2
```
